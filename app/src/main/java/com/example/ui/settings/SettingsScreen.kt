package com.example.ui.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.StreamUrlResolver
import com.example.player.EqualizerManager
import com.example.player.PlayerManager
import com.example.repository.MusicRepository
import com.example.ui.common.UserAvatarBadge
import com.example.ui.common.UserProfileM3CardDialog
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.FontOption
import com.example.ui.theme.NowPlayingStyle
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.launch

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
    val equalizerManager = remember { EqualizerManager.getInstance(context) }
    var showEqualizerScreen by rememberSaveable { mutableStateOf(false) }

    if (showEqualizerScreen) {
        EqualizerScreen(
            onBack = { showEqualizerScreen = false },
            equalizerManager = equalizerManager,
            modifier = modifier
        )
        return
    }

    BackHandler { onBack() }
    val scope = rememberCoroutineScope()

    val currentThemeMode by themeManager.themeMode.collectAsState()
    val useDynamicColor by themeManager.useDynamicColor.collectAsState()
    val isAmoledBlack by themeManager.isAmoledBlack.collectAsState()
    val accentPalette by themeManager.accentPalette.collectAsState()
    val currentFontOption by themeManager.fontOption.collectAsState()
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
                }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Profile Card (Avatar + Nickname, no extra text)
            item {
                Card(
                    onClick = { showProfileCardDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_profile_m3_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            UserAvatarBadge(
                                emoji = userAvatarEmoji,
                                customImageUri = userAvatarImageUri,
                                size = 48.dp,
                                fontSize = 22.sp
                            )
                            Text(
                                text = userNickname,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                        FilledTonalIconButton(
                            onClick = { showProfileCardDialog = true },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Profile",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. Clean Item Options List (Audio, Equalizer, Appearance, Playback, Data)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
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

                        SettingsDivider()

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

                        // Font Style Option Item
                        SettingsOptionRow(
                            icon = Icons.Outlined.TextFields,
                            title = "Font Style",
                            valueText = currentFontOption.displayName,
                            onClick = { showFontDialog = true },
                            testTag = "settings_font_style_item"
                        )

                        SettingsDivider()

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

    // Font Style Selection Dialog (Only item options)
    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Font Style", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    FontOption.entries.forEach { font ->
                        DialogRadioOptionItem(
                            title = font.displayName,
                            selected = currentFontOption == font,
                            onClick = {
                                themeManager.setFontOption(font)
                                showFontDialog = false
                            }
                        )
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
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
    )
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
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (!valueText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsToggleOptionRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
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
