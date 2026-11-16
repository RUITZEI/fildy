package com.fildy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fildy.R

// Figtree is a variable font: one file, one Font entry per weight we use.
val Figtree = FontFamily(
    listOf(400, 500, 600, 700, 800, 900).map { weight ->
        Font(
            resId = R.font.figtree,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

private val Default = Typography()

// Sizes in sp so they follow the system font scale. Minimum 13 sp; key data from 22 sp
// so it can be read in direct sunlight.
val Typography = Typography(
    // Display — 60/62 ExtraBold (e.g. "24°")
    displayLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 60.sp,
        lineHeight = 62.sp,
        letterSpacing = (-2).sp,
    ),
    displayMedium = Default.displayMedium.copy(fontFamily = Figtree),
    displaySmall = Default.displaySmall.copy(fontFamily = Figtree),
    headlineLarge = Default.headlineLarge.copy(fontFamily = Figtree),
    // Headline — 30/34 ExtraBold (e.g. "Hola, Martín")
    headlineMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = Default.headlineSmall.copy(fontFamily = Figtree),
    // Title L — 22/30 Bold
    titleLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    // Title M — 17/22 ExtraBold (e.g. "Mi campo")
    titleMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = Default.titleSmall.copy(fontFamily = Figtree),
    // Body — 16/23 SemiBold
    bodyLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp,
    ),
    // Secondary info — 14/20 SemiBold
    bodyMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // Small captions — 13/18 SemiBold (the 13 sp minimum)
    bodySmall = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    // Label — 15/20 Bold (e.g. "Tocá y hablá")
    labelLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    // Overline — 13/16 ExtraBold +0.8 (e.g. "HACIENDA · LOTE 5")
    labelMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp,
    ),
    labelSmall = Default.labelSmall.copy(fontFamily = Figtree),
)
