package dev.flare.speedtest.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestPlanTest {
    @Test fun `full reproduces CSV group order and all 49 transfers`() {
        val steps = TestPlan.full().detailedSteps
        assertEquals(listOf(10, 8, 8, 6, 6, 4, 4, 3), steps.map { it.rounds })
        assertEquals(listOf(100_000L, 1_000_000L, 100_000L, 1_000_000L, 10_000_000L, 10_000_000L, 25_000_000L, 99_999_999L), steps.map { it.bytes })
        assertEquals(listOf(TestPhase.DOWNLOAD, TestPhase.DOWNLOAD, TestPhase.UPLOAD, TestPhase.UPLOAD, TestPhase.DOWNLOAD, TestPhase.UPLOAD, TestPhase.DOWNLOAD, TestPhase.DOWNLOAD), steps.map { it.phase })
        assertEquals(49, steps.sumOf { it.rounds })
        assertEquals(515_799_997L, steps.sumOf { it.bytes * it.rounds })
    }

    @Test
    fun `quick transfers less data and has a shorter budget than every full size`() {
        fun TestPlan.bytes() = if (detailedSteps.isNotEmpty()) detailedSteps.sumOf { it.bytes * it.rounds } else downloadSteps.sumOf { it.bytes * it.rounds * downloadConnections } + uploadSteps.sumOf { it.bytes * it.rounds * uploadConnections }
        TestSize.entries.forEach { size ->
            val quick = TestPlan.forMode(TestMode.QUICK, size)
            val full = TestPlan.forMode(TestMode.FULL, size)
            assertTrue(quick.bytes() < full.bytes())
            assertTrue(quick.phaseBudgetMillis < full.phaseBudgetMillis)
            assertTrue(quick.latencySamples < full.latencySamples)
            assertEquals(TestPlan.full(), full)
        }
    }

    @Test
    fun `every size ramps up payloads in ascending order from 100 kB`() {
        TestSize.entries.forEach { size ->
            val plan = TestPlan.forSize(size)
            listOf(plan.downloadSteps, plan.uploadSteps).forEach { steps ->
                assertEquals(100_000L, steps.first().bytes)
                assertEquals(steps.map { it.bytes }, steps.map { it.bytes }.sorted())
            }
            assertEquals(10, plan.latencySamples)
            assertTrue(plan.downloadConnections in 3..4)
        }
    }

    @Test
    fun `normal reaches 25 MB while small stops at 10 MB`() {
        assertEquals(25_000_000L, TestPlan.forSize(TestSize.NORMAL).downloadSteps.last().bytes)
        assertEquals(10_000_000L, TestPlan.forSize(TestSize.SMALL).downloadSteps.last().bytes)
    }

    @Test
    fun `larger sizes get a longer time budget`() {
        val budgets = TestSize.entries.map { TestPlan.forSize(it).phaseBudgetMillis }
        assertEquals(budgets.sorted(), budgets)
    }
}
