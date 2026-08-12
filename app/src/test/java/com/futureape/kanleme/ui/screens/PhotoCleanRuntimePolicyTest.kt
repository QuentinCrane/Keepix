package com.futureape.kanleme.ui.screens

import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.data.settings.PhotoCleanMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoCleanRuntimePolicyTest {
    @Test
    fun normalModeFollowsImmersiveBackgroundSwitch() {
        assertTrue(AppSettings().toPhotoCleanRuntimePolicy().showImmersiveBackground)
        assertFalse(AppSettings(immersiveBackground = false).toPhotoCleanRuntimePolicy().showImmersiveBackground)
        assertEquals(3, AppSettings().toPhotoCleanRuntimePolicy().prefetchCount)
    }

    @Test
    fun diagnosticModeShowsOnlyDiagnosticsWithoutChangingQueuePolicy() {
        val policy = AppSettings(photoCleanMode = PhotoCleanMode.DIAGNOSTIC).toPhotoCleanRuntimePolicy()

        assertTrue(policy.showImmersiveBackground)
        assertTrue(policy.animateEntry)
        assertTrue(policy.showDiagnostics)
        assertEquals(3, policy.prefetchCount)
    }

    @Test
    fun performanceModeDisablesExpensiveVisualsAndShrinksPrefetch() {
        val policy = AppSettings(photoCleanMode = PhotoCleanMode.PERFORMANCE).toPhotoCleanRuntimePolicy()

        assertFalse(policy.showImmersiveBackground)
        assertFalse(policy.animateEntry)
        assertFalse(policy.showDiagnostics)
        assertEquals(1, policy.prefetchCount)
    }
}
