package dev.flare.speedtest.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.BuildConfig
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.AppSettings
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.TestMode
import dev.flare.speedtest.domain.TestSize
import dev.flare.speedtest.domain.ThemeMode
import dev.flare.speedtest.ui.components.CardShape
import dev.flare.speedtest.ui.components.IconDisc
import dev.flare.speedtest.ui.components.SectionLabel
import dev.flare.speedtest.ui.test.labelRes
import dev.flare.speedtest.ui.theme.Flare
import dev.flare.speedtest.ui.theme.dynamicColorSupported

private const val SOURCE_URL = "https://github.com/david-x3d/flare"

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUnit: (SpeedUnit) -> Unit,
    onTestSize: (TestSize) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    onKeepScreenOn: (Boolean) -> Unit = {},
    onSaveHistory: (Boolean) -> Unit = {},
    onDefaultMode: (TestMode) -> Unit = {},
    onHaptics: (Boolean) -> Unit = {},
) {
    val palette = Flare.palette
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 640.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 6.dp))

            SectionLabel(stringResource(R.string.section_test), Modifier.padding(start = 4.dp, top = 8.dp))
            Group {
                ChoiceItem(Icons.Rounded.Speed, palette.download, stringResource(R.string.default_mode), stringResource(R.string.default_mode_hint)) {
                    Choice(
                        TestMode.entries.map { it to stringResource(if (it == TestMode.QUICK) R.string.mode_quick else R.string.mode_full) },
                        settings.defaultMode, onDefaultMode,
                    )
                }
                Divider()
                ChoiceItem(Icons.Rounded.DataUsage, palette.latency, stringResource(R.string.settings_test_size), stringResource(R.string.settings_test_size_hint)) {
                    Choice(TestSize.entries.map { it to stringResource(it.labelRes()) }, settings.testSize, onTestSize)
                }
                Divider()
                ChoiceItem(Icons.Rounded.Straighten, palette.upload, stringResource(R.string.settings_unit)) {
                    Choice(
                        listOf(SpeedUnit.MBIT_PER_SECOND to stringResource(R.string.unit_mbit), SpeedUnit.MBYTE_PER_SECOND to stringResource(R.string.unit_mbyte)),
                        settings.unit, onUnit,
                    )
                }
            }

            SectionLabel(stringResource(R.string.section_appearance), Modifier.padding(start = 4.dp, top = 12.dp))
            Group {
                ChoiceItem(
                    when (settings.themeMode) {
                        ThemeMode.LIGHT -> Icons.Rounded.LightMode
                        ThemeMode.DARK -> Icons.Rounded.DarkMode
                        ThemeMode.SYSTEM -> Icons.Rounded.PhoneAndroid
                    },
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.settings_theme),
                ) {
                    Choice(
                        listOf(
                            ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                            ThemeMode.LIGHT to stringResource(R.string.theme_light),
                            ThemeMode.DARK to stringResource(R.string.theme_dark),
                        ),
                        settings.themeMode, onTheme,
                    )
                }
                Divider()
                SwitchItem(
                    Icons.Rounded.Palette, MaterialTheme.colorScheme.tertiary,
                    stringResource(R.string.settings_dynamic_color),
                    stringResource(if (dynamicColorSupported) R.string.settings_dynamic_color_desc else R.string.settings_dynamic_color_unavailable),
                    checked = settings.dynamicColor && dynamicColorSupported,
                    enabled = dynamicColorSupported,
                    onChange = onDynamicColor,
                )
            }

            SectionLabel(stringResource(R.string.section_behavior), Modifier.padding(start = 4.dp, top = 12.dp))
            Group {
                SwitchItem(Icons.Rounded.ScreenLockPortrait, palette.latency, stringResource(R.string.keep_screen_on), stringResource(R.string.keep_screen_on_hint), settings.keepScreenOn, onChange = onKeepScreenOn)
                Divider()
                SwitchItem(Icons.Rounded.Vibration, palette.upload, stringResource(R.string.haptics), stringResource(R.string.haptics_hint), settings.haptics, onChange = onHaptics)
                Divider()
                SwitchItem(Icons.Rounded.History, palette.good, stringResource(R.string.save_history), stringResource(R.string.save_history_hint), settings.saveHistory, onChange = onSaveHistory)
            }

            SectionLabel(stringResource(R.string.section_about), Modifier.padding(start = 4.dp, top = 12.dp))
            Group {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconDisc(Icons.Rounded.Info, MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Divider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL))) }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconDisc(Icons.Rounded.Code, MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.about_source), style = MaterialTheme.typography.titleSmall)
                        Text(SOURCE_URL.removePrefix("https://"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = CardShape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(content = content)
    }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(start = 66.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

@Composable
private fun ChoiceItem(icon: ImageVector, tint: Color, title: String, hint: String? = null, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        IconDisc(icon, tint)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            control()
        }
    }
}

@Composable
private fun SwitchItem(icon: ImageVector, tint: Color, title: String, hint: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Switch) { onChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconDisc(icon, tint)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** A segmented button row acting as a radio group: exactly one option is selected. */
@Composable
private fun <T> Choice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}
