package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

@Composable
fun buildExpressiveDarkColorScheme(
    accent: AccentPalette,
    isAmoled: Boolean,
    overridePrimary: Color? = null,
    overrideSecondary: Color? = null
): ColorScheme {
    val prim = overridePrimary ?: accent.primaryDark
    val sec = overrideSecondary ?: accent.secondary
    val bg = if (isAmoled) AmoledDarkBackground else PixelDarkBackground
    val surf = if (isAmoled) AmoledDarkSurface else PixelDarkSurface
    val surfVar = if (isAmoled) AmoledDarkSurfaceVariant else PixelDarkSurfaceVariant
    val surfCard = if (isAmoled) AmoledDarkSurfaceCard else PixelDarkSurfaceCard

    val onPrim = if (prim.luminance() > 0.5f) Color(0xFF10141D) else Color.White
    val onSec = if (sec.luminance() > 0.5f) Color(0xFF10141D) else Color.White

    return darkColorScheme(
        primary = prim,
        onPrimary = onPrim,
        primaryContainer = surfVar,
        onPrimaryContainer = Color.White,
        secondary = sec,
        onSecondary = onSec,
        secondaryContainer = surfCard,
        onSecondaryContainer = Color.White,
        tertiary = accent.tertiary,
        onTertiary = Color.White,
        background = bg,
        onBackground = Color.White,
        surface = surf,
        onSurface = Color.White,
        surfaceVariant = surfVar,
        onSurfaceVariant = Color(0xFFCBD5E1),
        surfaceContainer = surfCard,
        surfaceContainerHigh = surfCard,
        surfaceContainerHighest = surfVar,
        outline = Color(0xFF2E384D),
        error = Color(0xFFF87171),
        onError = Color.White
    )
}

@Composable
fun buildExpressiveLightColorScheme(
    accent: AccentPalette,
    overridePrimary: Color? = null,
    overrideSecondary: Color? = null
): ColorScheme {
    val prim = overridePrimary ?: accent.primaryLight
    val sec = overrideSecondary ?: accent.secondary

    val onPrim = if (prim.luminance() > 0.5f) Color(0xFF10141D) else Color.White
    val onSec = if (sec.luminance() > 0.5f) Color(0xFF10141D) else Color.White

    return lightColorScheme(
        primary = prim,
        onPrimary = onPrim,
        primaryContainer = Color(0xFFF3E8FF),
        onPrimaryContainer = PixelLightPrimary,
        secondary = sec,
        onSecondary = onSec,
        secondaryContainer = Color.White,
        onSecondaryContainer = PixelLightTextPrimary,
        tertiary = accent.tertiary,
        onTertiary = Color.White,
        background = PixelLightBackground,
        onBackground = PixelLightTextPrimary,
        surface = Color.White,
        onSurface = PixelLightTextPrimary,
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = PixelLightTextSecondary,
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color.White,
        surfaceContainerHighest = Color.White,
        outline = Color(0xFFCBD5E1),
        error = Color(0xFFDC2626),
        onError = Color.White
    )
}

