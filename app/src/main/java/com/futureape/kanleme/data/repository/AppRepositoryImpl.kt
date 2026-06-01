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
        statsDao.observeTodayPhotoCount(todayLocalDate()).map { it as Any? },
        statsDao.observeTodayVideoCount(todayLocalDate()).map { it as Any? },
        statsDao.observeTodayActionCount(todayLocalDate()).map { it as Any? },
        statsDao.observeUserStats().map { it as Any? },
    ) { values ->
        val photoFavorites = values[4] as Int
        val videoFavorites = values[5] as Int
        val photoPendingDelete = values[7] as Int
        val videoPendingDelete = values[8] as Int
        DashboardStats(
            photoCount = values[0] as Int,
            videoCount = values[1] as Int,
            processedPhotoCount = values[2] as Int,
            processedVideoCount = values[3] as Int,
            favoriteCount = photoFavorites + videoFavorites,
            trashCount = values[6] as Int,
            pendingDeleteCount = photoPendingDelete + videoPendingDelete,
            todayPhotoCount = values[9] as Int,
            todayVideoCount = values[10] as Int,
            todayActionCount = values[11] as Int,
            userStats = values[12] as? UserStatsEntity,
        )
    }

    override fun observePhotoTypeStats(): Flow<PhotoTypeStats> = combine(
        photoDao.observeActiveCount(),
        photoDao.observeTypeCount("normal"),
        photoDao.observeTypeCount("screenshot"),
        photoDao.observeTypeCount("selfie"),
        photoDao.observeTypeCount("motion"),
        photoDao.observeTypeCount("long"),
    ) { values ->
        PhotoTypeStats(
            all = values[0] as Int,
            normal = values[1] as Int,
            screenshot = values[2] as Int,
            selfie = values[3] as Int,
            motion = values[4] as Int,
            longImage = values[5] as Int,
        )
    }

    override fun observeRecentPhotos(limit: Int): Flow<List<PhotoEntity>> = photoDao.observeRecent(limit)
    override fun observeTimelinePhotos(limit: Int): Flow<List<PhotoEntity>> = photoDao.observeTimeline(limit)
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
        val scannedPhotos = scanner.scanImages().distinctBy { it.mediaStoreId }
        val scannedVideos = scanner.scanVideos().distinctBy { it.mediaStoreId }

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

    override suspend fun loadPhotoDeck(scope: CleaningScope): List<PhotoEntity> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        val range = rangeFor(scope.dateMode)
        val targetLimit = scope.batchSize.takeIf { it > 0 } ?: settings.photoBatchSize
        val queryLimit = if (settings.excludedFolderPaths.isEmpty()) {
            (targetLimit * 6).coerceAtLeast(120)
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
        ).filterNot { isExcludedFolder(it.folderPath, settings.excludedFolderPaths) }.take(targetLimit)
    }

    override suspend fun loadVideoDeck(scope: CleaningScope): List<VideoEntity> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        val range = rangeFor(scope.dateMode)
        val targetLimit = scope.batchSize.takeIf { it > 0 } ?: settings.videoBatchSize
        val queryLimit = if (settings.excludedFolderPaths.isEmpty()) {
            (targetLimit * 6).coerceAtLeast(120)
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
        ).filterNot { isExcludedFolder(it.folderPath, settings.excludedFolderPaths) }.take(targetLimit)
    }

    override suspend fun handlePhotoAction(photo: PhotoEntity, action: SwipeAction) = withContext(Dispatchers.IO) {
        val newProcessing = when (action) {
            SwipeAction.Keep -> ProcessingStatus.KEPT
            SwipeAction.Favorite -> ProcessingStatus.FAVORITED
            SwipeAction.Delete -> photo.processingStatus
        }
        val newDeletion = if (action == SwipeAction.Delete) DeletionStatus.PENDING else photo.deletionStatus
        recordOperation(photo.id, "photo", photo.processingStatus, photo.deletionStatus, newProcessing, newDeletion, action.name)
        when (action) {
            SwipeAction.Keep -> {
                photoDao.updateProcessingStatus(photo.id, ProcessingStatus.KEPT)
                bumpStats(photoDelta = 1)
            }
            SwipeAction.Favorite -> {
                photoDao.updateProcessingStatus(photo.id, ProcessingStatus.FAVORITED)
                bumpStats(favoriteDelta = 1, photoDelta = 1)
            }
            SwipeAction.Delete -> {
                val settings = settingsRepository.settings.first()
                if (settings.deleteMode == DeleteMode.SYSTEM_TRASH) {
                    when (mediaStoreActions.moveMediaToSystemTrash(photo.uri)) {
                        MediaOperationResult.Success -> photoDao.updateDeletionStatus(listOf(photo.id), DeletionStatus.TRASHED)
                        is MediaOperationResult.Failed -> photoDao.updateDeletionStatus(listOf(photo.id), DeletionStatus.PENDING)
                    }
                } else {
                    photoDao.updateDeletionStatus(listOf(photo.id), DeletionStatus.PENDING)
                }
                trashDao.insert(photo.toTrashItem())
                bumpStats(photoDelta = 1, deletedSize = photo.size)
            }
        }
    }

    override suspend fun handleVideoAction(video: VideoEntity, action: SwipeAction) = withContext(Dispatchers.IO) {
        val newProcessing = when (action) {
            SwipeAction.Keep -> ProcessingStatus.KEPT
            SwipeAction.Favorite -> ProcessingStatus.FAVORITED
            SwipeAction.Delete -> video.processingStatus
        }
        val newDeletion = if (action == SwipeAction.Delete) DeletionStatus.PENDING else video.deletionStatus
        recordOperation(video.id, "video", video.processingStatus, video.deletionStatus, newProcessing, newDeletion, action.name)
        when (action) {
            SwipeAction.Keep -> {
                videoDao.updateProcessingStatus(video.id, ProcessingStatus.KEPT)
                bumpStats(videoDelta = 1)
            }
            SwipeAction.Favorite -> {
                videoDao.updateProcessingStatus(video.id, ProcessingStatus.FAVORITED)
                bumpStats(favoriteDelta = 1, videoDelta = 1)
            }
            SwipeAction.Delete -> {
                val settings = settingsRepository.settings.first()
                if (settings.deleteMode == DeleteMode.SYSTEM_TRASH) {
                    when (mediaStoreActions.moveMediaToSystemTrash(video.uri)) {
                        MediaOperationResult.Success -> videoDao.updateDeletionStatus(listOf(video.id), DeletionStatus.TRASHED)
                        is MediaOperationResult.Failed -> videoDao.updateDeletionStatus(listOf(video.id), DeletionStatus.PENDING)
                    }
                } else {
                    videoDao.updateDeletionStatus(listOf(video.id), DeletionStatus.PENDING)
                }
                trashDao.insert(video.toTrashItem())
                bumpStats(videoDelta = 1, deletedSize = video.size)
            }
        }
    }

    override suspend fun movePhotoToFolder(photo: PhotoEntity, targetRelativePath: String): MovePhotoResult = withContext(Dispatchers.IO) {
        val normalizedPath = MediaStoreActions.normalizeRelativePath(targetRelativePath)
        when (val result = mediaStoreActions.moveImageToFolder(photo.uri, normalizedPath)) {
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
            .filter { it.photoId in activeIds }
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

        val fingerprints = similarDao.allFingerprints().filter { it.photoId in activeIds }
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
        mediaStoreActions.restoreFromSystemTrash(item.uri)
        if (item.mediaType == "photo") photoDao.restoreStatus(item.mediaId, ProcessingStatus.UNPROCESSED, DeletionStatus.NONE)
        else videoDao.restoreStatus(item.mediaId, ProcessingStatus.UNPROCESSED, DeletionStatus.NONE)
        trashDao.deleteById(trashId)
    }

    override suspend fun permanentlyDeleteTrashItem(trashId: Long) = withContext(Dispatchers.IO) {
        val item = trashDao.byId(trashId) ?: return@withContext
        mediaStoreActions.permanentlyDeleteMedia(item.uri)
        if (item.mediaType == "photo") photoDao.updateDeletionStatus(listOf(item.mediaId), DeletionStatus.DELETED)
        else videoDao.updateDeletionStatus(listOf(item.mediaId), DeletionStatus.DELETED)
        trashDao.deleteById(trashId)
    }

    override suspend fun permanentlyDeleteAllTrash() = withContext(Dispatchers.IO) {
        observeTrash().first().forEach { permanentlyDeleteTrashItem(it.id) }
    }

    override suspend fun undoLastAction(): Boolean = withContext(Dispatchers.IO) {
        val op = operationDao.lastUndoable() ?: return@withContext false
        if (op.mediaType == "photo") {
            photoDao.restoreStatus(op.mediaId, op.previousProcessingStatus, op.previousDeletionStatus)
        } else {
            videoDao.restoreStatus(op.mediaId, op.previousProcessingStatus, op.previousDeletionStatus)
        }
        if (op.action == SwipeAction.Delete.name) trashDao.deleteByMedia(op.mediaId, op.mediaType)
        operationDao.markUndone(op.id)
        bumpStats(undoDelta = 1)
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
            AchievementUi(spec.id, spec.title, spec.description, spec.difficulty, spec.target, current)
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
    ) {
        operationDao.insert(
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
        SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(java.util.Date(now))

    private fun rangeFor(mode: String): Pair<Long, Long>? {
        val cal = Calendar.getInstance()
        val end = cal.timeInMillis
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
            else -> null
        }
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

    private data class AchievementSpec(val id: String, val title: String, val description: String, val difficulty: String, val target: Int, val metric: String)

    private fun achievementSpecs(): List<AchievementSpec> {
        val bases = listOf(
            "clear" to "整理",
            "favorite" to "珍藏",
            "delete" to "断舍离",
            "storage" to "释放空间",
            "streak" to "连续整理",
            "undo" to "谨慎撤销",
        )
        val tiers = listOf(
            Triple("bronze", "青铜", 5),
            Triple("silver", "白银", 30),
            Triple("gold", "黄金", 100),
            Triple("diamond", "钻石", 500),
            Triple("master", "大师", 1500),
            Triple("legend", "传说", 5000),
            Triple("myth", "神话", 12000),
            Triple("cosmic", "星河", 30000),
            Triple("eternal", "永恒", 60000),
            Triple("apex", "极境", 100000),
        )
        return bases.flatMap { (metric, label) ->
            tiers.mapIndexed { index, tier ->
                AchievementSpec(
                    id = "$metric-${tier.first}",
                    title = "$label · ${tier.second}",
                    description = "累计达成 ${tier.third} 次${label}相关行为",
                    difficulty = when (index) { 0, 1 -> "普通"; 2, 3, 4 -> "稀有"; 5, 6 -> "史诗"; else -> "传奇" },
                    target = tier.third,
                    metric = metric,
                )
            }
        }
    }
}
