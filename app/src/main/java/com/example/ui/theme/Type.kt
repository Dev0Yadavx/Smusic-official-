package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// Plus Jakarta Sans Font Family (Bold, Italic, Normal, Medium sabhi weights ke sath)
val PlusJakartaSans = FontFamily(
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.ExtraBold, style = FontStyle.Normal),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.ExtraBold, style = FontStyle.Italic)
)

// Primary AppFontFamily (Plus Jakarta Sans with Bold, Italic, Normal & Medium support)
val AppFontFamily = PlusJakartaSans

// Poppins Google Font Family
val PoppinsFontFamily = FontFamily(
    Font(resId = R.font.poppins, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.poppins, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.poppins, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.poppins, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(resId = R.font.poppins, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.poppins, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(resId = R.font.poppins, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.poppins, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(resId = R.font.poppins, weight = FontWeight.ExtraBold, style = FontStyle.Normal)
)

// Figtree Geometric / Clean modern Google Font
val FigtreeFontFamily = FontFamily(
    Font(resId = R.font.figtree, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.figtree, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.figtree, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.figtree, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.figtree, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.figtree, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(resId = R.font.figtree, weight = FontWeight.ExtraBold, style = FontStyle.Normal)
)

// Inter / SF Pro Display Modern Neo-Grotesque Google Font
val SFProFontFamily = FontFamily(
    Font(resId = R.font.inter, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.inter, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.inter, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.inter, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.inter, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.inter, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(resId = R.font.inter, weight = FontWeight.ExtraBold, style = FontStyle.Normal)
)

// Modern geometric Google font for bold headings, display titles, and hero sections
val OutfitFontFamily = FontFamily(
    Font(resId = R.font.outfit, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.outfit, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.outfit, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.outfit, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.outfit, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.outfit, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(resId = R.font.outfit, weight = FontWeight.ExtraBold, style = FontStyle.Normal)
)

// DM Sans Geometric font
val DMSansFontFamily = FontFamily(
    Font(resId = R.font.dm_sans, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.dm_sans, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.dm_sans, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.dm_sans, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.dm_sans, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.dm_sans, weight = FontWeight.Bold, style = FontStyle.Italic)
)

// Manrope Studio font
val ManropeFontFamily = FontFamily(
    Font(resId = R.font.manrope, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(resId = R.font.manrope, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(resId = R.font.manrope, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(resId = R.font.manrope, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(resId = R.font.manrope, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(resId = R.font.manrope, weight = FontWeight.Bold, style = FontStyle.Italic)
)

enum class FontOption(val id: String, val displayName: String, val subtitle: String) {
    PLUS_JAKARTA_SANS("plus_jakarta_sans", "Plus Jakarta Sans", "Bold titles & italic artist modern typography"),
    POPPINS("poppins", "Poppins", "Rounded geometric studio aesthetics"),
    SF_PRO_DISPLAY("sf_pro_display", "Inter / SF Pro", "Clean neo-grotesque precision"),
    FIGTREE("figtree", "Figtree", "Clean, modern geometric sans-serif"),
    OUTFIT("outfit", "Outfit & Jakarta", "Pixel dynamic expressive"),
    DM_SANS("dm_sans", "DM Sans", "Geometric modern design"),
    MANROPE("manrope", "Manrope Studio", "Tech & modern precision");

    companion object {
        fun fromId(id: String): FontOption {
            return entries.firstOrNull { it.id == id } ?: PLUS_JAKARTA_SANS
        }
    }
}

fun getAppTypography(fontOption: FontOption): Typography {
    val (displayFont, bodyFont) = when (fontOption) {
        FontOption.PLUS_JAKARTA_SANS -> Pair(PlusJakartaSans, PlusJakartaSans)
        FontOption.POPPINS -> Pair(PoppinsFontFamily, PoppinsFontFamily)
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

val Typography = getAppTypography(FontOption.PLUS_JAKARTA_SANS)
