package com.example.ui.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.remote.StreamUrlResolver
import com.example.player.EqualizerManager
import com.example.player.PlayerManager
import com.example.repository.MusicRepository
import com.example.ui.common.UserAvatarBadge
import com.example.ui.common.UserProfileM3CardDialog
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.FontOption
import com.example.ui.theme.MiniPlayerScallopedShape
import com.example.ui.theme.NowPlayingStyle
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

private val GitHubIcon: ImageVector
    get() = ImageVector.Builder(
        name = "GitHub",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
                "M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z"
            ).toNodes(),
            fill = androidx.compose.ui.graphics.SolidColor(Color.White)
        )
    }.build()

private val TelegramIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Telegram",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
                "M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0zm5.894 8.221l-1.97 9.28c-.145.658-.537.818-1.084.508l-3-2.21-1.446 1.394c-.16.16-.295.295-.605.295l.213-3.053 5.56-5.023c.242-.213-.054-.333-.373-.121l-6.871 4.326-2.962-.924c-.643-.204-.657-.643.136-.953l11.57-4.461c.536-.194 1.006.131.832.942z"
            ).toNodes(),
            fill = androidx.compose.ui.graphics.SolidColor(Color.White)
        )
    }.build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: MusicRepository,
    playerManager: PlayerManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    themeManager: ThemeManager = ThemeManager.getInstance(LocalContext.current)
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val equalizerManager = remember { EqualizerManager.getInstance(context) }
    var showEqualizerScreen by rememberSaveable { mutableStateOf(false) }
    var showLicensesScreen by rememberSaveable { mutableStateOf(false) }
    var showAboutScreen by rememberSaveable { mutableStateOf(false) }

    if (showEqualizerScreen) {
        EqualizerScreen(
            onBack = { showEqualizerScreen = false },
            equalizerManager = equalizerManager,
            modifier = modifier
        )
        return
    }

    if (showLicensesScreen) {
        OpenSourceLicensesScreen(
            onBack = { showLicensesScreen = false },
            modifier = modifier
        )
        return
    }

    if (showAboutScreen) {
        AboutAppScreen(
            onBack = { showAboutScreen = false },
            modifier = modifier
        )
        return
    }

    BackHandler { onBack() }
    val scope = rememberCoroutineScope()

    val currentThemeMode by themeManager.themeMode.collectAsState()
    val useDynamicColor by themeManager.useDynamicColor.collectAsState()
    val isAmoledBlack by themeManager.isAmoledBlack.collectAsState()
    val isDynamicSongThemeEnabled by themeManager.isDynamicSongThemeEnabled.collectAsState()
    val accentPalette by themeManager.accentPalette.collectAsState()
    val currentFontOption by themeManager.fontOption.collectAsState()
    val useDeviceFont by themeManager.useDeviceFont.collectAsState()
    val userNickname by themeManager.userNickname.collectAsState()
    val userAvatarEmoji by themeManager.userAvatarEmoji.collectAsState()
    val userAvatarImageUri by themeManager.userAvatarImageUri.collectAsState()

    val isEqEnabled by equalizerManager.isEnabled.collectAsState()
    val selectedEqPreset by equalizerManager.selectedPreset.collectAsState()
    val isAutoplayEnabled by playerManager.isAutoplayEnabled.collectAsState()

    var showProfileCardDialog by remember { mutableStateOf(false) }
    var pauseOnDisconnect by remember { mutableStateOf(true) }
    var selectedQuality by remember { mutableStateOf(StreamUrlResolver.AudioQuality.HIGH) }

    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearRecentDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    if (showProfileCardDialog) {
        UserProfileM3CardDialog(
            themeManager = themeManager,
            onDismiss = { showProfileCardDialog = false }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 165.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. PROFILE CATEGORY (Large Card Style Shape)
            item {
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                SettingsCategoryHeader(title = "Profile")
                Card(
                    onClick = { showProfileCardDialog = true },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF191924).copy(alpha = 0.75f)
                        else Color.White.copy(alpha = 0.85f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_profile_m3_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            UserAvatarBadge(
                                emoji = userAvatarEmoji,
                                customImageUri = userAvatarImageUri,
                                size = 60.dp,
                                fontSize = 27.sp
                            )
                            Text(
                                text = userNickname,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .clickable { showProfileCardDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // 2. AUDIO & PLAYBACK CATEGORY (Large Card Style Shape)
            item {
                SettingsCategoryHeader(title = "Audio & Playback")
                SettingsLargeCard {
                    // Equalizer Option Item
                    SettingsOptionRow(
                        icon = Icons.Outlined.Equalizer,
                        title = "Equalizer",
                        valueText = if (isEqEnabled) selectedEqPreset.displayName else "Off",
                        onClick = { showEqualizerScreen = true },
                        testTag = "settings_equalizer_item"
                    )

                    SettingsDivider()

                    // Streaming Quality Option Item
                    SettingsOptionRow(
                        icon = Icons.Outlined.HighQuality,
                        title = "Streaming Quality",
                        valueText = when (selectedQuality) {
                            StreamUrlResolver.AudioQuality.HIGH -> "320 kbps"
                            StreamUrlResolver.AudioQuality.MEDIUM -> "160 kbps"
                            StreamUrlResolver.AudioQuality.LOW -> "96 kbps"
                        },
                        onClick = { showQualityDialog = true },
                        testTag = "settings_quality_item"
                    )

                    SettingsDivider()

                    // Autoplay Option Item
                    SettingsToggleOptionRow(
                        icon = Icons.Outlined.Autorenew,
                        title = "Autoplay",
                        checked = isAutoplayEnabled,
                        onCheckedChange = { playerManager.setAutoplayEnabled(it) },
                        testTag = "settings_autoplay_item"
                    )

                    SettingsDivider()

                    // Pause on Disconnect Option Item
                    SettingsToggleOptionRow(
                        icon = Icons.Outlined.Headphones,
                        title = "Pause on Disconnect",
                        checked = pauseOnDisconnect,
                        onCheckedChange = { pauseOnDisconnect = it },
                        testTag = "settings_pause_disconnect_item"
                    )
                }
            }

            // 3. APPEARANCE & CUSTOMIZATION CATEGORY (Large Card Style Shape)
            item {
                SettingsCategoryHeader(title = "Appearance & Customization")
                SettingsLargeCard {
                    // Theme Mode Option Item
                    SettingsOptionRow(
                        icon = when (currentThemeMode) {
                            ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Outlined.LightMode
                            ThemeMode.DARK -> Icons.Outlined.DarkMode
                        },
                        title = "Theme Mode",
                        valueText = when (currentThemeMode) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        },
                        onClick = { showThemeModeDialog = true },
                        testTag = "settings_theme_mode_item"
                    )

                    // AMOLED Pure Black Option Item
                    AnimatedVisibility(visible = currentThemeMode != ThemeMode.LIGHT) {
                        Column {
                            SettingsDivider()
                            SettingsToggleOptionRow(
                                icon = Icons.Outlined.Contrast,
                                title = "AMOLED Pure Black",
                                checked = isAmoledBlack,
                                onCheckedChange = { themeManager.setAmoledBlack(it) },
                                testTag = "settings_amoled_item"
                            )
                        }
                    }

                    // Dynamic Color Option Item
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        SettingsDivider()
                        SettingsToggleOptionRow(
                            icon = Icons.Outlined.ColorLens,
                            title = "Dynamic Color",
                            checked = useDynamicColor,
                            onCheckedChange = { themeManager.setDynamicColor(it) },
                            testTag = "settings_dynamic_color_item"
                        )
                    }

                    // Accent Color Palette Option Item
                    AnimatedVisibility(visible = !useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        Column {
                            SettingsDivider()
                            SettingsOptionRow(
                                icon = Icons.Outlined.Palette,
                                title = "Accent Color",
                                valueText = accentPalette.displayName,
                                onClick = { showAccentDialog = true },
                                testTag = "settings_accent_color_item"
                            )
                        }
                    }

                    SettingsDivider()

                    // Dynamic Song Colors On / Off Option Item
                    SettingsToggleOptionRow(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Dynamic Song Colors",
                        subtitle = "Adapt app theme and accent colors to currently playing song",
                        checked = isDynamicSongThemeEnabled,
                        onCheckedChange = { themeManager.setDynamicSongThemeEnabled(it) },
                        testTag = "settings_dynamic_song_theme_item"
                    )

                    SettingsDivider()

                    // Use Device Font Toggle Switch
                    SettingsToggleOptionRow(
                        icon = Icons.Outlined.FontDownload,
                        title = "Use Device Font",
                        subtitle = "Use system default font instead of Fredoka custom font",
                        checked = useDeviceFont,
                        onCheckedChange = { themeManager.setUseDeviceFont(it) },
                        testTag = "settings_use_device_font_item"
                    )

                    SettingsDivider()

                    // Font Style Option Item
                    SettingsOptionRow(
                        icon = Icons.Outlined.TextFields,
                        title = "Font Style",
                        valueText = if (useDeviceFont) "Device System Default" else currentFontOption.displayName,
                        onClick = { if (!useDeviceFont) showFontDialog = true },
                        testTag = "settings_font_style_item"
                    )
                }
            }

            // 4. DATA & HISTORY CATEGORY (Large Card Style Shape)
            item {
                SettingsCategoryHeader(title = "Data & History")
                SettingsLargeCard {
                    // Clear Search History Option Item
                    SettingsOptionRow(
                        icon = Icons.Outlined.History,
                        title = "Clear Search History",
                        valueText = null,
                        onClick = { showClearHistoryDialog = true },
                        testTag = "settings_clear_search_history_item"
                    )

                    SettingsDivider()

                    // Clear Recently Played Option Item
                    SettingsOptionRow(
                        icon = Icons.Outlined.DeleteSweep,
                        title = "Clear Recently Played",
                        valueText = null,
                        onClick = { showClearRecentDialog = true },
                        testTag = "settings_clear_recent_item"
                    )
                }
            }

            // 5. ABOUT & INFO CATEGORY
            item {
                SettingsCategoryHeader(title = "About & Info")
                SettingsLargeCard {
                    // About App Option Item (Opens dedicated AboutAppScreen)
                    SettingsOptionRow(
                        icon = Icons.Outlined.Info,
                        title = "About App",
                        valueText = "v1.0.0",
                        onClick = { showAboutScreen = true },
                        testTag = "settings_about_app_item"
                    )

                    SettingsDivider()

                    // Open Source Licenses Option Item
                    SettingsOptionRow(
                        icon = Icons.Outlined.Description,
                        title = "Open Source Licenses",
                        valueText = "${allOpenSourceLicenses.size} Libraries",
                        onClick = { showLicensesScreen = true },
                        testTag = "settings_open_source_licenses_item"
                    )
                }
            }
        }
    }

    // Theme Mode Dialog (Only item options)
    if (showThemeModeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            title = { Text("Theme Mode", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        }
                        DialogRadioOptionItem(
                            title = label,
                            selected = currentThemeMode == mode,
                            onClick = {
                                themeManager.setThemeMode(mode)
                                showThemeModeDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeModeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Accent Color Dialog (Only item options)
    if (showAccentDialog) {
        AlertDialog(
            onDismissRequest = { showAccentDialog = false },
            title = { Text("Accent Color", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AccentPalette.entries.forEach { palette ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    themeManager.setAccentPalette(palette)
                                    showAccentDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(palette.primaryDark)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = palette.displayName,
                                fontWeight = if (accentPalette == palette) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            RadioButton(
                                selected = accentPalette == palette,
                                onClick = {
                                    themeManager.setAccentPalette(palette)
                                    showAccentDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccentDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Streaming Quality Dialog (Only item options)
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Streaming Quality", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    StreamUrlResolver.AudioQuality.entries.forEach { quality ->
                        val label = when (quality) {
                            StreamUrlResolver.AudioQuality.HIGH -> "320 kbps"
                            StreamUrlResolver.AudioQuality.MEDIUM -> "160 kbps"
                            StreamUrlResolver.AudioQuality.LOW -> "96 kbps"
                        }
                        DialogRadioOptionItem(
                            title = label,
                            selected = selectedQuality == quality,
                            onClick = {
                                selectedQuality = quality
                                showQualityDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Font Style Selection Dialog (Only 3 uploaded font options)
    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Font Style", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FontOption.entries.forEach { font ->
                        val sampleFontFamily = when (font) {
                            FontOption.FREDOKA_REGULAR -> com.example.ui.theme.FredokaRegularFontFamily
                            FontOption.FREDOKA_MEDIUM -> com.example.ui.theme.FredokaMediumFontFamily
                            FontOption.FREDOKA_SEMIBOLD -> com.example.ui.theme.FredokaSemiBoldFontFamily
                        }
                        Surface(
                            onClick = {
                                themeManager.setFontOption(font)
                                showFontDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentFontOption == font) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentFontOption == font,
                                    onClick = {
                                        themeManager.setFontOption(font)
                                        showFontDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = font.displayName,
                                        fontFamily = sampleFontFamily,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = sampleFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = font.subtitle,
                                        fontFamily = sampleFontFamily,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = sampleFontFamily,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Search History", fontWeight = FontWeight.Bold) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearSearchHistory()
                            showClearHistoryDialog = false
                            snackbarHostState.showSnackbar("Search history cleared")
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Recent Dialog
    if (showClearRecentDialog) {
        AlertDialog(
            onDismissRequest = { showClearRecentDialog = false },
            title = { Text("Clear Recently Played", fontWeight = FontWeight.Bold) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearRecentlyPlayed()
                            showClearRecentDialog = false
                            snackbarHostState.showSnackbar("Recently played cleared")
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearRecentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            letterSpacing = 0.9.sp,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
    )
}

@Composable
private fun SettingsLargeCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF191924).copy(alpha = 0.75f)
            else Color.White.copy(alpha = 0.85f)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.16f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    )
}

@Composable
private fun SettingsIconSquareBox(
    icon: ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector,
    title: String,
    valueText: String?,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconSquareBox(icon = icon, contentDescription = title)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (!valueText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SettingsToggleOptionRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconSquareBox(icon = icon, contentDescription = title)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DialogRadioOptionItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

data class OpenSourceLicenseItem(
    val id: String,
    val name: String,
    val category: String,
    val version: String,
    val author: String,
    val licenseName: String,
    val description: String,
    val projectUrl: String,
    val licenseUrl: String,
    val icon: ImageVector
)

private val allOpenSourceLicenses = listOf(
    OpenSourceLicenseItem(
        id = "jetpack_compose",
        name = "Jetpack Compose & Material 3",
        category = "UI & Compose",
        version = "BOM 2024.09.00",
        author = "Google / AndroidX",
        licenseName = "Apache-2.0",
        description = "Modern declarative UI toolkit and Material Design 3 components for Android.",
        projectUrl = "https://developer.android.com/jetpack/compose",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        icon = Icons.Outlined.Layers
    ),
    OpenSourceLicenseItem(
        id = "haze_blur",
        name = "Haze Blur & Liquid Glass",
        category = "UI & Compose",
        version = "2.0.1",
        author = "Chris Banes",
        licenseName = "Apache-2.0",
        description = "Hardware-accelerated glassmorphism and background blur effects for Compose.",
        projectUrl = "https://github.com/chrisbanes/haze",
        licenseUrl = "https://github.com/chrisbanes/haze/blob/main/LICENSE",
        icon = Icons.Outlined.BlurOn
    ),
    OpenSourceLicenseItem(
        id = "media3_exoplayer",
        name = "AndroidX Media3 ExoPlayer",
        category = "Audio & Media",
        version = "1.5.1",
        author = "Google / AndroidX",
        licenseName = "Apache-2.0",
        description = "High-performance media playback engine, MediaSession, and audio streaming pipeline.",
        projectUrl = "https://github.com/androidx/media",
        licenseUrl = "https://github.com/androidx/media/blob/release/LICENSE",
        icon = Icons.Outlined.GraphicEq
    ),
    OpenSourceLicenseItem(
        id = "coil_compose",
        name = "Coil Compose",
        category = "UI & Compose",
        version = "2.7.0",
        author = "Coil Contributors",
        licenseName = "Apache-2.0",
        description = "Coroutine-based image loading library for Android and Jetpack Compose.",
        projectUrl = "https://github.com/coil-kt/coil",
        licenseUrl = "https://github.com/coil-kt/coil/blob/main/LICENSE.txt",
        icon = Icons.Outlined.Image
    ),
    OpenSourceLicenseItem(
        id = "retrofit",
        name = "Retrofit 2",
        category = "Networking & Data",
        version = "2.12.0",
        author = "Square, Inc.",
        licenseName = "Apache-2.0",
        description = "Type-safe HTTP client for Android and Kotlin REST API integration.",
        projectUrl = "https://github.com/square/retrofit",
        licenseUrl = "https://github.com/square/retrofit/blob/trunk/LICENSE.txt",
        icon = Icons.Outlined.CloudSync
    ),
    OpenSourceLicenseItem(
        id = "okhttp",
        name = "OkHttp & Logging Interceptor",
        category = "Networking & Data",
        version = "4.10.0",
        author = "Square, Inc.",
        licenseName = "Apache-2.0",
        description = "Resilient HTTP/2 client with connection pooling, caching, and stream handling.",
        projectUrl = "https://github.com/square/okhttp",
        licenseUrl = "https://github.com/square/okhttp/blob/master/LICENSE.txt",
        icon = Icons.Outlined.Http
    ),
    OpenSourceLicenseItem(
        id = "room_database",
        name = "AndroidX Room Database",
        category = "Networking & Data",
        version = "2.7.0",
        author = "Google / AndroidX",
        licenseName = "Apache-2.0",
        description = "SQLite object mapping library for offline tracks, playlists, and history persistence.",
        projectUrl = "https://developer.android.com/training/data-storage/room",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        icon = Icons.Outlined.Storage
    ),
    OpenSourceLicenseItem(
        id = "kotlin_coroutines",
        name = "Kotlin & Kotlinx Coroutines",
        category = "Networking & Data",
        version = "1.10.2",
        author = "JetBrains",
        licenseName = "Apache-2.0",
        description = "Asynchronous reactive programming with StateFlow, SharedFlow, and structured concurrency.",
        projectUrl = "https://github.com/Kotlin/kotlinx.coroutines",
        licenseUrl = "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt",
        icon = Icons.Outlined.Code
    ),
    OpenSourceLicenseItem(
        id = "moshi_gson",
        name = "Moshi & Google Gson",
        category = "Networking & Data",
        version = "1.15.2 / 2.11.0",
        author = "Square & Google",
        licenseName = "Apache-2.0",
        description = "Modern JSON serialization and deserialization libraries for Kotlin and Java.",
        projectUrl = "https://github.com/square/moshi",
        licenseUrl = "https://github.com/square/moshi/blob/master/LICENSE.txt",
        icon = Icons.Outlined.DataObject
    ),
    OpenSourceLicenseItem(
        id = "androidx_lifecycle",
        name = "AndroidX Lifecycle & Core KTX",
        category = "UI & Compose",
        version = "2.8.7 / 1.18.0",
        author = "Google / AndroidX",
        licenseName = "Apache-2.0",
        description = "Lifecycle-aware ViewModel, Compose state collection, and Android core extensions.",
        projectUrl = "https://developer.android.com/jetpack/androidx/releases/lifecycle",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        icon = Icons.Outlined.Extension
    ),
    OpenSourceLicenseItem(
        id = "fredoka_font",
        name = "Fredoka Font Family",
        category = "Fonts & Design",
        version = "OFL 1.1",
        author = "Ben Nathan, Milena B. Brandão / Google Fonts",
        licenseName = "OFL-1.1",
        description = "Rounded, modern, semi-expanded typeface family bundled with Regular, Medium, and SemiBold styles.",
        projectUrl = "https://fonts.google.com/specimen/Fredoka",
        licenseUrl = "https://openfontlicense.org",
        icon = Icons.Outlined.FontDownload
    ),
    OpenSourceLicenseItem(
        id = "material_icons",
        name = "Material Symbols & Icons",
        category = "Fonts & Design",
        version = "Material 3",
        author = "Google Design",
        licenseName = "Apache-2.0",
        description = "Official Material Design 3 vector icons and symbols for Android.",
        projectUrl = "https://fonts.google.com/icons",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        icon = Icons.Outlined.AutoAwesome
    ),
    OpenSourceLicenseItem(
        id = "google_ksp",
        name = "Kotlin Symbol Processing (KSP)",
        category = "Build & Tooling",
        version = "2.2.10-2.0.2",
        author = "Google",
        licenseName = "Apache-2.0",
        description = "Compiler plugin API used for Room and Moshi code generation.",
        projectUrl = "https://github.com/google/ksp",
        licenseUrl = "https://github.com/google/ksp/blob/main/LICENSE",
        icon = Icons.Outlined.Build
    ),
    OpenSourceLicenseItem(
        id = "secrets_gradle_plugin",
        name = "Secrets Gradle Plugin",
        category = "Build & Tooling",
        version = "2.0.1",
        author = "Google Maps Platform",
        licenseName = "Apache-2.0",
        description = "Gradle plugin for securely providing environment properties and BuildConfig keys.",
        projectUrl = "https://github.com/google/secrets-gradle-plugin",
        licenseUrl = "https://github.com/google/secrets-gradle-plugin/blob/main/LICENSE",
        icon = Icons.Outlined.VpnKey
    ),
    OpenSourceLicenseItem(
        id = "lrclib_lyrics",
        name = "LRCLIB Synced Lyrics",
        category = "Audio & Media",
        version = "API v1",
        author = "Tran Xuan Thang",
        licenseName = "MIT",
        description = "Open-source community database providing time-synced LRC and plain lyrics.",
        projectUrl = "https://github.com/tranxuanthang/lrclib",
        licenseUrl = "https://github.com/tranxuanthang/lrclib/blob/main/LICENSE",
        icon = Icons.Outlined.Lyrics
    ),
    OpenSourceLicenseItem(
        id = "aosp_audiofx",
        name = "Android Open Source Project (AOSP)",
        category = "Audio & Media",
        version = "Android SDK 36",
        author = "Google / AOSP",
        licenseName = "Apache-2.0",
        description = "Core Android OS framework, AudioEffect Equalizer, BassBoost, and system media APIs.",
        projectUrl = "https://source.android.com",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0",
        icon = Icons.Outlined.PhoneAndroid
    )
)

@Composable
private fun SettingsDirectLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit,
    onLicenseClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconSquareBox(icon = icon, contentDescription = title)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            onClick = onLicenseClick,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = "Open Direct Link",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val uriHandler = LocalUriHandler.current
    val categories = remember {
        listOf("All") + allOpenSourceLicenses.map { it.category }.distinct()
    }
    var selectedCategory by remember { mutableStateOf("All") }

    val filteredLicenses = remember(selectedCategory) {
        if (selectedCategory == "All") {
            allOpenSourceLicenses
        } else {
            allOpenSourceLicenses.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licenses", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 165.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category,
                                    fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Direct Links for Official Licenses under a single Large Card Layout
            item {
                SettingsCategoryHeader(title = "Standard License Texts (Direct Links)")
                SettingsLargeCard {
                    SettingsDirectLinkRow(
                        icon = Icons.Outlined.Gavel,
                        title = "Apache License 2.0",
                        subtitle = "apache.org/licenses/LICENSE-2.0",
                        badgeText = "Apache-2.0",
                        onClick = {
                            runCatching { uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0") }
                        },
                        onLicenseClick = {
                            runCatching { uriHandler.openUri("https://www.apache.org/licenses/LICENSE-2.0") }
                        },
                        testTag = "standard_license_apache"
                    )
                    SettingsDivider()
                    SettingsDirectLinkRow(
                        icon = Icons.Outlined.FontDownload,
                        title = "SIL Open Font License 1.1",
                        subtitle = "openfontlicense.org",
                        badgeText = "OFL-1.1",
                        onClick = {
                            runCatching { uriHandler.openUri("https://openfontlicense.org") }
                        },
                        onLicenseClick = {
                            runCatching { uriHandler.openUri("https://openfontlicense.org") }
                        },
                        testTag = "standard_license_ofl"
                    )
                    SettingsDivider()
                    SettingsDirectLinkRow(
                        icon = Icons.Outlined.Verified,
                        title = "MIT Open Source License",
                        subtitle = "opensource.org/licenses/MIT",
                        badgeText = "MIT",
                        onClick = {
                            runCatching { uriHandler.openUri("https://opensource.org/licenses/MIT") }
                        },
                        onLicenseClick = {
                            runCatching { uriHandler.openUri("https://opensource.org/licenses/MIT") }
                        },
                        testTag = "standard_license_mit"
                    )
                }
            }

            // All Open Source Libraries grouped together inside ONE Large Card Layout
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsCategoryHeader(title = "All Libraries & Direct Links (${filteredLicenses.size})")
                SettingsLargeCard {
                    filteredLicenses.forEachIndexed { index, item ->
                        if (index > 0) {
                            SettingsDivider()
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    runCatching { uriHandler.openUri(item.projectUrl) }
                                }
                                .padding(horizontal = 18.dp, vertical = 16.dp)
                                .testTag("license_card_${item.id}"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SettingsIconSquareBox(
                                    icon = item.icon,
                                    contentDescription = item.name
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.author} • v${item.version}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    onClick = {
                                        runCatching { uriHandler.openUri(item.licenseUrl) }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = item.licenseName,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        runCatching { uriHandler.openUri(item.projectUrl) }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Direct Project Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        runCatching { uriHandler.openUri(item.licenseUrl) }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("License Link", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
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
fun AboutAppScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("About App", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 165.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. APP LOGO, NAME & VERSION CARD
            item {
                SettingsCategoryHeader(title = "Application Overview")
                SettingsLargeCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(MiniPlayerScallopedShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = "SMusic App Icon",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SMusic",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 26.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Version 1.0.0 (Build 2026.10)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.5.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Text(
                            text = "Expressive Material 3 Music Player for Android with synced lyrics, offline equalizer, dynamic theme palettes, and seamless audio playback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. DEVELOPER LEAD CARD
            item {
                SettingsCategoryHeader(title = "Developer Lead")
                SettingsLargeCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "DEVELOPER LEAD",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.5.sp
                                )
                            )
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    RoundedCornerShape(22.dp)
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(MiniPlayerScallopedShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                        )
                                    )
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), MiniPlayerScallopedShape),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                        .data("https://avatars.githubusercontent.com/u/257059002?v=4")
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Developer Lead Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MiniPlayerScallopedShape),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "—͟͞͞ 𝙔ᴀᴅᴀᴠ<\\>x- 🇮🇳",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Lead Developer & Creator",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }

                        // Social Buttons Row (GitHub & Telegram)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // GitHub Button
                            Surface(
                                onClick = {
                                    runCatching { uriHandler.openUri("https://github.com/Dev0Yadavx") }
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("about_github_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = GitHubIcon,
                                        contentDescription = "GitHub",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GitHub",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Telegram Button
                            Surface(
                                onClick = {
                                    runCatching { uriHandler.openUri("https://t.me/YADAVXAHIR") }
                                },
                                shape = RoundedCornerShape(18.dp),
                                color = Color(0xFF0088CC).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFF0088CC).copy(alpha = 0.40f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("about_telegram_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = TelegramIcon,
                                        contentDescription = "Telegram",
                                        tint = Color(0xFF0088CC),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Telegram",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        ),
                                        color = Color(0xFF0088CC)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. SYSTEM & ENGINE INFORMATION CARD
            item {
                SettingsCategoryHeader(title = "App Features & Stack")
                SettingsLargeCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        SettingsOptionRow(
                            icon = Icons.Outlined.Layers,
                            title = "UI Framework",
                            valueText = "Jetpack Compose M3",
                            onClick = {},
                            testTag = "about_ui_framework"
                        )
                        SettingsDivider()
                        SettingsOptionRow(
                            icon = Icons.Outlined.GraphicEq,
                            title = "Playback Engine",
                            valueText = "Media3 ExoPlayer",
                            onClick = {},
                            testTag = "about_playback_engine"
                        )
                        SettingsDivider()
                        SettingsOptionRow(
                            icon = Icons.Outlined.Lyrics,
                            title = "Lyrics Engine",
                            valueText = "LRCLIB Synced",
                            onClick = {},
                            testTag = "about_lyrics_engine"
                        )
                        SettingsDivider()
                        SettingsOptionRow(
                            icon = Icons.Outlined.Palette,
                            title = "Theme Engine",
                            valueText = "Dynamic & Pure OLED",
                            onClick = {},
                            testTag = "about_theme_engine"
                        )
                    }
                }
            }
        }
    }
}
