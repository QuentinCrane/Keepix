package com.futureape.kanleme.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class TrashRetentionPolicyTest {
    @Test
    fun reviewDueAt_usesThirtyDayReviewWindow() {
        val trashedAt = 1_700_000_000_000L

        assertEquals(
            trashedAt + 30L * TrashRetentionPolicy.MILLIS_PER_DAY,
            TrashRetentionPolicy.reviewDueAt(trashedAt),
        )
    }

    @Test
    fun daysUntilReview_ceilBoundaryAndNeverBecomesNegative() {
        val dueAt = 1_700_000_000_000L + 30L * TrashRetentionPolicy.MILLIS_PER_DAY

        assertEquals(2, TrashRetentionPolicy.daysUntilReview(dueAt, dueAt - 1L * TrashRetentionPolicy.MILLIS_PER_DAY - 1L))
        assertEquals(1, TrashRetentionPolicy.daysUntilReview(dueAt, dueAt - 1L))
        assertEquals(0, TrashRetentionPolicy.daysUntilReview(dueAt, dueAt))
        assertEquals(0, TrashRetentionPolicy.daysUntilReview(dueAt, dueAt + TrashRetentionPolicy.MILLIS_PER_DAY))
    }

    @Test
    fun daysStored_usesElapsedWholeDays() {
        val trashedAt = 1_700_000_000_000L

        assertEquals(0, TrashRetentionPolicy.daysStored(trashedAt, trashedAt + TrashRetentionPolicy.MILLIS_PER_DAY - 1L))
        assertEquals(3, TrashRetentionPolicy.daysStored(trashedAt, trashedAt + 3L * TrashRetentionPolicy.MILLIS_PER_DAY + 99L))
    }
}
