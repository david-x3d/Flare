package dev.flare.speedtest.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.SpeedTestRecord
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.ui.components.label
import dev.flare.speedtest.ui.test.DetailedResults
import dev.flare.speedtest.ui.test.MetricTiles
import dev.flare.speedtest.ui.test.ResultSummary
import dev.flare.speedtest.ui.test.TestUiState
import dev.flare.speedtest.ui.test.rememberShare
import java.text.DateFormat
import java.util.Date

/** Everything about one saved test in a bottom sheet: numbers, verdict, steps, share and delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSheet(record: SpeedTestRecord, unit: SpeedUnit, onDismiss: () -> Unit, onDelete: (SpeedTestRecord) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val m = record.measurement
    val tilesState = remember(record, unit) {
        TestUiState(
            phase = TestPhase.DONE, pingMs = m.pingMs, jitterMs = m.jitterMs,
            downloadMbps = m.downloadMbps, uploadMbps = m.uploadMbps,
            loadedDownMs = m.downloadLoadedMs, loadedUpMs = m.uploadLoadedMs, unit = unit,
        )
    }
    val share = rememberShare(m, unit)
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text(dateFormat.format(Date(record.timestampMillis)), style = MaterialTheme.typography.titleLarge)
                Text(record.networkType.label(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            MetricTiles(tilesState)
            ResultSummary(m, record.networkType)
            if (m.samples.isNotEmpty()) DetailedResults(m.samples, unit)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = share, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.action_share), modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = { onDelete(record) }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Text(stringResource(R.string.action_delete), modifier = Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.error)
                }
            }
            Column(Modifier.height(12.dp)) {}
        }
    }
}
