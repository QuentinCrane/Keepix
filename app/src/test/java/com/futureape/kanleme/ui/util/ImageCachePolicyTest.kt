package com.futureape.kanleme.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCachePolicyTest {
    @Test
    fun normalDeviceKeepsCurrentPrefetchBudgetUntilMeasured() {
        assertEquals(0.30, imageMemoryCachePercent(isLowRamDevice = false), 0.0)
        assertEquals(
            ImagePrefetchPolicy(48, 12, 54, 3),
            imagePrefetchPolicy(isLowRamDevice = false),
        )
    }

    @Test
    fun lowRamDeviceUsesSmallerCacheAndPrefetchBudget() {
        assertEquals(0.15, imageMemoryCachePercent(isLowRamDevice = true), 0.0)
        val policy = imagePrefetchPolicy(isLowRamDevice = true)

        assertTrue(policy.initialGridCount < 48)
        assertTrue(policy.visibleAfterCount < 54)
        assertEquals(1, policy.deckHighResolutionCount)
    }
}
