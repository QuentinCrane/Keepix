package com.futureape.kanleme.ui.util

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaDayKeyCacheTest {
    @Test fun theSameTimestampCanBelongToDifferentLocalDays() {
        val timestamp = Instant.parse("2026-10-05T18:00:00Z").toEpochMilli()
        assertEquals("2026-10-05", MediaDayKeyCache.dayKey(timestamp, ZoneOffset.UTC))
        assertEquals("2026-10-06", MediaDayKeyCache.dayKey(timestamp, ZoneOffset.ofHours(8)))
    }

    @Test fun repeatedDatesAndEvictedEntriesStillReturnTheCorrectDay() {
        val start = Instant.parse("2026-10-05T00:00:00Z").toEpochMilli()
        repeat(1100) { MediaDayKeyCache.dayKey(start + it * 1000L, ZoneOffset.UTC) }
        assertEquals("2026-10-05", MediaDayKeyCache.dayKey(start, ZoneOffset.UTC))
    }
}
