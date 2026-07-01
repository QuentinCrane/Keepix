package com.futureape.kanleme.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PhotoEntity::class,
        VideoEntity::class,
        PhotoFingerprintEntity::class,
        LshBucketEntity::class,
        SimilarGroupEntity::class,
        SimilarGroupPhotoEntity::class,
        TrashItemEntity::class,
        FolderFilterEntity::class,
        FolderUsageEntity::class,
        SwipeStatisticsEntity::class,
        SwipeStatSessionEntity::class,
        UserStatsEntity::class,
        AchievementProgressEntity::class,
        CleanupEventEntity::class,
        BackupImportAuditLogEntity::class,
        DeviceModelCacheEntity::class,
        OperationHistoryEntity::class,
    ],
    version = 16,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun videoDao(): VideoDao
    abstract fun similarDao(): SimilarDao
    abstract fun trashDao(): TrashDao
    abstract fun statsDao(): StatsDao
    abstract fun operationDao(): OperationDao
}
