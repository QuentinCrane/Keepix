package com.futureape.kanleme.benchmark

import java.util.Locale
import java.util.Random

data class MediaMetadataFixture(
    val mediaStoreId: Long,
    val mediaType: String,
    val folderPath: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
)

object FixtureManifest {
    val supportedScales: List<Int> = listOf(0, 100, 1_000, 10_000, 50_000)

    fun create(count: Int, seed: Long = 0x4B45455049584CL): List<MediaMetadataFixture> {
        require(count >= 0) { "count must be non-negative" }
        val random = Random(seed)
        val baseTime = 1_735_689_600_000L
        return List(count) { index ->
            val isVideo = index % 5 == 0
            MediaMetadataFixture(
                mediaStoreId = 10_000L + index,
                mediaType = if (isVideo) "video" else "photo",
                folderPath = String.format(Locale.ROOT, "Pictures/Benchmark/%02d", index % 16),
                sizeBytes = (256_000L + random.nextInt(8_000_000)).let { if (isVideo) it * 4 else it },
                dateTakenMillis = baseTime - index * 86_400_000L,
            )
        }
    }

    fun csv(count: Int, seed: Long = 0x4B45455049584CL): String = buildString {
        appendLine("media_store_id,media_type,folder_path,size_bytes,date_taken_millis")
        create(count, seed).forEach { fixture ->
            append(fixture.mediaStoreId).append(',')
                .append(fixture.mediaType).append(',')
                .append(fixture.folderPath).append(',')
                .append(fixture.sizeBytes).append(',')
                .appendLine(fixture.dateTakenMillis)
        }
    }
}
