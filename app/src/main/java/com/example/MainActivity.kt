package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.PlayerManager
import com.example.repository.MusicRepository
import com.example.ui.details.AlbumDetailScreen
import com.example.ui.details.ArtistDetailScreen
import com.example.ui.details.PlaylistDetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.player.MiniPlayer
import com.example.ui.player.NowPlayingModal
import com.example.ui.search.SearchScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.splash.ExpressiveSplashScreen
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.SMusicTheme
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode

enum class RootScreen {
    HOME, SEARCH, LIBRARY
}

sealed class SubScreen {
    object None : SubScreen()
    object Settings : SubScreen()
    data class AlbumDetail(
        val id: String,
        val title: String = "",
        val artist: String = "",
        val artwork: String = ""
    ) : SubScreen()
    data class PlaylistDetail(val id: String) : SubScreen()
    data class ArtistDetail(val id: String) : SubScreen()
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val playerManager = PlayerManager.getInstance(applicationContext)
        val repository = MusicRepository(applicationContext)

        val themeManager = ThemeManager.getInstance(applicationContext)

        setContent {
            val themeMode by themeManager.themeMode.collectAsState()
            val useDynamicColor by themeManager.useDynamicColor.collectAsState()
            val isAmoledBlack by themeManager.isAmoledBlack.collectAsState()
            val accentPalette by themeManager.accentPalette.collectAsState()
            val fontOption by themeManager.fontOption.collectAsState()

            SMusicTheme(
                themeMode = themeMode,
                dynamicColor = useDynamicColor,
                isAmoledBlack = isAmoledBlack,
                accentPalette = accentPalette,
                fontOption = fontOption
            ) {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                var isMainContentReady by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(150)
                    isMainContentReady = true
                }

                // Runtime Notification Permission on Android 13+ (Pixel lockscreen / media notification support)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notifPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { /* Handled */ }

                    LaunchedEffect(showSplash) {
                        if (!showSplash && ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                var currentRootScreen by remember { mutableStateOf(RootScreen.HOME) }
                var currentSubScreen by remember { mutableStateOf<SubScreen>(SubScreen.None) }
                var isNowPlayingOpen by remember { mutableStateOf(false) }

                val currentTrack by playerManager.currentTrack.collectAsState()

                // Intercept back button when Now Playing or Subscreen is open
                BackHandler(enabled = isNowPlayingOpen || currentSubScreen !is SubScreen.None) {
                    if (isNowPlayingOpen) {
                        isNowPlayingOpen = false
                    } else if (currentSubScreen !is SubScreen.None) {
                        currentSubScreen = SubScreen.None
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (isMainContentReady || !showSplash) {
                        // Full Screen Content Layer: Extends fully to screen edges underneath floating controls
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            when (val sub = currentSubScreen) {
                                is SubScreen.Settings -> {
                                    SettingsScreen(
                                        repository = repository,
                                        playerManager = playerManager,
                                        themeManager = themeManager,
                                        onBack = { currentSubScreen = SubScreen.None }
                                    )
                                }
                                is SubScreen.AlbumDetail -> {
                                    AlbumDetailScreen(
                                        albumId = sub.id,
                                        initialTitle = sub.title,
                                        initialArtist = sub.artist,
                                        initialArtwork = sub.artwork,
                                        repository = repository,
                                        playerManager = playerManager,
                                        onBack = { currentSubScreen = SubScreen.None }
                                    )
                                }
                                is SubScreen.PlaylistDetail -> {
                                    PlaylistDetailScreen(
                                        playlistId = sub.id,
                                        repository = repository,
                                        playerManager = playerManager,
                                        onBack = { currentSubScreen = SubScreen.None }
                                    )
                                }
                                is SubScreen.ArtistDetail -> {
                                    ArtistDetailScreen(
                                        artistId = sub.id,
                                        repository = repository,
                                        playerManager = playerManager,
                                        onNavigateToAlbum = { albumId -> 
                                            currentSubScreen = SubScreen.AlbumDetail(id = albumId) 
                                        },
                                        onBack = { currentSubScreen = SubScreen.None }
                                    )
                                }
                                SubScreen.None -> {
                                    when (currentRootScreen) {
                                        RootScreen.HOME -> {
                                            HomeScreen(
                                                viewModel = homeViewModel,
                                                playerManager = playerManager,
                                                onNavigateToSearch = { currentRootScreen = RootScreen.SEARCH },
                                                onNavigateToSettings = { currentSubScreen = SubScreen.Settings },
                                                onNavigateToAlbum = { album ->
                                                    currentSubScreen = SubScreen.AlbumDetail(
                                                        id = album.id,
                                                        title = album.title,
                                                        artist = album.artist,
                                                        artwork = album.artwork
                                                    )
                                                },
                                                onNavigateToPlaylist = { currentSubScreen = SubScreen.PlaylistDetail(it) },
                                                onNavigateToArtist = { currentSubScreen = SubScreen.ArtistDetail(it) }
                                            )
                                        }
                                        RootScreen.SEARCH -> {
                                            SearchScreen(
                                                viewModel = searchViewModel,
                                                playerManager = playerManager,
                                                onNavigateToAlbum = { album ->
                                                    currentSubScreen = SubScreen.AlbumDetail(
                                                        id = album.id,
                                                        title = album.title,
                                                        artist = album.artist,
                                                        artwork = album.artwork
                                                    )
                                                },
                                                onNavigateToPlaylist = { currentSubScreen = SubScreen.PlaylistDetail(it) },
                                                onNavigateToArtist = { currentSubScreen = SubScreen.ArtistDetail(it) }
                                            )
                                        }
                                        RootScreen.LIBRARY -> {
                                            LibraryScreen(
                                                viewModel = libraryViewModel,
                                                playerManager = playerManager,
                                                onNavigateToPlaylist = { currentSubScreen = SubScreen.PlaylistDetail(it) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Full Mask Gradient Scrim (Smooth fade overlay so scrolling content effortlessly blends under floating controls)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.40f),
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                            MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
                                        )
                                    )
                                )
                        )

                        // Floating Row containing Split Liquid Glass Navigation Bar and Mini Player
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Full Rounded Floating Mini Player with Liquid Glass styling
                            AnimatedVisibility(
                                visible = currentTrack != null && !isNowPlayingOpen,
                                enter = slideInVertically(initialOffsetY = { it / 2 }) + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it / 2 }) + shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.35f),
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
                                            )
                                        )
                                    ),
                                    tonalElevation = 6.dp,
                                    shadowElevation = 10.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("floating_mini_player_container")
                                ) {
                                    MiniPlayer(
                                        playerManager = playerManager,
                                        repository = repository,
                                        onClick = { isNowPlayingOpen = true }
                                    )
                                }
                            }

