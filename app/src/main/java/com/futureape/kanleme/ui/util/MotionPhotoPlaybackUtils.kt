package com.futureape.kanleme.ui.util

import android.content.Context
import android.net.Uri
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.media.DynamicPhotoDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

sealed class MotionPhotoPlaybackSource {
    data class Ready(val uri: Uri, val label: String) : MotionPhotoPlaybackSource()
    data class Unavailable(val reason: String) : MotionPhotoPlaybackSource()
}

suspend fun resolveMotionPhotoPlaybackSource(
    context: Context,
    photo: PhotoEntity,
): MotionPhotoPlaybackSource = withContext(Dispatchers.IO) {
    if (photo.isGif) {
        return@withContext MotionPhotoPlaybackSource.Unavailable("GIF 已由图片组件直接播放")
    }
    val shouldTryResolve = photo.isMotionPhoto ||
        photo.motionPhotoNeedsDetection ||
        photo.isSeparateVideo ||
        !photo.motionVideoUri.isNullOrBlank() ||
        photo.motionVideoOffset > 0L ||
        photo.motionVideoSize > 0L
    if (!shouldTryResolve) {
        return@withContext MotionPhotoPlaybackSource.Unavailable("这不是可播放的实况照片")
    }

    trimMotionPreviewCache(context)

    val companionUri = photo.motionVideoUri
        ?.takeIf { photo.isSeparateVideo && it.isNotBlank() }
        ?.let { runCatching { Uri.parse(it) }.getOrNull() }
    if (companionUri != null) {
        return@withContext MotionPhotoPlaybackSource.Ready(companionUri, "实况视频段")
    }

    val cached = embeddedMotionCacheFile(context, photo)
    if (cached.exists() && cached.length() > MIN_VIDEO_BYTES) {
        return@withContext MotionPhotoPlaybackSource.Ready(Uri.fromFile(cached), "实况片段")
    }

    val extracted = withTimeoutOrNull(4200L) { extractEmbeddedMotionVideo(context, photo, cached) }
    if (extracted != null && extracted.exists() && extracted.length() > MIN_VIDEO_BYTES) {
        MotionPhotoPlaybackSource.Ready(Uri.fromFile(extracted), "实况片段")
    } else {
        MotionPhotoPlaybackSource.Unavailable(
            "这张照片被识别为实况，但当前文件没有可直接提取的标准 MP4 片段。部分厂商会把动态数据放在私有容器里，只能由系统相册播放。"
        )
    }
}

private const val MIN_VIDEO_BYTES = 16 * 1024L

private fun embeddedMotionCacheFile(context: Context, photo: PhotoEntity): File {
    val dir = File(context.cacheDir, "motion_photo_preview").apply { mkdirs() }
    val identity = listOf(photo.mediaStoreId, photo.dateModified, photo.motionVideoOffset, photo.motionVideoSize)
        .joinToString(separator = "_")
    return File(dir, identity + ".mp4")
}

private fun extractEmbeddedMotionVideo(
    context: Context,
    photo: PhotoEntity,
    target: File,
): File? {
    val sourceUri = runCatching { Uri.parse(photo.uri) }.getOrNull() ?: return null
    val resolver = context.contentResolver
    val size = photo.size.takeIf { it > 0L } ?: return null

    val exactOffset = photo.motionVideoOffset.takeIf { it > 0L && it < size }
    val exactSize = photo.motionVideoSize.takeIf { it > MIN_VIDEO_BYTES && exactOffset != null && exactOffset + it <= size }
    if (exactOffset != null) {
        val copied = copyRangeFromUri(
            context = context,
            sourceUri = sourceUri,
            start = exactOffset,
            byteCount = exactSize ?: (size - exactOffset),
            target = target,
        )
        if (copied != null && copied.length() > MIN_VIDEO_BYTES) return copied
    }

    // Motion Photo 的视频段通常拼接在文件尾部。只读尾部，避免大图全量读入内存。
    val searchBytes = size.coerceAtMost(36L * 1024L * 1024L).toInt()
    val tailStart = (size - searchBytes).coerceAtLeast(0L)
    val tail = resolver.openInputStream(sourceUri)?.use { input ->
        skipFully(input, tailStart)
        input.readBytesCapped(searchBytes)
    } ?: return null

    val localStart = DynamicPhotoDetector.findEmbeddedMp4Start(tail) ?: findEmbeddedMp4Start(tail) ?: return null
    val globalStart = tailStart + localStart
    if (globalStart <= 0L || globalStart >= size) return null

    return copyRangeFromUri(
        context = context,
        sourceUri = sourceUri,
        start = globalStart,
        byteCount = size - globalStart,
        target = target,
    )
}

