package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AccentPalette(
    val id: String,
    val displayName: String,
    val primaryDark: Color,
    val primaryLight: Color,
    val secondary: Color,
    val tertiary: Color
) {
    VIOLET(
        id = "violet",
        displayName = "Electric Violet",
        primaryDark = Color(0xFFA78BFA),
        primaryLight = Color(0xFF7C3AED),
        secondary = Color(0xFFF472B6),
        tertiary = Color(0xFF38BDF8)
    ),
    OCEAN(
        id = "ocean",
        displayName = "Pixel Ocean",
        primaryDark = Color(0xFF38BDF8),
        primaryLight = Color(0xFF0284C7),
        secondary = Color(0xFF818CF8),
        tertiary = Color(0xFFF472B6)
    ),
    INDIGO(
        id = "indigo",
        displayName = "Deep Indigo",
        primaryDark = Color(0xFF818CF8),
        primaryLight = Color(0xFF4F46E5),
        secondary = Color(0xFFC084FC),
        tertiary = Color(0xFF38BDF8)
    ),
    SUNSET(
        id = "sunset",
        displayName = "Sunset Amber",
        primaryDark = Color(0xFFFBBF24),
        primaryLight = Color(0xFFD97706),
        secondary = Color(0xFFFB923C),
        tertiary = Color(0xFFF43F5E)
    ),
    ROSE(
        id = "rose",
        displayName = "Electric Rose",
        primaryDark = Color(0xFFFB7185),
        primaryLight = Color(0xFFE11D48),
        secondary = Color(0xFFA855F7),
        tertiary = Color(0xFF38BDF8)
    );

    companion object {
        fun fromId(id: String): AccentPalette {
            return entries.firstOrNull { it.id == id } ?: VIOLET
        }
    }
}

enum class NowPlayingStyle(
    val id: String,
    val displayName: String,
    val description: String
) {
    IMMERSIVE_POSTER(
        id = "immersive_poster",
        displayName = "Immersive Poster",
        description = "Full canvas artwork with blur gradient scrim & glass controls"
    ),
    VINYL_DISC(
        id = "vinyl_disc",
        displayName = "Vinyl Disc",
        description = "Circular disc artwork with quick action tool row & squircle play"
    ),
    MODERN_CARD(
        id = "modern_card",
        displayName = "Modern Card",
        description = "Classic elevated album card with fluid timeline & studio controls"
    );

    companion object {
        fun fromId(id: String): NowPlayingStyle {
            return entries.firstOrNull { it.id == id } ?: IMMERSIVE_POSTER
        }
    }
}

class ThemeManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        "smusic_theme_preferences",
        Context.MODE_PRIVATE
    )

    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(
        prefs.getBoolean(KEY_DYNAMIC_COLOR, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    )
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    private val _isAmoledBlack = MutableStateFlow(
        prefs.getBoolean(KEY_AMOLED_BLACK, false)
    )
    val isAmoledBlack: StateFlow<Boolean> = _isAmoledBlack.asStateFlow()

    private val _isLiquidGlassEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_LIQUID_GLASS_ENABLED, false)
    )
    val isLiquidGlassEnabled: StateFlow<Boolean> = _isLiquidGlassEnabled.asStateFlow()

    private val _isDynamicSongBackgroundEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DYNAMIC_SONG_BACKGROUND, false)
    )
    val isDynamicSongBackgroundEnabled: StateFlow<Boolean> = _isDynamicSongBackgroundEnabled.asStateFlow()

    private val _isDynamicSongThemeEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DYNAMIC_SONG_THEME, true)
    )
    val isDynamicSongThemeEnabled: StateFlow<Boolean> = _isDynamicSongThemeEnabled.asStateFlow()

    private val _accentPalette = MutableStateFlow(
        AccentPalette.fromId(prefs.getString(KEY_ACCENT_PALETTE, AccentPalette.VIOLET.id) ?: AccentPalette.VIOLET.id)
    )
    val accentPalette: StateFlow<AccentPalette> = _accentPalette.asStateFlow()

    private val _fontOption = MutableStateFlow(
        FontOption.fromId(prefs.getString(KEY_FONT_OPTION, FontOption.FREDOKA_REGULAR.id) ?: FontOption.FREDOKA_REGULAR.id)
    )
    val fontOption: StateFlow<FontOption> = _fontOption.asStateFlow()

    private val _useDeviceFont = MutableStateFlow(
        prefs.getBoolean(KEY_USE_DEVICE_FONT, false)
    )
    val useDeviceFont: StateFlow<Boolean> = _useDeviceFont.asStateFlow()

    private val _nowPlayingStyle = MutableStateFlow(
        NowPlayingStyle.fromId(prefs.getString(KEY_NOW_PLAYING_STYLE, NowPlayingStyle.IMMERSIVE_POSTER.id) ?: NowPlayingStyle.IMMERSIVE_POSTER.id)
    )
    val nowPlayingStyle: StateFlow<NowPlayingStyle> = _nowPlayingStyle.asStateFlow()

    private val _userNickname = MutableStateFlow(
        prefs.getString(KEY_USER_NICKNAME, "Music Lover") ?: "Music Lover"
    )
    val userNickname: StateFlow<String> = _userNickname.asStateFlow()

    private val _userAvatarEmoji = MutableStateFlow(
        prefs.getString(KEY_USER_AVATAR_EMOJI, "🎧") ?: "🎧"
    )
    val userAvatarEmoji: StateFlow<String> = _userAvatarEmoji.asStateFlow()

    private val _userAvatarImageUri = MutableStateFlow(
        prefs.getString(KEY_USER_AVATAR_IMAGE_URI, null)
    )
    val userAvatarImageUri: StateFlow<String?> = _userAvatarImageUri.asStateFlow()

    private val _hasAgreedPermissions = MutableStateFlow(
        prefs.getBoolean(KEY_HAS_AGREED_PERMISSIONS, true)
    )
    val hasAgreedPermissions: StateFlow<Boolean> = _hasAgreedPermissions.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _useDynamicColor.value = enabled
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun setAmoledBlack(enabled: Boolean) {
        _isAmoledBlack.value = enabled
        prefs.edit().putBoolean(KEY_AMOLED_BLACK, enabled).apply()
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        _isLiquidGlassEnabled.value = enabled
        prefs.edit().putBoolean(KEY_LIQUID_GLASS_ENABLED, enabled).apply()
    }

    fun setDynamicSongBackgroundEnabled(enabled: Boolean) {
        _isDynamicSongBackgroundEnabled.value = enabled
        prefs.edit().putBoolean(KEY_DYNAMIC_SONG_BACKGROUND, enabled).apply()
    }

    fun setDynamicSongThemeEnabled(enabled: Boolean) {
        _isDynamicSongThemeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_DYNAMIC_SONG_THEME, enabled).apply()
    }

    fun setAccentPalette(palette: AccentPalette) {
        _accentPalette.value = palette
        prefs.edit().putString(KEY_ACCENT_PALETTE, palette.id).apply()
    }

    fun setFontOption(font: FontOption) {
        _fontOption.value = font
        prefs.edit().putString(KEY_FONT_OPTION, font.id).apply()
    }

    fun setUseDeviceFont(enabled: Boolean) {
        _useDeviceFont.value = enabled
        prefs.edit().putBoolean(KEY_USE_DEVICE_FONT, enabled).apply()
    }

    fun setNowPlayingStyle(style: NowPlayingStyle) {
        _nowPlayingStyle.value = style
        prefs.edit().putString(KEY_NOW_PLAYING_STYLE, style.id).apply()
    }

    fun setUserNickname(nickname: String) {
        val clean = nickname.trim().ifEmpty { "Music Lover" }
        _userNickname.value = clean
        prefs.edit().putString(KEY_USER_NICKNAME, clean).apply()
    }

    fun setUserAvatarEmoji(emoji: String) {
        _userAvatarEmoji.value = emoji
        _userAvatarImageUri.value = null
        prefs.edit()
            .putString(KEY_USER_AVATAR_EMOJI, emoji)
            .remove(KEY_USER_AVATAR_IMAGE_URI)
            .apply()
    }

    fun setUserAvatarCustomImage(context: Context, uri: Uri) {
        try {
            val avatarsDir = File(context.applicationContext.filesDir, "avatars")
            if (!avatarsDir.exists()) avatarsDir.mkdirs()
            // Clean up older avatar files
            avatarsDir.listFiles()?.forEach { it.delete() }
            val destFile = File(avatarsDir, "avatar_${System.currentTimeMillis()}.jpg")
            context.applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            val savedPath = destFile.absolutePath
            _userAvatarImageUri.value = savedPath
            prefs.edit().putString(KEY_USER_AVATAR_IMAGE_URI, savedPath).apply()
        } catch (e: Exception) {
            // Fallback to URI string if file copy fails
            val uriStr = uri.toString()
            _userAvatarImageUri.value = uriStr
            prefs.edit().putString(KEY_USER_AVATAR_IMAGE_URI, uriStr).apply()
        }
    }

    fun setHasAgreedPermissions(agreed: Boolean) {
        _hasAgreedPermissions.value = agreed
        prefs.edit().putBoolean(KEY_HAS_AGREED_PERMISSIONS, agreed).apply()
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        private const val KEY_AMOLED_BLACK = "key_amoled_black"
        private const val KEY_LIQUID_GLASS_ENABLED = "key_liquid_glass_enabled"
        private const val KEY_DYNAMIC_SONG_BACKGROUND = "key_dynamic_song_background"
        private const val KEY_DYNAMIC_SONG_THEME = "key_dynamic_song_theme_v1"
        private const val KEY_ACCENT_PALETTE = "key_accent_palette"
        private const val KEY_FONT_OPTION = "key_font_option_v3"
        private const val KEY_USE_DEVICE_FONT = "key_use_device_font"
        private const val KEY_NOW_PLAYING_STYLE = "key_now_playing_style"
        private const val KEY_USER_NICKNAME = "key_user_nickname"
        private const val KEY_USER_AVATAR_EMOJI = "key_user_avatar_emoji"
        private const val KEY_USER_AVATAR_IMAGE_URI = "key_user_avatar_image_uri"
        private const val KEY_HAS_AGREED_PERMISSIONS = "key_m3_permission_agreed_v2"

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context).also { instance = it }
            }
        }
    }
}
