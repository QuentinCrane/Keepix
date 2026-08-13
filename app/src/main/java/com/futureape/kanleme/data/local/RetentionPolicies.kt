package com.futureape.kanleme.data.local

object OperationHistoryRetention {
    const val UNDONE_RETENTION_MILLIS: Long = 90L * 24L * 60L * 60L * 1000L

    fun undoneCutoff(now: Long): Long = now - UNDONE_RETENTION_MILLIS
}
