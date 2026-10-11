package dev.flare.speedtest.ui.test

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.TestPhase

private data class Step(val phase: TestPhase, val icon: ImageVector, val label: Int)

private val Steps = listOf(
    Step(TestPhase.PING, Icons.Rounded.NetworkPing, R.string.phase_ping),
    Step(TestPhase.DOWNLOAD, Icons.Rounded.Download, R.string.phase_download),
    Step(TestPhase.UPLOAD, Icons.Rounded.Upload, R.string.phase_upload),
)

/** Ping -> Download -> Upload, with the active step grown and highlighted, plus overall progress. */
@Composable
fun PhaseStepper(phase: TestPhase, progress: Float, modifier: Modifier = Modifier) {
    val motion = MaterialTheme.motionScheme
    val animatedProgress by animateFloatAsState(progress, motion.slowSpatialSpec(), label = "overall")
    val running = phase == TestPhase.PING || phase == TestPhase.DOWNLOAD || phase == TestPhase.UPLOAD

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Steps.forEach { step ->
                val state = when {
                    phase == TestPhase.DONE || phase.ordinal > step.phase.ordinal -> StepState.Done
                    phase == step.phase -> StepState.Active
                    else -> StepState.Pending
                }
                StepChip(step, state, modifier = Modifier.weight(if (state == StepState.Active) 1.25f else 1f))
            }
        }
        LinearWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            amplitude = { p -> WavyProgressIndicatorDefaults.indicatorAmplitude(p) * if (running) 1f else 0f },
        )
    }
}

private enum class StepState { Pending, Active, Done }

@Composable
private fun StepChip(step: Step, state: StepState, modifier: Modifier) {
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when (state) {
            StepState.Active -> colors.primaryContainer
            StepState.Done -> colors.tertiaryContainer
            StepState.Pending -> colors.surfaceContainerHigh
        },
        motion.defaultEffectsSpec(),
        label = "stepContainer",
    )
    val content by animateColorAsState(
        when (state) {
            StepState.Active -> colors.onPrimaryContainer
            StepState.Done -> colors.onTertiaryContainer
            StepState.Pending -> colors.onSurfaceVariant
        },
        motion.defaultEffectsSpec(),
        label = "stepContent",
    )
    val corner by animateFloatAsState(if (state == StepState.Active) 28f else 16f, motion.defaultSpatialSpec(), label = "stepCorner")

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(corner.coerceAtLeast(0f).dp),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                if (state == StepState.Done) Icons.Rounded.Check else step.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(step.label),
                modifier = Modifier.padding(start = 6.dp),
                maxLines = 1,
                style = if (state == StepState.Active) MaterialTheme.typography.labelLargeEmphasized else MaterialTheme.typography.labelLarge,
            )
        }
    }
}
