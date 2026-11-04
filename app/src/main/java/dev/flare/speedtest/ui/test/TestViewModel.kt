package dev.flare.speedtest.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.flare.speedtest.domain.AppSettings
import dev.flare.speedtest.domain.ConnectionInfo
import dev.flare.speedtest.domain.HistoryRepository
import dev.flare.speedtest.domain.Measurement
import dev.flare.speedtest.domain.NetworkMonitor
import dev.flare.speedtest.domain.NetworkType
import dev.flare.speedtest.domain.SettingsRepository
import dev.flare.speedtest.domain.SpeedTestEngine
import dev.flare.speedtest.domain.SpeedTestError
import dev.flare.speedtest.domain.SpeedTestEvent
import dev.flare.speedtest.domain.SpeedTestException
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.Statistics
import dev.flare.speedtest.domain.TestMode
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.TestPlan
import dev.flare.speedtest.domain.TestSize
import dev.flare.speedtest.domain.TransferSample
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TestUiState(
    val phase: TestPhase = TestPhase.IDLE,
    /** 0..1 over the whole run (ping, download, upload). */
    val progress: Float = 0f,
    val liveMbps: Double = 0.0,
    val livePingMs: Double? = null,
    val samples: List<TransferSample> = emptyList(),
    val stepIndex: Int = -1,
    val stepRound: Int = 0,
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    /** Live throughput readings in Mbit/s, oldest first, for the sparkline. */
    val downloadSeries: List<Float> = emptyList(),
    val uploadSeries: List<Float> = emptyList(),
    val loadedDownMs: Double? = null,
    val loadedUpMs: Double? = null,
    val info: ConnectionInfo? = null,
    /** The complete result once the run has finished. */
    val result: Measurement? = null,
    val networkType: NetworkType = NetworkType.OTHER,
    val unit: SpeedUnit = SpeedUnit.MBIT_PER_SECOND,
    val testSize: TestSize = TestSize.NORMAL,
    val keepScreenOn: Boolean = true,
    val haptics: Boolean = true,
    val mode: TestMode = TestMode.QUICK,
    val error: SpeedTestError? = null,
) {
    val isRunning: Boolean
        get() = phase == TestPhase.PING || phase == TestPhase.DOWNLOAD || phase == TestPhase.UPLOAD
    val hasResults: Boolean get() = pingMs != null
}

/** The part of the state that belongs to one run; settings and network are merged in on top. */
private data class RunState(
    val phase: TestPhase = TestPhase.IDLE,
    val progress: Float = 0f,
    val liveMbps: Double = 0.0,
    val livePingMs: Double? = null,
    val samples: List<TransferSample> = emptyList(),
    val stepIndex: Int = -1,
    val stepRound: Int = 0,
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val downloadSeries: List<Float> = emptyList(),
    val uploadSeries: List<Float> = emptyList(),
    val loadedDown: List<Double> = emptyList(),
    val loadedUp: List<Double> = emptyList(),
    val info: ConnectionInfo? = null,
    val result: Measurement? = null,
    val error: SpeedTestError? = null,
)

