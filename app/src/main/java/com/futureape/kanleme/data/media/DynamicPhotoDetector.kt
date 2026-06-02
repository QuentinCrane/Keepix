package com.futureape.kanleme.data.media

import android.content.ContentResolver
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Best-effort dynamic photo detector for Android OEM formats.
 *
 * The old HuiLiu APK already stored motion_video_offset / motion_video_size and
 * had separate/embedded autoplay switches. Keepix keeps the same data direction:
 * scan fast metadata first, then use exact offset when XMP exposes it, otherwise
 * fall back to tail MP4 probing at playback time.
 */
object DynamicPhotoDetector {
    data class MotionInfo(
        val isMotionPhoto: Boolean,
        val motionVideoOffset: Long = 0L,
        val motionVideoSize: Long = 0L,
        val needsDetection: Boolean = false,
        val hint: String = "",
    )

    private val dynamicNameKeywords = listOf(
        "motion", "motionphoto", "moving", "movingphoto", "live", "livephoto",
        "dynamic", "dynamicphoto", "实况", "實況", "动态", "動態", "动图", "動圖",
        "小米实况", "华为动态", "荣耀动态", "oppo实况", "vivo动态"
    )
    private val embeddedMotionKeywords = listOf(
        "GCamera:MotionPhoto", "Camera:MotionPhoto", "MotionPhoto", "MicroVideo",
        "MotionPhoto_Data", "LivePhoto", "MovingPhoto", "DynamicPhoto",
        "xmpNote:HasExtendedXMP", "com.android.camera.motion", "com.google.android.apps.photos",
        "MiCamera", "HUAWEI", "HONOR", "OPPO", "vivo", "OnePlus", "MPF"
    )
    private val mp4TailMarkers = listOf("ftypmp4", "ftypisom", "ftypMSNV", "ftyp3gp", "mdat", "moov")

    fun isGif(mimeType: String?, displayName: String): Boolean {
        val mime = mimeType.orEmpty().lowercase(Locale.ROOT)
        val name = displayName.lowercase(Locale.ROOT)
        return mime == "image/gif" || name.endsWith(".gif")
    }


    fun hasMotionNameHint(displayName: String, relativePath: String?): Boolean {
        val name = displayName.lowercase(Locale.ROOT)
        val nameAndPath = (relativePath.orEmpty() + "/" + displayName).lowercase(Locale.ROOT)
        return name.startsWith("mvimg") || dynamicNameKeywords.any { nameAndPath.contains(it.lowercase(Locale.ROOT)) }
    }

    fun isPossiblyAnimatedStill(mimeType: String?, displayName: String): Boolean {
        val mime = mimeType.orEmpty().lowercase(Locale.ROOT)
        val name = displayName.lowercase(Locale.ROOT)
        return isGif(mimeType, displayName) ||
            mime == "image/webp" ||
            mime == "image/heic" ||
            mime == "image/heif" ||
            name.endsWith(".webp") ||
            name.endsWith(".heic") ||
            name.endsWith(".heif")
    }

    fun detectMotionPhoto(
        resolver: ContentResolver,
        uri: Uri,
        displayName: String,
        relativePath: String?,
        mimeType: String?,
        size: Long,
    ): Boolean = detectMotionInfo(resolver, uri, displayName, relativePath, mimeType, size).isMotionPhoto

    fun detectMotionInfo(
        resolver: ContentResolver,
        uri: Uri,
        displayName: String,
        relativePath: String?,
        mimeType: String?,
        size: Long,
    ): MotionInfo {
        if (isGif(mimeType, displayName)) return MotionInfo(isMotionPhoto = false, needsDetection = false)

        val name = displayName.lowercase(Locale.ROOT)
        val nameAndPath = (relativePath.orEmpty() + "/" + displayName).lowercase(Locale.ROOT)
        val nameHintsMotion = name.startsWith("mvimg") || dynamicNameKeywords.any { nameAndPath.contains(it.lowercase(Locale.ROOT)) }
        val mime = mimeType.orEmpty().lowercase(Locale.ROOT)
        val likelyContainer = mime in setOf("image/jpeg", "image/jpg", "image/heic", "image/heif", "image/webp") ||
            displayName.endsWith(".jpg", true) || displayName.endsWith(".jpeg", true) ||
            displayName.endsWith(".heic", true) || displayName.endsWith(".heif", true) || displayName.endsWith(".webp", true)

        if (!likelyContainer || size <= 0L || size > 260L * 1024L * 1024L) {
            return MotionInfo(isMotionPhoto = nameHintsMotion, needsDetection = nameHintsMotion, hint = if (nameHintsMotion) "name" else "")
        }

        val shouldSniff = nameHintsMotion ||
            mime in setOf("image/heic", "image/heif", "image/webp") ||
            displayName.endsWith(".heic", true) || displayName.endsWith(".heif", true) || displayName.endsWith(".webp", true)

        if (!shouldSniff) {
            return MotionInfo(isMotionPhoto = false, needsDetection = true)
        }

        return runCatching {
            val head = readHeadText(resolver, uri, 192 * 1024)
            val tailBytes = readTailBytes(resolver, uri, size, 2 * 1024 * 1024)
            val tailText = tailBytes.toSafeText()
            val joined = head + "\n" + tailText

            parseOffsetFromXmp(joined, size)?.let { exact ->
                return@runCatching MotionInfo(
                    isMotionPhoto = true,
                    motionVideoOffset = exact.first,
                    motionVideoSize = exact.second,
                    needsDetection = false,
                    hint = "xmp-offset",
                )
            }

            val hasMotionMetadata = embeddedMotionKeywords.any { joined.contains(it, ignoreCase = true) }
            val tailStart = (size - tailBytes.size).coerceAtLeast(0L)
            val mp4StartInTail = findEmbeddedMp4Start(tailBytes)
            val hasVideoPayload = mp4TailMarkers.any { tailText.contains(it, ignoreCase = true) } || mp4StartInTail != null
            if (mp4StartInTail != null && (hasMotionMetadata || nameHintsMotion)) {
                val offset = tailStart + mp4StartInTail
                return@runCatching MotionInfo(
                    isMotionPhoto = true,
                    motionVideoOffset = offset,
                    motionVideoSize = (size - offset).coerceAtLeast(0L),
                    needsDetection = false,
                    hint = "tail-mp4",
                )
            }

            MotionInfo(
                isMotionPhoto = hasMotionMetadata || (hasVideoPayload && nameHintsMotion),
                needsDetection = hasMotionMetadata || hasVideoPayload || nameHintsMotion,
                hint = when {
                    hasMotionMetadata -> "metadata"
                    hasVideoPayload -> "tail-video"
                    nameHintsMotion -> "name"
                    else -> ""
                },
            )
        }.getOrElse {
            MotionInfo(isMotionPhoto = nameHintsMotion, needsDetection = true, hint = if (nameHintsMotion) "name" else "")
        }
    }

