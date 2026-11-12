package com.futureape.kanleme.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.os.CancellationSignal
import android.util.LruCache
import android.util.Size
import androidx.core.net.toUri
import androidx.core.graphics.scale
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.inspector.frame.FrameExtractor
import com.futureape.kanleme.data.media.AdvancedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Visible-row covers use the MediaStore cache first, then decode a bounded video frame. */
object AdvancedVideoThumbnails {
    private val permits = Semaphore(2)
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val failures = LruCache<String, Long>(64)
    private val cacheLock = Any()
    @Volatile private var generation = 0L

    fun key(video: AdvancedVideo): String = video.uri + ":" + video.sizeBytes + ":" + video.dateModifiedMs + ":256x160"

    suspend fun load(
        context: Context,
        video: AdvancedVideo,
        retryTransientFailure: Boolean = false,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val key = key(video)
        cache.get(key)?.let { return@withContext it }
        if (retryTransientFailure) failures.remove(key)
        else if ((failures.get(key) ?: 0L) > System.currentTimeMillis() - 30_000L) return@withContext null
        permits.withPermit {
            cache.get(key)?.let { return@withPermit it }
            val requestGeneration = generation
            suspendCancellableCoroutine<Bitmap?> { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                try {
                    val uri = video.uri.toUri()
                    val mediaStoreThumbnail = runCatching {
                        context.contentResolver.loadThumbnail(uri, Size(256, 160), signal)
                    }.getOrNull()
                    if (!continuation.isActive) {
                        mediaStoreThumbnail?.recycle()
                        return@suspendCancellableCoroutine
                    }
                    val source = mediaStoreThumbnail
                        ?: runCatching { loadMedia3Frame(context, uri) }.getOrNull()
                        ?: loadVideoFrame(context, uri)
                    if (source == null) {
                        synchronized(cacheLock) { if (requestGeneration == generation) failures.put(key, System.currentTimeMillis()) }
                        if (continuation.isActive) continuation.resume(null)
                        return@suspendCancellableCoroutine
                    }
                    if (!continuation.isActive) {
                        source.recycle()
                        return@suspendCancellableCoroutine
                    }
                    val scale = minOf(256f / source.width, 160f / source.height, 1f)
                    val bitmap = if (scale < 1f) source.scale(
                        (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
                        .also { if (it !== source) source.recycle() } else source
                    if (continuation.isActive) {
                        synchronized(cacheLock) { if (requestGeneration == generation) cache.put(key, bitmap) }
                        continuation.resume(bitmap)
                    } else {
                        bitmap.recycle()
                    }
                } catch (_: Exception) {
                    if (continuation.isActive) {
                        synchronized(cacheLock) { if (requestGeneration == generation) failures.put(key, System.currentTimeMillis()) }
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun loadMedia3Frame(context: Context, uri: android.net.Uri): Bitmap? =
        FrameExtractor.Builder(context, MediaItem.fromUri(uri))
            .setEffects(listOf(Presentation.createForHeight(160)))
            .build()
            .use { extractor -> extractor.getThumbnail().get().bitmap }

    private fun loadVideoFrame(context: Context, uri: android.net.Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val representative = retriever.getScaledFrameAtTime(
                -1L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                256,
                160,
            )
            if (representative != null) representative else {
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.coerceIn(0L, Long.MAX_VALUE / 1_000L) ?: 0L
                val durationUs = durationMs * 1_000L
                val middleUs = durationUs / 2L
                retriever.getScaledFrameAtTime(
                    middleUs,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                    256,
                    160,
                ) ?: retriever.getScaledFrameAtTime(
                    0L,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                    256,
                    160,
                )
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun clear() { synchronized(cacheLock) { generation++; cache.evictAll(); failures.evictAll() } }
    internal fun cachedBytes(): Int = cache.size()
}
