package com.futureape.kanleme.data.repository

import androidx.room.withTransaction
import com.futureape.kanleme.data.local.AppDatabase
import com.futureape.kanleme.data.local.CleanupEventEntity
import com.futureape.kanleme.data.local.DeletionStatus
import com.futureape.kanleme.data.local.OperationHistoryEntity
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.ProcessingStatus
import com.futureape.kanleme.data.local.SimilarGroupEntity
import com.futureape.kanleme.data.local.TrashItemEntity
import com.futureape.kanleme.data.local.TrashRetentionPolicy
import com.futureape.kanleme.data.local.UserStatsEntity
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.data.media.MediaAuthorizationResult
import com.futureape.kanleme.data.media.MediaOperationKind
import com.futureape.kanleme.data.media.MediaOperationResult
import com.futureape.kanleme.data.media.MediaStoreActions
import com.futureape.kanleme.data.media.MediaStoreScanner
import com.futureape.kanleme.data.media.SimilarPhotoAnalyzer
import com.futureape.kanleme.data.settings.AppSettingsRepository
import com.futureape.kanleme.data.settings.DeleteMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val scanner: MediaStoreScanner,
    private val mediaStoreActions: MediaStoreActions,
    private val settingsRepository: AppSettingsRepository,
    private val similarPhotoAnalyzer: SimilarPhotoAnalyzer,
) : AppRepository {
    private companion object {
        const val SQLITE_BIND_BATCH_SIZE = 900
    }

    private val photoDao = db.photoDao()
    private val videoDao = db.videoDao()
    private val trashDao = db.trashDao()
    private val statsDao = db.statsDao()
    private val similarDao = db.similarDao()
    private val operationDao = db.operationDao()

    override fun observeDashboard(): Flow<DashboardStats> = combine(
        photoDao.observeCount().map { it as Any? },
        videoDao.observeCount().map { it as Any? },
        photoDao.observeProcessedCount().map { it as Any? },
        videoDao.observeProcessedCount().map { it as Any? },
        photoDao.observeCountByStatus(ProcessingStatus.FAVORITED).map { it as Any? },
        videoDao.observeCountByStatus(ProcessingStatus.FAVORITED).map { it as Any? },
        trashDao.observeCount().map { it as Any? },
        photoDao.observeDeleteCount().map { it as Any? },
        videoDao.observeDeleteCount().map { it as Any? },
        photoDao.observeDeleteSize().map { it as Any? },
        videoDao.observeDeleteSize().map { it as Any? },
        statsDao.observeTodayPhotoCount(todayLocalDate()).map { it as Any? },
        statsDao.observeTodayVideoCount(todayLocalDate()).map { it as Any? },
        statsDao.observeTodayActionCount(todayLocalDate()).map { it as Any? },
        statsDao.observeUserStats().map { it as Any? },
    ) { values ->
        val photoFavorites = values[4] as Int
        val videoFavorites = values[5] as Int
        val photoPendingDelete = values[7] as Int
        val videoPendingDelete = values[8] as Int
        val photoPendingDeleteBytes = values[9] as Long
        val videoPendingDeleteBytes = values[10] as Long
        DashboardStats(
            photoCount = values[0] as Int,
            videoCount = values[1] as Int,
            processedPhotoCount = values[2] as Int,
            processedVideoCount = values[3] as Int,
            favoriteCount = photoFavorites + videoFavorites,
            trashCount = values[6] as Int,
            pendingDeleteCount = photoPendingDelete + videoPendingDelete,
            pendingDeleteBytes = photoPendingDeleteBytes + videoPendingDeleteBytes,
            photoPendingDeleteCount = photoPendingDelete,
            videoPendingDeleteCount = videoPendingDelete,
            photoPendingDeleteBytes = photoPendingDeleteBytes,
            videoPendingDeleteBytes = videoPendingDeleteBytes,
            todayPhotoCount = values[11] as Int,
            todayVideoCount = values[12] as Int,
            todayActionCount = values[13] as Int,
            userStats = values[14] as? UserStatsEntity,
        )
    }

    override fun observePhotoTypeStats(): Flow<PhotoTypeStats> = combine(
        photoDao.observeActiveCount(),
        photoDao.observeTypeCount("normal"),
        photoDao.observeTypeCount("screenshot"),
        photoDao.observeTypeCount("selfie"),
        photoDao.observeTypeCount("motion"),
        photoDao.observeTypeCount("gif"),
        photoDao.observeTypeCount("long"),
    ) { values ->
        PhotoTypeStats(
            all = values[0] as Int,
            normal = values[1] as Int,
            screenshot = values[2] as Int,
            selfie = values[3] as Int,
            motion = values[4] as Int,
            gif = values[5] as Int,
            longImage = values[6] as Int,
        )
    }

    override fun observeRecentPhotos(limit: Int): Flow<List<PhotoEntity>> = photoDao.observeRecent(limit)
    override fun observeTimelinePhotos(limit: Int): Flow<List<PhotoEntity>> = photoDao.observeTimeline(limit)
    override fun observeTimelineVideos(limit: Int): Flow<List<VideoEntity>> = videoDao.observeTimeline(limit)
    override fun observeCleanedPhotoHistory(limit: Int): Flow<List<PhotoEntity>> = photoDao.observeCleanedHistory(limit)
    override fun observeTodayInHistory(limit: Int): Flow<List<PhotoEntity>> = photoDao.observeTodayInHistory(limit = limit)
    override fun observeTodayInHistoryVideos(limit: Int): Flow<List<VideoEntity>> = videoDao.observeTodayInHistory(limit = limit)
    override fun observeRecentlyAddedPhotos(days: Int, limit: Int): Flow<List<PhotoEntity>> {
        val sinceSeconds = (System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L) / 1000L
        return photoDao.observeRecentlyAdded(sinceSeconds, limit)
    }
    override fun observePhotoFolders(): Flow<List<String>> = photoDao.observeFolderPaths()
    override fun observeVideoFolders(): Flow<List<String>> = videoDao.observeFolderPaths()
    override fun observeRecentVideos(limit: Int): Flow<List<VideoEntity>> = videoDao.observeRecent(limit)
    override fun observeFavoritePhotos(limit: Int): Flow<List<PhotoEntity>> = photoDao.observeFavorites(limit)
    override fun observeFavoriteVideos(limit: Int): Flow<List<VideoEntity>> = videoDao.observeFavorites(limit)
    override fun observeTrash(): Flow<List<TrashItemEntity>> = trashDao.observeTrash()
    override fun observeSimilarGroups(): Flow<List<SimilarGroupEntity>> = similarDao.observeGroups()

    override suspend fun refreshMediaLibrary(options: MediaLibrarySyncOptions): Pair<Int, Int> = withContext(Dispatchers.IO) {
        // Scan images and videos independently. A slow or failing image provider row should not
        // prevent the video library from being discovered, and vice versa. This also protects the
        // first permission sync from showing an empty library just because one MediaStore query failed.
        val photoScan = runCatching { scanner.scanImages().distinctBy { it.mediaStoreId } }
        val videoScan = runCatching { scanner.scanVideos().distinctBy { it.mediaStoreId } }
        if (photoScan.isFailure && videoScan.isFailure) {
            throw photoScan.exceptionOrNull() ?: videoScan.exceptionOrNull() ?: IllegalStateException("媒体库同步失败")
        }
        val scannedPhotosRaw = photoScan.getOrElse { emptyList() }
        val scannedVideosRaw = videoScan.getOrElse { emptyList() }
        val (scannedPhotos, scannedVideos) = linkSeparateDynamicPhotoPairs(scannedPhotosRaw, scannedVideosRaw)
        val now = System.currentTimeMillis()

        val existingPhotos = if (scannedPhotos.isEmpty()) {
            emptyMap()
        } else {
            scannedPhotos
                .map { it.mediaStoreId }
                .chunked(SQLITE_BIND_BATCH_SIZE)
                .flatMap { photoDao.byMediaStoreIds(it) }
                .associateBy { it.mediaStoreId }
        }
        val existingVideos = if (scannedVideos.isEmpty()) {
            emptyMap()
        } else {
            scannedVideos
                .map { it.mediaStoreId }
                .chunked(SQLITE_BIND_BATCH_SIZE)
                .flatMap { videoDao.byMediaStoreIds(it) }
                .associateBy { it.mediaStoreId }
        }

        val mergedPhotos = scannedPhotos.map { fresh ->
            val old = existingPhotos[fresh.mediaStoreId]
            if (old == null) {
                fresh
            } else {
                fresh.copy(
                    id = old.id,
                    processingStatus = old.processingStatus,
                    deletionStatus = old.deletionStatus,
                    processedAt = old.processedAt,
                    deletedAt = old.deletedAt,
                    deleteError = old.deleteError,
                    createdAt = old.createdAt,
                    updatedAt = now,
                )
            }
        }
        val mergedVideos = scannedVideos.map { fresh ->
            val old = existingVideos[fresh.mediaStoreId]
            if (old == null) {
                fresh
            } else {
                fresh.copy(
                    id = old.id,
                    processingStatus = old.processingStatus,
                    deletionStatus = old.deletionStatus,
                    processedAt = old.processedAt,
                    deletedAt = old.deletedAt,
                    deleteError = old.deleteError,
                    createdAt = old.createdAt,
                    updatedAt = now,
                )
            }
        }

        db.withTransaction {
            if (photoScan.isSuccess) {
                if (options.markMissingPhotos) photoDao.markAllMissing(now)
                if (mergedPhotos.isNotEmpty()) photoDao.upsertAll(mergedPhotos)
            }
            if (videoScan.isSuccess) {
                if (options.markMissingVideos) videoDao.markAllMissing(now)
                if (mergedVideos.isNotEmpty()) videoDao.upsertAll(mergedVideos)
            }
        }
        mergedPhotos.size to mergedVideos.size
    }


    private fun linkSeparateDynamicPhotoPairs(
        photos: List<PhotoEntity>,
        videos: List<VideoEntity>,
    ): Pair<List<PhotoEntity>, List<VideoEntity>> {
        if (photos.isEmpty() || videos.isEmpty()) return photos to videos
        val shortVideosByKey = videos
            .filter { video -> video.duration in 500L..12_000L && video.size in 1L..80L * 1024L * 1024L }
            .groupBy { video -> dynamicPairKey(video.folderPath, video.displayName) }

        val pairedVideoIds = mutableSetOf<Long>()
        val patchedPhotos = photos.map { photo ->
            val key = dynamicPairKey(photo.folderPath, photo.displayName)
            val companion = shortVideosByKey[key]
                ?.firstOrNull { video -> video.folderPath == photo.folderPath || video.folderName == photo.folderName }
            if (companion != null) {
                pairedVideoIds += companion.mediaStoreId
                photo.copy(
                    isMotionPhoto = true,
                    motionVideoUri = companion.uri,
                    isSeparateVideo = true,
                    motionPhotoNeedsDetection = true,
                )
            } else {
                photo
            }
        }
        val patchedVideos = videos.map { video ->
            if (video.mediaStoreId in pairedVideoIds) video.copy(isMotionPhotoVideo = true) else video
        }
        return patchedPhotos to patchedVideos
    }

    private fun dynamicPairKey(folderPath: String?, displayName: String): String {
        val folder = folderPath.orEmpty().trim('/').lowercase(Locale.ROOT)
        val rawStem = displayName.substringBeforeLast('.', displayName).lowercase(Locale.ROOT)
        val stem = rawStem
            .removeSuffix("_mp")
            .removeSuffix("_motion")
            .removeSuffix("_live")
            .removeSuffix("_dynamic")
            .removeSuffix("_video")
            .removeSuffix("-motion")
            .removeSuffix("-live")
            .removeSuffix("-video")
        return folder + "|" + stem
    }

    override suspend fun loadPhotoDeck(scope: CleaningScope): List<PhotoEntity> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        val range = rangeFor(scope.dateMode)
        val monthTokens = monthTokensForDateMode(scope.dateMode)
        val loadLimit = (
            scope.deckLoadWindow.takeIf { it > 0 }
                ?: scope.sessionTarget.takeIf { it > 0 }
                ?: settings.photoBatchSize
            ).coerceAtLeast(1)
        val randomMode = scope.sortOrder == "random"
        val randomQueryLimit = if (loadLimit <= 24) {
            (loadLimit * 120).coerceIn(240, 1200)
        } else {
            (loadLimit * 18).coerceIn(4000, 16000)
        }
        val queryLimit = if (settings.excludedFolderPaths.isEmpty()) {
            when {
                randomMode -> randomQueryLimit
                else -> (loadLimit * 6).coerceAtLeast(120)
            }
        } else {
            if (randomMode) (randomQueryLimit * 2).coerceAtMost(24000) else 20000
        }
        val photos = photoDao.nextFilteredBatch(
            folderPath = scope.folderPaths.firstOrNull(),
            startMillis = range?.first,
            endMillis = range?.second,
            multiMonthEnabled = monthTokens.isNotEmpty(),
            monthTokens = monthTokens.ifEmpty { listOf("") },
            mediaType = scope.mediaType,
            randomOrder = randomMode,
            randomSeed = scope.randomSeed,
            todayOnly = scope.todayInHistory,
            limit = queryLimit,
        )
            .filter { dateModeMatches(it.dateTaken, scope.dateMode) }
            .filterNot { isExcludedFolder(it.folderPath, settings.excludedFolderPaths) }
        if (randomMode) {
            spreadRandomPhotosForCleaning(photos, scope.randomSeed, loadLimit)
        } else {
            photos.take(loadLimit)
        }
    }

    override suspend fun loadVideoDeck(scope: CleaningScope): List<VideoEntity> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        val range = rangeFor(scope.dateMode)
        val monthTokens = monthTokensForDateMode(scope.dateMode)
        val loadLimit = (
            scope.deckLoadWindow.takeIf { it > 0 }
                ?: scope.sessionTarget.takeIf { it > 0 }
                ?: settings.videoBatchSize
            ).coerceAtLeast(1)
        val queryLimit = if (settings.excludedFolderPaths.isEmpty()) {
            (loadLimit * 6).coerceAtLeast(120)
        } else {
            5000
        }
        val videos = videoDao.nextFilteredBatch(
            folderPath = scope.folderPaths.firstOrNull(),
            startMillis = range?.first,
            endMillis = range?.second,
            multiMonthEnabled = monthTokens.isNotEmpty(),
            monthTokens = monthTokens.ifEmpty { listOf("") },
            randomOrder = scope.sortOrder == "random",
            randomSeed = scope.randomSeed,
            limit = queryLimit,
        )
            .filter { dateModeMatches(it.dateTaken, scope.dateMode) }
            .filterNot { isExcludedFolder(it.folderPath, settings.excludedFolderPaths) }
        if (scope.sortOrder == "random") {
            spreadRandomVideosByFolder(videos, scope.randomSeed, loadLimit)
        } else {
            videos.take(loadLimit)
        }
    }

    private data class ShufflePhotoBucket(
        val dayKey: String,
        val monthKey: String,
        val folderKey: String,
        val photos: MutableList<PhotoEntity>,
    )

    private data class ShufflePhotoKey(
        val dayKey: String,
        val monthKey: String,
        val folderKey: String,
    )

    private data class ShuffleVideoFolderBucket(
        val folderKey: String,
        val videos: MutableList<VideoEntity>,
    )

    private fun spreadRandomPhotosForCleaning(
        photos: List<PhotoEntity>,
        randomSeed: Long,
        limit: Int,
    ): List<PhotoEntity> {
        if (photos.size <= 2 || limit <= 0) return photos.take(limit.coerceAtLeast(0))
        val random = Random(randomSeed xor (photos.size.toLong() * 1_103_515_245L))
        val buckets = photos
            .groupBy { photoShuffleKey(it) }
            .map { (key, bucketPhotos) ->
                val shuffled = bucketPhotos.toMutableList()
                java.util.Collections.shuffle(shuffled, random)
                ShufflePhotoBucket(
                    dayKey = key.dayKey,
                    monthKey = key.monthKey,
                    folderKey = key.folderKey,
                    photos = shuffled,
                )
            }
            .toMutableList()
        java.util.Collections.shuffle(buckets, random)

        val result = ArrayList<PhotoEntity>(limit.coerceAtMost(photos.size))
        val recentDays = ArrayDeque<String>()
        val recentMonths = ArrayDeque<String>()
        val recentFolders = ArrayDeque<String>()
        while (result.size < limit && buckets.any { it.photos.isNotEmpty() }) {
            val available = buckets.filter { it.photos.isNotEmpty() }
            val differentDay = available.filter { it.dayKey !in recentDays }
            val differentMonth = differentDay.filter { it.monthKey !in recentMonths }
            val differentFolder = differentMonth.filter { it.folderKey !in recentFolders }
            val candidateBuckets = when {
                differentFolder.isNotEmpty() -> differentFolder
                differentMonth.isNotEmpty() -> differentMonth
                differentDay.isNotEmpty() -> differentDay
                else -> available
            }
            val selectedBucket = candidateBuckets.maxByOrNull { bucket ->
                bucket.photos.size.coerceAtMost(5) * 8 + random.nextInt(1024)
            } ?: break
            val next = selectedBucket.photos.removeAt(0)
            result += next
            recentDays.addLast(selectedBucket.dayKey)
            recentMonths.addLast(selectedBucket.monthKey)
            recentFolders.addLast(selectedBucket.folderKey)
            while (recentDays.size > 6) recentDays.removeFirst()
            while (recentMonths.size > 3) recentMonths.removeFirst()
            while (recentFolders.size > 4) recentFolders.removeFirst()
        }
        return result
    }

    private fun photoShuffleKey(photo: PhotoEntity): ShufflePhotoKey {
        val folderKey = photoShuffleFolderKey(photo)
        if (photo.dateTaken <= 0L) {
            val group = Math.floorMod(photo.mediaStoreId, 31L).toString().padStart(2, '0')
            return ShufflePhotoKey(dayKey = "unknown-" + group, monthKey = "unknown", folderKey = folderKey)
        }
        val cal = Calendar.getInstance().apply { timeInMillis = photo.dateTaken }
        val year = cal.get(Calendar.YEAR).toString()
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val monthKey = year + "-" + month
        return ShufflePhotoKey(dayKey = monthKey + "-" + day, monthKey = monthKey, folderKey = folderKey)
    }

    private fun photoShuffleFolderKey(photo: PhotoEntity): String {
        val normalized = photo.folderPath.trim().trim('/').lowercase(Locale.ROOT)
        return normalized.ifBlank {
            photo.folderName.trim().lowercase(Locale.ROOT).ifBlank {
                "unknown-" + Math.floorMod(photo.mediaStoreId, 17L)
            }
        }
    }

    private fun spreadRandomVideosByFolder(
        videos: List<VideoEntity>,
        randomSeed: Long,
        limit: Int,
    ): List<VideoEntity> {
        if (videos.size <= 2 || limit <= 0) return videos.take(limit.coerceAtLeast(0))
        val random = Random(randomSeed xor (videos.size.toLong() * 2_654_435_761L))
        val buckets = videos
            .groupBy { videoShuffleFolderKey(it) }
            .map { (folderKey, folderVideos) ->
                val shuffled = folderVideos.toMutableList()
                java.util.Collections.shuffle(shuffled, random)
                ShuffleVideoFolderBucket(folderKey = folderKey, videos = shuffled)
            }
            .toMutableList()
        java.util.Collections.shuffle(buckets, random)

        val result = ArrayList<VideoEntity>(limit.coerceAtMost(videos.size))
        val recentFolders = ArrayDeque<String>()
        while (result.size < limit && buckets.any { it.videos.isNotEmpty() }) {
            val available = buckets.filter { it.videos.isNotEmpty() }
            val lessRecent = available.filter { it.folderKey !in recentFolders }
            val candidateBuckets = lessRecent.ifEmpty { available }
            val selectedBucket = candidateBuckets.maxByOrNull { bucket ->
                bucket.videos.size * 10 + random.nextInt(512)
            } ?: break
            result += selectedBucket.videos.removeAt(0)
            recentFolders.addLast(selectedBucket.folderKey)
            while (recentFolders.size > 3) recentFolders.removeFirst()
        }
        return result
    }

    private fun videoShuffleFolderKey(video: VideoEntity): String {
        val normalized = video.folderPath.trim().trim('/').lowercase(Locale.ROOT)
        return normalized.ifBlank {
            video.folderName.trim().lowercase(Locale.ROOT).ifBlank {
                "unknown-" + Math.floorMod(video.mediaStoreId, 17L)
            }
        }
    }

    override suspend fun loadPhotoMemoryWindow(
        currentPhoto: PhotoEntity,
        dayCount: Int,
        limit: Int,
    ): List<PhotoEntity> = withContext(Dispatchers.IO) {
        val safeDayCount = dayCount.coerceIn(1, 14)
        val anchorMillis = photoMemoryMillis(currentPhoto)
        val (anchorStart, anchorEnd) = dayRangeForMillis(anchorMillis)
        val anchorDayKey = memoryDayKey(anchorMillis)
        val sideBudget = safeDayCount - 1
        val preferredBefore = sideBudget / 2
        val preferredAfter = sideBudget - preferredBefore
        val beforeKeys = photoDao
            .memoryDayKeysBefore(anchorStart, sideBudget)
            .filter { it.isNotBlank() && it != anchorDayKey }
            .distinct()
            .sorted()
        val afterKeys = photoDao
            .memoryDayKeysAfter(anchorEnd, sideBudget)
            .filter { it.isNotBlank() && it != anchorDayKey }
            .distinct()
            .sorted()
        var beforeTake = preferredBefore.coerceAtMost(beforeKeys.size)
        var afterTake = preferredAfter.coerceAtMost(afterKeys.size)
        if (beforeTake < preferredBefore) {
            afterTake = (afterTake + (preferredBefore - beforeTake)).coerceAtMost(afterKeys.size)
        }
        if (afterTake < preferredAfter) {
            beforeTake = (beforeTake + (preferredAfter - afterTake)).coerceAtMost(beforeKeys.size)
        }
        val selectedDayKeys = (beforeKeys.takeLast(beforeTake) + anchorDayKey + afterKeys.take(afterTake)).distinct()
        if (selectedDayKeys.isEmpty()) return@withContext listOf(currentPhoto)
        val photos = photoDao.photosForMemoryDayKeys(selectedDayKeys, limit.coerceAtLeast(32))
            .distinctBy { it.mediaStoreId }
        photos.ifEmpty { listOf(currentPhoto) }
    }

    override suspend fun handlePhotoAction(
        photo: PhotoEntity,
        action: SwipeAction,
        systemTrashAuthorized: Boolean,
    ): Long = withContext(Dispatchers.IO) {
        val current = photoDao.byId(photo.id) ?: photo
        val actionAllowed = current.deletionStatus == DeletionStatus.NONE
        val settings = if (action == SwipeAction.Delete) settingsRepository.settings.first() else null
        val systemTrashRequested = action == SwipeAction.Delete &&
            (settings?.deleteMode == DeleteMode.SYSTEM_TRASH || systemTrashAuthorized)
        if (systemTrashRequested && !systemTrashAuthorized) {
            throw IllegalStateException("系统回收站操作需要先完成用户确认")
        }
        val targetProcessing = when (action) {
            SwipeAction.Keep -> ProcessingStatus.KEPT
            SwipeAction.Favorite -> ProcessingStatus.FAVORITED
            SwipeAction.Delete -> current.processingStatus
        }
        val newProcessing = if (actionAllowed) targetProcessing else current.processingStatus
        val newDeletion = if (action == SwipeAction.Delete && actionAllowed) {
            if (systemTrashRequested) DeletionStatus.TRASHED else DeletionStatus.PENDING
        } else {
            current.deletionStatus
        }
        val operationId = recordOperation(current.id, "photo", current.processingStatus, current.deletionStatus, newProcessing, newDeletion, action.name)
        when (action) {
            SwipeAction.Keep -> {
                if (actionAllowed && current.processingStatus != ProcessingStatus.KEPT) {
                    photoDao.updateProcessingStatus(current.id, ProcessingStatus.KEPT)
                    if (current.processingStatus == ProcessingStatus.UNPROCESSED) {
                        bumpStats(photoDelta = 1, sourceRef = operationSourceRef(operationId))
                    }
                }
            }
            SwipeAction.Favorite -> {
                if (actionAllowed && current.processingStatus != ProcessingStatus.FAVORITED) {
                    photoDao.updateProcessingStatus(current.id, ProcessingStatus.FAVORITED)
                    bumpStats(
                        favoriteDelta = 1,
                        photoDelta = if (current.processingStatus == ProcessingStatus.UNPROCESSED) 1 else 0,
                        sourceRef = operationSourceRef(operationId),
                    )
                }
            }
            SwipeAction.Delete -> {
                if (actionAllowed) {
                    photoDao.updateDeletionStatus(
                        listOf(current.id),
                        if (systemTrashRequested) DeletionStatus.TRASHED else DeletionStatus.PENDING,
                    )
                    trashDao.deleteByMedia(current.id, "photo")
                    trashDao.insert(current.toTrashItem())
                    bumpStats(
                        photoDelta = if (current.processingStatus == ProcessingStatus.UNPROCESSED) 1 else 0,
                        deletedSize = current.size,
                        sourceRef = operationSourceRef(operationId),
                    )
                }
            }
        }
        operationId
    }

    override suspend fun handleVideoAction(
        video: VideoEntity,
        action: SwipeAction,
        systemTrashAuthorized: Boolean,
    ): Long = withContext(Dispatchers.IO) {
        val current = videoDao.byId(video.id) ?: video
        val actionAllowed = current.deletionStatus == DeletionStatus.NONE
        val settings = if (action == SwipeAction.Delete) settingsRepository.settings.first() else null
        val systemTrashRequested = action == SwipeAction.Delete &&
            (settings?.deleteMode == DeleteMode.SYSTEM_TRASH || systemTrashAuthorized)
        if (systemTrashRequested && !systemTrashAuthorized) {
            throw IllegalStateException("系统回收站操作需要先完成用户确认")
        }
        val targetProcessing = when (action) {
            SwipeAction.Keep -> ProcessingStatus.KEPT
            SwipeAction.Favorite -> ProcessingStatus.FAVORITED
            SwipeAction.Delete -> current.processingStatus
        }
        val newProcessing = if (actionAllowed) targetProcessing else current.processingStatus
        val newDeletion = if (action == SwipeAction.Delete && actionAllowed) {
            if (systemTrashRequested) DeletionStatus.TRASHED else DeletionStatus.PENDING
        } else {
            current.deletionStatus
        }
        val operationId = recordOperation(current.id, "video", current.processingStatus, current.deletionStatus, newProcessing, newDeletion, action.name)
        when (action) {
            SwipeAction.Keep -> {
                if (actionAllowed && current.processingStatus != ProcessingStatus.KEPT) {
                    videoDao.updateProcessingStatus(current.id, ProcessingStatus.KEPT)
                    if (current.processingStatus == ProcessingStatus.UNPROCESSED) {
                        bumpStats(videoDelta = 1, sourceRef = operationSourceRef(operationId))
                    }
                }
            }
            SwipeAction.Favorite -> {
                if (actionAllowed && current.processingStatus != ProcessingStatus.FAVORITED) {
                    videoDao.updateProcessingStatus(current.id, ProcessingStatus.FAVORITED)
                    bumpStats(
                        favoriteDelta = 1,
                        videoDelta = if (current.processingStatus == ProcessingStatus.UNPROCESSED) 1 else 0,
                        sourceRef = operationSourceRef(operationId),
                    )
                }
            }
            SwipeAction.Delete -> {
                if (actionAllowed) {
                    videoDao.updateDeletionStatus(
                        listOf(current.id),
                        if (systemTrashRequested) DeletionStatus.TRASHED else DeletionStatus.PENDING,
                    )
                    trashDao.deleteByMedia(current.id, "video")
                    trashDao.insert(current.toTrashItem())
                    bumpStats(
                        videoDelta = if (current.processingStatus == ProcessingStatus.UNPROCESSED) 1 else 0,
                        deletedSize = current.size,
                        sourceRef = operationSourceRef(operationId),
                    )
                }
            }
        }
        operationId
    }

    override suspend fun createMediaAuthorizationRequest(
        kind: MediaOperationKind,
        mediaUris: Collection<String>,
        isTrashed: Boolean,
    ): MediaAuthorizationResult = withContext(Dispatchers.IO) {
        when (kind) {
            MediaOperationKind.WRITE -> mediaStoreActions.createWriteAuthorizationRequest(mediaUris)
            MediaOperationKind.TRASH,
            MediaOperationKind.RESTORE,
            -> mediaStoreActions.createTrashAuthorizationRequest(
                mediaUris = mediaUris,
                isTrashed = kind == MediaOperationKind.TRASH || isTrashed,
            )
            MediaOperationKind.PERMANENT_DELETE -> mediaStoreActions.createDeleteAuthorizationRequest(mediaUris)
        }
    }

    override suspend fun prepareTrashRestore(trashIds: Collection<Long>): TrashRestoreAuthorizationPlan =
        withContext(Dispatchers.IO) {
            val items = trashIds.distinct().mapNotNull { trashDao.byId(it) }
            val systemTrashItems = items.filter { isTrashItemInSystemTrash(it) }
            val systemTrashIds = systemTrashItems.map { it.id }.toSet()
            val localOnlyIds = items.filterNot { it.id in systemTrashIds }.map { it.id }
            val authorization = if (systemTrashItems.isEmpty()) {
                MediaAuthorizationResult.Completed
            } else {
                createMediaAuthorizationRequest(
                    kind = MediaOperationKind.RESTORE,
                    mediaUris = systemTrashItems.map { it.uri },
                )
            }
            TrashRestoreAuthorizationPlan(
                localOnlyTrashIds = localOnlyIds,
                systemTrashIds = systemTrashItems.map { it.id },
                authorization = authorization,
            )
        }

    override suspend fun movePhotoToFolder(
        photo: PhotoEntity,
        targetRelativePath: String,
        writeAuthorized: Boolean,
    ): MovePhotoResult = withContext(Dispatchers.IO) {
        if (!writeAuthorized) return@withContext MovePhotoResult(false, "移动媒体需要先完成系统写入授权")
        val normalizedPath = MediaStoreActions.normalizeRelativePath(targetRelativePath)
        when (val result = mediaStoreActions.moveMediaToFolder(photo.uri, normalizedPath)) {
            MediaOperationResult.Success -> {
                photoDao.updateFolder(
                    id = photo.id,
                    relativePath = normalizedPath,
                    folderName = MediaStoreActions.folderNameOf(normalizedPath),
                )
                MovePhotoResult(true, "已移动到 ${MediaStoreActions.folderNameOf(normalizedPath)}")
            }
            is MediaOperationResult.Failed -> MovePhotoResult(false, result.reason)
        }
    }

    override suspend fun moveVideoToFolder(
        video: VideoEntity,
        targetRelativePath: String,
        writeAuthorized: Boolean,
    ): MovePhotoResult = withContext(Dispatchers.IO) {
        if (!writeAuthorized) return@withContext MovePhotoResult(false, "移动媒体需要先完成系统写入授权")
        val normalizedPath = MediaStoreActions.normalizeRelativePath(targetRelativePath)
        when (val result = mediaStoreActions.moveMediaToFolder(video.uri, normalizedPath)) {
            MediaOperationResult.Success -> {
                videoDao.updateFolder(
                    id = video.id,
                    relativePath = normalizedPath,
                    folderName = MediaStoreActions.folderNameOf(normalizedPath),
                )
                MovePhotoResult(true, "已移动到 ${MediaStoreActions.folderNameOf(normalizedPath)}")
            }
            is MediaOperationResult.Failed -> MovePhotoResult(false, result.reason)
        }
    }

    override suspend fun generateSimilarGroups(
        limit: Int,
        onProgress: (processed: Int, total: Int, stage: String) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val rawPhotos = photoDao.allActive(limit)
        val photos = rawPhotos.filter { photo ->
            photo.deletionStatus == DeletionStatus.NONE &&
                !photo.isRaw &&
                !photo.isGif &&
                photo.width > 0 &&
                photo.height > 0
        }
        val total = photos.size
        onProgress(0, total, if (rawPhotos.size == photos.size) "读取照片索引" else "已跳过 RAW/GIF 等不适合快速检测的文件")
        if (total < 2) return@withContext 0

        val activeIds = photos.map { it.id }.toHashSet()
        val cachedFingerprints = similarDao.allFingerprints()
            .filter { it.photoId in activeIds && it.colorHistogram.contains("rgb=") }
            .associateBy { it.photoId }
        val missingPhotos = photos.filterNot { it.id in cachedFingerprints }
        val now = System.currentTimeMillis()
        val pendingFingerprints = mutableListOf<com.futureape.kanleme.data.local.PhotoFingerprintEntity>()

        onProgress(cachedFingerprints.size.coerceAtMost(total), total, "复用已完成的缩略指纹")
        missingPhotos.forEachIndexed { index, photo ->
            val fp = similarPhotoAnalyzer.fingerprintOrNull(photo, now)
            if (fp != null) pendingFingerprints += fp
            if (pendingFingerprints.size >= 24) {
                similarDao.upsertFingerprints(pendingFingerprints.toList())
                pendingFingerprints.clear()
            }
            val processed = (cachedFingerprints.size + index + 1).coerceAtMost(total)
            if (index % 2 == 0 || index == missingPhotos.lastIndex) {
                onProgress(processed, total, "正在计算缩略指纹")
            }
            yield()
        }
        if (pendingFingerprints.isNotEmpty()) {
            similarDao.upsertFingerprints(pendingFingerprints.toList())
            pendingFingerprints.clear()
        }

        val fingerprints = similarDao.allFingerprints().filter { it.photoId in activeIds && it.colorHistogram.contains("rgb=") }
        onProgress(total, total, "正在聚类相似照片")
        yield()
        val result = similarPhotoAnalyzer.buildGroups(photos = photos, fingerprints = fingerprints, now = now)
        onProgress(total, total, "正在保存检测结果")
        similarDao.clearGroupPhotos()
        similarDao.clearGroups()
        result.groups.chunked(80).forEach { similarDao.upsertGroups(it) }
        result.members.chunked(240).forEach { similarDao.upsertGroupPhotos(it) }
        result.groups.size
    }

    override suspend fun similarGroupDetail(groupId: String): SimilarGroupDetail? = withContext(Dispatchers.IO) {
        val group = similarDao.observeGroups().first().firstOrNull { it.id == groupId } ?: return@withContext null
        SimilarGroupDetail(group, similarDao.groupPhotoEntities(groupId))
    }

    override suspend fun restoreTrashItem(trashId: Long) = withContext(Dispatchers.IO) {
        val item = trashDao.byId(trashId) ?: return@withContext
        if (isTrashItemInSystemTrash(item)) {
            throw IllegalStateException("系统媒体恢复需要先完成用户确认")
        }
        restoreTrashItemLocally(item)
    }

    override suspend fun restoreTrashItemsAfterSystemAuthorization(trashIds: Collection<Long>) =
        withContext(Dispatchers.IO) {
            trashIds.distinct().mapNotNull { trashDao.byId(it) }.forEach { restoreTrashItemLocally(it) }
        }

    override suspend fun restoreAllTrash() = withContext(Dispatchers.IO) {
        observeTrash().first().forEach { restoreTrashItem(it.id) }
    }

    override suspend fun finalizePermanentDelete(trashIds: Collection<Long>): PermanentDeleteFinalization =
        withContext(Dispatchers.IO) {
            val items = trashIds.distinct().mapNotNull { trashDao.byId(it) }
            if (items.isEmpty()) {
                return@withContext PermanentDeleteFinalization(
                    deletedTrashIds = emptyList(),
                    failedTrashIds = emptyList(),
                    result = MediaAuthorizationResult.Completed,
                )
            }
            when (val verification = mediaStoreActions.verifyMediaDeleted(items.map { it.uri })) {
                MediaAuthorizationResult.Completed -> {
                    items.forEach { markTrashItemDeletedLocally(it) }
                    PermanentDeleteFinalization(
                        deletedTrashIds = items.map { it.id },
                        failedTrashIds = emptyList(),
                        result = verification,
                    )
                }
                is MediaAuthorizationResult.PartialFailure -> {
                    val deletedUris = verification.succeededUris.map { it.toString() }.toSet()
                    val deletedItems = items.filter { it.uri in deletedUris }
                    val deletedIds = deletedItems.map { it.id }.toSet()
                    deletedItems.forEach { markTrashItemDeletedLocally(it) }
                    PermanentDeleteFinalization(
                        deletedTrashIds = deletedItems.map { it.id },
                        failedTrashIds = items.filterNot { it.id in deletedIds }.map { it.id },
                        result = verification,
                    )
                }
                is MediaAuthorizationResult.Failed -> PermanentDeleteFinalization(
                    deletedTrashIds = emptyList(),
                    failedTrashIds = items.map { it.id },
                    result = verification,
                )
                is MediaAuthorizationResult.NeedsUserConsent,
                is MediaAuthorizationResult.Cancelled,
                -> PermanentDeleteFinalization(
                    deletedTrashIds = emptyList(),
                    failedTrashIds = items.map { it.id },
                    result = verification,
                )
            }
        }

    override suspend fun createUndoAuthorizationRequest(mediaType: String?): MediaAuthorizationResult =
        withContext(Dispatchers.IO) {
            val operation = operationDao.lastUndoable(mediaType) ?: return@withContext MediaAuthorizationResult.Completed
            val currentPhoto = if (operation.mediaType == "photo") photoDao.byId(operation.mediaId) else null
            val currentVideo = if (operation.mediaType == "video") videoDao.byId(operation.mediaId) else null
            val currentDeletionStatus = currentPhoto?.deletionStatus ?: currentVideo?.deletionStatus
            val currentUri = currentPhoto?.uri ?: currentVideo?.uri
            if (
                operation.action != SwipeAction.Delete.name ||
                currentDeletionStatus != DeletionStatus.TRASHED ||
                currentUri.isNullOrBlank()
            ) {
                MediaAuthorizationResult.Completed
            } else {
                createMediaAuthorizationRequest(
                    kind = MediaOperationKind.RESTORE,
                    mediaUris = listOf(currentUri),
                )
            }
        }

    override suspend fun undoLastActionAfterSystemAuthorization(mediaType: String?): Boolean =
        withContext(Dispatchers.IO) {
            val operation = operationDao.lastUndoable(mediaType) ?: return@withContext false
            undoOperation(operation, restoreSystemTrash = false)
            true
        }

    override suspend fun undoOperationAfterSystemAuthorization(operationId: Long): Boolean =
        withContext(Dispatchers.IO) {
            val operation = operationDao.undoableById(operationId) ?: return@withContext false
            undoOperation(operation, restoreSystemTrash = false)
            true
        }

    private suspend fun isTrashItemInSystemTrash(item: TrashItemEntity): Boolean =
        if (item.mediaType == "photo") {
            photoDao.byId(item.mediaId)?.deletionStatus == DeletionStatus.TRASHED
        } else {
            videoDao.byId(item.mediaId)?.deletionStatus == DeletionStatus.TRASHED
        }

    private suspend fun restoreTrashItemLocally(item: TrashItemEntity) {
        val deleteOperation = operationDao.lastByMediaAndAction(item.mediaId, item.mediaType, SwipeAction.Delete.name)
        if (deleteOperation != null) {
            undoOperation(deleteOperation, restoreSystemTrash = false, undoDelta = 0)
            return
        }
        if (item.mediaType == "photo") {
            photoDao.restoreStatus(item.mediaId, ProcessingStatus.UNPROCESSED, DeletionStatus.NONE)
        } else {
            videoDao.restoreStatus(item.mediaId, ProcessingStatus.UNPROCESSED, DeletionStatus.NONE)
        }
        trashDao.deleteById(item.id)
    }

    private suspend fun markTrashItemDeletedLocally(item: TrashItemEntity) {
        if (item.mediaType == "photo") {
            photoDao.updateDeletionStatus(listOf(item.mediaId), DeletionStatus.DELETED)
        } else {
            videoDao.updateDeletionStatus(listOf(item.mediaId), DeletionStatus.DELETED)
        }
        trashDao.deleteById(item.id)
    }

    override suspend fun undoLastAction(mediaType: String?): Boolean = withContext(Dispatchers.IO) {
        val op = operationDao.lastUndoable(mediaType) ?: return@withContext false
        undoOperation(op)
        true
    }

    override suspend fun undoOperation(operationId: Long): Boolean = withContext(Dispatchers.IO) {
        val op = operationDao.undoableById(operationId) ?: return@withContext false
        undoOperation(op)
        true
    }

    override suspend fun buildAnnualReport(year: Int): AnnualReport = withContext(Dispatchers.IO) {
        val dashboard = observeDashboard().first()
        val stats = dashboard.userStats
        val folders = photoDao.allActive(3000).groupBy { it.folderName }.maxByOrNull { it.value.size }?.key ?: "Camera"
        AnnualReport(
            year = year,
            photoCount = dashboard.photoCount,
            videoCount = dashboard.videoCount,
            clearedCount = dashboard.processedCount,
            favoriteCount = dashboard.favoriteCount,
            deletedCount = dashboard.pendingDeleteCount,
            freedBytes = stats?.totalStorageFreed ?: 0L,
            topFolder = folders,
            styleTitle = when {
                dashboard.favoriteCount > dashboard.pendingDeleteCount -> "珍藏型整理者"
                dashboard.pendingDeleteCount > dashboard.favoriteCount * 2 -> "果断型整理者"
                else -> "稳健型整理者"
            },
            styleDescription = "这一年你更常整理 ${folders}，已处理 ${dashboard.processedCount} 个媒体文件。",
        )
    }

    override suspend fun buildAchievements(): List<AchievementUi> = withContext(Dispatchers.IO) {
        val dashboard = observeDashboard().first()
        val stats = dashboard.userStats ?: UserStatsEntity()
        achievementSpecs().map { spec ->
            val current = when (spec.metric) {
                "clear" -> dashboard.processedCount
                "favorite" -> dashboard.favoriteCount
                "delete" -> dashboard.pendingDeleteCount
                "storage" -> ((stats.totalStorageFreed / (1024 * 1024)).coerceAtLeast(0)).toInt()
                "streak" -> stats.currentStreak
                "undo" -> stats.totalUndoCount
                else -> dashboard.processedCount
            }
            AchievementUi(
                id = spec.id,
                title = spec.title,
                description = spec.description,
                difficulty = spec.difficulty,
                target = spec.target,
                current = current,
                category = spec.category,
                rarity = spec.rarity,
                xp = spec.xp,
                iconKey = spec.iconKey,
            )
        }
    }

    private suspend fun undoOperation(
        op: OperationHistoryEntity,
        restoreSystemTrash: Boolean = true,
        undoDelta: Int = 1,
    ) {
        val currentPhoto = if (op.mediaType == "photo") photoDao.byId(op.mediaId) else null
        val currentVideo = if (op.mediaType == "video") videoDao.byId(op.mediaId) else null
        val currentDeletionStatus = currentPhoto?.deletionStatus ?: currentVideo?.deletionStatus
        val currentUri = currentPhoto?.uri ?: currentVideo?.uri
        if (
            restoreSystemTrash &&
            op.action == SwipeAction.Delete.name &&
            currentDeletionStatus == DeletionStatus.TRASHED &&
            currentUri != null
        ) {
            throw IllegalStateException("系统媒体恢复需要先完成用户确认")
        }

        val adjustment = statsAdjustmentForUndo(op, currentPhoto?.size ?: currentVideo?.size ?: 0L)
        db.withTransaction {
            if (op.mediaType == "photo") {
                photoDao.restoreStatus(op.mediaId, op.previousProcessingStatus, op.previousDeletionStatus)
            } else {
                videoDao.restoreStatus(op.mediaId, op.previousProcessingStatus, op.previousDeletionStatus)
            }
            if (op.action == SwipeAction.Delete.name) trashDao.deleteByMedia(op.mediaId, op.mediaType)
            operationDao.markUndone(op.id)
            adjustStatsInTransaction(
                photoDelta = adjustment.photoDelta,
                videoDelta = adjustment.videoDelta,
                favoriteDelta = adjustment.favoriteDelta,
                deletedSize = adjustment.storageDelta,
                undoDelta = undoDelta,
                cleanupEventSourceRefToDelete = operationSourceRef(op.id),
            )
        }
    }

    private suspend fun recordOperation(
        mediaId: Long,
        mediaType: String,
        previousProcessing: String,
        previousDeletion: String,
        newProcessing: String,
        newDeletion: String,
        action: String,
    ): Long {
        return operationDao.insert(
            OperationHistoryEntity(
                mediaId = mediaId,
                mediaType = mediaType,
                previousProcessingStatus = previousProcessing,
                previousDeletionStatus = previousDeletion,
                newProcessingStatus = newProcessing,
                newDeletionStatus = newDeletion,
                action = action,
            )
        )
    }

    private suspend fun bumpStats(
        photoDelta: Int = 0,
        videoDelta: Int = 0,
        favoriteDelta: Int = 0,
        deletedSize: Long = 0L,
        undoDelta: Int = 0,
        sourceRef: String? = null,
    ) {
        db.withTransaction {
            adjustStatsInTransaction(
                photoDelta = photoDelta,
                videoDelta = videoDelta,
                favoriteDelta = favoriteDelta,
                deletedSize = deletedSize,
                undoDelta = undoDelta,
                cleanupEventSourceRefToDelete = null,
                sourceRef = sourceRef,
            )
        }
    }

    private suspend fun adjustStatsInTransaction(
        photoDelta: Int,
        videoDelta: Int,
        favoriteDelta: Int,
        deletedSize: Long,
        undoDelta: Int,
        cleanupEventSourceRefToDelete: String?,
        sourceRef: String? = null,
    ) {
        val now = System.currentTimeMillis()
        statsDao.insertUserStatsIfAbsent(UserStatsEntity())
        statsDao.adjustUserStats(
            photoDelta = photoDelta,
            videoDelta = videoDelta,
            storageDelta = deletedSize,
            favoriteDelta = favoriteDelta,
            undoDelta = undoDelta,
            now = now,
        )
        cleanupEventSourceRefToDelete?.let { statsDao.deleteCleanupEventsBySourceRef(it) }
        if (photoDelta > 0 || videoDelta > 0 || favoriteDelta > 0 || deletedSize > 0L) {
            statsDao.insertCleanupEvent(
                CleanupEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    eventType = "clean_action",
                    photoCount = photoDelta,
                    videoCount = videoDelta,
                    freedBytes = deletedSize,
                    localDate = todayLocalDate(now),
                    occurredAt = now,
                    sourceType = "organizer",
                    sourceRef = sourceRef,
                    createdAt = now,
                )
            )
        }
    }

    private fun operationSourceRef(operationId: Long): String = "operation:" + operationId

    private fun isExcludedFolder(folderPath: String?, excludedRules: Set<String>): Boolean {
        if (excludedRules.isEmpty()) return false
        val normalizedPath = normalizeFolderPath(folderPath.orEmpty())
        if (normalizedPath.isBlank()) return false
        val folderName = normalizedPath.trim('/').substringAfterLast('/')
        return excludedRules.any { rule ->
            val normalizedRule = normalizeFolderPath(rule)
            if (normalizedRule.isBlank()) {
                false
            } else {
                val ruleBody = normalizedRule.trim('/')
                val ruleName = ruleBody.substringAfterLast('/')
                normalizedPath == normalizedRule ||
                    normalizedPath.startsWith(normalizedRule) ||
                    normalizedPath.endsWith("/" + ruleBody + "/") ||
                    folderName.equals(ruleBody, ignoreCase = true) ||
                    folderName.equals(ruleName, ignoreCase = true)
            }
        }
    }

    private fun normalizeFolderPath(path: String): String {
        val cleaned = path.trim().replace('\\', '/').trim('/')
        return if (cleaned.isBlank()) "" else cleaned + "/"
    }

    private fun todayLocalDate(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(java.util.Date(now))

    private fun photoMemoryMillis(photo: PhotoEntity): Long {
        return listOf(photo.dateTaken, photo.dateModified, photo.dateAdded)
            .firstOrNull { it > 0L }
            ?.let(::normalizeMediaTimeMillis)
            ?: System.currentTimeMillis()
    }

    private fun normalizeMediaTimeMillis(value: Long): Long {
        return if (value in 1L..10_000_000_000L) value * 1000L else value
    }

    private fun memoryDayKey(timeMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(timeMillis))

    private fun dayRangeForMillis(timeMillis: Long): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = start.clone() as Calendar
        end.add(Calendar.DAY_OF_MONTH, 1)
        return start.timeInMillis to end.timeInMillis
    }

    private fun rangeFor(mode: String): Pair<Long, Long>? {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
        fun yearRange(year: Int): Pair<Long, Long> {
            val startCal = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.DAY_OF_YEAR, 1)
            }
            val endCal = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, year + 1)
                set(Calendar.DAY_OF_YEAR, 1)
            }
            return startCal.timeInMillis to endCal.timeInMillis
        }
        fun monthRange(year: Int, month: Int): Pair<Long, Long> {
            val startCal = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val endCal = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, 1)
            }
            return startCal.timeInMillis to endCal.timeInMillis
        }
        fun dayRange(year: Int, month: Int, day: Int): Pair<Long, Long> {
            val startCal = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
            }
            val endCal = Calendar.getInstance().apply {
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                add(Calendar.DAY_OF_MONTH, 1)
            }
            return startCal.timeInMillis to endCal.timeInMillis
        }
        return when (mode) {
            "seven_days" -> end - 7L * 24L * 60L * 60L * 1000L to end
            "month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to end
            }
            "year" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to end
            }
            else -> {
                when {
                    mode.startsWith("y:") -> mode.removePrefix("y:").toIntOrNull()?.let(::yearRange)
                    mode.startsWith("ym:") -> {
                        val parts = mode.removePrefix("ym:").split("-")
                        val year = parts.getOrNull(0)?.toIntOrNull()
                        val month = parts.getOrNull(1)?.toIntOrNull()
                        if (year != null && month != null && month in 1..12) monthRange(year, month) else null
                    }
                    mode.startsWith("d:") -> {
                        val parts = mode.removePrefix("d:").split("-")
                        val year = parts.getOrNull(0)?.toIntOrNull()
                        val month = parts.getOrNull(1)?.toIntOrNull()
                        val day = parts.getOrNull(2)?.toIntOrNull()
                        if (year != null && month != null && day != null && month in 1..12 && day in 1..31) dayRange(year, month, day) else null
                    }
                    mode.startsWith("multiym:") -> null
                    else -> null
                }
            }
        }
    }

    private fun dateModeMatches(timeMillis: Long, mode: String): Boolean {
        if (!mode.startsWith("multiym:")) return true
        val tokens = monthTokensForDateMode(mode).toSet()
        if (tokens.isEmpty()) return true
        if (timeMillis <= 0L) return false
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val token = cal.get(Calendar.YEAR).toString() + "-" + (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        return token in tokens
    }

    private fun monthTokensForDateMode(mode: String): List<String> =
        if (mode.startsWith("multiym:")) {
            mode.removePrefix("multiym:")
                .split(",")
                .filter { Regex("""\d{4}-\d{2}""").matches(it) }
                .distinct()
        } else {
            emptyList()
        }

    private fun PhotoEntity.toTrashItem(): TrashItemEntity = TrashItemEntity(
        mediaId = id,
        mediaType = "photo",
        mediaStoreId = mediaStoreId,
        uri = uri,
        displayName = displayName,
        size = size,
        dateTaken = dateTaken,
        folderPath = folderPath,
        folderName = folderName,
        mimeType = mimeType,
        width = width,
        height = height,
        trashedAt = System.currentTimeMillis(),
        autoDeleteAt = TrashRetentionPolicy.reviewDueAt(System.currentTimeMillis()),
    )

    private fun VideoEntity.toTrashItem(): TrashItemEntity = TrashItemEntity(
        mediaId = id,
        mediaType = "video",
        mediaStoreId = mediaStoreId,
        uri = uri,
        displayName = displayName,
        size = size,
        dateTaken = dateTaken,
        folderPath = folderPath,
        folderName = folderName,
        mimeType = mimeType,
        width = width,
        height = height,
        duration = duration,
        trashedAt = System.currentTimeMillis(),
        autoDeleteAt = TrashRetentionPolicy.reviewDueAt(System.currentTimeMillis()),
    )

    private data class AchievementSpec(
        val id: String,
        val title: String,
        val description: String,
        val difficulty: String,
        val target: Int,
        val metric: String,
        val category: String,
        val rarity: String,
        val xp: Int,
        val iconKey: String,
    )

    private data class AchievementMetricSpec(
        val metric: String,
        val label: String,
        val category: String,
        val iconKey: String,
        val descriptionBuilder: (Int) -> String,
    )

    private data class AchievementTierSpec(
        val key: String,
        val label: String,
        val target: Int,
        val rarity: String,
        val difficulty: String,
        val xp: Int,
    )

    private fun achievementSpecs(): List<AchievementSpec> {
        val metrics = listOf(
            AchievementMetricSpec("clear", "整理家", "整理", "clean", { "累计整理 $it 个媒体文件" }),
            AchievementMetricSpec("favorite", "收藏家", "珍藏", "favorite", { "累计收藏 $it 张值得留下的照片或视频" }),
            AchievementMetricSpec("delete", "空间清道夫", "断舍离", "delete", { "累计放入回收站 $it 个不再需要的媒体" }),
            AchievementMetricSpec("storage", "腾挪大师", "空间", "storage", { "累计释放或标记释放 $it MB 空间" }),
            AchievementMetricSpec("streak", "连续打卡", "习惯", "streak", { "连续 $it 天打开 Keepix 整理媒体" }),
            AchievementMetricSpec("undo", "谨慎大师", "修正", "undo", { "累计撤销 $it 次操作，保住重要回忆" }),
        )
        val tiers = listOf(
            AchievementTierSpec("starter", "初见", 5, "普通", "普通", 10),
            AchievementTierSpec("bronze", "青铜", 30, "普通", "普通", 20),
            AchievementTierSpec("silver", "白银", 100, "稀有", "稀有", 35),
            AchievementTierSpec("gold", "黄金", 500, "稀有", "稀有", 50),
            AchievementTierSpec("platinum", "白金", 1500, "史诗", "史诗", 75),
            AchievementTierSpec("diamond", "钻石", 5000, "史诗", "史诗", 100),
            AchievementTierSpec("master", "大师", 12000, "传奇", "传奇", 150),
            AchievementTierSpec("apex", "极境", 30000, "传奇", "传奇", 220),
            AchievementTierSpec("eternal", "永恒", 60000, "神话", "传奇", 320),
            AchievementTierSpec("immortal", "不朽", 100000, "神话", "传奇", 500),
        )
        return metrics.flatMap { metric ->
            tiers.map { tier ->
                AchievementSpec(
                    id = "${metric.metric}-${tier.key}",
                    title = "${metric.label} · ${tier.label}",
                    description = metric.descriptionBuilder(tier.target),
                    difficulty = tier.difficulty,
                    target = tier.target,
                    metric = metric.metric,
                    category = metric.category,
                    rarity = tier.rarity,
                    xp = tier.xp,
                    iconKey = metric.iconKey,
                )
            }
        }
    }
}

