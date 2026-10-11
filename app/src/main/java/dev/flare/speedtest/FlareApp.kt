package dev.flare.speedtest

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dev.flare.speedtest.data.db.FlareDatabase
import dev.flare.speedtest.data.network.AndroidNetworkMonitor
import dev.flare.speedtest.data.network.CloudflareSpeedTestEngine
import dev.flare.speedtest.data.repo.RoomHistoryRepository
import dev.flare.speedtest.data.settings.DataStoreSettingsRepository
import dev.flare.speedtest.domain.HistoryRepository
import dev.flare.speedtest.domain.NetworkMonitor
import dev.flare.speedtest.domain.SettingsRepository
import dev.flare.speedtest.domain.SpeedTestEngine

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class FlareApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Hand-wired dependencies; small enough that a DI framework would be noise. */
class AppContainer(context: Context) {
    private val database = FlareDatabase.create(context)

    val history: HistoryRepository = RoomHistoryRepository(database.testResultDao())
    val settings: SettingsRepository = DataStoreSettingsRepository(context.applicationContext.settingsDataStore)
    val networkMonitor: NetworkMonitor = AndroidNetworkMonitor(context)
    val speedTestEngine: SpeedTestEngine = CloudflareSpeedTestEngine()
}
