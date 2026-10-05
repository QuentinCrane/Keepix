package com.futureape.kanleme.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCachePolicyTest {
    @Test
    fun largeHeapsStillRespectAbsoluteCacheLimits() {
        val largeHeap = 2L * 1024 * 1024 * 1024
        assertEquals(96L * 1024 * 1024, imageMemoryCacheMaxBytes(largeHeap, false))
        assertEquals(24L * 1024 * 1024, imageMemoryCacheMaxBytes(largeHeap, true))
    }

    @Test
    fun smallHeapsKeepTheDeviceAwareFractionRatherThanAllocatingTheFullLimit() {
        val heap = 64L * 1024 * 1024
        val normal = imageMemoryCacheMaxBytes(heap, false)
        val lowRam = imageMemoryCacheMaxBytes(heap, true)
        assertTrue(normal < 24L * 1024 * 1024)
        assertTrue(lowRam < normal)
    }

    @Test
    fun prefetchedAndDisplayedPhotosShareAKeyWhileAnimatedBackgroundsStaySeparate() {
        assertEquals(normalizedPhotoCacheSuffix("photo_fit"), normalizedPhotoCacheSuffix("clean_prefetch"))
        assertEquals(normalizedPhotoCacheSuffix("photo_fit"), normalizedPhotoCacheSuffix("clean_background"))
        assertEquals("gif_background", normalizedPhotoCacheSuffix("clean_background", isGif = true))
        assertEquals(normalizedPhotoCacheSuffix("viewer_photo"), normalizedPhotoCacheSuffix("favorite_preview"))
    }
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