internal data class StatsAdjustment(
    val photoDelta: Int = 0,
    val videoDelta: Int = 0,
    val favoriteDelta: Int = 0,
    val storageDelta: Long = 0L,
)

internal fun statsAdjustmentForUndo(
    operation: OperationHistoryEntity,
    mediaSize: Long,
): StatsAdjustment {
    val mediaWasFirstProcessed = operation.previousProcessingStatus == ProcessingStatus.UNPROCESSED &&
        operation.newProcessingStatus != ProcessingStatus.UNPROCESSED
    val mediaDelta = if (mediaWasFirstProcessed) -1 else 0
    val isPhoto = operation.mediaType == "photo"
    val isVideo = operation.mediaType == "video"
    return when (operation.action) {
        SwipeAction.Keep.name -> StatsAdjustment(
            photoDelta = if (isPhoto) mediaDelta else 0,
            videoDelta = if (isVideo) mediaDelta else 0,
        )
        SwipeAction.Favorite.name -> StatsAdjustment(
            photoDelta = if (isPhoto) mediaDelta else 0,
            videoDelta = if (isVideo) mediaDelta else 0,
            favoriteDelta = if (
                operation.previousProcessingStatus != ProcessingStatus.FAVORITED &&
                operation.newProcessingStatus == ProcessingStatus.FAVORITED
            ) -1 else 0,
        )
        SwipeAction.Delete.name -> {
            val deletionWasAdded = operation.previousDeletionStatus == DeletionStatus.NONE &&
                operation.newDeletionStatus != DeletionStatus.NONE
            val deletedMediaDelta = if (deletionWasAdded && operation.previousProcessingStatus == ProcessingStatus.UNPROCESSED) -1 else 0
            StatsAdjustment(
                photoDelta = if (isPhoto) deletedMediaDelta else 0,
                videoDelta = if (isVideo) deletedMediaDelta else 0,
                storageDelta = if (deletionWasAdded && (isPhoto || isVideo)) -mediaSize.coerceAtLeast(0L) else 0L,
            )
        }
        else -> StatsAdjustment()
    }
}
