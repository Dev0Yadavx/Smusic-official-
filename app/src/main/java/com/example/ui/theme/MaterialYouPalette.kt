package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min

/**
 * Material Design 3 / Material You Tonal Palette Generator
 * Generates full M3 tonal color schemes dynamically from a song's seed color.
 */
object MaterialYouPaletteGenerator {

    /**
     * Converts RGB to HSL
     * h in [0..360], s in [0..1], l in [0..1]
     */
    fun rgbToHsl(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue

        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val delta = max - min

        var h = 0f
        var s = 0f
        val l = (max + min) / 2f

        if (delta > 0.0001f) {
            s = if (l <= 0.5f) delta / (max + min) else delta / (2f - max - min)
            h = when (max) {
                r -> ((g - b) / delta + (if (g < b) 6f else 0f))
                g -> ((b - r) / delta + 2f)
                else -> ((r - g) / delta + 4f)
            } * 60f
        }

        return floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    }

    /**
     * Converts HSL to Compose Color
     */
    fun hslToColor(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
        val normH = (h % 360f + 360f) % 360f
        val normS = s.coerceIn(0f, 1f)
        val normL = l.coerceIn(0f, 1f)

        val c = (1f - kotlin.math.abs(2f * normL - 1f)) * normS
        val x = c * (1f - kotlin.math.abs((normH / 60f) % 2f - 1f))
        val m = normL - c / 2f

        val (rPrime, gPrime, bPrime) = when {
            normH < 60f -> Triple(c, x, 0f)
            normH < 120f -> Triple(x, c, 0f)
            normH < 180f -> Triple(0f, c, x)
            normH < 240f -> Triple(0f, x, c)
            normH < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = (rPrime + m).coerceIn(0f, 1f),
            green = (gPrime + m).coerceIn(0f, 1f),
            blue = (bPrime + m).coerceIn(0f, 1f),
            alpha = alpha
        )
    }

    /**
     * Generates a tonal color given a base hue, desired saturation, and tone level [0..100]
     */
    fun tone(hue: Float, saturation: Float, toneLevel: Int): Color {
        val lightness = (toneLevel.coerceIn(0, 100)) / 100f
        return hslToColor(hue, saturation, lightness)
    }

