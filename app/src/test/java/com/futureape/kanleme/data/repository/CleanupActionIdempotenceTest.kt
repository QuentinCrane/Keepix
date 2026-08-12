package com.futureape.kanleme.data.repository

import com.futureape.kanleme.data.local.DeletionStatus
import com.futureape.kanleme.data.local.ProcessingStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupActionIdempotenceTest {
    @Test
    fun onlyTheSameAlreadyAppliedActionIsARepositoryNoOp() {
        assertTrue(isCleanupActionAlreadyApplied(SwipeAction.Keep, ProcessingStatus.KEPT, DeletionStatus.NONE))
        assertFalse(isCleanupActionAlreadyApplied(SwipeAction.Keep, ProcessingStatus.FAVORITED, DeletionStatus.NONE))
        assertTrue(isCleanupActionAlreadyApplied(SwipeAction.Favorite, ProcessingStatus.FAVORITED, DeletionStatus.NONE))
        assertFalse(isCleanupActionAlreadyApplied(SwipeAction.Favorite, ProcessingStatus.KEPT, DeletionStatus.NONE))
        assertTrue(isCleanupActionAlreadyApplied(SwipeAction.Delete, ProcessingStatus.UNPROCESSED, DeletionStatus.PENDING))
        assertTrue(isCleanupActionAlreadyApplied(SwipeAction.Delete, ProcessingStatus.UNPROCESSED, DeletionStatus.TRASHED))
        assertFalse(isCleanupActionAlreadyApplied(SwipeAction.Delete, ProcessingStatus.UNPROCESSED, DeletionStatus.NONE))
    }

    @Test
    fun statsAdjustmentReportsOnlyRealStateAndEventDeltas() {
        assertTrue(StatsAdjustment(photoDelta = 1).hasDelta())
        assertTrue(StatsAdjustment(favoriteDelta = 1).hasDelta())
        assertTrue(StatsAdjustment(storageDelta = 1024L).hasDelta())
        assertFalse(StatsAdjustment().hasDelta())
    }
}