    /**
     * Handles common Google/Samsung XMP styles:
     * - Camera:MicroVideoOffset is usually bytes from EOF to MP4 start.
     * - Container/Item directory often exposes MotionPhoto item Length + optional Padding.
     */
    private fun parseOffsetFromXmp(text: String, totalSize: Long): Pair<Long, Long>? {
        val microOffset = firstLong(
            text,
            listOf(
                "Camera:MicroVideoOffset\\s*=\\s*[\"'](\\d+)[\"']",
                "GCamera:MicroVideoOffset\\s*=\\s*[\"'](\\d+)[\"']",
                "MicroVideoOffset\\s*=\\s*[\"'](\\d+)[\"']",
                "MicroVideoOffset>(\\d+)<",
            )
        )
        if (microOffset != null && microOffset in 16_385L until totalSize) {
            val start = totalSize - microOffset
            if (start in 1 until totalSize) return start to microOffset
        }

        val motionDirectoryChunk = text.substringAfter("MotionPhoto", text).take(4096)
        val itemLength = firstLong(
            motionDirectoryChunk,
            listOf(
                "Item:Length\\s*=\\s*[\"'](\\d+)[\"']",
                "GContainerItem:Length\\s*=\\s*[\"'](\\d+)[\"']",
                "Length\\s*=\\s*[\"'](\\d+)[\"']",
            )
        )
        val itemPadding = firstLong(
            motionDirectoryChunk,
            listOf(
                "Item:Padding\\s*=\\s*[\"'](\\d+)[\"']",
                "GContainerItem:Padding\\s*=\\s*[\"'](\\d+)[\"']",
                "Padding\\s*=\\s*[\"'](\\d+)[\"']",
            )
        ) ?: 0L
        if (itemLength != null && itemLength in 16_385L until totalSize && itemPadding >= 0L && itemLength + itemPadding < totalSize) {
            val start = totalSize - itemPadding - itemLength
            if (start in 1 until totalSize) return start to itemLength
        }
        return null
    }

    private fun firstLong(text: String, patterns: List<String>): Long? {
        for (pattern in patterns) {
            val value = Regex(pattern, setOf(RegexOption.IGNORE_CASE)).find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
            if (value != null) return value
        }
        return null
    }

    private fun readHeadText(resolver: ContentResolver, uri: Uri, maxBytes: Int): String {
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(maxBytes)
            val read = input.read(buffer)
            if (read > 0) return buffer.copyOf(read).toSafeText()
        }
        return ""
    }

    private fun readTailText(resolver: ContentResolver, uri: Uri, size: Long, maxBytes: Int): String =
        readTailBytes(resolver, uri, size, maxBytes).toSafeText()

    private fun readTailBytes(resolver: ContentResolver, uri: Uri, size: Long, maxBytes: Int): ByteArray {
        resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            afd.createInputStream().use { input ->
                val start = (size - maxBytes).coerceAtLeast(0L)
                var skipped = 0L
                while (skipped < start) {
                    val delta = input.skip(start - skipped)
                    if (delta <= 0L) {
                        if (input.read() == -1) break else skipped++
                    } else {
                        skipped += delta
                    }
                }
                val out = ByteArrayOutputStream(maxBytes.coerceAtMost(2 * 1024 * 1024))
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    out.write(buffer, 0, read)
                    if (out.size() >= maxBytes) break
                }
                return out.toByteArray()
            }
        }
        return ByteArray(0)
    }

    fun findEmbeddedMp4Start(bytes: ByteArray): Int? {
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

    private fun ByteArray.toSafeText(): String = buildString(size.coerceAtMost(4096)) {
        for (b in this@toSafeText) {
            val c = b.toInt().and(0xFF).toChar()
            append(if (c.code in 32..126 || c == '\n' || c == '\r' || c == '\t') c else ' ')
        }
    }
}
