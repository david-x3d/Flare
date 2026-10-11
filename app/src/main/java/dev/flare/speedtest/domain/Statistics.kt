package dev.flare.speedtest.domain

import kotlin.math.sqrt

/** Pure maths behind the speed test; kept free of Android and networking so it is easy to test. */
object Statistics {

    private const val BITS_PER_BYTE = 8.0
    private const val BITS_PER_MEGABIT = 1_000_000.0

    /** Megabit per second for [bytes] transferred in [seconds]: `bytes * 8 / seconds / 1e6`. */
    fun megabitsPerSecond(bytes: Long, seconds: Double): Double {
        if (bytes <= 0L || seconds <= 0.0) return 0.0
        return bytes * BITS_PER_BYTE / seconds / BITS_PER_MEGABIT
    }

    /** Same as [megabitsPerSecond] for a duration in nanoseconds. */
    fun megabitsPerSecondNanos(bytes: Long, nanos: Long): Double =
        megabitsPerSecond(bytes, nanos / 1e9)

    /** Median; the mean of the two middle values for an even count. Empty input yields 0. */
    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2.0
    }

    fun mean(values: List<Double>): Double = if (values.isEmpty()) 0.0 else values.sum() / values.size

    /** Population standard deviation, used as jitter. Fewer than two values yield 0. */
    fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = mean(values)
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance)
    }

    /**
     * Percentile with linear interpolation between closest ranks (the "type 7" definition used by
     * NumPy and Excel). [fraction] is in 0..1; empty input yields 0.
     */
    fun percentile(values: List<Double>, fraction: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        if (sorted.size == 1) return sorted[0]
        val rank = fraction.coerceIn(0.0, 1.0) * (sorted.size - 1)
        val lower = rank.toInt()
        val upper = minOf(lower + 1, sorted.lastIndex)
        return sorted[lower] + (sorted[upper] - sorted[lower]) * (rank - lower)
    }

    /**
     * Robust throughput estimate from per-round measurements. Early rounds are slowed by TCP slow
     * start and request latency, so the mean would under-report; the 90th percentile ignores them
     * while still shrugging off a single lucky outlier.
     */
    fun robustThroughput(roundMbps: List<Double>, fraction: Double = ROBUST_PERCENTILE): Double =
        percentile(roundMbps, fraction)

    const val ROBUST_PERCENTILE = 0.9
}
