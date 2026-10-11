package dev.flare.speedtest.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.flare.speedtest.domain.AppSettings
import dev.flare.speedtest.domain.HistoryRepository
import dev.flare.speedtest.domain.NetworkMonitor
import dev.flare.speedtest.domain.NetworkType
import dev.flare.speedtest.domain.SettingsRepository
import dev.flare.speedtest.domain.SpeedTestEngine
import dev.flare.speedtest.domain.SpeedTestError
import dev.flare.speedtest.domain.SpeedTestEvent
import dev.flare.speedtest.domain.SpeedTestException
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.TestPlan
import dev.flare.speedtest.domain.TestSize
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
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val networkType: NetworkType = NetworkType.OTHER,
    val unit: SpeedUnit = SpeedUnit.MBIT_PER_SECOND,
    val testSize: TestSize = TestSize.NORMAL,
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
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
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

    val uiState: StateFlow<TestUiState> = combine(run, settings.settings, network.networkType) { run, settings, net ->
        run.toUiState(settings, net)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TestUiState())

    fun start() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            if (network.currentType() == NetworkType.OFFLINE) {
                run.value = RunState(error = SpeedTestError.Offline)
                return@launch
            }
            val plan = TestPlan.forSize(settings.settings.first().testSize)
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

    fun dismissError() = run.update { it.copy(error = null) }

    private suspend fun onEvent(event: SpeedTestEvent) {
        when (event) {
            is SpeedTestEvent.PhaseStarted -> run.update {
                it.copy(phase = event.phase, liveMbps = 0.0, progress = it.progress.coerceAtLeast(event.phase.start()))
            }

            is SpeedTestEvent.LatencySample -> run.update {
                it.copy(
                    livePingMs = event.ms,
                    progress = TestPhase.PING.span(event.index.toFloat() / event.total),
                )
            }

            is SpeedTestEvent.LatencyResult -> run.update {
                it.copy(pingMs = event.pingMs, jitterMs = event.jitterMs, livePingMs = event.pingMs)
            }

            is SpeedTestEvent.Throughput -> run.update {
                it.copy(liveMbps = event.mbps, progress = event.phase.span(event.progress).coerceAtLeast(it.progress))
            }

            is SpeedTestEvent.PhaseResult -> run.update {
                when (event.phase) {
                    TestPhase.DOWNLOAD -> it.copy(downloadMbps = event.mbps)
                    TestPhase.UPLOAD -> it.copy(uploadMbps = event.mbps)
                    else -> it
                }
            }

            is SpeedTestEvent.Finished -> {
                val m = event.measurement
                run.update {
                    it.copy(
                        phase = TestPhase.DONE, progress = 1f, liveMbps = m.downloadMbps,
                        pingMs = m.pingMs, jitterMs = m.jitterMs,
                        downloadMbps = m.downloadMbps, uploadMbps = m.uploadMbps,
                    )
                }
                history.save(network.currentType(), m, clock())
            }
        }
    }

    /** "Server not reachable" while the device is offline is really an offline problem. */
    private fun SpeedTestError.refine(): SpeedTestError =
        if (this is SpeedTestError.Unreachable && network.currentType() == NetworkType.OFFLINE) SpeedTestError.Offline else this

    private fun RunState.toUiState(settings: AppSettings, net: NetworkType) = TestUiState(
        phase = phase, progress = progress, liveMbps = liveMbps, livePingMs = livePingMs,
        pingMs = pingMs, jitterMs = jitterMs, downloadMbps = downloadMbps, uploadMbps = uploadMbps,
        networkType = net, unit = settings.unit, testSize = settings.testSize, error = error,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

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
