package com.example.ui.splash

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.Cookie4Sided
import com.example.ui.theme.MiniPlayerScallopedShape

@Composable
fun ExpressivePermissionScreen(
    onAgreeAndContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var isNotifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var isAudioGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionsToRequest = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            results[Manifest.permission.POST_NOTIFICATIONS]?.let { isNotifGranted = it }
            results[Manifest.permission.READ_MEDIA_AUDIO]?.let { isAudioGranted = it }
        } else {
            results[Manifest.permission.READ_EXTERNAL_STORAGE]?.let { isAudioGranted = it }
        }
        onAgreeAndContinue()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "perm_expressive_motion")
    val slowSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "perm_slow_spin"
    )
    val counterSpin by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "perm_counter_spin"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "perm_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("m3_permission_screen")
    ) {
        // Ambient Luxury Mesh Glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.24f
            val radius = size.minDimension * 0.72f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = 0.22f * pulse),
                        colorScheme.tertiary.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = radius * pulse
                ),
                radius = radius * pulse,
                center = Offset(cx, cy)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Expressive Hero Emblem + Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(152.dp)
                ) {
                    // Outer Rotating Scalloped Halo
                    Surface(
                        shape = MiniPlayerScallopedShape,
                        color = colorScheme.primaryContainer.copy(alpha = 0.85f),
                        tonalElevation = 10.dp,
                        border = BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    colorScheme.primary.copy(alpha = 0.35f)
                                )
                            )
                        ),
                        modifier = Modifier
                            .size(134.dp)
                            .graphicsLayer {
                                scaleX = pulse
                                scaleY = pulse
                                rotationZ = slowSpin
                            }
                    ) {}

                    // Inner Counter-Rotating Cookie4Sided Surface
                    Surface(
                        shape = Cookie4Sided,
                        color = colorScheme.surfaceContainerHighest,
                        tonalElevation = 16.dp,
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = colorScheme.primary.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .size(96.dp)
                            .graphicsLayer {
                                rotationZ = counterSpin
                            }
                    ) {}

                    // Center Shield / Security Emblem
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primary,
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    shape = CircleShape,
                    color = colorScheme.secondaryContainer.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SMusic Permissions",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSecondaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Seamless Audio Experience",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // M3 Expressive Permission Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExpressivePermissionCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Playback & Lockscreen Controls",
                    subtitle = "Live media notifications, background playback & quick controls",
                    isGranted = isNotifGranted,
                    accentColor = colorScheme.primary
                )

                ExpressivePermissionCard(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "Audio & Offline Downloads",
                    subtitle = "Save high-quality tracks offline & access local audio files",
                    isGranted = isAudioGranted,
                    accentColor = colorScheme.tertiary
                )

                ExpressivePermissionCard(
                    icon = Icons.Rounded.GraphicEq,
                    title = "Background Streaming Service",
                    subtitle = "Uninterrupted gapless playback with auto-radio queue",
                    isGranted = true,
                    accentColor = colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Agree & Continue Action Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        multiPermissionLauncher.launch(permissionsToRequest)
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("permission_agree_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Agree & Continue",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    )
                }

                TextButton(
                    onClick = onAgreeAndContinue,
                    shape = CircleShape,
                    modifier = Modifier.testTag("permission_skip_button")
                ) {
                    Text(
                        text = "Continue Now",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressivePermissionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    accentColor: Color
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        tonalElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.35f),
                    colorScheme.outlineVariant.copy(alpha = 0.20f)
                )
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f)),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = CircleShape,
                color = if (isGranted) {
                    colorScheme.primaryContainer
                } else {
                    colorScheme.surfaceContainerHighest
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Rounded.Check else Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = if (isGranted) colorScheme.onPrimaryContainer else colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isGranted) "Ready" else "Allow",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
