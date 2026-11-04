package dev.flare.speedtest.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.flare.speedtest.domain.HistoryRepository
import dev.flare.speedtest.domain.SettingsRepository
import dev.flare.speedtest.domain.SpeedTestRecord
import dev.flare.speedtest.domain.SpeedUnit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val loading: Boolean = true,
    /** Newest first. */
    val records: List<SpeedTestRecord> = emptyList(),
    val unit: SpeedUnit = SpeedUnit.MBIT_PER_SECOND,
)

class HistoryViewModel(
    private val history: HistoryRepository,
    settings: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(history.observeAll(), settings.settings) { records, s ->
        HistoryUiState(loading = false, records = records, unit = s.unit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun delete(id: Long) {
        viewModelScope.launch { history.delete(id) }
    }

    fun restore(record: SpeedTestRecord) {
        viewModelScope.launch { history.restore(record) }
    }

    fun clear() {
        viewModelScope.launch { history.clear() }
    }
}
