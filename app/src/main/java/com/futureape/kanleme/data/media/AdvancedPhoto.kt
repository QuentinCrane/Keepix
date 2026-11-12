package com.futureape.kanleme.data.media

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

data class PhotoFolderKey(val volume: String, val relativePath: String)
data class PhotoFolder(val key: PhotoFolderKey, val count: Int)

/** Metadata-only image row used by the advanced photo browser. */
data class AdvancedPhoto(
    val uri: String,
    val name: String,
    val folder: PhotoFolderKey,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateAddedMs: Long,
    val mimeType: String,
    val dateModifiedMs: Long,
    val isScreenshot: Boolean = false,
    val isSelfie: Boolean = false,
    val isMotion: Boolean = false,
    val isGif: Boolean = false,
    val isRaw: Boolean = false,
    val isLongImage: Boolean = false,
) {
    val mediaStoreId: Long get() = uri.substringAfterLast('/').toLong()
    val shortEdge: Int get() = minOf(width, height)
    val extension: String get() = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
}

fun normalizeAdvancedPhotoMovePath(input: String): String? {
    val path = input.trim().replace('\\', '/').trim('/')
    if (path.isEmpty() || path.split('/').any { it.isBlank() || it == "." || it == ".." }) return null
    if (path.any { it == '\u0000' || it == ':' } || path.length > 240) return null
    return path + "/"
}

fun photoFolderFromDocumentId(documentId: String): PhotoFolderKey? {
    val separator = documentId.indexOf(':')
    if (separator < 1) return null
    val root = documentId.substring(0, separator)
    val rawPath = documentId.substring(separator + 1).trim('/')
    val volume = when (root.lowercase(Locale.ROOT)) {
        "primary", "home" -> "external_primary"
        else -> root.lowercase(Locale.ROOT)
    }
    val path = if (root.equals("home", true)) "Documents/" + rawPath else rawPath
    if (path.split('/').any { it == "." || it == ".." } || path.any { it == '\u0000' || it == ':' || it == '\\' }) return null
    val normalized = if (path.isEmpty()) "" else path.trimEnd('/') + "/"
    return PhotoFolderKey(volume, normalized)
}

fun photoFolderInScope(folder: PhotoFolderKey, selected: Set<PhotoFolderKey>, includeSubfolders: Boolean): Boolean =
    selected.any { parent -> parent.volume == folder.volume &&
        (parent.relativePath == folder.relativePath || (includeSubfolders && folder.relativePath.startsWith(parent.relativePath))) }

enum class PhotoResolution { ALL, BELOW_HD, HD, FULL_HD, UHD, UNKNOWN }
enum class PhotoOrientation { ALL, LANDSCAPE, PORTRAIT, SQUARE, UNKNOWN }
enum class PhotoFormat { ALL, JPEG, PNG, WEBP, HEIF, GIF, RAW, OTHER }
enum class PhotoKind { ALL, NORMAL, SCREENSHOT, SELFIE, MOTION, GIF, RAW, LONG_IMAGE }
enum class PhotoDate { ALL, WEEK, MONTH, YEAR }
enum class PhotoSort { NEWEST, OLDEST, LARGEST, SMALLEST, RESOLUTION, NAME }

data class PhotoSizeRange(val minBytes: Long? = null, val maxBytes: Long? = null) {
    companion object {
        /** MiB inputs, inclusive bounds; reject negative, overflow and reversed ranges. */
        fun parse(min: String, max: String): Result<PhotoSizeRange> = runCatching {
            fun bytes(text: String): Long? = text.trim().takeIf { it.isNotEmpty() }?.let {
                val number = BigDecimal(it)
                require(number.signum() >= 0)
                number.multiply(BigDecimal(1_048_576)).setScale(0, RoundingMode.HALF_UP).longValueExact()
            }
            val range = PhotoSizeRange(bytes(min), bytes(max))
            require(range.minBytes == null || range.maxBytes == null || range.minBytes <= range.maxBytes)
            range
        }
    }
}

