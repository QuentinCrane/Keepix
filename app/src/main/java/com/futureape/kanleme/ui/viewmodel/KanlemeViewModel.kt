package com.futureape.kanleme.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.TrashItemEntity
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.data.repository.AppRepository
import com.futureape.kanleme.data.repository.CleaningScope
import com.futureape.kanleme.data.repository.DashboardStats
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.repository.PhotoTypeStats
import com.futureape.kanleme.data.repository.AnnualReport
import com.futureape.kanleme.data.repository.AchievementUi
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.data.settings.AppSettingsRepository
import com.futureape.kanleme.data.settings.AppVisualStyle
import com.futureape.kanleme.data.settings.DeleteMode
import com.futureape.kanleme.data.settings.FolderDisplayMode
import com.futureape.kanleme.data.settings.GestureDirection
import com.futureape.kanleme.data.settings.HapticLevel
import com.futureape.kanleme.data.settings.PhotoCleanMode
import com.futureape.kanleme.data.settings.SwipeSensitivity
import com.futureape.kanleme.data.settings.ThemeMode
import com.futureape.kanleme.data.settings.VideoDisplayMode
import com.futureape.kanleme.data.settings.nextAccentColor
import com.futureape.kanleme.data.settings.nextBatchSize
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.i18n.UiText
import com.futureape.kanleme.ui.i18n.dynamicUiText
import com.futureape.kanleme.ui.i18n.uiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SimilarDetectionUiState(
    val running: Boolean = false,
    val progress: Float = 0f,
    val stage: UiText = uiText(R.string.similar_stage_not_started),
    val processedHint: Int = 0,
    val totalHint: Int = 0,
    val lastResultCount: Int = 0,
)

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

@HiltViewModel
class KanlemeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {
    private companion object {
        const val CONTINUOUS_PHOTO_DECK_SIZE = 420
        const val CONTINUOUS_VIDEO_DECK_SIZE = 240
        const val PHOTO_PREFETCH_THRESHOLD = 120
        const val VIDEO_PREFETCH_THRESHOLD = 80
        const val HOME_PREVIEW_DECK_SIZE = 3
    }
    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded = _settingsLoaded.asStateFlow()

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

    private val _similarDetectionState = MutableStateFlow(SimilarDetectionUiState())
    val similarDetectionState = _similarDetectionState.asStateFlow()
    val similarDetectionRunning = _similarDetectionState.map { it.running }
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

