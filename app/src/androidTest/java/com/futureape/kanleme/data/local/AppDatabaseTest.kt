package com.futureape.kanleme.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futureape.kanleme.data.local.ProcessingStatus.KEPT
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
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
    fun schemaCreatesAndPhotoDaoReadsWrites() = runBlocking {
        val photo = photo(mediaStoreId = 42L, displayName = "sample.jpg")

        database.photoDao().upsertAll(listOf(photo))
        val stored = database.photoDao().byMediaStoreIds(listOf(42L)).single()
        database.photoDao().updateProcessingStatus(stored.id, KEPT, now = 1_700_000_001_000L)

        assertEquals(1, database.photoDao().observeCount().first())
        assertEquals(KEPT, database.photoDao().byId(stored.id)?.processingStatus)
    }

    @Test
    fun dashboardProjectionPreservesCountsAndTodayAggregates() = runBlocking {
        database.photoDao().upsertAll(
            listOf(
                photo(mediaStoreId = 43L, displayName = "pending.jpg").copy(
                    processingStatus = KEPT,
                    deletionStatus = DeletionStatus.TRASHED,
                    size = 4_096L,
                ),
            ),
        )
        database.statsDao().insertCleanupEvent(
            CleanupEventEntity(
                eventId = "dashboard-event",
                eventType = "kept",
                mediaType = "photo",
                photoDelta = 1,
                actionCount = 2,
                localDate = "2026-08-13",
                occurredAt = 1_700_000_001_000L,
                sourceType = "test",
                createdAt = 1_700_000_001_000L,
            ),
        )

        val counts = database.statsDao().observeDashboardCounts("2026-08-13").first()

        assertEquals(1, counts.photoCount)
        assertEquals(1, counts.processedPhotoCount)
        assertEquals(1, counts.photoPendingDeleteCount)
        assertEquals(4_096L, counts.photoPendingDeleteBytes)
        assertEquals(1, counts.todayPhotoCount)
        assertEquals(2, counts.todayActionCount)
    }

    @Test
    fun photoTypeProjectionCountsOnlyActiveRows() = runBlocking {
        database.photoDao().upsertAll(
            listOf(
                photo(mediaStoreId = 44L, displayName = "normal.jpg"),
                photo(mediaStoreId = 45L, displayName = "screen.png").copy(isScreenshot = true),
                photo(mediaStoreId = 46L, displayName = "missing.jpg").copy(missingSince = 1_700_000_000_000L),
            ),
        )

        val counts = database.photoDao().observeTypeStats().first()

        assertEquals(2, counts.allCount)
        assertEquals(1, counts.normalCount)
        assertEquals(1, counts.screenshotCount)
    }

    @Test
    fun criticalQueryPlansUseD05CompositeIndexes() {
        val queuePlan = explainPlan(
            "SELECT id FROM photos WHERE processing_status = 'unprocessed' " +
                "AND deletion_status = 'none' AND missing_since IS NULL " +
                "ORDER BY date_taken DESC LIMIT 120",
        )
        val activePlan = explainPlan(
            "SELECT id FROM photos WHERE deletion_status = 'none' " +
                "AND missing_since IS NULL ORDER BY date_taken DESC LIMIT 5000",
        )
        val operationPlan = explainPlan(
            "SELECT id FROM operation_history WHERE media_id = 42 AND media_type = 'photo' " +
                "AND action = 'Delete' AND is_undone = 0 ORDER BY created_at DESC LIMIT 1",
        )
        val eventPlan = explainPlan(
            "SELECT event_id FROM cleanup_events WHERE source_ref = 'op:1' " +
                "AND reversal_of_event_id IS NULL ORDER BY occurred_at DESC LIMIT 1",
        )

        assertTrue(queuePlan, queuePlan.contains("index_photos_queue_date"))
        assertTrue(activePlan, activePlan.contains("index_photos_active_date"))
        assertTrue(operationPlan, operationPlan.contains("index_operation_history_media_action"))
        assertTrue(eventPlan, eventPlan.contains("index_cleanup_events_source"))
    }

    @Test
    fun randomQueueOrderIsStableWhenRefreshedWithTheSameSeed() = runBlocking {
        database.photoDao().upsertAll(
            (0 until 12).map { index ->
                photo(
                    mediaStoreId = 100L + index,
                    displayName = "queue-$index.jpg",
                    dateTaken = 1_700_000_000_000L - index * 86_400_000L,
                    folderPath = if (index % 2 == 0) "Pictures/A" else "Pictures/B",
                )
            },
        )

        suspend fun load() = database.photoDao().nextFilteredBatch(
            folderPath = null,
            startMillis = null,
            endMillis = null,
            multiMonthEnabled = false,
            monthTokens = listOf(""),
            mediaType = "all",
            randomOrder = true,
            randomSeed = 9_876L,
            todayOnly = false,
            limit = 12,
        )

        val firstOrder = load().map { it.mediaStoreId }
        val refreshedOrder = load().map { it.mediaStoreId }

        assertEquals(12, firstOrder.size)
        assertEquals(firstOrder, refreshedOrder)
    }

    @Test
    fun operationHistoryProvidesOneUndoableRecordUntilConsumed() = runBlocking {
        val operationId = database.operationDao().insert(
            OperationHistoryEntity(
                mediaId = 77L,
                mediaType = "photo",
                previousProcessingStatus = ProcessingStatus.UNPROCESSED,
                previousDeletionStatus = DeletionStatus.NONE,
                newProcessingStatus = ProcessingStatus.KEPT,
                newDeletionStatus = DeletionStatus.NONE,
                action = "Keep",
            ),
        )

        assertEquals(operationId, database.operationDao().undoableById(operationId)?.id)
        database.operationDao().markUndone(operationId)
        assertNull(database.operationDao().undoableById(operationId))
        assertNull(database.operationDao().lastUndoable("photo"))
    }

    @Test
    fun similarAnalysisTaskPersistsCursorAndCancellationRequest() = runBlocking {
        val dao = database.similarDao()
        val queued = SimilarAnalysisTaskEntity(
            status = SimilarAnalysisTaskStatus.QUEUED,
            runToken = "run-1",
            mediaVersion = "media-v1",
            accessKey = "images=true;videos=true;selected=false",
            requestedLimit = 3600,
            totalPhotos = 120,
            cursor = 24,
            stage = "正在计算缩略指纹",
        )
        dao.upsertAnalysisTask(queued)

        dao.upsertAnalysisTask(
            queued.copy(
                status = SimilarAnalysisTaskStatus.CANCEL_REQUESTED,
                cancelRequested = true,
                updatedAt = queued.updatedAt + 1,
            )
        )

        val restored = dao.analysisTask()
        assertEquals(SimilarAnalysisTaskStatus.CANCEL_REQUESTED, restored?.status)
        assertEquals(true, restored?.cancelRequested)
        assertEquals(24, restored?.cursor)
        assertEquals("media-v1", restored?.mediaVersion)
        assertEquals("run-1", restored?.runToken)
    }

    @Test
    fun mediaSyncStatePersistsGenerationAndPermissionScope() = runBlocking {
        val state = MediaSyncStateEntity(
            syncKey = "external",
            volumeName = "external",
            mediaStoreVersion = "media-store-v1",
            lastGeneration = 42L,
            permissionScopeFingerprint = "images=true;videos=false;selected=true",
            updatedAt = 1_700_000_001_000L,
        )

        database.mediaSyncStateDao().upsert(state)

        assertEquals(state, database.mediaSyncStateDao().bySyncKey("external"))
    }

    @Test
    fun currentSchemaDoesNotCreateRemovedPlaceholderTables() {
        val cursor = database.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table'",
        )
        val tables = buildSet {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
        cursor.close()

        listOf(
            "lsh_buckets",
            "folder_filters",
            "folder_usage",
            "swipe_statistics",
            "swipe_stat_sessions",
            "backup_import_audit_logs",
            "device_model_cache",
        ).forEach { removedTable ->
            assertFalse("removed table must not be created: $removedTable", removedTable in tables)
        }
    }

    @Test
    fun todayInHistoryOnlyMatchesTheSameMonthAndDayFromAnotherYear() = runBlocking {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val previousYear = (today.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        val currentYearPhoto = photo(
            mediaStoreId = 201L,
            displayName = "current-year.jpg",
            dateTaken = today.timeInMillis,
        )
        val previousYearPhoto = photo(
            mediaStoreId = 202L,
            displayName = "previous-year.jpg",
            dateTaken = previousYear.timeInMillis,
        )
        database.photoDao().upsertAll(listOf(currentYearPhoto, previousYearPhoto))

        val memories = database.photoDao().observeTodayInHistory(
            now = today.timeInMillis,
            limit = 10,
        ).first()

        assertEquals(listOf("previous-year.jpg"), memories.map { it.displayName })
    }

    private fun photo(
        mediaStoreId: Long,
        displayName: String,
        dateTaken: Long = 1_700_000_000_000L,
        folderPath: String = "Pictures/Keepix",
    ) = PhotoEntity(
        mediaStoreId = mediaStoreId,
        uri = "content://media/external/images/media/" + mediaStoreId,
        displayName = displayName,
        size = 1024L,
        dateTaken = dateTaken,
        dateAdded = 1_700_000_000L,
        dateModified = 1_700_000_000L,
        folderPath = folderPath,
        folderName = folderPath.substringAfterLast('/'),
        mimeType = "image/jpeg",
        width = 1200,
        height = 900,
    )

    private fun explainPlan(sql: String): String {
        val cursor = database.openHelper.readableDatabase.query("EXPLAIN QUERY PLAN " + sql)
        return cursor.use { rows ->
            buildString {
                while (rows.moveToNext()) append(rows.getString(3)).append('\n')
            }
        }
    }
}
