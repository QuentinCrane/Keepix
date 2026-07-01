package com.futureape.kanleme.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
    indices = [
        Index("date_taken"), Index("folder_path"), Index("processing_status"),
        Index("deletion_status"), Index("is_motion_photo"), Index("is_screenshot"),
        Index("is_selfie"), Index("is_gif"), Index("is_raw"), Index("is_long_image"),
        Index("location_name"), Index("stable_media_key"), Index("missing_since"),
        Index(value = ["media_store_id"], unique = true),
        Index(value = ["processing_status", "deletion_status"])
    ]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "media_store_id") val mediaStoreId: Long,
    @ColumnInfo(name = "stable_media_key") val stableMediaKey: String = "",
    @ColumnInfo(name = "identity_version") val identityVersion: Int = 0,
    @ColumnInfo(name = "missing_since") val missingSince: Long? = null,
    val uri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val size: Long,
    @ColumnInfo(name = "date_taken") val dateTaken: Long,
    @ColumnInfo(name = "date_added") val dateAdded: Long,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
    @ColumnInfo(name = "relative_path") val relativePath: String? = null,
    @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "folder_name") val folderName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    val width: Int,
    val height: Int,
    @ColumnInfo(name = "is_motion_photo") val isMotionPhoto: Boolean = false,
    @ColumnInfo(name = "motion_video_uri") val motionVideoUri: String? = null,
    @ColumnInfo(name = "motion_video_offset") val motionVideoOffset: Long = 0,
    @ColumnInfo(name = "motion_video_size") val motionVideoSize: Long = 0,
    @ColumnInfo(name = "is_separate_video") val isSeparateVideo: Boolean = false,
    @ColumnInfo(name = "is_screenshot") val isScreenshot: Boolean = false,
    @ColumnInfo(name = "is_selfie") val isSelfie: Boolean = false,
    @ColumnInfo(name = "is_gif") val isGif: Boolean = false,
    @ColumnInfo(name = "is_raw") val isRaw: Boolean = false,
    @ColumnInfo(name = "is_long_image") val isLongImage: Boolean = false,
    @ColumnInfo(name = "motion_photo_needs_detection") val motionPhotoNeedsDetection: Boolean = false,
    @ColumnInfo(name = "exif_latitude") val exifLatitude: Double? = null,
    @ColumnInfo(name = "exif_longitude") val exifLongitude: Double? = null,
    @ColumnInfo(name = "location_name") val locationName: String? = null,
    @ColumnInfo(name = "exif_make") val exifMake: String? = null,
    @ColumnInfo(name = "exif_model") val exifModel: String? = null,
    @ColumnInfo(name = "exif_orientation") val exifOrientation: Int? = null,
    @ColumnInfo(name = "exif_width") val exifWidth: Int? = null,
    @ColumnInfo(name = "exif_height") val exifHeight: Int? = null,
    @ColumnInfo(name = "exif_focal_length") val exifFocalLength: String? = null,
    @ColumnInfo(name = "exif_aperture") val exifAperture: String? = null,
    @ColumnInfo(name = "exif_iso") val exifIso: String? = null,
    @ColumnInfo(name = "exif_exposure_time") val exifExposureTime: String? = null,
    @ColumnInfo(name = "exif_white_balance") val exifWhiteBalance: String? = null,
    @ColumnInfo(name = "exif_flash") val exifFlash: String? = null,
    @ColumnInfo(name = "exif_metering_mode") val exifMeteringMode: String? = null,
    @ColumnInfo(name = "exif_color_space") val exifColorSpace: String? = null,
    @ColumnInfo(name = "exif_lens_make") val exifLensMake: String? = null,
    @ColumnInfo(name = "exif_lens_model") val exifLensModel: String? = null,
    @ColumnInfo(name = "exif_software") val exifSoftware: String? = null,
    @ColumnInfo(name = "exif_scanned") val exifScanned: Boolean = false,
    @ColumnInfo(name = "processing_status") val processingStatus: String = ProcessingStatus.UNPROCESSED,
    @ColumnInfo(name = "deletion_status") val deletionStatus: String = DeletionStatus.NONE,
    @ColumnInfo(name = "processed_at") val processedAt: Long? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "delete_error") val deleteError: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "videos",
    indices = [
        Index("date_taken"), Index("folder_path"), Index("processing_status"),
        Index("deletion_status"), Index("stable_media_key"), Index("missing_since"),
        Index(value = ["media_store_id"], unique = true),
        Index(value = ["processing_status", "deletion_status"])
    ]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "media_store_id") val mediaStoreId: Long,
    @ColumnInfo(name = "stable_media_key") val stableMediaKey: String = "",
    @ColumnInfo(name = "identity_version") val identityVersion: Int = 0,
    @ColumnInfo(name = "missing_since") val missingSince: Long? = null,
    val uri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val size: Long,
    val duration: Long,
    @ColumnInfo(name = "date_taken") val dateTaken: Long,
    @ColumnInfo(name = "date_added") val dateAdded: Long,
    @ColumnInfo(name = "date_modified") val dateModified: Long,
    @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "folder_name") val folderName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    val width: Int,
    val height: Int,
    @ColumnInfo(name = "is_motion_photo_video") val isMotionPhotoVideo: Boolean = false,
    @ColumnInfo(name = "exif_latitude") val exifLatitude: Double? = null,
    @ColumnInfo(name = "exif_longitude") val exifLongitude: Double? = null,
    @ColumnInfo(name = "location_name") val locationName: String? = null,
    @ColumnInfo(name = "exif_make") val exifMake: String? = null,
    @ColumnInfo(name = "exif_model") val exifModel: String? = null,
    @ColumnInfo(name = "exif_scanned") val exifScanned: Boolean = false,
    @ColumnInfo(name = "processing_status") val processingStatus: String = ProcessingStatus.UNPROCESSED,
    @ColumnInfo(name = "deletion_status") val deletionStatus: String = DeletionStatus.NONE,
    @ColumnInfo(name = "processed_at") val processedAt: Long? = null,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "delete_error") val deleteError: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "photo_fingerprints",
    foreignKeys = [ForeignKey(entity = PhotoEntity::class, parentColumns = ["id"], childColumns = ["photo_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("photo_id"), Index("p_hash_prefix")]
)
data class PhotoFingerprintEntity(
    @PrimaryKey @ColumnInfo(name = "photo_id") val photoId: Long,
    @ColumnInfo(name = "p_hash") val pHash: Long,
    @ColumnInfo(name = "d_hash") val dHash: Long,
    @ColumnInfo(name = "a_hash") val aHash: Long = 0,
    @ColumnInfo(name = "color_histogram") val colorHistogram: String = "",
    @ColumnInfo(name = "p_hash_prefix") val pHashPrefix: Long,
    @ColumnInfo(name = "quality_score") val qualityScore: Double,
    val sharpness: Double,
    val exposure: Double,
    @ColumnInfo(name = "computed_at") val computedAt: Long,
)

@Entity(
    tableName = "lsh_buckets",
    primaryKeys = ["table_index", "bucket_key", "photo_id"],
    foreignKeys = [ForeignKey(entity = PhotoFingerprintEntity::class, parentColumns = ["photo_id"], childColumns = ["photo_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("photo_id"), Index(value = ["table_index", "bucket_key"])]
)
data class LshBucketEntity(
    @ColumnInfo(name = "table_index") val tableIndex: Int,
    @ColumnInfo(name = "bucket_key") val bucketKey: Long,
    @ColumnInfo(name = "photo_id") val photoId: Long,
)

@Entity(tableName = "similar_groups", indices = [Index("created_at"), Index("type")])
data class SimilarGroupEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "best_photo_id") val bestPhotoId: Long,
    val type: String,
    @ColumnInfo(name = "average_similarity") val averageSimilarity: Double,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "is_dismissed") val isDismissed: Boolean = false,
)

@Entity(
    tableName = "similar_group_photos",
    primaryKeys = ["group_id", "photo_id"],
    indices = [Index("group_id"), Index("photo_id")]
)
data class SimilarGroupPhotoEntity(
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "photo_id") val photoId: Long,
    val similarity: Double,
    @ColumnInfo(name = "is_best") val isBest: Boolean = false,
)

@Entity(tableName = "trash_items", indices = [Index("media_type"), Index("trashed_at"), Index(value = ["media_id", "media_type"])])
data class TrashItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "media_type") val mediaType: String,
    @ColumnInfo(name = "media_store_id") val mediaStoreId: Long,
    val uri: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    val size: Long,
    @ColumnInfo(name = "date_taken") val dateTaken: Long,
    @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "folder_name") val folderName: String,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    val width: Int,
    val height: Int,
    val duration: Long? = null,
    @ColumnInfo(name = "trashed_at") val trashedAt: Long,
    @ColumnInfo(name = "auto_delete_at") val autoDeleteAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "folder_filters")
