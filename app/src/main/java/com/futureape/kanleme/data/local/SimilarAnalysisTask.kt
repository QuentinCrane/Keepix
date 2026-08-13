package com.futureape.kanleme.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object SimilarAnalysisTaskStatus {
    const val IDLE = "idle"
    const val QUEUED = "queued"
    const val RUNNING = "running"
    const val CANCEL_REQUESTED = "cancel_requested"
    const val PAUSED = "paused"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
    const val FAILED = "failed"

    fun isRunning(status: String): Boolean = status == QUEUED || status == RUNNING || status == CANCEL_REQUESTED

    fun isCancellable(status: String): Boolean = status == QUEUED ||
        status == RUNNING ||
        status == CANCEL_REQUESTED ||
        status == PAUSED ||
        status == FAILED
}

object SimilarAnalysisContract {
    const val TASK_ID = 1
    const val ALGORITHM_VERSION = 1
    const val DEFAULT_LIMIT = 3_600
    const val PAGE_SIZE = 24
}

@Entity(
    tableName = "similar_analysis_tasks",
    indices = [Index("status"), Index("updated_at")],
)
data class SimilarAnalysisTaskEntity(
    @PrimaryKey val id: Int = SimilarAnalysisContract.TASK_ID,
    val status: String = SimilarAnalysisTaskStatus.IDLE,
    @ColumnInfo(name = "run_token") val runToken: String = "",
    @ColumnInfo(name = "media_version") val mediaVersion: String = "",
    @ColumnInfo(name = "access_key") val accessKey: String = "",
    @ColumnInfo(name = "algorithm_version") val algorithmVersion: Int = SimilarAnalysisContract.ALGORITHM_VERSION,
    @ColumnInfo(name = "requested_limit") val requestedLimit: Int = SimilarAnalysisContract.DEFAULT_LIMIT,
    val cursor: Int = 0,
    @ColumnInfo(name = "total_photos") val totalPhotos: Int = 0,
    val stage: String = "未开始",
    @ColumnInfo(name = "cancel_requested") val cancelRequested: Boolean = false,
    @ColumnInfo(name = "last_result_count") val lastResultCount: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

data class SimilarAnalysisRunResult(
    val status: String,
    val resultCount: Int,
)

class SimilarAnalysisInputChangedException : IllegalStateException("媒体库在相似照片检测期间发生变化，任务已重新准备")
