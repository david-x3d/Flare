package dev.flare.speedtest.ui.test

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import java.io.IOException
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.*
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed

@Composable
fun DetailedResults(samples: List<TransferSample>, unit: SpeedUnit, activeStep: Int = -1, activeRound: Int = 0, running: Boolean = false) {
    val steps = TestPlan.full().detailedSteps
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
                } catch (_: IOException) { Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show() }
                  catch (_: SecurityException) { Toast.makeText(context, failedMessage, Toast.LENGTH_LONG).show() }
            }
        }
    }
    val downColor = MaterialTheme.colorScheme.primary
    val upColor = MaterialTheme.colorScheme.tertiary
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.full_steps), style = MaterialTheme.typography.titleLargeEmphasized)
        if (samples.isNotEmpty() && !running) OutlinedButton(onClick = {
            csv = transferCsv(samples)
            exporter.launch("flare-transfers-${samples.first().timestampMillis}.csv")
        }) { Text(stringResource(R.string.export_transfers)) }
        Text(stringResource(R.string.full_budget), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (samples.any { it.mbps != null }) {
            Canvas(Modifier.fillMaxWidth().height(100.dp)) {
                val max = samples.mapNotNull { it.mbps }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                val width = size.width / 49f
                samples.forEachIndexed { index, sample ->
                    val height = ((sample.mbps ?: 0.0) / max * size.height).toFloat()
                    drawLine(if (sample.phase == TestPhase.DOWNLOAD) downColor else upColor, Offset(width * (index + .5f), size.height), Offset(width * (index + .5f), size.height - height), strokeWidth = width * .65f)
                }
            }
            Text(stringResource(R.string.sample_chart_hint), style = MaterialTheme.typography.labelSmall)
        }
        steps.forEachIndexed { index, step ->
            val results = samples.filter { it.stepIndex == index }
            val active = running && activeStep == index
            var expanded by rememberSaveable(index) { mutableStateOf(false) }
            Surface(
                onClick = { expanded = !expanded },
                shape = RoundedCornerShape(topStart = if (index % 2 == 0) 28.dp else 12.dp, topEnd = 28.dp, bottomEnd = if (index % 2 == 0) 12.dp else 28.dp, bottomStart = 28.dp),
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (results.size == step.rounds && results.all { it.status == "completed" }) Icons.Rounded.CheckCircle else if (step.phase == TestPhase.DOWNLOAD) Icons.Rounded.Download else Icons.Rounded.Upload, null, tint = if (step.phase == TestPhase.DOWNLOAD) downColor else upColor)
                        Column(Modifier.weight(1f)) {
                            val direction = stringResource(if (step.phase == TestPhase.DOWNLOAD) R.string.metric_download else R.string.metric_upload)
                            Text("${index + 1} · $direction · ${payloadLabel(step.bytes)}", style = MaterialTheme.typography.titleSmallEmphasized)
                            Text(stringResource(R.string.step_rounds, if (active) activeRound else results.size, step.rounds), style = MaterialTheme.typography.labelMedium)
                        }
                        val rates = results.mapNotNull { it.mbps }
                        if (rates.isNotEmpty()) Text("${formatSpeed(unit.fromMbps(rates.average()))} ${unit.label()}", style = MaterialTheme.typography.labelLarge)
                    }
                    if (active) LinearWavyProgressIndicator(progress = { results.size.toFloat() / step.rounds }, modifier = Modifier.fillMaxWidth())
                    if (expanded) {
                        if (results.isEmpty()) Text(stringResource(R.string.step_pending), style = MaterialTheme.typography.bodySmall)
                        results.forEach { sample ->
                            val status = when (sample.status) {
                                "completed" -> R.string.sample_completed
                                "timeout" -> R.string.sample_timeout
                                "failed" -> R.string.sample_failed
                                else -> R.string.sample_skipped
                            }
                            Text("#${sample.round} · ${stringResource(status)}" + (sample.mbps?.let { " · ${formatSpeed(unit.fromMbps(it))} ${unit.label()}" } ?: ""), style = MaterialTheme.typography.labelLarge)
                            sample.durationMs?.let { Text(stringResource(R.string.sample_duration, formatMillis(it)), style = MaterialTheme.typography.bodySmall) }
                            sample.latencyMs?.let { Text(stringResource(R.string.sample_latency, formatMillis(it)), style = MaterialTheme.typography.bodySmall) }
                            sample.serverTimeMs?.let { Text(stringResource(R.string.sample_server, formatMillis(it)), style = MaterialTheme.typography.bodySmall) }
                            sample.responseBytes?.let { Text(stringResource(R.string.sample_response, it), style = MaterialTheme.typography.bodySmall) }
                            if (sample.loadedLatencies.isNotEmpty()) Text(stringResource(R.string.sample_loaded, formatMillis(Statistics.median(sample.loadedLatencies)), sample.loadedLatencies.size), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun payloadLabel(bytes: Long): String = if (bytes < 1_000_000) "${bytes / 1000} kB" else "${bytes / 1_000_000} MB"
