package com.futureape.kanleme.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT COUNT(*) FROM photos")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE deletion_status = 'none'")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE processing_status = :status AND deletion_status = 'none'")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE processing_status != 'unprocessed' OR deletion_status != 'none'")
    fun observeProcessedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photos WHERE deletion_status IN ('pending','trashed')")
    fun observeDeleteCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(size), 0) FROM photos WHERE deletion_status IN ('pending','trashed')")
    fun observeDeleteSize(): Flow<Long>

    @Query("SELECT * FROM photos WHERE deletion_status = 'none' ORDER BY date_taken DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE deletion_status = 'none' ORDER BY date_taken DESC LIMIT :limit")
    fun observeTimeline(limit: Int): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE deletion_status = 'none' AND strftime('%m-%d', date_taken / 1000, 'unixepoch', 'localtime') = strftime('%m-%d', :now / 1000, 'unixepoch', 'localtime') AND strftime('%Y', date_taken / 1000, 'unixepoch', 'localtime') != strftime('%Y', :now / 1000, 'unixepoch', 'localtime') ORDER BY date_taken DESC LIMIT :limit")
    fun observeTodayInHistory(now: Long = System.currentTimeMillis(), limit: Int = 400): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE deletion_status = 'none' AND date_added >= :sinceSeconds ORDER BY date_added DESC LIMIT :limit")
    fun observeRecentlyAdded(sinceSeconds: Long, limit: Int = 120): Flow<List<PhotoEntity>>

    @Query("SELECT DISTINCT folder_path FROM photos WHERE folder_path IS NOT NULL AND folder_path != '' ORDER BY folder_path")
    fun observeFolderPaths(): Flow<List<String>>

    @Query("""
        SELECT COUNT(*) FROM photos WHERE deletion_status = 'none'
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
        AND (:folderPath IS NULL OR folder_path = :folderPath)
        AND (:startMillis IS NULL OR date_taken >= :startMillis)
        AND (:endMillis IS NULL OR date_taken < :endMillis)
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
        mediaType: String,
        randomOrder: Boolean,
        randomSeed: Long,
        todayOnly: Boolean,
        now: Long = System.currentTimeMillis(),
        limit: Int,
    ): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE processing_status = :status AND deletion_status = 'none' ORDER BY date_taken DESC LIMIT :limit")
    suspend fun nextBatch(status: String = ProcessingStatus.UNPROCESSED, limit: Int): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE deletion_status = 'none' ORDER BY date_taken DESC LIMIT :limit")
    suspend fun allActive(limit: Int): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id IN (:ids)")
    suspend fun byIds(ids: List<Long>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE media_store_id IN (:mediaStoreIds)")
    suspend fun byMediaStoreIds(mediaStoreIds: List<Long>): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): PhotoEntity?

    @Query("SELECT * FROM photos WHERE processing_status = 'favorited' AND deletion_status = 'none' ORDER BY updated_at DESC LIMIT :limit")
    fun observeFavorites(limit: Int): Flow<List<PhotoEntity>>

    @Query("UPDATE photos SET processing_status = :status, processed_at = :now, updated_at = :now WHERE id = :id")
    suspend fun updateProcessingStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET deletion_status = :status, deleted_at = :now, updated_at = :now WHERE id IN (:ids)")
    suspend fun updateDeletionStatus(ids: List<Long>, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET processing_status = :processing, deletion_status = :deletion, updated_at = :now WHERE id = :id")
    suspend fun restoreStatus(id: Long, processing: String, deletion: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE photos SET relative_path = :relativePath, folder_path = :relativePath, folder_name = :folderName, updated_at = :now WHERE id = :id")
    suspend fun updateFolder(id: Long, relativePath: String, folderName: String, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PhotoEntity>)
}

@Dao
interface VideoDao {
    @Query("SELECT COUNT(*) FROM videos")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM videos WHERE deletion_status = 'none'")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM videos WHERE processing_status = :status AND deletion_status = 'none'")
    fun observeCountByStatus(status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM videos WHERE processing_status != 'unprocessed' OR deletion_status != 'none'")
    fun observeProcessedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM videos WHERE deletion_status IN ('pending','trashed')")
    fun observeDeleteCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(size), 0) FROM videos WHERE deletion_status IN ('pending','trashed')")
    fun observeDeleteSize(): Flow<Long>

    @Query("SELECT * FROM videos WHERE deletion_status = 'none' ORDER BY date_taken DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE deletion_status = 'none' AND strftime('%m-%d', date_taken / 1000, 'unixepoch', 'localtime') = strftime('%m-%d', :now / 1000, 'unixepoch', 'localtime') AND strftime('%Y', date_taken / 1000, 'unixepoch', 'localtime') != strftime('%Y', :now / 1000, 'unixepoch', 'localtime') ORDER BY date_taken DESC LIMIT :limit")
    fun observeTodayInHistory(now: Long = System.currentTimeMillis(), limit: Int = 400): Flow<List<VideoEntity>>

    @Query("SELECT DISTINCT folder_path FROM videos WHERE folder_path IS NOT NULL AND folder_path != '' ORDER BY folder_path")
    fun observeFolderPaths(): Flow<List<String>>

    @Query("""
        SELECT * FROM videos
        WHERE processing_status = 'unprocessed' AND deletion_status = 'none'
        AND (:folderPath IS NULL OR folder_path = :folderPath)
        AND (:startMillis IS NULL OR date_taken >= :startMillis)
        AND (:endMillis IS NULL OR date_taken < :endMillis)
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
        randomOrder: Boolean,
        randomSeed: Long,
        limit: Int,
    ): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE processing_status = :status AND deletion_status = 'none' ORDER BY date_taken DESC LIMIT :limit")
    suspend fun nextBatch(status: String = ProcessingStatus.UNPROCESSED, limit: Int): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): VideoEntity?

    @Query("SELECT * FROM videos WHERE media_store_id IN (:mediaStoreIds)")
    suspend fun byMediaStoreIds(mediaStoreIds: List<Long>): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE processing_status = 'favorited' AND deletion_status = 'none' ORDER BY updated_at DESC LIMIT :limit")
    fun observeFavorites(limit: Int): Flow<List<VideoEntity>>

    @Query("UPDATE videos SET processing_status = :status, processed_at = :now, updated_at = :now WHERE id = :id")
    suspend fun updateProcessingStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE videos SET deletion_status = :status, deleted_at = :now, updated_at = :now WHERE id IN (:ids)")
    suspend fun updateDeletionStatus(ids: List<Long>, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE videos SET processing_status = :processing, deletion_status = :deletion, updated_at = :now WHERE id = :id")
    suspend fun restoreStatus(id: Long, processing: String, deletion: String, now: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VideoEntity>)
}

