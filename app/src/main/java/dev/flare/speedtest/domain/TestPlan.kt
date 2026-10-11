package dev.flare.speedtest.domain

/** [rounds] rounds of [bytes]-sized transfers; every round runs on all parallel connections. */
data class TransferStep(val bytes: Long, val rounds: Int)

/** Describes how a test run is carried out. Pure data so tests can run tiny plans. */
data class TestPlan(
    val latencySamples: Int,
    val downloadSteps: List<TransferStep>,
    val uploadSteps: List<TransferStep>,
    val downloadConnections: Int,
    val uploadConnections: Int,
    /** Hard time limit per transfer phase; a slow link never blocks the app for longer. */
    val phaseBudgetMillis: Long,
    /** A step is skipped when its extrapolated round time exceeds this. */
    val maxRoundMillis: Long = 5_000L,
    /** Rounds shorter than this are dominated by latency and are ignored for the result. */
    val minRoundMillis: Long = 10L,
) {
    companion object {
        private const val KB = 1_000L
        private const val MB = 1_000_000L

        fun forSize(size: TestSize): TestPlan = when (size) {
            TestSize.SMALL -> TestPlan(
                latencySamples = 10,
                downloadSteps = listOf(TransferStep(100 * KB, 2), TransferStep(1 * MB, 3), TransferStep(10 * MB, 2)),
                uploadSteps = listOf(TransferStep(100 * KB, 2), TransferStep(1 * MB, 3), TransferStep(10 * MB, 2)),
                downloadConnections = 3,
                uploadConnections = 3,
                phaseBudgetMillis = 8_000L,
            )

            TestSize.NORMAL -> TestPlan(
                latencySamples = 10,
                downloadSteps = listOf(
                    TransferStep(100 * KB, 2), TransferStep(1 * MB, 3),
                    TransferStep(10 * MB, 3), TransferStep(25 * MB, 2),
                ),
                uploadSteps = listOf(
                    TransferStep(100 * KB, 2), TransferStep(1 * MB, 3),
                    TransferStep(10 * MB, 3), TransferStep(25 * MB, 2),
                ),
                downloadConnections = 4,
                uploadConnections = 3,
                phaseBudgetMillis = 12_000L,
            )

            TestSize.LARGE -> TestPlan(
                latencySamples = 10,
                downloadSteps = listOf(
                    TransferStep(100 * KB, 2), TransferStep(1 * MB, 3),
                    TransferStep(10 * MB, 4), TransferStep(25 * MB, 8),
                ),
                uploadSteps = listOf(
                    TransferStep(100 * KB, 2), TransferStep(1 * MB, 3),
                    TransferStep(10 * MB, 4), TransferStep(25 * MB, 6),
                ),
                downloadConnections = 4,
                uploadConnections = 4,
                phaseBudgetMillis = 25_000L,
            )
        }
    }
}
