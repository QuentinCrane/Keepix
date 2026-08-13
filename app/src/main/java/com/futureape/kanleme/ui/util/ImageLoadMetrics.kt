package com.futureape.kanleme.ui.util

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import coil.EventListener
import coil.decode.DataSource
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Dimension
import coil.size.Size
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

    private fun recordDecodedSize(drawable: android.graphics.drawable.Drawable) {
        val bitmap = (drawable as? BitmapDrawable)?.bitmap
        val width = bitmap?.width ?: drawable.intrinsicWidth
        val height = bitmap?.height ?: drawable.intrinsicHeight
        if (width > 0 && height > 0) {
            decodedPixels.addAndGet(width.toLong() * height.toLong())
        }
    }

    private object MetricsEventListener : EventListener {
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
                    recordDecodedSize(result.drawable)
                }
                else -> {
                    decoded.incrementAndGet()
                    recordDecodedSize(result.drawable)
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
)

private fun Dimension.pixelsOrZero(): Int = (this as? Dimension.Pixels)?.px ?: 0
