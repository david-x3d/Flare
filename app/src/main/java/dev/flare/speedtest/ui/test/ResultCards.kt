package dev.flare.speedtest.ui.test

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed

private enum class Metric { PING, JITTER, DOWNLOAD, UPLOAD }

/** Ping, jitter, download and upload as four cards, each with its own shape, colour and badge. */
@Composable
fun ResultCards(state: TestUiState, columns: Int, modifier: Modifier = Modifier) {
    val unit = state.unit.label()
    val ms = stringResource(R.string.unit_ms)
    val cards = listOf(
        CardModel(
            metric = Metric.DOWNLOAD, title = R.string.metric_download, icon = Icons.Rounded.Download,
            value = state.downloadMbps?.let { formatSpeed(state.unit.fromMbps(it)) }, unit = unit,
            active = state.phase == TestPhase.DOWNLOAD,
        ),
        CardModel(
            metric = Metric.UPLOAD, title = R.string.metric_upload, icon = Icons.Rounded.Upload,
            value = state.uploadMbps?.let { formatSpeed(state.unit.fromMbps(it)) }, unit = unit,
            active = state.phase == TestPhase.UPLOAD,
        ),
        CardModel(
            metric = Metric.PING, title = R.string.metric_ping, icon = Icons.Rounded.NetworkPing,
            value = state.pingMs?.let { formatMillis(it) }, unit = ms,
            active = state.phase == TestPhase.PING,
        ),
        CardModel(
            metric = Metric.JITTER, title = R.string.metric_jitter, icon = Icons.AutoMirrored.Rounded.ShowChart,
            value = state.jitterMs?.let { formatMillis(it) }, unit = ms,
            active = state.phase == TestPhase.PING,
        ),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { ResultCard(it, Modifier.weight(1f)) }
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

private data class CardModel(
    val metric: Metric,
    val title: Int,
    val icon: ImageVector,
    val value: String?,
    val unit: String,
    val active: Boolean,
)

private fun Metric.shape(): Shape = when (this) {
    Metric.DOWNLOAD -> RoundedCornerShape(topStart = 44.dp, topEnd = 16.dp, bottomEnd = 44.dp, bottomStart = 16.dp)
    Metric.UPLOAD -> RoundedCornerShape(topStart = 16.dp, topEnd = 44.dp, bottomEnd = 16.dp, bottomStart = 44.dp)
    Metric.PING -> RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp, bottomEnd = 16.dp, bottomStart = 44.dp)
    Metric.JITTER -> CutCornerShape(topStart = 28.dp, topEnd = 8.dp, bottomEnd = 28.dp, bottomStart = 8.dp)
}

private fun Metric.badge(): RoundedPolygon = when (this) {
    Metric.DOWNLOAD -> MaterialShapes.Cookie9Sided
    Metric.UPLOAD -> MaterialShapes.Sunny
    Metric.PING -> MaterialShapes.Clover4Leaf
    Metric.JITTER -> MaterialShapes.Pentagon
}

@Composable
private fun ResultCard(model: CardModel, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme
    val (container, onContainer, accent, onAccent) = when (model.metric) {
        Metric.DOWNLOAD -> listOf(colors.primaryContainer, colors.onPrimaryContainer, colors.primary, colors.onPrimary)
        Metric.UPLOAD -> listOf(colors.tertiaryContainer, colors.onTertiaryContainer, colors.tertiary, colors.onTertiary)
        Metric.PING -> listOf(colors.secondaryContainer, colors.onSecondaryContainer, colors.secondary, colors.onSecondary)
        Metric.JITTER -> listOf(colors.surfaceContainerHigh, colors.onSurface, colors.outline, colors.surface)
    }
    val hasValue = model.value != null
    val containerColor by animateColorAsState(if (hasValue || model.active) container else colors.surfaceContainerLow, motion.defaultEffectsSpec(), label = "cardColor")
    // A fresh result pops in with a springy scale.
    val pop by animateFloatAsState(if (hasValue) 1f else 0.96f, motion.fastSpatialSpec(), label = "pop")

    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulseAlpha",
    )
    val border = if (model.active) BorderStroke(2.dp, accent.copy(alpha = pulse.value)) else null

    Surface(
        modifier = modifier.scale(pop),
        shape = model.metric.shape(),
        color = containerColor,
        contentColor = if (hasValue || model.active) onContainer else colors.onSurfaceVariant,
        border = border,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(model.metric.badge().toShape())
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(model.icon, contentDescription = null, tint = onAccent, modifier = Modifier.size(20.dp))
                }
                Text(stringResource(model.title), style = MaterialTheme.typography.titleMediumEmphasized)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    model.value ?: stringResource(R.string.value_placeholder),
                    style = MaterialTheme.typography.headlineLargeEmphasized,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    model.unit,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.alignByBaseline().padding(bottom = 2.dp),
                )
            }
        }
    }
}