    private var lastAutoRefreshAccessKey: String? = null
    private var photoDeckRefilling = false
    private var videoDeckRefilling = false
    private var photoDeckLoadJob: Job? = null
    private var videoDeckLoadJob: Job? = null
    private var photoDeckPreviewLoadJob: Job? = null
    private var videoDeckPreviewLoadJob: Job? = null
    private var photoDeckPreviewScope: CleaningScope? = null
    private var videoDeckPreviewScope: CleaningScope? = null
    private var photoDeckPreviewScopeLoaded = false
    private var videoDeckPreviewScopeLoaded = false
    private var photoDayMemoryWindowJob: Job? = null
    private var photoDeckGeneration = 0L
    private var videoDeckGeneration = 0L
    private var photoDeckPreviewGeneration = 0L
    private var videoDeckPreviewGeneration = 0L
    private var photoDayMemoryWindowGeneration = 0L
    private var lastGeneratedRandomSeed = 1L
    private var similarDetectionJob: Job? = null
    private val pendingPhotoActionMediaIds = mutableSetOf<Long>()
    private val handledPhotoActionMediaIds = mutableSetOf<Long>()
    private val photoSessionActions = ArrayDeque<PhotoSessionAction>()
    private val photoPersistenceBarriers = mutableMapOf<Long, Job>()
    private val handledVideoActionMediaIds = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            val persisted = settingsRepository.settings.first()
            val photoScope = persisted.toPhotoCleaningScope()
            val videoScope = persisted.toVideoCleaningScope()
            _photoScope.value = photoScope
            _videoScope.value = videoScope
            if (persisted.appVisualStyle == AppVisualStyle.IMMERSIVE_PHOTO || persisted.homeMediaTab != "video") {
                loadPhotoDeckPreview(photoScope)
            } else {
                loadVideoDeckPreview(videoScope)
            }
        }
    }

    fun onMediaAccessReady(accessKey: String, accessLabel: String) {
        if (lastAutoRefreshAccessKey == accessKey) return
        lastAutoRefreshAccessKey = accessKey
        // 首次授权后的自动同步不再弹出悬浮提示，避免遮挡首页或整理页按钮。
        refreshLibrary(accessLabel = accessLabel, showMessage = false)
    }

    fun refreshLibrary(accessLabel: String? = null, showMessage: Boolean = true) = viewModelScope.launch {
        val prefix = accessLabel?.let { it + "：" }.orEmpty()
        runCatching { repository.refreshMediaLibrary() }
            .onSuccess { (p, v) ->
                if (showMessage) {
                    _message.value = uiText(R.string.message_library_synced, p, v)
                }
            }
            .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_library_sync_failed) }
    }

    fun clearMessage() { _message.value = null }

    fun showMessage(message: String) {
        _message.value = dynamicUiText(message)
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
        photoDeckPreviewScope = null
        photoDeckPreviewScopeLoaded = false
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
        invalidatePhotoDeckLoads()
        _photoDeck.value = _photoDeck.value.filterNot { it.mediaStoreId == photo.mediaStoreId }
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
                .take(CONTINUOUS_PHOTO_DECK_SIZE)
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
            .take(CONTINUOUS_PHOTO_DECK_SIZE)
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
                    snapshot.undoPersistedAction(operationId)
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

    fun loadPhotoDeck(scope: CleaningScope = _photoScope.value) {
        val requestedScope = ensureRandomSeed(scope)
        _photoScope.value = requestedScope
        val showPreparing = _photoDeck.value.isEmpty()
        if (showPreparing) _photoDeckPreparing.value = true
        val generation = nextPhotoDeckGeneration()
        photoDeckLoadJob = viewModelScope.launch {
            try {
                val loaded = repository
                    .loadPhotoDeck(requestedScope.copy(batchSize = CONTINUOUS_PHOTO_DECK_SIZE))
                    .withoutLocalPhotoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == photoDeckGeneration) {
                    _photoDeck.value = loaded
                    _photoDeckPreview.value = loaded.take(HOME_PREVIEW_DECK_SIZE)
                }
            } finally {
                if (showPreparing && generation == photoDeckGeneration) _photoDeckPreparing.value = false
            }
        }
    }

    fun loadPhotoDeckPreview(scope: CleaningScope = _photoScope.value) {
        if (_photoDeck.value.isNotEmpty()) {
            _photoDeckPreview.value = _photoDeck.value.take(HOME_PREVIEW_DECK_SIZE)
            return
        }
        val requestedScope = ensureRandomSeed(scope.copy(sortOrder = if (scope.sortOrder.isBlank()) "random" else scope.sortOrder))
        if (photoDeckPreviewScope == requestedScope && (_photoDeckPreviewPreparing.value || photoDeckPreviewScopeLoaded)) {
            return
        }
        _photoScope.value = requestedScope
        photoDeckPreviewScope = requestedScope
        photoDeckPreviewScopeLoaded = false
        _photoDeckPreviewPreparing.value = true
        photoDeckPreviewLoadJob?.cancel()
        val generation = ++photoDeckPreviewGeneration
        photoDeckPreviewLoadJob = viewModelScope.launch {
            try {
                val preview = repository
                    .loadPhotoDeck(requestedScope.copy(batchSize = HOME_PREVIEW_DECK_SIZE))
                    .withoutLocalPhotoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == photoDeckPreviewGeneration && _photoDeck.value.isEmpty()) {
                    _photoDeckPreview.value = preview
                    photoDeckPreviewScopeLoaded = true
                }
            } finally {
                if (generation == photoDeckPreviewGeneration) _photoDeckPreviewPreparing.value = false
            }
        }
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
                val loaded = repository
                    .loadVideoDeck(requestedScope.copy(batchSize = CONTINUOUS_VIDEO_DECK_SIZE))
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
                val preview = repository
                    .loadVideoDeck(requestedScope.copy(batchSize = HOME_PREVIEW_DECK_SIZE))
                    .withoutLocalVideoExclusions()
                    .distinctBy { it.mediaStoreId }
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
        val base = _photoScope.value
        val session = ensureRandomSeed(base.copy(sortOrder = if (base.sortOrder.isBlank()) "random" else base.sortOrder))
        _photoScope.value = session
        _photoSessionActionCount.value = 0
        _lastPhotoAction.value = null
        pendingPhotoActionMediaIds.clear()
        clearPhotoActionHistory()
        _photoDeck.value = _photoDeck.value.withoutLocalPhotoExclusions()
        if (_photoDeck.value.isNotEmpty()) {
            if (session.sortOrder == "random") {
                viewModelScope.launch { settingsRepository.setPhotoSortOrderWithSeed("random", session.randomSeed) }
            }
            _photoDeckPreparing.value = false
            return
        }
        _photoDeckPreparing.value = true
        val generation = nextPhotoDeckGeneration()
        photoDeckLoadJob = viewModelScope.launch {
            if (session.sortOrder == "random") settingsRepository.setPhotoSortOrderWithSeed("random", session.randomSeed)
            try {
                val loaded = repository
                    .loadPhotoDeck(session.copy(batchSize = CONTINUOUS_PHOTO_DECK_SIZE))
                    .withoutLocalPhotoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == photoDeckGeneration) {
                    _photoDeck.value = loaded
                    _photoDeckPreview.value = loaded.take(HOME_PREVIEW_DECK_SIZE)
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
        _pendingVideoKeeps.value = emptyMap()
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
                    .loadVideoDeck(session.copy(batchSize = CONTINUOUS_VIDEO_DECK_SIZE))
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
        val generation = nextPhotoDeckGeneration()
        photoDeckLoadJob = viewModelScope.launch {
            settingsRepository.setPhotoSortOrderWithSeed("random", session.randomSeed)
            try {
                val loaded = repository
                    .loadPhotoDeck(session.copy(batchSize = CONTINUOUS_PHOTO_DECK_SIZE))
                    .withoutLocalPhotoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == photoDeckGeneration) _photoDeck.value = loaded
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
        _pendingVideoKeeps.value = emptyMap()
        _videoDeck.value = emptyList()
        val generation = nextVideoDeckGeneration()
        videoDeckLoadJob = viewModelScope.launch {
            settingsRepository.setVideoSortOrderWithSeed("random", session.randomSeed)
            try {
                val loaded = repository
                    .loadVideoDeck(session.copy(batchSize = CONTINUOUS_VIDEO_DECK_SIZE))
                    .withoutLocalVideoExclusions()
                    .distinctBy { it.mediaStoreId }
                if (generation == videoDeckGeneration) _videoDeck.value = loaded
            } finally {
                if (generation == videoDeckGeneration) _videoDeckPreparing.value = false
            }
        }
    }

    private suspend fun refillPhotoDeckIfNeeded(force: Boolean = false) {
        if (!force && _photoDeck.value.size > PHOTO_PREFETCH_THRESHOLD) return
        if (photoDeckRefilling) return
        val generation = photoDeckGeneration
        photoDeckRefilling = true
        try {
            val fresh = repository.loadPhotoDeck(_photoScope.value.copy(batchSize = CONTINUOUS_PHOTO_DECK_SIZE))
            if (generation != photoDeckGeneration) return
            val merged = (_photoDeck.value + fresh)
                .withoutLocalPhotoExclusions()
                .distinctBy { it.mediaStoreId }
                .take(CONTINUOUS_PHOTO_DECK_SIZE)
            if (generation == photoDeckGeneration) _photoDeck.value = merged
        } finally {
            photoDeckRefilling = false
        }
    }

    private suspend fun refillVideoDeckIfNeeded(force: Boolean = false) {
        if (!force && _videoDeck.value.size > VIDEO_PREFETCH_THRESHOLD) return
        if (videoDeckRefilling) return
        val generation = videoDeckGeneration
        videoDeckRefilling = true
        try {
            val fresh = repository.loadVideoDeck(_videoScope.value.copy(batchSize = CONTINUOUS_VIDEO_DECK_SIZE))
            if (generation != videoDeckGeneration) return
            val merged = (_videoDeck.value + fresh)
                .withoutLocalVideoExclusions()
                .distinctBy { it.mediaStoreId }
                .take(CONTINUOUS_VIDEO_DECK_SIZE)
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
        loadPhotoDeck(_photoScope.value.copy(mediaType = type))
    }

    fun setPhotoTypePreview(type: String) = viewModelScope.launch {
        settingsRepository.setPhotoMediaType(type)
        preparePhotoPreview(_photoScope.value.copy(mediaType = type))
    }

    fun setPhotoDateMode(mode: String) = viewModelScope.launch {
        settingsRepository.setPhotoDateMode(mode)
        loadPhotoDeck(_photoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"))
    }

    fun setPhotoDateModePreview(mode: String) = viewModelScope.launch {
        settingsRepository.setPhotoDateMode(mode)
        preparePhotoPreview(_photoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"))
    }

    fun setPhotoSessionDateMode(mode: String) {
        loadPhotoDeck(_photoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"))
    }

    fun resetPhotoSessionDateMode() {
        _photoScope.value = _photoScope.value.copy(dateMode = "all", todayInHistory = false)
    }

    fun setVideoDateMode(mode: String) = viewModelScope.launch {
        settingsRepository.setVideoDateMode(mode)
        loadVideoDeck(_videoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"))
    }

    fun setVideoDateModePreview(mode: String) = viewModelScope.launch {
        settingsRepository.setVideoDateMode(mode)
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
        loadPhotoDeck(_photoScope.value.copy(folderPaths = path?.let { setOf(it) } ?: emptySet()))
    }

    fun setPhotoFolderPreview(path: String?) = viewModelScope.launch {
        settingsRepository.setPhotoFolderPath(path)
        preparePhotoPreview(_photoScope.value.copy(folderPaths = path?.let { setOf(it) } ?: emptySet()))
    }

    fun togglePhotoRandom() = viewModelScope.launch {
        val next = if (_photoScope.value.sortOrder == "random") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _photoScope.value.randomSeed
        settingsRepository.setPhotoSortOrderWithSeed(next, seed)
        loadPhotoDeck(_photoScope.value.copy(sortOrder = next, randomSeed = seed))
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
        loadPhotoDeck(_photoScope.value.copy(sortOrder = next, randomSeed = seed))
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
        _photoScope.value = _photoScope.value.copy(batchSize = next)
    }

    fun setPhotoBatchSize(size: Int) = viewModelScope.launch {
        val next = size.coerceIn(1, 100)
        settingsRepository.setPhotoBatchSize(next)
        _photoScope.value = _photoScope.value.copy(batchSize = next)
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
        _videoScope.value = _videoScope.value.copy(batchSize = next)
    }

    fun setVideoBatchSize(size: Int) = viewModelScope.launch {
        val next = size.coerceIn(1, 100)
        settingsRepository.setVideoBatchSize(next)
        _videoScope.value = _videoScope.value.copy(batchSize = next)
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
        if (!applyOptimisticPhotoAction(photo, action)) return
        val operationId = persistPhotoAction(photo) {
            repository.handlePhotoAction(photo, action)
        }
        rememberPhotoSessionAction(photo, action, operationId, exitTargetX, exitTargetY)
    }

    fun markPhotoForTrashOutsideCleaning(photo: PhotoEntity) = viewModelScope.launch {
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
        if (!applyOptimisticPhotoAction(photo, action)) return
        val operationId = persistPhotoAction(photo) {
            val s = settingsRepository.settings.first()
            if (targetRelativePath != null && s.autoMoveOnKeepFavorite && action in setOf(SwipeAction.Keep, SwipeAction.Favorite)) {
                repository.movePhotoToFolder(photo, targetRelativePath)
            }
            repository.handlePhotoAction(photo, action)
        }
        rememberPhotoSessionAction(photo, action, operationId, exitTargetX, exitTargetY)
    }

    fun onVideoAction(video: VideoEntity, action: SwipeAction) = viewModelScope.launch {
        handledVideoActionMediaIds += video.mediaStoreId
        invalidateVideoDeckLoads()
        val wasPending = removePendingVideoKeep(video)
        _videoDeck.value = _videoDeck.value.filterNot { it.mediaStoreId == video.mediaStoreId }
        repository.handleVideoAction(video, action)
        if (!wasPending) _videoSessionActionCount.value += 1
        _lastVideoAction.value = action
        refillVideoDeckIfNeeded()
    }

    fun markVideoPendingKeep(video: VideoEntity) {
        if (video.processingStatus != com.futureape.kanleme.data.local.ProcessingStatus.UNPROCESSED) return
        if (_videoDeck.value.none { it.mediaStoreId == video.mediaStoreId }) return
        val pending = _pendingVideoKeeps.value
        if (video.mediaStoreId in pending) return
        _pendingVideoKeeps.value = pending + (video.mediaStoreId to video)
        _videoSessionActionCount.value += 1
        _lastVideoAction.value = SwipeAction.Keep
    }

    private fun removePendingVideoKeep(video: VideoEntity): Boolean {
        val pending = _pendingVideoKeeps.value
        if (video.mediaStoreId !in pending) return false
        _pendingVideoKeeps.value = pending - video.mediaStoreId
        return true
    }

    fun undoPendingVideoKeep(): Long? {
        val pending = _pendingVideoKeeps.value
        if (pending.isEmpty()) return null
        val mediaStoreId = pending.keys.last()
        _pendingVideoKeeps.value = pending - mediaStoreId
        _videoSessionActionCount.value = (_videoSessionActionCount.value - 1).coerceAtLeast(0)
        _lastVideoAction.value = SwipeAction.Keep
        return mediaStoreId
    }

    fun finishVideoCleaningSession(currentVideo: VideoEntity?) = viewModelScope.launch {
        currentVideo?.let { markVideoPendingKeep(it) }
        val pending = _pendingVideoKeeps.value
        if (pending.isEmpty()) return@launch
        val pendingIds = pending.keys
        handledVideoActionMediaIds += pendingIds
        invalidateVideoDeckLoads()
        pending.values.forEach { video ->
            repository.handleVideoAction(video, SwipeAction.Keep)
        }
        _videoDeck.value = _videoDeck.value.filterNot { it.mediaStoreId in pendingIds }
        _pendingVideoKeeps.value = emptyMap()
        refillVideoDeckIfNeeded()
    }

    fun undoVideoCleaningAction(): Long? {
        undoPendingVideoKeep()?.let { mediaStoreId ->
            _message.value = uiText(R.string.message_back_to_previous_video)
            return mediaStoreId
        }
        viewModelScope.launch {
            runCatching { repository.undoLastAction() }
                .onSuccess { ok ->
                    if (ok) {
                        handledVideoActionMediaIds.clear()
                        invalidateVideoDeckLoads()
                        _videoSessionActionCount.value = (_videoSessionActionCount.value - 1).coerceAtLeast(0)
                        _message.value = uiText(R.string.message_video_undo_success)
                        loadVideoDeck()
                    } else {
                        _message.value = uiText(R.string.message_video_undo_empty)
                    }
                }
                .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_undo_failed) }
        }
        return null
    }

    fun movePhotoToFolder(photo: PhotoEntity, relativePath: String) = viewModelScope.launch {
        runCatching { repository.movePhotoToFolder(photo, relativePath) }
            .onSuccess { _message.value = dynamicUiText(it.message) }
            .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_move_folder_failed) }
    }

    fun archivePhotoToFolder(photo: PhotoEntity, relativePath: String) = viewModelScope.launch {
        handledPhotoActionMediaIds += photo.mediaStoreId
        invalidatePhotoDeckLoads()
        runCatching {
            val result = repository.movePhotoToFolder(photo, relativePath)
            if (!result.success) error(result.message)
            val operationId = repository.handlePhotoAction(photo, SwipeAction.Keep)
            result.message to operationId
        }.onSuccess { (message, operationId) ->
            _photoDeck.value = _photoDeck.value.filterNot { it.mediaStoreId == photo.mediaStoreId }
            _photoSessionActionCount.value += 1
            _lastPhotoAction.value = SwipeAction.Keep
            rememberPhotoSessionAction(
                photo = photo,
                action = SwipeAction.Keep,
                operationId = CompletableDeferred(operationId),
                exitTargetX = 980f,
                exitTargetY = 0f,
                undoPersistedAction = { id ->
                    repository.undoOperation(id)
                    repository.movePhotoToFolder(photo, photo.relativePath ?: photo.folderPath)
                },
            )
            _message.value = dynamicUiText(message)
            refillPhotoDeckIfNeeded()
        }.onFailure {
            handledPhotoActionMediaIds -= photo.mediaStoreId
            _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_archive_failed)
        }
    }

    fun archiveVideoToFolder(video: VideoEntity, relativePath: String) = viewModelScope.launch {
        handledVideoActionMediaIds += video.mediaStoreId
        invalidateVideoDeckLoads()
        runCatching {
            val result = repository.moveVideoToFolder(video, relativePath)
            if (!result.success) error(result.message)
            repository.handleVideoAction(video, SwipeAction.Keep)
            result.message
        }.onSuccess { message ->
            val wasPending = removePendingVideoKeep(video)
            _videoDeck.value = _videoDeck.value.filterNot { it.mediaStoreId == video.mediaStoreId }
            if (!wasPending) _videoSessionActionCount.value += 1
            _lastVideoAction.value = SwipeAction.Keep
            _message.value = dynamicUiText(message)
            refillVideoDeckIfNeeded()
        }.onFailure {
            handledVideoActionMediaIds -= video.mediaStoreId
            _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_archive_failed)
        }
    }

    fun seedSimilarGroups(limit: Int = 3600) {
        if (similarDetectionJob?.isActive == true) {
            _message.value = uiText(R.string.message_similar_detection_background)
            return
        }
        similarDetectionJob = viewModelScope.launch {
            val totalHint = dashboard.value.photoCount.takeIf { it > 0 } ?: limit
            _similarDetectionState.value = SimilarDetectionUiState(
                running = true,
                progress = 0.01f,
                stage = uiText(R.string.similar_stage_prepare_library),
                processedHint = 0,
                totalHint = totalHint,
                lastResultCount = _similarDetectionState.value.lastResultCount,
            )
            try {
                if (dashboard.value.photoCount == 0) {
                    _similarDetectionState.value = _similarDetectionState.value.copy(stage = uiText(R.string.similar_stage_syncing_before_first_run))
                    runCatching { repository.refreshMediaLibrary() }
                }
                runCatching {
                    repository.generateSimilarGroups(limit = limit) { processed, total, stage ->
                        val safeTotal = total.coerceAtLeast(1)
                        val progress = (processed.toFloat() / safeTotal.toFloat()).coerceIn(0f, 0.96f)
                        _similarDetectionState.value = _similarDetectionState.value.copy(
                            running = true,
                            progress = if (processed >= total && total > 0) 0.96f else progress.coerceAtLeast(0.02f),
                            stage = dynamicUiText(stage),
                            processedHint = processed.coerceAtLeast(0),
                            totalHint = total.coerceAtLeast(1),
                        )
                    }
                }
                    .onSuccess { count ->
                        val total = _similarDetectionState.value.totalHint.coerceAtLeast(_similarDetectionState.value.processedHint)
                        _similarDetectionState.value = SimilarDetectionUiState(
                            running = false,
                            progress = 1f,
                            stage = if (count > 0) uiText(R.string.similar_stage_done_with_groups) else uiText(R.string.similar_stage_done_empty),
                            processedHint = total,
                            totalHint = total,
                            lastResultCount = count,
                        )
                        _message.value = if (count > 0) UiText.Plural(R.plurals.similar_group_count, count) else uiText(R.string.message_similar_detection_no_result)
                    }
                    .onFailure { throwable ->
                        _similarDetectionState.value = _similarDetectionState.value.copy(
                            running = false,
                            stage = throwable.message?.let(::dynamicUiText) ?: uiText(R.string.similar_stage_failed_retry),
                        )
                        _message.value = throwable.message?.let(::dynamicUiText) ?: uiText(R.string.message_similar_detection_failed)
                    }
            } finally {
                if (_similarDetectionState.value.running) {
                    _similarDetectionState.value = _similarDetectionState.value.copy(running = false)
                }
            }
        }
    }

    fun continueSimilarDetection() {
        val nextLimit = when {
            dashboard.value.photoCount > 0 -> dashboard.value.photoCount.coerceAtMost(8000)
            else -> 4800
        }
        seedSimilarGroups(limit = nextLimit)
    }

    fun restoreTrash(id: Long) = viewModelScope.launch {
        val item = trashItems.value.firstOrNull { it.id == id }
        runCatching { repository.restoreTrashItem(id) }
            .onSuccess {
                item?.let { restoreTrashItemsToDeck(listOf(it)) }
                _message.value = uiText(R.string.message_restore_success)
            }
            .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_restore_failed) }
    }

    fun confirmTrashDeleted(id: Long) = confirmTrashDeleted(listOf(id))

    fun confirmTrashDeleted(ids: List<Long>) = viewModelScope.launch {
        val distinctIds = ids.distinct()
        runCatching { distinctIds.forEach { repository.markTrashItemDeleted(it) } }
            .onSuccess {
                _message.value = if (distinctIds.size > 1) {
                    uiText(R.string.message_trash_all_processed)
                } else {
                    uiText(R.string.message_permanent_delete_success)
                }
            }
            .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_permanent_delete_failed) }
    }

    fun permanentlyDeleteTrash(id: Long) = viewModelScope.launch {
        runCatching { repository.permanentlyDeleteTrashItem(id) }
            .onSuccess { _message.value = uiText(R.string.message_permanent_delete_success) }
            .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_permanent_delete_failed) }
    }

    fun permanentlyDeleteAllTrash() = viewModelScope.launch {
        runCatching { repository.permanentlyDeleteAllTrash() }
            .onSuccess { _message.value = uiText(R.string.message_trash_all_processed) }
            .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_batch_delete_failed) }
    }

    fun restoreAllTrash() = viewModelScope.launch {
        val items = trashItems.value
        runCatching { repository.restoreAllTrash() }
            .onSuccess {
                restoreTrashItemsToDeck(items)
                _message.value = uiText(R.string.message_trash_all_restored)
            }
            .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_batch_restore_failed) }
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
            invalidateVideoDeckLoads()
            loadVideoDeck()
        }
    }

    fun undoLastAction() = viewModelScope.launch {
        runCatching { repository.undoLastAction() }
            .onSuccess { ok ->
                if (ok) {
                    pendingPhotoActionMediaIds.clear()
                    handledPhotoActionMediaIds.clear()
                    clearPhotoActionHistory()
                    handledVideoActionMediaIds.clear()
                    invalidatePhotoDeckLoads()
                    invalidateVideoDeckLoads()
                }
                _message.value = if (ok) uiText(R.string.message_undo_success) else uiText(R.string.message_undo_empty)
            }
            .onFailure { _message.value = it.message?.let(::dynamicUiText) ?: uiText(R.string.message_undo_failed) }
        loadPhotoDeck()
        loadVideoDeck()
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
        // Kept for old settings compatibility. The organizer now uses a continuous rolling buffer,
        // so this no longer limits a cleaning round.
        loadPhotoDeck()
        loadVideoDeck()
    }

    fun setAutoMoveOnKeepFavorite(value: Boolean) = viewModelScope.launch { settingsRepository.setAutoMoveOnKeepFavorite(value) }

    fun setSimilarDetection(value: Boolean) = viewModelScope.launch {
        settingsRepository.setSimilarDetection(value)
        if (value) seedSimilarGroups()
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

    fun cycleAppVisualStyle() = viewModelScope.launch {
        val next = if (settings.value.appVisualStyle == AppVisualStyle.LIQUID_GLASS) {
            AppVisualStyle.IMMERSIVE_PHOTO
        } else {
            AppVisualStyle.LIQUID_GLASS
        }
        settingsRepository.setAppVisualStyle(next)
    }

    fun setAppVisualStyle(value: AppVisualStyle) = viewModelScope.launch {
        settingsRepository.setAppVisualStyle(value)
    }

    fun cycleThemeMode() = viewModelScope.launch {
        val values = ThemeMode.entries
        settingsRepository.setThemeMode(values[(values.indexOf(settings.value.themeMode) + 1) % values.size])
    }

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(value)
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
        batchSize = photoBatchSize,
        randomSeed = photoRandomSeed,
    )

    private fun AppSettings.toVideoCleaningScope(): CleaningScope = CleaningScope(
        dateMode = videoDateMode,
        folderPaths = videoFolderPath.takeIf { it.isNotBlank() }?.let { setOf(it) } ?: emptySet(),
        sortOrder = videoSortOrder,
        todayInHistory = videoDateMode == "today_history",
        batchSize = videoBatchSize,
        randomSeed = videoRandomSeed,
    )
}
