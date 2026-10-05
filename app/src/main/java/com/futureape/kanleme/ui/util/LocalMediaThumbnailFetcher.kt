package com.futureape.kanleme.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okio.buffer
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

/** The cached data is a bounded derivative, never a copy of the original media file. */
data class LocalMediaThumbnail(val uri: Uri, val versionKey: String, val width: Int = 420, val height: Int = 560) {
    init { require(width in 1..1024 && height in 1..1024) }
    val diskKey: String get() = "local-thumbnail-v1:" + versionKey + ":" + width + "x" + height
}

class LocalMediaThumbnailKeyer : Keyer<LocalMediaThumbnail> {
    override fun key(data: LocalMediaThumbnail, options: Options) = data.diskKey
}

/** Separate from Coil's DISK counter, which also counts reads of original content URIs. */
internal object LocalThumbnailMetrics {
    val diskHits = AtomicLong()
    val generated = AtomicLong()
}

class LocalMediaThumbnailFetcher private constructor(
    private val data: LocalMediaThumbnail,
    private val options: Options,
    private val cache: DiskCache?,
    private val load: (Context, LocalMediaThumbnail) -> Bitmap,
    private val validate: (Context, LocalMediaThumbnail) -> Unit,
) : Fetcher {
    override suspend fun fetch() = withContext(Dispatchers.IO) {
        val lock = locks[(data.diskKey.hashCode() and Int.MAX_VALUE) % locks.size]
        lock.withLock {
            permits.withPermit {
                val cached = if (options.diskCachePolicy.readEnabled) readCached() else null
                val bitmap = cached ?: bounded(load(options.context, data)).also { LocalThumbnailMetrics.generated.incrementAndGet() }
                try {
                    currentCoroutineContext().ensureActive()
                    if (cached == null && options.diskCachePolicy.writeEnabled) writeCached(bitmap)
                    currentCoroutineContext().ensureActive()
                    bitmap.prepareToDraw()
                    // The thumbnail is the complete image represented by this model's fixed size.
                    ImageFetchResult(bitmap.asImage(), isSampled = false, dataSource = DataSource.DISK)
                } catch (cancelled: CancellationException) {
                    // This bitmap has not been handed to Coil or the UI yet.
                    bitmap.recycle()
                    throw cancelled
                }
            }
        }
    }

    private fun bounded(bitmap: Bitmap): Bitmap {
        val scale = minOf(data.width.toFloat() / bitmap.width, data.height.toFloat() / bitmap.height, 1f)
        if (scale >= 1f) return bitmap
        val resized = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        if (resized !== bitmap) bitmap.recycle()
        return resized
    }

    private fun readCached(): Bitmap? {
        val diskCache = cache ?: return null
        val snapshot = try { diskCache.openSnapshot(data.diskKey) } catch (_: IOException) { null }
            ?: return null
        val bitmap = snapshot.use {
            // A persistent cache must not turn a revoked/deleted source into readable media.
            validate(options.context, data)
            val path = it.data.toString()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth !in 1..data.width || bounds.outHeight !in 1..data.height) null
            else BitmapFactory.decodeFile(path)
        }
        if (bitmap == null) diskCache.remove(data.diskKey)
        else LocalThumbnailMetrics.diskHits.incrementAndGet()
        return bitmap
    }

    private fun writeCached(bitmap: Bitmap) {
        val diskCache = cache ?: return
        val editor = try { diskCache.openEditor(data.diskKey) } catch (_: IOException) { null } ?: return
        var committed = false
        var software: Bitmap? = null
        try {
            val encodable = if (bitmap.config == Bitmap.Config.HARDWARE)
                bitmap.copy(Bitmap.Config.ARGB_8888, false).also { software = it } else bitmap
            if (encodable == null) return
            diskCache.fileSystem.write(editor.metadata) { writeUtf8("local-thumbnail-v1") }
            diskCache.fileSystem.sink(editor.data).buffer().outputStream().use { output ->
                val format = if (encodable.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                if (!encodable.compress(format, 86, output)) throw IOException("Thumbnail encoding failed")
            }
            editor.commit()
            committed = true
        } catch (_: IOException) {
            // A full/unavailable cache must not prevent viewing the source thumbnail.
        } finally {
            software?.recycle()
            if (!committed) runCatching { editor.abort() }
        }
    }

    class Factory(
        private val load: (Context, LocalMediaThumbnail) -> Bitmap = { context, data ->
            context.contentResolver.loadThumbnail(data.uri, Size(data.width, data.height), null)
        },
        private val validate: (Context, LocalMediaThumbnail) -> Unit = { context, data ->
            context.contentResolver.openAssetFileDescriptor(data.uri, "r")?.use { }
                ?: throw FileNotFoundException("Source media is unavailable")
        },
    ) : Fetcher.Factory<LocalMediaThumbnail> {
        override fun create(data: LocalMediaThumbnail, options: Options, imageLoader: ImageLoader): Fetcher =
            LocalMediaThumbnailFetcher(data, options, imageLoader.diskCache, load, validate)
    }

    companion object {
        private val permits = Semaphore(2)
        // A fixed number of locks coalesces same-key cache misses without an unbounded key map.
        private val locks = Array(16) { Mutex() }
    }
}
