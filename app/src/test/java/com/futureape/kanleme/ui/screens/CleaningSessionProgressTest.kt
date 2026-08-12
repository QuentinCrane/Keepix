package com.futureape.kanleme.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleaningSessionProgressTest {
    @Test
    fun normalTargetUsesSessionActionsAndDoesNotUseDeckWindow() {
        val progress = cleaningSessionProgress(
            completedActions = 12,
            requestedTarget = 30,
            availableRemaining = 88,
        )

        assertEquals(30, progress.target)
        assertEquals(18, progress.remaining)
        assertEquals(40, progress.percent)
    }

    @Test
    fun shortQueueBecomesEffectiveTarget() {
        val beforeActions = cleaningSessionProgress(
            completedActions = 0,
            requestedTarget = 30,
            availableRemaining = 5,
        )
        val afterActions = cleaningSessionProgress(
            completedActions = 5,
            requestedTarget = 30,
            availableRemaining = 0,
        )

        assertEquals(5, beforeActions.target)
        assertEquals(5, beforeActions.remaining)
        assertEquals(5, afterActions.target)
        assertEquals(0, afterActions.remaining)
        assertEquals(100, afterActions.percent)
    }

    @Test
    fun invalidAndOverTargetValuesStayBounded() {
        val negative = cleaningSessionProgress(
            completedActions = -3,
            requestedTarget = 0,
            availableRemaining = -4,
        )
        val overTarget = cleaningSessionProgress(
            completedActions = 99,
            requestedTarget = 20,
            availableRemaining = 0,
        )

        assertEquals(1, negative.target)
        assertEquals(0, negative.completed)
        assertEquals(1, negative.remaining)
        assertEquals(0, negative.percent)
        assertEquals(20, overTarget.target)
        assertEquals(20, overTarget.completed)
        assertEquals(0, overTarget.remaining)
        assertEquals(100, overTarget.percent)
        assertTrue(negative.percent in 0..100)
        assertTrue(overTarget.percent in 0..100)
    }
}
