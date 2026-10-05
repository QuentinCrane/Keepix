package com.futureape.kanleme.ui.util

import android.util.Log
import coil3.EventListener
import coil3.Image
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.size.Size
import java.util.concurrent.atomic.AtomicLong

/** Process-local, media-free image loading counters for debug diagnostics. */
internal object ImageLoadMetrics {
    private const val TAG = "ImageLoadMetrics"
    private const val LOG_EVERY_COMPLETION = 50L

    private val started = AtomicLong()
    private val memoryHits = AtomicLong()
    private val diskHits = AtomicLong()
    private val decoded = AtomicLong()
    private val cancelled = AtomicLong()
    private val errors = AtomicLong()
    private val requestedPixels = AtomicLong()
    private val decodedPixels = AtomicLong()

    val eventListenerFactory: EventListener.Factory = EventListener.Factory { MetricsEventListener }

    internal fun snapshot(): ImageLoadSnapshot = ImageLoadSnapshot(
        started = started.get(),
        memoryHits = memoryHits.get(),
        diskHits = diskHits.get(),
        decoded = decoded.get(),
        cancelled = cancelled.get(),
        errors = errors.get(),
        requestedPixels = requestedPixels.get(),
        decodedPixels = decodedPixels.get(),
        thumbnailDiskHits = LocalThumbnailMetrics.diskHits.get(),
        thumbnailGenerations = LocalThumbnailMetrics.generated.get(),
    )

    private fun maybeLog() {
        val completed = memoryHits.get() + diskHits.get() + decoded.get() + cancelled.get() + errors.get()
        if (completed > 0 && completed % LOG_EVERY_COMPLETION == 0L) {
            Log.d(TAG, snapshot().toString())
        }
    }

    private fun recordSize(size: Size) {
        val width = size.width.pixelsOrZero()
        val height = size.height.pixelsOrZero()
        if (width > 0 && height > 0) {
            requestedPixels.addAndGet(width.toLong() * height.toLong())
        }
    }

    private fun recordDecodedSize(image: Image) {
        val width = image.width
        val height = image.height
        if (width > 0 && height > 0) {
            decodedPixels.addAndGet(width.toLong() * height.toLong())
        }
    }

    private object MetricsEventListener : EventListener() {
        override fun onStart(request: ImageRequest) {
            started.incrementAndGet()
        }

        override fun resolveSizeEnd(request: ImageRequest, size: Size) {
            recordSize(size)
        }

        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
            when (result.dataSource) {
                DataSource.MEMORY_CACHE -> memoryHits.incrementAndGet()
                DataSource.DISK -> {
                    diskHits.incrementAndGet()
                    decoded.incrementAndGet()
                    recordDecodedSize(result.image)
                }
                else -> {
                    decoded.incrementAndGet()
                    recordDecodedSize(result.image)
                }
            }
            maybeLog()
        }

        override fun onCancel(request: ImageRequest) {
            cancelled.incrementAndGet()
            maybeLog()
        }

        override fun onError(request: ImageRequest, result: ErrorResult) {
            errors.incrementAndGet()
            maybeLog()
        }
    }
}

internal data class ImageLoadSnapshot(
    val started: Long,
    val memoryHits: Long,
    val diskHits: Long,
    val decoded: Long,
    val cancelled: Long,
    val errors: Long,
    val requestedPixels: Long,
    val decodedPixels: Long,
    val thumbnailDiskHits: Long,
    val thumbnailGenerations: Long,
)

private fun Dimension.pixelsOrZero(): Int = (this as? Dimension.Pixels)?.px ?: 0
