package com.futureape.kanleme.data.media

import org.junit.Assert.*
import org.junit.Test

class AdvancedVideoFilterTest {
    @Test fun systemDocumentFoldersMapToMediaStoreVolumes() {
        assertEquals(VideoFolderKey("external_primary", "Movies/精选/"), videoFolderFromDocumentId("primary:Movies/精选"))
        assertEquals(VideoFolderKey("abcd-1234", "DCIM/Camera/"), videoFolderFromDocumentId("ABCD-1234:DCIM/Camera"))
        assertEquals(VideoFolderKey("external_primary", "Documents/Saved/"), videoFolderFromDocumentId("home:Saved"))
        assertEquals(VideoFolderKey("external_primary", "Movies/ name /"), videoFolderFromDocumentId("primary:Movies/ name "))
        assertNull(videoFolderFromDocumentId("missing-colon"))
        assertNull(videoFolderFromDocumentId("primary:Movies/../DCIM"))
    }

    @Test fun recursiveFolderScopeRespectsVolumeAndDirectoryBoundaries() {
        val parent = VideoFolderKey("external_primary", "Movies/A/")
        val child = parent.copy(relativePath = "Movies/A/Child/")
        assertTrue(videoFolderInScope(child, setOf(parent), true))
        assertFalse(videoFolderInScope(child, setOf(parent), false))
        assertFalse(videoFolderInScope(parent.copy(relativePath = "Movies/AB/"), setOf(parent), true))
        assertFalse(videoFolderInScope(child.copy(volume = "abcd-1234"), setOf(parent), true))
    }
    @Test fun movePathsRejectTraversalAndInvalidPaths() {
        assertEquals("Movies/精选/", normalizeAdvancedVideoMovePath(" Movies\\精选 "))
        listOf("", "/", "Movies/../DCIM", "Movies/./x", "Movies//x", "E:/Video", "Movies/\u0000x").forEach {
            assertNull(normalizeAdvancedVideoMovePath(it))
        }
    }
    private val now = 1_800_000_000_000L
    private fun video(
        uri: String = "content://media/external_primary/video/media/1",
        width: Int = 1920, height: Int = 1080, bytes: Long = 524_288_000,
        duration: Long = 60_000, name: String = "Holiday.MP4", date: Long = now,
    ) = AdvancedVideo(uri, name, VideoFolderKey("external_primary", "Movies/"),
        bytes, width, height, duration, date, "video/mp4")

    @Test fun resolutionUsesShortEdgeForBothOrientations() {
        val filter = AdvancedVideoFilter(resolution = VideoResolution.FULL_HD)
        assertTrue(filter.matches(video(), now))
        assertTrue(filter.matches(video(width = 1080, height = 1920), now))
        assertFalse(filter.matches(video(width = 1920, height = 800), now))
    }

    @Test fun resolutionBandEdgesAreDisjoint() {
        val edges = listOf(719 to VideoResolution.BELOW_HD, 720 to VideoResolution.HD,
            1079 to VideoResolution.HD, 1080 to VideoResolution.FULL_HD,
            2159 to VideoResolution.FULL_HD, 2160 to VideoResolution.UHD)
        edges.forEach { (edge, band) ->
            VideoResolution.entries.filter { it != VideoResolution.ALL }.forEach {
                assertEquals("edge=" + edge + ", band=" + it, it == band,
                    AdvancedVideoFilter(resolution = it).matches(video(width = edge * 2, height = edge), now))
            }
        }
    }

    @Test fun unknownMetadataDoesNotMatchKnownDimensionsOrDuration() {
        val unknown = video(width = 0, height = 0, duration = 0, bytes = 0)
        assertTrue(AdvancedVideoFilter().matches(unknown, now))
        assertTrue(AdvancedVideoFilter(resolution = VideoResolution.UNKNOWN,
            duration = VideoDuration.UNKNOWN, orientation = VideoOrientation.UNKNOWN).matches(unknown, now))
        assertFalse(AdvancedVideoFilter(orientation = VideoOrientation.SQUARE).matches(unknown, now))
        assertFalse(AdvancedVideoFilter(size = VideoSizeRange(minBytes = 0)).matches(unknown, now))
    }

    @Test fun durationEdgesAreDisjoint() {
        assertTrue(AdvancedVideoFilter(duration = VideoDuration.SHORT).matches(video(duration = 59_999), now))
        assertFalse(AdvancedVideoFilter(duration = VideoDuration.SHORT).matches(video(duration = 60_000), now))
        assertTrue(AdvancedVideoFilter(duration = VideoDuration.MEDIUM).matches(video(duration = 60_000), now))
        assertFalse(AdvancedVideoFilter(duration = VideoDuration.MEDIUM).matches(video(duration = 600_000), now))
        assertTrue(AdvancedVideoFilter(duration = VideoDuration.LONG).matches(video(duration = 600_000), now))
    }

