package dev.flare.speedtest.domain

import kotlinx.coroutines.flow.Flow

/** Runs one full test. Cancelling the collector aborts all in-flight network calls. */
fun interface SpeedTestEngine {
    /** Emits [SpeedTestEvent]s and ends with [SpeedTestEvent.Finished]; failures throw [SpeedTestException]. */
    fun run(plan: TestPlan): Flow<SpeedTestEvent>
}

interface HistoryRepository {
    fun observeAll(): Flow<List<SpeedTestRecord>>
    suspend fun save(networkType: NetworkType, measurement: Measurement, timestampMillis: Long)
    suspend fun delete(id: Long)
    suspend fun clear()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setUnit(unit: SpeedUnit)
    suspend fun setTestSize(size: TestSize)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
}

interface NetworkMonitor {
    /** Emits the current transport and every change of it. */
    val networkType: Flow<NetworkType>
    fun currentType(): NetworkType
}
