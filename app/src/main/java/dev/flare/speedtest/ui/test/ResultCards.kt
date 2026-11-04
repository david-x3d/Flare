package dev.flare.speedtest.ui.test

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.ui.components.FlareCard
import dev.flare.speedtest.ui.components.SectionLabel
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed
import dev.flare.speedtest.ui.components.label
import dev.flare.speedtest.ui.theme.Flare
import dev.flare.speedtest.ui.theme.NumberStyle

/** Ping, download and upload side by side; the tile of the running phase lights up. */
@Composable
fun MetricTiles(state: TestUiState, modifier: Modifier = Modifier) {
    val palette = Flare.palette
    val ms = stringResource(R.string.unit_ms)
    val unit = state.unit.label()
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricTile(
            label = stringResource(R.string.metric_ping),
            value = (state.pingMs ?: state.livePingMs)?.let(::formatMillis),
            unit = ms,
            sub = state.jitterMs?.let { stringResource(R.string.jitter_value, formatMillis(it)) },
            color = palette.latency,
            active = state.phase == TestPhase.PING,
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = stringResource(R.string.metric_download),
            value = (state.downloadMbps ?: state.liveMbps.takeIf { state.phase == TestPhase.DOWNLOAD })
                ?.let { formatSpeed(state.unit.fromMbps(it)) },
            unit = unit,
            sub = state.loadedDownMs?.let { stringResource(R.string.loaded_value, formatMillis(it)) },
            color = palette.download,
            active = state.phase == TestPhase.DOWNLOAD,
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            label = stringResource(R.string.metric_upload),
            value = (state.uploadMbps ?: state.liveMbps.takeIf { state.phase == TestPhase.UPLOAD })
                ?.let { formatSpeed(state.unit.fromMbps(it)) },
            unit = unit,
            sub = state.loadedUpMs?.let { stringResource(R.string.loaded_value, formatMillis(it)) },
            color = palette.upload,
            active = state.phase == TestPhase.UPLOAD,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricTile(label: String, value: String?, unit: String, sub: String?, color: Color, active: Boolean, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (active) color.copy(alpha = 0.12f).compositeOverSurface(colors.surfaceContainerLow) else colors.surfaceContainerLow,
        label = "tile",
    )
    val pop by animateFloatAsState(if (value != null) 1f else 0.97f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium), label = "pop")
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(24.dp),
        color = container,
        border = if (active) BorderStroke(1.5.dp, color.copy(alpha = 0.7f)) else null,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).background(color, CircleShape))
                Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                value ?: "0",
                style = MaterialTheme.typography.headlineSmall.merge(NumberStyle),
                color = if (value == null) colors.onSurfaceVariant.copy(alpha = 0.5f) else colors.onSurface,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp).graphicsLayer { scaleX = pop; scaleY = pop; transformOrigin = TransformOrigin(0f, 0.5f) },
            )
            Text(
                sub ?: unit,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun Color.compositeOverSurface(surface: Color): Color {
    val a = alpha
    return Color(
        red = red * a + surface.red * (1 - a),
        green = green * a + surface.green * (1 - a),
        blue = blue * a + surface.blue * (1 - a),
        alpha = 1f,
    )
}

/** Live throughput of both directions as a sparkline, so you can see how steady the link is. */
@Composable
fun LiveThroughputCard(download: List<Float>, upload: List<Float>, modifier: Modifier = Modifier) {
    val palette = Flare.palette
    FlareCard(modifier, padding = 18.dp) {
        SectionLabel(stringResource(R.string.live_chart_title)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(palette.download, stringResource(R.string.metric_download))
                LegendDot(palette.upload, stringResource(R.string.metric_upload))
            }
        }
        val grid = MaterialTheme.colorScheme.outlineVariant
        Canvas(Modifier.fillMaxWidth().height(84.dp)) {
            val max = ((download + upload).maxOrNull() ?: 1f).coerceAtLeast(1f) * 1.1f
            for (i in 1..2) {
                val y = size.height * i / 3f
                drawLine(grid.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            drawSpark(upload, max, palette.upload, fill = true)
            drawSpark(download, max, palette.download, fill = true)
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun DrawScope.drawSpark(values: List<Float>, max: Float, color: Color, fill: Boolean) {
    if (values.size < 2) return
    val step = size.width / (values.size - 1)
    fun y(v: Float) = size.height - (v / max).coerceIn(0f, 1f) * size.height
    val line = Path().apply {
        moveTo(0f, y(values[0]))
        for (i in 1 until values.size) {
            val x0 = step * (i - 1)
            val x1 = step * i
            val mid = (x0 + x1) / 2f
            cubicTo(mid, y(values[i - 1]), mid, y(values[i]), x1, y(values[i]))
        }
    }
    if (fill) {
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), color.copy(alpha = 0f))))
    }
    drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}
