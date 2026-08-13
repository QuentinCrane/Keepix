package com.futureape.kanleme.data.repository

import java.time.LocalDate

data class CleaningStreakSummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val activeDays: Int,
)

internal fun calculateCleaningStreak(
    localDates: Collection<String>,
    today: String,
): CleaningStreakSummary {
    val dates = localDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSortedSet()
    if (dates.isEmpty()) return CleaningStreakSummary(0, 0, 0)

    var longest = 1
    var run = 1
    val sorted = dates.toList()
    for (index in 1 until sorted.size) {
        run = if (sorted[index - 1].plusDays(1) == sorted[index]) run + 1 else 1
        longest = maxOf(longest, run)
    }

    val todayDate = runCatching { LocalDate.parse(today) }.getOrNull()
    val current = if (todayDate == null) {
        0
    } else {
        val anchor = when {
            todayDate in dates -> todayDate
            todayDate.minusDays(1) in dates -> todayDate.minusDays(1)
            else -> null
        }
        if (anchor == null) 0 else {
            var cursor: LocalDate = anchor!!
            var count = 0
            while (cursor in dates) {
                count += 1
                cursor = cursor.minusDays(1)
            }
            count
        }
    }
    return CleaningStreakSummary(currentStreak = current, longestStreak = longest, activeDays = dates.size)
}
