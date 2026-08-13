package com.futureape.kanleme.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.SimilarAnalysisContract
import com.futureape.kanleme.data.local.SimilarAnalysisTaskEntity
import com.futureape.kanleme.data.local.SimilarAnalysisTaskStatus
import com.futureape.kanleme.data.local.TrashItemEntity
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.data.media.MediaAccessState
import com.futureape.kanleme.data.media.MediaAuthorizationResult
import com.futureape.kanleme.data.media.MediaOperationKind
import com.futureape.kanleme.data.media.MediaOperationRequest
import com.futureape.kanleme.data.media.MediaOperationTracker
import com.futureape.kanleme.data.repository.AppRepository
import com.futureape.kanleme.data.repository.CleaningScope
import com.futureape.kanleme.data.repository.DashboardStats
import com.futureape.kanleme.data.repository.MediaLibrarySyncOptions
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.repository.PhotoTypeStats
import com.futureape.kanleme.data.repository.AnnualReport
import com.futureape.kanleme.data.repository.AchievementUi
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.data.settings.AppSettingsRepository
import com.futureape.kanleme.data.settings.DeleteMode
import com.futureape.kanleme.data.settings.FolderDisplayMode
import com.futureape.kanleme.data.settings.GestureDirection
import com.futureape.kanleme.data.settings.HapticLevel
import com.futureape.kanleme.data.settings.PhotoCleanMode
import com.futureape.kanleme.data.settings.SwipeSensitivity
import com.futureape.kanleme.data.settings.SwipeSoundStyle
import com.futureape.kanleme.data.settings.ThemeMode
import com.futureape.kanleme.data.settings.TodayInHistoryEntryMode
import com.futureape.kanleme.data.settings.VideoDisplayMode
import com.futureape.kanleme.data.settings.nextAccentColor
import com.futureape.kanleme.data.settings.nextBatchSize
import com.futureape.kanleme.data.worker.SimilarAnalysisScheduler
import com.futureape.kanleme.data.worker.SimilarAnalysisWorkMode
import com.futureape.kanleme.R
import com.futureape.kanleme.performance.StartupTrace
import com.futureape.kanleme.ui.i18n.UiText
import com.futureape.kanleme.ui.i18n.dynamicUiText
import com.futureape.kanleme.ui.i18n.uiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun MediaAccessState?.toSyncOptions(): MediaLibrarySyncOptions =
    MediaLibrarySyncOptions(
        markMissingPhotos = this?.canReadImages ?: true,
        markMissingVideos = this?.canReadVideos ?: true,
        permissionScopeFingerprint = this?.key.orEmpty(),
    )

data class SimilarDetectionUiState(
    val running: Boolean = false,
    val progress: Float = 0f,
    val stage: UiText = uiText(R.string.similar_stage_not_started),
    val processedHint: Int = 0,
    val totalHint: Int = 0,
    val lastResultCount: Int = 0,
)

private fun SimilarAnalysisTaskEntity.toSimilarDetectionUiState(): SimilarDetectionUiState {
    val safeTotal = totalPhotos.coerceAtLeast(0)
    val progress = when {
        status == SimilarAnalysisTaskStatus.COMPLETED -> 1f
        safeTotal == 0 -> 0f
        else -> (cursor.toFloat() / safeTotal.toFloat()).coerceIn(0f, 0.99f)
    }
    val stageText = when {
        status == SimilarAnalysisTaskStatus.IDLE -> uiText(R.string.similar_stage_not_started)
        status == SimilarAnalysisTaskStatus.FAILED && !lastError.isNullOrBlank() -> dynamicUiText(lastError)
        stage.isBlank() -> uiText(R.string.similar_stage_not_started)
        else -> dynamicUiText(stage)
    }
    return SimilarDetectionUiState(
        running = SimilarAnalysisTaskStatus.isRunning(status),
        progress = progress,
        stage = stageText,
        processedHint = cursor.coerceAtLeast(0),
        totalHint = safeTotal,
        lastResultCount = lastResultCount,
    )
}

data class PhotoUndoAnimation(
    val mediaStoreId: Long,
    val action: SwipeAction,
    val fromX: Float,
    val fromY: Float,
    val sequence: Long = System.nanoTime(),
)

private data class PhotoSessionAction(
    val photo: PhotoEntity,
    val action: SwipeAction,
    val operationId: Deferred<Long>,
    val exitTargetX: Float,
    val exitTargetY: Float,
    val undoPersistedAction: suspend (Long) -> Unit,
    val affectsDeck: Boolean = true,
)

sealed interface PendingMediaAuthorizationTarget {
    data class RestoreTrash(
        val ids: List<Long>,
        val items: List<TrashItemEntity>,
        val remainingItems: List<TrashItemEntity> = emptyList(),
    ) : PendingMediaAuthorizationTarget

    data class PhotoSystemTrash(
        val photo: PhotoEntity,
        val exitTargetX: Float,
        val exitTargetY: Float,
        val affectsDeck: Boolean = true,
    ) : PendingMediaAuthorizationTarget

    data class VideoSystemTrash(
        val video: VideoEntity,
    ) : PendingMediaAuthorizationTarget

    data class UndoOperation(
        val operationId: Long,
    ) : PendingMediaAuthorizationTarget

    data class UndoLastAction(
        val mediaType: String?,
    ) : PendingMediaAuthorizationTarget

    data class PermanentDelete(
        val ids: List<Long>,
    ) : PendingMediaAuthorizationTarget

    data class PhotoMove(
        val photo: PhotoEntity,
        val targetRelativePath: String,
    ) : PendingMediaAuthorizationTarget

    data class VideoMove(
        val video: VideoEntity,
        val targetRelativePath: String,
    ) : PendingMediaAuthorizationTarget

    data class PhotoArchive(
        val photo: PhotoEntity,
        val targetRelativePath: String,
        val originalRelativePath: String,
    ) : PendingMediaAuthorizationTarget

    data class VideoArchive(
        val video: VideoEntity,
        val targetRelativePath: String,
        val originalRelativePath: String,
    ) : PendingMediaAuthorizationTarget

    data class PhotoActionWithMove(
        val photo: PhotoEntity,
        val action: SwipeAction,
        val targetRelativePath: String,
        val originalRelativePath: String,
        val exitTargetX: Float,
        val exitTargetY: Float,
    ) : PendingMediaAuthorizationTarget
}

data class PendingMediaAuthorization(
    val request: MediaOperationRequest,
    val target: PendingMediaAuthorizationTarget,
)