@Composable
fun animateColorScheme(target: ColorScheme): ColorScheme {
    val primary by animateColorAsState(target.primary, animationSpec = tween(550), label = "c_prim")
    val onPrimary by animateColorAsState(target.onPrimary, animationSpec = tween(550), label = "c_onprim")
    val primaryContainer by animateColorAsState(target.primaryContainer, animationSpec = tween(550), label = "c_prim_c")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, animationSpec = tween(550), label = "c_onprim_c")
    val secondary by animateColorAsState(target.secondary, animationSpec = tween(550), label = "c_sec")
    val onSecondary by animateColorAsState(target.onSecondary, animationSpec = tween(550), label = "c_onsec")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, animationSpec = tween(550), label = "c_sec_c")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, animationSpec = tween(550), label = "c_onsec_c")
    val tertiary by animateColorAsState(target.tertiary, animationSpec = tween(550), label = "c_tert")
    val onTertiary by animateColorAsState(target.onTertiary, animationSpec = tween(550), label = "c_ontert")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, animationSpec = tween(550), label = "c_tert_c")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, animationSpec = tween(550), label = "c_ontert_c")
    val background by animateColorAsState(target.background, animationSpec = tween(550), label = "c_bg")
    val onBackground by animateColorAsState(target.onBackground, animationSpec = tween(550), label = "c_onbg")
    val surface by animateColorAsState(target.surface, animationSpec = tween(550), label = "c_surf")
    val onSurface by animateColorAsState(target.onSurface, animationSpec = tween(550), label = "c_onsurf")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, animationSpec = tween(550), label = "c_surf_v")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, animationSpec = tween(550), label = "c_onsurf_v")
    val surfaceContainer by animateColorAsState(target.surfaceContainer, animationSpec = tween(550), label = "c_surf_cnt")
    val surfaceContainerHigh by animateColorAsState(target.surfaceContainerHigh, animationSpec = tween(550), label = "c_surf_cnth")
    val surfaceContainerHighest by animateColorAsState(target.surfaceContainerHighest, animationSpec = tween(550), label = "c_surf_cnthh")
    val outline by animateColorAsState(target.outline, animationSpec = tween(550), label = "c_outl")
    val outlineVariant by animateColorAsState(target.outlineVariant, animationSpec = tween(550), label = "c_outlv")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        outline = outline,
        outlineVariant = outlineVariant
    )
}

@Composable
fun SMusicTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = true,
    isAmoledBlack: Boolean = false,
    accentPalette: AccentPalette = AccentPalette.VIOLET,
    fontOption: FontOption = FontOption.FREDOKA_REGULAR,
    useDeviceFont: Boolean = false,
    songSeedColor: Color? = null,
    songPrimaryColor: Color? = null,
    songSecondaryColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (context as? android.app.Activity)?.window
            if (window != null) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDarkTheme
                insetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    val rawColorScheme = when {
        songSeedColor != null -> {
            if (isDarkTheme) {
                MaterialYouPaletteGenerator.buildDarkColorScheme(
                    seedColor = songSeedColor,
                    isAmoled = isAmoledBlack
                )
            } else {
                MaterialYouPaletteGenerator.buildLightColorScheme(
                    seedColor = songSeedColor
                )
            }
        }
        songPrimaryColor != null -> {
            if (isDarkTheme) {
                MaterialYouPaletteGenerator.buildDarkColorScheme(
                    seedColor = songPrimaryColor,
                    isAmoled = isAmoledBlack
                )
            } else {
                MaterialYouPaletteGenerator.buildLightColorScheme(
                    seedColor = songPrimaryColor
                )
            }
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val baseScheme = if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (isDarkTheme) {
                if (isAmoledBlack) {
                    baseScheme.copy(
                        background = AmoledDarkBackground,
                        onBackground = Color.White,
                        surface = AmoledDarkSurface,
                        onSurface = Color.White,
                        surfaceVariant = AmoledDarkSurfaceVariant,
                        onSurfaceVariant = Color(0xFFCBD5E1),
                        onPrimaryContainer = Color.White,
                        onSecondaryContainer = Color.White
                    )
                } else {
                    baseScheme.copy(
                        onBackground = Color.White,
                        onSurface = Color.White,
                        onSurfaceVariant = Color(0xFFCBD5E1),
                        onPrimaryContainer = Color.White,
                        onSecondaryContainer = Color.White
                    )
                }
            } else {
                baseScheme.copy(
                    onBackground = PixelLightTextPrimary,
                    surface = Color.White,
                    surfaceContainer = Color.White,
                    surfaceContainerHigh = Color.White,
                    surfaceContainerHighest = Color.White,
                    secondaryContainer = Color.White,
                    onSurface = PixelLightTextPrimary,
                    onSurfaceVariant = PixelLightTextSecondary,
                    onPrimaryContainer = PixelLightPrimary,
                    onSecondaryContainer = PixelLightTextPrimary
                )
            }
        }
        isDarkTheme -> buildExpressiveDarkColorScheme(accentPalette, isAmoledBlack)
        else -> buildExpressiveLightColorScheme(accentPalette)
    }

    val colorScheme = animateColorScheme(rawColorScheme)
    val typography = getAppTypography(fontOption, useDeviceFont)

    androidx.compose.runtime.CompositionLocalProvider(
        LocalContentColor provides colorScheme.onBackground
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

