package com.futureape.kanleme.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class SimilarAnalysisInput(
    val id: Long,
    @androidx.room.ColumnInfo(name = "media_store_id") val mediaStoreId: Long,
    @androidx.room.ColumnInfo(name = "stable_media_key") val stableMediaKey: String,
    val uri: String,
    val size: Long,
    @androidx.room.ColumnInfo(name = "date_taken") val dateTaken: Long,
    @androidx.room.ColumnInfo(name = "date_modified") val dateModified: Long,
    val width: Int,
    val height: Int,
)

data class PhotoTypeStatsProjection(
    @ColumnInfo(name = "all_count") val allCount: Int,
    @ColumnInfo(name = "normal_count") val normalCount: Int,
    @ColumnInfo(name = "screenshot_count") val screenshotCount: Int,
    @ColumnInfo(name = "selfie_count") val selfieCount: Int,
    @ColumnInfo(name = "motion_count") val motionCount: Int,
    @ColumnInfo(name = "gif_count") val gifCount: Int,
    @ColumnInfo(name = "long_count") val longCount: Int,
)

@Dao
interface PhotoDao {
    @Query("""
        SELECT
            COUNT(*) AS all_count,
            COUNT(CASE WHEN is_screenshot = 0 AND is_selfie = 0 AND is_motion_photo = 0 AND is_long_image = 0 AND is_gif = 0 AND is_raw = 0 THEN 1 END) AS normal_count,
            COUNT(CASE WHEN is_screenshot = 1 THEN 1 END) AS screenshot_count,
            COUNT(CASE WHEN is_selfie = 1 THEN 1 END) AS selfie_count,
            COUNT(CASE WHEN is_motion_photo = 1 THEN 1 END) AS motion_count,
            COUNT(CASE WHEN is_gif = 1 THEN 1 END) AS gif_count,
            COUNT(CASE WHEN is_long_image = 1 THEN 1 END) AS long_count
        FROM photos
        WHERE deletion_status = 'none' AND missing_since IS NULL
    """)
    fun observeTypeStats(): Flow<PhotoTypeStatsProjection>

