package dev.flare.speedtest.ui.test

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import dev.flare.speedtest.R
import dev.flare.speedtest.domain.TestSize
import dev.flare.speedtest.ui.components.NetworkChip
import dev.flare.speedtest.ui.components.formatMillis
import dev.flare.speedtest.ui.components.formatSpeed

@Composable
fun TestScreen(
    state: TestUiState,
    onStart: () -> Unit,
    onMode: (dev.flare.speedtest.domain.TestMode) -> Unit = {},
    onStop: () -> Unit,
    onDismissError: () -> Unit,
    onTestSizeSelected: (TestSize) -> Unit,
    twoPane: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    // A running test must not be interrupted by the screen timing out.
    val view = LocalView.current
    DisposableEffect(state.isRunning, state.keepScreenOn) {
        view.keepScreenOn = state.isRunning && state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    val context = LocalContext.current
    val unitLabel = state.unit.label()
    val shareText = if (state.downloadMbps != null && state.uploadMbps != null && state.pingMs != null && state.jitterMs != null) {
        stringResource(
            R.string.share_text,
            formatSpeed(state.unit.fromMbps(state.downloadMbps)),
            formatSpeed(state.unit.fromMbps(state.uploadMbps)),
            unitLabel,
            formatMillis(state.pingMs),
            formatMillis(state.jitterMs),
        )
    } else {
        ""
    }
    val shareTitle = stringResource(R.string.test_share)
    val onShare = {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(send, shareTitle))
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            androidx.compose.ui.res.painterResource(R.drawable.flare_logo),
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLargeEmphasized)
                    }
                },
                actions = {
                    NetworkChip(state.networkType)
                    Spacer(Modifier.size(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        val contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding)
        if (twoPane) {
            Row(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                ) {
                    GaugeSection(state, onStart, onStop, onShare, onTestSizeSelected, onMode, maxGauge = 360.dp)
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    ErrorSection(state, onStart, onDismissError)
                    ResultCards(state, columns = 2)
                    ConnectionInsights(state)
                    if (state.mode == dev.flare.speedtest.domain.TestMode.FULL) DetailedResults(state.samples, state.unit, state.stepIndex, state.stepRound, state.isRunning)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GaugeSection(state, onStart, onStop, onShare, onTestSizeSelected, onMode, maxGauge = 248.dp)
                ErrorSection(state, onStart, onDismissError)
                ResultCards(state, columns = 2, modifier = Modifier.widthIn(max = 640.dp))
                ConnectionInsights(state)
                    if (state.mode == dev.flare.speedtest.domain.TestMode.FULL) DetailedResults(state.samples, state.unit, state.stepIndex, state.stepRound, state.isRunning)
            }
        }
    }
}

@Composable
private fun GaugeSection(
    state: TestUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onShare: () -> Unit,
    onTestSizeSelected: (TestSize) -> Unit,
    onMode: (dev.flare.speedtest.domain.TestMode) -> Unit,
    maxGauge: Dp,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val gauge = minOf(maxWidth, maxGauge)
        Column(
            modifier = Modifier.widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModePicker(state, onMode)
            SpeedGauge(state = state, onStart = onStart, size = gauge)
            PhaseStepper(phase = state.phase, progress = state.progress, sequential = state.mode != dev.flare.speedtest.domain.TestMode.FULL)
            TestControls(
                state = state,
                onStart = onStart,
                onStop = onStop,
                onShare = onShare,
                onTestSizeSelected = onTestSizeSelected,
            )
        }
    }
}

@Composable
private fun ErrorSection(state: TestUiState, onRetry: () -> Unit, onDismiss: () -> Unit) {
    // Keep the last error around while the card animates out.
    var lastError = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(state.error) }
    if (state.error != null) lastError.value = state.error
    AnimatedVisibility(
        visible = state.error != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        lastError.value?.let { ErrorCard(it, onRetry = onRetry, onDismiss = onDismiss, modifier = Modifier.widthIn(max = 640.dp)) }
    }
}
