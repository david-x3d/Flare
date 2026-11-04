package dev.flare.speedtest.ui.test

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.NetworkPing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.SpeedTestRecord
import dev.flare.speedtest.domain.TestMode
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.TestSize
import dev.flare.speedtest.ui.components.FlareCard
import dev.flare.speedtest.ui.components.NetworkChip
import dev.flare.speedtest.ui.components.SectionLabel
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed
import dev.flare.speedtest.ui.components.label
import dev.flare.speedtest.ui.theme.Flare
import dev.flare.speedtest.ui.theme.NumberStyle

@Composable
fun TestScreen(
    state: TestUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDismissError: () -> Unit,
    onTestSizeSelected: (TestSize) -> Unit,
    twoPane: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    onMode: (TestMode) -> Unit = {},
    lastRecord: SpeedTestRecord? = null,
    onOpenRecord: (SpeedTestRecord) -> Unit = {},
) {
    // A running test must not be interrupted by the screen timing out.
    val view = LocalView.current
    DisposableEffect(state.isRunning, state.keepScreenOn) {
        view.keepScreenOn = state.isRunning && state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    PhaseHaptics(state)
    val onShare = rememberShare(state.result, state.unit)

    if (twoPane) {
        Row(
            modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(
                Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Hero(state, onStart, onStop, onShare, onMode, onTestSizeSelected, maxDial = 380.dp)
            }
            Column(
                Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 72.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Details(state, onStart, onDismissError, lastRecord, onOpenRecord)
            }
        }
    } else {
        Column(
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 20.dp, bottom = bottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.widthIn(max = 560.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Hero(state, onStart, onStop, onShare, onMode, onTestSizeSelected, maxDial = 340.dp)
                Details(state, onStart, onDismissError, lastRecord, onOpenRecord)
            }
        }
    }
}

@Composable
private fun ColumnScope.Hero(
    state: TestUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onShare: () -> Unit,
    onMode: (TestMode) -> Unit,
    onTestSize: (TestSize) -> Unit,
    maxDial: Dp,
) {
    Header(state)
    AnimatedContent(
        targetState = state.isRunning,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "top",
        modifier = Modifier.fillMaxWidth(),
    ) { running ->
        if (running) PhaseTrack(state, Modifier.padding(vertical = 8.dp)) else ModeSelector(state, onMode, onTestSize)
    }
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        SpeedDial(state, onStart, size = minOf(maxWidth, maxDial))
    }
    AnimatedContent(
        targetState = when {
            state.isRunning -> 1
            state.phase == TestPhase.DONE -> 2
            else -> 0
        },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "controls",
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) { which ->
        when (which) {
            1 -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { StopButton(onStop) }
            2 -> DoneButtons(onAgain = onStart, onShare = onShare)
            else -> Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.test_tap_hint), style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.test_server_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Header(state: TestUiState) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(painterResource(R.drawable.flare_logo), contentDescription = null, modifier = Modifier.size(30.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black))
        Spacer(Modifier.weight(1f))
        NetworkChip(state.networkType, detail = state.info?.isp)
    }
}

@Composable
private fun ColumnScope.Details(
    state: TestUiState,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    lastRecord: SpeedTestRecord?,
    onOpenRecord: (SpeedTestRecord) -> Unit,
) {
    ErrorSection(state, onRetry, onDismissError)
    val showMetrics = state.phase != TestPhase.IDLE
    AnimatedVisibility(showMetrics, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MetricTiles(state)
            if (state.downloadSeries.size > 1 || state.uploadSeries.size > 1) {
                LiveThroughputCard(state.downloadSeries, state.uploadSeries)
            }
        }
    }
    state.result?.let { ResultSummary(it, state.networkType) }
    if (state.mode == TestMode.FULL && (state.isRunning || state.samples.isNotEmpty())) {
        DetailedResults(state.samples, state.unit, state.stepIndex, state.stepRound, state.isRunning)
    }
    if (state.phase == TestPhase.IDLE && state.error == null && lastRecord != null) {
        LastResultCard(lastRecord, state, onOpenRecord)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun LastResultCard(record: SpeedTestRecord, state: TestUiState, onOpen: (SpeedTestRecord) -> Unit) {
    val palette = Flare.palette
    val m = record.measurement
    val unit = state.unit
    FlareCard(Modifier.fillMaxWidth(), padding = 18.dp) {
        SectionLabel(
            stringResource(R.string.last_test) + " · " +
                DateUtils.getRelativeTimeSpanString(record.timestampMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LastValue(Icons.Rounded.ArrowDownward, palette.download, formatSpeed(unit.fromMbps(m.downloadMbps)), unit.label())
            LastValue(Icons.Rounded.ArrowUpward, palette.upload, formatSpeed(unit.fromMbps(m.uploadMbps)), unit.label())
            LastValue(Icons.Rounded.NetworkPing, palette.latency, formatMillis(m.pingMs), stringResource(R.string.unit_ms))
        }
        androidx.compose.material3.TextButton(onClick = { onOpen(record) }, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.last_test_details))
        }
    }
}

@Composable
private fun LastValue(icon: ImageVector, color: Color, value: String, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.merge(NumberStyle))
        Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorSection(state: TestUiState, onRetry: () -> Unit, onDismiss: () -> Unit) {
    // Keep the last error around while the card animates out.
    val lastError = remember { mutableStateOf(state.error) }
    if (state.error != null) lastError.value = state.error
    AnimatedVisibility(
        visible = state.error != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        lastError.value?.let { ErrorCard(it, onRetry = onRetry, onDismiss = onDismiss) }
    }
}

/** A soft tick when a phase starts, a confirmation at the end, a reject buzz on failure. */
@Composable
private fun PhaseHaptics(state: TestUiState) {
    val haptics = LocalHapticFeedback.current
    var previous by remember { mutableStateOf(state.phase) }
    var hadError by remember { mutableStateOf(state.error != null) }
    LaunchedEffect(state.phase, state.error != null) {
        if (state.haptics) {
            when {
                state.error != null && !hadError -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
                state.phase != previous && state.phase == TestPhase.DONE -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                state.phase != previous && state.isRunning -> haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
        previous = state.phase
        hadError = state.error != null
    }
}
