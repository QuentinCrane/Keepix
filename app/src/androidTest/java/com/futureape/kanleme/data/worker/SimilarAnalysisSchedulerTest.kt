package com.futureape.kanleme.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

        when (InstrumentationRegistry.getArguments().getString(PROCESS_RECOVERY_PHASE)) {
            PHASE_ENQUEUE -> {
                scheduler.cancel()
                scheduler.enqueue(SimilarAnalysisWorkMode.AUTO)
                val queued = workManager
                    .getWorkInfosForUniqueWork(SimilarAnalysisWorkContract.UNIQUE_WORK_NAME)
                    .get()
                    .single()
                assertEquals(WorkInfo.State.ENQUEUED, queued.state)
                return
            }
            PHASE_VERIFY -> {
                val recovered = workManager
                    .getWorkInfosForUniqueWork(SimilarAnalysisWorkContract.UNIQUE_WORK_NAME)
                    .get()
                    .single()
                assertTrue(
                    "Persisted work was not recoverable: " + recovered.state,
                    recovered.state == WorkInfo.State.ENQUEUED || recovered.state == WorkInfo.State.RUNNING,
                )
                scheduler.cancel()
                return
            }
        }

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

    private companion object {
        const val PROCESS_RECOVERY_PHASE = "kanleme.processRecoveryPhase"
        const val PHASE_ENQUEUE = "enqueue"
        const val PHASE_VERIFY = "verify"
    }
}
