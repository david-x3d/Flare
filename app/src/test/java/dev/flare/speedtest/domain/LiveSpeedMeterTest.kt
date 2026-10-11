package dev.flare.speedtest.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveSpeedMeterTest {

    private fun millis(ms: Long) = ms * 1_000_000L

    @Test
    fun `steady stream converges to the true rate`() {
        val meter = LiveSpeedMeter()
        // 1.25 MB every 100 ms = 100 Mbit/s
        var bytes = 0L
        var last = 0.0
        for (i in 1..40) {
            bytes += 1_250_000
            last = meter.update(millis(i * 100L), bytes)
        }
        assertEquals(100.0, last, 1.0)
    }

    @Test
    fun `reading rises and falls with the stream`() {
        val meter = LiveSpeedMeter()
        var bytes = 0L
        var t = 0L
        repeat(20) { t += 100; bytes += 1_250_000; meter.update(millis(t), bytes) }
        val fast = meter.currentMbps
        repeat(20) { t += 100; bytes += 125_000; meter.update(millis(t), bytes) }
        assertTrue(meter.currentMbps < fast / 2)
    }

    @Test
    fun `first sample alone cannot produce a rate`() {
        val meter = LiveSpeedMeter()
        assertEquals(0.0, meter.update(millis(100), 1_000_000), 0.0)
    }
}
