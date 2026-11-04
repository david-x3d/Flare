package dev.flare.speedtest.ui.test

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed
import dev.flare.speedtest.ui.components.label
import dev.flare.speedtest.ui.theme.Flare
import dev.flare.speedtest.ui.theme.Navy
import dev.flare.speedtest.ui.theme.NumberStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE = 140f
private const val SWEEP_ANGLE = 260f

/** Where the arc's tick labels sit, in Mbit/s; matches [GaugeScale]. */
private val TickMbps = listOf(0.0, 5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2000.0)

private fun tickLabel(mbps: Double, unit: SpeedUnit): String {
    val v = unit.fromMbps(mbps)
    return when {
        v >= 1000 -> "${(v / 1000).toInt()}G"
        v == 0.0 -> "0"
        v < 10 && v % 1.0 != 0.0 -> String.format(Locale.ROOT, "%.1f", v)
        else -> v.toInt().toString()
    }
}

private fun TestUiState.arcFraction(): Float = when (phase) {
    TestPhase.IDLE -> 0f
    TestPhase.PING -> (progress / 0.1f).coerceIn(0f, 1f)
    TestPhase.DOWNLOAD, TestPhase.UPLOAD -> GaugeScale.fraction(liveMbps)
    TestPhase.DONE -> GaugeScale.fraction(downloadMbps ?: 0.0)
}

/**
 * The hero of the test screen: a 260° arc with a flame gradient that follows the live speed. While
 * idle, its centre is the start button: Flare's cookie-shaped logo, gently turning and sending out
 * pulses.
 */
@Composable
fun SpeedDial(state: TestUiState, onStart: () -> Unit, size: Dp, modifier: Modifier = Modifier) {
    val palette = Flare.palette
    val colors = MaterialTheme.colorScheme
    val idle = state.phase == TestPhase.IDLE

    val accent by animateColorAsState(
        when (state.phase) {
            TestPhase.PING -> palette.latency
            TestPhase.UPLOAD -> palette.upload
            else -> palette.download
        },
        tween(450), label = "accent",
    )
    val accentStart by animateColorAsState(
        when (state.phase) {
            TestPhase.PING -> palette.latency.copy(alpha = 0.55f)
            TestPhase.UPLOAD -> lerp(palette.upload, Color.White, 0.45f)
            else -> palette.latency
        },
        tween(450), label = "accentStart",
    )
    val fraction by animateFloatAsState(
        state.arcFraction(),
        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "arc",
    )
    val labelAlpha by animateFloatAsState(if (idle) 0.45f else 1f, tween(400), label = "labels")

    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = (size.value * 0.04f).coerceIn(10f, 14f).sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
    val labels = remember(state.unit, labelStyle) { TickMbps.map { measurer.measure(tickLabel(it, state.unit), labelStyle) } }
    val track = colors.surfaceContainerHighest
    val tickColor = colors.onSurfaceVariant
    val description = stringResource(R.string.cd_gauge)

    Box(modifier.size(size).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.05f
            val inset = stroke * 1.5f
            val radius = this.size.minDimension / 2f - inset
            val arcTopLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            drawArc(track, START_ANGLE, SWEEP_ANGLE, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))

            TickMbps.forEachIndexed { index, _ ->
                val angle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * index / (TickMbps.size - 1)).toDouble())
                val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                val passed = GaugeScale.fraction(TickMbps[index]) <= fraction + 0.001f && !idle
                drawCircle(
                    color = if (passed) accent else tickColor.copy(alpha = 0.45f * labelAlpha),
                    radius = stroke * 0.12f,
                    center = center + dir * (radius - stroke * 1.25f),
                )
                val layout = labels[index]
                val labelCenter = center + dir * (radius - stroke * 2.5f)
                drawText(
                    layout,
                    color = tickColor.copy(alpha = labelAlpha * if (passed) 1f else 0.75f),
                    topLeft = labelCenter - Offset(layout.size.width / 2f, layout.size.height / 2f),
                )
            }

            if (fraction > 0.002f) {
                val sweep = SWEEP_ANGLE * fraction
                val brush = Brush.sweepGradient(
                    0f to accentStart, (SWEEP_ANGLE / 360f) to accent, 1f to accentStart,
                    center = center,
                )
                rotate(START_ANGLE, center) {
                    // A wide, faint copy underneath reads as glow.
                    drawArc(brush, 0f, sweep, false, arcTopLeft, arcSize, alpha = 0.22f, style = Stroke(stroke * 2.4f, cap = StrokeCap.Round))
                    drawArc(brush, 0f, sweep, false, arcTopLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                val headAngle = Math.toRadians((START_ANGLE + sweep).toDouble())
                val head = center + Offset(cos(headAngle).toFloat(), sin(headAngle).toFloat()) * radius
                drawCircle(accent.copy(alpha = 0.35f), radius = stroke * 1.05f, center = head)
                drawCircle(Color.White, radius = stroke * 0.34f, center = head)
            }
        }

        AnimatedContent(
            targetState = idle,
            transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.85f)) togetherWith (fadeOut(tween(200)) + scaleOut(targetScale = 1.08f)) },
            label = "dialCenter",
        ) { isIdle ->
            if (isIdle) StartButton(onStart, size * 0.46f) else DialReading(state, size, accent)
        }
    }
}