data class FolderFilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "folder_name") val folderName: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean,
)

@Entity(tableName = "folder_usage")
data class FolderUsageEntity(
    @PrimaryKey @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "folder_name") val folderName: String,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long,
    @ColumnInfo(name = "usage_count") val usageCount: Int,
    @ColumnInfo(name = "photo_last_used_at") val photoLastUsedAt: Long? = null,
    @ColumnInfo(name = "photo_usage_count") val photoUsageCount: Int = 0,
    @ColumnInfo(name = "video_last_used_at") val videoLastUsedAt: Long? = null,
    @ColumnInfo(name = "video_usage_count") val videoUsageCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "swipe_statistics")
data class SwipeStatisticsEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "swipe_count") val swipeCount: Int = 0,
    @ColumnInfo(name = "keep_count") val keepCount: Int = 0,
    @ColumnInfo(name = "favorite_count") val favoriteCount: Int = 0,
    @ColumnInfo(name = "delete_count") val deleteCount: Int = 0,
    @ColumnInfo(name = "undo_count") val undoCount: Int = 0,
    @ColumnInfo(name = "session_minutes") val sessionMinutes: Int = 0,
    @ColumnInfo(name = "night_swipe_count") val nightSwipeCount: Int = 0,
    @ColumnInfo(name = "deleted_size_bytes") val deletedSizeBytes: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "swipe_stat_sessions", indices = [Index("date")])
