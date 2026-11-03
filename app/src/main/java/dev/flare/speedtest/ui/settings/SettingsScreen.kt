package dev.flare.speedtest.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.BuildConfig
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.AppSettings
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.TestSize
import dev.flare.speedtest.domain.ThemeMode
import dev.flare.speedtest.ui.test.labelRes
import dev.flare.speedtest.ui.theme.dynamicColorSupported

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
    onDefaultMode: (dev.flare.speedtest.domain.TestMode) -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsSection(stringResource(R.string.default_mode), stringResource(R.string.mode_hint)) {
                    ConnectedChoice(
                        options = dev.flare.speedtest.domain.TestMode.entries.map { it to stringResource(if (it == dev.flare.speedtest.domain.TestMode.QUICK) R.string.mode_quick else R.string.mode_full) },
                        selected = settings.defaultMode, onSelect = onDefaultMode,
                    )
                }
                SettingsSection(stringResource(R.string.settings_unit)) {
                    ConnectedChoice(
                        options = listOf(
                            SpeedUnit.MBIT_PER_SECOND to stringResource(R.string.unit_mbit),
                            SpeedUnit.MBYTE_PER_SECOND to stringResource(R.string.unit_mbyte),
                        ),
                        selected = settings.unit,
                        onSelect = onUnit,
                    )
                }
                SettingsSection(stringResource(R.string.settings_test_size), hint = stringResource(R.string.settings_test_size_hint)) {
                    ConnectedChoice(
                        options = TestSize.entries.map { it to stringResource(it.labelRes()) },
                        selected = settings.testSize,
                        onSelect = onTestSize,
                    )
                }
                SettingsSection(stringResource(R.string.settings_theme)) {
                    ConnectedChoice(
                        options = listOf(
                            ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                            ThemeMode.LIGHT to stringResource(R.string.theme_light),
                            ThemeMode.DARK to stringResource(R.string.theme_dark),
                        ),
                        selected = settings.themeMode,
                        onSelect = onTheme,
                    )
                    DynamicColorRow(settings.dynamicColor, onDynamicColor)
                }
                SettingsSection(stringResource(R.string.settings_behavior)) {
                    PreferenceSwitch(R.string.keep_screen_on, R.string.keep_screen_on_hint, settings.keepScreenOn, onKeepScreenOn)
                    PreferenceSwitch(R.string.save_history, R.string.save_history_hint, settings.saveHistory, onSaveHistory)
                }
                SettingsSection(stringResource(R.string.settings_about)) {
                    Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, hint: String? = null, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
            hint?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

/** A connected button group acting as a radio group: exactly one option is selected. */
@Composable
private fun <T> ConnectedChoice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, (value, label) ->
            val isSelected = value == selected
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { if (it) onSelect(value) },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                if (isSelected) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(label, modifier = Modifier.padding(start = 6.dp), maxLines = 1)
                } else {
                    Text(label, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DynamicColorRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val supported = dynamicColorSupported
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_dynamic_color), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(if (supported) R.string.settings_dynamic_color_desc else R.string.settings_dynamic_color_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled && supported,
            onCheckedChange = onChange,
            enabled = supported,
            thumbContent = if (enabled && supported) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchIconSize)) }
            } else {
                null
            },
        )
    }
}

private val SwitchIconSize = 16.dp

@Composable
private fun PreferenceSwitch(title: Int, hint: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
