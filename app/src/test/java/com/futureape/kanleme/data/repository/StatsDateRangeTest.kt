package com.futureape.kanleme.data.repository

import java.time.Duration
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsDateRangeTest {
    @Test
    fun leapYearWindowUsesTheLocalStartOfTheNextYear() {
        val window = localYearWindow(2024, ZoneId.of("Asia/Shanghai"))

        assertEquals(Duration.ofDays(366).toMillis(), window.endMillis - window.startMillis)
    }

    @Test
    fun yearWindowIsHalfOpenAndDoesNotIncludeTheNextYearBoundary() {
        val window = localYearWindow(2025, ZoneId.of("Asia/Shanghai"))

        assertTrue(window.startMillis < window.endMillis)
        assertEquals(window.endMillis, localYearWindow(2026, ZoneId.of("Asia/Shanghai")).startMillis)
    }
}
