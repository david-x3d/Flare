package dev.flare.speedtest.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.flare.speedtest.domain.AppSettings
import dev.flare.speedtest.domain.SettingsRepository
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.TestSize
import dev.flare.speedtest.domain.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setUnit(unit: SpeedUnit) = launch { repository.setUnit(unit) }
    fun setTestSize(size: TestSize) = launch { repository.setTestSize(size) }
    fun setThemeMode(mode: ThemeMode) = launch { repository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { repository.setDynamicColor(enabled) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
