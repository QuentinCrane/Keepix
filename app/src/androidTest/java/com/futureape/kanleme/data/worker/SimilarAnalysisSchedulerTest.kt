package com.futureape.kanleme.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkInfo
import androidx.work.WorkManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimilarAnalysisSchedulerTest {
    @Test
    fun automaticWorkIsPersistedAndCanBeCancelled() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workManager = WorkManager.getInstance(context)
        val scheduler = SimilarAnalysisScheduler(context)

        scheduler.cancel()
        scheduler.enqueue(SimilarAnalysisWorkMode.AUTO)

        val work = workManager
            .getWorkInfosForUniqueWork(SimilarAnalysisWorkContract.UNIQUE_WORK_NAME)
            .get()
            .single()

        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertTrue(work.tags.contains(SimilarAnalysisWorkContract.UNIQUE_WORK_NAME))
        scheduler.cancel()
        val cancelled = workManager
            .getWorkInfosForUniqueWork(SimilarAnalysisWorkContract.UNIQUE_WORK_NAME)
            .get()
            .single()
        assertEquals(WorkInfo.State.CANCELLED, cancelled.state)
    }
}
