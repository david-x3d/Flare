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

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val unit: SpeedUnit = SpeedUnit.MBIT_PER_SECOND,
    val testSize: TestSize = TestSize.NORMAL,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
)

/** Phases of one test run, in order. [IDLE] and [DONE] bracket a run. */
enum class TestPhase { IDLE, PING, DOWNLOAD, UPLOAD, DONE }

/** The final numbers of a complete test run. Throughput is in Mbit/s, latency in ms. */
data class Measurement(
    val pingMs: Double,
    val jitterMs: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
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

    /** One latency probe finished. [index] is 1-based. */
    data class LatencySample(val ms: Double, val index: Int, val total: Int) : SpeedTestEvent

    data class LatencyResult(val pingMs: Double, val jitterMs: Double) : SpeedTestEvent

    /** Smoothed live throughput during [phase]; [progress] is 0..1 within that phase. */
    data class Throughput(val phase: TestPhase, val mbps: Double, val progress: Float) : SpeedTestEvent

    data class PhaseResult(val phase: TestPhase, val mbps: Double) : SpeedTestEvent

    data class Finished(val measurement: Measurement) : SpeedTestEvent
}
