package com.futureape.kanleme.data.worker

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.futureape.kanleme.data.local.SimilarAnalysisInputChangedException
import com.futureape.kanleme.data.local.SimilarAnalysisTaskStatus
import com.futureape.kanleme.data.media.MediaAccessSnapshot
import com.futureape.kanleme.data.repository.AppRepository
import com.futureape.kanleme.data.settings.AppSettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

enum class SimilarAnalysisWorkMode {
    AUTO,
    MANUAL,
}

object SimilarAnalysisWorkContract {
    const val UNIQUE_WORK_NAME = "kanleme-similar-photo-analysis"
    const val INPUT_MODE = "mode"
    const val MODE_AUTO = "auto"
    const val MODE_MANUAL = "manual"
    const val MAX_RETRY_ATTEMPTS = 4

    data class Spec(
        val mode: SimilarAnalysisWorkMode,
        val requiresDeviceIdle: Boolean,
        val existingWorkPolicy: ExistingWorkPolicy,
        val backoffSeconds: Long,
    )

    fun specFor(mode: SimilarAnalysisWorkMode): Spec = Spec(
        mode = mode,
        requiresDeviceIdle = mode == SimilarAnalysisWorkMode.AUTO,
        existingWorkPolicy = if (mode == SimilarAnalysisWorkMode.MANUAL) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
        backoffSeconds = 30,
    )

    fun modeValue(mode: SimilarAnalysisWorkMode): String = when (mode) {
        SimilarAnalysisWorkMode.AUTO -> MODE_AUTO
        SimilarAnalysisWorkMode.MANUAL -> MODE_MANUAL
    }
}

@Singleton
class SimilarAnalysisScheduler @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun enqueue(mode: SimilarAnalysisWorkMode) {
        val spec = SimilarAnalysisWorkContract.specFor(mode)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .apply {
                if (spec.requiresDeviceIdle && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setRequiresDeviceIdle(true)
                }
            }
            .build()
        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<SimilarAnalysisWorker>()
            .setInputData(Data.Builder().putString(SimilarAnalysisWorkContract.INPUT_MODE, SimilarAnalysisWorkContract.modeValue(mode)).build())
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, spec.backoffSeconds, TimeUnit.SECONDS)
            .addTag(SimilarAnalysisWorkContract.UNIQUE_WORK_NAME)
            .build()
        workManager.enqueueUniqueWork(SimilarAnalysisWorkContract.UNIQUE_WORK_NAME, spec.existingWorkPolicy, request)
    }

    fun cancel() {
        workManager.cancelUniqueWork(SimilarAnalysisWorkContract.UNIQUE_WORK_NAME)
    }
}

@HiltWorker
class SimilarAnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AppRepository,
    private val settingsRepository: AppSettingsRepository,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val isAutomatic = inputData.getString(SimilarAnalysisWorkContract.INPUT_MODE) == SimilarAnalysisWorkContract.MODE_AUTO
        if (isAutomatic && !settingsRepository.settings.first().similarDetection) {
            repository.cancelSimilarAnalysis()
            repository.markSimilarAnalysisCancelled()
            return Result.success()
        }

        val existing = repository.similarAnalysisTask() ?: return Result.success()
        if (existing.status == SimilarAnalysisTaskStatus.CANCELLED || existing.cancelRequested || existing.status == SimilarAnalysisTaskStatus.CANCEL_REQUESTED) {
            repository.markSimilarAnalysisCancelled()
            return Result.success()
        }

        val access = MediaAccessSnapshot.current(applicationContext)
        if (!access.isGranted) {
            repository.pauseSimilarAnalysis("等待照片访问权限")
            return Result.success()
        }
        val task = if (existing.accessKey == access.key) {
            existing
        } else {
            repository.prepareSimilarAnalysis(
                accessKey = access.key,
                limit = existing.requestedLimit,
                forceRestart = false,
            )
        }
        if (task.status == SimilarAnalysisTaskStatus.COMPLETED && !task.cancelRequested) return Result.success()

        return try {
            val result = repository.runSimilarAnalysis(task)
            when (result.status) {
                SimilarAnalysisTaskStatus.COMPLETED,
                SimilarAnalysisTaskStatus.CANCELLED,
                SimilarAnalysisTaskStatus.PAUSED,
                -> Result.success()
                else -> Result.retry()
            }
        } catch (_: SimilarAnalysisInputChangedException) {
            if (runAttemptCount < SimilarAnalysisWorkContract.MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            if (runAttemptCount < SimilarAnalysisWorkContract.MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
        }
    }
}
