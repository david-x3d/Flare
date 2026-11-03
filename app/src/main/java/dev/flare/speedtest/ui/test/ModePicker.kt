package dev.flare.speedtest.ui.test

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.TestMode
import dev.flare.speedtest.domain.TestPlan

@Composable
fun ModePicker(state: TestUiState, onMode: (TestMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.test_headline), style = MaterialTheme.typography.headlineLargeEmphasized)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            TestMode.entries.forEachIndexed { index, mode ->
                ToggleButton(
                    checked = state.mode == mode,
                    onCheckedChange = { if (it) onMode(mode) },
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1f),
                    shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                ) {
                    Icon(if (mode == TestMode.QUICK) Icons.Rounded.Bolt else Icons.Rounded.Insights, null, Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(if (mode == TestMode.QUICK) R.string.mode_quick else R.string.mode_full))
                }
            }
        }
        Text(stringResource(if (state.mode == TestMode.QUICK) R.string.quick_description else R.string.full_description), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val plan = TestPlan.forMode(state.mode, state.testSize)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Cloud, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(if (state.mode == TestMode.FULL) stringResource(R.string.full_plan_info) else stringResource(R.string.test_info, plan.latencySamples, plan.phaseBudgetMillis / 1000), style = MaterialTheme.typography.labelMedium)
        }
    }
}
