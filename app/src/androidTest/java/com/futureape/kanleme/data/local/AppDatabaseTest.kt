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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        val photo = PhotoEntity(
            mediaStoreId = 42L,
            uri = "content://media/external/images/media/42",
            displayName = "sample.jpg",
            size = 1024L,
            dateTaken = 1_700_000_000_000L,
            dateAdded = 1_700_000_000L,
            dateModified = 1_700_000_000L,
            folderPath = "Pictures/Keepix",
            folderName = "Keepix",
            mimeType = "image/jpeg",
            width = 1200,
            height = 900,
        )

        database.photoDao().upsertAll(listOf(photo))
        val stored = database.photoDao().byMediaStoreIds(listOf(42L)).single()
        database.photoDao().updateProcessingStatus(stored.id, KEPT, now = 1_700_000_001_000L)

        assertEquals(1, database.photoDao().observeCount().first())
        assertEquals(KEPT, database.photoDao().byId(stored.id)?.processingStatus)
    }
}
