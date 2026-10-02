package com.example.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.example.repository.MusicRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {

    private var player: Player? = null
    private var mediaSession: MediaSession? = null
    private var becomingNoisyReceiverRegistered = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val imageLoader by lazy { ImageLoader(applicationContext) }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                PlayerManager.getInstance(applicationContext).pause()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val playerManager = PlayerManager.getInstance(applicationContext)
        val basePlayer = playerManager.getOrCreatePlayer()
        player = basePlayer
        playerManager.attachPlayer(basePlayer, this)

        // Wrap Player with ForwardingPlayer so Lockscreen, Notification, Bluetooth AVRCP,
        // and Headset controls always work across all Android versions and OEMs.
        val forwardingPlayer = object : ForwardingPlayer(basePlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_PREPARE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_DEFAULT_POSITION)
                    .add(Player.COMMAND_SEEK_TO_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_GET_TIMELINE)
                    .add(Player.COMMAND_GET_METADATA)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_PREPARE,
                    Player.COMMAND_STOP,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                    Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                    Player.COMMAND_SEEK_BACK,
                    Player.COMMAND_SEEK_FORWARD,
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_GET_TIMELINE,
                    Player.COMMAND_GET_METADATA -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun hasNextMediaItem(): Boolean {
                return playerManager.currentTrack.value != null
            }

            override fun hasPreviousMediaItem(): Boolean {
                return playerManager.currentTrack.value != null
            }

            override fun play() {
                playerManager.resume()
            }

            override fun pause() {
                playerManager.pause()
            }

            override fun setPlayWhenReady(playWhenReady: Boolean) {
                if (playWhenReady) {
                    playerManager.resume()
                } else {
                    playerManager.pause()
                }
            }

            override fun seekToNext() {
                playerManager.skipToNext()
            }

            override fun seekToNextMediaItem() {
                playerManager.skipToNext()
            }

            override fun seekToPrevious() {
                playerManager.skipToPrevious()
            }

            override fun seekToPreviousMediaItem() {
                playerManager.skipToPrevious()
            }

            override fun seekBack() {
                val target = (playerManager.currentPositionMs.value - 10_000L).coerceAtLeast(0L)
                playerManager.seekTo(target)
            }

            override fun seekForward() {
                val maxDur = playerManager.durationMs.value.coerceAtLeast(0L)
                val target = (playerManager.currentPositionMs.value + 10_000L).let {
                    if (maxDur > 0L) it.coerceAtMost(maxDur) else it
                }
                playerManager.seekTo(target)
            }

            override fun seekTo(positionMs: Long) {
                playerManager.seekTo(positionMs)
            }

            override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
                playerManager.seekTo(positionMs)
            }
        }

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Configure Media3 Notification Provider with crisp monochrome icon and full lockscreen visibility
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setNotificationId(NOTIFICATION_ID)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_music_note)
            }
        setMediaNotificationProvider(notificationProvider)

        val builtSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivityIntent)
            .setBitmapLoader(CoilMediaBitmapLoader())
            .setCallback(ServiceSessionCallback())
            .build()
        mediaSession = builtSession
        try {
            addSession(builtSession)
        } catch (_: Exception) {}

        // Observe current track like state to keep Lockscreen & Notification heart icon in sync
        serviceScope.launch {
            playerManager.isCurrentTrackLiked.collectLatest { isLiked ->
                updateCustomHeartButton(isLiked)
                syncMediaNotification()
            }
        }

        // Observe track, playing state, buffering, and duration so Notification always stays in sync
        serviceScope.launch {
            kotlinx.coroutines.flow.combine(
                playerManager.currentTrack,
                playerManager.isPlaying,
                playerManager.isBuffering,
                playerManager.durationMs
            ) { track, playing, buffering, _ ->
                Triple(track, playing, buffering)
            }.collectLatest { (track, playing, buffering) ->
                if (track != null) {
                    syncMediaNotification(forceForeground = playing || buffering)
                }
            }
        }

        registerBecomingNoisy()
    }

    fun syncMediaNotification(forceForeground: Boolean? = null) {
        val session = mediaSession ?: return
        val pm = PlayerManager.getInstance(applicationContext)
        if (pm.currentTrack.value == null) return
        val foreground = forceForeground ?: (pm.isPlaying.value || pm.isBuffering.value)
        try {
            onUpdateNotification(session, foreground)
        } catch (_: Exception) {}
    }

    fun updateCustomHeartButton(isLiked: Boolean) {
        val favoriteButton = CommandButton.Builder()
            .setDisplayName(if (isLiked) "Liked" else "Like")
            .setIconResId(if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
            .setSessionCommand(SessionCommand(CUSTOM_ACTION_LIKE, Bundle.EMPTY))
            .build()
        mediaSession?.setCustomLayout(ImmutableList.of(favoriteButton))
    }

    private fun createNotificationChannel() {
        SMusicNotificationHelper.ensureChannelsCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        syncMediaNotification()
        return result
    }

    private fun registerBecomingNoisy() {
        if (!becomingNoisyReceiverRegistered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            ContextCompat.registerReceiver(
                this,
                becomingNoisyReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            becomingNoisyReceiverRegistered = true
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val pm = PlayerManager.getInstance(applicationContext)
        if (!pm.isPlaying.value && !pm.isBuffering.value) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        if (becomingNoisyReceiverRegistered) {
            try {
                unregisterReceiver(becomingNoisyReceiver)
            } catch (_: Exception) {}
            becomingNoisyReceiverRegistered = false
        }
        mediaSession?.run {
            release()
            mediaSession = null
        }
        player = null
        super.onDestroy()
    }

    private inner class ServiceSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(CUSTOM_ACTION_LIKE, Bundle.EMPTY))
                .build()
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_PREPARE)
                .add(Player.COMMAND_STOP)
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_BACK)
                .add(Player.COMMAND_SEEK_FORWARD)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == CUSTOM_ACTION_LIKE) {
                PlayerManager.getInstance(applicationContext).toggleCurrentTrackLike()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    /**
     * Universal Coil-backed BitmapLoader for Media3 Lockscreen & Notification Album Artwork.
     * Ensures software ARGB_8888 Bitmaps (<= 512x512) are always delivered for online URLs,
     * local files, byte arrays, and provides a clean fallback cover if artwork is missing.
     */
    private inner class CoilMediaBitmapLoader : BitmapLoader {
        override fun supportsMimeType(mimeType: String): Boolean {
            return mimeType.startsWith("image/")
        }

        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
            val future = SettableFuture.create<Bitmap>()
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val decoded = BitmapFactory.decodeByteArray(data, 0, data.size)
                    if (decoded != null) {
                        val safeBitmap = scaleAndEnsureSoftwareBitmap(decoded)
                        future.set(safeBitmap)
                    } else {
                        future.set(createFallbackArtworkBitmap())
                    }
                } catch (e: Exception) {
                    future.set(createFallbackArtworkBitmap())
                }
            }
            return future
        }

        override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
            val future = SettableFuture.create<Bitmap>()
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val rawStr = uri.toString()
                    val resolvedStr = com.example.data.remote.JioSaavnImageResolver.resolve(rawStr, 500).ifBlank { rawStr }
                    val request = ImageRequest.Builder(applicationContext)
                        .data(resolvedStr)
                        .size(512, 512)
                        .allowHardware(false)
                        .build()
                    val result = imageLoader.execute(request)
                    val drawable = result.drawable
                    if (drawable is BitmapDrawable && drawable.bitmap != null) {
                        future.set(scaleAndEnsureSoftwareBitmap(drawable.bitmap))
                    } else {
                        future.set(createFallbackArtworkBitmap())
                    }
                } catch (e: Exception) {
                    future.set(createFallbackArtworkBitmap())
                }
            }
            return future
        }

        override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
            val data = metadata.artworkData
            if (data != null && data.isNotEmpty()) {
                return decodeBitmap(data)
            }
            val uri = metadata.artworkUri
            if (uri != null) {
                return loadBitmap(uri)
            }
            return Futures.immediateFuture(createFallbackArtworkBitmap())
        }

        private fun scaleAndEnsureSoftwareBitmap(src: Bitmap): Bitmap {
            val maxDim = 512
            val width = src.width.coerceAtLeast(1)
            val height = src.height.coerceAtLeast(1)
            val scaled = if (width > maxDim || height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
                val targetW = (width * ratio).toInt().coerceAtLeast(1)
                val targetH = (height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(src, targetW, targetH, true)
            } else {
                src
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && scaled.config == Bitmap.Config.HARDWARE) {
                scaled.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                scaled
            }
        }

        private fun createFallbackArtworkBitmap(): Bitmap {
            val size = 512
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                intArrayOf(
                    Color.parseColor("#1E1B4B"),
                    Color.parseColor("#4338CA"),
                    Color.parseColor("#7C3AED")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

            val noteDrawable = ContextCompat.getDrawable(applicationContext, R.drawable.ic_music_note)
            if (noteDrawable != null) {
                val iconSize = 200
                val left = (size - iconSize) / 2
                val top = (size - iconSize) / 2
                noteDrawable.setBounds(left, top, left + iconSize, top + iconSize)
                noteDrawable.setTint(Color.WHITE)
                noteDrawable.draw(canvas)
            }
            return bmp
        }
    }

    companion object {
        const val CHANNEL_ID = "music_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val CUSTOM_ACTION_LIKE = "com.example.ACTION_LIKE"
    }
}
