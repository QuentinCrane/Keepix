package com.futureape.kanleme.data.repository

import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.SimilarGroupEntity
import com.futureape.kanleme.data.local.TrashItemEntity
import com.futureape.kanleme.data.local.UserStatsEntity
import com.futureape.kanleme.data.local.VideoEntity
import kotlinx.coroutines.flow.Flow

// Scope recovered from APK business keys and expanded to match the public feature docs.
data class CleaningScope(
    val dateMode: String = "all", // all, today, seven_days, month, year
    val folderPaths: Set<String> = emptySet(),
    val mediaType: String = "all", // all, normal, screenshot, selfie, motion, long, gif, raw
    val sortOrder: String = "random", // newest, random
    val todayInHistory: Boolean = false,
    val batchSize: Int = 0,
    val randomSeed: Long = 1L, // stable random order seed; changes only when the user explicitly toggles random again
)

data class DashboardStats(
    val photoCount: Int = 0,
    val videoCount: Int = 0,
    val processedPhotoCount: Int = 0,
    val processedVideoCount: Int = 0,
    val favoriteCount: Int = 0,
    val trashCount: Int = 0,
    val pendingDeleteCount: Int = 0,
    val pendingDeleteBytes: Long = 0L,
    val todayPhotoCount: Int = 0,
    val todayVideoCount: Int = 0,
    val todayActionCount: Int = 0,
    val userStats: UserStatsEntity? = null,
) {
    val totalCount: Int get() = photoCount + videoCount
    val processedCount: Int get() = processedPhotoCount + processedVideoCount
    val progress: Float get() = if (totalCount == 0) 0f else processedCount.toFloat() / totalCount.toFloat()
}

data class PhotoTypeStats(
    val all: Int = 0,
    val normal: Int = 0,
    val screenshot: Int = 0,
    val selfie: Int = 0,
    val motion: Int = 0,
    val gif: Int = 0,
    val longImage: Int = 0,
)

data class SimilarGroupDetail(
    val group: SimilarGroupEntity,
    val photos: List<PhotoEntity>,
)

data class AnnualReport(
    val year: Int,
    val photoCount: Int,
    val videoCount: Int,
    val clearedCount: Int,
    val favoriteCount: Int,
    val deletedCount: Int,
    val freedBytes: Long,
    val topFolder: String,
    val styleTitle: String,
    val styleDescription: String,
)

data class AchievementUi(
    val id: String,
    val title: String,
    val description: String,
    val difficulty: String,
    val target: Int,
    val current: Int,
    val category: String = "整理",
    val rarity: String = "普通",
    val xp: Int = 10,
    val iconKey: String = "trophy",
) {
    val unlocked: Boolean get() = current >= target
    val progress: Float get() = if (target == 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
    val remaining: Int get() = (target - current).coerceAtLeast(0)
}

enum class SwipeAction { Keep, Favorite, Delete }

data class MovePhotoResult(
    val success: Boolean,
    val message: String,
)

interface AppRepository {
    fun observeDashboard(): Flow<DashboardStats>
    fun observePhotoTypeStats(): Flow<PhotoTypeStats>
    fun observeRecentPhotos(limit: Int = 50): Flow<List<PhotoEntity>>
    fun observeTimelinePhotos(limit: Int = 5000): Flow<List<PhotoEntity>>
    fun observeTimelineVideos(limit: Int = 5000): Flow<List<VideoEntity>>
    fun observeTodayInHistory(limit: Int = 400): Flow<List<PhotoEntity>>
    fun observeTodayInHistoryVideos(limit: Int = 400): Flow<List<VideoEntity>>
    fun observeRecentlyAddedPhotos(days: Int = 7, limit: Int = 120): Flow<List<PhotoEntity>>
    fun observePhotoFolders(): Flow<List<String>>
    fun observeVideoFolders(): Flow<List<String>>
    fun observeRecentVideos(limit: Int = 30): Flow<List<VideoEntity>>
    fun observeFavoritePhotos(limit: Int = 120): Flow<List<PhotoEntity>>
    fun observeFavoriteVideos(limit: Int = 120): Flow<List<VideoEntity>>
    fun observeTrash(): Flow<List<TrashItemEntity>>
    fun observeSimilarGroups(): Flow<List<SimilarGroupEntity>>

    suspend fun refreshMediaLibrary(): Pair<Int, Int>
    suspend fun loadPhotoDeck(scope: CleaningScope): List<PhotoEntity>
    suspend fun loadVideoDeck(scope: CleaningScope): List<VideoEntity>
    suspend fun handlePhotoAction(photo: PhotoEntity, action: SwipeAction)
    suspend fun handleVideoAction(video: VideoEntity, action: SwipeAction)
    suspend fun movePhotoToFolder(photo: PhotoEntity, targetRelativePath: String): MovePhotoResult
    suspend fun moveVideoToFolder(video: VideoEntity, targetRelativePath: String): MovePhotoResult
    suspend fun generateSimilarGroups(
        limit: Int = 1200,
        onProgress: (processed: Int, total: Int, stage: String) -> Unit = { _, _, _ -> },
    ): Int
    suspend fun similarGroupDetail(groupId: String): SimilarGroupDetail?
    suspend fun restoreTrashItem(trashId: Long)
    suspend fun permanentlyDeleteTrashItem(trashId: Long)
    suspend fun permanentlyDeleteAllTrash()
    suspend fun restoreAllTrash()
    suspend fun undoLastAction(): Boolean
    suspend fun buildAnnualReport(year: Int): AnnualReport
    suspend fun buildAchievements(): List<AchievementUi>
}
