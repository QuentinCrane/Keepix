package com.futureape.kanleme.data.repository

import java.time.LocalDate
import java.time.ZoneId

data class LocalYearWindow(
    val startMillis: Long,
    val endMillis: Long,
)

internal fun localYearWindow(year: Int, zoneId: ZoneId = ZoneId.systemDefault()): LocalYearWindow {
    val start = LocalDate.of(year, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val end = LocalDate.of(year + 1, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return LocalYearWindow(startMillis = start, endMillis = end)
}