                            // Split Liquid Glass Navigation Bar (Home + Library Capsule + Separate Search Button)
                            LiquidGlassSplitBottomBar(
                                currentRootScreen = currentRootScreen,
                                isSubScreenOpen = currentSubScreen !is SubScreen.None,
                                onSelectTab = { screen ->
                                    currentRootScreen = screen
                                    currentSubScreen = SubScreen.None
                                }
                            )
                        }
                    }

                    // Full Screen Now Playing Modal (Slide in from bottom, covers root)
                    AnimatedVisibility(
                        visible = isNowPlayingOpen && currentTrack != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        NowPlayingModal(
                            playerManager = playerManager,
                            repository = repository,
                            onDismiss = { isNowPlayingOpen = false },
                            onViewAlbum = { albumId ->
                                isNowPlayingOpen = false
                                currentSubScreen = SubScreen.AlbumDetail(albumId)
                            },
                            onViewArtist = { artistId ->
                                isNowPlayingOpen = false
                                currentSubScreen = SubScreen.ArtistDetail(artistId)
                            }
                        )
                    }

                    // Material 3 Expressive Full-Screen Splash Overlay
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(420, easing = FastOutSlowInEasing)) +
                            scaleOut(
                                targetScale = 1.08f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ExpressiveSplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidGlassSplitBottomBar(
    currentRootScreen: RootScreen,
    isSubScreenOpen: Boolean,
    onSelectTab: (RootScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .wrapContentWidth()
            .testTag("bottom_nav_bar"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Unified Capsule for Home & Library (Closer pass-pass icons)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.76f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.60f),
                        Color.White.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                    )
                )
            ),
            tonalElevation = 10.dp,
            shadowElevation = 16.dp,
            modifier = Modifier.testTag("nav_capsule_home_library")
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassNavPill(
                    selected = currentRootScreen == RootScreen.HOME && !isSubScreenOpen,
                    selectedIcon = Icons.Rounded.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    label = "Home",
                    onClick = { onSelectTab(RootScreen.HOME) },
                    testTag = "nav_item_home"
                )

                LiquidGlassNavPill(
                    selected = currentRootScreen == RootScreen.LIBRARY && !isSubScreenOpen,
                    selectedIcon = Icons.Rounded.LibraryMusic,
                    unselectedIcon = Icons.Outlined.LibraryMusic,
                    label = "Library",
                    onClick = { onSelectTab(RootScreen.LIBRARY) },
                    testTag = "nav_item_library"
                )
            }
        }

        // 2. Separate Single Rounded Liquid Glass Search Button
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.76f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.60f),
                        Color.White.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                    )
                )
            ),
            tonalElevation = 10.dp,
            shadowElevation = 16.dp,
            modifier = Modifier.testTag("nav_search_container")
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassNavPill(
                    selected = currentRootScreen == RootScreen.SEARCH && !isSubScreenOpen,
                    selectedIcon = Icons.Rounded.Search,
                    unselectedIcon = Icons.Outlined.Search,
                    label = "Search",
                    onClick = { onSelectTab(RootScreen.SEARCH) },
                    testTag = "nav_item_search"
                )
            }
        }
    }
}

@Composable
fun LiquidGlassNavPill(
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pill_icon_scale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "pill_content_color"
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.0f,
        animationSpec = tween(240),
        label = "pill_bg_alpha"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f * backgroundAlpha)
        } else {
            Color.Transparent
        },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.50f * backgroundAlpha),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f * backgroundAlpha)
                    )
                )
            )
        } else null,
        modifier = modifier
            .height(42.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = if (selected) 14.dp else 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale)
            )

            // Animated expanding label when selected (Icon tap to label show)
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(animationSpec = tween(180)),
                exit = shrinkHorizontally(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) + fadeOut(animationSpec = tween(140))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = contentColor
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
