package com.example.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.data.model.PlayableTrack
import com.example.data.remote.StreamUrlResolver
import com.example.repository.MusicRepository
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

enum class RepeatMode {
    OFF, ALL, ONE
}

@OptIn(UnstableApi::class)
class PlayerManager private constructor(private val appContext: Context) {

    private val tag = "PlayerManager"
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val repository = MusicRepository(appContext)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val equalizerManager: EqualizerManager = EqualizerManager.getInstance(appContext)

    private var mediaPlayer: MediaPlayer? = null
    private var isMediaPlayerPrepared = false
    private var playWhenReadyRequested = false
    private var currentPlaybackState: Int = Player.STATE_IDLE
    private var activeMediaItem: MediaItem? = null
    private var serviceContext: Context? = null
    private var artworkJob: Job? = null
    private var playbackJob: Job? = null

    // State flows
    private val _currentTrack = MutableStateFlow<PlayableTrack?>(null)
    val currentTrack: StateFlow<PlayableTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<PlayableTrack>>(emptyList())
    val queue: StateFlow<List<PlayableTrack>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isAutoplayEnabled = MutableStateFlow(true)
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isCurrentTrackLiked = MutableStateFlow(false)
    val isCurrentTrackLiked: StateFlow<Boolean> = _isCurrentTrackLiked.asStateFlow()

