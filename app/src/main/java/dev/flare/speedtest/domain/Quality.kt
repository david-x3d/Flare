package dev.flare.speedtest.domain

import kotlin.math.ln
import kotlin.math.roundToInt

/** Overall verdict for a connection. */
enum class Grade { EXCELLENT, GOOD, FAIR, POOR }

/** How much latency grows under load; A+ is a link that stays responsive while saturated. */
enum class BloatGrade { A_PLUS, A, B, C, D, F }

/** Everyday things a connection can (or cannot) carry. */
enum class UseCase { BROWSING, STREAMING_HD, STREAMING_4K, VIDEO_CALLS, GAMING, CLOUD_BACKUP }

/** Pure, testable rules that turn raw numbers into something a person can read. */
object Quality {

    /** 0..100: download 40 %, upload 20 %, ping 25 %, jitter 15 %, throughput on a log scale. */
    fun score(m: Measurement): Int {
        fun logScale(value: Double, full: Double) =
            if (value <= 1.0) (value / 1.0) * 0.05 else (ln(value) / ln(full)).coerceIn(0.0, 1.0)
        fun linear(value: Double, best: Double, worst: Double) =
            ((worst - value) / (worst - best)).coerceIn(0.0, 1.0)

        val score = 40 * logScale(m.downloadMbps, 300.0) +
            20 * logScale(m.uploadMbps, 100.0) +
            25 * linear(m.pingMs, 8.0, 150.0) +
            15 * linear(m.jitterMs, 1.0, 40.0)
        return score.roundToInt().coerceIn(0, 100)
    }

    fun grade(score: Int): Grade = when {
        score >= 80 -> Grade.EXCELLENT
        score >= 60 -> Grade.GOOD
        score >= 40 -> Grade.FAIR
        else -> Grade.POOR
    }

    /** Extra latency under load in ms, the worse of both directions; null if not measured. */
    fun latencyIncrease(m: Measurement): Double? {
        val loaded = listOfNotNull(m.downloadLoadedMs, m.uploadLoadedMs).maxOrNull() ?: return null
        return (loaded - m.pingMs).coerceAtLeast(0.0)
    }

    /** Thresholds follow the widely used Waveform bufferbloat grading. */
    fun bloatGrade(increaseMs: Double): BloatGrade = when {
        increaseMs < 5 -> BloatGrade.A_PLUS
        increaseMs < 30 -> BloatGrade.A
        increaseMs < 60 -> BloatGrade.B
        increaseMs < 200 -> BloatGrade.C
        increaseMs < 400 -> BloatGrade.D
        else -> BloatGrade.F
    }

    fun isSuitable(useCase: UseCase, m: Measurement): Boolean = when (useCase) {
        UseCase.BROWSING -> m.downloadMbps >= 3 && m.pingMs < 300
        UseCase.STREAMING_HD -> m.downloadMbps >= 8
        UseCase.STREAMING_4K -> m.downloadMbps >= 25
        UseCase.VIDEO_CALLS -> m.downloadMbps >= 4 && m.uploadMbps >= 3 && m.pingMs < 150 && m.jitterMs < 30
        UseCase.GAMING -> m.downloadMbps >= 10 && m.uploadMbps >= 2 && m.pingMs < 60 && m.jitterMs < 15
        UseCase.CLOUD_BACKUP -> m.uploadMbps >= 20
    }
}
