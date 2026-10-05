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
) {
    val shortEdge: Int get() = minOf(width, height)
    val extension: String get() = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
}

enum class VideoResolution { ALL, BELOW_HD, HD, FULL_HD, UHD, UNKNOWN }
enum class VideoDuration { ALL, SHORT, MEDIUM, LONG, UNKNOWN }
enum class VideoOrientation { ALL, LANDSCAPE, PORTRAIT, SQUARE, UNKNOWN }
enum class VideoFormat { ALL, MP4, MKV, WEBM, MOV, OTHER }
enum class VideoDate { ALL, WEEK, MONTH, YEAR }
enum class VideoSort { NEWEST, OLDEST, LARGEST, SMALLEST, RESOLUTION, DURATION, NAME }

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
