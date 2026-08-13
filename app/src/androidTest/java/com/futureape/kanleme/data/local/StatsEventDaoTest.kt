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
        occurredAt = 1_755_033_600_000L,
        sourceType = CleanupEventSourceType.ORGANIZER,
        sourceRef = sourceRef,
        reversalOfEventId = reversalOfEventId,
        folderPath = "Pictures/Keepix/",
        createdAt = 1_755_033_600_000L,
    )
}
