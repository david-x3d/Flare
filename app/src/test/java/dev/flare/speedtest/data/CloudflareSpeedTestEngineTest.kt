package dev.flare.speedtest.data

import dev.flare.speedtest.data.network.CloudflareSpeedTestEngine
import dev.flare.speedtest.domain.SpeedTestError
import dev.flare.speedtest.domain.SpeedTestEvent
import dev.flare.speedtest.domain.SpeedTestException
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.TestPlan
import dev.flare.speedtest.domain.TransferStep
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CloudflareSpeedTestEngineTest {

    private lateinit var server: MockWebServer
    private val uploadedBytes = AtomicInteger()
    private val downloadRequests = AtomicInteger()

    /** Small plan so the suite stays fast; the logic under test is identical to the real plan. */
    private val plan = TestPlan(
        latencySamples = 5,
        downloadSteps = listOf(TransferStep(50_000, 1), TransferStep(500_000, 2)),
        uploadSteps = listOf(TransferStep(50_000, 1), TransferStep(500_000, 2)),
        downloadConnections = 3,
        uploadConnections = 2,
        phaseBudgetMillis = 10_000,
        minRoundMillis = 0,
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.url.encodedPath
                return when {
                    path == "/__down" -> {
                        downloadRequests.incrementAndGet()
                        val bytes = request.url.queryParameter("bytes")!!.toInt()
                        MockResponse.Builder().body(Buffer().write(ByteArray(bytes))).build()
                    }

                    path == "/__up" && request.method == "POST" -> {
                        uploadedBytes.addAndGet(request.bodySize.toInt())
                        MockResponse.Builder().code(200).build()
                    }

                    else -> MockResponse.Builder().code(404).build()
                }
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun engine() = CloudflareSpeedTestEngine(baseUrl = server.url("/").toString())

    @Test
    fun `full run emits phases in order and a finished measurement`() = runBlocking {
        val events = withTimeout(30_000) { engine().run(plan).toList() }

        val phases = events.filterIsInstance<SpeedTestEvent.PhaseStarted>().map { it.phase }
        assertEquals(listOf(TestPhase.PING, TestPhase.DOWNLOAD, TestPhase.UPLOAD), phases)
        assertEquals(plan.latencySamples, events.filterIsInstance<SpeedTestEvent.LatencySample>().size)

        val finished = events.last() as SpeedTestEvent.Finished
        assertTrue(finished.measurement.pingMs > 0.0)
        assertTrue(finished.measurement.jitterMs >= 0.0)
        assertTrue(finished.measurement.downloadMbps > 0.0)
        assertTrue(finished.measurement.uploadMbps > 0.0)

        val results = events.filterIsInstance<SpeedTestEvent.PhaseResult>()
        assertEquals(listOf(TestPhase.DOWNLOAD, TestPhase.UPLOAD), results.map { it.phase })
    }

    @Test
    fun `latency probes use zero byte downloads and send a warm-up first`() = runBlocking {
        withTimeout(30_000) { engine().run(plan).toList() }
        val first = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("0", first.url.queryParameter("bytes"))
        // warm-up + latency samples all precede the first real payload
        val probes = (1 until plan.latencySamples + 1).map { server.takeRequest(1, TimeUnit.SECONDS)!!.url.queryParameter("bytes") }
        assertTrue(probes.all { it == "0" })
    }

    @Test
    fun `upload actually sends the planned number of bytes`() = runBlocking {
        withTimeout(30_000) { engine().run(plan).toList() }
        val expected = plan.uploadConnections * plan.uploadSteps.sumOf { it.bytes * it.rounds }
        assertEquals(expected, uploadedBytes.get().toLong())
    }

    @Test
    fun `live throughput is reported while transferring`() = runBlocking {
        val slowPlan = plan.copy(downloadSteps = listOf(TransferStep(2_000_000, 2)), uploadSteps = listOf(TransferStep(50_000, 1)))
        val events = withTimeout(30_000) { engine().run(slowPlan).toList() }
        val live = events.filterIsInstance<SpeedTestEvent.Throughput>().filter { it.phase == TestPhase.DOWNLOAD }
        assertTrue(live.isNotEmpty())
        assertEquals(1f, live.last().progress, 0f)
    }

    @Test
    fun `unexpected status code becomes EndpointChanged`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse.Builder().code(404).build()
        }
        val error = collectError()
        assertTrue(error is SpeedTestError.EndpointChanged)
        assertEquals(404, (error as SpeedTestError.EndpointChanged).httpCode)
    }

    @Test
    fun `wrong payload size becomes EndpointChanged`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val asked = request.url.queryParameter("bytes")!!.toInt()
                return MockResponse.Builder().body(Buffer().write(ByteArray(if (asked == 0) 0 else asked / 2))).build()
            }
        }
        val error = collectError()
        assertTrue("was $error", error is SpeedTestError.EndpointChanged)
    }

    @Test
    fun `unreachable server becomes Unreachable`() = runBlocking {
        val url = server.url("/").toString()
        server.close()
        val error = collectError(CloudflareSpeedTestEngine(baseUrl = url))
        assertTrue("was $error", error is SpeedTestError.Unreachable)
    }

    @Test
    fun `cancelling the collector aborts in-flight transfers promptly`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val bytes = request.url.queryParameter("bytes")?.toInt() ?: 0
                return MockResponse.Builder()
                    .body(Buffer().write(ByteArray(bytes)))
                    .throttleBody(50_000, 1, TimeUnit.SECONDS) // would take ~40 s for 2 MB
                    .build()
            }
        }
        val slow = plan.copy(downloadSteps = listOf(TransferStep(2_000_000, 1)), phaseBudgetMillis = 60_000)
        val job = async(Dispatchers.Default) { engine().run(slow).toList() }
        delay(1_500)
        val started = System.nanoTime()
        job.cancel()
        runCatching { job.await() }
        val tookMillis = (System.nanoTime() - started) / 1_000_000
        assertTrue("cancellation took ${tookMillis}ms", tookMillis < 3_000)
    }

    @Test
    fun `phase budget ends a slow transfer with a result instead of hanging`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val bytes = request.url.queryParameter("bytes")?.toInt() ?: 0
                val builder = MockResponse.Builder().body(Buffer().write(ByteArray(bytes)))
                if (bytes > 0) builder.throttleBody(100_000, 1, TimeUnit.SECONDS)
                return builder.build()
            }
        }
        val tight = plan.copy(
            downloadSteps = listOf(TransferStep(3_000_000, 1)),
            uploadSteps = listOf(TransferStep(10_000, 1)),
            phaseBudgetMillis = 1_500,
        )
        val events = withTimeout(30_000) { engine().run(tight).toList() }
        val download = events.filterIsInstance<SpeedTestEvent.PhaseResult>().first { it.phase == TestPhase.DOWNLOAD }
        assertTrue(download.mbps > 0.0)
    }

    private suspend fun collectError(engine: CloudflareSpeedTestEngine = engine()): SpeedTestError {
        try {
            withTimeout(30_000) { engine.run(plan).toList() }
        } catch (e: SpeedTestException) {
            return e.error
        }
        fail("expected a SpeedTestException")
        throw AssertionError()
    }
}
