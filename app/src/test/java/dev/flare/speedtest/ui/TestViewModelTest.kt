package dev.flare.speedtest.ui

import dev.flare.speedtest.domain.AppSettings
import dev.flare.speedtest.domain.HistoryRepository
import dev.flare.speedtest.domain.Measurement
import dev.flare.speedtest.domain.NetworkMonitor
import dev.flare.speedtest.domain.NetworkType
import dev.flare.speedtest.domain.SettingsRepository
import dev.flare.speedtest.domain.SpeedTestEngine
import dev.flare.speedtest.domain.SpeedTestError
import dev.flare.speedtest.domain.SpeedTestEvent
import dev.flare.speedtest.domain.SpeedTestException
import dev.flare.speedtest.domain.SpeedTestRecord
import dev.flare.speedtest.domain.SpeedUnit
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.TestPlan
import dev.flare.speedtest.domain.TestSize
import dev.flare.speedtest.domain.ThemeMode
import dev.flare.speedtest.ui.test.TestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TestViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val measurement = Measurement(pingMs = 12.0, jitterMs = 1.5, downloadMbps = 250.0, uploadMbps = 40.0)

    private class FakeHistory : HistoryRepository {
        val saved = mutableListOf<Pair<NetworkType, Measurement>>()
        override fun observeAll(): Flow<List<SpeedTestRecord>> = emptyFlow()
        override suspend fun save(networkType: NetworkType, measurement: Measurement, timestampMillis: Long) {
            saved += networkType to measurement
        }
        override suspend fun delete(id: Long) = Unit
        override suspend fun restore(record: SpeedTestRecord) = Unit
        override suspend fun clear() = Unit
    }

    private class FakeSettings(initial: AppSettings = AppSettings()) : SettingsRepository {
        val state = MutableStateFlow(initial)
        override val settings: Flow<AppSettings> = state
        override suspend fun setUnit(unit: SpeedUnit) = state.update { it.copy(unit = unit) }
        override suspend fun setTestSize(size: TestSize) = state.update { it.copy(testSize = size) }
        override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
        override suspend fun setKeepScreenOn(enabled: Boolean) = Unit
        override suspend fun setSaveHistory(enabled: Boolean) = Unit
        override suspend fun setDefaultMode(mode: dev.flare.speedtest.domain.TestMode) = Unit
        override suspend fun setHaptics(enabled: Boolean) = Unit
        override suspend fun setDynamicColor(enabled: Boolean) = state.update { it.copy(dynamicColor = enabled) }
        private fun MutableStateFlow<AppSettings>.update(f: (AppSettings) -> AppSettings) { value = f(value) }
    }

    private class FakeNetwork(var type: NetworkType) : NetworkMonitor {
        override val networkType: Flow<NetworkType> = flow { emit(type) }
        override fun currentType() = type
    }

    private fun TestScope.viewModel(
        engine: SpeedTestEngine,
        history: FakeHistory = FakeHistory(),
        network: FakeNetwork = FakeNetwork(NetworkType.WIFI),
        settings: FakeSettings = FakeSettings(),
    ) = TestViewModel(engine, history, settings, network, clock = { 42L })

    @Test
    fun `default quick mode reaches engine and explicit full selection overrides it`() = runTest(dispatcher) {
        var plan: TestPlan? = null
        val settings = FakeSettings(AppSettings(defaultMode = dev.flare.speedtest.domain.TestMode.QUICK))
        val vm = viewModel({ p -> plan = p; flow { emit(SpeedTestEvent.Finished(measurement)) } }, settings = settings)
        val job = launchCollect(vm)
        vm.start()
        advanceUntilIdle()
        assertEquals(TestPlan.forMode(dev.flare.speedtest.domain.TestMode.QUICK, TestSize.NORMAL), plan)
        vm.selectMode(dev.flare.speedtest.domain.TestMode.FULL)
        vm.start()
        advanceUntilIdle()
        assertEquals(TestPlan.full(), plan)
        job.cancel()
    }

    @Test
    fun `disabled history keeps results visible without storing them`() = runTest(dispatcher) {
        val history = FakeHistory()
        val vm = viewModel({ flow { emit(SpeedTestEvent.Finished(measurement)) } }, history,
            settings = FakeSettings(AppSettings(saveHistory = false)))
        val job = launchCollect(vm)
        vm.start()
        advanceUntilIdle()
        assertEquals(TestPhase.DONE, vm.uiState.value.phase)
        assertTrue(history.saved.isEmpty())
        job.cancel()
    }

    @Test
    fun `successful run exposes results and stores them with the network type`() = runTest(dispatcher) {
        val history = FakeHistory()
        val engine = SpeedTestEngine {
            flow {
                emit(SpeedTestEvent.PhaseStarted(TestPhase.PING))
                emit(SpeedTestEvent.LatencyResult(12.0, 1.5))
                emit(SpeedTestEvent.PhaseStarted(TestPhase.DOWNLOAD))
                emit(SpeedTestEvent.PhaseResult(TestPhase.DOWNLOAD, 250.0))
                emit(SpeedTestEvent.PhaseStarted(TestPhase.UPLOAD))
                emit(SpeedTestEvent.PhaseResult(TestPhase.UPLOAD, 40.0))
                emit(SpeedTestEvent.Finished(measurement))
            }
        }
        val vm = viewModel(engine, history)
        val job = launchCollect(vm)
        vm.start()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TestPhase.DONE, state.phase)
        assertEquals(250.0, state.downloadMbps!!, 0.0)
        assertEquals(40.0, state.uploadMbps!!, 0.0)
        assertEquals(1f, state.progress, 0f)
        assertEquals(listOf(NetworkType.WIFI to measurement), history.saved)
        job.cancel()
    }

    @Test
    fun `starting while offline fails fast without touching the engine`() = runTest(dispatcher) {
        var called = false
        val vm = viewModel({ called = true; emptyFlow() }, network = FakeNetwork(NetworkType.OFFLINE))
        val job = launchCollect(vm)
        vm.start()
        advanceUntilIdle()
        assertEquals(SpeedTestError.Offline, vm.uiState.value.error)
        assertEquals(TestPhase.IDLE, vm.uiState.value.phase)
        assertTrue(!called)
        job.cancel()
    }

    @Test
    fun `engine failure surfaces as error and returns to idle`() = runTest(dispatcher) {
        val vm = viewModel({ flow { throw SpeedTestException(SpeedTestError.Timeout) } })
        val job = launchCollect(vm)
        vm.start()
        advanceUntilIdle()
        assertEquals(SpeedTestError.Timeout, vm.uiState.value.error)
        assertEquals(TestPhase.IDLE, vm.uiState.value.phase)
        vm.dismissError()
        runCurrent()
        assertNull(vm.uiState.value.error)
        job.cancel()
    }

    @Test
    fun `server unreachable while the device has lost its network is reported as offline`() = runTest(dispatcher) {
        val network = FakeNetwork(NetworkType.WIFI)
        val vm = viewModel(
            { flow { network.type = NetworkType.OFFLINE; throw SpeedTestException(SpeedTestError.Unreachable("x")) } },
            network = network,
        )
        val job = launchCollect(vm)
        vm.start()
        advanceUntilIdle()
        assertEquals(SpeedTestError.Offline, vm.uiState.value.error)
        job.cancel()
    }

    @Test
    fun `stop cancels the running engine and resets the state`() = runTest(dispatcher) {
        var cancelled = false
        val engine = SpeedTestEngine {
            flow {
                emit(SpeedTestEvent.PhaseStarted(TestPhase.PING))
                try { awaitCancellation() } finally { cancelled = true }
            }
        }
        val history = FakeHistory()
        val vm = viewModel(engine, history)
        val job = launchCollect(vm)
        vm.start()
        runCurrent()
        assertEquals(TestPhase.PING, vm.uiState.value.phase)
        assertTrue(vm.uiState.value.isRunning)

        vm.stop()
        runCurrent()
        assertTrue(cancelled)
        assertEquals(TestPhase.IDLE, vm.uiState.value.phase)
        assertTrue(history.saved.isEmpty())
        job.cancel()
    }

    @Test
    fun `settings feed the state and the test plan size`() = runTest(dispatcher) {
        var plan: TestPlan? = null
        val settings = FakeSettings(AppSettings(unit = SpeedUnit.MBYTE_PER_SECOND, testSize = TestSize.LARGE))
        val vm = viewModel({ p -> plan = p; flow { emit(SpeedTestEvent.Finished(measurement)) } }, settings = settings)
        val job = launchCollect(vm)
        vm.start()
        advanceUntilIdle()
        assertEquals(SpeedUnit.MBYTE_PER_SECOND, vm.uiState.value.unit)
        assertEquals(TestPlan.forMode(dev.flare.speedtest.domain.TestMode.QUICK, TestSize.LARGE), plan)
        job.cancel()
    }

    private fun TestScope.launchCollect(vm: TestViewModel) =
        backgroundScope.launchIn { vm.uiState.collect { } }

    private fun kotlinx.coroutines.CoroutineScope.launchIn(block: suspend () -> Unit) =
        this.launch { block() }
}
