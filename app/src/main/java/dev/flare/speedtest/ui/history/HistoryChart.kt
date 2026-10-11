package dev.flare.speedtest.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.SpeedTestRecord
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.ui.components.formatSpeed
import dev.flare.speedtest.ui.test.label

/** Maximum number of tests the sparkline covers. */
private const val CHART_POINTS = 20

/** Download and upload of the most recent tests as two smooth, animated lines. */
@Composable
fun HistoryChartCard(records: List<SpeedTestRecord>, unit: SpeedUnit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    // Oldest first, so the line runs left to right through time.
    val points = remember(records) { records.take(CHART_POINTS).reversed() }
    val download = remember(points, unit) { points.map { unit.fromMbps(it.measurement.downloadMbps) } }
    val upload = remember(points, unit) { points.map { unit.fromMbps(it.measurement.uploadMbps) } }
    val maxValue = remember(download, upload) { niceCeiling((download + upload).maxOrNull() ?: 1.0) }

    val reveal = remember { Animatable(0f) }
    val motion = MaterialTheme.motionScheme
    LaunchedEffect(points) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, motion.slowSpatialSpec())
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = colors.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.history_chart_title), style = MaterialTheme.typography.titleMediumEmphasized)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Legend(colors.primary, stringResource(R.string.metric_download))
                    Legend(colors.tertiary, stringResource(R.string.metric_upload))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val gridColor = colors.outlineVariant
                val downloadColor = colors.primary
                val uploadColor = colors.tertiary
                Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    drawGrid(gridColor)
                    clipRect(right = size.width * reveal.value) {
                        drawSeries(upload, maxValue, uploadColor, fill = false)
                        drawSeries(download, maxValue, downloadColor, fill = true)
                    }
                }
                Text(
                    "${formatSpeed(maxValue)} ${unit.label()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/** Rounds up to 1, 2, 2.5 or 5 times a power of ten so the top grid line gets a friendly label. */
internal fun niceCeiling(value: Double): Double {
    if (value <= 0.0) return 1.0
    var magnitude = 1.0
    while (magnitude * 10 <= value) magnitude *= 10
    while (magnitude > value) magnitude /= 10
    return listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= value }
}

private fun DrawScope.drawGrid(color: Color) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 12f))
    for (i in 0..2) {
        val y = size.height * i / 2f
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
    }
}

private fun DrawScope.drawSeries(values: List<Double>, max: Double, color: Color, fill: Boolean) {
    if (values.isEmpty()) return
    val top = 8.dp.toPx()
    val bottom = size.height - 4.dp.toPx()
    val left = 4.dp.toPx()
    val right = size.width - 12.dp.toPx()
    fun x(i: Int) = if (values.size == 1) (left + right) / 2f else left + (right - left) * i / (values.size - 1)
    fun y(v: Double) = (bottom - (bottom - top) * (v / max).toFloat().coerceIn(0f, 1f))

    val line = Path().apply {
        moveTo(x(0), y(values[0]))
        for (i in 1 until values.size) {
            // Cubic segments with horizontal tangents give a smooth curve that never overshoots.
            val midX = (x(i - 1) + x(i)) / 2f
            cubicTo(midX, y(values[i - 1]), midX, y(values[i]), x(i), y(values[i]))
        }
    }
    if (fill && values.size > 1) {
        val area = Path().apply {
            addPath(line)
            lineTo(x(values.lastIndex), bottom)
            lineTo(x(0), bottom)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)), startY = top, endY = bottom))
    }
    drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    val last = values.lastIndex
    drawCircle(color.copy(alpha = 0.22f), radius = 9.dp.toPx(), center = Offset(x(last), y(values[last])))
    drawCircle(color, radius = 4.5.dp.toPx(), center = Offset(x(last), y(values[last])))
}
