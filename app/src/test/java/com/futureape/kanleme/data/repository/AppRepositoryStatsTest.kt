package com.futureape.kanleme.data.repository

import com.futureape.kanleme.data.local.DeletionStatus
import com.futureape.kanleme.data.local.OperationHistoryEntity
import com.futureape.kanleme.data.local.ProcessingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRepositoryStatsTest {
    @Test
    fun undoDelete_reversesFirstProcessedPhotoAndStorage() {
        val adjustment = statsAdjustmentForUndo(
            operation = operation(
                mediaType = "photo",
                previousProcessing = ProcessingStatus.UNPROCESSED,
                previousDeletion = DeletionStatus.NONE,
                newProcessing = ProcessingStatus.UNPROCESSED,
                newDeletion = DeletionStatus.PENDING,
                action = SwipeAction.Delete,
            ),
            mediaSize = 12_345L,
        )

        assertEquals(StatsAdjustment(photoDelta = -1, storageDelta = -12_345L), adjustment)
    }

    @Test
    fun undoFavoriteFromKept_reversesFavoriteWithoutDoubleCountingProcessedVideo() {
        val adjustment = statsAdjustmentForUndo(
            operation = operation(
                mediaType = "video",
                previousProcessing = ProcessingStatus.KEPT,
                previousDeletion = DeletionStatus.NONE,
                newProcessing = ProcessingStatus.FAVORITED,
                newDeletion = DeletionStatus.NONE,
                action = SwipeAction.Favorite,
            ),
            mediaSize = 9_999L,
        )

        assertEquals(StatsAdjustment(favoriteDelta = -1), adjustment)
    }

    @Test
    fun undoNoOpKeep_doesNotChangeCounters() {
        val adjustment = statsAdjustmentForUndo(
            operation = operation(
                mediaType = "photo",
                previousProcessing = ProcessingStatus.KEPT,
                previousDeletion = DeletionStatus.NONE,
                newProcessing = ProcessingStatus.KEPT,
                newDeletion = DeletionStatus.NONE,
                action = SwipeAction.Keep,
            ),
            mediaSize = 1_000L,
        )

        assertEquals(StatsAdjustment(), adjustment)
    }

    private fun operation(
        mediaType: String,
        previousProcessing: String,
        previousDeletion: String,
        newProcessing: String,
        newDeletion: String,
        action: SwipeAction,
    ) = OperationHistoryEntity(
        mediaId = 42L,
        mediaType = mediaType,
        previousProcessingStatus = previousProcessing,
        previousDeletionStatus = previousDeletion,
        newProcessingStatus = newProcessing,
        newDeletionStatus = newDeletion,
        action = action.name,
    )
}
