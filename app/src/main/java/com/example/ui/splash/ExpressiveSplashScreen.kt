package com.example.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Cookie4Sided
import com.example.ui.theme.MiniPlayerScallopedShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpressiveSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    var showBranding by remember { mutableStateOf(false) }
    var showBottomProgress by remember { mutableStateOf(false) }

    val outerBadgeScale = remember { Animatable(0.2f) }
    val innerCookieScale = remember { Animatable(0f) }
    val centerIconScale = remember { Animatable(0f) }
    val progressValue = remember { Animatable(0f) }

    // 10 Seconds splash duration
    LaunchedEffect(Unit) {
        launch {
            outerBadgeScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        delay(120)
        launch {
            innerCookieScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        delay(100)
        launch {
            centerIconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        showBranding = true
        delay(150)
        showBottomProgress = true

        // Progress smoothly fills over 10 seconds (10,000 ms)
        launch {
            progressValue.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 10000, easing = LinearEasing)
            )
        }
        delay(10000)
        onSplashFinished()
    }

    // Continuous expressive motion animations
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_splash_motion")

    val scallopedRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scalloped_rotation"
    )

    val cookieCounterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cookie_counter_rotation"
    )

    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    val orbitPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_phase"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSplashFinished
            )
            .testTag("expressive_splash_screen")
    ) {
        // 1. Ambient Mesh Glow & Orbital Geometry
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.44f
            val maxRadius = size.minDimension * 0.72f

            // Top-left primary radial glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = 0.22f * auraPulse),
                        colorScheme.primaryContainer.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(cx - maxRadius * 0.25f, cy - maxRadius * 0.2f),
                    radius = maxRadius * auraPulse
                ),
                radius = maxRadius * auraPulse,
                center = Offset(cx - maxRadius * 0.25f, cy - maxRadius * 0.2f)
            )

            // Bottom-right tertiary radial glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.tertiary.copy(alpha = 0.18f),
                        colorScheme.secondaryContainer.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(cx + maxRadius * 0.3f, cy + maxRadius * 0.28f),
                    radius = maxRadius * 0.9f
                ),
                radius = maxRadius * 0.9f,
                center = Offset(cx + maxRadius * 0.3f, cy + maxRadius * 0.28f)
            )

            // Orbital concentric rings
            val ringRadius1 = size.minDimension * 0.36f * auraPulse
            val ringRadius2 = size.minDimension * 0.45f / auraPulse

            drawCircle(
                color = colorScheme.primary.copy(alpha = 0.10f),
                radius = ringRadius1,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )

            drawCircle(
                color = colorScheme.tertiary.copy(alpha = 0.07f),
                radius = ringRadius2,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )

            // Orbiting satellite dots
            for (i in 0 until 4) {
                val angle = orbitPhase + i * (Math.PI.toFloat() / 2f)
                val r = if (i % 2 == 0) ringRadius1 else ringRadius2
                val dotX = cx + r * cos(angle)
                val dotY = cy + r * sin(angle)
                drawCircle(
                    color = if (i % 2 == 0) colorScheme.primary.copy(alpha = 0.45f)
                    else colorScheme.tertiary.copy(alpha = 0.40f),
                    radius = if (i % 2 == 0) 5.dp.toPx() else 3.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }

        // 2. Main Center Content & Progress Indicator
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Centerpiece: Layered Morphing Shapes & Clean App Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    // Outer Glowing Halo Scalloped Shape
                    Box(
                        modifier = Modifier
                            .size(224.dp)
                            .graphicsLayer {
                                val s = outerBadgeScale.value * auraPulse
                                scaleX = s
                                scaleY = s
                                rotationZ = scallopedRotation
                            }
                            .clip(MiniPlayerScallopedShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        colorScheme.primary.copy(alpha = 0.24f),
                                        colorScheme.tertiary.copy(alpha = 0.18f),
                                        colorScheme.secondary.copy(alpha = 0.22f)
                                    )
                                )
                            )
                    )

                    // Primary Scalloped Flower/Star Surface
                    Surface(
                        shape = MiniPlayerScallopedShape,
                        color = colorScheme.primaryContainer,
                        tonalElevation = 12.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(
                            width = 2.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.45f),
                                    colorScheme.primary.copy(alpha = 0.35f)
                                )
                            )
                        ),
                        modifier = Modifier
                            .size(182.dp)
                            .graphicsLayer {
                                val s = outerBadgeScale.value
                                scaleX = s
                                scaleY = s
                                rotationZ = scallopedRotation
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            colorScheme.primary.copy(alpha = 0.32f),
                                            colorScheme.primaryContainer
                                        )
                                    )
                                )
                        )
                    }

                    // Counter-Rotating 4-Sided Expressive Cookie Shape
                    Surface(
                        shape = Cookie4Sided,
                        color = colorScheme.surfaceContainerHighest,
                        tonalElevation = 16.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    colorScheme.primary.copy(alpha = 0.6f),
                                    colorScheme.tertiary.copy(alpha = 0.5f)
                                )
                            )
                        ),
                        modifier = Modifier
                            .size(128.dp)
                            .graphicsLayer {
                                val s = innerCookieScale.value
                                scaleX = s
                                scaleY = s
                                rotationZ = cookieCounterRotation
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            colorScheme.surfaceContainerHighest,
                                            colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                    }

                    // Center Equalizer Emblem
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primary,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(68.dp)
                            .graphicsLayer {
                                val s = centerIconScale.value
                                scaleX = s
                                scaleY = s
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            colorScheme.primary,
                                            colorScheme.primary.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        ) {
                            ExpressiveEqualizerBars(
                                phase = wavePhase,
                                barColor = colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Floating Accent Note
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.tertiaryContainer,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-14).dp, y = 18.dp)
                            .graphicsLayer {
                                val s = innerCookieScale.value
                                scaleX = s
                                scaleY = s
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Floating Accent Acoustic Wave
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.secondaryContainer,
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 14.dp, y = (-18).dp)
                            .graphicsLayer {
                                val s = innerCookieScale.value
                                scaleX = s
                                scaleY = s
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Clean App Title
                AnimatedVisibility(
                    visible = showBranding,
                    enter = fadeIn(tween(420)) + slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                ) {
                    Text(
                        text = "SMusic",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.0).sp,
                            fontSize = 44.sp
                        ),
                        color = colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Minimal Sleek Progress Bar
            AnimatedVisibility(
                visible = showBottomProgress,
                enter = fadeIn(tween(380)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp, vertical = 16.dp)
            ) {
                ExpressiveSegmentedProgressBar(
                    progress = progressValue.value,
                    primaryColor = colorScheme.primary,
                    tertiaryColor = colorScheme.tertiary,
                    trackColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpressiveEqualizerBars(
    phase: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = 5
        val spacing = size.width * 0.09f
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val offsetPhase = phase + i * 0.85f
            val normalized = (sin(offsetPhase) + 1f) / 2f
            val minFraction = if (i == 2) 0.42f else 0.25f
            val maxFraction = if (i == 2) 1.0f else if (i == 1 || i == 3) 0.85f else 0.65f
            val heightFraction = minFraction + normalized * (maxFraction - minFraction)
            val barHeight = size.height * heightFraction
            val left = i * (barWidth + spacing)
            val top = centerY - barHeight / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun ExpressiveSegmentedProgressBar(
    progress: Float,
    primaryColor: Color,
    tertiaryColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        val clamped = progress.coerceIn(0f, 1f)

        // Background Track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = radius
        )

        // Active Progress Fill
        val activeWidth = size.width * clamped
        if (activeWidth > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(primaryColor, tertiaryColor)
                ),
                topLeft = Offset.Zero,
                size = Size(activeWidth, size.height),
                cornerRadius = radius
            )
        }
    }
}
