package com.futureape.kanleme.data.media

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

data class VideoFolderKey(val volume: String, val relativePath: String)
data class VideoFolder(val key: VideoFolderKey, val count: Int)

/** Metadata only: scanning never creates a decoder or loads video frames. */
data class AdvancedVideo(
    val uri: String,
    val name: String,
    val folder: VideoFolderKey,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val dateAddedMs: Long,
    val mimeType: String,
    val dateModifiedMs: Long = 0,
) {
    val mediaStoreId: Long get() = uri.substringAfterLast('/').toLong()
    val shortEdge: Int get() = minOf(width, height)
    val extension: String get() = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    /** Approximate total file bitrate; includes audio and container overhead. */
    val estimatedAverageBitrateBps: Long?
        get() {
            if (sizeBytes <= 0L || durationMs <= 0L) return null
            val estimate = sizeBytes.toDouble() * 8_000.0 / durationMs
            return estimate.takeIf { it.isFinite() && it >= 1.0 }?.toLong()
        }
    /** SDR, standard-frame-rate reference bitrate by the shorter edge. */
    val referenceBitrateBps: Long?
        get() = when (shortEdge) {
            in 360..479 -> 1_000_000L
            in 480..719 -> 2_500_000L
            in 720..1079 -> 5_000_000L
            in 1080..1439 -> 8_000_000L
            in 1440..2159 -> 16_000_000L
            in 2160..Int.MAX_VALUE -> 35_000_000L
            else -> null
        }
}

fun normalizeAdvancedVideoMovePath(input: String): String? {
    val path = input.trim().replace('\\', '/').trim('/')
    if (path.isEmpty() || path.split('/').any { it.isBlank() || it == "." || it == ".." }) return null
    if (path.any { it == '\u0000' || it == ':' } || path.length > 240) return null
    return path + "/"
}

fun videoFolderFromDocumentId(documentId: String): VideoFolderKey? {
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
    // Provider document IDs already contain literal filesystem names; keep their spaces intact.
    val normalized = if (path.isEmpty()) "" else path.trimEnd('/') + "/"
    return VideoFolderKey(volume, normalized)
}

fun videoFolderInScope(folder: VideoFolderKey, selected: Set<VideoFolderKey>, includeSubfolders: Boolean): Boolean =
    selected.any { parent -> parent.volume == folder.volume &&
        (parent.relativePath == folder.relativePath || (includeSubfolders && folder.relativePath.startsWith(parent.relativePath))) }

enum class VideoResolution { ALL, BELOW_HD, HD, FULL_HD, UHD, UNKNOWN }
enum class VideoDuration { ALL, SHORT, MEDIUM, LONG, UNKNOWN }
enum class VideoBitrateBand { ALL, VERY_LOW, LOW, REFERENCE, HIGH, UNKNOWN }
enum class VideoOrientation { ALL, LANDSCAPE, PORTRAIT, SQUARE, UNKNOWN }
enum class VideoFormat { ALL, MP4, MKV, WEBM, MOV, OTHER }
enum class VideoDate { ALL, WEEK, MONTH, YEAR }
enum class VideoSort { NEWEST, OLDEST, LARGEST, SMALLEST, RESOLUTION, DURATION, BITRATE, NAME }

data class VideoSizeRange(val minBytes: Long? = null, val maxBytes: Long? = null) {
    companion object {
        /** MiB inputs, inclusive bounds; reject negative, overflow and reversed ranges. */
        fun parse(min: String, max: String): Result<VideoSizeRange> = runCatching {
            fun bytes(text: String): Long? = text.trim().takeIf { it.isNotEmpty() }?.let {
                val number = BigDecimal(it)
                require(number.signum() >= 0)
                number.multiply(BigDecimal(1_048_576)).setScale(0, RoundingMode.HALF_UP).longValueExact()
            }
            val range = VideoSizeRange(bytes(min), bytes(max))
            require(range.minBytes == null || range.maxBytes == null || range.minBytes <= range.maxBytes)
            range
        }
    }
}

