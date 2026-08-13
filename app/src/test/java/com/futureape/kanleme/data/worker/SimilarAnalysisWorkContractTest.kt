package com.futureape.kanleme.data.worker

import androidx.work.ExistingWorkPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarAnalysisWorkContractTest {
    @Test
    fun automaticWorkIsIdleOnlyAndKeepsOneExistingRequest() {
        val spec = SimilarAnalysisWorkContract.specFor(SimilarAnalysisWorkMode.AUTO)

        assertTrue(spec.requiresDeviceIdle)
        assertEquals(ExistingWorkPolicy.KEEP, spec.existingWorkPolicy)
        assertEquals(SimilarAnalysisWorkContract.UNIQUE_WORK_NAME, "kanleme-similar-photo-analysis")
    }

    @Test
    fun manualWorkCanReplaceAWaitingAutomaticRequestWithoutConcurrency() {
        val spec = SimilarAnalysisWorkContract.specFor(SimilarAnalysisWorkMode.MANUAL)

        assertFalse(spec.requiresDeviceIdle)
        assertEquals(ExistingWorkPolicy.REPLACE, spec.existingWorkPolicy)
        assertEquals(30L, spec.backoffSeconds)
    }
}
