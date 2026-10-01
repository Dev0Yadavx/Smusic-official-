package com.example.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.model.PlayableTrack
import com.example.data.remote.NetworkResult
import com.example.player.PlayerManager
import com.example.player.RepeatMode
import com.example.repository.MusicRepository
import com.example.ui.common.ThreeLineVisualizer
import com.example.ui.settings.EqualizerScreen
import com.example.ui.theme.AppFontFamily
import com.example.ui.theme.AppIcons
import com.example.ui.theme.MiniPlayerScallopedShape
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * Dynamic Palette extracted from Song Artwork
 */
data class DynamicSongColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val surfaceContainer: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val glowAccent: Color
)

@Composable
fun rememberDynamicSongColors(
    artworkUrl: String?,
    seedKey: String,
    isDark: Boolean
): DynamicSongColors {
    val context = LocalContext.current

    // Generate fallback base colors from track seed
    val defaultHue = remember(seedKey) {
        val hash = seedKey.hashCode()
        kotlin.math.abs(hash % 360).toFloat()
    }

    var extractedPrimary by remember(artworkUrl) {
        mutableStateOf<Color?>(null)
    }
    var extractedSecondary by remember(artworkUrl) {
        mutableStateOf<Color?>(null)
    }

    LaunchedEffect(artworkUrl) {
        if (artworkUrl.isNullOrBlank()) {
            extractedPrimary = null
            extractedSecondary = null
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.drawable.toBitmap(96, 96, Bitmap.Config.ARGB_8888)
                    val sampledColors = mutableListOf<Int>()
                    val step = 8
                    for (x in 0 until bitmap.width step step) {
                        for (y in 0 until bitmap.height step step) {
                            val pixel = bitmap.getPixel(x, y)
                            val alpha = (pixel ushr 24) and 0xff
                            if (alpha > 128) {
                                sampledColors.add(pixel)
                            }
                        }
                    }

                    if (sampledColors.isNotEmpty()) {
                        // Find vibrant saturated colors
                        val hsv = FloatArray(3)
                        val scoredColors = sampledColors.map { c ->
                            android.graphics.Color.colorToHSV(c, hsv)
                            val saturation = hsv[1]
                            val brightness = hsv[2]
                            // Score based on saturation and balanced brightness
                            val score = saturation * 2f + (if (brightness in 0.3f..0.85f) 1f else 0.2f)
                            c to score
                        }.sortedByDescending { it.second }

                        val topColorInt = scoredColors.firstOrNull()?.first
                        val secondaryColorInt = scoredColors.drop(scoredColors.size / 3).firstOrNull()?.first

                        withContext(Dispatchers.Main) {
                            topColorInt?.let { extractedPrimary = Color(it) }
                            secondaryColorInt?.let { extractedSecondary = Color(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback gracefully
            }
        }
    }

    val basePrimary = extractedPrimary ?: if (isDark) Color(0xFF6EE7B7) else Color(0xFF006C4C)
    val baseSecondary = extractedSecondary ?: if (isDark) Color(0xFF93C5FD) else Color(0xFF1E6586)

    // Smoothly animate the colors when song changes
    val animPrimary by animateColorAsState(basePrimary, animationSpec = tween(700), label = "prim")
    val animSecondary by animateColorAsState(baseSecondary, animationSpec = tween(700), label = "sec")

    val bgTop = if (isDark) {
        animPrimary.copy(alpha = 0.38f)
    } else {
        animPrimary.copy(alpha = 0.18f)
    }
    val bgBottom = if (isDark) Color(0xFF0B0E0D) else Color(0xFFF7FBF7)
    val surfContainer = if (isDark) {
        Color(0xFF181C1A).copy(alpha = 0.85f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.88f)
    }
    val onSurf = if (isDark) Color(0xFFF0F4F0) else Color(0xFF121A16)
    val onSurfVar = if (isDark) Color(0xFFA8B4AD) else Color(0xFF53635B)

    return DynamicSongColors(
        primary = animPrimary,
        secondary = animSecondary,
        tertiary = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
        backgroundTop = bgTop,
        backgroundBottom = bgBottom,
        surfaceContainer = surfContainer,
        onSurface = onSurf,
        onSurfaceVariant = onSurfVar,
        glowAccent = animPrimary.copy(alpha = 0.45f)
    )
}

/**
 * Authentic M3 Squiggly Wave Seekbar with Live Sine Wave Animation & Fluid Bar Scrubber
 */
@Composable
fun SquigglySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "squiggly")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2 * Math.PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "phase"
    )

    // Smooth wave amplitude animation when playing/paused
    val targetAmplitude = if (isPlaying) 4.2f else 0f
    val waveAmpDp by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "waveAmp"
    )

    var isDragging by remember { mutableStateOf(false) }
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "thumbScale"
    )

    val currentFraction = if (valueRange.endInclusive > valueRange.start) {
        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    } else 0f

    val density = LocalDensity.current
    val trackStrokeWidth = with(density) { 5.dp.toPx() }
    val waveLength = with(density) { 34.dp.toPx() }
    val waveAmplitude = with(density) { waveAmpDp.dp.toPx() }

    // Vertical bar scrubber thumb ("|" shape)
    val barWidth = with(density) { (4.5.dp * thumbScale).toPx() }
    val barHeight = with(density) { (22.dp * thumbScale).toPx() }
    val barCorner = with(density) { 2.5.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(valueRange) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                        tryAwaitRelease()
                        isDragging = false
                        onValueChangeFinished()
                    }
                )
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished()
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val newFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                        onValueChange(newValue)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            val centerY = size.height / 2f
            val startX = 0f
            val endX = size.width
            val currentProgressX = startX + (endX - startX) * currentFraction

            // 1. Inactive Track (Unplayed area)
            if (currentProgressX < endX) {
                drawLine(
                    color = inactiveColor,
                    start = Offset(currentProgressX, centerY),
                    end = Offset(endX, centerY),
                    strokeWidth = trackStrokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 2. Active Wave Track (Played sine wave)
            if (currentProgressX > startX) {
                val wavePath = Path()
                wavePath.moveTo(startX, centerY)

                var x = startX
                val step = 2f
                while (x <= currentProgressX) {
                    val angle = ((x - startX) / waveLength * (2 * Math.PI) + phase).toFloat()
                    val y = centerY + waveAmplitude * sin(angle)
                    wavePath.lineTo(x, y)
                    x += step
                }
                drawPath(
                    path = wavePath,
                    color = activeColor,
                    style = Stroke(width = trackStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 3. Thumb Scrubber Glow & Bar
            if (isDragging) {
                drawCircle(
                    color = thumbColor.copy(alpha = 0.22f),
                    radius = barHeight * 0.8f,
                    center = Offset(currentProgressX, centerY)
                )
            }

            // Scrubber Bar "|"
            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(currentProgressX - barWidth / 2f, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barCorner, barCorner)
            )
        }
    }
}

/**
 * MiniPlayer docked seamlessly with bottom navigation
 */
@Composable
fun MiniPlayer(
    playerManager: PlayerManager,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    repository: MusicRepository? = null,
    backgroundGraphicsLayer: GraphicsLayer? = null
) {
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val currentPosMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val scope = rememberCoroutineScope()

    val track = currentTrack ?: return

    val progress = if (durationMs > 0) {
        (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    var isLiked by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "mini_artwork_running_spin")
    val runningRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "mini_artwork_spin_angle"
    )
    val artworkRotation = if (isPlaying) runningRotation else 0f

    LaunchedEffect(track.id, repository) {
        if (repository != null) {
            repository.isSongLiked(track.id).collect { liked ->
                isLiked = liked
            }
        }
    }

    // Horizontal swipe gesture for skipping tracks
    var dragOffsetX by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .pointerInput(track.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragOffsetX = 0f },
                    onDragEnd = {
                        if (dragOffsetX < -80f) {
                            playerManager.skipToNext()
                        } else if (dragOffsetX > 80f) {
                            playerManager.skipToPrevious()
                        }
                        dragOffsetX = 0f
                    },
                    onDragCancel = { dragOffsetX = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragOffsetX += dragAmount
                    }
                )
            }
            .clickable(onClick = onClick)
            .testTag("mini_player")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Full Rounded Album Art with Circular Progress Ring & Spin Animation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    strokeWidth = 2.5.dp
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .rotate(artworkRotation)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = track.artwork,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist with Marquee
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track.title,
                    fontFamily = AppFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = track.artist,
                    fontFamily = AppFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = AppFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Like / Favorite Button
            if (repository != null) {
                val heartScale by animateFloatAsState(
                    targetValue = if (isLiked) 1.2f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "mini_like_scale"
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            repository.toggleLike(track)
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .scale(heartScale)
                        .testTag("mini_player_like")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Play / Pause Button in Full Rounded Circle Container
            Surface(
                onClick = { playerManager.playPause() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("mini_player_play_pause")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Next Track Button
            Surface(
                onClick = { playerManager.skipToNext() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f),
                modifier = Modifier
                    .size(38.dp)
                    .testTag("mini_player_next")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full Screen Now Playing Screen with M3 Expressive Dynamic Song Color Gradient,
 * Squiggly Wave Seekbar, Wave Like & Download buttons, M3 Morphing Play/Pause Shape,
 * and Niche Bottom Controls (Queue, Lyrics, Connected Device).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingModal(
    playerManager: PlayerManager,
    repository: MusicRepository,
    onDismiss: () -> Unit,
    onViewAlbum: (String) -> Unit,
    onViewArtist: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val track by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val currentPosMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val isShuffle by playerManager.isShuffleEnabled.collectAsState()
    val volume by playerManager.volume.collectAsState()
    val queue by playerManager.queue.collectAsState()
    val currentIndex by playerManager.currentIndex.collectAsState()
    val isAutoplayEnabled by playerManager.isAutoplayEnabled.collectAsState()
    val errorMessage by playerManager.errorMessage.collectAsState()

    var showQueueSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showCreditsSheet by remember { mutableStateOf(false) }
    var showMoreOptionsSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showDeviceDialog by remember { mutableStateOf(false) }
    var isDownloaded by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val downloadManager = remember { com.example.download.SongDownloadManager.getInstance(context) }
    val activeDownloads by downloadManager.activeDownloads.collectAsState()

    LaunchedEffect(track?.id) {
        track?.let { t ->
            launch {
                repository.isSongLiked(t.id).collect { liked ->
                    isLiked = liked
                }
            }
            launch {
                repository.isSongDownloaded(t.id).collect { dl ->
                    isDownloaded = dl
                }
            }
        }
    }

    val currentT = track ?: return

    val activeTask = activeDownloads[currentT.id]
    val isDownloading = activeTask?.status == com.example.download.DownloadStatus.DOWNLOADING || activeTask?.status == com.example.download.DownloadStatus.QUEUED
    val downloadProgress = activeTask?.progress ?: 0

    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPositionMs by remember { mutableStateOf(0f) }

    val displayPositionMs = if (isUserDraggingSlider) sliderDragPositionMs.toLong() else currentPosMs
    val totalDurationMs = if (durationMs > 0) durationMs else (currentT.duration * 1000)

    val formatTime = { ms: Long ->
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        "%d:%02d".format(m, s)
    }

    fun shareCurrentTrack() {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "🎵 Listening to \"${currentT.title}\" by ${currentT.artist} on SMusic!"
                )
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Track")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share track: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Dynamic Song Colors extracted from album artwork
    val dynamicColors = rememberDynamicSongColors(
        artworkUrl = currentT.artwork,
        seedKey = "${currentT.id}_${currentT.title}",
        isDark = isDark
    )

    // Animated like button bounce
    var likeAnimateTrigger by remember { mutableStateOf(false) }
    val likeScale by animateFloatAsState(
        targetValue = if (likeAnimateTrigger) 1.35f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        finishedListener = { likeAnimateTrigger = false },
        label = "likeScale"
    )

    val themeManager = remember { ThemeManager.getInstance(context) }
    val nowPlayingStyle by themeManager.nowPlayingStyle.collectAsState()
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    var dragOffsetY by remember { mutableStateOf(0f) }
    val animatedOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dragOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { androidx.compose.ui.unit.IntOffset(0, animatedOffsetY.toInt().coerceAtLeast(0)) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragOffsetY > 140f) {
                            onDismiss()
                        } else {
                            dragOffsetY = 0f
                        }
                    },
                    onDragCancel = {
                        dragOffsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        if (dragAmount.y > 0 || dragOffsetY > 0) {
                            dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtLeast(0f)
                            change.consume()
                        }
                    }
                )
            }
    ) {
        ImmersivePosterNowPlayingLayout(
            track = currentT,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            displayPositionMs = displayPositionMs,
            totalDurationMs = totalDurationMs,
            formatTime = formatTime,
            isLiked = isLiked,
            likeScale = likeScale,
            errorMessage = errorMessage,
            dynamicColors = dynamicColors,
            onToggleLike = {
                likeAnimateTrigger = true
                scope.launch { repository.toggleLike(currentT) }
            },
            onMoreOptions = { showMoreOptionsSheet = true },
            onSeek = { pos ->
                isUserDraggingSlider = true
                sliderDragPositionMs = pos
            },
            onSeekFinished = {
                playerManager.seekTo(sliderDragPositionMs.toLong())
                isUserDraggingSlider = false
            },
            onPrevious = { playerManager.skipToPrevious() },
            onPlayPause = { playerManager.playPause() },
            onNext = { playerManager.skipToNext() },
            onRetry = { playerManager.retryCurrentTrack() },
            onOpenQueue = { showQueueSheet = true },
            onOpenLyrics = { showLyricsSheet = true },
            onOpenDeviceSelector = { showDeviceDialog = true }
        )
    }

    // Bottom Sheet: Queue (Up Next)
    if (showQueueSheet) {
        QueueBottomSheet(
            queue = queue,
            currentIndex = currentIndex,
            isPlaying = isPlaying,
            isAutoplayEnabled = isAutoplayEnabled,
            onToggleAutoplay = { playerManager.setAutoplayEnabled(!isAutoplayEnabled) },
            onLoadMoreRecommendations = {
                playerManager.loadMoreRecommendations()
                Toast.makeText(context, "Loading more recommended songs...", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showQueueSheet = false },
            onSelectTrack = { index ->
                playerManager.playTrack(queue[index], queue)
                showQueueSheet = false
            },
            onRemoveTrack = { playerManager.removeFromQueue(it) },
            onClearQueue = {
                playerManager.clearQueue()
                showQueueSheet = false
            }
        )
    }

    // Bottom Sheet: Lyrics
    if (showLyricsSheet) {
        LyricsBottomSheet(
            track = currentT,
            repository = repository,
            onDismiss = { showLyricsSheet = false }
        )
    }

    // Bottom Sheet: Song Credits
    if (showCreditsSheet) {
        SongCreditsBottomSheet(
            track = currentT,
            repository = repository,
            onViewArtist = { artistName ->
                showCreditsSheet = false
                onDismiss()
                onViewArtist(artistName)
            },
            onViewAlbum = { albumId ->
                showCreditsSheet = false
                onDismiss()
                onViewAlbum(albumId)
            },
            onDismiss = { showCreditsSheet = false }
        )
    }

    // Bottom Sheet: 3-Dot More Options
    var showEqualizerModal by remember { mutableStateOf(false) }

    if (showEqualizerModal) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            EqualizerScreen(
                onBack = { showEqualizerModal = false },
                equalizerManager = playerManager.equalizerManager
            )
        }
    }

    if (showMoreOptionsSheet) {
        SongOptionsBottomSheet(
            track = currentT,
            isLiked = isLiked,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            volume = volume,
            onVolumeChange = { playerManager.setVolume(it) },
            onToggleLike = {
                scope.launch { repository.toggleLike(currentT) }
                showMoreOptionsSheet = false
            },
            onToggleShuffle = {
                playerManager.toggleShuffle()
            },
            onToggleRepeat = {
                playerManager.toggleRepeat()
            },
            onShowQueue = {
                showMoreOptionsSheet = false
                showQueueSheet = true
            },
            onShowLyrics = {
                showMoreOptionsSheet = false
                showLyricsSheet = true
            },
            onShowCredits = {
                showMoreOptionsSheet = false
                showCreditsSheet = true
            },
            onOpenEqualizer = {
                showMoreOptionsSheet = false
                showEqualizerModal = true
            },
            onDownload = {
                if (isDownloading) {
                    downloadManager.cancelDownload(currentT.id)
                    Toast.makeText(context, "Download cancelled", Toast.LENGTH_SHORT).show()
                } else if (isDownloaded) {
                    downloadManager.deleteDownloadedSong(currentT.id)
                    Toast.makeText(context, "Song deleted from downloads", Toast.LENGTH_SHORT).show()
                } else {
                    downloadManager.startDownload(currentT, "320")
                    Toast.makeText(context, "Downloading ${currentT.title}...", Toast.LENGTH_SHORT).show()
                }
            },
            onStartRadio = {
                showMoreOptionsSheet = false
                playerManager.startRadio(currentT)
                Toast.makeText(context, "Playing Radio Mix for ${currentT.title}", Toast.LENGTH_SHORT).show()
            },
            onAddToPlaylist = {
                showMoreOptionsSheet = false
                showAddToPlaylistDialog = true
            },
            onViewAlbum = {
                showMoreOptionsSheet = false
                if (currentT.albumId.isNotBlank()) onViewAlbum(currentT.albumId)
            },
            onShare = {
                showMoreOptionsSheet = false
                shareCurrentTrack()
            },
            onDismiss = { showMoreOptionsSheet = false }
        )
    }

    // Dialog: Add to Playlist
    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            track = currentT,
            repository = repository,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }

    // Dialog: Connected Device Selector (M3 Style Audio Device Switcher)
    if (showDeviceDialog) {
        DeviceConnectDialog(
            onDismiss = { showDeviceDialog = false }
        )
    }

    // Dialog: Sleep Timer
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onSetTimerMinutes = { mins ->
                Toast.makeText(context, if (mins > 0) "Sleep timer set for $mins mins" else "Playback will stop at end of track", Toast.LENGTH_SHORT).show()
                if (mins > 0) {
                    scope.launch {
                        kotlinx.coroutines.delay(mins * 60 * 1000L)
                        playerManager.pause()
                    }
                }
            },
            onCancelTimer = {
                Toast.makeText(context, "Sleep timer cancelled", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

/**
 * Real-time Active Audio Output Device State & Listener
 * Monitors connected Bluetooth, Wired Headphones, USB DACs, Cast/HDMI, or Speaker in real time.
 */
data class ActiveAudioDeviceUi(
    val name: String,
    val icon: ImageVector,
    val isExternalConnected: Boolean
)

@Composable
fun rememberActiveAudioOutputDevice(): ActiveAudioDeviceUi {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    fun queryCurrentDevice(): ActiveAudioDeviceUi {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val btDevice = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                        it.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                    ))
        }
        if (btDevice != null) {
            val productName = btDevice.productName?.toString()?.trim()
            val label = if (!productName.isNullOrEmpty() && !productName.equals(Build.MODEL, ignoreCase = true)) {
                productName
            } else {
                "Bluetooth"
            }
            return ActiveAudioDeviceUi(
                name = label,
                icon = Icons.Rounded.BluetoothAudio,
                isExternalConnected = true
            )
        }

        val wiredDevice = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        }
        if (wiredDevice != null) {
            val productName = wiredDevice.productName?.toString()?.trim()
            val label = if (!productName.isNullOrEmpty() && !productName.equals(Build.MODEL, ignoreCase = true)) {
                productName
            } else {
                "Headphones"
            }
            return ActiveAudioDeviceUi(
                name = label,
                icon = Icons.Rounded.Headphones,
                isExternalConnected = true
            )
        }

        val usbDevice = devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        }
        if (usbDevice != null) {
            val productName = usbDevice.productName?.toString()?.trim()
            val label = if (!productName.isNullOrEmpty() && !productName.equals(Build.MODEL, ignoreCase = true)) {
                productName
            } else {
                "USB Audio"
            }
            return ActiveAudioDeviceUi(
                name = label,
                icon = Icons.Rounded.Usb,
                isExternalConnected = true
            )
        }

        return ActiveAudioDeviceUi(
            name = "Speaker",
            icon = Icons.Outlined.VolumeUp,
            isExternalConnected = false
        )
    }

    var activeDevice by remember { mutableStateOf(queryCurrentDevice()) }

    DisposableEffect(audioManager) {
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                activeDevice = queryCurrentDevice()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                activeDevice = queryCurrentDevice()
            }
        }
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        onDispose {
            audioManager.unregisterAudioDeviceCallback(callback)
        }
    }

    return activeDevice
}

