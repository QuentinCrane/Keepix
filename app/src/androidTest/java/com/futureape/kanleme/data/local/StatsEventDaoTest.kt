package com.futureape.kanleme.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StatsEventDaoTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun dailyQueriesUseNetMediaDeltasButCountSubmittedActionsSeparately() = runBlocking {
        val date = "2026-08-13"
        val dao = database.statsDao()
        dao.insertCleanupEvent(event(
            id = "keep-photo",
            type = CleanupEventType.KEPT,
            operationId = 1L,
            mediaType = "photo",
            photoDelta = 1,
            sourceRef = "operation:1",
        ))
        dao.insertCleanupEvent(event(
            id = "restore-photo",
            type = CleanupEventType.COMPENSATED,
            operationId = 1L,
            mediaType = "photo",
            photoDelta = -1,
            sourceRef = "operation:1:restore",
            reversalOfEventId = "keep-photo",
        ))
        dao.insertCleanupEvent(event(
            id = "favorite-video",
            type = CleanupEventType.FAVORITED,
            operationId = 2L,
            mediaType = "video",
            videoDelta = 1,
            favoriteDelta = 1,
            sourceRef = "operation:2",
        ))
        dao.insertCleanupEvent(event(
            id = "other-day",
            type = CleanupEventType.KEPT,
            operationId = 3L,
            mediaType = "photo",
            photoDelta = 1,
            sourceRef = "operation:3",
            localDate = "2026-08-12",
        ))

        assertEquals(0, dao.observeTodayPhotoCount(date).first())
        assertEquals(1, dao.observeTodayVideoCount(date).first())
        assertEquals(3, dao.observeTodayActionCount(date).first())
        assertNotNull(dao.eventBySourceRef("operation:1"))
    }

    @Test
    fun annualAggregatesUseAHalfOpenWindowAndReturnEmptyPeriodsHonestly() = runBlocking {
        val zone = ZoneId.of("Asia/Shanghai")
        val start = LocalDate.of(2024, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(2025, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val dao = database.statsDao()
        dao.insertCleanupEvent(event(
            id = "inside-year",
            type = CleanupEventType.KEPT,
            operationId = 11L,
            mediaType = "photo",
            photoDelta = 1,
            sourceRef = "operation:11",
            localDate = "2024-02-29",
            occurredAt = start + 1_000L,
        ))
        dao.insertCleanupEvent(event(
            id = "at-next-year",
            type = CleanupEventType.KEPT,
            operationId = 12L,
            mediaType = "photo",
            photoDelta = 1,
            sourceRef = "operation:12",
            localDate = "2025-01-01",
            occurredAt = end,
        ))

        val aggregate = dao.aggregateEvents(start, end)
        val months = dao.monthlyEventCounts(start, end)
        val folder = dao.topFolderForPeriod(start, end)
        val emptyStart = end + 1L
        val empty = dao.aggregateEvents(emptyStart, emptyStart + 365L * 24L * 60L * 60L * 1_000L)

        assertEquals(1L, aggregate.photoDelta)
        assertEquals("02", months.single().month)
        assertEquals("Pictures/Keepix/", folder?.folderPath)
        assertEquals(0L, empty.photoDelta)
        assertEquals(0L, empty.actionCount)
    }

    @Test
    fun achievementProgressUpsertKeepsUnlockTimestampIdempotently() = runBlocking {
        val dao = database.statsDao()
        val unlocked = AchievementProgressEntity(
            achievementId = "clear-starter",
            currentValue = 5,
            unlocked = true,
            unlockedAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_000_000L,
        )
        dao.upsertAchievementProgress(unlocked)
        dao.upsertAchievementProgress(unlocked.copy(currentValue = 8, updatedAt = 1_700_000_001_000L))

        val restored = dao.achievementProgress("clear-starter")
        assertEquals(8, restored?.currentValue)
        assertEquals(true, restored?.unlocked)
        assertEquals(1_700_000_000_000L, restored?.unlockedAt)
    }

    private fun event(
        id: String,
        type: String,
        operationId: Long,
        mediaType: String,
        photoDelta: Int = 0,
        videoDelta: Int = 0,
        favoriteDelta: Int = 0,
        sourceRef: String,
        reversalOfEventId: String? = null,
        localDate: String = "2026-08-13",
        occurredAt: Long = 1_755_033_600_000L,
    ) = CleanupEventEntity(
        eventId = id,
        eventType = type,
        operationId = operationId,
        mediaType = mediaType,
        photoDelta = photoDelta,
        videoDelta = videoDelta,
        affectedMediaCount = 1,
        mediaSizeBytes = 1024L,
        sizeDeltaBytes = 0L,
        favoriteDelta = favoriteDelta,
        actionCount = 1,
        localDate = localDate,
        occurredAt = occurredAt,
        sourceType = CleanupEventSourceType.ORGANIZER,
        sourceRef = sourceRef,
        reversalOfEventId = reversalOfEventId,
        folderPath = "Pictures/Keepix/",
        createdAt = occurredAt,
    )
}
