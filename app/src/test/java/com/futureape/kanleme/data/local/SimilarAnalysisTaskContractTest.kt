package com.futureape.kanleme.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarAnalysisTaskContractTest {
    @Test
    fun onlyQueuedRunningAndCancelRequestedStatusesAreActive() {
        assertTrue(SimilarAnalysisTaskStatus.isRunning(SimilarAnalysisTaskStatus.QUEUED))
        assertTrue(SimilarAnalysisTaskStatus.isRunning(SimilarAnalysisTaskStatus.RUNNING))
        assertTrue(SimilarAnalysisTaskStatus.isRunning(SimilarAnalysisTaskStatus.CANCEL_REQUESTED))
        assertFalse(SimilarAnalysisTaskStatus.isRunning(SimilarAnalysisTaskStatus.PAUSED))
        assertFalse(SimilarAnalysisTaskStatus.isRunning(SimilarAnalysisTaskStatus.COMPLETED))
        assertFalse(SimilarAnalysisTaskStatus.isRunning(SimilarAnalysisTaskStatus.CANCELLED))
        assertFalse(SimilarAnalysisTaskStatus.isRunning(SimilarAnalysisTaskStatus.FAILED))
    }

    @Test
    fun pageSizeAndAlgorithmVersionAreStableTaskInputs() {
        assertTrue(SimilarAnalysisContract.PAGE_SIZE > 0)
        assertTrue(SimilarAnalysisContract.ALGORITHM_VERSION > 0)
    }
}
