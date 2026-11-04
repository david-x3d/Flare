package dev.flare.speedtest.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import dev.flare.speedtest.ui.components.FlareCard
import dev.flare.speedtest.ui.components.SectionLabel
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed
import dev.flare.speedtest.ui.components.label
import dev.flare.speedtest.ui.theme.Flare
import java.text.DateFormat
import java.util.Date

/** Maximum number of tests the trend covers. */
private const val CHART_POINTS = 30

enum class TrendMetric { DOWNLOAD, UPLOAD, PING }

/** One metric over the most recent tests, with its average as a dashed guide. */
@Composable
fun TrendCard(records: List<SpeedTestRecord>, unit: SpeedUnit, modifier: Modifier = Modifier) {
    val palette = Flare.palette
    val colors = MaterialTheme.colorScheme
    var metric by rememberSaveable { mutableStateOf(TrendMetric.DOWNLOAD) }
    // Oldest first, so the line runs left to right through time.
    val points = remember(records) { records.take(CHART_POINTS).reversed() }
    val values = remember(points, unit, metric) {
        points.map {
            when (metric) {
                TrendMetric.DOWNLOAD -> unit.fromMbps(it.measurement.downloadMbps)
                TrendMetric.UPLOAD -> unit.fromMbps(it.measurement.uploadMbps)
                TrendMetric.PING -> it.measurement.pingMs
            }
        }
    }
    val color = when (metric) {
        TrendMetric.DOWNLOAD -> palette.download
        TrendMetric.UPLOAD -> palette.upload
        TrendMetric.PING -> palette.latency
    }
    val maxValue = remember(values) { niceCeiling(values.maxOrNull() ?: 1.0) }
    val average = if (values.isEmpty()) 0.0 else values.average()
    val format: (Double) -> String = if (metric == TrendMetric.PING) { v -> formatMillis(v) } else { v -> formatSpeed(v) }
    val unitLabel = if (metric == TrendMetric.PING) stringResource(R.string.unit_ms) else unit.label()

    val reveal = remember { Animatable(0f) }
    val motion = MaterialTheme.motionScheme
    LaunchedEffect(points, metric) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, motion.slowSpatialSpec())
    }

    FlareCard(modifier) {
        SectionLabel(stringResource(R.string.chart_title)) {
            Text(
                stringResource(R.string.chart_average, "${format(average)} $unitLabel"),
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrendMetric.entries.forEach { option ->
                val optionColor = when (option) {
                    TrendMetric.DOWNLOAD -> palette.download
                    TrendMetric.UPLOAD -> palette.upload
                    TrendMetric.PING -> palette.latency
                }
                FilterChip(
                    selected = metric == option,
                    onClick = { metric = option },
                    label = {
                        Text(
                            stringResource(
                                when (option) {
                                    TrendMetric.DOWNLOAD -> R.string.metric_download
                                    TrendMetric.UPLOAD -> R.string.metric_upload
                                    TrendMetric.PING -> R.string.metric_ping
                                },
                            ),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = optionColor.copy(alpha = 0.18f),
                        selectedLabelColor = colors.onSurface,
                    ),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            val grid = colors.outlineVariant
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                drawGrid(grid)
                drawAverage(average, maxValue, color)
                clipRect(right = size.width * reveal.value) {
                    drawSeries(values, maxValue, color)
                }
            }
            Text(
                "${format(maxValue)} $unitLabel",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
        if (points.size > 1) {
            val dateFormat = remember { DateFormat.getDateInstance(DateFormat.SHORT) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateFormat.format(Date(points.first().timestampMillis)), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(dateFormat.format(Date(points.last().timestampMillis)), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
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

private fun DrawScope.chartTop() = 18.dp.toPx()
private fun DrawScope.chartBottom() = size.height - 6.dp.toPx()

private fun DrawScope.drawGrid(color: Color) {
    for (i in 0..2) {
        val y = chartTop() + (chartBottom() - chartTop()) * i / 2f
        drawLine(color.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
    }
}

private fun DrawScope.drawAverage(average: Double, max: Double, color: Color) {
    if (average <= 0) return
    val y = chartBottom() - (chartBottom() - chartTop()) * (average / max).toFloat().coerceIn(0f, 1f)
    drawLine(
        color.copy(alpha = 0.6f), Offset(0f, y), Offset(size.width, y),
        strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
    )
}

private fun DrawScope.drawSeries(values: List<Double>, max: Double, color: Color) {
    if (values.isEmpty()) return
    val top = chartTop()
    val bottom = chartBottom()
    val left = 6.dp.toPx()
    val right = size.width - 6.dp.toPx()
    fun x(i: Int) = if (values.size == 1) (left + right) / 2f else left + (right - left) * i / (values.size - 1)
    fun y(v: Double) = bottom - (bottom - top) * (v / max).toFloat().coerceIn(0f, 1f)

    val line = Path().apply {
        moveTo(x(0), y(values[0]))
        for (i in 1 until values.size) {
            // Cubic segments with horizontal tangents give a smooth curve that never overshoots.
            val midX = (x(i - 1) + x(i)) / 2f
            cubicTo(midX, y(values[i - 1]), midX, y(values[i]), x(i), y(values[i]))
        }
    }
    if (values.size > 1) {
        val area = Path().apply {
            addPath(line)
            lineTo(x(values.lastIndex), bottom)
            lineTo(x(0), bottom)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0f)), startY = top, endY = bottom))
    }
    drawPath(line, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    values.indices.forEach { i ->
        drawCircle(color, radius = 2.5.dp.toPx(), center = Offset(x(i), y(values[i])))
    }
    val last = values.lastIndex
    drawCircle(color.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = Offset(x(last), y(values[last])))
    drawCircle(color, radius = 4.5.dp.toPx(), center = Offset(x(last), y(values[last])))
}