@HiltViewModel
class KanlemeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val settingsRepository: AppSettingsRepository,
    private val similarAnalysisScheduler: SimilarAnalysisScheduler,
) : ViewModel() {
    private companion object {
        const val PHOTO_DECK_LOAD_WINDOW = 420
        const val VIDEO_DECK_LOAD_WINDOW = 240
        const val PHOTO_DECK_REFILL_THRESHOLD = 120
        const val VIDEO_DECK_REFILL_THRESHOLD = 80
        const val HOME_PREVIEW_DECK_SIZE = 3
        const val MAX_MEDIA_AUTHORIZATION_BATCH = 100
    }
    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded = _settingsLoaded.asStateFlow()
    private val _organizerScopesReady = MutableStateFlow(false)
    val organizerScopesReady = _organizerScopesReady.asStateFlow()
    private val _startupMediaBootstrapPending = MutableStateFlow(false)
    val startupMediaBootstrapPending = _startupMediaBootstrapPending.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .onEach { _settingsLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val dashboard: StateFlow<DashboardStats> = repository.observeDashboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardStats())

    val photoTypeStats: StateFlow<PhotoTypeStats> = repository.observePhotoTypeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotoTypeStats())

    val recentPhotos = repository.observeRecentPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val timelinePhotos = repository.observeTimelinePhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val timelineVideos = repository.observeTimelineVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val cleanedPhotoHistory = repository.observeCleanedPhotoHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayInHistoryPhotos = repository.observeTodayInHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val todayInHistoryVideos = repository.observeTodayInHistoryVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentlyAddedPhotos = repository.observeRecentlyAddedPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val photoFolders = repository.observePhotoFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val videoFolders = repository.observeVideoFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentVideos = repository.observeRecentVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoritePhotos = repository.observeFavoritePhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val favoriteVideos = repository.observeFavoriteVideos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val trashItems = repository.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val similarGroups = repository.observeSimilarGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val similarDetectionState: StateFlow<SimilarDetectionUiState> = repository.observeSimilarAnalysisTask()
        .map { it?.toSimilarDetectionUiState() ?: SimilarDetectionUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SimilarDetectionUiState())
    val similarDetectionRunning = similarDetectionState.map { it.running }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _photoScope = MutableStateFlow(CleaningScope())
    val photoScope = _photoScope.asStateFlow()

    private val _videoScope = MutableStateFlow(CleaningScope())
    val videoScope = _videoScope.asStateFlow()

    private val _annualReport = MutableStateFlow<AnnualReport?>(null)
    val annualReport = _annualReport.asStateFlow()

    private val _achievements = MutableStateFlow<List<AchievementUi>>(emptyList())
    val achievements = _achievements.asStateFlow()

    private val _photoDeck = MutableStateFlow<List<PhotoEntity>>(emptyList())
    val photoDeck = _photoDeck.asStateFlow()

    private val _videoDeck = MutableStateFlow<List<VideoEntity>>(emptyList())
    val videoDeck = _videoDeck.asStateFlow()

    private val _photoDeckPreview = MutableStateFlow<List<PhotoEntity>>(emptyList())
    val photoDeckPreview = _photoDeckPreview.asStateFlow()

    private val _photoDayMemoryWindow = MutableStateFlow<List<PhotoEntity>>(emptyList())
    val photoDayMemoryWindow = _photoDayMemoryWindow.asStateFlow()

    private val _videoDeckPreview = MutableStateFlow<List<VideoEntity>>(emptyList())
    val videoDeckPreview = _videoDeckPreview.asStateFlow()

    private val _photoDeckPreparing = MutableStateFlow(false)
    val photoDeckPreparing = _photoDeckPreparing.asStateFlow()

    private val _photoDeckPreviewPreparing = MutableStateFlow(false)
    val photoDeckPreviewPreparing = _photoDeckPreviewPreparing.asStateFlow()

    private val _photoDeckPreviewReady = MutableStateFlow(false)
    val photoDeckPreviewReady = _photoDeckPreviewReady.asStateFlow()

    private val _photoUndoAnimation = MutableStateFlow<PhotoUndoAnimation?>(null)
    val photoUndoAnimation = _photoUndoAnimation.asStateFlow()

    private val _videoDeckPreparing = MutableStateFlow(false)
    val videoDeckPreparing = _videoDeckPreparing.asStateFlow()

    private val _videoDeckPreviewPreparing = MutableStateFlow(false)
    val videoDeckPreviewPreparing = _videoDeckPreviewPreparing.asStateFlow()

    private val _photoSessionActionCount = MutableStateFlow(0)
    val photoSessionActionCount = _photoSessionActionCount.asStateFlow()

    private val _lastPhotoAction = MutableStateFlow<SwipeAction?>(null)
    val lastPhotoAction = _lastPhotoAction.asStateFlow()

    private val _videoSessionActionCount = MutableStateFlow(0)
    val videoSessionActionCount = _videoSessionActionCount.asStateFlow()

    private val _lastVideoAction = MutableStateFlow<SwipeAction?>(null)
    val lastVideoAction = _lastVideoAction.asStateFlow()

    private val _pendingVideoKeeps = MutableStateFlow<Map<Long, VideoEntity>>(emptyMap())
    val pendingVideoKeeps = _pendingVideoKeeps.asStateFlow()

    private val _message = MutableStateFlow<UiText?>(null)
    val message = _message.asStateFlow()
    private val _pendingMediaAuthorization = MutableStateFlow<PendingMediaAuthorization?>(null)
    val pendingMediaAuthorization = _pendingMediaAuthorization.asStateFlow()
    private val mediaOperationTracker = MediaOperationTracker()
    private val _mediaLibraryRefreshing = MutableStateFlow(false)
    val mediaLibraryRefreshing = _mediaLibraryRefreshing.asStateFlow()

    private var lastAutoRefreshAccessKey: String? = null
    private var currentMediaAccessState: MediaAccessState? = null
    private var photoDeckRefilling = false
    private var videoDeckRefilling = false
    private var photoDeckLoadJob: Job? = null
    private var videoDeckLoadJob: Job? = null
    private var photoDeckPreviewLoadJob: Job? = null
    private var videoDeckPreviewLoadJob: Job? = null
    private val _photoDeckScope = MutableStateFlow<CleaningScope?>(null)
    private var photoDeckScope: CleaningScope?
        get() = _photoDeckScope.value
        set(value) { _photoDeckScope.value = value }
    private var photoDeckPreviewScope: CleaningScope? = null
    private var videoDeckPreviewScope: CleaningScope? = null
    private var photoDeckPreviewScopeLoaded = false
    private var videoDeckPreviewScopeLoaded = false
    private var photoCleaningSessionActive = false
    private var photoDayMemoryWindowJob: Job? = null
    private var photoDeckGeneration = 0L
    private var videoDeckGeneration = 0L
    private var photoDeckPreviewGeneration = 0L
    private var videoDeckPreviewGeneration = 0L
    private var photoDayMemoryWindowGeneration = 0L
    private var lastGeneratedRandomSeed = 1L
    private val pendingPhotoActionMediaIds = mutableSetOf<Long>()
    private val handledPhotoActionMediaIds = mutableSetOf<Long>()
    private val photoSessionActions = ArrayDeque<PhotoSessionAction>()
    private val photoPersistenceBarriers = mutableMapOf<Long, Job>()
    private val handledVideoActionMediaIds = mutableSetOf<Long>()
    private val videoActionInFlightMediaIds = mutableSetOf<Long>()
    private val pendingVideoKeepOperations = mutableMapOf<Long, Deferred<Long>>()

    val photoDeckMatchesCurrentScope = combine(_photoScope, _photoDeckScope) { scope, deckScope ->
        samePhotoDeckScope(deckScope, scope)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            StartupTrace.asyncSection("kanleme.startup.viewmodelBootstrap") {
                val persisted = settingsRepository.loadStartupSettings()
                val photoScope = startupSeededScope(persisted.toPhotoCleaningScope())
                val videoScope = startupSeededScope(persisted.toVideoCleaningScope())
                _photoScope.value = photoScope
                _videoScope.value = videoScope
                _organizerScopesReady.value = true
                loadPhotoDeckPreview(photoScope)
                loadPhotoDeck(photoScope)
            }
        }
    }

    fun onMediaAccessReady(access: MediaAccessState, accessLabel: String) {
        if (lastAutoRefreshAccessKey == access.key && !access.isPartialVisualAccess) return
        lastAutoRefreshAccessKey = access.key
        currentMediaAccessState = access
        val needsBootstrap = _photoDeck.value.isEmpty() &&
            _videoDeck.value.isEmpty() &&
            dashboard.value.photoCount == 0 &&
            dashboard.value.videoCount == 0
        if (needsBootstrap) _startupMediaBootstrapPending.value = true
        // 首次授权后的自动同步不再弹出悬浮提示，避免遮挡首页或整理页按钮。
        refreshLibrary(
            accessLabel = accessLabel,
            showMessage = false,
            startupBootstrap = needsBootstrap,
            syncOptions = access.toSyncOptions(),
        )
    }

    private fun resumeSimilarAnalysisIfEnabled(access: MediaAccessState) {
        viewModelScope.launch {
            if (!settingsRepository.settings.first().similarDetection) return@launch
            val task = runCatching {
                repository.prepareSimilarAnalysis(
                    accessKey = access.key,
                    limit = SimilarAnalysisContract.DEFAULT_LIMIT,
                    forceRestart = false,
                )
            }.getOrNull() ?: return@launch
            if (task.status != SimilarAnalysisTaskStatus.COMPLETED && task.status != SimilarAnalysisTaskStatus.CANCELLED) {
                similarAnalysisScheduler.enqueue(SimilarAnalysisWorkMode.AUTO)
            }
        }
    }

    fun refreshLibrary(
        accessLabel: String? = null,
        showMessage: Boolean = true,
        startupBootstrap: Boolean = false,
        syncOptions: MediaLibrarySyncOptions? = null,
    ) = viewModelScope.launch {
        val prefix = accessLabel?.let { it + "：" }.orEmpty()
        val showPhotoPreparing = _photoDeck.value.isEmpty()
        if (showPhotoPreparing) _photoDeckPreparing.value = true
        _mediaLibraryRefreshing.value = true
        try {
            runCatching { repository.refreshMediaLibrary(syncOptions ?: currentMediaAccessState.toSyncOptions()) }
                .onSuccess { (p, v) ->
                    if (showMessage) {
                        _message.value = uiText(R.string.message_library_synced, p, v)
                    }
                    if (_photoDeck.value.isEmpty()) {
                        invalidatePhotoDeckLoads()
                        _photoDeckPreview.value = emptyList()
                        requestPhotoDeckPreview(
                            scope = _photoScope.value,
                            allowDuringSession = true,
                            promoteToDeckWhenReady = photoCleaningSessionActive,
                        )
                        loadPhotoDeck(_photoScope.value)
                    } else if (showPhotoPreparing) {
                        _photoDeckPreparing.value = false
                    }
                    if (_videoDeck.value.isEmpty()) {
                        videoDeckPreviewScope = null
                        videoDeckPreviewScopeLoaded = false
                        loadVideoDeckPreview(_videoScope.value)
                    }
                    currentMediaAccessState?.let(::resumeSimilarAnalysisIfEnabled)
                }
                .onFailure {
                    if (showPhotoPreparing) _photoDeckPreparing.value = false
                    _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_library_sync_failed)
                }
        } finally {
            _mediaLibraryRefreshing.value = false
            if (startupBootstrap) _startupMediaBootstrapPending.value = false
        }
    }

    fun clearMessage() { _message.value = null }

    fun showMessage(message: String) {
        _message.value = dynamicUiText(message)
    }

    private suspend fun requestMediaAuthorization(
        kind: MediaOperationKind,
        mediaUris: Collection<String>,
        isTrashed: Boolean,
        target: PendingMediaAuthorizationTarget,
    ) {
        if (_pendingMediaAuthorization.value != null) {
            _message.value = uiText(R.string.message_media_authorization_in_progress)
            return
        }
        val result = runCatching {
            repository.createMediaAuthorizationRequest(kind, mediaUris, isTrashed)
        }.getOrElse { throwable ->
            MediaAuthorizationResult.Failed(throwable.message ?: "系统媒体授权请求失败")
        }
        when (result) {
            MediaAuthorizationResult.Completed -> completeMediaAuthorization(target)
            is MediaAuthorizationResult.NeedsUserConsent -> {
                if (mediaOperationTracker.register(result.request.operationId)) {
                    _pendingMediaAuthorization.value = PendingMediaAuthorization(result.request, target)
                } else {
                    _message.value = uiText(R.string.message_media_authorization_failed)
                }
            }
            is MediaAuthorizationResult.Cancelled -> {
                _message.value = uiText(R.string.message_media_authorization_cancelled)
            }
            is MediaAuthorizationResult.PartialFailure -> {
                _message.value = uiText(R.string.message_media_authorization_partial)
            }
            is MediaAuthorizationResult.Failed -> {
                _message.value = dynamicUiText(result.reason)
            }
        }
    }

    fun onMediaAuthorizationResult(approved: Boolean) {
        val pending = _pendingMediaAuthorization.value ?: return
        if (!mediaOperationTracker.consume(pending.request.operationId)) return
        _pendingMediaAuthorization.value = null
        if (!approved) {
            resetAfterMediaAuthorizationCancelled(pending.target)
            _message.value = uiText(R.string.message_media_authorization_cancelled)
            return
        }
        viewModelScope.launch {
            runCatching { completeMediaAuthorization(pending.target) }
                .onFailure { throwable ->
                    resetAfterMediaAuthorizationCancelled(pending.target)
                    _message.value = throwable.message?.let(::dynamicUiText)
                        ?: uiText(R.string.message_media_authorization_failed)
                }
        }
    }

    fun onMediaAuthorizationLaunchFailed() {
        val pending = _pendingMediaAuthorization.value ?: return
        if (!mediaOperationTracker.consume(pending.request.operationId)) return
        _pendingMediaAuthorization.value = null
        resetAfterMediaAuthorizationCancelled(pending.target)
        _message.value = uiText(R.string.message_media_authorization_failed)
    }

    private suspend fun completeMediaAuthorization(target: PendingMediaAuthorizationTarget) {
        when (target) {
            is PendingMediaAuthorizationTarget.RestoreTrash -> {
                repository.restoreTrashItemsAfterSystemAuthorization(target.ids)
                restoreTrashItemsToDeck(target.items)
                if (target.remainingItems.isNotEmpty()) {
                    restoreTrashItems(target.remainingItems)
                } else {
                    _message.value = if (target.ids.size > 1) {
                        uiText(R.string.message_trash_all_restored)
                    } else {
                        uiText(R.string.message_restore_success)
                    }
                }
            }
            is PendingMediaAuthorizationTarget.PhotoSystemTrash -> {
                commitPhotoSystemTrash(target)
            }
            is PendingMediaAuthorizationTarget.VideoSystemTrash -> {
                onVideoAction(target.video, SwipeAction.Delete, systemTrashAuthorized = true)
            }
            is PendingMediaAuthorizationTarget.UndoOperation -> {
                if (!repository.undoOperationAfterSystemAuthorization(target.operationId)) {
                    throw IllegalStateException("撤销操作已失效")
                }
            }
            is PendingMediaAuthorizationTarget.UndoLastAction -> {
                val ok = repository.undoLastActionAfterSystemAuthorization(target.mediaType)
                applyUndoLastActionResult(target.mediaType, ok)
            }
            is PendingMediaAuthorizationTarget.PermanentDelete -> {
                val finalization = repository.finalizePermanentDelete(target.ids)
                when (finalization.result) {
                    MediaAuthorizationResult.Completed -> {
                        _message.value = if (finalization.deletedTrashIds.size > 1) {
                            uiText(R.string.message_trash_all_processed)
                        } else {
                            uiText(R.string.message_permanent_delete_success)
                        }
                    }
                    is MediaAuthorizationResult.PartialFailure -> {
                        _message.value = uiText(R.string.message_media_authorization_partial)
                    }
                    is MediaAuthorizationResult.Failed -> {
                        _message.value = dynamicUiText(finalization.result.reason)
                    }
                    is MediaAuthorizationResult.NeedsUserConsent,
                    is MediaAuthorizationResult.Cancelled,
                    -> _message.value = uiText(R.string.message_media_authorization_failed)
                }
            }
            is PendingMediaAuthorizationTarget.PhotoMove -> {
                val result = repository.movePhotoToFolder(target.photo, target.targetRelativePath, writeAuthorized = true)
                if (!result.success) error(result.message)
                _message.value = dynamicUiText(result.message)
            }
            is PendingMediaAuthorizationTarget.VideoMove -> {
                val result = repository.moveVideoToFolder(target.video, target.targetRelativePath, writeAuthorized = true)
                if (!result.success) error(result.message)
                _message.value = dynamicUiText(result.message)
            }
            is PendingMediaAuthorizationTarget.PhotoArchive -> commitPhotoArchive(target)
            is PendingMediaAuthorizationTarget.VideoArchive -> commitVideoArchive(target)
            is PendingMediaAuthorizationTarget.PhotoActionWithMove -> commitPhotoActionWithMove(target)
        }
    }

    private fun resetAfterMediaAuthorizationCancelled(target: PendingMediaAuthorizationTarget) {
        when (target) {
            is PendingMediaAuthorizationTarget.RestoreTrash -> Unit
            is PendingMediaAuthorizationTarget.PhotoSystemTrash -> {
                _photoUndoAnimation.value = PhotoUndoAnimation(
                    mediaStoreId = target.photo.mediaStoreId,
                    action = SwipeAction.Delete,
                    fromX = target.exitTargetX,
                    fromY = target.exitTargetY,
                )
            }
            is PendingMediaAuthorizationTarget.VideoSystemTrash -> {
                invalidateVideoDeckLoads()
                loadVideoDeck()
            }
            is PendingMediaAuthorizationTarget.UndoOperation,
            is PendingMediaAuthorizationTarget.UndoLastAction,
            is PendingMediaAuthorizationTarget.PermanentDelete,
            is PendingMediaAuthorizationTarget.PhotoMove,
            is PendingMediaAuthorizationTarget.VideoMove,
            is PendingMediaAuthorizationTarget.PhotoArchive,
            is PendingMediaAuthorizationTarget.VideoArchive,
            is PendingMediaAuthorizationTarget.PhotoActionWithMove,
            -> Unit
        }
    }

    private fun commitPhotoSystemTrash(target: PendingMediaAuthorizationTarget.PhotoSystemTrash) {
        if (target.affectsDeck) {
            if (!applyOptimisticPhotoAction(target.photo, SwipeAction.Delete)) return
            val operationId = persistPhotoAction(target.photo) {
                repository.handlePhotoAction(target.photo, SwipeAction.Delete, systemTrashAuthorized = true)
            }
            rememberPhotoSessionAction(
                photo = target.photo,
                action = SwipeAction.Delete,
                operationId = operationId,
                exitTargetX = target.exitTargetX,
                exitTargetY = target.exitTargetY,
            )
            return
        }

        val operationId = viewModelScope.async {
            repository.handlePhotoAction(target.photo, SwipeAction.Delete, systemTrashAuthorized = true)
        }
        rememberPhotoSessionAction(
            photo = target.photo,
            action = SwipeAction.Delete,
            operationId = operationId,
            exitTargetX = target.exitTargetX,
            exitTargetY = target.exitTargetY,
            affectsDeck = false,
        )
        viewModelScope.launch {
            runCatching { operationId.await() }
                .onSuccess { _message.value = uiText(R.string.message_system_trash_success) }
                .onFailure { throwable ->
                    dropLastPhotoSessionAction(target.photo.mediaStoreId)
                    _message.value = throwable.message?.let(::dynamicUiText)
                        ?: uiText(R.string.message_media_authorization_failed)
                }
        }
    }

    private fun commitPhotoActionWithMove(target: PendingMediaAuthorizationTarget.PhotoActionWithMove) {
        if (!applyOptimisticPhotoAction(target.photo, target.action)) return
        val operationId = persistPhotoAction(target.photo) {
            val result = repository.movePhotoToFolder(
                target.photo,
                target.targetRelativePath,
                writeAuthorized = true,
            )
            if (!result.success) error(result.message)
            try {
                repository.handlePhotoAction(target.photo, target.action)
            } catch (throwable: Throwable) {
                runCatching {
                    repository.movePhotoToFolder(
                        target.photo,
                        target.originalRelativePath,
                        writeAuthorized = true,
                    )
                }
                throw throwable
            }
        }
        rememberPhotoSessionAction(
            photo = target.photo,
            action = target.action,
            operationId = operationId,
            exitTargetX = target.exitTargetX,
            exitTargetY = target.exitTargetY,
            undoPersistedAction = { id ->
                repository.undoOperation(id)
                requestMediaAuthorization(
                    kind = MediaOperationKind.WRITE,
                    mediaUris = listOf(target.photo.uri),
                    isTrashed = false,
                    target = PendingMediaAuthorizationTarget.PhotoMove(
                        photo = target.photo,
                        targetRelativePath = target.originalRelativePath,
                    ),
                )
            },
        )
    }

    private suspend fun commitPhotoArchive(target: PendingMediaAuthorizationTarget.PhotoArchive) {
        handledPhotoActionMediaIds += target.photo.mediaStoreId
        invalidatePhotoDeckLoads()
        val result = repository.movePhotoToFolder(
            target.photo,
            target.targetRelativePath,
            writeAuthorized = true,
        )
        if (!result.success) {
            handledPhotoActionMediaIds -= target.photo.mediaStoreId
            error(result.message)
        }
        val operationId = try {
            repository.handlePhotoAction(target.photo, SwipeAction.Keep)
        } catch (throwable: Throwable) {
            runCatching {
                repository.movePhotoToFolder(
                    target.photo,
                    target.originalRelativePath,
                    writeAuthorized = true,
                )
            }
            handledPhotoActionMediaIds -= target.photo.mediaStoreId
            throw throwable
        }
        _photoDeck.value = _photoDeck.value.filterNot { it.mediaStoreId == target.photo.mediaStoreId }
        _photoSessionActionCount.value += 1
        _lastPhotoAction.value = SwipeAction.Keep
        rememberPhotoSessionAction(
            photo = target.photo,
            action = SwipeAction.Keep,
            operationId = CompletableDeferred(operationId),
            exitTargetX = 980f,
            exitTargetY = 0f,
            undoPersistedAction = { id ->
                repository.undoOperation(id)
                requestMediaAuthorization(
                    kind = MediaOperationKind.WRITE,
                    mediaUris = listOf(target.photo.uri),
                    isTrashed = false,
                    target = PendingMediaAuthorizationTarget.PhotoMove(
                        photo = target.photo,
                        targetRelativePath = target.originalRelativePath,
                    ),
                )
            },
        )
        _message.value = dynamicUiText(result.message)
        refillPhotoDeckIfNeeded()
    }

    private suspend fun commitVideoArchive(target: PendingMediaAuthorizationTarget.VideoArchive) {
        handledVideoActionMediaIds += target.video.mediaStoreId
        invalidateVideoDeckLoads()
        val pendingKeepOperation = removePendingVideoKeep(target.video)
        val result = repository.moveVideoToFolder(
            target.video,
            target.targetRelativePath,
            writeAuthorized = true,
        )
        if (!result.success) {
            handledVideoActionMediaIds -= target.video.mediaStoreId
            error(result.message)
        }
        try {
            if (pendingKeepOperation != null) {
                pendingKeepOperation.await()
            } else {
                repository.handleVideoAction(target.video, SwipeAction.Keep)
            }
        } catch (throwable: Throwable) {
            runCatching {
                repository.moveVideoToFolder(
                    target.video,
                    target.originalRelativePath,
                    writeAuthorized = true,
                )
            }
            handledVideoActionMediaIds -= target.video.mediaStoreId
            throw throwable
        }
        _videoDeck.value = _videoDeck.value.filterNot { it.mediaStoreId == target.video.mediaStoreId }
        if (pendingKeepOperation == null) _videoSessionActionCount.value += 1
        _lastVideoAction.value = SwipeAction.Keep
        _message.value = dynamicUiText(result.message)
        refillVideoDeckIfNeeded()
    }

    private fun requestSystemTrashForPhoto(
        photo: PhotoEntity,
        exitTargetX: Float,
        exitTargetY: Float,
        affectsDeck: Boolean = true,
    ) {
        viewModelScope.launch {
            requestMediaAuthorization(
                kind = MediaOperationKind.TRASH,
                mediaUris = listOf(photo.uri),
                isTrashed = true,
                target = PendingMediaAuthorizationTarget.PhotoSystemTrash(
                    photo = photo,
                    exitTargetX = exitTargetX,
                    exitTargetY = exitTargetY,
                    affectsDeck = affectsDeck,
                ),
            )
        }
    }

    private suspend fun requestSystemTrashForVideo(video: VideoEntity) {
        requestMediaAuthorization(
            kind = MediaOperationKind.TRASH,
            mediaUris = listOf(video.uri),
            isTrashed = true,
            target = PendingMediaAuthorizationTarget.VideoSystemTrash(video),
        )
    }

    private fun requestMediaAuthorizationForPhotoActionWithMove(
        photo: PhotoEntity,
        action: SwipeAction,
        targetRelativePath: String,
        exitTargetX: Float,
        exitTargetY: Float,
    ) {
        viewModelScope.launch {
            requestMediaAuthorization(
                kind = MediaOperationKind.WRITE,
                mediaUris = listOf(photo.uri),
                isTrashed = false,
            target = PendingMediaAuthorizationTarget.PhotoActionWithMove(
                    photo = photo,
                    action = action,
                    targetRelativePath = targetRelativePath,
                    originalRelativePath = photo.relativePath ?: photo.folderPath,
                    exitTargetX = exitTargetX,
                    exitTargetY = exitTargetY,
                ),
            )
        }
    }

    private fun nextRandomSeed(): Long {
        val mixed = (System.currentTimeMillis() * 1_000_003L) xor System.nanoTime() xor ((lastGeneratedRandomSeed + 0x6A09E667F3BCC909L) * 31L)
        val positive = (mixed and Long.MAX_VALUE).coerceAtLeast(2L)
        val next = if (positive == lastGeneratedRandomSeed) positive + 1L else positive
        lastGeneratedRandomSeed = next
        return next
    }

    private fun ensureRandomSeed(scope: CleaningScope): CleaningScope =
        if (scope.sortOrder == "random" && scope.randomSeed <= 1L) scope.copy(randomSeed = nextRandomSeed()) else scope

    private fun startupSeededScope(scope: CleaningScope): CleaningScope {
        val normalized = if (scope.sortOrder.isBlank()) scope.copy(sortOrder = "random") else scope
        return ensureRandomSeed(normalized)
    }

    // Home preview must come from the same generated deck as the actual cleaning session,
    // otherwise random candidate-pool differences will make the preview jump on entry.
    private fun homePhotoPreviewLoadScope(scope: CleaningScope): CleaningScope =
        scope.copy(deckLoadWindow = PHOTO_DECK_LOAD_WINDOW)

    private fun homeVideoPreviewLoadScope(scope: CleaningScope): CleaningScope =
        scope.copy(deckLoadWindow = VIDEO_DECK_LOAD_WINDOW)

    private fun CleaningScope.deckScopeKey(): CleaningScope = copy(sessionTarget = 0, deckLoadWindow = 0)

    private fun samePhotoDeckScope(left: CleaningScope?, right: CleaningScope): Boolean =
        left?.deckScopeKey() == right.deckScopeKey()

    private fun nextPhotoDeckGeneration(): Long {
        photoDeckLoadJob?.cancel()
        photoDeckGeneration += 1L
        return photoDeckGeneration
    }

    private fun nextVideoDeckGeneration(): Long {
        videoDeckLoadJob?.cancel()
        videoDeckGeneration += 1L
        return videoDeckGeneration
    }

    private fun invalidatePhotoDeckLoads() {
        photoDeckLoadJob?.cancel()
        photoDeckGeneration += 1L
        photoDeckPreviewLoadJob?.cancel()
        photoDeckPreviewGeneration += 1L
        photoDeckScope = null
        photoDeckPreviewScope = null
        photoDeckPreviewScopeLoaded = false
        _photoDeckPreviewReady.value = false
        _photoDeckPreviewPreparing.value = false
    }

    private fun invalidateVideoDeckLoads() {
        videoDeckLoadJob?.cancel()
        videoDeckGeneration += 1L
        videoDeckPreviewLoadJob?.cancel()
        videoDeckPreviewGeneration += 1L
        videoDeckPreviewScope = null
        videoDeckPreviewScopeLoaded = false
        _videoDeckPreviewPreparing.value = false
    }

    private fun List<PhotoEntity>.withoutLocalPhotoExclusions(): List<PhotoEntity> =
        filterNot { it.mediaStoreId in pendingPhotoActionMediaIds || it.mediaStoreId in handledPhotoActionMediaIds }

    private fun List<VideoEntity>.withoutLocalVideoExclusions(): List<VideoEntity> =
        filterNot { it.mediaStoreId in handledVideoActionMediaIds }

    private fun mergeVisiblePhotoDeckWithLoaded(loaded: List<PhotoEntity>): List<PhotoEntity> {
        val currentVisible = _photoDeck.value.withoutLocalPhotoExclusions()
        return (currentVisible + loaded)
            .distinctBy { it.mediaStoreId }
            .take(PHOTO_DECK_LOAD_WINDOW)
    }

    private fun rememberPhotoSessionAction(
        photo: PhotoEntity,
        action: SwipeAction,
        operationId: Deferred<Long>,
        exitTargetX: Float,
        exitTargetY: Float,
        undoPersistedAction: suspend (Long) -> Unit = { id -> repository.undoOperation(id) },
        affectsDeck: Boolean = true,
    ) {
        photoSessionActions.addLast(PhotoSessionAction(photo, action, operationId, exitTargetX, exitTargetY, undoPersistedAction, affectsDeck))
    }

    private fun clearPhotoActionHistory() {
        photoSessionActions.clear()
        _photoUndoAnimation.value = null
    }

    private fun popPhotoSessionAction(): PhotoSessionAction? =
        if (photoSessionActions.isEmpty()) null else photoSessionActions.removeLast()

    private fun dropLastPhotoSessionAction(mediaStoreId: Long) {
        val actions = photoSessionActions.toList()
        val dropIndex = actions.indexOfLast { it.photo.mediaStoreId == mediaStoreId }
        if (dropIndex < 0) return
        photoSessionActions.clear()
        actions.forEachIndexed { index, action ->
            if (index != dropIndex) photoSessionActions.addLast(action)
        }
    }

    private fun hasPhotoSessionAction(operationId: Deferred<Long>): Boolean =
        photoSessionActions.any { it.operationId === operationId }

    private fun applyOptimisticPhotoAction(photo: PhotoEntity, action: SwipeAction): Boolean {
        if (photo.mediaStoreId in pendingPhotoActionMediaIds || photo.mediaStoreId in handledPhotoActionMediaIds) return false
        pendingPhotoActionMediaIds += photo.mediaStoreId
        handledPhotoActionMediaIds += photo.mediaStoreId
        _photoDeck.value = _photoDeck.value.filterNot { it.mediaStoreId == photo.mediaStoreId }
        if (_photoDeck.value.isEmpty()) _photoDeckPreparing.value = true
        _photoSessionActionCount.value += 1
        _lastPhotoAction.value = action
        return true
    }

    private fun rollbackOptimisticPhotoAction(photo: PhotoEntity) {
        if (photo.mediaStoreId !in pendingPhotoActionMediaIds && photo.mediaStoreId !in handledPhotoActionMediaIds) return
        pendingPhotoActionMediaIds -= photo.mediaStoreId
        handledPhotoActionMediaIds -= photo.mediaStoreId
        dropLastPhotoSessionAction(photo.mediaStoreId)
        invalidatePhotoDeckLoads()
        if (_photoDeck.value.none { it.mediaStoreId == photo.mediaStoreId }) {
            _photoDeck.value = (listOf(photo) + _photoDeck.value)
                .distinctBy { it.mediaStoreId }
                .take(PHOTO_DECK_LOAD_WINDOW)
            photoDeckScope = _photoScope.value
        }
        _photoSessionActionCount.value = (_photoSessionActionCount.value - 1).coerceAtLeast(0)
    }

    private fun restorePhotoSessionAction(snapshot: PhotoSessionAction) {
        val photo = snapshot.photo
        if (!snapshot.affectsDeck) {
            _lastPhotoAction.value = snapshot.action
            _message.value = uiText(R.string.message_photo_back_success)
            return
        }
        pendingPhotoActionMediaIds -= photo.mediaStoreId
        handledPhotoActionMediaIds -= photo.mediaStoreId
        invalidatePhotoDeckLoads()
        _photoDeck.value = (listOf(photo) + _photoDeck.value)
            .distinctBy { it.mediaStoreId }
            .take(PHOTO_DECK_LOAD_WINDOW)
        photoDeckScope = _photoScope.value
        _photoSessionActionCount.value = (_photoSessionActionCount.value - 1).coerceAtLeast(0)
        _lastPhotoAction.value = snapshot.action
        _photoUndoAnimation.value = PhotoUndoAnimation(
            mediaStoreId = photo.mediaStoreId,
            action = snapshot.action,
            fromX = snapshot.exitTargetX,
            fromY = snapshot.exitTargetY,
        )
        _message.value = uiText(R.string.message_photo_back_success)
    }

    private fun persistPhotoSessionUndo(snapshot: PhotoSessionAction) {
        val mediaStoreId = snapshot.photo.mediaStoreId
        lateinit var undoJob: Job
        undoJob = viewModelScope.launch {
            try {
                runCatching {
                    val operationId = snapshot.operationId.await()
                    if (snapshot.action == SwipeAction.Delete && settings.value.deleteMode == DeleteMode.SYSTEM_TRASH) {
                        requestMediaAuthorization(
                            kind = MediaOperationKind.RESTORE,
                            mediaUris = listOf(snapshot.photo.uri),
                            isTrashed = false,
                            target = PendingMediaAuthorizationTarget.UndoOperation(operationId),
                        )
                    } else {
                        snapshot.undoPersistedAction(operationId)
                    }
                }
                    .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_undo_failed) }
            } finally {
                if (photoPersistenceBarriers[mediaStoreId] == undoJob) {
                    photoPersistenceBarriers -= mediaStoreId
                }
            }
        }
        photoPersistenceBarriers[mediaStoreId] = undoJob
    }

    private fun persistPhotoAction(photo: PhotoEntity, actionBlock: suspend () -> Long): Deferred<Long> {
        val priorBarrier = photoPersistenceBarriers[photo.mediaStoreId]
        return viewModelScope.async {
            try {
                priorBarrier?.join()
                val operationId = actionBlock()
                refillPhotoDeckIfNeeded()
                operationId
            } catch (t: Throwable) {
                rollbackOptimisticPhotoAction(photo)
                _message.value = t.message?.let(::dynamicUiText) ?: uiText(R.string.message_undo_failed)
                throw t
            } finally {
                pendingPhotoActionMediaIds -= photo.mediaStoreId
            }
        }
    }

    fun finishPhotoCleaningSession() {
        photoCleaningSessionActive = false
        pendingPhotoActionMediaIds.clear()
        clearPhotoActionHistory()
        clearPhotoDayMemoryWindow()
        _photoSessionActionCount.value = 0
        _lastPhotoAction.value = null
        _photoDeck.value = _photoDeck.value.withoutLocalPhotoExclusions()
    }

    fun clearPhotoUndoAnimation(sequence: Long) {
        if (_photoUndoAnimation.value?.sequence == sequence) {
            _photoUndoAnimation.value = null
        }
    }

    fun loadPhotoDeck(scope: CleaningScope = _photoScope.value, replaceWithEmpty: Boolean = false) {
        val requestedScope = ensureRandomSeed(scope)
        _photoScope.value = requestedScope
        if (replaceWithEmpty && !samePhotoDeckScope(photoDeckScope, requestedScope)) {
            _photoDeck.value = emptyList()
            _photoDeckPreview.value = emptyList()
            photoDeckScope = null
            photoDeckPreviewScope = null
            photoDeckPreviewScopeLoaded = false
            _photoDeckPreviewReady.value = false
        }
        val showPreparing = _photoDeck.value.isEmpty()
        if (showPreparing) _photoDeckPreparing.value = true
        val generation = nextPhotoDeckGeneration()
        photoDeckLoadJob = viewModelScope.launch {
            try {
                val loaded = StartupTrace.asyncSection("kanleme.photoDeck.query") {
                    repository.loadPhotoDeck(requestedScope.copy(deckLoadWindow = PHOTO_DECK_LOAD_WINDOW))
                }
                    .withoutLocalPhotoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == photoDeckGeneration) {
                    if (loaded.isNotEmpty() || _photoDeck.value.isEmpty() || replaceWithEmpty) {
                        _photoDeck.value = loaded
                        _photoDeckPreview.value = loaded.take(HOME_PREVIEW_DECK_SIZE)
                        photoDeckScope = requestedScope
                        photoDeckPreviewScope = requestedScope
                        photoDeckPreviewScopeLoaded = true
                        _photoDeckPreviewReady.value = true
                    }
                }
            } finally {
                if (showPreparing && generation == photoDeckGeneration) _photoDeckPreparing.value = false
            }
        }
    }

    fun loadPhotoDeckPreview(scope: CleaningScope = _photoScope.value) {
        requestPhotoDeckPreview(scope = scope)
    }

    fun reloadPhotoDeckPreview(scope: CleaningScope = _photoScope.value) {
        if (photoCleaningSessionActive) return
        photoDeckPreviewLoadJob?.cancel()
        photoDeckPreviewGeneration += 1L
        photoDeckPreviewScope = null
        photoDeckPreviewScopeLoaded = false
        _photoDeckPreviewReady.value = false
        _photoDeckPreviewPreparing.value = false
        _photoDeckPreview.value = emptyList()
        loadPhotoDeckPreview(scope)
    }

    private fun requestPhotoDeckPreview(
        scope: CleaningScope = _photoScope.value,
        allowDuringSession: Boolean = false,
        promoteToDeckWhenReady: Boolean = false,
    ) {
        if (photoCleaningSessionActive && !allowDuringSession) return
        val requestedScope = ensureRandomSeed(scope.copy(sortOrder = if (scope.sortOrder.isBlank()) "random" else scope.sortOrder))
        if (_photoDeck.value.isNotEmpty() && samePhotoDeckScope(photoDeckScope, requestedScope)) {
            _photoDeckPreview.value = _photoDeck.value.take(HOME_PREVIEW_DECK_SIZE)
            photoDeckPreviewScope = requestedScope
            photoDeckPreviewScopeLoaded = true
            _photoDeckPreviewReady.value = true
            return
        }
        if (!samePhotoDeckScope(photoDeckPreviewScope, requestedScope)) {
            _photoDeckPreview.value = emptyList()
            photoDeckPreviewScopeLoaded = false
            _photoDeckPreviewReady.value = false
        }
        if (photoDeckPreviewScope == requestedScope) {
            if (
                photoDeckPreviewScopeLoaded ||
                (_photoDeckPreviewPreparing.value && !promoteToDeckWhenReady)
            ) {
                if (
                    promoteToDeckWhenReady &&
                    _photoDeck.value.isEmpty() &&
                    _photoDeckPreview.value.isNotEmpty() &&
                    samePhotoDeckScope(_photoScope.value, requestedScope)
                ) {
                    _photoDeck.value = _photoDeckPreview.value.take(HOME_PREVIEW_DECK_SIZE)
                    photoDeckScope = requestedScope
                    _photoDeckPreparing.value = false
                }
                return
            }
        }
        _photoScope.value = requestedScope
        photoDeckPreviewScope = requestedScope
        photoDeckPreviewScopeLoaded = false
        _photoDeckPreviewReady.value = false
        _photoDeckPreviewPreparing.value = true
        photoDeckPreviewLoadJob?.cancel()
        val generation = ++photoDeckPreviewGeneration
        photoDeckPreviewLoadJob = viewModelScope.launch {
            try {
                val preview = StartupTrace.asyncSection("kanleme.photoPreview.query") {
                    repository.loadPhotoDeck(homePhotoPreviewLoadScope(requestedScope))
                }
                    .withoutLocalPhotoExclusions()
                    .distinctBy { it.mediaStoreId }
                    .take(HOME_PREVIEW_DECK_SIZE)
                if (generation == photoDeckPreviewGeneration) {
                    _photoDeckPreview.value = preview
                    photoDeckPreviewScopeLoaded = true
                    _photoDeckPreviewReady.value = true
                    if (
                        promoteToDeckWhenReady &&
                        preview.isNotEmpty() &&
                        _photoDeck.value.isEmpty() &&
                        samePhotoDeckScope(_photoScope.value, requestedScope)
                    ) {
                        _photoDeck.value = preview
                        photoDeckScope = requestedScope
                        _photoDeckPreparing.value = false
                    }
                }
            } finally {
                if (generation == photoDeckPreviewGeneration) _photoDeckPreviewPreparing.value = false
            }
        }
    }

    fun preparePhotoHomeQueue(scope: CleaningScope = _photoScope.value) {
        photoCleaningSessionActive = false
        ensurePhotoCleaningQueuePrepared(scope)
    }

    fun ensurePhotoCleaningQueuePrepared(scope: CleaningScope = _photoScope.value) {
        val requestedScope = ensureRandomSeed(scope.copy(sortOrder = if (scope.sortOrder.isBlank()) "random" else scope.sortOrder))
        if (photoDeckLoadJob?.isActive == true) return
        if (_photoDeckPreparing.value) _photoDeckPreparing.value = false
        if (_photoDeck.value.isNotEmpty() && samePhotoDeckScope(photoDeckScope, requestedScope)) return
        loadPhotoDeck(requestedScope, replaceWithEmpty = !samePhotoDeckScope(photoDeckScope, requestedScope))
    }

    fun loadPhotoDayMemoryWindow(currentPhoto: PhotoEntity) {
        photoDayMemoryWindowJob?.cancel()
        _photoDayMemoryWindow.value = listOf(currentPhoto)
        val generation = ++photoDayMemoryWindowGeneration
        photoDayMemoryWindowJob = viewModelScope.launch {
            val photos = repository
                .loadPhotoMemoryWindow(currentPhoto = currentPhoto)
                .distinctBy { it.mediaStoreId }
            if (generation == photoDayMemoryWindowGeneration) {
                _photoDayMemoryWindow.value = photos.ifEmpty { listOf(currentPhoto) }
            }
        }
    }

    fun clearPhotoDayMemoryWindow() {
        photoDayMemoryWindowJob?.cancel()
        photoDayMemoryWindowGeneration += 1L
        _photoDayMemoryWindow.value = emptyList()
    }

    fun loadVideoDeck(scope: CleaningScope = _videoScope.value) {
        val requestedScope = ensureRandomSeed(scope)
        _videoScope.value = requestedScope
        val showPreparing = _videoDeck.value.isEmpty()
        if (showPreparing) _videoDeckPreparing.value = true
        val generation = nextVideoDeckGeneration()
        videoDeckLoadJob = viewModelScope.launch {
            try {
                val loaded = StartupTrace.asyncSection("kanleme.videoDeck.query") {
                    repository.loadVideoDeck(requestedScope.copy(deckLoadWindow = VIDEO_DECK_LOAD_WINDOW))
                }
                    .withoutLocalVideoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == videoDeckGeneration) {
                    _videoDeck.value = loaded
                    _videoDeckPreview.value = loaded.take(HOME_PREVIEW_DECK_SIZE)
                }
            } finally {
                if (showPreparing && generation == videoDeckGeneration) _videoDeckPreparing.value = false
            }
        }
    }

    fun loadVideoDeckPreview(scope: CleaningScope = _videoScope.value) {
        if (_videoDeck.value.isNotEmpty()) {
            _videoDeckPreview.value = _videoDeck.value.take(HOME_PREVIEW_DECK_SIZE)
            return
        }
        val requestedScope = ensureRandomSeed(scope.copy(sortOrder = if (scope.sortOrder.isBlank()) "random" else scope.sortOrder))
        if (videoDeckPreviewScope == requestedScope && (_videoDeckPreviewPreparing.value || videoDeckPreviewScopeLoaded)) {
            return
        }
        _videoScope.value = requestedScope
        videoDeckPreviewScope = requestedScope
        videoDeckPreviewScopeLoaded = false
        _videoDeckPreviewPreparing.value = true
        videoDeckPreviewLoadJob?.cancel()
        val generation = ++videoDeckPreviewGeneration
        videoDeckPreviewLoadJob = viewModelScope.launch {
            try {
                val preview = StartupTrace.asyncSection("kanleme.videoPreview.query") {
                    repository.loadVideoDeck(homeVideoPreviewLoadScope(requestedScope))
                }
                    .withoutLocalVideoExclusions()
                    .distinctBy { it.mediaStoreId }
                    .take(HOME_PREVIEW_DECK_SIZE)
                if (generation == videoDeckPreviewGeneration && _videoDeck.value.isEmpty()) {
                    _videoDeckPreview.value = preview
                    videoDeckPreviewScopeLoaded = true
                }
            } finally {
                if (generation == videoDeckPreviewGeneration) _videoDeckPreviewPreparing.value = false
            }
        }
    }

    fun startPhotoCleaningSession() {
        photoCleaningSessionActive = true
        val base = _photoScope.value
        val session = ensureRandomSeed(base.copy(sortOrder = if (base.sortOrder.isBlank()) "random" else base.sortOrder))
        _photoScope.value = session
        _photoSessionActionCount.value = 0
        _lastPhotoAction.value = null
        pendingPhotoActionMediaIds.clear()
        clearPhotoActionHistory()
        _photoDeck.value = _photoDeck.value.withoutLocalPhotoExclusions()
        if (_photoDeck.value.isNotEmpty() && !samePhotoDeckScope(photoDeckScope, session)) {
            _photoDeck.value = emptyList()
            photoDeckScope = null
        }
        val previewForSession = _photoDeckPreview.value
            .withoutLocalPhotoExclusions()
            .distinctBy { it.mediaStoreId }
            .take(HOME_PREVIEW_DECK_SIZE)
        val promotedPreview = _photoDeck.value.isEmpty() &&
            previewForSession.isNotEmpty() &&
            samePhotoDeckScope(photoDeckPreviewScope, session) &&
            photoDeckPreviewScopeLoaded
        if (promotedPreview) {
            _photoDeck.value = previewForSession
            photoDeckScope = session
            _photoDeckPreparing.value = false
        }
        if (_photoDeck.value.isNotEmpty() && !promotedPreview) {
            if (session.sortOrder == "random") {
                viewModelScope.launch { settingsRepository.setPhotoSortOrderWithSeed("random", session.randomSeed) }
            }
            _photoDeckPreparing.value = false
            return
        }
        val showPreparing = _photoDeck.value.isEmpty()
        if (showPreparing) _photoDeckPreparing.value = true
        requestPhotoDeckPreview(
            scope = session,
            allowDuringSession = true,
            promoteToDeckWhenReady = true,
        )
        val generation = nextPhotoDeckGeneration()
        photoDeckLoadJob = viewModelScope.launch {
            if (session.sortOrder == "random") settingsRepository.setPhotoSortOrderWithSeed("random", session.randomSeed)
            try {
                val loaded = repository
                    .loadPhotoDeck(session.copy(deckLoadWindow = PHOTO_DECK_LOAD_WINDOW))
                    .withoutLocalPhotoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == photoDeckGeneration) {
                    val nextDeck = if (_photoDeck.value.isNotEmpty() && samePhotoDeckScope(photoDeckScope, session)) {
                        mergeVisiblePhotoDeckWithLoaded(loaded)
                    } else {
                        loaded
                    }
                    _photoDeck.value = nextDeck
                    _photoDeckPreview.value = nextDeck.take(HOME_PREVIEW_DECK_SIZE)
                    photoDeckScope = session
                    photoDeckPreviewScope = session
                    photoDeckPreviewScopeLoaded = true
                    _photoDeckPreviewReady.value = true
                }
            } finally {
                if (generation == photoDeckGeneration) _photoDeckPreparing.value = false
            }
        }
    }

    fun startVideoCleaningSession() {
        val base = _videoScope.value
        val session = ensureRandomSeed(base.copy(sortOrder = if (base.sortOrder.isBlank()) "random" else base.sortOrder))
        _videoScope.value = session
        _videoSessionActionCount.value = 0
        _lastVideoAction.value = null
        clearPendingVideoKeeps()
        _videoDeck.value = _videoDeck.value.withoutLocalVideoExclusions()
        if (_videoDeck.value.isNotEmpty()) {
            if (session.sortOrder == "random") {
                viewModelScope.launch { settingsRepository.setVideoSortOrderWithSeed("random", session.randomSeed) }
            }
            _videoDeckPreparing.value = false
            return
        }
        _videoDeckPreparing.value = true
        val generation = nextVideoDeckGeneration()
        videoDeckLoadJob = viewModelScope.launch {
            if (session.sortOrder == "random") settingsRepository.setVideoSortOrderWithSeed("random", session.randomSeed)
            try {
                val loaded = repository
                    .loadVideoDeck(session.copy(deckLoadWindow = VIDEO_DECK_LOAD_WINDOW))
                    .withoutLocalVideoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == videoDeckGeneration) {
                    _videoDeck.value = loaded
                    _videoDeckPreview.value = loaded.take(HOME_PREVIEW_DECK_SIZE)
                }
            } finally {
                if (generation == videoDeckGeneration) _videoDeckPreparing.value = false
            }
        }
    }


    fun reshufflePhotoCleaningSession() {
        val session = _photoScope.value.copy(sortOrder = "random", randomSeed = nextRandomSeed())
        _photoScope.value = session
        _photoDeckPreparing.value = true
        _photoDeck.value = emptyList()
        photoDeckScope = null
        val generation = nextPhotoDeckGeneration()
        photoDeckLoadJob = viewModelScope.launch {
            settingsRepository.setPhotoSortOrderWithSeed("random", session.randomSeed)
            try {
                val loaded = repository
                    .loadPhotoDeck(session.copy(deckLoadWindow = PHOTO_DECK_LOAD_WINDOW))
                    .withoutLocalPhotoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == photoDeckGeneration) {
                    _photoDeck.value = loaded
                    _photoDeckPreview.value = loaded.take(HOME_PREVIEW_DECK_SIZE)
                    photoDeckScope = session
                    photoDeckPreviewScope = session
                    photoDeckPreviewScopeLoaded = true
                    _photoDeckPreviewReady.value = true
                }
            } finally {
                if (generation == photoDeckGeneration) _photoDeckPreparing.value = false
            }
        }
    }

    fun reshuffleVideoCleaningSession() {
        val session = _videoScope.value.copy(sortOrder = "random", randomSeed = nextRandomSeed())
        _videoScope.value = session
        _videoDeckPreparing.value = true
        _videoSessionActionCount.value = 0
        _lastVideoAction.value = null
        clearPendingVideoKeeps()
        _videoDeck.value = emptyList()
        val generation = nextVideoDeckGeneration()
        videoDeckLoadJob = viewModelScope.launch {
            settingsRepository.setVideoSortOrderWithSeed("random", session.randomSeed)
            try {
                val loaded = repository
                    .loadVideoDeck(session.copy(deckLoadWindow = VIDEO_DECK_LOAD_WINDOW))
                    .withoutLocalVideoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == videoDeckGeneration) _videoDeck.value = loaded
            } finally {
                if (generation == videoDeckGeneration) _videoDeckPreparing.value = false
            }
        }
    }

    private suspend fun refillPhotoDeckIfNeeded(force: Boolean = false) {
        if (!force && _photoDeck.value.size > PHOTO_DECK_REFILL_THRESHOLD) return
        if (photoDeckRefilling) return
        val generation = photoDeckGeneration
        val showPreparing = _photoDeck.value.isEmpty()
        if (showPreparing) _photoDeckPreparing.value = true
        photoDeckRefilling = true
        try {
            val fresh = repository.loadPhotoDeck(_photoScope.value.copy(deckLoadWindow = PHOTO_DECK_LOAD_WINDOW))
            if (generation != photoDeckGeneration) return
            val merged = (_photoDeck.value + fresh)
                .withoutLocalPhotoExclusions()
                .distinctBy { it.mediaStoreId }
                .take(PHOTO_DECK_LOAD_WINDOW)
            if (generation == photoDeckGeneration) {
                _photoDeck.value = merged
                photoDeckScope = _photoScope.value
            }
        } finally {
            photoDeckRefilling = false
            if (showPreparing && generation == photoDeckGeneration) _photoDeckPreparing.value = false
        }
    }

    private suspend fun refillVideoDeckIfNeeded(force: Boolean = false) {
        if (!force && _videoDeck.value.size > VIDEO_DECK_REFILL_THRESHOLD) return
        if (videoDeckRefilling) return
        val generation = videoDeckGeneration
        videoDeckRefilling = true
        try {
            val fresh = repository.loadVideoDeck(_videoScope.value.copy(deckLoadWindow = VIDEO_DECK_LOAD_WINDOW))
            if (generation != videoDeckGeneration) return
            val merged = (_videoDeck.value + fresh)
                .withoutLocalVideoExclusions()
                .distinctBy { it.mediaStoreId }
                .take(VIDEO_DECK_LOAD_WINDOW)
            if (generation == videoDeckGeneration) _videoDeck.value = merged
        } finally {
            videoDeckRefilling = false
        }
    }

    private fun preparePhotoPreview(scope: CleaningScope) {
        invalidatePhotoDeckLoads()
        _photoDeck.value = emptyList()
        _photoDeckPreview.value = emptyList()
        _photoDeckPreparing.value = false
        _photoScope.value = scope
        loadPhotoDeckPreview(scope)
    }

    private fun prepareVideoPreview(scope: CleaningScope) {
        invalidateVideoDeckLoads()
        _videoDeck.value = emptyList()
        _videoDeckPreview.value = emptyList()
        _videoDeckPreparing.value = false
        _videoScope.value = scope
        loadVideoDeckPreview(scope)
    }

    fun setHomeMediaTab(tab: String) = viewModelScope.launch {
        settingsRepository.setHomeMediaTab(tab)
    }

    fun setPhotoTypeFilter(type: String) = viewModelScope.launch {
        settingsRepository.setPhotoMediaType(type)
        loadPhotoDeck(_photoScope.value.copy(mediaType = type), replaceWithEmpty = true)
    }

    fun setPhotoTypePreview(type: String) = viewModelScope.launch {
        settingsRepository.setPhotoMediaType(type)
        preparePhotoPreview(_photoScope.value.copy(mediaType = type))
    }

    fun setPhotoDateMode(mode: String) = viewModelScope.launch {
        settingsRepository.setPhotoDateMode(mode)
        loadPhotoDeck(_photoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"), replaceWithEmpty = true)
    }

    fun setPhotoDateModePreview(mode: String) = viewModelScope.launch {
        preparePhotoPreview(_photoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"))
    }

    fun setPhotoSessionDateMode(mode: String) {
        loadPhotoDeck(_photoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"), replaceWithEmpty = true)
    }

    fun resetPhotoSessionDateMode() {
        val nextScope = _photoScope.value.copy(dateMode = "all", todayInHistory = false)
        _photoScope.value = nextScope
        if (_photoDeck.value.isNotEmpty() && !samePhotoDeckScope(photoDeckScope, nextScope)) {
            invalidatePhotoDeckLoads()
            _photoDeck.value = emptyList()
            _photoDeckPreview.value = emptyList()
            _photoDeckPreviewReady.value = false
        }
    }

    fun setVideoDateMode(mode: String) = viewModelScope.launch {
        settingsRepository.setVideoDateMode(mode)
        loadVideoDeck(_videoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"))
    }

    fun setVideoDateModePreview(mode: String) = viewModelScope.launch {
        prepareVideoPreview(_videoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"))
    }

    fun setVideoSessionDateMode(mode: String) {
        loadVideoDeck(_videoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"))
    }

    fun resetVideoSessionDateMode() {
        _videoScope.value = _videoScope.value.copy(dateMode = "all", todayInHistory = false)
    }

    fun setPhotoFolder(path: String?) = viewModelScope.launch {
        settingsRepository.setPhotoFolderPath(path)
        loadPhotoDeck(_photoScope.value.copy(folderPaths = path?.let { setOf(it) } ?: emptySet()), replaceWithEmpty = true)
    }

    fun setPhotoFolderPreview(path: String?) = viewModelScope.launch {
        settingsRepository.setPhotoFolderPath(path)
        preparePhotoPreview(_photoScope.value.copy(folderPaths = path?.let { setOf(it) } ?: emptySet()))
    }

    fun togglePhotoRandom() = viewModelScope.launch {
        val next = if (_photoScope.value.sortOrder == "random") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _photoScope.value.randomSeed
        settingsRepository.setPhotoSortOrderWithSeed(next, seed)
        loadPhotoDeck(_photoScope.value.copy(sortOrder = next, randomSeed = seed), replaceWithEmpty = true)
    }

    fun togglePhotoRandomPreview() = viewModelScope.launch {
        val next = if (_photoScope.value.sortOrder == "random") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _photoScope.value.randomSeed
        settingsRepository.setPhotoSortOrderWithSeed(next, seed)
        preparePhotoPreview(_photoScope.value.copy(sortOrder = next, randomSeed = seed))
    }

    fun setPhotoSortOrder(order: String) = viewModelScope.launch {
        val next = if (order == "newest") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _photoScope.value.randomSeed
        settingsRepository.setPhotoSortOrderWithSeed(next, seed)
        loadPhotoDeck(_photoScope.value.copy(sortOrder = next, randomSeed = seed), replaceWithEmpty = true)
    }

    fun setPhotoSortOrderPreview(order: String) = viewModelScope.launch {
        val next = if (order == "newest") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _photoScope.value.randomSeed
        settingsRepository.setPhotoSortOrderWithSeed(next, seed)
        preparePhotoPreview(_photoScope.value.copy(sortOrder = next, randomSeed = seed))
    }

    fun cyclePhotoBatchSize() = viewModelScope.launch {
        val next = nextBatchSize(settings.value.photoBatchSize)
        settingsRepository.setPhotoBatchSize(next)
        _photoScope.value = _photoScope.value.copy(sessionTarget = next)
    }

    fun setPhotoBatchSize(size: Int) = viewModelScope.launch {
        val next = size.coerceIn(1, 100)
        settingsRepository.setPhotoBatchSize(next)
        _photoScope.value = _photoScope.value.copy(sessionTarget = next)
    }

    fun setVideoFolder(path: String?) = viewModelScope.launch {
        settingsRepository.setVideoFolderPath(path)
        loadVideoDeck(_videoScope.value.copy(folderPaths = path?.let { setOf(it) } ?: emptySet()))
    }

    fun setVideoFolderPreview(path: String?) = viewModelScope.launch {
        settingsRepository.setVideoFolderPath(path)
        prepareVideoPreview(_videoScope.value.copy(folderPaths = path?.let { setOf(it) } ?: emptySet()))
    }

    fun toggleVideoRandom() = viewModelScope.launch {
        val next = if (_videoScope.value.sortOrder == "random") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _videoScope.value.randomSeed
        settingsRepository.setVideoSortOrderWithSeed(next, seed)
        loadVideoDeck(_videoScope.value.copy(sortOrder = next, randomSeed = seed))
    }

    fun toggleVideoRandomPreview() = viewModelScope.launch {
        val next = if (_videoScope.value.sortOrder == "random") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _videoScope.value.randomSeed
        settingsRepository.setVideoSortOrderWithSeed(next, seed)
        prepareVideoPreview(_videoScope.value.copy(sortOrder = next, randomSeed = seed))
    }

    fun setVideoSortOrder(order: String) = viewModelScope.launch {
        val next = if (order == "newest") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _videoScope.value.randomSeed
        settingsRepository.setVideoSortOrderWithSeed(next, seed)
        loadVideoDeck(_videoScope.value.copy(sortOrder = next, randomSeed = seed))
    }

    fun setVideoSortOrderPreview(order: String) = viewModelScope.launch {
        val next = if (order == "newest") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _videoScope.value.randomSeed
        settingsRepository.setVideoSortOrderWithSeed(next, seed)
        prepareVideoPreview(_videoScope.value.copy(sortOrder = next, randomSeed = seed))
    }

    fun cycleVideoBatchSize() = viewModelScope.launch {
        val next = nextBatchSize(settings.value.videoBatchSize)
        settingsRepository.setVideoBatchSize(next)
        _videoScope.value = _videoScope.value.copy(sessionTarget = next)
    }

    fun setVideoBatchSize(size: Int) = viewModelScope.launch {
        val next = size.coerceIn(1, 100)
        settingsRepository.setVideoBatchSize(next)
        _videoScope.value = _videoScope.value.copy(sessionTarget = next)
    }

    fun onPhotoAction(photo: PhotoEntity, action: SwipeAction) {
        val exitTargetX = when (action) {
            SwipeAction.Keep -> 980f
            else -> 0f
        }
        val exitTargetY = when (action) {
            SwipeAction.Delete -> -1260f
            SwipeAction.Favorite -> 1260f
            SwipeAction.Keep -> 0f
        }
        onPhotoAction(photo, action, exitTargetX, exitTargetY)
    }

    fun onPhotoAction(photo: PhotoEntity, action: SwipeAction, exitTargetX: Float, exitTargetY: Float) {
        onPhotoAction(photo, action, exitTargetX, exitTargetY, systemTrashAuthorized = false)
    }

    private fun onPhotoAction(
        photo: PhotoEntity,
        action: SwipeAction,
        exitTargetX: Float,
        exitTargetY: Float,
        systemTrashAuthorized: Boolean,
    ) {
        if (action == SwipeAction.Delete &&
            settings.value.deleteMode == DeleteMode.SYSTEM_TRASH &&
            !systemTrashAuthorized
        ) {
            requestSystemTrashForPhoto(photo, exitTargetX, exitTargetY)
            return
        }
        if (!applyOptimisticPhotoAction(photo, action)) return
        val operationId = persistPhotoAction(photo) {
            repository.handlePhotoAction(photo, action, systemTrashAuthorized)
        }
        rememberPhotoSessionAction(photo, action, operationId, exitTargetX, exitTargetY)
    }

    fun markPhotoForTrashOutsideCleaning(photo: PhotoEntity) = viewModelScope.launch {
        if (settings.value.deleteMode == DeleteMode.SYSTEM_TRASH) {
            requestMediaAuthorization(
                kind = MediaOperationKind.TRASH,
                mediaUris = listOf(photo.uri),
                isTrashed = true,
                target = PendingMediaAuthorizationTarget.PhotoSystemTrash(
                    photo = photo,
                    exitTargetX = 0f,
                    exitTargetY = -920f,
                    affectsDeck = false,
                ),
            )
            return@launch
        }
        val operationId = viewModelScope.async { repository.handlePhotoAction(photo, SwipeAction.Delete) }
        rememberPhotoSessionAction(
            photo = photo,
            action = SwipeAction.Delete,
            operationId = operationId,
            exitTargetX = 0f,
            exitTargetY = -920f,
            affectsDeck = false,
        )
        runCatching { operationId.await() }
            .onSuccess {
                if (hasPhotoSessionAction(operationId)) _message.value = dynamicUiText("已加入待删区")
            }
            .onFailure {
                dropLastPhotoSessionAction(photo.mediaStoreId)
                _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_undo_failed)
            }
    }

    fun onPhotoActionWithOptionalMove(
        photo: PhotoEntity,
        action: SwipeAction,
        targetRelativePath: String?,
        exitTargetX: Float,
        exitTargetY: Float,
    ) {
        if (action == SwipeAction.Delete && settings.value.deleteMode == DeleteMode.SYSTEM_TRASH) {
            requestSystemTrashForPhoto(photo, exitTargetX, exitTargetY)
            return
        }
        if (
            targetRelativePath != null &&
            settings.value.autoMoveOnKeepFavorite &&
            action in setOf(SwipeAction.Keep, SwipeAction.Favorite)
        ) {
            requestMediaAuthorizationForPhotoActionWithMove(
                photo = photo,
                action = action,
                targetRelativePath = targetRelativePath,
                exitTargetX = exitTargetX,
                exitTargetY = exitTargetY,
            )
            return
        }
        if (!applyOptimisticPhotoAction(photo, action)) return
        val operationId = persistPhotoAction(photo) {
            repository.handlePhotoAction(photo, action)
        }
        rememberPhotoSessionAction(photo, action, operationId, exitTargetX, exitTargetY)
    }

    fun onVideoAction(
        video: VideoEntity,
        action: SwipeAction,
        systemTrashAuthorized: Boolean = false,
    ) = viewModelScope.launch {
        if (!videoActionInFlightMediaIds.add(video.mediaStoreId)) return@launch
        try {
            if (action == SwipeAction.Delete &&
                settings.value.deleteMode == DeleteMode.SYSTEM_TRASH &&
                !systemTrashAuthorized
            ) {
                requestSystemTrashForVideo(video)
                return@launch
            }
            handledVideoActionMediaIds += video.mediaStoreId
            invalidateVideoDeckLoads()
            val pendingKeepOperation = removePendingVideoKeep(video)
            _videoDeck.value = _videoDeck.value.filterNot { it.mediaStoreId == video.mediaStoreId }
            try {
                if (pendingKeepOperation != null && action == SwipeAction.Keep) {
                    pendingKeepOperation.await()
                } else {
                    pendingKeepOperation?.await()
                    repository.handleVideoAction(video, action, systemTrashAuthorized)
                }
                if (pendingKeepOperation == null || action != SwipeAction.Keep) _videoSessionActionCount.value += 1
                _lastVideoAction.value = action
                refillVideoDeckIfNeeded()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                handledVideoActionMediaIds -= video.mediaStoreId
                invalidateVideoDeckLoads()
                loadVideoDeck()
                _message.value = throwable.message?.let(::dynamicUiText) ?: uiText(R.string.message_video_action_failed)
            }
        } finally {
            videoActionInFlightMediaIds -= video.mediaStoreId
        }
    }

    fun markVideoPendingKeep(video: VideoEntity) {
        if (video.processingStatus != com.futureape.kanleme.data.local.ProcessingStatus.UNPROCESSED) return
        if (_videoDeck.value.none { it.mediaStoreId == video.mediaStoreId }) return
        val pending = _pendingVideoKeeps.value
        if (video.mediaStoreId in pending) return
        _pendingVideoKeeps.value = pending + (video.mediaStoreId to video)
        handledVideoActionMediaIds += video.mediaStoreId
        invalidateVideoDeckLoads()
        pendingVideoKeepOperations[video.mediaStoreId] = viewModelScope.async {
            repository.handleVideoAction(video, SwipeAction.Keep)
        }
        _videoSessionActionCount.value += 1
        _lastVideoAction.value = SwipeAction.Keep
    }

    private fun removePendingVideoKeep(video: VideoEntity): Deferred<Long>? {
        val pending = _pendingVideoKeeps.value
        if (video.mediaStoreId !in pending) return null
        _pendingVideoKeeps.value = pending - video.mediaStoreId
        return pendingVideoKeepOperations.remove(video.mediaStoreId)
    }

    private fun clearPendingVideoKeeps() {
        _pendingVideoKeeps.value = emptyMap()
        pendingVideoKeepOperations.clear()
    }

    fun undoPendingVideoKeep(): Long? {
        val pending = _pendingVideoKeeps.value
        if (pending.isEmpty()) return null
        val mediaStoreId = pending.keys.last()
        _pendingVideoKeeps.value = pending - mediaStoreId
        val pendingKeepOperation = pendingVideoKeepOperations.remove(mediaStoreId)
        handledVideoActionMediaIds -= mediaStoreId
        invalidateVideoDeckLoads()
        _videoSessionActionCount.value = (_videoSessionActionCount.value - 1).coerceAtLeast(0)
        _lastVideoAction.value = SwipeAction.Keep
        if (pendingKeepOperation != null) {
            viewModelScope.launch {
                runCatching {
                    val operationId = pendingKeepOperation.await()
                    repository.undoOperation(operationId)
                }.onFailure {
                    _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_undo_failed)
                }
            }
        }
        return mediaStoreId
    }

    fun finishVideoCleaningSession(currentVideo: VideoEntity?) = viewModelScope.launch {
        currentVideo?.let { markVideoPendingKeep(it) }
        val pending = _pendingVideoKeeps.value
        if (pending.isEmpty()) return@launch
        val pendingIds = pending.keys
        handledVideoActionMediaIds += pendingIds
        invalidateVideoDeckLoads()
        pendingVideoKeepOperations.values.forEach { operation ->
            runCatching { operation.await() }
                .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_video_action_failed) }
        }
        _videoDeck.value = _videoDeck.value.filterNot { it.mediaStoreId in pendingIds }
        clearPendingVideoKeeps()
        refillVideoDeckIfNeeded()
    }

    fun undoVideoCleaningAction(): Long? {
        undoPendingVideoKeep()?.let { mediaStoreId ->
            _message.value = uiText(R.string.message_back_to_previous_video)
            return mediaStoreId
        }
        undoLastAction("video")
        return null
    }

    fun movePhotoToFolder(photo: PhotoEntity, relativePath: String) = viewModelScope.launch {
        requestMediaAuthorization(
            kind = MediaOperationKind.WRITE,
            mediaUris = listOf(photo.uri),
            isTrashed = false,
            target = PendingMediaAuthorizationTarget.PhotoMove(photo, relativePath),
        )
    }

    fun moveVideoToFolder(video: VideoEntity, relativePath: String) = viewModelScope.launch {
        requestMediaAuthorization(
            kind = MediaOperationKind.WRITE,
            mediaUris = listOf(video.uri),
            isTrashed = false,
            target = PendingMediaAuthorizationTarget.VideoMove(video, relativePath),
        )
    }

    fun archivePhotoToFolder(photo: PhotoEntity, relativePath: String) = viewModelScope.launch {
        requestMediaAuthorization(
            kind = MediaOperationKind.WRITE,
            mediaUris = listOf(photo.uri),
            isTrashed = false,
            target = PendingMediaAuthorizationTarget.PhotoArchive(
                photo = photo,
                targetRelativePath = relativePath,
                originalRelativePath = photo.relativePath ?: photo.folderPath,
            ),
        )
    }

    fun archiveVideoToFolder(video: VideoEntity, relativePath: String) = viewModelScope.launch {
        requestMediaAuthorization(
            kind = MediaOperationKind.WRITE,
            mediaUris = listOf(video.uri),
            isTrashed = false,
            target = PendingMediaAuthorizationTarget.VideoArchive(
                video = video,
                targetRelativePath = relativePath,
                originalRelativePath = video.folderPath,
            ),
        )
    }

    fun seedSimilarGroups(limit: Int = 3600) {
        if (similarDetectionState.value.running) {
            _message.value = uiText(R.string.message_similar_detection_background)
            return
        }
        viewModelScope.launch {
            val accessKey = currentMediaAccessState?.key
            if (accessKey == null) {
                _message.value = uiText(R.string.message_media_permission_required)
                return@launch
            }
            if (dashboard.value.photoCount == 0) {
                runCatching { repository.refreshMediaLibrary(currentMediaAccessState.toSyncOptions()) }
            }
            val task = prepareSimilarAnalysis(accessKey, limit, forceRestart = true) ?: return@launch
            similarAnalysisScheduler.enqueue(SimilarAnalysisWorkMode.MANUAL)
            if (task.status == SimilarAnalysisTaskStatus.COMPLETED) {
                _message.value = uiText(R.string.message_similar_detection_background)
            }
        }
    }

    fun continueSimilarDetection() {
        val nextLimit = when {
            dashboard.value.photoCount > 0 -> dashboard.value.photoCount.coerceAtMost(8000)
            else -> 4800
        }
        if (similarDetectionState.value.running) {
            _message.value = uiText(R.string.message_similar_detection_background)
            return
        }
        viewModelScope.launch {
            val accessKey = currentMediaAccessState?.key
            if (accessKey == null) {
                _message.value = uiText(R.string.message_media_permission_required)
                return@launch
            }
            prepareSimilarAnalysis(
                accessKey = accessKey,
                limit = nextLimit,
                forceRestart = similarDetectionState.value.progress >= 1f,
            ) ?: return@launch
            similarAnalysisScheduler.enqueue(SimilarAnalysisWorkMode.MANUAL)
        }
    }

    private suspend fun prepareSimilarAnalysis(
        accessKey: String,
        limit: Int,
        forceRestart: Boolean,
    ): SimilarAnalysisTaskEntity? {
        return runCatching {
            repository.prepareSimilarAnalysis(
                accessKey = accessKey,
                limit = limit.coerceIn(2, SimilarAnalysisContract.DEFAULT_LIMIT * 3),
                forceRestart = forceRestart,
            )
        }.getOrElse { throwable ->
            _message.value = throwable.message?.let(::dynamicUiText) ?: uiText(R.string.message_similar_detection_failed)
            null
        }
    }

    fun restoreTrash(id: Long) {
        trashItems.value.firstOrNull { it.id == id }?.let { restoreTrashItems(listOf(it)) }
    }

    fun restoreTrashItems(items: List<TrashItemEntity>): Job = viewModelScope.launch {
        val distinctItems = items.distinctBy { it.id }
        if (distinctItems.isEmpty()) return@launch
        val currentBatch = distinctItems.take(MAX_MEDIA_AUTHORIZATION_BATCH)
        val remainingItems = distinctItems.drop(MAX_MEDIA_AUTHORIZATION_BATCH)
        runCatching { repository.prepareTrashRestore(currentBatch.map { it.id }) }
            .onSuccess { plan ->
                when (val authorization = plan.authorization) {
                    MediaAuthorizationResult.Completed -> {
                        currentBatch.forEach { repository.restoreTrashItem(it.id) }
                        restoreTrashItemsToDeck(currentBatch)
                        if (remainingItems.isNotEmpty()) {
                            restoreTrashItems(remainingItems)
                        } else {
                            _message.value = if (currentBatch.size > 1) {
                                uiText(R.string.message_trash_all_restored)
                            } else {
                                uiText(R.string.message_restore_success)
                            }
                        }
                    }
                    is MediaAuthorizationResult.NeedsUserConsent -> {
                        if (mediaOperationTracker.register(authorization.request.operationId)) {
                            _pendingMediaAuthorization.value = PendingMediaAuthorization(
                                request = authorization.request,
                                target = PendingMediaAuthorizationTarget.RestoreTrash(
                                    ids = currentBatch.map { it.id },
                                    items = currentBatch,
                                    remainingItems = remainingItems,
                                ),
                            )
                        } else {
                            _message.value = uiText(R.string.message_media_authorization_failed)
                        }
                    }
                    is MediaAuthorizationResult.Cancelled -> {
                        _message.value = uiText(R.string.message_media_authorization_cancelled)
                    }
                    is MediaAuthorizationResult.PartialFailure -> {
                        _message.value = uiText(R.string.message_media_authorization_partial)
                    }
                    is MediaAuthorizationResult.Failed -> {
                        _message.value = dynamicUiText(authorization.reason)
                    }
                }
            }
            .onFailure { throwable ->
                _message.value = throwable.message?.let(::dynamicUiText)
                    ?: uiText(R.string.message_restore_failed)
            }
    }

    fun requestPermanentDelete(items: List<TrashItemEntity>) = viewModelScope.launch {
        val distinctItems = items.distinctBy { it.id }
        if (distinctItems.isEmpty()) return@launch
        requestMediaAuthorization(
            kind = MediaOperationKind.PERMANENT_DELETE,
            mediaUris = distinctItems.map { it.uri },
            isTrashed = false,
            target = PendingMediaAuthorizationTarget.PermanentDelete(distinctItems.map { it.id }),
        )
    }

    fun restoreAllTrash() {
        restoreTrashItems(trashItems.value)
    }

    private fun restoreTrashItemsToDeck(items: List<TrashItemEntity>) {
        val photoMediaStoreIds = items
            .filter { it.mediaType != "video" }
            .map { it.mediaStoreId }
            .toSet()
        if (photoMediaStoreIds.isNotEmpty()) {
            pendingPhotoActionMediaIds.removeAll(photoMediaStoreIds)
            handledPhotoActionMediaIds.removeAll(photoMediaStoreIds)
            invalidatePhotoDeckLoads()
            loadPhotoDeck()
        }

        val videoMediaStoreIds = items
            .filter { it.mediaType == "video" }
            .map { it.mediaStoreId }
            .toSet()
        if (videoMediaStoreIds.isNotEmpty()) {
            handledVideoActionMediaIds.removeAll(videoMediaStoreIds)
            _pendingVideoKeeps.value = _pendingVideoKeeps.value - videoMediaStoreIds
            videoMediaStoreIds.forEach { pendingVideoKeepOperations.remove(it) }
            invalidateVideoDeckLoads()
            loadVideoDeck()
        }
    }

    fun undoLastAction(mediaType: String? = null) = viewModelScope.launch {
        val authorization = runCatching { repository.createUndoAuthorizationRequest(mediaType) }
            .getOrElse { throwable ->
                _message.value = throwable.message?.let(::dynamicUiText) ?: uiText(R.string.message_undo_failed)
                return@launch
            }
        when (authorization) {
            MediaAuthorizationResult.Completed -> {
                runCatching { repository.undoLastAction(mediaType) }
                    .onSuccess { ok -> applyUndoLastActionResult(mediaType, ok) }
                    .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_undo_failed) }
            }
            is MediaAuthorizationResult.NeedsUserConsent -> {
                if (mediaOperationTracker.register(authorization.request.operationId)) {
                    _pendingMediaAuthorization.value = PendingMediaAuthorization(
                        request = authorization.request,
                        target = PendingMediaAuthorizationTarget.UndoLastAction(mediaType),
                    )
                } else {
                    _message.value = uiText(R.string.message_media_authorization_failed)
                }
            }
            is MediaAuthorizationResult.Cancelled -> _message.value = uiText(R.string.message_media_authorization_cancelled)
            is MediaAuthorizationResult.PartialFailure -> _message.value = uiText(R.string.message_media_authorization_partial)
            is MediaAuthorizationResult.Failed -> _message.value = dynamicUiText(authorization.reason)
        }
    }

    private fun applyUndoLastActionResult(mediaType: String?, ok: Boolean) {
        if (ok) {
            if (mediaType == null || mediaType == "photo") {
                pendingPhotoActionMediaIds.clear()
                handledPhotoActionMediaIds.clear()
                clearPhotoActionHistory()
                invalidatePhotoDeckLoads()
                loadPhotoDeck()
            }
            if (mediaType == null || mediaType == "video") {
                handledVideoActionMediaIds.clear()
                clearPendingVideoKeeps()
                invalidateVideoDeckLoads()
                loadVideoDeck()
            }
        }
        _message.value = if (ok) uiText(R.string.message_undo_success) else uiText(R.string.message_undo_empty)
    }

    fun undoPhotoCleaningAction() {
        val snapshot = popPhotoSessionAction()
        if (snapshot == null) {
            _message.value = uiText(R.string.message_photo_undo_empty)
            return
        }
        restorePhotoSessionAction(snapshot)
        persistPhotoSessionUndo(snapshot)
    }

    fun buildAnnualReport(year: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) = viewModelScope.launch {
        _annualReport.value = repository.buildAnnualReport(year)
    }

    fun loadAchievements() = viewModelScope.launch {
        _achievements.value = repository.buildAchievements()
    }

    fun cycleDeleteMode() = viewModelScope.launch {
        val next = if (settings.value.deleteMode == DeleteMode.PENDING_CONFIRM) DeleteMode.SYSTEM_TRASH else DeleteMode.PENDING_CONFIRM
        settingsRepository.setDeleteMode(next)
    }

    fun setDeleteMode(value: DeleteMode) = viewModelScope.launch { settingsRepository.setDeleteMode(value) }

    fun cycleBatchSize() = viewModelScope.launch {
        // Kept for old settings compatibility. The setting is a session target;
        // continuous Deck loading uses its own internal window.
        loadPhotoDeck()
        loadVideoDeck()
    }

    fun setAutoMoveOnKeepFavorite(value: Boolean) = viewModelScope.launch { settingsRepository.setAutoMoveOnKeepFavorite(value) }

    fun setSimilarDetection(value: Boolean) = viewModelScope.launch {
        settingsRepository.setSimilarDetection(value)
        if (value) {
            val accessKey = currentMediaAccessState?.key
            if (accessKey != null) {
                val task = repository.prepareSimilarAnalysis(
                    accessKey = accessKey,
                    limit = SimilarAnalysisContract.DEFAULT_LIMIT,
                    forceRestart = false,
                )
                if (task.status != SimilarAnalysisTaskStatus.COMPLETED && task.status != SimilarAnalysisTaskStatus.CANCELLED) {
                    similarAnalysisScheduler.enqueue(SimilarAnalysisWorkMode.AUTO)
                }
            }
        } else {
            repository.cancelSimilarAnalysis()
            similarAnalysisScheduler.cancel()
            repository.markSimilarAnalysisCancelled()
        }
    }

    fun cycleSwipeSensitivity() = viewModelScope.launch {
        val values = SwipeSensitivity.entries
        settingsRepository.setSwipeSensitivity(values[(values.indexOf(settings.value.swipeSensitivity) + 1) % values.size])
    }

    fun setSwipeSensitivity(value: SwipeSensitivity) = viewModelScope.launch { settingsRepository.setSwipeSensitivity(value) }

    fun cycleGestureDirection() = viewModelScope.launch {
        val next = if (settings.value.gestureDirection == GestureDirection.DEFAULT) GestureDirection.REVERSE_VERTICAL else GestureDirection.DEFAULT
        settingsRepository.setGestureDirection(next)
    }

    fun setGestureDirection(value: GestureDirection) = viewModelScope.launch { settingsRepository.setGestureDirection(value) }

    fun setQuickActionButtons(value: Boolean) = viewModelScope.launch { settingsRepository.setQuickActionButtons(value) }
    fun setSwapShareAndUndo(value: Boolean) = viewModelScope.launch { settingsRepository.setSwapShareAndUndo(value) }
    fun setSwipeSound(value: Boolean) = viewModelScope.launch { settingsRepository.setSwipeSound(value) }
    fun setSwipeSoundStyle(value: SwipeSoundStyle) = viewModelScope.launch { settingsRepository.setSwipeSoundStyle(value) }
    fun setVideoDefaultMuted(value: Boolean) = viewModelScope.launch { settingsRepository.setVideoDefaultMuted(value) }

    fun cycleVideoDisplayMode() = viewModelScope.launch {
        val values = VideoDisplayMode.entries
        settingsRepository.setVideoDisplayMode(values[(values.indexOf(settings.value.videoDisplayMode) + 1) % values.size])
    }

    fun setVideoDisplayMode(value: VideoDisplayMode) = viewModelScope.launch { settingsRepository.setVideoDisplayMode(value) }

    fun toggleVideoDisplayModeQuick() = viewModelScope.launch {
        val next = if (settings.value.videoDisplayMode == VideoDisplayMode.FIT_SCREEN) {
            VideoDisplayMode.IMMERSIVE_CROP
        } else {
            VideoDisplayMode.FIT_SCREEN
        }
        settingsRepository.setVideoDisplayMode(next)
    }

    fun cycleHapticLevel() = viewModelScope.launch {
        val values = HapticLevel.entries
        settingsRepository.setHapticLevel(values[(values.indexOf(settings.value.hapticLevel) + 1) % values.size])
    }

    fun setHapticLevel(value: HapticLevel) = viewModelScope.launch {
        settingsRepository.setHapticLevel(value)
    }

    fun setKeepHapticLevel(value: HapticLevel) = viewModelScope.launch {
        settingsRepository.setKeepHapticLevel(value)
    }

    fun setDeleteHapticLevel(value: HapticLevel) = viewModelScope.launch {
        settingsRepository.setDeleteHapticLevel(value)
    }

    fun setFavoriteHapticLevel(value: HapticLevel) = viewModelScope.launch {
        settingsRepository.setFavoriteHapticLevel(value)
    }

    fun setUndoHapticLevel(value: HapticLevel) = viewModelScope.launch {
        settingsRepository.setUndoHapticLevel(value)
    }

    fun cycleThemeMode() = viewModelScope.launch {
        val values = ThemeMode.entries
        settingsRepository.setThemeMode(values[(values.indexOf(settings.value.themeMode) + 1) % values.size])
    }

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(value)
    }

    fun setTodayInHistoryEntryMode(value: TodayInHistoryEntryMode) = viewModelScope.launch {
        settingsRepository.setTodayInHistoryEntryMode(value)
    }

    fun cycleAccentColor() = viewModelScope.launch { settingsRepository.setAccentColor(nextAccentColor(settings.value.accentColor)) }

    fun setAccentColor(value: Long) = viewModelScope.launch {
        settingsRepository.setAccentColor(value)
    }

    fun cycleFolderDisplay() = viewModelScope.launch {
        val next = if (settings.value.folderDisplay == FolderDisplayMode.SINGLE_LINE) FolderDisplayMode.MULTI_LINE else FolderDisplayMode.SINGLE_LINE
        settingsRepository.setFolderDisplay(next)
    }

    fun setFolderDisplay(value: FolderDisplayMode) = viewModelScope.launch {
        settingsRepository.setFolderDisplay(value)
    }

    fun setImmersiveBackground(value: Boolean) = viewModelScope.launch { settingsRepository.setImmersiveBackground(value) }

    fun setPhotoFocusMode(value: Boolean) = viewModelScope.launch { settingsRepository.setPhotoFocusMode(value) }
    fun setPhotoShowTopBar(value: Boolean) = viewModelScope.launch { settingsRepository.setPhotoShowTopBar(value) }
    fun setPhotoShowFilterChips(value: Boolean) = viewModelScope.launch { settingsRepository.setPhotoShowFilterChips(value) }
    fun setPhotoShowFolderChips(value: Boolean) = viewModelScope.launch { settingsRepository.setPhotoShowFolderChips(value) }
    fun setPhotoShowActionRail(value: Boolean) = viewModelScope.launch { settingsRepository.setPhotoShowActionRail(value) }
    fun setPhotoShowInfoBar(value: Boolean) = viewModelScope.launch { settingsRepository.setPhotoShowInfoBar(value) }
    fun setPhotoShowGestureHint(value: Boolean) = viewModelScope.launch { settingsRepository.setPhotoShowGestureHint(value) }
    fun setPhotoShowShuffleButton(value: Boolean) = viewModelScope.launch { settingsRepository.setPhotoShowShuffleButton(value) }
    fun setVideoShowTopBar(value: Boolean) = viewModelScope.launch { settingsRepository.setVideoShowTopBar(value) }
    fun setVideoShowActionRail(value: Boolean) = viewModelScope.launch { settingsRepository.setVideoShowActionRail(value) }
    fun setVideoShowInfoPanel(value: Boolean) = viewModelScope.launch { settingsRepository.setVideoShowInfoPanel(value) }
    fun setVideoShowFolderChips(value: Boolean) = viewModelScope.launch { settingsRepository.setVideoShowFolderChips(value) }
    fun setVideoShowProgressBar(value: Boolean) = viewModelScope.launch { settingsRepository.setVideoShowProgressBar(value) }
    fun setVideoShowShuffleButton(value: Boolean) = viewModelScope.launch { settingsRepository.setVideoShowShuffleButton(value) }
    fun setVideoChromeVisible(value: Boolean) = viewModelScope.launch { settingsRepository.setVideoChromeVisible(value) }

    fun markPhotoGuideShown() = viewModelScope.launch { settingsRepository.setPhotoGuideShown(true) }
    fun markVideoGuideShown() = viewModelScope.launch { settingsRepository.setVideoGuideShown(true) }
    fun markOnboardingShown() = viewModelScope.launch { settingsRepository.setOnboardingShown(true) }

    fun replayPositionGuides() = viewModelScope.launch {
        settingsRepository.setPhotoGuideShown(false)
        settingsRepository.setVideoGuideShown(false)
        _message.value = uiText(R.string.message_position_guides_reset)
    }

    fun addExcludedFolder(path: String) = viewModelScope.launch {
        settingsRepository.addExcludedFolder(path)
        loadPhotoDeck()
        loadVideoDeck()
    }

    fun toggleExcludedFolder(path: String) = viewModelScope.launch {
        settingsRepository.toggleExcludedFolder(path)
        loadPhotoDeck()
        loadVideoDeck()
    }

    fun clearExcludedFolders() = viewModelScope.launch {
        settingsRepository.clearExcludedFolders()
        loadPhotoDeck()
        loadVideoDeck()
    }

    fun cyclePhotoCleanMode() = viewModelScope.launch {
        val values = PhotoCleanMode.entries
        val next = values[(values.indexOf(settings.value.photoCleanMode) + 1) % values.size]
        settingsRepository.setPhotoCleanMode(next)
    }

    fun setPhotoCleanMode(value: PhotoCleanMode) = viewModelScope.launch {
        settingsRepository.setPhotoCleanMode(value)
    }

    private fun AppSettings.toPhotoCleaningScope(): CleaningScope = CleaningScope(
        dateMode = photoDateMode,
        folderPaths = photoFolderPath.takeIf { it.isNotBlank() }?.let { setOf(it) } ?: emptySet(),
        mediaType = photoMediaType,
        sortOrder = photoSortOrder,
        todayInHistory = photoDateMode == "today_history",
        sessionTarget = photoBatchSize.coerceIn(1, 100),
        randomSeed = photoRandomSeed,
    )

    private fun AppSettings.toVideoCleaningScope(): CleaningScope = CleaningScope(
        dateMode = videoDateMode,
        folderPaths = videoFolderPath.takeIf { it.isNotBlank() }?.let { setOf(it) } ?: emptySet(),
        sortOrder = videoSortOrder,
        todayInHistory = videoDateMode == "today_history",
        sessionTarget = videoBatchSize.coerceIn(1, 100),
        randomSeed = videoRandomSeed,
    )
}
