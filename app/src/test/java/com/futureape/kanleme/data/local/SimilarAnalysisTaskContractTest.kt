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
    fun pausedAndFailedTasksCanBeCancelledBeforeAWorkerResumes() {
        assertTrue(SimilarAnalysisTaskStatus.isCancellable(SimilarAnalysisTaskStatus.PAUSED))
        assertTrue(SimilarAnalysisTaskStatus.isCancellable(SimilarAnalysisTaskStatus.FAILED))
        assertFalse(SimilarAnalysisTaskStatus.isCancellable(SimilarAnalysisTaskStatus.COMPLETED))
        assertFalse(SimilarAnalysisTaskStatus.isCancellable(SimilarAnalysisTaskStatus.CANCELLED))
    }

    @Test
    fun pageSizeAndAlgorithmVersionAreStableTaskInputs() {
        assertTrue(SimilarAnalysisContract.PAGE_SIZE > 0)
        assertTrue(SimilarAnalysisContract.ALGORITHM_VERSION > 0)
        assertTrue(SimilarAnalysisContract.SQLITE_ID_QUERY_CHUNK <= 999)
    }
}