@Dao
interface SimilarDao {
    @Query("SELECT * FROM similar_groups WHERE is_dismissed = 0 ORDER BY created_at DESC")
    fun observeGroups(): Flow<List<SimilarGroupEntity>>

    @Query("SELECT * FROM similar_group_photos WHERE group_id = :groupId ORDER BY is_best DESC, similarity DESC")
    suspend fun groupPhotos(groupId: String): List<SimilarGroupPhotoEntity>

    @Query("SELECT * FROM photos WHERE id IN (SELECT photo_id FROM similar_group_photos WHERE group_id = :groupId) ORDER BY id")
    suspend fun groupPhotoEntities(groupId: String): List<PhotoEntity>

    @Query("DELETE FROM similar_groups")
    suspend fun clearGroups()

    @Query("DELETE FROM similar_group_photos")
    suspend fun clearGroupPhotos()

    @Query("SELECT photo_id FROM photo_fingerprints")
    suspend fun fingerprintedIds(): List<Long>

    @Query("SELECT * FROM photo_fingerprints")
    suspend fun allFingerprints(): List<PhotoFingerprintEntity>

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
    suspend fun insert(item: OperationHistoryEntity)

    @Query("SELECT * FROM operation_history WHERE is_undone = 0 ORDER BY created_at DESC LIMIT 1")
    suspend fun lastUndoable(): OperationHistoryEntity?

    @Query("UPDATE operation_history SET is_undone = 1 WHERE id = :id")
    suspend fun markUndone(id: Long)
}

@Dao
interface StatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun observeUserStats(): Flow<UserStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserStats(stats: UserStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCleanupEvent(event: CleanupEventEntity)

    @Query("SELECT CAST(COALESCE(SUM(photo_count), 0) AS INTEGER) FROM cleanup_events WHERE local_date = :localDate")
    fun observeTodayPhotoCount(localDate: String): Flow<Int>

    @Query("SELECT CAST(COALESCE(SUM(video_count), 0) AS INTEGER) FROM cleanup_events WHERE local_date = :localDate")
    fun observeTodayVideoCount(localDate: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM cleanup_events WHERE local_date = :localDate")
    fun observeTodayActionCount(localDate: String): Flow<Int>
}