class TestViewModel(
    private val engine: SpeedTestEngine,
    private val history: HistoryRepository,
    private val settings: SettingsRepository,
    private val network: NetworkMonitor,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val run = MutableStateFlow(RunState())
    private var job: Job? = null
    private val selectedMode = MutableStateFlow<TestMode?>(null)

    fun selectMode(mode: TestMode) {
        if (!uiState.value.isRunning) selectedMode.value = mode
    }

    val uiState: StateFlow<TestUiState> = combine(run, settings.settings, network.networkType, selectedMode) { run, settings, net, mode ->
        run.toUiState(settings, net).copy(mode = mode ?: settings.defaultMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TestUiState())

    fun start() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            if (network.currentType() == NetworkType.OFFLINE) {
                run.value = RunState(error = SpeedTestError.Offline)
                return@launch
            }
            val preferences = settings.settings.first()
            val mode = selectedMode.value ?: preferences.defaultMode
            selectedMode.value = mode
            val plan = TestPlan.forMode(mode, preferences.testSize)
            run.value = RunState(phase = TestPhase.PING)
            try {
                engine.run(plan).collect(::onEvent)
            } catch (e: CancellationException) {
                throw e
            } catch (e: SpeedTestException) {
                run.value = RunState(error = e.error.refine())
            } catch (e: Exception) {
                run.value = RunState(error = SpeedTestError.Unknown(e.message))
            }
        }
    }

    /** Aborts a running test; all network calls are cancelled through coroutine cancellation. */
    fun stop() {
        job?.cancel()
        job = null
        run.value = RunState()
    }

    /** Back to the start screen after a finished run. */
    fun reset() {
        if (!uiState.value.isRunning) run.value = RunState()
    }

    fun dismissError() = run.update { it.copy(error = null) }

    private suspend fun onEvent(event: SpeedTestEvent) {
        when (event) {
            is SpeedTestEvent.StepStarted -> run.update { it.copy(stepIndex = event.index, stepRound = event.round) }
            is SpeedTestEvent.SampleCompleted -> run.update {
                it.copy(samples = it.samples + event.sample, progress = 0.1f + 0.9f * event.completed / event.total)
            }

            is SpeedTestEvent.PhaseStarted -> run.update {
                it.copy(
                    phase = event.phase, liveMbps = 0.0,
                    progress = it.progress.coerceAtLeast(if (it.stepIndex >= 0) it.progress else event.phase.start()),
                )
            }

            is SpeedTestEvent.LatencySample -> run.update {
                it.copy(livePingMs = event.ms, progress = TestPhase.PING.span(event.index.toFloat() / event.total))
            }

            is SpeedTestEvent.LatencyResult -> run.update {
                it.copy(pingMs = event.pingMs, jitterMs = event.jitterMs, livePingMs = event.pingMs)
            }

            is SpeedTestEvent.Throughput -> run.update {
                val value = event.mbps.toFloat()
                it.copy(
                    liveMbps = event.mbps,
                    progress = (if (it.stepIndex >= 0) 0.1f + 0.9f * event.progress else event.phase.span(event.progress)).coerceAtLeast(it.progress),
                    downloadSeries = if (event.phase == TestPhase.DOWNLOAD) it.downloadSeries.append(value) else it.downloadSeries,
                    uploadSeries = if (event.phase == TestPhase.UPLOAD) it.uploadSeries.append(value) else it.uploadSeries,
                )
            }

            is SpeedTestEvent.PhaseResult -> run.update {
                when (event.phase) {
                    TestPhase.DOWNLOAD -> it.copy(downloadMbps = event.mbps)
                    TestPhase.UPLOAD -> it.copy(uploadMbps = event.mbps)
                    else -> it
                }
            }

            is SpeedTestEvent.Info -> run.update { it.copy(info = event.info) }

            is SpeedTestEvent.LoadedLatency -> run.update {
                when (event.phase) {
                    TestPhase.DOWNLOAD -> it.copy(loadedDown = it.loadedDown + event.ms)
                    TestPhase.UPLOAD -> it.copy(loadedUp = it.loadedUp + event.ms)
                    else -> it
                }
            }

            is SpeedTestEvent.Finished -> {
                val m = event.measurement.let { m -> if (m.info == null) m.copy(info = run.value.info) else m }
                run.update {
                    it.copy(
                        phase = TestPhase.DONE, progress = 1f, liveMbps = m.downloadMbps,
                        pingMs = m.pingMs, jitterMs = m.jitterMs,
                        downloadMbps = m.downloadMbps, uploadMbps = m.uploadMbps,
                        info = m.info, result = m,
                    )
                }
                if (settings.settings.first().saveHistory) history.save(network.currentType(), m, clock())
            }
        }
    }

    /** "Server not reachable" while the device is offline is really an offline problem. */
    private fun SpeedTestError.refine(): SpeedTestError =
        if (this is SpeedTestError.Unreachable && network.currentType() == NetworkType.OFFLINE) SpeedTestError.Offline else this

    private fun RunState.toUiState(settings: AppSettings, net: NetworkType) = TestUiState(
        samples = samples, stepIndex = stepIndex, stepRound = stepRound,
        phase = phase, progress = progress, liveMbps = liveMbps, livePingMs = livePingMs,
        pingMs = pingMs, jitterMs = jitterMs, downloadMbps = downloadMbps, uploadMbps = uploadMbps,
        downloadSeries = downloadSeries, uploadSeries = uploadSeries,
        loadedDownMs = result?.downloadLoadedMs ?: loadedDown.medianOrNull(),
        loadedUpMs = result?.uploadLoadedMs ?: loadedUp.medianOrNull(),
        info = info, result = result,
        keepScreenOn = settings.keepScreenOn, haptics = settings.haptics, networkType = net,
        unit = settings.unit, testSize = settings.testSize, error = error,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_SERIES = 240

        fun List<Double>.medianOrNull() = if (isEmpty()) null else Statistics.median(this)

        /** Keeps the sparkline bounded on long Full runs by halving its resolution when full. */
        fun List<Float>.append(value: Float): List<Float> {
            val next = this + value
            return if (next.size <= MAX_SERIES) next else next.filterIndexed { index, _ -> index % 2 == 0 }
        }

        // The overall progress bar: ping is a short first slice, then download and upload.
        fun TestPhase.start() = when (this) {
            TestPhase.PING -> 0f
            TestPhase.DOWNLOAD -> 0.1f
            TestPhase.UPLOAD -> 0.55f
            else -> 0f
        }

        fun TestPhase.span(fraction: Float): Float {
            val (from, to) = when (this) {
                TestPhase.PING -> 0f to 0.1f
                TestPhase.DOWNLOAD -> 0.1f to 0.55f
                TestPhase.UPLOAD -> 0.55f to 1f
                else -> 0f to 0f
            }
            return from + (to - from) * fraction.coerceIn(0f, 1f)
        }
    }
}
