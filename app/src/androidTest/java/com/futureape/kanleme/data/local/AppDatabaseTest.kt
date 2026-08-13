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
import org.junit.Assert.assertNull
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
}
