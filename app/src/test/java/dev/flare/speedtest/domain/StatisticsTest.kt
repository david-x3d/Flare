package dev.flare.speedtest.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsTest {

    @Test
    fun `megabits per second uses bytes times eight over seconds over one million`() {
        // 12.5 MB in 1 s = 100 Mbit/s
        assertEquals(100.0, Statistics.megabitsPerSecond(12_500_000, 1.0), 1e-9)
        // 25 MB in 2 s = 100 Mbit/s
        assertEquals(100.0, Statistics.megabitsPerSecond(25_000_000, 2.0), 1e-9)
        assertEquals(8.0, Statistics.megabitsPerSecondNanos(1_000_000, 1_000_000_000L), 1e-9)
    }

    @Test
    fun `megabits per second is zero for empty or instant transfers`() {
        assertEquals(0.0, Statistics.megabitsPerSecond(0, 1.0), 0.0)
        assertEquals(0.0, Statistics.megabitsPerSecond(1_000, 0.0), 0.0)
        assertEquals(0.0, Statistics.megabitsPerSecond(1_000, -1.0), 0.0)
    }

    @Test
    fun `median of odd count is the middle value`() {
        assertEquals(3.0, Statistics.median(listOf(5.0, 1.0, 3.0)), 0.0)
    }

    @Test
    fun `median of even count is the mean of the two middle values`() {
        assertEquals(2.5, Statistics.median(listOf(4.0, 1.0, 3.0, 2.0)), 0.0)
    }

    @Test
    fun `median ignores outliers and empty input yields zero`() {
        assertEquals(10.0, Statistics.median(listOf(9.0, 10.0, 11.0, 500.0, 10.0)), 0.0)
        assertEquals(0.0, Statistics.median(emptyList()), 0.0)
    }

    @Test
    fun `standard deviation is the population deviation`() {
        // classic example: mean 5, variance 4, deviation 2
        assertEquals(2.0, Statistics.standardDeviation(listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0)), 1e-12)
    }

    @Test
    fun `jitter is zero for constant latency and for fewer than two samples`() {
        assertEquals(0.0, Statistics.standardDeviation(List(10) { 20.0 }), 0.0)
        assertEquals(0.0, Statistics.standardDeviation(listOf(20.0)), 0.0)
        assertEquals(0.0, Statistics.standardDeviation(emptyList()), 0.0)
    }

    @Test
    fun `percentile interpolates linearly between ranks`() {
        val values = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        assertEquals(10.0, Statistics.percentile(values, 0.0), 1e-12)
        assertEquals(30.0, Statistics.percentile(values, 0.5), 1e-12)
        assertEquals(46.0, Statistics.percentile(values, 0.9), 1e-12)
        assertEquals(50.0, Statistics.percentile(values, 1.0), 1e-12)
    }

    @Test
    fun `percentile handles unsorted input single value and out of range fractions`() {
        assertEquals(46.0, Statistics.percentile(listOf(50.0, 10.0, 40.0, 20.0, 30.0), 0.9), 1e-12)
        assertEquals(7.0, Statistics.percentile(listOf(7.0), 0.9), 0.0)
        assertEquals(50.0, Statistics.percentile(listOf(10.0, 50.0), 3.0), 0.0)
        assertEquals(0.0, Statistics.percentile(emptyList(), 0.9), 0.0)
    }

    @Test
    fun `robust throughput discards slow-start rounds that drag a mean down`() {
        val rounds = listOf(5.0, 20.0, 90.0, 95.0, 100.0, 100.0, 98.0, 101.0, 99.0, 100.0)
        val robust = Statistics.robustThroughput(rounds)
        assertEquals(true, robust in 99.0..101.0)
        assertEquals(true, Statistics.mean(rounds) < 85.0)
    }
}
