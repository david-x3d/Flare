package dev.flare.speedtest.domain

/**
 * Turns a monotonically growing byte counter into a smooth live Mbit/s reading: a sliding window
 * gives a stable instantaneous rate, an exponential moving average removes the remaining flicker.
 */
class LiveSpeedMeter(
    private val windowNanos: Long = 600_000_000L,
    private val smoothing: Double = 0.35,
) {
    private data class Sample(val timeNanos: Long, val totalBytes: Long)

    private val window = ArrayDeque<Sample>()
    private var smoothed = 0.0

    /** Latest smoothed value in Mbit/s. */
    val currentMbps: Double get() = smoothed

    /** Feeds the counter value observed at [timeNanos] and returns the updated reading. */
    fun update(timeNanos: Long, totalBytes: Long): Double {
        window.addLast(Sample(timeNanos, totalBytes))
        while (window.size > 2 && timeNanos - window.first().timeNanos > windowNanos) window.removeFirst()
        val first = window.first()
        val elapsed = timeNanos - first.timeNanos
        if (elapsed > 0) {
            val instant = Statistics.megabitsPerSecondNanos(totalBytes - first.totalBytes, elapsed)
            smoothed = if (smoothed == 0.0) instant else smoothed + smoothing * (instant - smoothed)
        }
        return smoothed
    }

    /** Call while no transfer is running so the idle gap does not drag the next reading down. */
    fun pause() = window.clear()
}
