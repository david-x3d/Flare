package dev.flare.speedtest.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.TestMode
import dev.flare.speedtest.domain.TestPlan
import dev.flare.speedtest.domain.TestSize
import dev.flare.speedtest.domain.plannedBytes
import dev.flare.speedtest.ui.components.formatBytes

/** Quick / Full switch plus an honest data and time estimate for the chosen mode. */
@Composable
fun ModeSelector(
    state: TestUiState,
    onMode: (TestMode) -> Unit,
    onTestSize: (TestSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            TestMode.entries.forEachIndexed { index, mode ->
                val selected = state.mode == mode
                SegmentedButton(
                    selected = selected,
                    onClick = { onMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, TestMode.entries.size),
                    enabled = !state.isRunning,
                    icon = {
                        SegmentedButtonDefaults.Icon(selected) {
                            Icon(
                                if (mode == TestMode.QUICK) Icons.Rounded.Bolt else Icons.Rounded.Insights,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        }
                    },
                    label = { Text(stringResource(if (mode == TestMode.QUICK) R.string.mode_quick else R.string.mode_full)) },
                )
            }
        }
        val plan = TestPlan.forMode(state.mode, state.testSize)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                if (state.mode == TestMode.FULL) stringResource(R.string.mode_full_info, formatBytes(plan.plannedBytes()))
                else stringResource(R.string.mode_quick_info, formatBytes(plan.plannedBytes()), (plan.phaseBudgetMillis * 2 / 1000 + 2).toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.mode == TestMode.QUICK) SizeMenu(state.testSize, enabled = !state.isRunning, onTestSize = onTestSize)
        }
    }
}

@Composable
private fun SizeMenu(size: TestSize, enabled: Boolean, onTestSize: (TestSize) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val cd = stringResource(R.string.cd_test_size_menu)
    Box {
        TextButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.semantics { contentDescription = cd }) {
            Text(stringResource(size.labelRes()), style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            TestSize.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes())) },
                    trailingIcon = { if (option == size) Icon(Icons.Rounded.Check, contentDescription = null) },
                    onClick = { open = false; onTestSize(option) },
                )
            }
        }
    }
}

fun TestSize.labelRes(): Int = when (this) {
    TestSize.SMALL -> R.string.size_small
    TestSize.NORMAL -> R.string.size_normal
    TestSize.LARGE -> R.string.size_large
}