    @Query("SELECT COUNT(*) FROM photos WHERE missing_since IS NULL")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE deletion_status = 'none' AND missing_since IS NULL")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE processing_status = :status AND deletion_status = 'none' AND missing_since IS NULL")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE missing_since IS NULL AND (processing_status != 'unprocessed' OR deletion_status != 'none')")
    fun observeProcessedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE deletion_status IN ('pending','trashed') AND missing_since IS NULL")
    fun observeDeleteCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(size), 0) FROM photos WHERE deletion_status IN ('pending','trashed') AND missing_since IS NULL")
    fun observeDeleteSize(): Flow<Long>

    @Query("SELECT * FROM photos WHERE deletion_status = 'none' AND missing_since IS NULL ORDER BY date_taken DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE deletion_status = 'none' AND missing_since IS NULL ORDER BY date_taken DESC LIMIT :limit")
    fun observeTimeline(limit: Int): Flow<List<PhotoEntity>>

    @Query("""
        SELECT p.* FROM photos p
        WHERE p.deletion_status = 'none'
        AND p.missing_since IS NULL
        AND p.id IN (
            SELECT media_id FROM operation_history
            WHERE media_type = 'photo' AND is_undone = 0
        )
        ORDER BY p.processed_at DESC, p.updated_at DESC
        LIMIT :limit
    """)
    fun observeCleanedHistory(limit: Int): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE deletion_status = 'none' AND missing_since IS NULL AND strftime('%m-%d', date_taken / 1000, 'unixepoch', 'localtime') = strftime('%m-%d', :now / 1000, 'unixepoch', 'localtime') AND strftime('%Y', date_taken / 1000, 'unixepoch', 'localtime') != strftime('%Y', :now / 1000, 'unixepoch', 'localtime') ORDER BY date_taken DESC LIMIT :limit")
    fun observeTodayInHistory(now: Long = System.currentTimeMillis(), limit: Int = 400): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE deletion_status = 'none' AND missing_since IS NULL AND date_added >= :sinceSeconds ORDER BY date_added DESC LIMIT :limit")
    fun observeRecentlyAdded(sinceSeconds: Long, limit: Int = 120): Flow<List<PhotoEntity>>

    @Query("SELECT DISTINCT folder_path FROM photos WHERE folder_path IS NOT NULL AND folder_path != '' AND missing_since IS NULL ORDER BY folder_path")
    fun observeFolderPaths(): Flow<List<String>>

    @Query("""
        SELECT COUNT(*) FROM photos WHERE deletion_status = 'none'
        AND missing_since IS NULL
        AND (:type = 'all'
            OR (:type = 'normal' AND is_screenshot = 0 AND is_selfie = 0 AND is_motion_photo = 0 AND is_long_image = 0 AND is_gif = 0 AND is_raw = 0)
            OR (:type = 'screenshot' AND is_screenshot = 1)
            OR (:type = 'selfie' AND is_selfie = 1)
            OR (:type = 'motion' AND is_motion_photo = 1)
            OR (:type = 'long' AND is_long_image = 1)
            OR (:type = 'gif' AND is_gif = 1)
            OR (:type = 'raw' AND is_raw = 1))
    """)
    fun observeTypeCount(type: String): Flow<Int>

    @Query("""
        SELECT * FROM photos
        WHERE processing_status = 'unprocessed' AND deletion_status = 'none'
        AND missing_since IS NULL
        AND (:folderPath IS NULL OR folder_path = :folderPath)
        AND (:startMillis IS NULL OR date_taken >= :startMillis)
        AND (:endMillis IS NULL OR date_taken < :endMillis)
        AND (:multiMonthEnabled = 0 OR strftime('%Y-%m', date_taken / 1000, 'unixepoch', 'localtime') IN (:monthTokens))
        AND (:todayOnly = 0 OR strftime('%m-%d', date_taken / 1000, 'unixepoch', 'localtime') = strftime('%m-%d', :now / 1000, 'unixepoch', 'localtime'))
        AND (:mediaType = 'all'
            OR (:mediaType = 'normal' AND is_screenshot = 0 AND is_selfie = 0 AND is_motion_photo = 0 AND is_long_image = 0 AND is_gif = 0 AND is_raw = 0)
            OR (:mediaType = 'screenshot' AND is_screenshot = 1)
            OR (:mediaType = 'selfie' AND is_selfie = 1)
            OR (:mediaType = 'motion' AND is_motion_photo = 1)
            OR (:mediaType = 'long' AND is_long_image = 1)
            OR (:mediaType = 'gif' AND is_gif = 1)
            OR (:mediaType = 'raw' AND is_raw = 1))
        ORDER BY CASE WHEN :randomOrder = 1 THEN ABS((
            ((media_store_id % 2147483647) * ((:randomSeed % 1000003) + 1009))
            + (((date_taken / 1000) % 2147483647) * ((:randomSeed % 65537) + 257))
            + ((size % 1000003) * ((:randomSeed % 4099) + 8191))
            + ((media_store_id % 9973) * ((:randomSeed % 131071) + 17))
        ) % 2147483647) ELSE date_taken END DESC, date_taken DESC
        LIMIT :limit
    """)
    suspend fun nextFilteredBatch(
        folderPath: String?,
        startMillis: Long?,
        endMillis: Long?,
        multiMonthEnabled: Boolean,
        monthTokens: List<String>,
        mediaType: String,
        randomOrder: Boolean,
        randomSeed: Long,
        todayOnly: Boolean,
        now: Long = System.currentTimeMillis(),
        limit: Int,
    ): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE processing_status = :status AND deletion_status = 'none' AND missing_since IS NULL ORDER BY date_taken DESC LIMIT :limit")
    suspend fun nextBatch(status: String = ProcessingStatus.UNPROCESSED, limit: Int): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE deletion_status = 'none' AND missing_since IS NULL ORDER BY date_taken DESC LIMIT :limit")
    suspend fun allActive(limit: Int): List<PhotoEntity>

    @Query("""
        SELECT COUNT(*) FROM photos
        WHERE deletion_status = 'none'
        AND missing_since IS NULL
        AND is_raw = 0
        AND is_gif = 0
        AND width > 0
        AND height > 0
    """)
    suspend fun countSimilarAnalysisCandidates(): Int

    @Query("""
        SELECT * FROM photos
        WHERE deletion_status = 'none'
        AND missing_since IS NULL
        AND is_raw = 0
        AND is_gif = 0
        AND width > 0
        AND height > 0
        ORDER BY date_taken DESC, id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun similarAnalysisCandidates(limit: Int, offset: Int): List<PhotoEntity>

    @Query("""
        SELECT id, media_store_id, stable_media_key, uri, size, date_taken, date_modified, width, height
        FROM photos
        WHERE deletion_status = 'none'
        AND missing_since IS NULL
        AND is_raw = 0
        AND is_gif = 0
        AND width > 0
        AND height > 0
        ORDER BY date_taken DESC, id DESC
    """)
    suspend fun similarAnalysisInputs(): List<SimilarAnalysisInput>

    @Query("SELECT * FROM photos WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<PhotoEntity>

    @Query("""
        SELECT day_key FROM (
            SELECT strftime('%Y-%m-%d', date_taken / 1000, 'unixepoch', 'localtime') AS day_key,
                   MAX(date_taken) AS latest
            FROM photos
            WHERE deletion_status = 'none'
            AND missing_since IS NULL
            AND date_taken > 0
            AND date_taken < :anchorStartMillis
            GROUP BY day_key
            ORDER BY latest DESC
            LIMIT :limit
        )
    """)
    suspend fun memoryDayKeysBefore(anchorStartMillis: Long, limit: Int): List<String>

    @Query("""
        SELECT day_key FROM (
            SELECT strftime('%Y-%m-%d', date_taken / 1000, 'unixepoch', 'localtime') AS day_key,
                   MIN(date_taken) AS earliest
            FROM photos
            WHERE deletion_status = 'none'
            AND missing_since IS NULL
            AND date_taken > 0
            AND date_taken >= :anchorEndMillis
            GROUP BY day_key
            ORDER BY earliest ASC
            LIMIT :limit
        )
    """)
    suspend fun memoryDayKeysAfter(anchorEndMillis: Long, limit: Int): List<String>

    @Query("""
        SELECT * FROM photos
        WHERE deletion_status = 'none'
        AND missing_since IS NULL
        AND strftime('%Y-%m-%d', date_taken / 1000, 'unixepoch', 'localtime') IN (:dayKeys)
        ORDER BY date_taken ASC, id ASC
        LIMIT :limit
    """)
    suspend fun photosForMemoryDayKeys(dayKeys: List<String>, limit: Int): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE media_store_id IN (:mediaStoreIds)")
    suspend fun byMediaStoreIds(mediaStoreIds: List<Long>): List<PhotoEntity>

    @Query("SELECT media_store_id FROM photos WHERE deletion_status = 'none' AND missing_since IS NULL")
    suspend fun activeMediaStoreIds(): List<Long>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): PhotoEntity?

    @Query("SELECT * FROM photos WHERE processing_status = 'favorited' AND deletion_status = 'none' AND missing_since IS NULL ORDER BY updated_at DESC LIMIT :limit")
    fun observeFavorites(limit: Int): Flow<List<PhotoEntity>>

    @Query("UPDATE photos SET processing_status = :status, processed_at = :now, updated_at = :now WHERE id = :id")
    suspend fun updateProcessingStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET deletion_status = :status, deleted_at = :now, updated_at = :now WHERE id IN (:ids)")
    suspend fun updateDeletionStatus(ids: List<Long>, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET processing_status = :processing, deletion_status = :deletion, updated_at = :now WHERE id = :id")
    suspend fun restoreStatus(id: Long, processing: String, deletion: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET relative_path = :relativePath, folder_path = :relativePath, folder_name = :folderName, updated_at = :now WHERE id = :id")
    suspend fun updateFolder(id: Long, relativePath: String, folderName: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET missing_since = NULL, updated_at = :now WHERE media_store_id IN (:mediaStoreIds)")
    suspend fun clearMissingForMediaStoreIds(mediaStoreIds: List<Long>, now: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET missing_since = COALESCE(missing_since, :now), updated_at = :now WHERE deletion_status = 'none'")
    suspend fun markAllMissing(now: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET missing_since = COALESCE(missing_since, :now), updated_at = :now WHERE media_store_id IN (:mediaStoreIds) AND deletion_status = 'none'")
    suspend fun markMissingByMediaStoreIds(mediaStoreIds: List<Long>, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PhotoEntity>)
}

@Dao
interface VideoDao {
    @Query("SELECT COUNT(*) FROM videos WHERE missing_since IS NULL")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM videos WHERE deletion_status = 'none' AND missing_since IS NULL")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM videos WHERE processing_status = :status AND deletion_status = 'none' AND missing_since IS NULL")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM videos WHERE missing_since IS NULL AND (processing_status != 'unprocessed' OR deletion_status != 'none')")
    fun observeProcessedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM videos WHERE deletion_status IN ('pending','trashed') AND missing_since IS NULL")
    fun observeDeleteCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(size), 0) FROM videos WHERE deletion_status IN ('pending','trashed') AND missing_since IS NULL")
    fun observeDeleteSize(): Flow<Long>

    @Query("SELECT * FROM videos WHERE deletion_status = 'none' AND missing_since IS NULL ORDER BY date_taken DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE deletion_status = 'none' AND missing_since IS NULL ORDER BY date_taken DESC LIMIT :limit")
    fun observeTimeline(limit: Int): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE deletion_status = 'none' AND missing_since IS NULL AND strftime('%m-%d', date_taken / 1000, 'unixepoch', 'localtime') = strftime('%m-%d', :now / 1000, 'unixepoch', 'localtime') AND strftime('%Y', date_taken / 1000, 'unixepoch', 'localtime') != strftime('%Y', :now / 1000, 'unixepoch', 'localtime') ORDER BY date_taken DESC LIMIT :limit")
    fun observeTodayInHistory(now: Long = System.currentTimeMillis(), limit: Int = 400): Flow<List<VideoEntity>>

    @Query("SELECT DISTINCT folder_path FROM videos WHERE folder_path IS NOT NULL AND folder_path != '' AND missing_since IS NULL ORDER BY folder_path")
    fun observeFolderPaths(): Flow<List<String>>

    @Query("""
        SELECT * FROM videos
        WHERE processing_status = 'unprocessed' AND deletion_status = 'none'
        AND missing_since IS NULL
        AND (:folderPath IS NULL OR folder_path = :folderPath)
        AND (:startMillis IS NULL OR date_taken >= :startMillis)
        AND (:endMillis IS NULL OR date_taken < :endMillis)
        AND (:multiMonthEnabled = 0 OR strftime('%Y-%m', date_taken / 1000, 'unixepoch', 'localtime') IN (:monthTokens))
        ORDER BY CASE WHEN :randomOrder = 1 THEN ABS((
            ((media_store_id % 2147483647) * ((:randomSeed % 1000003) + 1009))
            + (((date_taken / 1000) % 2147483647) * ((:randomSeed % 65537) + 257))
            + ((size % 1000003) * ((:randomSeed % 4099) + 8191))
            + ((media_store_id % 9973) * ((:randomSeed % 131071) + 17))
        ) % 2147483647) ELSE date_taken END DESC, date_taken DESC
        LIMIT :limit
    """)
    suspend fun nextFilteredBatch(
        folderPath: String?,
        startMillis: Long?,
        endMillis: Long?,
        multiMonthEnabled: Boolean,
        monthTokens: List<String>,
        randomOrder: Boolean,
        randomSeed: Long,
        limit: Int,
    ): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE processing_status = :status AND deletion_status = 'none' AND missing_since IS NULL ORDER BY date_taken DESC LIMIT :limit")
    suspend fun nextBatch(status: String = ProcessingStatus.UNPROCESSED, limit: Int): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): VideoEntity?

    @Query("SELECT * FROM videos WHERE media_store_id IN (:mediaStoreIds)")
    suspend fun byMediaStoreIds(mediaStoreIds: List<Long>): List<VideoEntity>

    @Query("SELECT media_store_id FROM videos WHERE deletion_status = 'none' AND missing_since IS NULL")
    suspend fun activeMediaStoreIds(): List<Long>

    @Query("SELECT * FROM videos WHERE processing_status = 'favorited' AND deletion_status = 'none' AND missing_since IS NULL ORDER BY updated_at DESC LIMIT :limit")
    fun observeFavorites(limit: Int): Flow<List<VideoEntity>>

    @Query("UPDATE videos SET processing_status = :status, processed_at = :now, updated_at = :now WHERE id = :id")
    suspend fun updateProcessingStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE videos SET deletion_status = :status, deleted_at = :now, updated_at = :now WHERE id IN (:ids)")
    suspend fun updateDeletionStatus(ids: List<Long>, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE videos SET processing_status = :processing, deletion_status = :deletion, updated_at = :now WHERE id = :id")
    suspend fun restoreStatus(id: Long, processing: String, deletion: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE videos SET folder_path = :relativePath, folder_name = :folderName, updated_at = :now WHERE id = :id")
    suspend fun updateFolder(id: Long, relativePath: String, folderName: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE videos SET missing_since = NULL, updated_at = :now WHERE media_store_id IN (:mediaStoreIds)")
    suspend fun clearMissingForMediaStoreIds(mediaStoreIds: List<Long>, now: Long = System.currentTimeMillis())

    @Query("UPDATE videos SET missing_since = COALESCE(missing_since, :now), updated_at = :now WHERE deletion_status = 'none'")
    suspend fun markAllMissing(now: Long = System.currentTimeMillis())

    @Query("UPDATE videos SET missing_since = COALESCE(missing_since, :now), updated_at = :now WHERE media_store_id IN (:mediaStoreIds) AND deletion_status = 'none'")
    suspend fun markMissingByMediaStoreIds(mediaStoreIds: List<Long>, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VideoEntity>)
}

@Dao
interface SimilarDao {
    @Query("SELECT * FROM similar_groups WHERE is_dismissed = 0 ORDER BY created_at DESC")
    fun observeGroups(): Flow<List<SimilarGroupEntity>>

    @Query("SELECT * FROM similar_group_photos WHERE group_id = :groupId ORDER BY is_best DESC, similarity DESC")
    suspend fun groupPhotos(groupId: String): List<SimilarGroupPhotoEntity>

    @Query("SELECT * FROM photos WHERE missing_since IS NULL AND deletion_status = 'none' AND id IN (SELECT photo_id FROM similar_group_photos WHERE group_id = :groupId) ORDER BY id")
    suspend fun groupPhotoEntities(groupId: String): List<PhotoEntity>

    @Query("DELETE FROM similar_groups")
    suspend fun clearGroups()

    @Query("DELETE FROM similar_group_photos")
    suspend fun clearGroupPhotos()

    @Query("SELECT photo_id FROM photo_fingerprints")
    suspend fun fingerprintedIds(): List<Long>

    @Query("SELECT * FROM photo_fingerprints WHERE photo_id IN (:photoIds)")
    suspend fun fingerprintsByPhotoIds(photoIds: List<Long>): List<PhotoFingerprintEntity>

    @Query("""
        DELETE FROM photo_fingerprints
        WHERE photo_id NOT IN (
            SELECT id FROM photos
            WHERE deletion_status = 'none'
              AND missing_since IS NULL
              AND is_raw = 0
              AND is_gif = 0
              AND width > 0
              AND height > 0
        )
    """)
    suspend fun deleteStaleFingerprints(): Int

    @Query("SELECT * FROM similar_analysis_tasks WHERE id = 1 LIMIT 1")
    suspend fun analysisTask(): SimilarAnalysisTaskEntity?

    @Query("SELECT * FROM similar_analysis_tasks WHERE id = 1 LIMIT 1")
    fun observeAnalysisTask(): Flow<SimilarAnalysisTaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnalysisTask(task: SimilarAnalysisTaskEntity)

    @Query("DELETE FROM photo_fingerprints")
    suspend fun clearFingerprints()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroups(groups: List<SimilarGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGroupPhotos(items: List<SimilarGroupPhotoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFingerprints(items: List<PhotoFingerprintEntity>)
}

@Dao
interface TrashDao {
    @Query("SELECT COUNT(*) FROM trash_items")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM trash_items ORDER BY trashed_at DESC")
    fun observeTrash(): Flow<List<TrashItemEntity>>

    @Query("SELECT * FROM trash_items WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): TrashItemEntity?

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trash_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM trash_items WHERE media_id = :mediaId AND media_type = :mediaType")
    suspend fun deleteByMedia(mediaId: Long, mediaType: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TrashItemEntity)
}

@Dao
interface OperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: OperationHistoryEntity): Long

    @Query("SELECT * FROM operation_history WHERE is_undone = 0 AND action IN ('Keep', 'Favorite', 'Delete') AND (:mediaType IS NULL OR media_type = :mediaType) ORDER BY created_at DESC, id DESC LIMIT 1")
    suspend fun lastUndoable(mediaType: String?): OperationHistoryEntity?

    @Query("SELECT * FROM operation_history WHERE id = :id AND is_undone = 0 AND action IN ('Keep', 'Favorite', 'Delete') LIMIT 1")
    suspend fun undoableById(id: Long): OperationHistoryEntity?

    @Query("SELECT * FROM operation_history WHERE media_id = :mediaId AND media_type = :mediaType AND action = :action AND is_undone = 0 ORDER BY created_at DESC, id DESC LIMIT 1")
    suspend fun lastByMediaAndAction(mediaId: Long, mediaType: String, action: String): OperationHistoryEntity?

    @Query("UPDATE operation_history SET is_undone = 1 WHERE id = :id")
    suspend fun markUndone(id: Long)

    @Query("DELETE FROM operation_history WHERE is_undone = 1 AND created_at < :cutoffMillis")
    suspend fun deleteUndoneBefore(cutoffMillis: Long): Int
}

@Dao
interface MediaSyncStateDao {
    @Query("SELECT * FROM media_sync_state WHERE sync_key = :syncKey LIMIT 1")
    suspend fun bySyncKey(syncKey: String): MediaSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: MediaSyncStateEntity)
}

data class CleanupEventAggregate(
    @ColumnInfo(name = "photo_delta") val photoDelta: Long,
    @ColumnInfo(name = "video_delta") val videoDelta: Long,
    @ColumnInfo(name = "cleaned_media_count") val cleanedMediaCount: Long,
    @ColumnInfo(name = "favorite_delta") val favoriteDelta: Long,
    @ColumnInfo(name = "favorite_action_count") val favoriteActionCount: Long,
    @ColumnInfo(name = "size_delta_bytes") val sizeDeltaBytes: Long,
    @ColumnInfo(name = "marked_size_bytes") val markedSizeBytes: Long,
    @ColumnInfo(name = "action_count") val actionCount: Long,
    @ColumnInfo(name = "undo_count") val undoCount: Long,
)

data class CleanupEventDeletionAggregate(
    @ColumnInfo(name = "moved_to_trash_count") val movedToTrashCount: Long,
    @ColumnInfo(name = "restored_count") val restoredCount: Long,
)

data class CleanupEventMonthAggregate(
    val month: String,
    @ColumnInfo(name = "cleared_count") val clearedCount: Long,
)

data class CleanupEventFolderAggregate(
    @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "affected_count") val affectedCount: Long,
)

data class DashboardCountsProjection(
    @ColumnInfo(name = "photo_count") val photoCount: Int,
    @ColumnInfo(name = "video_count") val videoCount: Int,
    @ColumnInfo(name = "processed_photo_count") val processedPhotoCount: Int,
    @ColumnInfo(name = "processed_video_count") val processedVideoCount: Int,
    @ColumnInfo(name = "photo_favorite_count") val photoFavoriteCount: Int,
    @ColumnInfo(name = "video_favorite_count") val videoFavoriteCount: Int,
    @ColumnInfo(name = "trash_count") val trashCount: Int,
    @ColumnInfo(name = "photo_pending_delete_count") val photoPendingDeleteCount: Int,
    @ColumnInfo(name = "video_pending_delete_count") val videoPendingDeleteCount: Int,
    @ColumnInfo(name = "photo_pending_delete_bytes") val photoPendingDeleteBytes: Long,
    @ColumnInfo(name = "video_pending_delete_bytes") val videoPendingDeleteBytes: Long,
    @ColumnInfo(name = "today_photo_count") val todayPhotoCount: Int,
    @ColumnInfo(name = "today_video_count") val todayVideoCount: Int,
    @ColumnInfo(name = "today_action_count") val todayActionCount: Int,
)

@Dao
interface StatsDao {
    @Query("""
        SELECT
            (SELECT COUNT(*) FROM photos WHERE missing_since IS NULL) AS photo_count,
            (SELECT COUNT(*) FROM videos WHERE missing_since IS NULL) AS video_count,
            (SELECT COUNT(*) FROM photos WHERE missing_since IS NULL AND (processing_status != 'unprocessed' OR deletion_status != 'none')) AS processed_photo_count,
            (SELECT COUNT(*) FROM videos WHERE missing_since IS NULL AND (processing_status != 'unprocessed' OR deletion_status != 'none')) AS processed_video_count,
            (SELECT COUNT(*) FROM photos WHERE processing_status = 'favorited' AND deletion_status = 'none' AND missing_since IS NULL) AS photo_favorite_count,
            (SELECT COUNT(*) FROM videos WHERE processing_status = 'favorited' AND deletion_status = 'none' AND missing_since IS NULL) AS video_favorite_count,
            (SELECT COUNT(*) FROM trash_items) AS trash_count,
            (SELECT COUNT(*) FROM photos WHERE deletion_status IN ('pending', 'trashed') AND missing_since IS NULL) AS photo_pending_delete_count,
            (SELECT COUNT(*) FROM videos WHERE deletion_status IN ('pending', 'trashed') AND missing_since IS NULL) AS video_pending_delete_count,
            COALESCE((SELECT SUM(size) FROM photos WHERE deletion_status IN ('pending', 'trashed') AND missing_since IS NULL), 0) AS photo_pending_delete_bytes,
            COALESCE((SELECT SUM(size) FROM videos WHERE deletion_status IN ('pending', 'trashed') AND missing_since IS NULL), 0) AS video_pending_delete_bytes,
            MAX(0, CAST(COALESCE((SELECT SUM(photo_delta) FROM cleanup_events WHERE local_date = :localDate), 0) AS INTEGER)) AS today_photo_count,
            MAX(0, CAST(COALESCE((SELECT SUM(video_delta) FROM cleanup_events WHERE local_date = :localDate), 0) AS INTEGER)) AS today_video_count,
            MAX(0, CAST(COALESCE((SELECT SUM(action_count) FROM cleanup_events WHERE local_date = :localDate), 0) AS INTEGER)) AS today_action_count
    """)
    fun observeDashboardCounts(localDate: String): Flow<DashboardCountsProjection>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun observeUserStats(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun userStats(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserStats(stats: UserStatsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserStatsIfAbsent(stats: UserStatsEntity): Long

    @Query("""
        UPDATE user_stats SET
            total_photos_cleared = MAX(0, total_photos_cleared + :photoDelta),
            total_videos_cleared = MAX(0, total_videos_cleared + :videoDelta),
            total_storage_freed = MAX(0, total_storage_freed + :storageDelta),
            total_favorited = MAX(0, total_favorited + :favoriteDelta),
            total_undo_count = MAX(0, total_undo_count + :undoDelta),
            last_active_date = :now,
            updated_at = :now
        WHERE id = 1
    """)
    suspend fun adjustUserStats(
        photoDelta: Int,
        videoDelta: Int,
        storageDelta: Long,
        favoriteDelta: Int,
        undoDelta: Int,
        now: Long,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCleanupEvent(event: CleanupEventEntity)

    @Query("SELECT * FROM cleanup_events WHERE source_ref = :sourceRef AND reversal_of_event_id IS NULL ORDER BY occurred_at DESC LIMIT 1")
    suspend fun eventBySourceRef(sourceRef: String): CleanupEventEntity?

    @Query("SELECT MAX(0, CAST(COALESCE(SUM(photo_delta), 0) AS INTEGER)) FROM cleanup_events WHERE local_date = :localDate")
    fun observeTodayPhotoCount(localDate: String): Flow<Int>

    @Query("SELECT MAX(0, CAST(COALESCE(SUM(video_delta), 0) AS INTEGER)) FROM cleanup_events WHERE local_date = :localDate")
    fun observeTodayVideoCount(localDate: String): Flow<Int>

    @Query("SELECT MAX(0, CAST(COALESCE(SUM(action_count), 0) AS INTEGER)) FROM cleanup_events WHERE local_date = :localDate")
    fun observeTodayActionCount(localDate: String): Flow<Int>

    @Query("""
        SELECT
            COALESCE(SUM(photo_delta), 0) AS photo_delta,
            COALESCE(SUM(video_delta), 0) AS video_delta,
            COALESCE(SUM(CASE WHEN event_type IN ('kept', 'favorited', 'moved_to_app_trash', 'archived_moved') THEN affected_media_count ELSE 0 END), 0) AS cleaned_media_count,
            COALESCE(SUM(favorite_delta), 0) AS favorite_delta,
            COALESCE(SUM(CASE WHEN event_type = 'favorited' AND favorite_delta > 0 THEN favorite_delta ELSE 0 END), 0) AS favorite_action_count,
            COALESCE(SUM(size_delta_bytes), 0) AS size_delta_bytes,
            COALESCE(SUM(CASE WHEN event_type = 'moved_to_app_trash' THEN size_delta_bytes ELSE 0 END), 0) AS marked_size_bytes,
            COALESCE(SUM(action_count), 0) AS action_count,
            COALESCE(SUM(CASE WHEN event_type IN ('restored', 'compensated') THEN action_count ELSE 0 END), 0) AS undo_count
        FROM cleanup_events
        WHERE occurred_at >= :startMillis AND occurred_at < :endMillis
    """)
    suspend fun aggregateEvents(startMillis: Long, endMillis: Long): CleanupEventAggregate

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN event_type = 'moved_to_app_trash' THEN affected_media_count ELSE 0 END), 0) AS moved_to_trash_count,
            COALESCE(SUM(CASE WHEN event_type = 'restored' THEN affected_media_count ELSE 0 END), 0) AS restored_count
        FROM cleanup_events
        WHERE occurred_at >= :startMillis AND occurred_at < :endMillis
    """)
    suspend fun aggregateDeletionEvents(startMillis: Long, endMillis: Long): CleanupEventDeletionAggregate

    @Query("""
        SELECT substr(local_date, 6, 2) AS month,
            COALESCE(SUM(photo_delta + video_delta), 0) AS cleared_count
        FROM cleanup_events
        WHERE occurred_at >= :startMillis AND occurred_at < :endMillis
        GROUP BY substr(local_date, 6, 2)
        ORDER BY month
    """)
    suspend fun monthlyEventCounts(startMillis: Long, endMillis: Long): List<CleanupEventMonthAggregate>

    @Query("""
        SELECT folder_path, SUM(affected_media_count) AS affected_count
        FROM cleanup_events
        WHERE occurred_at >= :startMillis AND occurred_at < :endMillis
          AND folder_path IS NOT NULL AND folder_path != ''
          AND event_type IN ('kept', 'favorited', 'moved_to_app_trash', 'archived_moved')
        GROUP BY folder_path
        ORDER BY affected_count DESC, folder_path ASC
        LIMIT 1
    """)
    suspend fun topFolderForPeriod(startMillis: Long, endMillis: Long): CleanupEventFolderAggregate?

    @Query("SELECT DISTINCT local_date FROM cleanup_events WHERE event_type IN ('kept', 'favorited', 'moved_to_app_trash', 'archived_moved') AND affected_media_count > 0 AND action_count > 0 ORDER BY local_date")
    suspend fun qualifyingCleanupDates(): List<String>

    @Query("SELECT * FROM achievement_progress WHERE achievement_id = :achievementId LIMIT 1")
    suspend fun achievementProgress(achievementId: String): AchievementProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievementProgress(progress: AchievementProgressEntity)
}
