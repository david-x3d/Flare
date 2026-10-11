package dev.flare.speedtest.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestPlanTest {
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