data class AdvancedPhotoFilter(
    val query: String = "",
    val resolution: PhotoResolution = PhotoResolution.ALL,
    val size: PhotoSizeRange = PhotoSizeRange(),
    val orientation: PhotoOrientation = PhotoOrientation.ALL,
    val format: PhotoFormat = PhotoFormat.ALL,
    val kind: PhotoKind = PhotoKind.ALL,
    val date: PhotoDate = PhotoDate.ALL,
    val sort: PhotoSort = PhotoSort.NEWEST,
) {
    fun matches(photo: AdvancedPhoto, nowMs: Long): Boolean {
        if (!photo.name.contains(query.trim(), ignoreCase = true)) return false
        val edge = photo.shortEdge
        if (!when (resolution) {
            PhotoResolution.ALL -> true
            PhotoResolution.BELOW_HD -> edge in 1..719
            PhotoResolution.HD -> edge in 720..1079
            PhotoResolution.FULL_HD -> edge in 1080..2159
            PhotoResolution.UHD -> edge >= 2160
            PhotoResolution.UNKNOWN -> edge <= 0
        }) return false
        if ((size.minBytes != null || size.maxBytes != null) && photo.sizeBytes <= 0L) return false
        if (size.minBytes != null && photo.sizeBytes < size.minBytes) return false
        if (size.maxBytes != null && photo.sizeBytes > size.maxBytes) return false
        if (!when (orientation) {
            PhotoOrientation.ALL -> true
            PhotoOrientation.LANDSCAPE -> edge > 0 && photo.width > photo.height
            PhotoOrientation.PORTRAIT -> edge > 0 && photo.height > photo.width
            PhotoOrientation.SQUARE -> edge > 0 && photo.width == photo.height
            PhotoOrientation.UNKNOWN -> edge <= 0
        }) return false
        if (format != PhotoFormat.ALL && actualFormat(photo) != format) return false
        if (!when (kind) {
            PhotoKind.ALL -> true
            PhotoKind.NORMAL -> !photo.isScreenshot && !photo.isSelfie && !photo.isMotion && !photo.isGif && !photo.isRaw && !photo.isLongImage
            PhotoKind.SCREENSHOT -> photo.isScreenshot
            PhotoKind.SELFIE -> photo.isSelfie
            PhotoKind.MOTION -> photo.isMotion
            PhotoKind.GIF -> photo.isGif
            PhotoKind.RAW -> photo.isRaw
            PhotoKind.LONG_IMAGE -> photo.isLongImage
        }) return false
        val days = when (date) {
            PhotoDate.ALL -> 0
            PhotoDate.WEEK -> 7
            PhotoDate.MONTH -> 30
            PhotoDate.YEAR -> 365
        }
        return days == 0 || photo.dateAddedMs > 0L && photo.dateAddedMs in (nowMs - days * 86_400_000L)..nowMs
    }

    fun apply(photos: List<AdvancedPhoto>, nowMs: Long, checkCancelled: () -> Unit = {}): List<AdvancedPhoto> {
        val comparator = when (sort) {
            PhotoSort.NEWEST -> compareByDescending<AdvancedPhoto> { it.dateAddedMs }
            PhotoSort.OLDEST -> compareBy { it.dateAddedMs }
            PhotoSort.LARGEST -> compareByDescending { it.sizeBytes }
            PhotoSort.SMALLEST -> compareBy { it.sizeBytes }
            PhotoSort.RESOLUTION -> compareByDescending { it.width.toLong() * it.height }
            PhotoSort.NAME -> Comparator { left: AdvancedPhoto, right: AdvancedPhoto ->
                left.name.compareTo(right.name, ignoreCase = true)
            }
        }.thenBy { it.uri }
        val matched = ArrayList<AdvancedPhoto>()
        photos.forEachIndexed { index, photo ->
            if (index % 256 == 0) checkCancelled()
            if (matches(photo, nowMs)) matched.add(photo)
        }
        var comparisons = 0
        matched.sortWith { left, right ->
            if (comparisons++ % 256 == 0) checkCancelled()
            comparator.compare(left, right)
        }
        checkCancelled()
        return matched
    }

    private fun actualFormat(photo: AdvancedPhoto): PhotoFormat = when (photo.extension) {
        "jpg", "jpeg", "jpe" -> PhotoFormat.JPEG
        "png" -> PhotoFormat.PNG
        "webp" -> PhotoFormat.WEBP
        "heic", "heif", "avif" -> PhotoFormat.HEIF
        "gif" -> PhotoFormat.GIF
        "dng", "nef", "cr2", "cr3", "arw", "raf", "rw2", "orf", "pef" -> PhotoFormat.RAW
        else -> when (photo.mimeType.lowercase(Locale.ROOT)) {
            "image/jpeg" -> PhotoFormat.JPEG
            "image/png" -> PhotoFormat.PNG
            "image/webp" -> PhotoFormat.WEBP
            "image/heic", "image/heif", "image/avif" -> PhotoFormat.HEIF
            "image/gif" -> PhotoFormat.GIF
            "image/x-adobe-dng", "image/x-nikon-nef", "image/x-canon-cr2", "image/x-sony-arw" -> PhotoFormat.RAW
            else -> PhotoFormat.OTHER
        }
    }
}