/**
 * UNIFIED NOW PLAYING SCREEN
 * - No Close Arrow or Share icon in top header
 * - Tap artwork to toggle between Full-Screen Poster Cover and Spotify-Style Square Art Cover
 * - Full Bottom Mask Blur under controls
 * - Splash-Screen Style Rounded Gradient Loading Progress Seekbar
 * - Square FAB style Queue & Lyrics buttons + Real-time Connected Audio Output Device button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersivePosterNowPlayingLayout(
    track: PlayableTrack,
    isPlaying: Boolean,
    isBuffering: Boolean,
    displayPositionMs: Long,
    totalDurationMs: Long,
    formatTime: (Long) -> String,
    isLiked: Boolean,
    likeScale: Float,
    errorMessage: String?,
    dynamicColors: DynamicSongColors,
    onToggleLike: () -> Unit,
    onMoreOptions: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenDeviceSelector: () -> Unit
) {
    // Toggle between Full-Screen Poster Art Cover and Spotify-Style Square Art Cover on tap
    var isSpotifyStyleCover by remember { mutableStateOf(false) }

    // Real-time connected audio output device (Bluetooth, Headphones, USB, or Speaker)
    val activeAudioDevice = rememberActiveAudioOutputDevice()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070809))
    ) {
        // 1. Background Layer:
        // In Spotify-style mode, full background is deeply blurred ambient cover.
        // In Full Poster mode, full-bleed sharp cover is rendered in upper canvas.
        AsyncImage(
            model = track.artwork,
            contentDescription = track.title,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isSpotifyStyleCover) {
                        Modifier
                            .scale(1.25f)
                            .blur(55.dp)
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    isSpotifyStyleCover = !isSpotifyStyleCover
                }
                .testTag("now_playing_artwork_toggle"),
            contentScale = ContentScale.Crop
        )

        // 2. Smooth Progressive Mask Blur Below Seekbar ("mask style blur rudus seekbar se neche")
        // Uses full-screen aligned artwork with Offscreen DstIn vertical gradient alpha mask
        // so there is NEVER a hard horizontal line and the blur feathers in smoothly around/below the seekbar.
        AsyncImage(
            model = track.artwork,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.64f to Color.Transparent,
                                0.74f to Color.Black.copy(alpha = 0.65f),
                                0.84f to Color.Black,
                                1.0f to Color.Black
                            )
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                .blur(28.dp),
            contentScale = ContentScale.Crop
        )

        // Smooth bottom dark scrim starting gently around the track title & seekbar down to the bottom controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.56f to Color.Transparent,
                            0.68f to Color.Black.copy(alpha = 0.38f),
                            0.78f to Color.Black.copy(alpha = 0.72f),
                            0.90f to Color(0xFF07080A).copy(alpha = 0.90f),
                            1.0f to Color(0xFF050608).copy(alpha = 0.96f)
                        )
                    )
                )
        )

        // Subtle top vignette for header readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 3. Foreground UI Elements
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Close arrow & Share icon removed — Clean centered NOW PLAYING + Track title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 16.dp, end = 16.dp)
            ) {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.68f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        fontSize = 10.5.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            // Center Interactive Artwork Area:
            // Tapping switches between Full-Bleed Poster Cover and Spotify-Style Square Art Cover
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        isSpotifyStyleCover = !isSpotifyStyleCover
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSpotifyStyleCover,
                    enter = fadeIn(tween(280)) + scaleIn(initialScale = 0.86f, animationSpec = tween(320, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(220)) + scaleOut(targetScale = 0.90f, animationSpec = tween(240))
                ) {
                    // Spotify-Style Centered Square Album Cover Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .aspectRatio(1f)
                            .shadow(
                                elevation = 28.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = dynamicColors.primary.copy(alpha = 0.55f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF16191D))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        AsyncImage(
                            model = track.artwork,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Error Banner if any
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFDC2626).copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onRetry) {
                            Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Track Title & Artist Info with Like & More Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = track.title,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = AppFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        fontFamily = AppFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = AppFontFamily,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier
                            .size(48.dp)
                            .scale(likeScale)
                            .testTag("now_playing_like_button")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isLiked) Color(0xFFFF4D6D) else Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoreOptions,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "More Options",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Splash-Screen Loading Progress Style Seekbar ("seekbar jo splash screen me loading progress tha wahi use")
            Column(modifier = Modifier.fillMaxWidth()) {
                val safeDuration = totalDurationMs.toFloat().coerceAtLeast(1f)
                val progressFraction = (displayPositionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
                var barWidthPx by remember { mutableFloatStateOf(1f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .onSizeChanged { size ->
                            barWidthPx = size.width.toFloat().coerceAtLeast(1f)
                        }
                        .pointerInput(safeDuration) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / barWidthPx).coerceIn(0f, 1f)
                                onSeek(fraction * safeDuration)
                                onSeekFinished()
                            }
                        }
                        .pointerInput(safeDuration) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    val fraction = (offset.x / barWidthPx).coerceIn(0f, 1f)
                                    onSeek(fraction * safeDuration)
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    val fraction = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                                    onSeek(fraction * safeDuration)
                                },
                                onDragEnd = {
                                    onSeekFinished()
                                },
                                onDragCancel = {
                                    onSeekFinished()
                                }
                            )
                        }
                        .testTag("now_playing_splash_seekbar"),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Pure White Splash-Screen Style Seekbar ("seekbar white color no any color")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.24f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }

                // Time Row: Elapsed, Audio format badge (WEBM HD / AAC 320), Remaining Time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(displayPositionMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )

                    // Audio Stream Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (track.streamUrl.contains("mp4") || track.streamUrl.contains("aac")) "AAC 320" else "WEBM HD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }

                    val remainingMs = (totalDurationMs - displayPositionMs).coerceAtLeast(0L)
                    Text(
                        text = "-" + formatTime(remainingMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Playback Controls Row: Prev, Big Rounded Play/Pause, Next
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Surface(
                    onClick = onPlayPause,
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    shadowElevation = 12.dp,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.Black,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Dock: Square FAB Style Queue & Lyrics + Real-time Connected Audio Output Device
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Queue Button - Square FAB Style
                    Surface(
                        onClick = onOpenQueue,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("now_playing_queue_fab")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Lyrics Button - Square FAB Style
                    Surface(
                        onClick = onOpenLyrics,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .testTag("now_playing_lyrics_fab")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = "Lyrics",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Real-time Audio Output Device Square FAB Pill
                // Automatically displays connected Bluetooth speaker/headphones/USB name in real-time or "Speaker"
                Surface(
                    onClick = onOpenDeviceSelector,
                    shape = RoundedCornerShape(14.dp),
                    color = if (activeAudioDevice.isExternalConnected) {
                        dynamicColors.primary.copy(alpha = 0.28f)
                    } else {
                        Color.White.copy(alpha = 0.18f)
                    },
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .height(48.dp)
                        .border(
                            width = 1.dp,
                            color = if (activeAudioDevice.isExternalConnected) {
                                dynamicColors.primary.copy(alpha = 0.55f)
                            } else {
                                Color.White.copy(alpha = 0.22f)
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                        .testTag("now_playing_speaker_device_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeAudioDevice.isExternalConnected) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399))
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                        }
                        Icon(
                            imageVector = activeAudioDevice.icon,
                            contentDescription = activeAudioDevice.name,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeAudioDevice.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sleep Timer Dialog
 */
