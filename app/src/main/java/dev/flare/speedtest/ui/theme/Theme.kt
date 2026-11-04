package dev.flare.speedtest.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.flare.speedtest.domain.ThemeMode

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

val dynamicColorSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Colours with a meaning beyond Material roles: one per metric plus a traffic light. */
@Immutable
data class FlarePalette(
    val download: Color,
    val upload: Color,
    val latency: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
    val dark: Boolean,
)

private val LocalFlarePalette = staticCompositionLocalOf {
    FlarePalette(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, false)
}

object Flare {
    val palette: FlarePalette
        @Composable @ReadOnlyComposable get() = LocalFlarePalette.current
}

/** Tabular figures keep live numbers from jittering sideways while they change. */
val NumberStyle = TextStyle(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", letterSpacing = (-0.5).sp)

private fun flareTypography(): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun FlareTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = themeMode.isDark()
    val context = LocalContext.current
    val dynamic = dynamicColor && dynamicColorSupported
    val colorScheme: ColorScheme = when {
        dynamic -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> FlareDarkColors
        else -> FlareLightColors
    }
    val palette = FlarePalette(
        download = colorScheme.primary,
        upload = colorScheme.tertiary,
        latency = colorScheme.secondary,
        good = if (dark) Color(0xFF3DDC97) else Color(0xFF0B8A5E),
        warn = if (dark) Color(0xFFFFC145) else Color(0xFFB26A00),
        bad = colorScheme.error,
        dark = dark,
    )
    CompositionLocalProvider(LocalFlarePalette provides palette) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = flareTypography(),
            content = {
                // Supplies the default content color; bare Text would otherwise render black.
                androidx.compose.material3.Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = colorScheme.background,
                    contentColor = colorScheme.onBackground,
                    content = content,
                )
            },
        )
    }
}
