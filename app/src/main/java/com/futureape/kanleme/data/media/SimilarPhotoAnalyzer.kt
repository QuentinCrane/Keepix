package com.futureape.kanleme.data.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.PhotoFingerprintEntity
import com.futureape.kanleme.data.local.SimilarGroupEntity
import com.futureape.kanleme.data.local.SimilarGroupPhotoEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

@Singleton
class SimilarPhotoAnalyzer @Inject constructor(
    private val resolver: ContentResolver,
) {
    data class Result(
        val fingerprints: List<PhotoFingerprintEntity>,
        val groups: List<SimilarGroupEntity>,
        val members: List<SimilarGroupPhotoEntity>,
    )

    fun analyze(photos: List<PhotoEntity>): Result {
        val now = System.currentTimeMillis()
        val fps = photos.mapNotNull { photo -> fingerprintOrNull(photo, now) }
        return buildGroups(photos = photos, fingerprints = fps, now = now).copy(fingerprints = fps)
    }

    fun fingerprintOrNull(photo: PhotoEntity, now: Long = System.currentTimeMillis()): PhotoFingerprintEntity? {
        if (photo.isRaw || photo.isGif || photo.width <= 0 || photo.height <= 0) return null
        return runCatching { fingerprint(photo, now) }.getOrNull()
    }

    fun buildGroups(
        photos: List<PhotoEntity>,
        fingerprints: List<PhotoFingerprintEntity>,
        now: Long = System.currentTimeMillis(),
    ): Result {
        val byId = photos.associateBy { it.id }
        val fps = fingerprints.distinctBy { it.photoId }.filter { it.photoId in byId }
        val fpById = fps.associateBy { it.photoId }
        val groups = mutableListOf<SimilarGroupEntity>()
        val members = mutableListOf<SimilarGroupPhotoEntity>()
        val used = mutableSetOf<Long>()

        fun addGroup(type: String, ids: List<Long>, similarity: Double) {
            val unique = ids.distinct().filter { byId[it] != null }
            if (unique.size < 2) return
            val candidates = unique.mapNotNull { id -> fpById[id] }
            val best = candidates.maxByOrNull { it.qualityScore }?.photoId ?: unique.first()
            val groupId = "$type-${UUID.nameUUIDFromBytes(unique.sorted().joinToString("|").toByteArray())}"
            groups += SimilarGroupEntity(
                id = groupId,
                bestPhotoId = best,
                type = type,
                averageSimilarity = similarity.coerceIn(0.0, 1.0),
                createdAt = now,
                updatedAt = now,
            )
            members += unique.map { id ->
                SimilarGroupPhotoEntity(
                    groupId = groupId,
                    photoId = id,
                    similarity = if (id == best) 1.0 else similarity.coerceIn(0.0, 1.0),
                    isBest = id == best,
                )
            }
            used += unique
        }

        // Burst groups: same folder, shot within a short interval.
        photos.sortedBy { it.dateTaken }.groupBy { it.folderPath }.values.forEach { list ->
            var bucket = mutableListOf<PhotoEntity>()
            list.forEach { photo ->
                if (bucket.isEmpty() || abs(photo.dateTaken - bucket.last().dateTaken) <= 6_000L) {
                    bucket += photo
                } else {
                    if (bucket.size >= 3) addGroup("burst", bucket.map { it.id }, 0.92)
                    bucket = mutableListOf(photo)
                }
            }
            if (bucket.size >= 3) addGroup("burst", bucket.map { it.id }, 0.92)
        }

        // Screenshot groups: same resolution and folder.
        photos.filter { it.isScreenshot }.groupBy { "${it.width}x${it.height}:${it.folderPath}" }.values
            .filter { it.size >= 2 }
            .take(80)
            .forEach { addGroup("screenshot", it.map { p -> p.id }, 0.88) }

        // Blur list: group a few blurry photos together to provide a single cleanup entry.
        fps.filter { it.sharpness < 0.055 }.sortedBy { it.sharpness }.chunked(6).filter { it.size >= 2 }.forEach {
            addGroup("blur", it.map { fp -> fp.photoId }, 0.70)
        }

        // Perceptual hash grouping. Keep this bounded so a large library does not get stuck in O(n²) comparisons.
        val remaining = fps.filterNot { it.photoId in used }.sortedByDescending { it.qualityScore }
        val bucketIndex = buildCandidateBuckets(remaining)
        val consumed = mutableSetOf<Long>()
        remaining.forEach { anchor ->
            if (anchor.photoId in consumed) return@forEach
            val candidateIds = hashBucketKeys(anchor)
                .flatMap { key -> bucketIndex[key].orEmpty() }
                .asSequence()
                .filter { it != anchor.photoId && it !in consumed }
                .distinct()
                .take(360)
                .toList()
            if (candidateIds.isEmpty()) return@forEach
            val near = mutableListOf(anchor)
            candidateIds.forEach { candidateId ->
                val other = fpById[candidateId] ?: return@forEach
                val pDistance = hamming(anchor.pHash, other.pHash)
                val dDistance = hamming(anchor.dHash, other.dHash)
                val aDistance = hamming(anchor.aHash, other.aHash)
                val sameSize = byId[anchor.photoId]?.let { a ->
                    byId[other.photoId]?.let { b ->
                        a.width == b.width && a.height == b.height && abs(a.size - b.size) < 128 * 1024
                    }
                } == true
                if (pDistance <= 10 || dDistance <= 8 || aDistance <= 6 || sameSize) {
                    near += other
                }
            }
            if (near.size >= 2) {
                val avgDistance = near.drop(1).map { minOf(hamming(anchor.pHash, it.pHash), hamming(anchor.dHash, it.dHash), hamming(anchor.aHash, it.aHash)) }
                    .average()
                    .takeIf { !it.isNaN() } ?: 0.0
                val similarity = 1.0 - (avgDistance / 64.0)
                val anchorSize = byId[anchor.photoId]?.size
                val type = if (near.drop(1).any { fp -> byId[fp.photoId]?.size == anchorSize }) "duplicate" else "similar"
                addGroup(type, near.map { it.photoId }, similarity)
                consumed += near.map { it.photoId }
            }
        }

        return Result(emptyList(), groups.distinctBy { it.id }, members.distinctBy { it.groupId to it.photoId })
    }

    private fun fingerprint(photo: PhotoEntity, now: Long): PhotoFingerprintEntity? {
        val bitmap = decodeSampledBitmap(Uri.parse(photo.uri)) ?: return null
        val small = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        if (small != bitmap) bitmap.recycle()
        val gray = IntArray(9 * 8)
        var sum = 0L
        for (y in 0 until 8) {
            for (x in 0 until 9) {
                val c = small.getPixel(x, y)
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val lum = (r * 299 + g * 587 + b * 114) / 1000
                gray[y * 9 + x] = lum
                if (x < 8) sum += lum.toLong()
            }
        }
        val avg = (sum / 64).toInt()
        var aHash = 0L
        var dHash = 0L
        var bit = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                if (gray[y * 9 + x] >= avg) aHash = aHash or (1L shl bit)
                if (gray[y * 9 + x] > gray[y * 9 + x + 1]) dHash = dHash or (1L shl bit)
                bit++
            }
        }
        small.recycle()
        val pHash = aHash xor java.lang.Long.rotateLeft(dHash, 17)
        val sharpness = estimateSharpness(gray)
        val exposure = (avg / 255.0).coerceIn(0.0, 1.0)
        val exposureScore = 1.0 - abs(exposure - 0.5) * 1.6
        val quality = (sharpness * 0.65 + exposureScore.coerceIn(0.0, 1.0) * 0.35).coerceIn(0.0, 1.0)
        return PhotoFingerprintEntity(
            photoId = photo.id,
            pHash = pHash,
            dHash = dHash,
            aHash = aHash,
            colorHistogram = "avg=$avg",
            pHashPrefix = pHash ushr 48,
            qualityScore = quality,
            sharpness = sharpness,
            exposure = exposure,
            computedAt = now,
        )
    }

    private fun decodeSampledBitmap(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val maxSide = max(bounds.outWidth, bounds.outHeight)
        while (maxSide / sample > 384) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return resolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream, null, options) }
    }

    private fun estimateSharpness(gray: IntArray): Double {
        var total = 0.0
        var count = 0
        for (y in 1 until 7) {
            for (x in 1 until 8) {
                val center = gray[y * 9 + x]
                val lap = abs(center * 4 - gray[y * 9 + x - 1] - gray[y * 9 + x + 1] - gray[(y - 1) * 9 + x] - gray[(y + 1) * 9 + x])
                total += lap / 255.0
                count++
            }
        }
        return if (count == 0) 0.0 else (total / count).coerceIn(0.0, 1.0)
    }

    private fun buildCandidateBuckets(fingerprints: List<PhotoFingerprintEntity>): Map<String, List<Long>> {
        val buckets = mutableMapOf<String, MutableList<Long>>()
        fingerprints.forEach { fp ->
            hashBucketKeys(fp).forEach { key -> buckets.getOrPut(key) { mutableListOf() } += fp.photoId }
        }
        return buckets
    }

    private fun hashBucketKeys(fp: PhotoFingerprintEntity): List<String> = listOf(
        "p16:${fp.pHash ushr 48}",
        "p12:${fp.pHash ushr 52}",
        "d16:${fp.dHash ushr 48}",
        "d12:${fp.dHash ushr 52}",
        "a16:${fp.aHash ushr 48}",
    )

    private fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
