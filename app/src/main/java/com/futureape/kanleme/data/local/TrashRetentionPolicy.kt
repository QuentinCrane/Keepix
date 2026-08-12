package com.futureape.kanleme.data.local

import kotlin.math.ceil

object TrashRetentionPolicy {
    const val REVIEW_WINDOW_DAYS = 30
    const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun reviewDueAt(trashedAt: Long): Long =
        if (trashedAt <= 0L) 0L else trashedAt + REVIEW_WINDOW_DAYS * MILLIS_PER_DAY

    fun daysUntilReview(reviewDueAt: Long, now: Long): Int {
        if (reviewDueAt <= 0L || now >= reviewDueAt) return 0
        return ceil((reviewDueAt - now).toDouble() / MILLIS_PER_DAY).toInt()
    }

    fun daysStored(trashedAt: Long, now: Long): Int {
        if (trashedAt <= 0L || now <= trashedAt) return 0
        return ((now - trashedAt) / MILLIS_PER_DAY).toInt()
    }
}
