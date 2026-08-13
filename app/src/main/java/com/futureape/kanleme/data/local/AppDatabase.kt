package com.futureape.kanleme.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PhotoEntity::class,
        VideoEntity::class,
        PhotoFingerprintEntity::class,
        SimilarGroupEntity::class,
        SimilarGroupPhotoEntity::class,
        TrashItemEntity::class,
        UserStatsEntity::class,
        AchievementProgressEntity::class,
        CleanupEventEntity::class,
        OperationHistoryEntity::class,
        SimilarAnalysisTaskEntity::class,
        MediaSyncStateEntity::class,
    ],
    version = 20,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun videoDao(): VideoDao
    abstract fun similarDao(): SimilarDao
    abstract fun trashDao(): TrashDao
    abstract fun statsDao(): StatsDao
    abstract fun operationDao(): OperationDao
    abstract fun mediaSyncStateDao(): MediaSyncStateDao
}