    @Test fun decimalMiBAndInclusiveSizeBounds() {
        val range = VideoSizeRange.parse("0.5", "1").getOrThrow()
        assertEquals(524_288L, range.minBytes)
        assertEquals(1_048_576L, range.maxBytes)
        val filter = AdvancedVideoFilter(size = range)
        assertTrue(filter.matches(video(bytes = 524_288), now))
        assertTrue(filter.matches(video(bytes = 1_048_576), now))
        assertFalse(filter.matches(video(bytes = 524_287), now))
        assertFalse(filter.matches(video(bytes = 1_048_577), now))
    }

    @Test fun invalidAndOverflowSizeInputIsRejected() {
        listOf("-1", "NaN", "Infinity", "text", "999999999999999999999999999999").forEach {
            assertTrue(VideoSizeRange.parse(it, "").isFailure)
        }
        assertTrue(VideoSizeRange.parse("100", "10").isFailure)
        assertEquals(VideoSizeRange(), VideoSizeRange.parse(" ", "").getOrThrow())
    }

    @Test fun allConditionsAreCombinedAndSearchIgnoresCase() {
        val filter = AdvancedVideoFilter(query = " HOLIDAY ", resolution = VideoResolution.FULL_HD,
            size = VideoSizeRange(minBytes = 500 * 1_048_576L), duration = VideoDuration.MEDIUM,
            format = VideoFormat.MP4, orientation = VideoOrientation.LANDSCAPE)
        assertTrue(filter.matches(video(), now))
        assertFalse(filter.matches(video(name = "Other.mp4"), now))
        assertFalse(filter.matches(video(width = 1080, height = 1920), now))
        assertFalse(filter.matches(video(bytes = 499 * 1_048_576L), now))
    }

    @Test fun formatUsesCaseInsensitiveExtensionAndMimeFallback() {
        assertTrue(AdvancedVideoFilter(format = VideoFormat.MKV).matches(video(name = "FILE.MKV"), now))
        assertTrue(AdvancedVideoFilter(format = VideoFormat.MP4).matches(video(name = "no_extension"), now))
        assertTrue(AdvancedVideoFilter(format = VideoFormat.MP4).matches(video(name = "movie.m4v"), now))
        assertFalse(AdvancedVideoFilter(format = VideoFormat.WEBM).matches(video(), now))
    }

    @Test fun recentDateUsesInclusiveWindowAndExcludesUnknownOrFuture() {
        val filter = AdvancedVideoFilter(date = VideoDate.WEEK)
        assertTrue(filter.matches(video(date = now - 7 * 86_400_000L), now))
        assertFalse(filter.matches(video(date = now - 7 * 86_400_000L - 1), now))
        assertFalse(filter.matches(video(date = 0), now))
        assertFalse(filter.matches(video(date = now + 1), now))
    }

    @Test fun noDeckLimitAndStableSortAcrossStorageVolumes() {
        val videos = (1..1501).map { video(uri = "content://media/volume/video/media/" + it, bytes = it.toLong()) }
        val results = AdvancedVideoFilter(sort = VideoSort.LARGEST).apply(videos, now)
        assertEquals(1501, results.size)
        assertEquals(1501L, results.first().sizeBytes)
        assertEquals(1L, results.last().sizeBytes)
        val sameIdOtherVolume = video(uri = "content://media/sdcard/video/media/1")
        val sameIdPrimary = video(uri = "content://media/external_primary/video/media/1")
        val sorted = AdvancedVideoFilter().apply(listOf(sameIdOtherVolume, sameIdPrimary), now)
        assertEquals(listOf(sameIdPrimary, sameIdOtherVolume), sorted)
    }

    @Test fun cancellationInterruptsFilteringWithoutPublishingPartialResults() {
        val videos = (1..50_000).map { video(uri = it.toString()) }
        var checks = 0
        val result = runCatching {
            AdvancedVideoFilter().apply(videos, now) {
                if (++checks == 3) throw java.util.concurrent.CancellationException()
            }
        }
        assertTrue(result.exceptionOrNull() is java.util.concurrent.CancellationException)
        assertEquals(3, checks)
    }

    @Test fun cancellationAlsoInterruptsSorting() {
        val videos = (1..10_000).map { video(uri = it.toString(), name = (10_001 - it).toString()) }
        val filterChecks = (videos.size + 255) / 256
        var checks = 0
        val result = runCatching {
            AdvancedVideoFilter(sort = VideoSort.NAME).apply(videos, now) {
                if (++checks > filterChecks + 2) throw java.util.concurrent.CancellationException()
            }
        }
        assertTrue(result.exceptionOrNull() is java.util.concurrent.CancellationException)
        assertEquals(filterChecks + 3, checks)
    }
}