@Composable
fun SleepTimerDialog(
    onSetTimerMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Bedtime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sleep Timer", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val durations = listOf(
                    15 to "15 minutes",
                    30 to "30 minutes",
                    45 to "45 minutes",
                    60 to "1 hour",
                    -1 to "End of Track"
                )
                durations.forEach { (mins, label) ->
                    Surface(
                        onClick = {
                            onSetTimerMinutes(mins)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onCancelTimer()
                onDismiss()
            }) {
                Text("Turn Off")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


object ArtistDpCache {
    fun get(name: String): String? = com.example.data.remote.ArtistDpManager.get(name)

    fun put(name: String, url: String) {
        com.example.data.remote.ArtistDpManager.put(name, url)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongCreditsBottomSheet(
    track: PlayableTrack,
    repository: MusicRepository? = null,
    onViewArtist: (String) -> Unit = {},
    onViewAlbum: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val actualRepo = remember(repository, context) { repository ?: MusicRepository(context) }

    val artistList = remember(track.artist) {
        track.artist.split(Regex("[,&;/]|\\bfeat\\.?\\b|\\bft\\.?\\b", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val artistPhotos = remember(artistList) {
        mutableStateMapOf<String, String>().apply {
            artistList.forEach { name ->
                val direct = com.example.data.remote.ArtistDpManager.getOriginalDp(name)
                if (direct.isNotBlank()) {
                    put(name, direct)
                }
            }
        }
    }

    LaunchedEffect(artistList) {
        artistList.forEach { name ->
            val cached = com.example.data.remote.ArtistDpManager.get(name)
            if (!cached.isNullOrBlank()) {
                artistPhotos[name] = cached
            } else {
                launch(Dispatchers.IO) {
                    try {
                        val result = actualRepo.searchArtists(name, limit = 5)
                        if (result is NetworkResult.Success && result.data.isNotEmpty()) {
                            val matched = result.data.firstOrNull {
                                it.name.trim().equals(name.trim(), ignoreCase = true) &&
                                it.image.isNotBlank() &&
                                !it.image.contains("default")
                            } ?: result.data.firstOrNull { it.image.isNotBlank() && !it.image.contains("default") }

                            if (matched != null && matched.image.isNotBlank()) {
                                com.example.data.remote.ArtistDpManager.put(name, matched.image)
                                artistPhotos[name] = matched.image
                            }
                        }
                    } catch (e: Exception) {
                        // ignore network errors
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header preview with album artwork
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = track.artwork,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Song Credits",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 1. Artists & Performers Section (with Real Direct Artist DP / Avatars and Clickable to open)
            Text(
                text = "Artists & Performers",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 2.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                artistList.forEach { artistName ->
                    val artistImage = artistPhotos[artistName] ?: com.example.data.remote.ArtistDpManager.getOriginalDp(artistName)

                    Surface(
                        onClick = {
                            onViewArtist(artistName)
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Round Artist Avatar with Real Direct DP
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (artistImage.isNotBlank()) {
                                    AsyncImage(
                                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                                            .data(artistImage)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = artistName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = artistName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Artist • Tap to view profile & songs",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = "View Artist",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. Track & Album Technical Metadata Card
            Text(
                text = "Release & Audio Details",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 2.dp)
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    CreditRow(label = "Song Title", value = track.title)

                    if (track.album.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (track.albumId.isNotBlank()) {
                                        Modifier.clickable { onViewAlbum(track.albumId) }
                                    } else Modifier
                                )
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "Album",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = track.album,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = if (track.albumId.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                if (track.albumId.isNotBlank()) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (track.language.isNotBlank()) {
                        CreditRow(label = "Language", value = track.language.replaceFirstChar { it.uppercase() })
                    }
                    if (track.year.isNotBlank()) {
                        CreditRow(label = "Release Year", value = track.year)
                    }
                    CreditRow(label = "Audio Quality", value = "320 kbps Ultra HD (Studio Master Fidelity)")
                    CreditRow(label = "Source", value = "SMusic Official Catalog")
                    if (track.copyright.isNotBlank()) {
                        CreditRow(label = "Copyright / Label", value = track.copyright)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    track: PlayableTrack,
    isLiked: Boolean,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Int,
    volume: Float = 1.0f,
    onVolumeChange: (Float) -> Unit = {},
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onShowQueue: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowCredits: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onDownload: () -> Unit,
    onStartRadio: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onViewAlbum: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Track preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = track.artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontFamily = AppFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = AppFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontFamily = AppFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = AppFontFamily,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
            )

            // Segmented 3-Card Row: Like (Start Rounding), Download (Middle), Add to Playlist (Last Rounding)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Like Card (Start Rounding)
                val likeShape = RoundedCornerShape(
                    topStart = 24.dp,
                    bottomStart = 24.dp,
                    topEnd = 6.dp,
                    bottomEnd = 6.dp
                )
                Surface(
                    onClick = onToggleLike,
                    shape = likeShape,
                    color = if (isLiked) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isLiked) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("song_options_card_like")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isLiked) "Liked" else "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isLiked) "Liked" else "Like",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 2. Download Card (Middle)
                val downloadShape = RoundedCornerShape(6.dp)
                val dlActive = isDownloaded || isDownloading
                val dlTitle = when {
                    isDownloading -> "$downloadProgress%"
                    isDownloaded -> "Downloaded"
                    else -> "Download"
                }
                Surface(
                    onClick = onDownload,
                    shape = downloadShape,
                    color = if (dlActive) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (dlActive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("song_options_card_download")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isDownloading -> Icons.Outlined.Downloading
                                isDownloaded -> com.example.ui.theme.AppIcons.DownloadForOffline
                                else -> com.example.ui.theme.AppIcons.Download
                            },
                            contentDescription = dlTitle,
                            tint = if (dlActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dlTitle,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (dlActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // 3. Add to Playlist Card (Last Rounding)
                val playlistShape = RoundedCornerShape(
                    topStart = 6.dp,
                    bottomStart = 6.dp,
                    topEnd = 24.dp,
                    bottomEnd = 24.dp
                )
                Surface(
                    onClick = onAddToPlaylist,
                    shape = playlistShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .testTag("song_options_card_add_to_playlist")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                            contentDescription = "Add to Playlist",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add to Playlist",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Clean Option Items List (Like, Download, Add to Playlist removed from line below)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                OptionItem(
                    icon = Icons.Outlined.Equalizer,
                    title = "Equalizer",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onOpenEqualizer
                )

                OptionItem(
                    icon = Icons.Outlined.Radio,
                    title = "Start Song Radio",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onStartRadio
                )

                OptionItem(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Up Next (Queue)",
                    onClick = onShowQueue
                )

                OptionItem(
                    icon = Icons.Default.Lyrics,
                    title = "View Lyrics",
                    onClick = onShowLyrics
                )

                OptionItem(
                    icon = Icons.Rounded.Shuffle,
                    title = if (isShuffle) "Shuffle: ON" else "Shuffle: OFF",
                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    onClick = onToggleShuffle
                )

                val repeatTitle = when (repeatMode) {
                    RepeatMode.ONE -> "Repeat: Track (One)"
                    RepeatMode.ALL -> "Repeat: All"
                    RepeatMode.OFF -> "Repeat: OFF"
                }
                OptionItem(
                    icon = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    title = repeatTitle,
                    tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    onClick = onToggleRepeat
                )

                if (track.albumId.isNotBlank()) {
                    OptionItem(
                        icon = Icons.Outlined.Album,
                        title = "View Album",
                        onClick = onViewAlbum
                    )
                }

                OptionItem(
                    icon = Icons.Outlined.Info,
                    title = "View Song Credits",
                    onClick = onShowCredits
                )

                OptionItem(
                    icon = Icons.AutoMirrored.Rounded.Send,
                    title = "Share",
                    onClick = onShare
                )
            }
        }
    }
}

@Composable
private fun MiniActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val contentColor = if (isActive) activeColor else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        shadowElevation = 0.dp,
        border = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = tint,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    track: PlayableTrack,
    repository: MusicRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playlists by repository.userPlaylists.collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            scope.launch {
                                val id = repository.createPlaylist(newPlaylistName.trim())
                                repository.addSongToPlaylist(id, track)
                                Toast.makeText(context, "Added to playlist \"$newPlaylistName\"", Toast.LENGTH_SHORT).show()
                                showCreateDialog = false
                                onDismiss()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Add to Playlist", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button to create new playlist
                Surface(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Create New Playlist",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists found. Create one above!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                    )
                } else {
                    playlists.forEach { playlist ->
                        Surface(
                            onClick = {
                                scope.launch {
                                    repository.addSongToPlaylist(playlist.id, track)
                                    Toast.makeText(context, "Added to playlist \"${playlist.name}\"", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    if (playlist.description.isNotBlank()) {
                                        Text(
                                            text = playlist.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

data class RealtimeAudioDevice(
    val id: String,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isCurrent: Boolean
)

@Composable
fun DeviceConnectDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    fun queryDevices(): List<RealtimeAudioDevice> {
        val result = mutableListOf<RealtimeAudioDevice>()
        var bluetoothConnected = false
        var wiredConnected = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            for (dev in devices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                    AudioDeviceInfo.TYPE_BLE_HEADSET,
                    AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                        bluetoothConnected = true
                        val prodName = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth Audio"
                        result.add(
                            RealtimeAudioDevice(
                                id = "bt_${dev.id}",
                                name = prodName,
                                icon = Icons.Outlined.BluetoothAudio,
                                isCurrent = true
                            )
                        )
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        wiredConnected = true
                        val prodName = dev.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Headphones"
                        result.add(
                            RealtimeAudioDevice(
                                id = "wired_${dev.id}",
                                name = prodName,
                                icon = Icons.Outlined.Headphones,
                                isCurrent = !bluetoothConnected
                            )
                        )
                    }
                }
            }
        }

        // Built-in phone speaker
        result.add(
            RealtimeAudioDevice(
                id = "phone_speaker",
                name = "This Phone",
                icon = Icons.Outlined.PhoneAndroid,
                isCurrent = !bluetoothConnected && !wiredConnected
            )
        )

        return result
    }

    var devicesList by remember { mutableStateOf(queryDevices()) }

    DisposableEffect(audioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    devicesList = queryDevices()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    devicesList = queryDevices()
                }
            }
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
            onDispose {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        } else {
            onDispose { }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Devices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Connect a Device",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AUDIO OUTPUT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Realtime detected devices
                devicesList.forEach { device ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (device.isCurrent) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = if (device.isCurrent) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = device.icon,
                                contentDescription = null,
                                tint = if (device.isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (device.isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (device.isCurrent) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (device.isCurrent) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Pair Bluetooth Button - M3 style with theme colors only
                Surface(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Bluetooth settings not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Bluetooth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Pair Bluetooth",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = "Open Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<PlayableTrack>,
    currentIndex: Int,
    isPlaying: Boolean = true,
    isAutoplayEnabled: Boolean = true,
    onToggleAutoplay: () -> Unit = {},
    onLoadMoreRecommendations: () -> Unit = {},
    onDismiss: () -> Unit,
    onSelectTrack: (Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onClearQueue: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Playing Queue (${queue.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (isAutoplayEnabled) "Auto-Radio Mix Enabled" else "Auto-Play Disabled",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isAutoplayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (queue.isNotEmpty()) {
                        TextButton(onClick = onClearQueue) {
                            Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Quick Auto-Play / Radio Controls Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isAutoplayEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Radio,
                                    contentDescription = null,
                                    tint = if (isAutoplayEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Infinite Auto-Radio",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Auto-append similar songs",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onLoadMoreRecommendations) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Load Recommendations",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Switch(
                            checked = isAutoplayEnabled,
                            onCheckedChange = { onToggleAutoplay() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Queue is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    itemsIndexed(queue, key = { idx, item -> "${item.id}_$idx" }) { index, track ->
                        val isCurrent = index == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
                                .clickable { onSelectTrack(index) }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = track.artwork,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.50f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ThreeLineVisualizer(
                                            isPlaying = isPlaying,
                                            color = MaterialTheme.colorScheme.primary,
                                            barWidth = 2.5.dp,
                                            maxHeight = 16.dp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontFamily = AppFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = AppFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    fontFamily = AppFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Normal,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = AppFontFamily,
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { onRemoveTrack(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsBottomSheet(
    track: PlayableTrack,
    repository: MusicRepository,
    onDismiss: () -> Unit
) {
    var lyricsText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(track.id) {
        isLoading = true
        val lyrics = repository.getLyrics(track.lyricsId, null)
        lyricsText = lyrics
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${track.title} • ${track.artist}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = lyricsText ?: "Lyrics not available for this track.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
