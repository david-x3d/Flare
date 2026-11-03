package dev.flare.speedtest.data.network

import dev.flare.speedtest.domain.LiveSpeedMeter
import dev.flare.speedtest.domain.Measurement
import dev.flare.speedtest.domain.SpeedTestEngine
import dev.flare.speedtest.domain.SpeedTestError
import dev.flare.speedtest.domain.SpeedTestEvent
import dev.flare.speedtest.domain.SpeedTestException
import dev.flare.speedtest.domain.Statistics
import dev.flare.speedtest.domain.TestPhase
import dev.flare.speedtest.domain.TestPlan
import dev.flare.speedtest.domain.TransferSample
import dev.flare.speedtest.domain.TransferStep
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink

/**
 * Speed test against the Cloudflare speed endpoints.
 *
 * - **Latency**: one warm-up request (opens the TLS connection), then `latencySamples` times
 *   `GET /__down?bytes=0`, timing the wait for the response headers. Ping is the median, jitter the
 *   standard deviation of those samples.
 * - **Download / upload**: rounds of N parallel HTTP/1.1 connections (really parallel TCP streams,
 *   not one multiplexed HTTP/2 connection) with growing payload sizes. A round's speed is the bytes
 *   moved divided by the time the round took; the result is the 90th percentile over all rounds
 *   that lasted long enough to be meaningful. Bigger steps are skipped when a slow link would make
 *   them take too long, and every phase has a hard time budget.
 * - **Live speed**: all connections feed one byte counter that a sampler turns into a smoothed reading.
 */
