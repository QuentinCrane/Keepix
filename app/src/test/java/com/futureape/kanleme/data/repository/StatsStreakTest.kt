package com.futureape.kanleme.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsStreakTest {
    @Test
    fun sameDayEventsCountOnceAndCurrentStreakIncludesToday() {
        val summary = calculateCleaningStreak(
            localDates = listOf("2026-08-10", "2026-08-11", "2026-08-11", "2026-08-13"),
            today = "2026-08-13",
        )

        assertEquals(CleaningStreakSummary(currentStreak = 1, longestStreak = 2, activeDays = 3), summary)
    }

    @Test
    fun yesterdayCanKeepAStreakAliveButAnOlderGapCannot() {
        assertEquals(
            2,
            calculateCleaningStreak(
                localDates = listOf("2026-08-11", "2026-08-12"),
                today = "2026-08-13",
            ).currentStreak,
        )
        assertEquals(
            0,
            calculateCleaningStreak(
                localDates = listOf("2026-08-10", "2026-08-11"),
                today = "2026-08-13",
            ).currentStreak,
        )
    }
}
