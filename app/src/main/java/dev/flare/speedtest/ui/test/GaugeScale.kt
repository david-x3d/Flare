package dev.flare.speedtest.ui.test

/**
 * Maps a speed in Mbit/s to 0..1 on the gauge. The scale is piecewise linear between "friendly"
 * speeds so that a 20 Mbit/s DSL line and a 900 Mbit/s fibre line both move the ring visibly.
 */
object GaugeScale {
    private val ticks = doubleArrayOf(0.0, 5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2000.0)

    fun fraction(mbps: Double): Float {
        if (mbps <= 0.0) return 0f
        if (mbps >= ticks.last()) return 1f
        val upper = ticks.indexOfFirst { mbps < it }
        val lower = upper - 1
        val within = (mbps - ticks[lower]) / (ticks[upper] - ticks[lower])
        return ((lower + within) / (ticks.size - 1)).toFloat()
    }
}
