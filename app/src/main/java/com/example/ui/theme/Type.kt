package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Figtree Geometric / Clean modern Google Font
val FigtreeFontFamily = FontFamily(
    Font(R.font.figtree, FontWeight.Normal),
    Font(R.font.figtree, FontWeight.Medium),
    Font(R.font.figtree, FontWeight.SemiBold),
    Font(R.font.figtree, FontWeight.Bold),
    Font(R.font.figtree, FontWeight.ExtraBold)
)

// SF Pro Display / Modern Neo-Grotesque Google Font (Inter)
val SFProFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
    Font(R.font.inter, FontWeight.ExtraBold)
)

// Modern geometric Google font for bold headings, display titles, and hero sections
val OutfitFontFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold),
    Font(R.font.outfit, FontWeight.ExtraBold)
)

// Clean Google font for body copy, labels, and metadata
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans, FontWeight.ExtraBold)
)

// DM Sans Geometric font
val DMSansFontFamily = FontFamily(
    Font(R.font.dm_sans, FontWeight.Normal),
    Font(R.font.dm_sans, FontWeight.Medium),
    Font(R.font.dm_sans, FontWeight.SemiBold),
    Font(R.font.dm_sans, FontWeight.Bold)
)

// Manrope Studio font
val ManropeFontFamily = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold)
)

enum class FontOption(val id: String, val displayName: String, val subtitle: String) {
    FIGTREE("figtree", "Figtree", "Clean, modern geometric sans-serif"),
    SF_PRO_DISPLAY("sf_pro_display", "SF Pro Display", "Clean iOS & Mac Studio aesthetics"),
    OUTFIT("outfit", "Outfit & Jakarta", "Pixel dynamic expressive"),
    DM_SANS("dm_sans", "DM Sans", "Geometric modern design"),
    MANROPE("manrope", "Manrope Studio", "Tech & modern precision");

    companion object {
        fun fromId(id: String): FontOption {
            return entries.firstOrNull { it.id == id } ?: FIGTREE
        }
    }
}

fun getAppTypography(fontOption: FontOption): Typography {
    val (displayFont, bodyFont) = when (fontOption) {
        FontOption.FIGTREE -> Pair(FigtreeFontFamily, FigtreeFontFamily)
        FontOption.SF_PRO_DISPLAY -> Pair(SFProFontFamily, SFProFontFamily)
        FontOption.OUTFIT -> Pair(OutfitFontFamily, PlusJakartaSans)
        FontOption.DM_SANS -> Pair(DMSansFontFamily, DMSansFontFamily)
        FontOption.MANROPE -> Pair(ManropeFontFamily, ManropeFontFamily)
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

val Typography = getAppTypography(FontOption.FIGTREE)
