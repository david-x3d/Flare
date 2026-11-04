package dev.flare.speedtest.ui.history

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.NetworkType
import dev.flare.speedtest.domain.SpeedTestRecord
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.historyCsv
import dev.flare.speedtest.ui.components.FlareCard
import dev.flare.speedtest.ui.components.IconDisc
import dev.flare.speedtest.ui.components.SectionLabel
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed
import dev.flare.speedtest.ui.components.icon
import dev.flare.speedtest.ui.components.label
import dev.flare.speedtest.ui.theme.Flare
import dev.flare.speedtest.ui.theme.NumberStyle
import java.io.IOException
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Period(val days: Long?) { WEEK(7), MONTH(30), ALL(null) }

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onDelete: (SpeedTestRecord) -> Unit,
    onClear: () -> Unit,
    twoPane: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    onOpen: (SpeedTestRecord) -> Unit = {},
    onRunTest: () -> Unit = {},
) {
    var confirmClear by remember { mutableStateOf(false) }
    var period by rememberSaveable { mutableStateOf(Period.ALL) }
    var network by rememberSaveable { mutableStateOf<NetworkType?>(null) }
    val now = System.currentTimeMillis()
    val visible = remember(state.records, period, network) {
        val days = period.days
        val type = network
        state.records.filter { r ->
            (type == null || r.networkType == type) &&
                (days == null || now - r.timestampMillis <= days * 86_400_000L)
        }
    }
    val exportCsv = rememberCsvExport()

    Column(modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            if (state.records.isNotEmpty()) {
                IconButton(onClick = { exportCsv(visible) }) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = stringResource(R.string.history_export))
                }
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = stringResource(R.string.history_clear_all))
                }
            }
        }

        val padding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = bottomPadding)
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator(modifier = Modifier.size(64.dp))
            }

            state.records.isEmpty() -> EmptyHistory(onRunTest, Modifier.fillMaxSize())

            twoPane -> Row(Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(Modifier.weight(1f), contentPadding = padding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item { Filters(state.records, period, { period = it }, network, { network = it }) }
                    item { StatsCard(visible, state.unit) }
                    if (visible.isNotEmpty()) item { TrendCard(visible, state.unit) }
                }
                RecordList(visible, state.unit, onOpen, onDelete, padding, Modifier.weight(1f))
            }

            else -> RecordList(visible, state.unit, onOpen, onDelete, padding, Modifier.fillMaxSize()) {
                item(key = "filters") { Filters(state.records, period, { period = it }, network, { network = it }) }
                item(key = "stats") { StatsCard(visible, state.unit) }
                if (visible.isNotEmpty()) item(key = "trend") { TrendCard(visible, state.unit) }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_message)) },
            confirmButton = {
                TextButton(onClick = { onClear(); confirmClear = false }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun rememberCsvExport(): (List<SpeedTestRecord>) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val failed = stringResource(R.string.export_failed)
    var pending by rememberSaveable { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val csv = pending
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("No output stream")
                        stream.bufferedWriter(Charsets.UTF_8).use { it.write(csv) }
                    }
                } catch (_: IOException) {
                    Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
                } catch (_: SecurityException) {
                    Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    return { records ->
        pending = historyCsv(records)
        launcher.launch("flare-results-${System.currentTimeMillis()}.csv")
    }
}

private sealed interface ListRow {
    data class Header(val day: LocalDate) : ListRow
    data class Item(val record: SpeedTestRecord) : ListRow
}

@Composable
private fun RecordList(
    records: List<SpeedTestRecord>,
    unit: SpeedUnit,
    onOpen: (SpeedTestRecord) -> Unit,
    onDelete: (SpeedTestRecord) -> Unit,
    padding: PaddingValues,
    modifier: Modifier,
    header: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {},
) {
    val zone = remember { ZoneId.systemDefault() }
    val rows = remember(records) {
        buildList {
            var day: LocalDate? = null
            records.forEach { r ->
                val d = Instant.ofEpochMilli(r.timestampMillis).atZone(zone).toLocalDate()
                if (d != day) {
                    add(ListRow.Header(d))
                    day = d
                }
                add(ListRow.Item(r))
            }
        }
    }
    LazyColumn(modifier, contentPadding = padding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        header()
        if (records.isEmpty()) {
            item(key = "none") {
                Text(
                    stringResource(R.string.history_no_matches),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        } else {
            item(key = "hint") {
                Text(stringResource(R.string.history_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(rows, key = { row -> if (row is ListRow.Item) row.record.id else "day-${(row as ListRow.Header).day}" }) { row ->
            when (row) {
                is ListRow.Header -> DayHeader(row.day, Modifier.animateItem())
                is ListRow.Item -> SwipeRow(row.record, unit, onOpen, onDelete, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, modifier: Modifier) {
    val today = LocalDate.now()
    val text = when (day) {
        today -> stringResource(R.string.day_today)
        today.minusDays(1) -> stringResource(R.string.day_yesterday)
        else -> remember(day) {
            DateFormat.getDateInstance(DateFormat.FULL).format(Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant()))
        }
    }
    SectionLabel(text, modifier.padding(top = 8.dp, start = 4.dp))
}

@Composable
private fun SwipeRow(record: SpeedTestRecord, unit: SpeedUnit, onOpen: (SpeedTestRecord) -> Unit, onDelete: (SpeedTestRecord) -> Unit, modifier: Modifier) {
    val dismiss = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismiss,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { onDelete(record) },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        RecordRow(record, unit) { onOpen(record) }
    }
}

@Composable
private fun RecordRow(record: SpeedTestRecord, unit: SpeedUnit, onClick: () -> Unit) {
    val palette = Flare.palette
    val colors = MaterialTheme.colorScheme
    val m = record.measurement
    val time = remember(record.timestampMillis) { DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(record.timestampMillis)) }
    val subtitle = m.info?.isp ?: record.networkType.label()
    Surface(onClick = onClick, shape = RoundedCornerShape(22.dp), color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconDisc(record.networkType.icon(), colors.primary, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(time, style = MaterialTheme.typography.titleSmall)
                    if (m.samples.isNotEmpty()) {
                        Text(
                            stringResource(R.string.mode_full),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onTertiaryContainer,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(colors.tertiaryContainer).padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Speed(Icons.Rounded.ArrowDownward, palette.download, formatSpeed(unit.fromMbps(m.downloadMbps)))
                    Speed(Icons.Rounded.ArrowUpward, palette.upload, formatSpeed(unit.fromMbps(m.uploadMbps)))
                }
                Text(
                    "${unit.label()} · ${formatMillis(m.pingMs)} ${stringResource(R.string.unit_ms)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Speed(icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.merge(NumberStyle))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Filters(
    all: List<SpeedTestRecord>,
    period: Period,
    onPeriod: (Period) -> Unit,
    network: NetworkType?,
    onNetwork: (NetworkType?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Period.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = period == option,
                    onClick = { onPeriod(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, Period.entries.size),
                    label = {
                        Text(
                            stringResource(
                                when (option) {
                                    Period.WEEK -> R.string.period_7d
                                    Period.MONTH -> R.string.period_30d
                                    Period.ALL -> R.string.period_all
                                },
                            ),
                            maxLines = 1,
                        )
                    },
                )
            }
        }
        val types = remember(all) { all.map { it.networkType }.distinct().sortedBy { it.ordinal } }
        if (types.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = network == null, onClick = { onNetwork(null) }, label = { Text(stringResource(R.string.filter_all)) })
                types.forEach { type ->
                    FilterChip(
                        selected = network == type,
                        onClick = { onNetwork(if (network == type) null else type) },
                        label = { Text(type.label()) },
                        leadingIcon = { Icon(type.icon(), contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(records: List<SpeedTestRecord>, unit: SpeedUnit) {
    if (records.isEmpty()) return
    val palette = Flare.palette
    val ms = records.map { it.measurement }
    FlareCard {
        SectionLabel(pluralStringResource(R.plurals.tests_count, records.size, records.size) + " · " + stringResource(R.string.stats_average))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(stringResource(R.string.metric_download), formatSpeed(unit.fromMbps(ms.map { it.downloadMbps }.average())), unit.label(), palette.download, Modifier.weight(1f))
            Stat(stringResource(R.string.metric_upload), formatSpeed(unit.fromMbps(ms.map { it.uploadMbps }.average())), unit.label(), palette.upload, Modifier.weight(1f))
            Stat(stringResource(R.string.metric_ping), formatMillis(ms.map { it.pingMs }.average()), stringResource(R.string.unit_ms), palette.latency, Modifier.weight(0.8f))
        }
        Text(
            stringResource(R.string.stats_best, formatSpeed(unit.fromMbps(ms.maxOf { it.downloadMbps })), unit.label()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, unit: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        Text(value, style = MaterialTheme.typography.headlineSmall.merge(NumberStyle), maxLines = 1)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyHistory(onRunTest: () -> Unit, modifier: Modifier = Modifier) {
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
        Text(stringResource(R.string.history_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.history_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Button(onClick = onRunTest) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.history_empty_action), modifier = Modifier.padding(start = 8.dp))
        }
    }
}
