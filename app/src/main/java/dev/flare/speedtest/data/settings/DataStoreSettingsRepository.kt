package dev.flare.speedtest.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.flare.speedtest.domain.AppSettings
import dev.flare.speedtest.domain.SettingsRepository
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.TestSize
import dev.flare.speedtest.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<AppSettings> = store.data.map { prefs ->
        val defaults = AppSettings()
        AppSettings(
            unit = prefs[UNIT].toEnum(defaults.unit),
            testSize = prefs[TEST_SIZE].toEnum(defaults.testSize),
            themeMode = prefs[THEME].toEnum(defaults.themeMode),
            dynamicColor = prefs[DYNAMIC_COLOR] ?: defaults.dynamicColor,
        )
    }

    override suspend fun setUnit(unit: SpeedUnit) = write { it[UNIT] = unit.name }
    override suspend fun setTestSize(size: TestSize) = write { it[TEST_SIZE] = size.name }
    override suspend fun setThemeMode(mode: ThemeMode) = write { it[THEME] = mode.name }
    override suspend fun setDynamicColor(enabled: Boolean) = write { it[DYNAMIC_COLOR] = enabled }

    private suspend fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private companion object {
        val UNIT = stringPreferencesKey("unit")
        val TEST_SIZE = stringPreferencesKey("test_size")
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }
}
