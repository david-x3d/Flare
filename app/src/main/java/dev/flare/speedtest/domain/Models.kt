package dev.flare.speedtest.domain

/** The transport the device is currently using, shown next to every result. */
enum class NetworkType { WIFI, CELLULAR, ETHERNET, VPN, OTHER, OFFLINE }

/** Display unit for throughput. Values are always stored as Mbit/s. */
enum class SpeedUnit(val factorFromMbps: Double) {
    MBIT_PER_SECOND(1.0),
    MBYTE_PER_SECOND(1.0 / 8.0);

    fun fromMbps(mbps: Double): Double = mbps * factorFromMbps
}

/** How much data a test may transfer. */
enum class TestSize { SMALL, NORMAL, LARGE }

enum class TestMode { QUICK, FULL }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val unit: SpeedUnit = SpeedUnit.MBIT_PER_SECOND,
    val testSize: TestSize = TestSize.NORMAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Off by default: Flare's own navy-and-flame palette is part of its look. */
    val dynamicColor: Boolean = false,
    val defaultMode: TestMode = TestMode.QUICK,
    val keepScreenOn: Boolean = true,
    val saveHistory: Boolean = true,
    val haptics: Boolean = true,
)

/** Who the device talks to: provider and the Cloudflare data centre that served the test. */
data class ConnectionInfo(
    val isp: String? = null,
    val asn: Int? = null,
    val ip: String? = null,
    /** IATA code of the Cloudflare colo, e.g. "FRA". */
    val serverCode: String? = null,
    val serverCity: String? = null,
    val clientCity: String? = null,
    val country: String? = null,
)

/** Phases of one test run, in order. [IDLE] and [DONE] bracket a run. */
enum class TestPhase { IDLE, PING, DOWNLOAD, UPLOAD, DONE }

/** The final numbers of a complete test run. Throughput is in Mbit/s, latency in ms. */
data class Measurement(
    val pingMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val samples: List<TransferSample> = emptyList(),
    /** Median ping while the download / upload was saturating the link (bufferbloat). */
    val downloadLoadedMs: Double? = null,
    val uploadLoadedMs: Double? = null,
    /** Payload bytes moved in both directions. */
    val dataBytes: Long = 0,
    val info: ConnectionInfo? = null,
)

data class TransferSample(
    val stepIndex: Int,
    val round: Int,
    val phase: TestPhase,
    val bytes: Long,
    val timestampMillis: Long,
    val mbps: Double? = null,
    val durationMs: Double? = null,
    val latencyMs: Double? = null,
    val serverTimeMs: Double? = null,
    val responseBytes: Long? = null,
    val loadedLatencies: List<Double> = emptyList(),
    val status: String = "completed",
)

/** A [Measurement] that has been persisted. */
data class SpeedTestRecord(
    val id: Long,
    val timestampMillis: Long,
    val networkType: NetworkType,
    val measurement: Measurement,
)

/** Everything that can go wrong during a run, independent of the HTTP stack. */
sealed interface SpeedTestError {
    /** The device has no network at all. */
    data object Offline : SpeedTestError

    /** The network is up but the test server could not be reached. */
    data class Unreachable(val host: String) : SpeedTestError

    data object Timeout : SpeedTestError

    /**
     * The server answered, but not the way the Cloudflare speed endpoints used to:
     * unexpected status code, wrong payload size, ... The endpoints are not an official, stable API.
     */
    data class EndpointChanged(val url: String, val httpCode: Int?, val detail: String?) : SpeedTestError

    data class Unknown(val message: String?) : SpeedTestError
}

class SpeedTestException(val error: SpeedTestError, cause: Throwable? = null) :
    Exception(error.toString(), cause)

/** Progress reported by a [SpeedTestEngine] while it runs. */
sealed interface SpeedTestEvent {
    data class PhaseStarted(val phase: TestPhase) : SpeedTestEvent
    data class StepStarted(val index: Int, val round: Int) : SpeedTestEvent
    data class SampleCompleted(val sample: TransferSample, val completed: Int, val total: Int) : SpeedTestEvent

    /** One latency probe finished. [index] is 1-based. */
    data class LatencySample(val ms: Double, val index: Int, val total: Int) : SpeedTestEvent

    data class LatencyResult(val pingMs: Double, val jitterMs: Double) : SpeedTestEvent

    /** Smoothed live throughput during [phase]; [progress] is 0..1 within that phase. */
    data class Throughput(val phase: TestPhase, val mbps: Double, val progress: Float) : SpeedTestEvent

    data class PhaseResult(val phase: TestPhase, val mbps: Double) : SpeedTestEvent

    /** Provider and server details; arrives once, early, and only if Cloudflare tells us. */
    data class Info(val info: ConnectionInfo) : SpeedTestEvent

    /** A latency probe that ran while [phase] was saturating the link. */
    data class LoadedLatency(val phase: TestPhase, val ms: Double) : SpeedTestEvent

    data class Finished(val measurement: Measurement) : SpeedTestEvent
}
