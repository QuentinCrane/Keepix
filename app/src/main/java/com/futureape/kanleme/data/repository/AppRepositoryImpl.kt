package com.futureape.kanleme.data.repository

import com.futureape.kanleme.data.local.AppDatabase
import com.futureape.kanleme.data.local.CleanupEventEntity
import com.futureape.kanleme.data.local.DeletionStatus
import com.futureape.kanleme.data.local.OperationHistoryEntity
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.ProcessingStatus
import com.futureape.kanleme.data.local.SimilarGroupEntity
import com.futureape.kanleme.data.local.TrashItemEntity
import com.futureape.kanleme.data.local.UserStatsEntity
import com.futureape.kanleme.data.local.VideoEntity
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
import java.util.Locale
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

    override suspend fun refreshMediaLibrary(): Pair<Int, Int> = withContext(Dispatchers.IO) {
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

        val existingPhotos = if (scannedPhotos.isEmpty()) {
            emptyMap()
        } else {
            photoDao.byMediaStoreIds(scannedPhotos.map { it.mediaStoreId }).associateBy { it.mediaStoreId }
        }
        val existingVideos = if (scannedVideos.isEmpty()) {
            emptyMap()
        } else {
            videoDao.byMediaStoreIds(scannedVideos.map { it.mediaStoreId }).associateBy { it.mediaStoreId }
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
                    updatedAt = System.currentTimeMillis(),
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
                    updatedAt = System.currentTimeMillis(),
                )
            }
        }

        photoDao.upsertAll(mergedPhotos)
        videoDao.upsertAll(mergedVideos)
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
        val requiresPostDateFilter = scope.dateMode.startsWith("multiym:")
        val targetLimit = scope.batchSize.takeIf { it > 0 } ?: settings.photoBatchSize
        val queryLimit = if (settings.excludedFolderPaths.isEmpty()) {
            if (requiresPostDateFilter) 5000 else (targetLimit * 6).coerceAtLeast(120)
        } else {
            20000
        }
        photoDao.nextFilteredBatch(
            folderPath = scope.folderPaths.firstOrNull(),
            startMillis = range?.first,
            endMillis = range?.second,
            mediaType = scope.mediaType,
            randomOrder = scope.sortOrder == "random",
            randomSeed = scope.randomSeed,
            todayOnly = scope.todayInHistory,
            limit = queryLimit,
        )
            .filter { dateModeMatches(it.dateTaken, scope.dateMode) }
            .filterNot { isExcludedFolder(it.folderPath, settings.excludedFolderPaths) }
            .take(targetLimit)
    }

    override suspend fun loadVideoDeck(scope: CleaningScope): List<VideoEntity> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        val range = rangeFor(scope.dateMode)
        val requiresPostDateFilter = scope.dateMode.startsWith("multiym:")
        val targetLimit = scope.batchSize.takeIf { it > 0 } ?: settings.videoBatchSize
        val queryLimit = if (settings.excludedFolderPaths.isEmpty()) {
            if (requiresPostDateFilter) 5000 else (targetLimit * 6).coerceAtLeast(120)
        } else {
            5000
        }
        videoDao.nextFilteredBatch(
            folderPath = scope.folderPaths.firstOrNull(),
            startMillis = range?.first,
            endMillis = range?.second,
            randomOrder = scope.sortOrder == "random",
            randomSeed = scope.randomSeed,
            limit = queryLimit,
        )
            .filter { dateModeMatches(it.dateTaken, scope.dateMode) }
            .filterNot { isExcludedFolder(it.folderPath, settings.excludedFolderPaths) }
            .take(targetLimit)
    }

    override suspend fun handlePhotoAction(photo: PhotoEntity, action: SwipeAction): Long = withContext(Dispatchers.IO) {
        val current = photoDao.byId(photo.id) ?: photo
        val actionAllowed = current.deletionStatus == DeletionStatus.NONE
        val targetProcessing = when (action) {
            SwipeAction.Keep -> ProcessingStatus.KEPT
            SwipeAction.Favorite -> ProcessingStatus.FAVORITED
            SwipeAction.Delete -> current.processingStatus
        }
        val newProcessing = if (actionAllowed) targetProcessing else current.processingStatus
        val newDeletion = if (action == SwipeAction.Delete && actionAllowed) DeletionStatus.PENDING else current.deletionStatus
        val operationId = recordOperation(current.id, "photo", current.processingStatus, current.deletionStatus, newProcessing, newDeletion, action.name)
        when (action) {
            SwipeAction.Keep -> {
                if (actionAllowed && current.processingStatus != ProcessingStatus.KEPT) {
                    photoDao.updateProcessingStatus(current.id, ProcessingStatus.KEPT)
                    if (current.processingStatus == ProcessingStatus.UNPROCESSED) bumpStats(photoDelta = 1)
                }
            }
            SwipeAction.Favorite -> {
                if (actionAllowed && current.processingStatus != ProcessingStatus.FAVORITED) {
                    photoDao.updateProcessingStatus(current.id, ProcessingStatus.FAVORITED)
                    if (current.processingStatus == ProcessingStatus.UNPROCESSED) {
                        bumpStats(favoriteDelta = 1, photoDelta = 1)
                    }
                }
            }
            SwipeAction.Delete -> {
                if (actionAllowed) {
                    val settings = settingsRepository.settings.first()
                    if (settings.deleteMode == DeleteMode.SYSTEM_TRASH) {
                        when (mediaStoreActions.moveMediaToSystemTrash(current.uri)) {
                            MediaOperationResult.Success -> photoDao.updateDeletionStatus(listOf(current.id), DeletionStatus.TRASHED)
                            is MediaOperationResult.Failed -> photoDao.updateDeletionStatus(listOf(current.id), DeletionStatus.PENDING)
                        }
                    } else {
                        photoDao.updateDeletionStatus(listOf(current.id), DeletionStatus.PENDING)
                    }
                    trashDao.deleteByMedia(current.id, "photo")
                    trashDao.insert(current.toTrashItem())
                    if (current.processingStatus == ProcessingStatus.UNPROCESSED) bumpStats(photoDelta = 1, deletedSize = current.size)
                }
            }
        }
        operationId
    }

    override suspend fun handleVideoAction(video: VideoEntity, action: SwipeAction): Long = withContext(Dispatchers.IO) {
        val current = videoDao.byId(video.id) ?: video
        val actionAllowed = current.deletionStatus == DeletionStatus.NONE
        val targetProcessing = when (action) {
            SwipeAction.Keep -> ProcessingStatus.KEPT
            SwipeAction.Favorite -> ProcessingStatus.FAVORITED
            SwipeAction.Delete -> current.processingStatus
        }
        val newProcessing = if (actionAllowed) targetProcessing else current.processingStatus
        val newDeletion = if (action == SwipeAction.Delete && actionAllowed) DeletionStatus.PENDING else current.deletionStatus
        val operationId = recordOperation(current.id, "video", current.processingStatus, current.deletionStatus, newProcessing, newDeletion, action.name)
        when (action) {
            SwipeAction.Keep -> {
                if (actionAllowed && current.processingStatus != ProcessingStatus.KEPT) {
                    videoDao.updateProcessingStatus(current.id, ProcessingStatus.KEPT)
                    if (current.processingStatus == ProcessingStatus.UNPROCESSED) bumpStats(videoDelta = 1)
                }
            }
            SwipeAction.Favorite -> {
                if (actionAllowed && current.processingStatus != ProcessingStatus.FAVORITED) {
                    videoDao.updateProcessingStatus(current.id, ProcessingStatus.FAVORITED)
                    if (current.processingStatus == ProcessingStatus.UNPROCESSED) {
                        bumpStats(favoriteDelta = 1, videoDelta = 1)
                    }
                }
            }
            SwipeAction.Delete -> {
                if (actionAllowed) {
                    val settings = settingsRepository.settings.first()
                    if (settings.deleteMode == DeleteMode.SYSTEM_TRASH) {
                        when (mediaStoreActions.moveMediaToSystemTrash(current.uri)) {
                            MediaOperationResult.Success -> videoDao.updateDeletionStatus(listOf(current.id), DeletionStatus.TRASHED)
                            is MediaOperationResult.Failed -> videoDao.updateDeletionStatus(listOf(current.id), DeletionStatus.PENDING)
                        }
                    } else {
                        videoDao.updateDeletionStatus(listOf(current.id), DeletionStatus.PENDING)
                    }
                    trashDao.deleteByMedia(current.id, "video")
                    trashDao.insert(current.toTrashItem())
                    if (current.processingStatus == ProcessingStatus.UNPROCESSED) bumpStats(videoDelta = 1, deletedSize = current.size)
                }
            }
        }
        operationId
    }

    override suspend fun movePhotoToFolder(photo: PhotoEntity, targetRelativePath: String): MovePhotoResult = withContext(Dispatchers.IO) {
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

    override suspend fun moveVideoToFolder(video: VideoEntity, targetRelativePath: String): MovePhotoResult = withContext(Dispatchers.IO) {
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
            requireMediaOperation(mediaStoreActions.restoreFromSystemTrash(item.uri))
        }
        restoreTrashItemLocally(item)
    }

    override suspend fun markTrashItemDeleted(trashId: Long) = withContext(Dispatchers.IO) {
        val item = trashDao.byId(trashId) ?: return@withContext
        markTrashItemDeletedLocally(item)
    }

    override suspend fun permanentlyDeleteTrashItem(trashId: Long) = withContext(Dispatchers.IO) {
        val item = trashDao.byId(trashId) ?: return@withContext
        requireMediaOperation(mediaStoreActions.permanentlyDeleteMedia(item.uri))
        markTrashItemDeletedLocally(item)
    }

    override suspend fun permanentlyDeleteAllTrash() = withContext(Dispatchers.IO) {
        observeTrash().first().forEach { permanentlyDeleteTrashItem(it.id) }
    }

    override suspend fun restoreAllTrash() = withContext(Dispatchers.IO) {
        observeTrash().first().forEach { restoreTrashItem(it.id) }
    }

    private suspend fun isTrashItemInSystemTrash(item: TrashItemEntity): Boolean =
        if (item.mediaType == "photo") {
            photoDao.byId(item.mediaId)?.deletionStatus == DeletionStatus.TRASHED
        } else {
            videoDao.byId(item.mediaId)?.deletionStatus == DeletionStatus.TRASHED
        }

    private suspend fun restoreTrashItemLocally(item: TrashItemEntity) {
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

    private fun requireMediaOperation(result: MediaOperationResult) {
        if (result is MediaOperationResult.Failed) throw IllegalStateException(result.reason)
    }

    override suspend fun undoLastAction(): Boolean = withContext(Dispatchers.IO) {
        val op = operationDao.lastUndoable() ?: return@withContext false
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

    private suspend fun undoOperation(op: OperationHistoryEntity) {
        if (op.mediaType == "photo") {
            photoDao.restoreStatus(op.mediaId, op.previousProcessingStatus, op.previousDeletionStatus)
        } else {
            videoDao.restoreStatus(op.mediaId, op.previousProcessingStatus, op.previousDeletionStatus)
        }
        if (op.action == SwipeAction.Delete.name) trashDao.deleteByMedia(op.mediaId, op.mediaType)
        operationDao.markUndone(op.id)
        bumpStats(undoDelta = 1)
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
    ) {
        val now = System.currentTimeMillis()
        val current = statsDao.observeUserStats().first() ?: UserStatsEntity()
        statsDao.upsertUserStats(
            current.copy(
                totalPhotosCleared = current.totalPhotosCleared + photoDelta,
                totalVideosCleared = current.totalVideosCleared + videoDelta,
                totalStorageFreed = current.totalStorageFreed + deletedSize,
                totalFavorited = current.totalFavorited + favoriteDelta,
                totalUndoCount = current.totalUndoCount + undoDelta,
                lastActiveDate = now,
                updatedAt = now,
            )
        )
        if (photoDelta > 0 || videoDelta > 0 || deletedSize > 0L) {
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
                    createdAt = now,
                )
            )
        }
    }

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
        val tokens = mode.removePrefix("multiym:").split(",").filter { it.isNotBlank() }.toSet()
        if (tokens.isEmpty() || timeMillis <= 0L) return true
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val token = cal.get(Calendar.YEAR).toString() + "-" + (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        return token in tokens
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
        autoDeleteAt = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L,
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
        autoDeleteAt = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L,
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
