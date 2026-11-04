package dev.flare.speedtest.ui.test

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.TestMode
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.TestPlan
import dev.flare.speedtest.ui.components.formatBytes
import dev.flare.speedtest.ui.theme.Flare

private class Segment(val phase: TestPhase, val label: Int, val from: Float, val to: Float)

private val Segments = listOf(
    Segment(TestPhase.PING, R.string.phase_ping, 0f, 0.1f),
    Segment(TestPhase.DOWNLOAD, R.string.phase_download, 0.1f, 0.55f),
    Segment(TestPhase.UPLOAD, R.string.phase_upload, 0.55f, 1f),
)

/**
 * Where the run is: three labelled segments (Ping, Download, Upload) for Quick; Full interleaves
 * directions, so it gets one bar plus the current step.
 */
@Composable
fun PhaseTrack(state: TestUiState, modifier: Modifier = Modifier) {
    val palette = Flare.palette
    val progress by animateFloatAsState(state.progress, tween(500), label = "progress")
    if (state.mode == TestMode.FULL) {
        val steps = TestPlan.full().detailedSteps
        val step = steps.getOrNull(state.stepIndex)
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (step == null) stringResource(R.string.phase_ping)
                    else "${stringResource(if (step.phase == TestPhase.UPLOAD) R.string.phase_upload else R.string.phase_download)} · ${formatBytes(step.bytes)}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${(progress * 100).toInt()} %",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Bar(progress, if (state.phase == TestPhase.UPLOAD) palette.upload else palette.download)
        }
        return
    }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Segments.forEach { segment ->
            val fill = ((progress - segment.from) / (segment.to - segment.from)).coerceIn(0f, 1f)
            val done = state.phase == TestPhase.DONE || state.phase.ordinal > segment.phase.ordinal
            val active = state.phase == segment.phase
            val color = when (segment.phase) {
                TestPhase.PING -> palette.latency
                TestPhase.UPLOAD -> palette.upload
                else -> palette.download
            }
            val labelColor by animateColorAsState(
                if (active || done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "segmentLabel",
            )
            Column(Modifier.weight(if (segment.phase == TestPhase.PING) 0.8f else 1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (done) Icon(Icons.Rounded.Check, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    Text(stringResource(segment.label), style = MaterialTheme.typography.labelMedium, color = labelColor, maxLines = 1)
                }
                Bar(if (done) 1f else fill, color)
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color),
        )
    }
}
