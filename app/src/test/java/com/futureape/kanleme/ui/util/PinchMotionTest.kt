package com.futureape.kanleme.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinchMotionTest {
    @Test fun tinyTwoFingerJitterDoesNotOwnTheGestureOrCommit() {
        val motion = PinchMotion(300f, 12f, -1f)
        motion.update(295f, 0)
        motion.update(302f, 16)
        assertFalse(motion.owned)
        assertFalse(motion.shouldCommit())
        assertEquals(0f, motion.progress, 0f)
    }

    @Test fun reverseAfterLockStaysOwnedAndReturnsContinuouslyToZero() {
        val motion = PinchMotion(300f, 12f, -1f)
        val expanded = motion.update(245f, 0)
        val reversed = motion.update(270f, 16)
        assertTrue(reversed < expanded)
        assertTrue(motion.owned)
        assertEquals(0f, motion.update(300f, 32), 0f)
        assertFalse(motion.shouldCommit())
    }

    @Test fun smallSlowPinchCancelsInsteadOfOpeningOnRelease() {
        val motion = PinchMotion(300f, 12f, -1f)
        motion.update(290f, 0)
        motion.update(280f, 300)
        assertTrue(motion.owned)
        assertFalse(motion.shouldCommit())
    }

    @Test fun clearPinchCommitsWithoutRequiringFullTravel() {
        val motion = PinchMotion(300f, 12f, -1f)
        motion.update(250f, 300)
        assertTrue(motion.shouldCommit())
    }

    @Test fun quickIntentionalPinchCanCommitButReverseFlickCannot() {
        val motion = PinchMotion(300f, 12f, -1f)
        motion.update(290f, 0)
        motion.update(278f, 16)
        assertTrue(motion.shouldCommit())
        motion.update(290f, 32)
        assertFalse(motion.shouldCommit())
    }

    @Test fun openingAndClosingUseTheSameDecisionInOppositeDirections() {
        val opening = PinchMotion(300f, 12f, -1f)
        val closing = PinchMotion(300f, 12f, 1f)
        assertEquals(opening.update(245f, 100), closing.update(355f, 100), 0.0001f)
        assertEquals(opening.shouldCommit(), closing.shouldCommit())
    }
}
