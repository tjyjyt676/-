package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val PlayfairDisplayFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold)
)

val ManropeFontFamily = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold)
)

fun buildCrmTypography(largeFont: Boolean): Typography {
    val scale = if (largeFont) 1.16f else 1.0f
    return Typography(
        displaySmall = TextStyle(
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (28f * scale).sp,
            lineHeight = (34f * scale).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = PlayfairDisplayFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (24f * scale).sp,
            lineHeight = (30f * scale).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (20f * scale).sp,
            lineHeight = (26f * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (18f * scale).sp,
            lineHeight = (24f * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15f * scale).sp,
            lineHeight = (21f * scale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5f * scale).sp,
            lineHeight = (19f * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13.5f * scale).sp,
            lineHeight = (19f * scale).sp
        ),
        bodySmall = TextStyle(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * scale).sp,
            lineHeight = (17f * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (13.5f * scale).sp,
            lineHeight = (18f * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * scale).sp,
            lineHeight = (16f * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = ManropeFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (10.5f * scale).sp,
            lineHeight = (14f * scale).sp
        )
    )
}
