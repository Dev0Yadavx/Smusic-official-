package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Fredoka Regular Font Family
val FredokaRegularFontFamily = FontFamily(
    Font(resId = R.font.fredoka_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_regular, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.ExtraBold, style = FontStyle.Normal)
)

// Fredoka Medium Font Family
val FredokaMediumFontFamily = FontFamily(
    Font(resId = R.font.fredoka_medium, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.ExtraBold, style = FontStyle.Normal)
)

// Fredoka SemiBold Font Family
val FredokaSemiBoldFontFamily = FontFamily(
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.fredoka_semibold, weight = FontWeight.ExtraBold, style = FontStyle.Normal)
)

// Primary AppFontFamily
val AppFontFamily = FredokaRegularFontFamily

enum class FontOption(val id: String, val displayName: String, val subtitle: String) {
    FREDOKA_REGULAR("fredoka_regular", "Fredoka Regular", "Clean & rounded semi-expanded sans"),
    FREDOKA_MEDIUM("fredoka_medium", "Fredoka Medium", "Medium weight studio typography"),
    FREDOKA_SEMIBOLD("fredoka_semibold", "Fredoka SemiBold", "Bold prominent header typography");

    companion object {
        fun fromId(id: String): FontOption {
            return entries.firstOrNull { it.id == id } ?: FREDOKA_REGULAR
        }
    }
}

fun getAppTypography(fontOption: FontOption, useDeviceFont: Boolean = false): Typography {
    if (useDeviceFont) {
        val sysFont = FontFamily.Default
        return Typography(
            displayLarge = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Bold, fontSize = 52.sp, lineHeight = 58.sp, letterSpacing = (-0.5).sp),
            displayMedium = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.25).sp),
            displaySmall = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = 0.sp),
            headlineLarge = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.2).sp),
            headlineMedium = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = 0.sp),
            headlineSmall = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
            titleLarge = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp),
            titleMedium = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = 0.15.sp),
            titleSmall = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
            bodyLarge = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.3.sp),
            bodyMedium = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
            bodySmall = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
            labelLarge = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
            labelMedium = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
            labelSmall = TextStyle(fontFamily = sysFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp)
        )
    }

    val (displayFont, bodyFont) = when (fontOption) {
        FontOption.FREDOKA_REGULAR -> Pair(FredokaRegularFontFamily, FredokaRegularFontFamily)
        FontOption.FREDOKA_MEDIUM -> Pair(FredokaMediumFontFamily, FredokaMediumFontFamily)
        FontOption.FREDOKA_SEMIBOLD -> Pair(FredokaSemiBoldFontFamily, FredokaSemiBoldFontFamily)
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 52.sp,
            lineHeight = 58.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 46.sp,
            letterSpacing = (-0.25).sp
        ),
        displaySmall = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.2).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.3.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp
        ),
        labelLarge = TextStyle(
            fontFamily = displayFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp
        ),
        labelMedium = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelSmall = TextStyle(
            fontFamily = bodyFont,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        )
    )
}

val Typography = getAppTypography(FontOption.FREDOKA_REGULAR)
