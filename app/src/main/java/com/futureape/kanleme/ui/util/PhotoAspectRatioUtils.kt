package com.futureape.kanleme.ui.util

import com.futureape.kanleme.data.local.PhotoEntity

fun photoDisplayAspectRatio(
    photo: PhotoEntity,
    minRatio: Float = 0.42f,
    maxRatio: Float = 2.20f,
): Float {
    val rawWidth = photo.exifWidth?.takeIf { it > 0 } ?: photo.width.takeIf { it > 0 } ?: 1
    val rawHeight = photo.exifHeight?.takeIf { it > 0 } ?: photo.height.takeIf { it > 0 } ?: 1
    val shouldSwap = shouldSwapPhotoDimensions(photo.exifOrientation) && rawWidth > rawHeight
    val displayWidth = if (shouldSwap) rawHeight else rawWidth
    val displayHeight = if (shouldSwap) rawWidth else rawHeight
    return (displayWidth.toFloat() / displayHeight.toFloat()).coerceIn(minRatio, maxRatio)
}

private fun shouldSwapPhotoDimensions(orientation: Int?): Boolean {
    return orientation == 90 ||
        orientation == 270 ||
        orientation in 5..8
}