    /**
     * Builds Material 3 Dark ColorScheme from seed color
     */
    fun buildDarkColorScheme(seedColor: Color, isAmoled: Boolean = false): ColorScheme {
        val hsl = rgbToHsl(seedColor)
        val primaryHue = hsl[0]
        val primarySat = hsl[1].coerceAtLeast(0.45f) // ensure vibrancy

        // Secondary has moderate saturation
        val secondaryHue = primaryHue
        val secondarySat = (primarySat * 0.40f).coerceIn(0.15f, 0.45f)

        // Tertiary is hue-shifted by +60 degrees (Material You harmonization)
        val tertiaryHue = (primaryHue + 60f) % 360f
        val tertiarySat = (primarySat * 0.65f).coerceIn(0.25f, 0.60f)

        // Neutral surfaces are subtly tinted with the song's hue (~6% saturation)
        val neutralHue = primaryHue
        val neutralSat = 0.08f

        // Neutral variant for outlines and dividers (~14% saturation)
        val neutralVariantSat = 0.16f

        val p80 = tone(primaryHue, primarySat, 80) // Dark Primary
        val p20 = tone(primaryHue, primarySat, 20) // onPrimary
        val p30 = tone(primaryHue, primarySat, 30) // Primary Container
        val p90 = tone(primaryHue, primarySat, 90) // onPrimary Container

        val s80 = tone(secondaryHue, secondarySat, 80) // Secondary
        val s20 = tone(secondaryHue, secondarySat, 20) // onSecondary
        val s30 = tone(secondaryHue, secondarySat, 30) // Secondary Container
        val s90 = tone(secondaryHue, secondarySat, 90) // onSecondary Container

        val t80 = tone(tertiaryHue, tertiarySat, 80) // Tertiary
        val t20 = tone(tertiaryHue, tertiarySat, 20) // onTertiary
        val t30 = tone(tertiaryHue, tertiarySat, 30) // Tertiary Container
        val t90 = tone(tertiaryHue, tertiarySat, 90) // onTertiary Container

        val bg = if (isAmoled) Color(0xFF000000) else tone(neutralHue, neutralSat, 6)
        val surf = if (isAmoled) Color(0xFF07080A) else tone(neutralHue, neutralSat, 8)
        val surfVar = if (isAmoled) Color(0xFF101318) else tone(neutralHue, neutralVariantSat, 14)
        val surfCard = if (isAmoled) Color(0xFF0B0D12) else tone(neutralHue, neutralSat, 12)
        val surfContainerHigh = if (isAmoled) Color(0xFF161922) else tone(neutralHue, neutralSat, 17)
        val surfContainerHighest = if (isAmoled) Color(0xFF1E222D) else tone(neutralHue, neutralSat, 22)

        val onSurf = tone(neutralHue, neutralSat, 94)
        val onSurfVar = tone(neutralHue, neutralVariantSat, 80)
        val outlineColor = tone(neutralHue, neutralVariantSat, 42)
        val outlineVariantColor = tone(neutralHue, neutralVariantSat, 26)

        return darkColorScheme(
            primary = p80,
            onPrimary = p20,
            primaryContainer = p30,
            onPrimaryContainer = p90,
            inversePrimary = tone(primaryHue, primarySat, 40),
            secondary = s80,
            onSecondary = s20,
            secondaryContainer = s30,
            onSecondaryContainer = s90,
            tertiary = t80,
            onTertiary = t20,
            tertiaryContainer = t30,
            onTertiaryContainer = t90,
            background = bg,
            onBackground = onSurf,
            surface = surf,
            onSurface = onSurf,
            surfaceVariant = surfVar,
            onSurfaceVariant = onSurfVar,
            surfaceContainer = surfCard,
            surfaceContainerHigh = surfContainerHigh,
            surfaceContainerHighest = surfContainerHighest,
            surfaceContainerLow = if (isAmoled) Color(0xFF040507) else tone(neutralHue, neutralSat, 9),
            surfaceContainerLowest = if (isAmoled) Color(0xFF000000) else tone(neutralHue, neutralSat, 4),
            outline = outlineColor,
            outlineVariant = outlineVariantColor,
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    }

    /**
     * Builds Material 3 Light ColorScheme from seed color
     */
    fun buildLightColorScheme(seedColor: Color): ColorScheme {
        val hsl = rgbToHsl(seedColor)
        val primaryHue = hsl[0]
        val primarySat = hsl[1].coerceAtLeast(0.45f)

        val secondaryHue = primaryHue
        val secondarySat = (primarySat * 0.40f).coerceIn(0.15f, 0.45f)

        val tertiaryHue = (primaryHue + 60f) % 360f
        val tertiarySat = (primarySat * 0.65f).coerceIn(0.25f, 0.60f)

        val neutralHue = primaryHue
        val neutralSat = 0.06f
        val neutralVariantSat = 0.12f

        val p40 = tone(primaryHue, primarySat, 40) // Light Primary
        val p100 = Color.White // onPrimary
        val p90 = tone(primaryHue, primarySat, 90) // Primary Container
        val p10 = tone(primaryHue, primarySat, 10) // onPrimary Container

        val s40 = tone(secondaryHue, secondarySat, 40) // Secondary
        val s100 = Color.White // onSecondary
        val s90 = tone(secondaryHue, secondarySat, 90) // Secondary Container
        val s10 = tone(secondaryHue, secondarySat, 10) // onSecondary Container

        val t40 = tone(tertiaryHue, tertiarySat, 40) // Tertiary
        val t100 = Color.White // onTertiary
        val t90 = tone(tertiaryHue, tertiarySat, 90) // Tertiary Container
        val t10 = tone(tertiaryHue, tertiarySat, 10) // onTertiary Container

        val bg = tone(neutralHue, neutralSat, 98)
        val surf = tone(neutralHue, neutralSat, 99)
        val surfVar = tone(neutralHue, neutralVariantSat, 90)
        val surfCard = tone(neutralHue, neutralSat, 94)
        val surfContainerHigh = tone(neutralHue, neutralSat, 92)
        val surfContainerHighest = tone(neutralHue, neutralSat, 90)

        val onSurf = tone(neutralHue, neutralSat, 10)
        val onSurfVar = tone(neutralHue, neutralVariantSat, 30)
        val outlineColor = tone(neutralHue, neutralVariantSat, 55)
        val outlineVariantColor = tone(neutralHue, neutralVariantSat, 80)

        return lightColorScheme(
            primary = p40,
            onPrimary = p100,
            primaryContainer = p90,
            onPrimaryContainer = p10,
            inversePrimary = tone(primaryHue, primarySat, 80),
            secondary = s40,
            onSecondary = s100,
            secondaryContainer = s90,
            onSecondaryContainer = s10,
            tertiary = t40,
            onTertiary = t100,
            tertiaryContainer = t90,
            onTertiaryContainer = t10,
            background = bg,
            onBackground = onSurf,
            surface = surf,
            onSurface = onSurf,
            surfaceVariant = surfVar,
            onSurfaceVariant = onSurfVar,
            surfaceContainer = surfCard,
            surfaceContainerHigh = surfContainerHigh,
            surfaceContainerHighest = surfContainerHighest,
            surfaceContainerLow = tone(neutralHue, neutralSat, 96),
            surfaceContainerLowest = Color.White,
            outline = outlineColor,
            outlineVariant = outlineVariantColor,
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
}