private fun copyRangeFromUri(
    context: Context,
    sourceUri: Uri,
    start: Long,
    byteCount: Long,
    target: File,
): File? = runCatching {
    target.parentFile?.mkdirs()
    val temp = File(target.parentFile, target.name + ".tmp")
    if (temp.exists() && !temp.delete()) return@runCatching null
    val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return@runCatching null
    inputStream.use { input ->
        skipFully(input, start)
        FileOutputStream(temp).use { output ->
            val buffer = ByteArray(64 * 1024)
            var remaining = byteCount
            while (remaining > 0L) {
                val requested = minOf(buffer.size.toLong(), remaining).toInt()
                val read = input.read(buffer, 0, requested)
                if (read <= 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
        }
    }
    if (temp.length() <= MIN_VIDEO_BYTES) {
        temp.delete()
        null
    } else {
        if (target.exists() && !target.delete()) {
            temp.delete()
            null
        } else if (temp.renameTo(target)) {
            target
        } else {
            temp.delete()
            null
        }
    }
}.getOrNull()

private fun findEmbeddedMp4Start(bytes: ByteArray): Int? {
    if (bytes.size < 16) return null
    var i = 4
    var best: Int? = null
    while (i < bytes.size - 12) {
        if (bytes[i] == 'f'.code.toByte() && bytes[i + 1] == 't'.code.toByte() && bytes[i + 2] == 'y'.code.toByte() && bytes[i + 3] == 'p'.code.toByte()) {
            val boxStart = i - 4
            val boxSize = readUInt32(bytes, boxStart)
            val brand = String(bytes, i + 4, 4).lowercase(Locale.ROOT)
            val validSize = boxSize in 8..4096
            val likelyMp4Brand = brand.startsWith("mp4") ||
                brand.startsWith("isom") ||
                brand.startsWith("iso") ||
                brand.startsWith("3gp") ||
                brand.startsWith("m4v") ||
                brand.startsWith("qt") ||
                brand.startsWith("msnv")
            val isStillImageContainer = brand.startsWith("heic") ||
                brand.startsWith("heix") ||
                brand.startsWith("hevc") ||
                brand.startsWith("mif1") ||
                brand.startsWith("msf1") ||
                brand.startsWith("avif")
            if (validSize && likelyMp4Brand && !isStillImageContainer && hasNearbyMovieBox(bytes, boxStart)) {
                // 取最后一个合法 MP4 起点，避开前面 XMP 文本或图片容器里的干扰。
                best = boxStart
            }
        }
        i++
    }
    return best
}

private fun hasNearbyMovieBox(bytes: ByteArray, start: Int): Boolean {
    val end = (start + 2 * 1024 * 1024).coerceAtMost(bytes.size - 4)
    var i = start + 8
    while (i < end) {
        val c0 = bytes[i]
        if ((c0 == 'm'.code.toByte() && bytes[i + 1] == 'o'.code.toByte() && bytes[i + 2] == 'o'.code.toByte() && bytes[i + 3] == 'v'.code.toByte()) ||
            (c0 == 'm'.code.toByte() && bytes[i + 1] == 'd'.code.toByte() && bytes[i + 2] == 'a'.code.toByte() && bytes[i + 3] == 't'.code.toByte())
        ) {
            return true
        }
        i++
    }
    return false
}

private fun readUInt32(bytes: ByteArray, index: Int): Int {
    if (index < 0 || index + 3 >= bytes.size) return -1
    return ((bytes[index].toInt() and 0xFF) shl 24) or
        ((bytes[index + 1].toInt() and 0xFF) shl 16) or
        ((bytes[index + 2].toInt() and 0xFF) shl 8) or
        (bytes[index + 3].toInt() and 0xFF)
}

private fun skipFully(input: java.io.InputStream, target: Long) {
    var skipped = 0L
    while (skipped < target) {
        val delta = input.skip(target - skipped)
        if (delta <= 0L) {
            if (input.read() == -1) break else skipped++
        } else {
            skipped += delta
        }
    }
}

private fun java.io.InputStream.readBytesCapped(maxBytes: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream(maxBytes)
    val buffer = ByteArray(64 * 1024)
    var remaining = maxBytes
    while (remaining > 0) {
        val read = read(buffer, 0, buffer.size.coerceAtMost(remaining))
        if (read <= 0) break
        out.write(buffer, 0, read)
        remaining -= read
    }
    return out.toByteArray()
}

private fun trimMotionPreviewCache(context: Context) {
    val dir = File(context.cacheDir, "motion_photo_preview")
    val files = dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: return
    var total = files.sumOf { it.length() }
    files.asReversed().forEach { file ->
        if (total <= MOTION_PREVIEW_CACHE_MAX_BYTES) return
        total -= file.length()
        file.delete()
    }
}
