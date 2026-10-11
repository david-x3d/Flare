package dev.flare.speedtest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.flare.speedtest.ui.AppViewModelProvider
import dev.flare.speedtest.ui.navigation.FlareAppRoot
import dev.flare.speedtest.ui.settings.SettingsViewModel
import dev.flare.speedtest.ui.theme.FlareTheme
import dev.flare.speedtest.ui.theme.isDark

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            FlareTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                // Status/navigation bar icons must follow the app theme, not just the system one.
                val dark = settings.themeMode.isDark()
                DisposableEffect(dark) {
                    val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose { }
                }
                FlareAppRoot(windowSizeClass = calculateWindowSizeClass(this@MainActivity))
            }
        }
    }
}
