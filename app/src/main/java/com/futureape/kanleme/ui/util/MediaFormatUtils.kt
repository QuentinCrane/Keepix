package com.futureape.kanleme.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.0f KB".format(kb)
        else -> "$bytes B"
    }
}

fun formatDuration(durationMs: Long): String {
    val total = durationMs.coerceAtLeast(0L) / 1000
    val min = total / 60
    val sec = total % 60
    return "%d:%02d".format(min, sec)
}

fun formatDate(timeMillis: Long): String {
    if (timeMillis <= 0L) return "未知时间"
    return SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date(timeMillis))
}