@Composable
private fun StartButton(onStart: () -> Unit, buttonSize: Dp) {
    val primary = MaterialTheme.colorScheme.primary
    val infinite = rememberInfiniteTransition(label = "start")
    val pulse by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing)), label = "pulse")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "spin")
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "press")
    val shape = MaterialShapes.Cookie12Sided.toShape()
    val cd = stringResource(R.string.cd_start)

    Box(Modifier.size(buttonSize * 1.3f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val base = buttonSize.toPx() / 2f
            repeat(2) { k ->
                val p = (pulse + k * 0.5f) % 1f
                drawCircle(primary.copy(alpha = (1f - p) * 0.4f), radius = base * (1f + 0.28f * p), style = Stroke(2.dp.toPx()))
            }
        }
        Box(
            Modifier
                .size(buttonSize)
                .scale(press)
                .clip(shape)
                .clickable(interactionSource = source, indication = null, role = Role.Button, onClickLabel = cd, onClick = onStart)
                .semantics { contentDescription = cd },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = spin }
                    .clip(shape)
                    .background(Brush.radialGradient(listOf(lerp(primary, Color.White, 0.18f), primary))),
            )
            Text(
                stringResource(R.string.test_start).uppercase(),
                color = Navy,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
            )
        }
    }
}

@Composable
private fun DialReading(state: TestUiState, size: Dp, accent: Color) {
    val palette = Flare.palette
    val (value, isLatency) = when (state.phase) {
        TestPhase.PING -> state.livePingMs to true
        TestPhase.DONE -> state.downloadMbps?.let { state.unit.fromMbps(it) } to false
        else -> state.unit.fromMbps(state.liveMbps) to false
    }
    val (icon, caption) = when (state.phase) {
        TestPhase.PING -> Icons.Rounded.NetworkPing to R.string.phase_ping
        TestPhase.UPLOAD -> Icons.Rounded.ArrowUpward to R.string.phase_upload
        else -> Icons.Rounded.ArrowDownward to R.string.phase_download
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            Text(stringResource(caption).uppercase(), color = accent, style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.5.sp))
        }
        if (value == null) {
            LoadingIndicator(color = accent, modifier = Modifier.size(size * 0.22f))
        } else {
            val shown by animateFloatAsState(
                value.toFloat(),
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "number",
            )
            val v = shown.coerceAtLeast(0f).toDouble()
            Text(
                if (isLatency) formatMillis(v) else formatSpeed(v),
                style = NumberStyle.copy(fontSize = (size.value * 0.19f).sp, lineHeight = (size.value * 0.21f).sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                if (isLatency) stringResource(R.string.unit_ms) else state.unit.label(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.phase == TestPhase.DONE && state.uploadMbps != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.semantics(mergeDescendants = true) {},
            ) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = palette.upload, modifier = Modifier.size(16.dp))
                Text(
                    "${formatSpeed(state.unit.fromMbps(state.uploadMbps))} ${state.unit.label()}",
                    style = MaterialTheme.typography.titleSmall.merge(NumberStyle.copy(fontWeight = FontWeight.SemiBold)),
                    color = palette.upload,
                )
            }
        }
    }
}
