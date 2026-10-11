package dev.flare.speedtest.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedUnitTest {
    @Test
    fun `megabit per second is the identity`() {
        assertEquals(94.5, SpeedUnit.MBIT_PER_SECOND.fromMbps(94.5), 0.0)
    }

    @Test
    fun `megabyte per second divides by eight`() {
        assertEquals(12.5, SpeedUnit.MBYTE_PER_SECOND.fromMbps(100.0), 1e-12)
    }
}
