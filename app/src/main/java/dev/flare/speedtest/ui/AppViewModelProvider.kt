package dev.flare.speedtest.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.flare.speedtest.FlareApp
import dev.flare.speedtest.ui.history.HistoryViewModel
import dev.flare.speedtest.ui.settings.SettingsViewModel
import dev.flare.speedtest.ui.test.TestViewModel

private fun CreationExtras.container() = (this[APPLICATION_KEY] as FlareApp).container

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val c = container()
            TestViewModel(c.speedTestEngine, c.history, c.settings, c.networkMonitor)
        }
        initializer {
            val c = container()
            HistoryViewModel(c.history, c.settings)
        }
        initializer {
            SettingsViewModel(container().settings)
        }
    }
}
