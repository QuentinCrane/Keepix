package com.futureape.kanleme.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.os.CancellationSignal
import android.util.LruCache
import android.util.Size
import androidx.core.net.toUri
import androidx.core.graphics.scale
import com.futureape.kanleme.data.media.AdvancedVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Visible-row covers only, using the platform's MediaStore thumbnail cache. */
object AdvancedVideoThumbnails {
    private val permits = Semaphore(2)
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val failures = LruCache<String, Long>(64)
    private val cacheLock = Any()
    @Volatile private var generation = 0L

    fun key(video: AdvancedVideo): String = video.uri + ":" + video.sizeBytes + ":" + video.dateModifiedMs + ":256x160"

    suspend fun load(context: Context, video: AdvancedVideo): Bitmap? = withContext(Dispatchers.IO) {
        val key = key(video)
        cache.get(key)?.let { return@withContext it }
        if ((failures.get(key) ?: 0L) > System.currentTimeMillis() - 30_000L) return@withContext null
        permits.withPermit {
            cache.get(key)?.let { return@withPermit it }
            val requestGeneration = generation
            suspendCancellableCoroutine<Bitmap?> { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                try {
                    val source = context.contentResolver.loadThumbnail(video.uri.toUri(), Size(256, 160), signal)
                    val scale = minOf(256f / source.width, 160f / source.height, 1f)
                    val bitmap = if (scale < 1f) source.scale(
                        (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
                        .also { if (it !== source) source.recycle() } else source
                    if (continuation.isActive) {
                        synchronized(cacheLock) { if (requestGeneration == generation) cache.put(key, bitmap) }
                        continuation.resume(bitmap)
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

    fun clear() { synchronized(cacheLock) { generation++; cache.evictAll(); failures.evictAll() } }
    internal fun cachedBytes(): Int = cache.size()
}
