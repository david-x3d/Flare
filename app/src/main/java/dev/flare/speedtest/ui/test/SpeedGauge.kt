package dev.flare.speedtest.ui.test

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed
import dev.flare.speedtest.ui.components.rememberChainedMorphShape

/** The shapes the centre badge morphs through: one per phase, in test order. */
private val BadgePolygons = listOf(
    MaterialShapes.Cookie9Sided, // idle: inviting start button
    MaterialShapes.Clover4Leaf, // ping
    MaterialShapes.SoftBurst, // download
    MaterialShapes.Flower, // upload
    MaterialShapes.Cookie12Sided, // done
)

private fun TestPhase.badgeIndex() = when (this) {
    TestPhase.IDLE -> 0
    TestPhase.PING -> 1
    TestPhase.DOWNLOAD -> 2
    TestPhase.UPLOAD -> 3
    TestPhase.DONE -> 4
}

/** What the gauge shows in its middle for the current state. */
private class GaugeReading(val value: Double?, val isLatency: Boolean, @androidx.annotation.StringRes val caption: Int?)

private fun TestUiState.reading(): GaugeReading = when (phase) {
    TestPhase.IDLE -> GaugeReading(null, false, null)
    TestPhase.PING -> GaugeReading(livePingMs, true, R.string.metric_ping)
    TestPhase.DOWNLOAD -> GaugeReading(unit.fromMbps(liveMbps), false, R.string.metric_download)
    TestPhase.UPLOAD -> GaugeReading(unit.fromMbps(liveMbps), false, R.string.metric_upload)
    TestPhase.DONE -> GaugeReading(downloadMbps?.let { unit.fromMbps(it) }, false, R.string.metric_download)
}

private fun TestUiState.ringTarget(): Float = when (phase) {
    TestPhase.IDLE -> 0f
    TestPhase.PING -> (progress / 0.1f).coerceIn(0f, 1f)
    TestPhase.DOWNLOAD, TestPhase.UPLOAD -> GaugeScale.fraction(liveMbps)
    TestPhase.DONE -> GaugeScale.fraction(downloadMbps ?: 0.0)
}

/**
 * The hero of the screen: a wavy ring that fills with the measured speed and, in its middle, a badge
 * that morphs from shape to shape as the test moves through its phases. In idle the badge *is* the
 * start button.
 */
@Composable
fun SpeedGauge(
    state: TestUiState,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 320.dp,
) {
    val motion = MaterialTheme.motionScheme
    val colors = MaterialTheme.colorScheme
    val running = state.isRunning
    val idle = state.phase == TestPhase.IDLE

    val ring by animateFloatAsState(state.ringTarget(), motion.slowSpatialSpec(), label = "ring")
    val waveAmplitude by animateFloatAsState(if (running) 1f else 0f, motion.defaultEffectsSpec(), label = "amplitude")

    val ringColor by animateColorAsState(
        if (state.phase == TestPhase.UPLOAD) colors.tertiary else colors.primary,
        motion.defaultEffectsSpec(),
        label = "ringColor",
    )
    val badgeColor by animateColorAsState(
        when (state.phase) {
            TestPhase.IDLE -> colors.primary
            TestPhase.DONE -> colors.tertiaryContainer
            TestPhase.UPLOAD -> colors.secondaryContainer
            else -> colors.primaryContainer
        },
        motion.defaultEffectsSpec(),
        label = "badgeColor",
    )
    val onBadge by animateColorAsState(
        when (state.phase) {
            TestPhase.IDLE -> colors.onPrimary
            TestPhase.DONE -> colors.onTertiaryContainer
            TestPhase.UPLOAD -> colors.onSecondaryContainer
            else -> colors.onPrimaryContainer
        },
        motion.defaultEffectsSpec(),
        label = "onBadgeColor",
    )

    val badgeShape = rememberChainedMorphShape(BadgePolygons, state.phase.badgeIndex())

    // The badge turns slowly while a test runs and breathes while it waits to be tapped.
    val infinite = rememberInfiniteTransition(label = "gauge")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 18_000, easing = LinearEasing)),
        label = "rotation",
    )
    val breathing by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1_400), RepeatMode.Reverse),
        label = "breathing",
    )
    val rotationScale by animateFloatAsState(if (running) 1f else 0f, spring(stiffness = Spring.StiffnessLow), label = "rot")

    val density = LocalDensity.current
    val ringStroke = remember(size, density) { Stroke(width = with(density) { (size.value * 0.03f).dp.toPx() }, cap = StrokeCap.Round) }
    val trackStroke = remember(size, density) { Stroke(width = with(density) { (size.value * 0.022f).dp.toPx() }, cap = StrokeCap.Round) }
    val gaugeDescription = stringResource(R.string.cd_gauge)

    Box(
        modifier = modifier.size(size).semantics { contentDescription = gaugeDescription },
        contentAlignment = Alignment.Center,
    ) {
        CircularWavyProgressIndicator(
            progress = { ring },
            modifier = Modifier.fillMaxSize(),
            color = ringColor,
            trackColor = colors.surfaceContainerHighest,
            stroke = ringStroke,
            trackStroke = trackStroke,
            amplitude = { p -> WavyProgressIndicatorDefaults.indicatorAmplitude(p) * waveAmplitude },
            wavelength = (size.value * 0.17f).dp,
        )

        val badgeSize = size * 0.66f
        Box(
            modifier = Modifier
                .size(badgeSize)
                .scale(if (idle) breathing else 1f)
                .graphicsLayer { rotationZ = rotation * rotationScale }
                .clip(badgeShape)
                .background(badgeColor)
                .then(if (idle) Modifier.clickable(role = Role.Button, onClickLabel = stringResource(R.string.test_start), onClick = onStart) else Modifier),
        )

        GaugeCenter(state = state, size = size, contentColor = onBadge, modifier = Modifier.size(badgeSize * 0.78f))
    }
}

@Composable
private fun GaugeCenter(state: TestUiState, size: Dp, contentColor: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val reading = state.reading()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            state.phase == TestPhase.IDLE -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    androidx.compose.ui.res.painterResource(R.drawable.flare_logo),
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.Unspecified,
                    modifier = Modifier.size(size * 0.2f),
                )
                Text(
                    stringResource(R.string.test_start),
                    color = contentColor,
                    style = MaterialTheme.typography.headlineMediumEmphasized,
                    textAlign = TextAlign.Center,
                )
            }

            reading.value == null -> LoadingIndicator(color = contentColor, modifier = Modifier.size(size * 0.28f))

            else -> {
                // Numbers chase their target on a spring instead of jumping.
                val shown by animateFloatAsState(
                    targetValue = reading.value.toFloat(),
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "number",
                )
                val text = if (reading.isLatency) formatMillis(shown.coerceAtLeast(0f).toDouble()) else formatSpeed(shown.coerceAtLeast(0f).toDouble())
                val unitLabel = if (reading.isLatency) stringResource(R.string.unit_ms) else state.unit.label()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text,
                        color = contentColor,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.displayLargeEmphasized.copy(fontSize = (size.value * 0.17f).sp, lineHeight = (size.value * 0.19f).sp),
                    )
                    Text(unitLabel, color = contentColor, style = MaterialTheme.typography.titleLargeEmphasized)
                    reading.caption?.let {
                        Text(
                            stringResource(it),
                            color = contentColor.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedUnit.label(): String = stringResource(
    when (this) {
        SpeedUnit.MBIT_PER_SECOND -> R.string.unit_mbit
        SpeedUnit.MBYTE_PER_SECOND -> R.string.unit_mbyte
    },
)
