package com.futureape.kanleme.ui.util

import android.app.ActivityManager
import android.content.Context

private const val NORMAL_MEMORY_CACHE_PERCENT = 0.30
private const val LOW_RAM_MEMORY_CACHE_PERCENT = 0.15
const val NORMAL_IMAGE_MEMORY_MAX_BYTES = 96L * 1024L * 1024L
const val LOW_RAM_IMAGE_MEMORY_MAX_BYTES = 24L * 1024L * 1024L

fun imageMemoryCacheMaxBytes(heapBytes: Long, isLowRamDevice: Boolean): Long =
    (heapBytes * imageMemoryCachePercent(isLowRamDevice)).toLong().coerceAtLeast(1L)
        .coerceAtMost(if (isLowRamDevice) LOW_RAM_IMAGE_MEMORY_MAX_BYTES else NORMAL_IMAGE_MEMORY_MAX_BYTES)

/**
 * Device-aware bounds for image work. Normal devices retain the current
 * behavior until a real-device benchmark justifies a smaller window.
 */
data class ImagePrefetchPolicy(
    val initialGridCount: Int,
    val visibleBeforeCount: Int,
    val visibleAfterCount: Int,
    val deckHighResolutionCount: Int,
)

fun imageMemoryCachePercent(isLowRamDevice: Boolean): Double =
    if (isLowRamDevice) LOW_RAM_MEMORY_CACHE_PERCENT else NORMAL_MEMORY_CACHE_PERCENT

fun imagePrefetchPolicy(isLowRamDevice: Boolean): ImagePrefetchPolicy = if (isLowRamDevice) {
    ImagePrefetchPolicy(
        initialGridCount = 18,
        visibleBeforeCount = 6,
        visibleAfterCount = 18,
        deckHighResolutionCount = 1,
    )
} else {
    ImagePrefetchPolicy(
        initialGridCount = 48,
        visibleBeforeCount = 12,
        visibleAfterCount = 54,
        deckHighResolutionCount = 3,
    )
}

fun Context.isLowRamDevice(): Boolean =
    (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isLowRamDevice == true
