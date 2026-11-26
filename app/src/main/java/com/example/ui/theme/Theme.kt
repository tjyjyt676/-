package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class CrmExtendedColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val violet: Color,
    val violetContainer: Color,
    val deepHeader: Color,
    val subtleBorder: Color
)

private val DarkExtendedColors = CrmExtendedColors(
    success = EmeraldOk,
    successContainer = EmeraldContainerDark,
    warning = AmberPrimary,
    warningContainer = AmberContainerDark,
    danger = CrimsonError,
    dangerContainer = CrimsonContainerDark,
    info = SkyInfo,
    infoContainer = SkyContainerDark,
    violet = VioletAccent,
    violetContainer = VioletContainerDark,
    deepHeader = NightNavyDeep,
    subtleBorder = SlateBorder
)

private val LightExtendedColors = CrmExtendedColors(
    success = Color(0xFF047857),
    successContainer = EmeraldContainerLight,
    warning = AmberLightPrimary,
    warningContainer = AmberLightContainer,
    danger = Color(0xFFB91C1C),
    dangerContainer = CrimsonContainerLight,
    info = Color(0xFF0369A1),
    infoContainer = SkyContainerLight,
    violet = Color(0xFF6D28D9),
    violetContainer = VioletContainerLight,
    deepHeader = Color(0xFFEAF0F7),
    subtleBorder = DayBorder
)

val LocalCrmColors = staticCompositionLocalOf { DarkExtendedColors }

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = AmberDarkInk,
    primaryContainer = AmberContainerDark,
    onPrimaryContainer = AmberLight,
    secondary = AmberDeep,
    onSecondary = SnowText,
    secondaryContainer = GraphiteElevated,
    onSecondaryContainer = SnowText,
    tertiary = SkyInfo,
    onTertiary = NightNavy,
    background = NightNavy,
    onBackground = SnowText,
    surface = GraphiteCard,
    onSurface = SnowText,
    surfaceVariant = GraphiteElevated,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = NightNavyDeep,
    surfaceContainerLow = NightNavy,
    surfaceContainer = GraphiteCard,
    surfaceContainerHigh = GraphiteElevated,
    outline = SlateBorder,
    outlineVariant = Color(0xFF283548),
    error = CrimsonError,
    onError = SnowText,
    errorContainer = CrimsonContainerDark,
    onErrorContainer = Color(0xFFFCA5A5)
)

private val LightColorScheme = lightColorScheme(
    primary = AmberLightPrimary,
    onPrimary = Color.White,
    primaryContainer = AmberLightContainer,
    onPrimaryContainer = Color(0xFF78350F),
    secondary = AmberDeep,
    onSecondary = Color.White,
    secondaryContainer = DaySurfaceVariant,
    onSecondaryContainer = DayInk,
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = DayCanvas,
    onBackground = DayInk,
    surface = DaySurface,
    onSurface = DayInk,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = DayMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = DayCanvas,
    surfaceContainer = DaySurface,
    surfaceContainerHigh = DaySurfaceVariant,
    outline = DayBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = CrimsonContainerLight,
    onErrorContainer = Color(0xFF991B1B)
)

private val CrmShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun KringeCrmTheme(
    darkTheme: Boolean = true,
    largeFont: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val typography = buildCrmTypography(largeFont)

    CompositionLocalProvider(LocalCrmColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = CrmShapes,
            content = content
        )
    }
}
