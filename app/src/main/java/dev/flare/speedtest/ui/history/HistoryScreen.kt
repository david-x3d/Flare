package dev.flare.speedtest.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.SpeedTestRecord
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed
import dev.flare.speedtest.ui.components.icon
import dev.flare.speedtest.ui.components.label
import dev.flare.speedtest.ui.test.label
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
    twoPane: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<SpeedTestRecord?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                subtitle = {
                    if (state.records.isNotEmpty()) Text(stringResource(R.string.history_hint_long_press))
                },
                actions = {
                    if (state.records.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.history_clear_all))
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        val contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding)
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator(modifier = Modifier.size(64.dp))
            }

            state.records.isEmpty() -> EmptyHistory(Modifier.fillMaxSize().padding(padding))

            twoPane -> Row(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(0.9f).padding(contentPadding)) {
                    HistoryChartCard(state.records, state.unit)
                }
                RecordList(state, { pendingDelete = it }, contentPadding, showChart = false, modifier = Modifier.weight(1.1f))
            }

            else -> RecordList(state, { pendingDelete = it }, contentPadding, showChart = true, modifier = Modifier.fillMaxSize().padding(padding))
        }
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_message)) },
            confirmButton = {
                TextButton(onClick = { onDelete(record.id); pendingDelete = null }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_message)) },
            confirmButton = {
                TextButton(onClick = { onClear(); confirmClear = false }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordList(
    state: HistoryUiState,
    onLongPress: (SpeedTestRecord) -> Unit,
    contentPadding: PaddingValues,
    showChart: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showChart) {
            item(key = "chart") { HistoryChartCard(state.records, state.unit) }
        }
        items(state.records, key = { it.id }) { record ->
            RecordCard(record, state.unit, onLongPress = { onLongPress(record) }, modifier = Modifier.animateItem())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordCard(record: SpeedTestRecord, unit: SpeedUnit, onLongPress: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val deleteLabel = stringResource(R.string.history_delete_title)
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    val m = record.measurement
    val unitLabel = unit.label()
    val msLabel = stringResource(R.string.unit_ms)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .combinedClickable(
                onClick = {},
                onLongClickLabel = deleteLabel,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            )
            .semantics { onLongClick(deleteLabel) { onLongPress(); true } },
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(MaterialShapes.Cookie6Sided.toShape()).background(colors.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(record.networkType.icon(), contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(dateFormat.format(Date(record.timestampMillis)), style = MaterialTheme.typography.titleSmallEmphasized)
                    Text(record.networkType.label(), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigValue(Icons.Rounded.Download, formatSpeed(unit.fromMbps(m.downloadMbps)), unitLabel, colors.primary, Modifier.weight(1f))
                BigValue(Icons.Rounded.Upload, formatSpeed(unit.fromMbps(m.uploadMbps)), unitLabel, colors.tertiary, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallValue(Icons.Rounded.NetworkPing, stringResource(R.string.metric_ping), "${formatMillis(m.pingMs)} $msLabel", Modifier.weight(1f))
                SmallValue(Icons.AutoMirrored.Rounded.ShowChart, stringResource(R.string.metric_jitter), "${formatMillis(m.jitterMs)} $msLabel", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BigValue(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, unit: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp).padding(bottom = 4.dp))
        Text(value, style = MaterialTheme.typography.headlineMediumEmphasized, maxLines = 1, modifier = Modifier.alignByBaseline())
        Text(unit, style = MaterialTheme.typography.labelMedium, modifier = Modifier.alignByBaseline())
    }
}

@Composable
private fun SmallValue(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text("$label  $value", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier.size(120.dp).clip(MaterialShapes.Cookie12Sided.toShape()).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(52.dp))
        }
        Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.history_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