data class AdvancedVideoFilter(
    val query: String = "",
    val resolution: VideoResolution = VideoResolution.ALL,
    val bitrateBand: VideoBitrateBand = VideoBitrateBand.ALL,
    val size: VideoSizeRange = VideoSizeRange(),
    val duration: VideoDuration = VideoDuration.ALL,
    val orientation: VideoOrientation = VideoOrientation.ALL,
    val format: VideoFormat = VideoFormat.ALL,
    val date: VideoDate = VideoDate.ALL,
    val sort: VideoSort = VideoSort.NEWEST,
) {
    fun matches(video: AdvancedVideo, nowMs: Long): Boolean {
        if (!video.name.contains(query.trim(), ignoreCase = true)) return false
        val edge = video.shortEdge
        if (!when (resolution) {
            VideoResolution.ALL -> true
            VideoResolution.BELOW_HD -> edge in 1..719
            VideoResolution.HD -> edge in 720..1079
            VideoResolution.FULL_HD -> edge in 1080..2159
            VideoResolution.UHD -> edge >= 2160
            VideoResolution.UNKNOWN -> edge <= 0
        }) return false
        if (bitrateBand != VideoBitrateBand.ALL) {
            val bitrate = video.estimatedAverageBitrateBps
            val reference = video.referenceBitrateBps
            val halfReference = reference?.div(2L)
            val highReference = reference?.let { it + it / 2L }
            val actualBand = when {
                bitrate == null || reference == null -> VideoBitrateBand.UNKNOWN
                bitrate < requireNotNull(halfReference) -> VideoBitrateBand.VERY_LOW
                bitrate < reference -> VideoBitrateBand.LOW
                bitrate < requireNotNull(highReference) -> VideoBitrateBand.REFERENCE
                else -> VideoBitrateBand.HIGH
            }
            if (actualBand != bitrateBand) return false
        }
        if ((size.minBytes != null || size.maxBytes != null) && video.sizeBytes <= 0) return false
        if (size.minBytes != null && video.sizeBytes < size.minBytes) return false
        if (size.maxBytes != null && video.sizeBytes > size.maxBytes) return false
        if (!when (duration) {
            VideoDuration.ALL -> true
            VideoDuration.SHORT -> video.durationMs in 1 until 60_000
            VideoDuration.MEDIUM -> video.durationMs in 60_000 until 600_000
            VideoDuration.LONG -> video.durationMs >= 600_000
            VideoDuration.UNKNOWN -> video.durationMs <= 0
        }) return false
        if (!when (orientation) {
            VideoOrientation.ALL -> true
            VideoOrientation.LANDSCAPE -> edge > 0 && video.width > video.height
            VideoOrientation.PORTRAIT -> edge > 0 && video.height > video.width
            VideoOrientation.SQUARE -> edge > 0 && video.width == video.height
            VideoOrientation.UNKNOWN -> edge <= 0
        }) return false
        if (format != VideoFormat.ALL) {
            val actualFormat = when (video.extension) {
                "mp4", "m4v" -> VideoFormat.MP4
                "mkv" -> VideoFormat.MKV
                "webm" -> VideoFormat.WEBM
                "mov" -> VideoFormat.MOV
                else -> when (video.mimeType.lowercase(Locale.ROOT)) {
                    "video/mp4" -> VideoFormat.MP4
                    "video/x-matroska" -> VideoFormat.MKV
                    "video/webm" -> VideoFormat.WEBM
                    "video/quicktime" -> VideoFormat.MOV
                    else -> VideoFormat.OTHER
                }
            }
            if (actualFormat != format) return false
        }
        val days = when (date) {
            VideoDate.ALL -> 0
            VideoDate.WEEK -> 7
            VideoDate.MONTH -> 30
            VideoDate.YEAR -> 365
        }
        return days == 0 || video.dateAddedMs in (nowMs - days * 86_400_000L)..nowMs
    }

    fun apply(videos: List<AdvancedVideo>, nowMs: Long, checkCancelled: () -> Unit = {}): List<AdvancedVideo> {
        val comparator = when (sort) {
            VideoSort.NEWEST -> compareByDescending<AdvancedVideo> { it.dateAddedMs }
            VideoSort.OLDEST -> compareBy { it.dateAddedMs }
            VideoSort.LARGEST -> compareByDescending { it.sizeBytes }
            VideoSort.SMALLEST -> compareBy { it.sizeBytes }
            VideoSort.RESOLUTION -> compareByDescending { it.width.toLong() * it.height }
            VideoSort.DURATION -> compareByDescending { it.durationMs }
            VideoSort.BITRATE -> compareByDescending<AdvancedVideo> { it.estimatedAverageBitrateBps ?: Long.MIN_VALUE }
            VideoSort.NAME -> Comparator { left: AdvancedVideo, right: AdvancedVideo ->
                left.name.compareTo(right.name, ignoreCase = true)
            }
        }.thenBy { it.uri }
        // One result allocation, in-place sort, and cooperative cancellation during both phases.
        val matched = ArrayList<AdvancedVideo>()
        videos.forEachIndexed { index, video ->
            if (index % 256 == 0) checkCancelled()
            if (matches(video, nowMs)) matched.add(video)
        }
        var comparisons = 0
        matched.sortWith { left, right ->
            if (comparisons++ % 256 == 0) checkCancelled()
            comparator.compare(left, right)
        }
        checkCancelled()
        return matched
    }
}