class CloudflareSpeedTestEngine(
    private val client: OkHttpClient = createHttpClient(),
    baseUrl: String = CloudflareEndpoints.BASE_URL,
) : SpeedTestEngine {

    private val base: HttpUrl = baseUrl.toHttpUrl()

    override fun run(plan: TestPlan): Flow<SpeedTestEvent> = channelFlow {
        try {
            send(SpeedTestEvent.PhaseStarted(TestPhase.PING))
            val latency = measureLatency(plan)
            send(SpeedTestEvent.LatencyResult(latency.pingMs, latency.jitterMs))

            val payload = Random.nextBytes(UPLOAD_PAYLOAD_BYTES)
            if (plan.detailedSteps.isNotEmpty()) {
                runDetailed(plan, latency, payload)
                return@channelFlow
            }
            val download = runTransferPhase(TestPhase.DOWNLOAD, plan.downloadSteps, plan.downloadConnections, plan) { bytes, connections, counter ->
                downloadRound(bytes, connections, counter)
            }
            val upload = runTransferPhase(TestPhase.UPLOAD, plan.uploadSteps, plan.uploadConnections, plan) { bytes, connections, counter ->
                uploadRound(bytes, connections, counter, payload)
            }
            send(SpeedTestEvent.Finished(Measurement(latency.pingMs, latency.jitterMs, download, upload)))
        } catch (t: Throwable) {
            throw t.toSpeedTestException(base.host)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun ProducerScope<SpeedTestEvent>.runDetailed(plan: TestPlan, latency: Latency, payload: ByteArray) = coroutineScope {
        val samples = mutableListOf<TransferSample>()
        val total = plan.detailedSteps.sumOf { it.rounds }
        val deadline = System.nanoTime() + plan.phaseBudgetMillis * NANOS_PER_MILLI
        plan.detailedSteps.forEachIndexed { index, step ->
            send(SpeedTestEvent.PhaseStarted(step.phase))
            repeat(step.rounds) { round ->
                send(SpeedTestEvent.StepStarted(index, round + 1))
                val started = System.currentTimeMillis()
                val counter = AtomicLong()
                val loaded = java.util.Collections.synchronizedList(mutableListOf<Double>())
                val probes = launch {
                    while (isActive) {
                        try { loaded += probeLatency() } catch (_: IOException) { }
                        delay(200)
                    }
                }
                val sampler = launch {
                    val meter = LiveSpeedMeter()
                    while (isActive) {
                        delay(SAMPLE_INTERVAL_MILLIS)
                        trySend(SpeedTestEvent.Throughput(step.phase, meter.update(System.nanoTime(), counter.get()), samples.size.toFloat() / total))
                    }
                }
                val remainingMs = ((deadline - System.nanoTime()) / NANOS_PER_MILLI).coerceAtLeast(0)
                val sample = try {
                    if (remainingMs == 0L) null else withTimeoutOrNull(minOf(remainingMs, 20_000L)) {
                        detailedTransfer(index, round + 1, step.phase, step.bytes, started, counter, payload)
                    }
                } catch (_: IOException) {
                    TransferSample(index, round + 1, step.phase, step.bytes, started, status = "failed")
                } finally {
                    probes.cancelAndJoin()
                    sampler.cancelAndJoin()
                }
                val result = (sample ?: TransferSample(index, round + 1, step.phase, step.bytes, started,
                    status = if (remainingMs == 0L) "skipped" else "timeout")).copy(loadedLatencies = loaded.toList())
                samples += result
                send(SpeedTestEvent.SampleCompleted(result, samples.size, total))
                val rates = samples.filter { it.phase == step.phase }.mapNotNull { it.mbps }
                if (rates.isNotEmpty()) send(SpeedTestEvent.PhaseResult(step.phase, Statistics.robustThroughput(rates)))
            }
        }
        fun speed(phase: TestPhase): Double {
            val rates = samples.filter { it.phase == phase }.mapNotNull { it.mbps }
            if (rates.isEmpty()) throw SpeedTestException(SpeedTestError.Timeout)
            return Statistics.robustThroughput(rates)
        }
        send(SpeedTestEvent.Finished(Measurement(latency.pingMs, latency.jitterMs, speed(TestPhase.DOWNLOAD), speed(TestPhase.UPLOAD), samples)))
    }

    private suspend fun detailedTransfer(index: Int, round: Int, phase: TestPhase, bytes: Long, timestamp: Long, counter: AtomicLong, payload: ByteArray): TransferSample {
        val body = if (phase == TestPhase.UPLOAD) RandomBody(bytes, payload, counter) else null
        val req = if (body == null) request(downloadUrl(bytes)) else Request.Builder().url(uploadUrl()).header("User-Agent", USER_AGENT).post(body).build()
        return client.executeCancellable(req) { response, startNanos ->
            val headersNanos = System.nanoTime()
            checkSuccess(response)
            val source = response.body.source()
            val buffer = ByteArray(READ_BUFFER_BYTES)
            var responseBytes = 0L
            while (true) {
                val read = source.read(buffer)
                if (read == -1) break
                responseBytes += read
                if (body == null) counter.addAndGet(read.toLong())
            }
            val endNanos = System.nanoTime()
            if (body == null && responseBytes != bytes) throw SpeedTestException(SpeedTestError.EndpointChanged(req.url.toString(), response.code, "expected $bytes bytes, received $responseBytes"))
            val durationMs = (endNanos - startNanos) / 1e6
            val headerMs = (headersNanos - startNanos) / 1e6
            val serverMs = Regex("(?:^|[, ])cfRequestDuration;dur=([0-9.]+)").find(response.header("server-timing").orEmpty())?.groupValues?.get(1)?.toDoubleOrNull()
            TransferSample(index, round, phase, bytes, timestamp,
                mbps = Statistics.megabitsPerSecondNanos(bytes, endNanos - startNanos),
                durationMs = durationMs, latencyMs = if (body == null) headerMs else null,
                serverTimeMs = serverMs, responseBytes = responseBytes)
        }
    }

    // ---------------------------------------------------------------- latency

    private class Latency(val pingMs: Double, val jitterMs: Double)

    private suspend fun ProducerScope<SpeedTestEvent>.measureLatency(plan: TestPlan): Latency {
        retrying { probeLatency() } // warm-up: pays for DNS + TCP + TLS so the samples reflect the round trip only
        val samples = ArrayList<Double>(plan.latencySamples)
        for (i in 1..plan.latencySamples) {
            val ms = retrying { probeLatency() }
            samples += ms
            send(SpeedTestEvent.LatencySample(ms, i, plan.latencySamples))
        }
        return Latency(Statistics.median(samples), Statistics.standardDeviation(samples))
    }

    /** Milliseconds until the response headers of a zero-byte download arrive. */
    private suspend fun probeLatency(): Double =
        client.executeCancellable(request(downloadUrl(0)), callTimeoutMillis = LATENCY_TIMEOUT_MILLIS) { response, startNanos ->
            val elapsedNanos = System.nanoTime() - startNanos
            checkSuccess(response)
            elapsedNanos / 1e6
        }

    // ---------------------------------------------------------------- transfers

    private class Transfer(val bytes: Long, val startNanos: Long, val endNanos: Long)

    private class RoundOutcome(val bytes: Long, val nanos: Long)

    private fun interface RoundRunner {
        suspend fun run(bytes: Long, connections: Int, counter: AtomicLong): RoundOutcome
    }

    private suspend fun ProducerScope<SpeedTestEvent>.runTransferPhase(
        phase: TestPhase,
        steps: List<TransferStep>,
        connections: Int,
        plan: TestPlan,
        runner: RoundRunner,
    ): Double = coroutineScope {
        send(SpeedTestEvent.PhaseStarted(phase))
        val counter = AtomicLong()
        val totalRounds = steps.sumOf { it.rounds }.coerceAtLeast(1)
        var completedRounds = 0
        var progress = 0f
        val startNanos = System.nanoTime()
        val deadlineNanos = startNanos + plan.phaseBudgetMillis * NANOS_PER_MILLI

        val sampler = launch {
            val meter = LiveSpeedMeter()
            while (isActive) {
                delay(SAMPLE_INTERVAL_MILLIS)
                val mbps = meter.update(System.nanoTime(), counter.get())
                // Live values are lossy by design: drop one rather than stall the transfers.
                trySend(SpeedTestEvent.Throughput(phase, mbps, progress))
            }
        }

        val roundMbps = ArrayList<Double>()
        val validMbps = ArrayList<Double>()
        var failedRounds = 0
        var lastFailure: IOException? = null
        try {
            var previousBytes = 0L
            var previousNanos = 0L
            steps@ for (step in steps) {
                if (previousNanos > 0 && previousBytes > 0) {
                    // Extrapolate from the last round; a slow link must not start a 25 MB step.
                    val estimatedNanos = (previousNanos.toDouble() / previousBytes) * step.bytes
                    if (estimatedNanos > plan.maxRoundMillis * NANOS_PER_MILLI) break@steps
                }
                for (round in 1..step.rounds) {
                    val remaining = deadlineNanos - System.nanoTime()
                    if (remaining <= 0) break@steps
                    val outcome = try {
                        withTimeoutOrNull(remaining.nanoseconds) {
                            runner.run(step.bytes, connections, counter)
                        } ?: break@steps // budget ran out mid-round; keep what we have
                    } catch (e: IOException) {
                        // Mobile links drop or corrupt single TLS streams (e.g. BAD_RECORD_MAC).
                        // Skip the round on fresh connections instead of failing the whole test.
                        if (++failedRounds > MAX_FAILED_ROUNDS) throw e
                        lastFailure = e
                        client.connectionPool.evictAll()
                        continue
                    }

                    completedRounds++
                    progress = (completedRounds.toFloat() / totalRounds).coerceIn(progress, 1f)
                    previousBytes = step.bytes
                    previousNanos = outcome.nanos
                    if (outcome.nanos > 0 && outcome.bytes > 0) {
                        val mbps = Statistics.megabitsPerSecondNanos(outcome.bytes, outcome.nanos)
                        roundMbps += mbps
                        if (outcome.nanos >= plan.minRoundMillis * NANOS_PER_MILLI) validMbps += mbps
                    }
                }
            }
        } finally {
            sampler.cancelAndJoin()
        }

        // Every round broke: that is a network problem, not a measurement.
        if (roundMbps.isEmpty() && lastFailure != null) throw lastFailure

        val result = when {
            validMbps.isNotEmpty() -> Statistics.robustThroughput(validMbps)
            roundMbps.isNotEmpty() -> roundMbps.max()
            // Not a single round finished inside the budget: average over the whole phase.
            else -> Statistics.megabitsPerSecondNanos(counter.get(), System.nanoTime() - startNanos)
        }
        send(SpeedTestEvent.Throughput(phase, result, 1f))
        send(SpeedTestEvent.PhaseResult(phase, result))
        result
    }

    private fun combine(transfers: List<Transfer>): RoundOutcome = RoundOutcome(
        bytes = transfers.sumOf { it.bytes },
        nanos = transfers.maxOf { it.endNanos } - transfers.minOf { it.startNanos },
    )

    private suspend fun downloadRound(bytes: Long, connections: Int, counter: AtomicLong): RoundOutcome =
        coroutineScope {
            combine((1..connections).map { async { downloadOne(bytes, counter) } }.awaitAll())
        }

    /** Streams one payload; timing starts at the first byte so latency does not pollute the rate. */
    private suspend fun downloadOne(bytes: Long, counter: AtomicLong): Transfer {
        val url = downloadUrl(bytes)
        return client.executeCancellable(request(url)) { response, _ ->
            checkSuccess(response)
            val source = response.body.source()
            val buffer = ByteArray(READ_BUFFER_BYTES)
            var total = 0L
            var firstChunk = 0L
            var startNanos = 0L
            while (true) {
                val read = source.read(buffer)
                if (read == -1) break
                if (total == 0L) {
                    startNanos = System.nanoTime()
                    firstChunk = read.toLong()
                }
                total += read
                counter.addAndGet(read.toLong())
            }
            val endNanos = System.nanoTime()
            if (total != bytes) {
                throw SpeedTestException(
                    SpeedTestError.EndpointChanged(url.toString(), response.code, "expected $bytes bytes, received $total"),
                )
            }
            // The first chunk arrived before the clock started, so it is not part of the timed window.
            Transfer(total - firstChunk, startNanos, endNanos)
        }
    }

    private suspend fun uploadRound(bytes: Long, connections: Int, counter: AtomicLong, payload: ByteArray): RoundOutcome =
        coroutineScope {
            combine((1..connections).map { async { uploadOne(bytes, counter, payload) } }.awaitAll())
        }

    /** Posts [bytes] of random data; done when the server has acknowledged the whole body. */
    private suspend fun uploadOne(bytes: Long, counter: AtomicLong, payload: ByteArray): Transfer {
        val body = RandomBody(bytes, payload, counter)
        val request = Request.Builder().url(uploadUrl()).header("User-Agent", USER_AGENT).post(body).build()
        return client.executeCancellable(request) { response, _ ->
            val endNanos = System.nanoTime()
            checkSuccess(response)
            Transfer(bytes, body.startNanos, endNanos)
        }
    }

    /** Writes pre-generated random bytes (incompressible, cheap to produce) and counts what it sends. */
    private class RandomBody(
        private val size: Long,
        private val payload: ByteArray,
        private val counter: AtomicLong,
    ) : RequestBody() {
        @Volatile
        var startNanos = 0L
            private set

        override fun contentType() = "text/plain".toMediaType()
        override fun contentLength() = size
        override fun isOneShot() = true

        override fun writeTo(sink: BufferedSink) {
            startNanos = System.nanoTime()
            var remaining = size
            var offset = Random.nextInt(payload.size)
            while (remaining > 0) {
                val chunk = minOf(remaining, (payload.size - offset).toLong(), WRITE_CHUNK_BYTES.toLong()).toInt()
                sink.write(payload, offset, chunk)
                counter.addAndGet(chunk.toLong())
                remaining -= chunk
                offset = (offset + chunk) % payload.size
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Runs [block] again on fresh connections when a transient I/O error interrupts it. */
    private suspend fun <T> retrying(block: suspend () -> T): T {
        repeat(MAX_RETRIES) {
            try {
                return block()
            } catch (_: IOException) {
                client.connectionPool.evictAll()
            }
        }
        return block()
    }

    private fun downloadUrl(bytes: Long): HttpUrl = base.newBuilder()
        .addPathSegment(CloudflareEndpoints.DOWNLOAD_PATH)
        .addQueryParameter(CloudflareEndpoints.DOWNLOAD_QUERY_BYTES, bytes.toString())
        .build()

    private fun uploadUrl(): HttpUrl = base.newBuilder().addPathSegment(CloudflareEndpoints.UPLOAD_PATH).build()

    private fun request(url: HttpUrl): Request = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        // No transparent gzip: payload sizes must match the bytes that crossed the wire.
        .header("Accept-Encoding", "identity")
        .build()

    private fun checkSuccess(response: Response) {
        if (!response.isSuccessful) {
            throw SpeedTestException(SpeedTestError.EndpointChanged(response.request.url.toString(), response.code, null))
        }
    }

    companion object {
        private const val USER_AGENT = "Flare/1.0 (Android speed test)"
        private const val LATENCY_TIMEOUT_MILLIS = 5_000L
        private const val SAMPLE_INTERVAL_MILLIS = 100L
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val READ_BUFFER_BYTES = 64 * 1024
        private const val WRITE_CHUNK_BYTES = 64 * 1024
        private const val UPLOAD_PAYLOAD_BYTES = 1024 * 1024
        private const val MAX_RETRIES = 2
        private const val MAX_FAILED_ROUNDS = 3

        /** HTTP/1.1 on purpose: every request gets its own TCP connection, like a classic speed test. */
        fun createHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1))
            .connectionPool(ConnectionPool(8, 30, TimeUnit.SECONDS))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