    private var currentArtworkBytes: ByteArray? = null
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var progressJob: Job? = null
    private var originalQueueBeforeShuffle: List<PlayableTrack> = emptyList()
    private var isFetchingReco = false
    private var resumeOnFocusGain = false
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                resumeOnFocusGain = _isPlaying.value
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                val duckVol = (_volume.value * 0.25f).coerceIn(0f, 1f)
                try {
                    mediaPlayer?.setVolume(duckVol, duckVol)
                } catch (_: Exception) {}
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                val vol = _volume.value
                try {
                    mediaPlayer?.setVolume(vol, vol)
                } catch (_: Exception) {}
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    resume()
                }
            }
        }
    }

    private inner class SessionPlayerImpl : SimpleBasePlayer(Looper.getMainLooper()) {
        fun notifyStateChanged() {
            invalidateState()
        }

        override fun getState(): State {
                val commands = Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_PREPARE,
                        Player.COMMAND_STOP,
                        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_BACK,
                        Player.COMMAND_SEEK_FORWARD,
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        Player.COMMAND_SET_REPEAT_MODE,
                        Player.COMMAND_SET_SHUFFLE_MODE,
                        Player.COMMAND_SET_VOLUME,
                        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_GET_TIMELINE,
                        Player.COMMAND_GET_METADATA,
                        Player.COMMAND_GET_AUDIO_ATTRIBUTES
                    )
                    .build()

                val repeatModeConst = when (this@PlayerManager._repeatMode.value) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                }

                val item = this@PlayerManager.activeMediaItem
                val builder = State.Builder()
                    .setAvailableCommands(commands)
                    .setPlayWhenReady(
                        this@PlayerManager._isPlaying.value || (this@PlayerManager._isBuffering.value && this@PlayerManager.playWhenReadyRequested),
                        Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
                    )
                    .setPlaybackState(this@PlayerManager.currentPlaybackState)
                    .setIsLoading(this@PlayerManager._isBuffering.value)
                    .setRepeatMode(repeatModeConst)
                    .setShuffleModeEnabled(this@PlayerManager._isShuffleEnabled.value)
                    .setVolume(this@PlayerManager._volume.value)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .setUsage(C.USAGE_MEDIA)
                            .build()
                    )

                if (item != null) {
                    val durUs = if (this@PlayerManager._durationMs.value > 0) this@PlayerManager._durationMs.value * 1000L else C.TIME_UNSET
                    val baseId = item.mediaId.ifBlank { "current_track" }
                    val prevItemData = MediaItemData.Builder("prev_$baseId")
                        .setMediaItem(MediaItem.fromUri(Uri.EMPTY))
                        .setIsSeekable(false)
                        .build()
                    val currentItemData = MediaItemData.Builder(baseId)
                        .setMediaItem(item)
                        .setMediaMetadata(item.mediaMetadata)
                        .setIsSeekable(true)
                        .setIsDynamic(false)
                        .setDurationUs(durUs)
                        .build()
                    val nextItemData = MediaItemData.Builder("next_$baseId")
                        .setMediaItem(MediaItem.fromUri(Uri.EMPTY))
                        .setIsSeekable(false)
                        .build()

                    builder.setPlaylist(listOf(prevItemData, currentItemData, nextItemData))
                    builder.setCurrentMediaItemIndex(1)
                    builder.setPlaylistMetadata(item.mediaMetadata)

                    val currentPos = this@PlayerManager.getCurrentPositionSafe()
                    val isAdvancing = this@PlayerManager._isPlaying.value &&
                        !this@PlayerManager._isBuffering.value &&
                        this@PlayerManager.currentPlaybackState == Player.STATE_READY
                    builder.setContentPositionMs(
                        if (isAdvancing) {
                            SimpleBasePlayer.PositionSupplier.getExtrapolating(currentPos, 1.0f)
                        } else {
                            SimpleBasePlayer.PositionSupplier.getConstant(currentPos)
                        }
                    )
                } else {
                    builder.setPlaylist(emptyList())
                    builder.setCurrentMediaItemIndex(C.INDEX_UNSET)
                    builder.setContentPositionMs(SimpleBasePlayer.PositionSupplier.getConstant(0L))
                }

                return builder.build()
            }

            override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
                if (playWhenReady) {
                    resume()
                } else {
                    pause()
                }
                return Futures.immediateVoidFuture()
            }

            override fun handlePrepare(): ListenableFuture<*> {
                return Futures.immediateVoidFuture()
            }

            override fun handleStop(): ListenableFuture<*> {
                stopPlaybackInternal()
                return Futures.immediateVoidFuture()
            }

            override fun handleSeek(
                mediaItemIndex: Int,
                positionMs: Long,
                seekCommand: Int
            ): ListenableFuture<*> {
                when {
                    seekCommand == Player.COMMAND_SEEK_TO_NEXT ||
                    seekCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                    mediaItemIndex > 1 -> skipToNext()

                    seekCommand == Player.COMMAND_SEEK_TO_PREVIOUS ||
                    seekCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
                    mediaItemIndex == 0 -> skipToPrevious()

                    seekCommand == Player.COMMAND_SEEK_BACK -> {
                        val target = (_currentPositionMs.value - 10_000L).coerceAtLeast(0L)
                        seekTo(target)
                    }
                    seekCommand == Player.COMMAND_SEEK_FORWARD -> {
                        val maxDur = _durationMs.value.coerceAtLeast(0L)
                        val target = (_currentPositionMs.value + 10_000L).let {
                            if (maxDur > 0) it.coerceAtMost(maxDur) else it
                        }
                        seekTo(target)
                    }
                    else -> {
                        val target = if (positionMs == C.TIME_UNSET) 0L else positionMs.coerceAtLeast(0L)
                        seekTo(target)
                    }
                }
                return Futures.immediateVoidFuture()
            }

            override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
                _repeatMode.value = when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    else -> RepeatMode.OFF
                }
                invalidateSessionState()
                return Futures.immediateVoidFuture()
            }

            override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
                if (_isShuffleEnabled.value != shuffleModeEnabled) {
                    toggleShuffle()
                }
                return Futures.immediateVoidFuture()
            }

            override fun handleSetVolume(volume: Float): ListenableFuture<*> {
                setVolume(volume)
                return Futures.immediateVoidFuture()
            }

        override fun handleRelease(): ListenableFuture<*> {
            releaseMediaPlayer()
            return Futures.immediateVoidFuture()
        }
    }

    private val sessionPlayer: SessionPlayerImpl by lazy { SessionPlayerImpl() }

    private fun invalidateSessionState() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            sessionPlayer.notifyStateChanged()
        } else {
            mainHandler.post { sessionPlayer.notifyStateChanged() }
        }
    }

    private fun getCurrentPositionSafe(): Long {
        val mp = mediaPlayer
        if (mp != null && isMediaPlayerPrepared) {
            try {
                val pos = mp.currentPosition.toLong()
                if (pos >= 0L) {
                    _currentPositionMs.value = pos
                    return pos
                }
            } catch (_: Exception) {}
        }
        return _currentPositionMs.value.coerceAtLeast(0L)
    }

    private fun requestAudioFocus(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        PlatformAudioAttributes.Builder()
                            .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener, mainHandler)
                    .build()
                    .also { audioFocusRequest = it }
                val res = audioManager.requestAudioFocus(req)
                res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED || res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED
            } else {
                @Suppress("DEPRECATION")
                val res = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
                res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.w(tag, "Audio focus request warning: ${e.message}")
            true
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (_: Exception) {}
    }

    fun ensureServiceStarted() {
        try {
            val intent = Intent(appContext, MusicPlaybackService::class.java)
            appContext.startService(intent)
        } catch (e: Exception) {
            Log.w(tag, "Service start deferred: ${e.message}")
        }
        try {
            if (mediaControllerFuture == null) {
                val sessionToken = SessionToken(
                    appContext,
                    ComponentName(appContext, MusicPlaybackService::class.java)
                )
                mediaControllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
            }
        } catch (e: Exception) {
            Log.w(tag, "MediaController connection deferred: ${e.message}")
        }
    }

    private fun acquireWifiLock() {
        try {
            if (wifiLock == null) {
                val wm = appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wm != null) {
                    val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                    } else {
                        @Suppress("DEPRECATION")
                        WifiManager.WIFI_MODE_FULL_HIGH_PERF
                    }
                    wifiLock = wm.createWifiLock(lockMode, "SMusic:PlaybackWifiLock").apply {
                        setReferenceCounted(false)
                    }
                }
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {}
    }

    private fun releaseWifiLock() {
        try {
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {}
    }

    private var likeStateJob: Job? = null

    fun toggleCurrentTrackLike() {
        val current = _currentTrack.value ?: return
        scope.launch(Dispatchers.IO) {
            try {
                repository.toggleLike(current)
            } catch (_: Exception) {}
        }
    }

    fun refreshCurrentTrackLikeState(trackId: String) {
        likeStateJob?.cancel()
        likeStateJob = scope.launch(Dispatchers.IO) {
            try {
                repository.isSongLiked(trackId).collect { isLiked ->
                    _isCurrentTrackLiked.value = isLiked
                }
            } catch (_: Exception) {}
        }
    }

    private val imageLoader by lazy { ImageLoader(appContext) }

    fun getOrCreatePlayer(): Player {
        return sessionPlayer
    }

    fun attachPlayer(player: Player, service: MusicPlaybackService) {
        serviceContext = service
        invalidateSessionState()
    }

    private fun releaseMediaPlayer() {
        stopProgressTracker()
        isMediaPlayerPrepared = false
        val mp = mediaPlayer
        mediaPlayer = null
        if (mp != null) {
            try {
                mp.setOnPreparedListener(null)
                mp.setOnCompletionListener(null)
                mp.setOnErrorListener(null)
                mp.setOnInfoListener(null)
                mp.reset()
            } catch (_: Exception) {}
            try {
                mp.release()
            } catch (_: Exception) {}
        }
    }

    private fun stopPlaybackInternal() {
        playWhenReadyRequested = false
        _isPlaying.value = false
        _isBuffering.value = false
        currentPlaybackState = Player.STATE_IDLE
        releaseMediaPlayer()
        releaseWifiLock()
        abandonAudioFocus()
        invalidateSessionState()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (isMediaPlayerPrepared) {
                    val mp = mediaPlayer
                    if (mp != null) {
                        try {
                            _currentPositionMs.value = mp.currentPosition.toLong().coerceAtLeast(0L)
                            val dur = mp.duration.toLong()
                            if (dur > 0) {
                                _durationMs.value = dur
                            }
                        } catch (_: Exception) {}
                    }
                }
                delay(400)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        getCurrentPositionSafe()
    }

    fun playTrack(track: PlayableTrack, newQueue: List<PlayableTrack> = emptyList()) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            _errorMessage.value = null

            val currentQ = if (newQueue.isNotEmpty()) {
                newQueue
            } else {
                val existing = _queue.value.toMutableList()
                if (!existing.any { it.id == track.id }) {
                    existing.add(track)
                }
                existing
            }

            _queue.value = currentQ
            val idx = currentQ.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
            _currentIndex.value = idx

            startPlaybackForTrack(track)
        }
    }

    private suspend fun startPlaybackForTrack(track: PlayableTrack) {
        _currentTrack.value = track
        _isBuffering.value = true
        _isPlaying.value = false
        playWhenReadyRequested = true
        currentPlaybackState = Player.STATE_BUFFERING
        _currentPositionMs.value = 0L
        _durationMs.value = if (track.duration > 0) track.duration * 1000L else 0L

        // Record recently played asynchronously
        scope.launch(Dispatchers.IO) {
            try {
                repository.recordRecentlyPlayed(track)
            } catch (_: Exception) {}
        }

        // Check if track is downloaded locally for offline playback
        var resolvedUrl = ""
        var localArtwork = track.artwork

        if (track.localFilePath.isNotBlank() && File(track.localFilePath).exists()) {
            resolvedUrl = Uri.fromFile(File(track.localFilePath)).toString()
        } else if (track.streamUrl.startsWith("file://") || track.streamUrl.startsWith("content://")) {
            resolvedUrl = track.streamUrl
        } else {
            val downloaded = repository.getDownloadedSong(track.id)
            if (downloaded != null && downloaded.localFilePath.isNotBlank() && File(downloaded.localFilePath).exists()) {
                resolvedUrl = Uri.fromFile(File(downloaded.localFilePath)).toString()
                if (downloaded.localArtworkPath.isNotBlank()) {
                    localArtwork = downloaded.localArtworkPath
                }
            }
        }

        // If not downloaded, resolve online stream URL
        if (resolvedUrl.isBlank()) {
            resolvedUrl = repository.resolveStreamUrl(track) ?: ""
        }

        if (resolvedUrl.isBlank()) {
            _isBuffering.value = false
            playWhenReadyRequested = false
            currentPlaybackState = Player.STATE_IDLE
            _errorMessage.value = "Unable to resolve stream URL for ${track.title}. Retry?"
            invalidateSessionState()
            return
        }

        val updatedTrack = track.copy(streamUrl = resolvedUrl, artwork = localArtwork)
        _currentTrack.value = updatedTrack
        currentArtworkBytes = null
        refreshCurrentTrackLikeState(updatedTrack.id)

        ensureServiceStarted()
        updateMediaItemMetadata(updatedTrack, localArtwork, null)

        // Load artwork bytes asynchronously so playback starts without waiting on image download
        artworkJob?.cancel()
        if (localArtwork.isNotBlank()) {
            artworkJob = scope.launch {
                val bytes = loadArtworkBytes(localArtwork)
                if (bytes != null && bytes.isNotEmpty() && _currentTrack.value?.id == updatedTrack.id) {
                    currentArtworkBytes = bytes
                    updateMediaItemMetadata(updatedTrack, localArtwork, bytes)
                }
            }
        }

        prepareAndPlayUrl(updatedTrack, resolvedUrl)

        // Proactively prefetch auto-recommendations if queue is short (<= 2 items)
        checkAndPreloadRecommendations(updatedTrack.id, updatedTrack)
    }

    private fun updateMediaItemMetadata(
        track: PlayableTrack,
        artworkUriStr: String,
        artworkBytes: ByteArray?
    ) {
        val durMs = _durationMs.value
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setDisplayTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setAlbumArtist(track.artist)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setArtworkUri(if (artworkUriStr.isNotBlank()) Uri.parse(artworkUriStr) else null)
            .apply {
                if (durMs > 0L) {
                    setDurationMs(durMs)
                }
                if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                    setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            }
            .build()

        activeMediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.streamUrl)
            .setMediaMetadata(mediaMetadata)
            .build()

        invalidateSessionState()
    }

    private fun prepareAndPlayUrl(track: PlayableTrack, url: String) {
        releaseMediaPlayer()
        requestAudioFocus()
        acquireWifiLock()

        _isBuffering.value = true
        playWhenReadyRequested = true
        currentPlaybackState = Player.STATE_BUFFERING
        invalidateSessionState()

        val mp = MediaPlayer()
        mediaPlayer = mp

        try {
            try {
                mp.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
            } catch (_: Exception) {}

            mp.setAudioAttributes(
                PlatformAudioAttributes.Builder()
                    .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
                    .build()
            )

            try {
                val targetSessionId = equalizerManager.getOrGenerateAudioSessionId()
                if (targetSessionId > 0) {
                    mp.audioSessionId = targetSessionId
                }
            } catch (_: Exception) {}

            setMediaPlayerDataSource(mp, url)

            mp.setOnPreparedListener { preparedMp ->
                if (mediaPlayer !== preparedMp) return@setOnPreparedListener
                isMediaPlayerPrepared = true
                _isBuffering.value = false
                _errorMessage.value = null
                currentPlaybackState = Player.STATE_READY

                try {
                    equalizerManager.bindToAudioSession(preparedMp.audioSessionId)
                } catch (_: Exception) {}

                val dur = try { preparedMp.duration.toLong() } catch (_: Exception) { 0L }
                if (dur > 0) {
                    _durationMs.value = dur
                }

                val vol = _volume.value
                try {
                    preparedMp.setVolume(vol, vol)
                } catch (_: Exception) {}

                if (playWhenReadyRequested) {
                    try {
                        preparedMp.start()
                        _isPlaying.value = true
                        startProgressTracker()
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to start MediaPlayer: ${e.message}")
                    }
                }
                updateMediaItemMetadata(track, track.artwork, currentArtworkBytes)
            }

            mp.setOnCompletionListener { completedMp ->
                if (mediaPlayer !== completedMp) return@setOnCompletionListener
                _isBuffering.value = false
                _isPlaying.value = false
                stopProgressTracker()
                currentPlaybackState = Player.STATE_ENDED
                invalidateSessionState()
                handleTrackEnded()
            }

            mp.setOnInfoListener { infoMp, what, _ ->
                if (mediaPlayer !== infoMp) return@setOnInfoListener false
                when (what) {
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        _isBuffering.value = true
                        currentPlaybackState = Player.STATE_BUFFERING
                        invalidateSessionState()
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                        _isBuffering.value = false
                        if (isMediaPlayerPrepared) {
                            currentPlaybackState = Player.STATE_READY
                        }
                        invalidateSessionState()
                    }
                }
                false
            }

            mp.setOnErrorListener { errorMp, what, extra ->
                if (mediaPlayer !== errorMp) return@setOnErrorListener true
                // Ignore -38 state notifications if triggered during reset
                if (what == -38) return@setOnErrorListener true
                Log.w(tag, "MediaPlayer fallback triggered (what=$what, extra=$extra) for ${track.title}")
                _isBuffering.value = false
                _isPlaying.value = false
                isMediaPlayerPrepared = false
                stopProgressTracker()
                handlePlaybackError()
                true
            }

            mp.prepareAsync()
        } catch (e: Exception) {
            Log.w(tag, "Error configuring stream URL: ${e.message}")
            _isBuffering.value = false
            _isPlaying.value = false
            isMediaPlayerPrepared = false
            handlePlaybackError()
        }
    }

    private fun setMediaPlayerDataSource(mp: MediaPlayer, url: String) {
        when {
            url.startsWith("file://") -> {
                val path = Uri.parse(url).path
                if (!path.isNullOrBlank() && File(path).exists()) {
                    FileInputStream(File(path)).use { fis ->
                        mp.setDataSource(fis.fd)
                    }
                } else {
                    mp.setDataSource(appContext, Uri.parse(url))
                }
            }
            url.startsWith("/") && File(url).exists() -> {
                FileInputStream(File(url)).use { fis ->
                    mp.setDataSource(fis.fd)
                }
            }
            url.startsWith("content://") -> {
                mp.setDataSource(appContext, Uri.parse(url))
            }
            else -> {
                val headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                mp.setDataSource(appContext, Uri.parse(url), headers)
            }
        }
    }

    fun startRadio(track: PlayableTrack) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            _isAutoplayEnabled.value = true
            _queue.value = listOf(track)
            _currentIndex.value = 0
            startPlaybackForTrack(track)

            // Immediately fetch high-quality radio mix recommendations for this seed song
            loadMoreRecommendations(seedTrack = track)
        }
    }

    fun loadMoreRecommendations(seedTrack: PlayableTrack? = null) {
        val target = seedTrack ?: _currentTrack.value ?: return
        if (isFetchingReco) return
        isFetchingReco = true
        scope.launch(Dispatchers.IO) {
            try {
                val recos = repository.getRecommendations(target.id, target)
                if (recos.isNotEmpty()) {
                    val currentQ = _queue.value
                    val newTracks = recos.map { it.toPlayableTrack() }.filter { recoTrack ->
                        currentQ.none { it.id == recoTrack.id }
                    }
                    if (newTracks.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _queue.value = _queue.value + newTracks
                            Log.d(tag, "RadioMixEngine: Added ${newTracks.size} recommended tracks. Total queue: ${_queue.value.size}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to load radio recommendations: ${e.message}")
            } finally {
                isFetchingReco = false
            }
        }
    }

    private fun checkAndPreloadRecommendations(songId: String, currentTrack: PlayableTrack? = null) {
        if (!_isAutoplayEnabled.value || isFetchingReco || songId.isBlank()) return

        val remaining = _queue.value.size - (_currentIndex.value + 1)
        if (remaining <= 2) {
            isFetchingReco = true
            scope.launch(Dispatchers.IO) {
                try {
                    val recos = repository.getRecommendations(songId, currentTrack)
                    if (recos.isNotEmpty()) {
                        val currentQ = _queue.value
                        val newTracks = recos.map { it.toPlayableTrack() }.filter { recoTrack ->
                            currentQ.none { it.id == recoTrack.id }
                        }
                        if (newTracks.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                _queue.value = _queue.value + newTracks
                                Log.d(tag, "AutoPlayEngine: Appended ${newTracks.size} recommendations to queue. Total items: ${_queue.value.size}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "AutoPlay recommendation prefetch error: ${e.message}")
                } finally {
                    isFetchingReco = false
                }
            }
        }
    }

    private suspend fun loadArtworkBytes(artworkUrlOrPath: String): ByteArray? = withContext(Dispatchers.IO) {
        if (artworkUrlOrPath.isBlank()) return@withContext null
        try {
            val request = ImageRequest.Builder(appContext)
                .data(artworkUrlOrPath)
                .size(512, 512)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            val drawable = result.drawable
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                return@withContext stream.toByteArray()
            }
        } catch (e: Exception) {
            Log.w(tag, "Artwork byte fetch for notification failed: ${e.message}")
        }
        null
    }

    private fun handlePlaybackError() {
        val current = _currentTrack.value ?: return
        val currentUrl = current.streamUrl

        // Try fallback quality
        val fallbackUrl = StreamUrlResolver.getFallbackUrl(currentUrl)
        if (fallbackUrl != null) {
            Log.d(tag, "Retrying with fallback stream quality: $fallbackUrl")
            val fallbackTrack = current.copy(streamUrl = fallbackUrl)
            _currentTrack.value = fallbackTrack
            updateMediaItemMetadata(fallbackTrack, fallbackTrack.artwork, null)
            prepareAndPlayUrl(fallbackTrack, fallbackUrl)
        } else if (current.mediaPreviewUrl.isNotBlank() && currentUrl != current.mediaPreviewUrl) {
            Log.d(tag, "Retrying with media preview URL: ${current.mediaPreviewUrl}")
            val previewUrl = current.mediaPreviewUrl.replace("http://", "https://")
            val previewTrack = current.copy(streamUrl = previewUrl)
            _currentTrack.value = previewTrack
            updateMediaItemMetadata(previewTrack, previewTrack.artwork, null)
            prepareAndPlayUrl(previewTrack, previewUrl)
        } else {
            currentPlaybackState = Player.STATE_IDLE
            playWhenReadyRequested = false
            _errorMessage.value = "Unable to play this song. Tap Retry."
            invalidateSessionState()
        }
    }

    fun retryCurrentTrack() {
        val current = _currentTrack.value ?: return
        playbackJob?.cancel()
        playbackJob = scope.launch {
            startPlaybackForTrack(current)
        }
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                resume()
            }
            RepeatMode.ALL -> {
                skipToNext()
            }
            RepeatMode.OFF -> {
                val q = _queue.value
                val nextIdx = _currentIndex.value + 1
                if (nextIdx < q.size) {
                    skipToNext()
                } else if (_isAutoplayEnabled.value) {
                    // Trigger Autoplay recommendations!
                    triggerAutoplay()
                } else {
                    _isPlaying.value = false
                    playWhenReadyRequested = false
                    invalidateSessionState()
                }
            }
        }
    }

    private fun triggerAutoplay() {
        val current = _currentTrack.value ?: return
        scope.launch {
            _isBuffering.value = true
            invalidateSessionState()
            val recos = repository.getRecommendations(current.id, current)
            if (recos.isNotEmpty()) {
                val newTracks = recos.map { it.toPlayableTrack() }
                val updatedQueue = _queue.value + newTracks
                _queue.value = updatedQueue
                skipToNext()
            } else {
                _isPlaying.value = false
                _isBuffering.value = false
                playWhenReadyRequested = false
                invalidateSessionState()
            }
        }
    }

    fun playPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        playWhenReadyRequested = false
        _isPlaying.value = false
        stopProgressTracker()
        releaseWifiLock()
        val mp = mediaPlayer
        if (mp != null && isMediaPlayerPrepared) {
            try {
                if (mp.isPlaying) {
                    mp.pause()
                }
            } catch (_: Exception) {}
        }
        invalidateSessionState()
    }

    fun resume() {
        ensureServiceStarted()
        val current = _currentTrack.value ?: return
        val mp = mediaPlayer
        if (mp != null && isMediaPlayerPrepared) {
            requestAudioFocus()
            acquireWifiLock()
            playWhenReadyRequested = true
            try {
                mp.start()
                _isPlaying.value = true
                currentPlaybackState = Player.STATE_READY
                startProgressTracker()
            } catch (e: Exception) {
                Log.w(tag, "Resume failed, retrying track: ${e.message}")
                retryCurrentTrack()
            }
            invalidateSessionState()
        } else {
            retryCurrentTrack()
        }
    }

    fun seekTo(positionMs: Long) {
        val clampedPos = positionMs.coerceAtLeast(0L)
        _currentPositionMs.value = clampedPos
        val mp = mediaPlayer
        if (mp != null && isMediaPlayerPrepared) {
            try {
                mp.seekTo(clampedPos.toInt())
            } catch (_: Exception) {}
        }
        invalidateSessionState()
    }

    fun skipToNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        var nextIdx = _currentIndex.value + 1
        if (nextIdx >= q.size) {
            if (_repeatMode.value == RepeatMode.ALL) {
                nextIdx = 0
            } else if (_isAutoplayEnabled.value) {
                triggerAutoplay()
                return
            } else {
                return
            }
        }
        _currentIndex.value = nextIdx
        val track = q[nextIdx]
        playbackJob?.cancel()
        playbackJob = scope.launch {
            startPlaybackForTrack(track)
        }
    }

    fun skipToPrevious() {
        if (getCurrentPositionSafe() > 3000L) {
            seekTo(0)
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return
        var prevIdx = _currentIndex.value - 1
        if (prevIdx < 0) {
            prevIdx = if (_repeatMode.value == RepeatMode.ALL) q.size - 1 else 0
        }
        _currentIndex.value = prevIdx
        val track = q[prevIdx]
        playbackJob?.cancel()
        playbackJob = scope.launch {
            startPlaybackForTrack(track)
        }
    }

    fun toggleRepeat() {
        val next = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = next
        syncPlayerModes()
    }

    fun toggleShuffle() {
        val enabled = !_isShuffleEnabled.value
        _isShuffleEnabled.value = enabled
        val q = _queue.value
        val curTrack = _currentTrack.value

        if (enabled) {
            originalQueueBeforeShuffle = q
            if (curTrack != null) {
                val others = q.filter { it.id != curTrack.id }.shuffled()
                _queue.value = listOf(curTrack) + others
                _currentIndex.value = 0
            } else {
                _queue.value = q.shuffled()
            }
        } else {
            if (originalQueueBeforeShuffle.isNotEmpty()) {
                _queue.value = originalQueueBeforeShuffle
                curTrack?.let { ct ->
                    _currentIndex.value = originalQueueBeforeShuffle.indexOfFirst { it.id == ct.id }.coerceAtLeast(0)
                }
            }
        }
        syncPlayerModes()
    }

    private fun syncPlayerModes() {
        invalidateSessionState()
    }

    fun addToQueue(track: PlayableTrack) {
        val updated = _queue.value.toMutableList()
        updated.add(track)
        _queue.value = updated
    }

    fun playNext(track: PlayableTrack) {
        val updated = _queue.value.toMutableList()
        val insertIdx = (_currentIndex.value + 1).coerceIn(0, updated.size)
        updated.add(insertIdx, track)
        _queue.value = updated
    }

    fun removeFromQueue(index: Int) {
        val updated = _queue.value.toMutableList()
        if (index in updated.indices) {
            val isCurrent = index == _currentIndex.value
            updated.removeAt(index)
            _queue.value = updated
            if (isCurrent) {
                if (updated.isNotEmpty()) {
                    val nextIdx = index.coerceAtMost(updated.size - 1)
                    _currentIndex.value = nextIdx
                    playbackJob?.cancel()
                    playbackJob = scope.launch { startPlaybackForTrack(updated[nextIdx]) }
                } else {
                    _currentTrack.value = null
                    activeMediaItem = null
                    stopPlaybackInternal()
                }
            } else if (index < _currentIndex.value) {
                _currentIndex.value = _currentIndex.value - 1
            }
        }
    }

    fun clearQueue() {
        _queue.value = emptyList()
        _currentIndex.value = -1
        _currentTrack.value = null
        activeMediaItem = null
        stopPlaybackInternal()
    }

    fun moveQueueItem(from: Int, to: Int) {
        val updated = _queue.value.toMutableList()
        if (from in updated.indices && to in updated.indices) {
            val item = updated.removeAt(from)
            updated.add(to, item)
            _queue.value = updated
            val cur = _currentTrack.value
            if (cur != null) {
                _currentIndex.value = updated.indexOfFirst { it.id == cur.id }
            }
        }
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        _isAutoplayEnabled.value = enabled
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        try {
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (_: Exception) {}
        invalidateSessionState()
    }

    companion object {
        @Volatile
        private var INSTANCE: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PlayerManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
