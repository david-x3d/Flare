package dev.flare.speedtest.ui.test

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.Statistics
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.TestPlan
import dev.flare.speedtest.domain.TransferSample
import dev.flare.speedtest.domain.transferCsv
import dev.flare.speedtest.ui.components.FlareCard
import dev.flare.speedtest.ui.components.SectionLabel
import dev.flare.speedtest.ui.components.formatBytes
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed
import dev.flare.speedtest.ui.components.label
import dev.flare.speedtest.ui.theme.Flare
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The Full test's 8 steps with per-round status dots, a transfer chart and CSV export. */
@Composable
fun DetailedResults(
    samples: List<TransferSample>,
    unit: SpeedUnit,
    activeStep: Int = -1,
    activeRound: Int = 0,
    running: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val steps = TestPlan.full().detailedSteps
    val palette = Flare.palette
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val failedMessage = stringResource(R.string.export_failed)
    var csv by rememberSaveable { mutableStateOf("") }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val content = csv
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("No output stream")
                        stream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
                    }
                } catch (_: IOException) {
                    Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show()
                } catch (_: SecurityException) {
                    Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    FlareCard(modifier) {
        SectionLabel(stringResource(R.string.full_steps)) {
            if (samples.isNotEmpty() && !running) {
                TextButton(onClick = {
                    csv = transferCsv(samples)
                    exporter.launch("flare-transfers-${samples.first().timestampMillis}.csv")
                }) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.export_transfers), modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        Text(stringResource(R.string.full_budget), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (samples.any { it.mbps != null }) {
            val total = steps.sumOf { it.rounds }
            Canvas(Modifier.fillMaxWidth().height(72.dp)) {
                val max = samples.mapNotNull { it.mbps }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                val slot = size.width / total
                samples.forEachIndexed { index, sample ->
                    val h = ((sample.mbps ?: 0.0) / max * size.height).toFloat().coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        if (sample.phase == TestPhase.DOWNLOAD) palette.download else palette.upload,
                        topLeft = Offset(slot * index + slot * 0.15f, size.height - h),
                        size = Size(slot * 0.7f, h),
                        cornerRadius = CornerRadius(slot * 0.35f),
                        alpha = if (sample.mbps == null) 0.3f else 1f,
                    )
                }
            }
        }

        steps.forEachIndexed { index, step ->
            val results = samples.filter { it.stepIndex == index }
            val active = running && activeStep == index
            val color = if (step.phase == TestPhase.DOWNLOAD) palette.download else palette.upload
            var expanded by rememberSaveable(index) { mutableStateOf(false) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) color.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        if (step.phase == TestPhase.DOWNLOAD) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                        contentDescription = null, tint = color, modifier = Modifier.size(18.dp),
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${stringResource(if (step.phase == TestPhase.DOWNLOAD) R.string.metric_download else R.string.metric_upload)} · ${formatBytes(step.bytes)}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        RoundDots(step.rounds, results, if (active) activeRound else -1, color)
                    }
                    val rates = results.mapNotNull { it.mbps }
                    Text(
                        if (rates.isEmpty()) stringResource(R.string.step_rounds, results.size, step.rounds)
                        else "${formatSpeed(unit.fromMbps(rates.average()))} ${unit.label()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (rates.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
                AnimatedVisibility(expanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (results.isEmpty()) Text(stringResource(R.string.step_pending), style = MaterialTheme.typography.bodySmall)
                        results.forEach { sample -> SampleLine(sample, unit) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundDots(rounds: Int, results: List<TransferSample>, activeRound: Int, color: Color) {
    val palette = Flare.palette
    val pending = MaterialTheme.colorScheme.outlineVariant
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(rounds) { i ->
            val sample = results.getOrNull(i)
            val dotColor = when {
                sample == null && activeRound == i + 1 -> color.copy(alpha = 0.5f)
                sample == null -> pending
                sample.status == "completed" -> color
                sample.status == "skipped" -> pending
                else -> palette.bad
            }
            Box(Modifier.size(width = 14.dp, height = 6.dp).clip(CircleShape).background(dotColor))
        }
    }
}

@Composable
private fun SampleLine(sample: TransferSample, unit: SpeedUnit) {
    val status = when (sample.status) {
        "completed" -> R.string.sample_completed
        "timeout" -> R.string.sample_timeout
        "failed" -> R.string.sample_failed
        else -> R.string.sample_skipped
    }
    val parts = buildList {
        sample.mbps?.let { add("${formatSpeed(unit.fromMbps(it))} ${unit.label()}") }
        sample.durationMs?.let { add(stringResource(R.string.sample_duration, formatMillis(it))) }
        sample.latencyMs?.let { add(stringResource(R.string.sample_latency, formatMillis(it))) }
        sample.serverTimeMs?.let { add(stringResource(R.string.sample_server, formatMillis(it))) }
        if (sample.loadedLatencies.isNotEmpty()) {
            add(stringResource(R.string.sample_loaded, formatMillis(Statistics.median(sample.loadedLatencies)), sample.loadedLatencies.size))
        }
    }
    Column {
        Text("#${sample.round} · ${stringResource(status)}", style = MaterialTheme.typography.labelLarge)
        if (parts.isNotEmpty()) {
            Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
