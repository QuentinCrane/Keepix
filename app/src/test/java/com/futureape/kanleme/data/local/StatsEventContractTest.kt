package com.futureape.kanleme.data.local

import com.futureape.kanleme.data.repository.SwipeAction
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsEventContractTest {
    @Test
    fun swipeActionsMapToStableCanonicalEventTypes() {
        assertEquals(CleanupEventType.KEPT, cleanupEventTypeForAction(SwipeAction.Keep.name))
        assertEquals(CleanupEventType.FAVORITED, cleanupEventTypeForAction(SwipeAction.Favorite.name))
        assertEquals(CleanupEventType.MOVED_TO_APP_TRASH, cleanupEventTypeForAction(SwipeAction.Delete.name))
    }

    @Test
    fun compensationKeepsAffectedMediaAndActionCountButReversesNetEffects() {
        val original = CleanupEventMetrics(
            photoDelta = 1,
            mediaSizeBytes = 12_345L,
            sizeDeltaBytes = 12_345L,
            favoriteDelta = 1,
            affectedMediaCount = 1,
            actionCount = 1,
        )

        assertEquals(
            CleanupEventMetrics(
                photoDelta = -1,
                mediaSizeBytes = 12_345L,
                sizeDeltaBytes = -12_345L,
                favoriteDelta = -1,
                affectedMediaCount = 1,
                actionCount = 1,
            ),
            original.compensation(),
        )
    }
}
