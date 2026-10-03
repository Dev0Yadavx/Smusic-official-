package com.example.player

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.example.data.remote.JioSaavnImageResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SMusicNotificationHelper {

    const val CHANNEL_PLAYBACK = "music_playback_channel"
    const val CHANNEL_DOWNLOADS = "smusic_downloads_channel"
    const val CHANNEL_CLOUD_SYNC = "smusic_cloud_channel"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannelsCreated(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java) ?: return

                val playbackChannel = NotificationChannel(
                    CHANNEL_PLAYBACK,
                    "SMusic Playback",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Media playback controls, lockscreen player, and song artwork"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                val downloadChannel = NotificationChannel(
                    CHANNEL_DOWNLOADS,
                    "SMusic Downloads",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Offline song download progress and completion alerts"
                    setShowBadge(true)
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                val cloudChannel = NotificationChannel(
                    CHANNEL_CLOUD_SYNC,
                    "Playlists & Library",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Playlist updates and library alerts"
                    setShowBadge(true)
                    enableVibration(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }

                manager.createNotificationChannels(
                    listOf(playbackChannel, downloadChannel, cloudChannel)
                )
            }
        } catch (_: Throwable) {}
    }

    private fun buildLaunchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun showDownloadProgressNotification(
        context: Context,
        songId: String,
        title: String,
        artist: String,
        progress: Int,
        isPaused: Boolean = false
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannelsCreated(context)

        val notifId = 2000 + (songId.hashCode() and 0x7FFFFFFF) % 5000
        val statusText = if (isPaused) "Paused ($progress%)" else "Downloading • $progress%"

        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title)
            .setContentText("$artist • $statusText")
            .setSubText("SMusic Offline")
            .setProgress(100, progress.coerceIn(0, 100), false)
            .setOngoing(!isPaused)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(buildLaunchPendingIntent(context))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {}
    }

    fun showDownloadCompletedNotification(
        context: Context,
        songId: String,
        title: String,
        artist: String,
        artworkUrl: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannelsCreated(context)

        val notifId = 2000 + (songId.hashCode() and 0x7FFFFFFF) % 5000
        scope.launch {
            val bitmap = loadArtworkBitmap(context, artworkUrl)
            val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOADS)
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(bitmap)
                .setContentTitle(title)
                .setContentText("$artist • Saved for offline playback (320 kbps)")
                .setSubText("Download Complete")
                .setAutoCancel(true)
                .setOngoing(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(buildLaunchPendingIntent(context))
                .build()

            try {
                NotificationManagerCompat.from(context).notify(notifId, notification)
            } catch (_: SecurityException) {}
        }
    }

    fun cancelDownloadNotification(context: Context, songId: String) {
        val notifId = 2000 + (songId.hashCode() and 0x7FFFFFFF) % 5000
        try {
            NotificationManagerCompat.from(context).cancel(notifId)
        } catch (_: Exception) {}
    }

    fun showPlaylistSyncNotification(
        context: Context,
        playlistId: String,
        playlistTitle: String,
        songCount: Int,
        artworkUrl: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannelsCreated(context)

        val notifId = 8000 + (playlistId.hashCode() and 0x7FFFFFFF) % 1000
        scope.launch {
            val bitmap = loadArtworkBitmap(context, artworkUrl)
            val notification = NotificationCompat.Builder(context, CHANNEL_CLOUD_SYNC)
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(bitmap)
                .setContentTitle(playlistTitle)
                .setContentText("$songCount songs saved to Playlists")
                .setSubText("Library")
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(buildLaunchPendingIntent(context))
                .build()

            try {
                NotificationManagerCompat.from(context).notify(notifId, notification)
            } catch (_: SecurityException) {}
        }
    }

    suspend fun loadArtworkBitmap(context: Context, rawUrl: String): Bitmap = withContext(Dispatchers.IO) {
        val resolved = JioSaavnImageResolver.resolve(rawUrl, 500).ifBlank { rawUrl }
        if (resolved.isNotBlank()) {
            try {
                val loader = ImageLoader(context.applicationContext)
                val request = ImageRequest.Builder(context.applicationContext)
                    .data(resolved)
                    .size(512, 512)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                val drawable = result.drawable
                if (drawable is BitmapDrawable && drawable.bitmap != null) {
                    return@withContext drawable.bitmap
                }
            } catch (_: Exception) {}
        }
        createFallbackBitmap(context)
    }

    fun createFallbackBitmap(context: Context): Bitmap {
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
        val noteDrawable = ContextCompat.getDrawable(context, R.drawable.ic_music_note)
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
