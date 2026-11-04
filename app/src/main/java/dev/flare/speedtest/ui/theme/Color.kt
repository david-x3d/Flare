package dev.flare.speedtest.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Flare's own palette, taken from the logo: deep "night" navy (#16123F) and flame orange
 * (#FF5A1F). Download is flame, upload is a cool violet, latency is amber.
 */

val Navy = Color(0xFF16123F)
val Flame = Color(0xFFFF5A1F)

val FlareDarkColors = darkColorScheme(
    primary = Color(0xFFFF6B2C),
    onPrimary = Color(0xFF2A0B00),
    primaryContainer = Color(0xFF5A2410),
    onPrimaryContainer = Color(0xFFFFDBCB),
    inversePrimary = Color(0xFFC2410C),
    secondary = Color(0xFFFFB547),
    onSecondary = Color(0xFF2B1A00),
    secondaryContainer = Color(0xFF4A3410),
    onSecondaryContainer = Color(0xFFFFE2B0),
    tertiary = Color(0xFF9A8CFF),
    onTertiary = Color(0xFF160F4A),
    tertiaryContainer = Color(0xFF2F2876),
    onTertiaryContainer = Color(0xFFE4DFFF),
    background = Color(0xFF0C0A1D),
    onBackground = Color(0xFFF2EFFF),
    surface = Color(0xFF0C0A1D),
    onSurface = Color(0xFFF2EFFF),
    surfaceVariant = Color(0xFF242050),
    onSurfaceVariant = Color(0xFFABA5CC),
    surfaceTint = Color(0xFFFF6B2C),
    inverseSurface = Color(0xFFF2EFFF),
    inverseOnSurface = Color(0xFF1B1840),
    error = Color(0xFFFF7A7A),
    onError = Color(0xFF450008),
    errorContainer = Color(0xFF5C1520),
    onErrorContainer = Color(0xFFFFDADB),
    outline = Color(0xFF575184),
    outlineVariant = Color(0xFF2C2858),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2C2858),
    surfaceDim = Color(0xFF0C0A1D),
    surfaceContainerLowest = Color(0xFF070614),
    surfaceContainerLow = Color(0xFF13102C),
    surfaceContainer = Color(0xFF181537),
    surfaceContainerHigh = Color(0xFF1F1B44),
    surfaceContainerHighest = Color(0xFF282352),
)

val FlareLightColors = lightColorScheme(
    primary = Color(0xFFE24E14),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF4F1600),
    inversePrimary = Color(0xFFFF9A6B),
    secondary = Color(0xFFA86400),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE2B8),
    onSecondaryContainer = Color(0xFF3A2200),
    tertiary = Color(0xFF5646D6),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE4DFFF),
    onTertiaryContainer = Color(0xFF180C6A),
    background = Color(0xFFFAF8FF),
    onBackground = Navy,
    surface = Color(0xFFFAF8FF),
    onSurface = Navy,
    surfaceVariant = Color(0xFFE6E2F3),
    onSurfaceVariant = Color(0xFF57527A),
    surfaceTint = Color(0xFFE24E14),
    inverseSurface = Color(0xFF221E4A),
    inverseOnSurface = Color(0xFFF2EFFF),
    error = Color(0xFFC62B3A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDADB),
    onErrorContainer = Color(0xFF5C0A15),
    outline = Color(0xFF7C7799),
    outlineVariant = Color(0xFFD3CFE4),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFAF8FF),
    surfaceDim = Color(0xFFDCD8EA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F1FC),
    surfaceContainer = Color(0xFFEEEBF8),
    surfaceContainerHigh = Color(0xFFE8E4F4),
    surfaceContainerHighest = Color(0xFFE1DDEF),
)