data class SwipeStatSessionEntity(
    @PrimaryKey @ColumnInfo(name = "session_id") val sessionId: String,
    val date: String,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "total_photos_cleared") val totalPhotosCleared: Int = 0,
    @ColumnInfo(name = "total_videos_cleared") val totalVideosCleared: Int = 0,
    @ColumnInfo(name = "total_storage_freed") val totalStorageFreed: Long = 0,
    @ColumnInfo(name = "current_streak") val currentStreak: Int = 0,
    @ColumnInfo(name = "longest_streak") val longestStreak: Int = 0,
    @ColumnInfo(name = "last_active_date") val lastActiveDate: Long? = null,
    @ColumnInfo(name = "batch_delete_count") val batchDeleteCount: Int = 0,
    @ColumnInfo(name = "similar_photo_clean_count") val similarPhotoCleanCount: Int = 0,
    @ColumnInfo(name = "total_favorited") val totalFavorited: Int = 0,
    @ColumnInfo(name = "total_undo_count") val totalUndoCount: Int = 0,
    @ColumnInfo(name = "night_cleaning_count") val nightCleaningCount: Int = 0,
    @ColumnInfo(name = "total_session_minutes") val totalSessionMinutes: Int = 0,
    @ColumnInfo(name = "max_single_session_cleared") val maxSingleSessionCleared: Int = 0,
    @ColumnInfo(name = "max_single_session_storage") val maxSingleSessionStorage: Long = 0,
    @ColumnInfo(name = "fast_cleaning_count") val fastCleaningCount: Int = 0,
    @ColumnInfo(name = "features_used") val featuresUsed: String = "[]",
    @ColumnInfo(name = "app_launch_count") val appLaunchCount: Int = 0,
    @ColumnInfo(name = "achievement_view_count") val achievementViewCount: Int = 0,
    @ColumnInfo(name = "cleaning_days_count") val cleaningDaysCount: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "achievement_progress", indices = [Index("unlocked"), Index("unlocked_at")])
data class AchievementProgressEntity(
    @PrimaryKey @ColumnInfo(name = "achievement_id") val achievementId: String,
    @ColumnInfo(name = "current_value") val currentValue: Int,
    val unlocked: Boolean,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "cleanup_events", indices = [Index("event_type"), Index("local_date"), Index("occurred_at")])
data class CleanupEventEntity(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "photo_count") val photoCount: Int,
    @ColumnInfo(name = "video_count") val videoCount: Int,
    @ColumnInfo(name = "freed_bytes") val freedBytes: Long,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "source_ref") val sourceRef: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "backup_import_audit_logs", indices = [Index("backup_file_id"), Index("created_at")])
data class BackupImportAuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "backup_file_id") val backupFileId: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    val detail: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "device_model_cache", primaryKeys = ["model_norm", "brand_norm", "language_tag"])
data class DeviceModelCacheEntity(
    @ColumnInfo(name = "model_norm") val modelNorm: String,
    @ColumnInfo(name = "brand_norm") val brandNorm: String,
    @ColumnInfo(name = "language_tag") val languageTag: String,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    @ColumnInfo(name = "brand_display") val brandDisplay: String? = null,
    @ColumnInfo(name = "fallback_name") val fallbackName: String? = null,
    @ColumnInfo(name = "source_type") val sourceType: String? = null,
    val confidence: Double? = null,
    @ColumnInfo(name = "data_version") val dataVersion: String? = null,
    @ColumnInfo(name = "last_hit") val lastHit: Long = 0,
    @ColumnInfo(name = "last_lookup_at") val lastLookupAt: Long = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = 0,
)

@Entity(tableName = "operation_history", indices = [Index("created_at"), Index("media_type")])
data class OperationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "media_type") val mediaType: String,
    @ColumnInfo(name = "previous_processing_status") val previousProcessingStatus: String,
    @ColumnInfo(name = "previous_deletion_status") val previousDeletionStatus: String,
    @ColumnInfo(name = "new_processing_status") val newProcessingStatus: String,
    @ColumnInfo(name = "new_deletion_status") val newDeletionStatus: String,
    val action: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_undone") val isUndone: Boolean = false,
)

object ProcessingStatus {
    const val UNPROCESSED = "unprocessed"
    const val KEPT = "kept"
    const val FAVORITED = "favorited"
    const val DELETED = "deleted"
}

object DeletionStatus {
    const val NONE = "none"
    const val PENDING = "pending"
    const val TRASHED = "trashed"
    const val DELETED = "deleted"
    const val FAILED = "failed"
}
