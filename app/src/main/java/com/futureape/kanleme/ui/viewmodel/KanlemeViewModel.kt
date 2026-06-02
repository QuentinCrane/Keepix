package com.futureape.kanleme.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.futureape.kanleme.data.local.PhotoEntity
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
import com.futureape.kanleme.data.settings.DeleteMode
import com.futureape.kanleme.data.settings.FolderDisplayMode
import com.futureape.kanleme.data.settings.GestureDirection
import com.futureape.kanleme.data.settings.HapticLevel
import com.futureape.kanleme.data.settings.PhotoCleanMode
import com.futureape.kanleme.data.settings.SwipeSensitivity
import com.futureape.kanleme.data.settings.ThemeMode
import com.futureape.kanleme.data.settings.VideoDisplayMode
import com.futureape.kanleme.data.settings.nextAccentColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SimilarDetectionUiState(
    val running: Boolean = false,
    val progress: Float = 0f,
    val stage: String = "未开始",
    val processedHint: Int = 0,
    val totalHint: Int = 0,
    val lastResultCount: Int = 0,
)

@HiltViewModel
class KanlemeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val settingsRepository: AppSettingsRepository,
) : ViewModel() {
    private companion object {
        const val CONTINUOUS_PHOTO_DECK_SIZE = 1200
        const val CONTINUOUS_VIDEO_DECK_SIZE = 600
        const val PHOTO_PREFETCH_THRESHOLD = 320
        const val VIDEO_PREFETCH_THRESHOLD = 160
    }
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val dashboard: StateFlow<DashboardStats> = repository.observeDashboard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardStats())

    val photoTypeStats: StateFlow<PhotoTypeStats> = repository.observePhotoTypeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PhotoTypeStats())

    val recentPhotos = repository.observeRecentPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val timelinePhotos = repository.observeTimelinePhotos()
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

    private val _photoDeckPreparing = MutableStateFlow(false)
    val photoDeckPreparing = _photoDeckPreparing.asStateFlow()

    private val _videoDeckPreparing = MutableStateFlow(false)
    val videoDeckPreparing = _videoDeckPreparing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private var lastAutoRefreshAccessKey: String? = null
    private var photoDeckRefilling = false
    private var videoDeckRefilling = false
    private var lastGeneratedRandomSeed = 1L
    private var similarDetectionJob: Job? = null

    init {
        viewModelScope.launch {
            val persisted = settingsRepository.settings.first()
            _photoScope.value = persisted.toPhotoCleaningScope()
            _videoScope.value = persisted.toVideoCleaningScope()
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
                    _message.value = prefix + "已同步 " + p + " 张照片、" + v + " 个视频"
                }
                loadPhotoDeck()
                loadVideoDeck()
            }
            .onFailure { _message.value = it.message ?: "媒体库同步失败，请检查相册权限" }
    }

    fun clearMessage() { _message.value = null }

    private fun nextRandomSeed(): Long {
        val mixed = (System.currentTimeMillis() * 1_000_003L) xor System.nanoTime() xor ((lastGeneratedRandomSeed + 0x6A09E667F3BCC909L) * 31L)
        val positive = (mixed and Long.MAX_VALUE).coerceAtLeast(2L)
        val next = if (positive == lastGeneratedRandomSeed) positive + 1L else positive
        lastGeneratedRandomSeed = next
        return next
    }

    private fun ensureRandomSeed(scope: CleaningScope): CleaningScope =
        if (scope.sortOrder == "random" && scope.randomSeed <= 1L) scope.copy(randomSeed = nextRandomSeed()) else scope

    fun loadPhotoDeck(scope: CleaningScope = _photoScope.value) = viewModelScope.launch {
        val requestedScope = ensureRandomSeed(scope)
        _photoScope.value = requestedScope
        val showPreparing = _photoDeck.value.isEmpty()
        if (showPreparing) _photoDeckPreparing.value = true
        try {
            _photoDeck.value = repository
                .loadPhotoDeck(requestedScope.copy(batchSize = CONTINUOUS_PHOTO_DECK_SIZE))
                .distinctBy { it.mediaStoreId }
        } finally {
            if (showPreparing) _photoDeckPreparing.value = false
        }
    }

    fun loadVideoDeck(scope: CleaningScope = _videoScope.value) = viewModelScope.launch {
        val requestedScope = ensureRandomSeed(scope)
        _videoScope.value = requestedScope
        val showPreparing = _videoDeck.value.isEmpty()
        if (showPreparing) _videoDeckPreparing.value = true
        try {
            _videoDeck.value = repository
                .loadVideoDeck(requestedScope.copy(batchSize = CONTINUOUS_VIDEO_DECK_SIZE))
                .distinctBy { it.mediaStoreId }
        } finally {
            if (showPreparing) _videoDeckPreparing.value = false
        }
    }

    fun startPhotoCleaningSession() {
        val base = _photoScope.value
        val session = ensureRandomSeed(base.copy(sortOrder = if (base.sortOrder.isBlank()) "random" else base.sortOrder)).let {
            if (it.sortOrder == "random") it.copy(randomSeed = nextRandomSeed()) else it
        }
        _photoScope.value = session
        _photoDeckPreparing.value = true
        _photoDeck.value = emptyList()
        viewModelScope.launch {
            if (session.sortOrder == "random") settingsRepository.setPhotoSortOrderWithSeed("random", session.randomSeed)
            try {
                _photoDeck.value = repository
                    .loadPhotoDeck(session.copy(batchSize = CONTINUOUS_PHOTO_DECK_SIZE))
                    .distinctBy { it.mediaStoreId }
            } finally {
                _photoDeckPreparing.value = false
            }
        }
    }

    fun startVideoCleaningSession() {
        val base = _videoScope.value
        val session = ensureRandomSeed(base.copy(sortOrder = if (base.sortOrder.isBlank()) "random" else base.sortOrder)).let {
            if (it.sortOrder == "random") it.copy(randomSeed = nextRandomSeed()) else it
        }
        _videoScope.value = session
        _videoDeckPreparing.value = true
        _videoDeck.value = emptyList()
        viewModelScope.launch {
            if (session.sortOrder == "random") settingsRepository.setVideoSortOrderWithSeed("random", session.randomSeed)
            try {
                _videoDeck.value = repository
                    .loadVideoDeck(session.copy(batchSize = CONTINUOUS_VIDEO_DECK_SIZE))
                    .distinctBy { it.mediaStoreId }
            } finally {
                _videoDeckPreparing.value = false
            }
        }
    }


    fun reshufflePhotoCleaningSession() {
        val session = _photoScope.value.copy(sortOrder = "random", randomSeed = nextRandomSeed())
        _photoScope.value = session
        _photoDeckPreparing.value = true
        _photoDeck.value = emptyList()
        viewModelScope.launch {
            settingsRepository.setPhotoSortOrderWithSeed("random", session.randomSeed)
            try {
                _photoDeck.value = repository
                    .loadPhotoDeck(session.copy(batchSize = CONTINUOUS_PHOTO_DECK_SIZE))
                    .distinctBy { it.mediaStoreId }
            } finally {
                _photoDeckPreparing.value = false
            }
        }
    }

    fun reshuffleVideoCleaningSession() {
        val session = _videoScope.value.copy(sortOrder = "random", randomSeed = nextRandomSeed())
        _videoScope.value = session
        _videoDeckPreparing.value = true
        _videoDeck.value = emptyList()
        viewModelScope.launch {
            settingsRepository.setVideoSortOrderWithSeed("random", session.randomSeed)
            try {
                _videoDeck.value = repository
                    .loadVideoDeck(session.copy(batchSize = CONTINUOUS_VIDEO_DECK_SIZE))
                    .distinctBy { it.mediaStoreId }
            } finally {
                _videoDeckPreparing.value = false
            }
        }
    }

    private suspend fun refillPhotoDeckIfNeeded(force: Boolean = false) {
        if (!force && _photoDeck.value.size > PHOTO_PREFETCH_THRESHOLD) return
        if (photoDeckRefilling) return
        photoDeckRefilling = true
        try {
            val fresh = repository.loadPhotoDeck(_photoScope.value.copy(batchSize = CONTINUOUS_PHOTO_DECK_SIZE))
            val merged = (_photoDeck.value + fresh)
                .distinctBy { it.mediaStoreId }
                .take(CONTINUOUS_PHOTO_DECK_SIZE)
            _photoDeck.value = merged
        } finally {
            photoDeckRefilling = false
        }
    }

    private suspend fun refillVideoDeckIfNeeded(force: Boolean = false) {
        if (!force && _videoDeck.value.size > VIDEO_PREFETCH_THRESHOLD) return
        if (videoDeckRefilling) return
        videoDeckRefilling = true
        try {
            val fresh = repository.loadVideoDeck(_videoScope.value.copy(batchSize = CONTINUOUS_VIDEO_DECK_SIZE))
            val merged = (_videoDeck.value + fresh)
                .distinctBy { it.mediaStoreId }
                .take(CONTINUOUS_VIDEO_DECK_SIZE)
            _videoDeck.value = merged
        } finally {
            videoDeckRefilling = false
        }
    }

    fun setHomeMediaTab(tab: String) = viewModelScope.launch {
        settingsRepository.setHomeMediaTab(tab)
    }

    fun setPhotoTypeFilter(type: String) = viewModelScope.launch {
        settingsRepository.setPhotoMediaType(type)
        loadPhotoDeck(_photoScope.value.copy(mediaType = type))
    }

    fun setPhotoDateMode(mode: String) = viewModelScope.launch {
        settingsRepository.setPhotoDateMode(mode)
        loadPhotoDeck(_photoScope.value.copy(dateMode = mode, todayInHistory = mode == "today_history"))
    }

    fun setPhotoFolder(path: String?) = viewModelScope.launch {
        settingsRepository.setPhotoFolderPath(path)
        loadPhotoDeck(_photoScope.value.copy(folderPaths = path?.let { setOf(it) } ?: emptySet()))
    }

    fun togglePhotoRandom() = viewModelScope.launch {
        val next = if (_photoScope.value.sortOrder == "random") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _photoScope.value.randomSeed
        settingsRepository.setPhotoSortOrderWithSeed(next, seed)
        loadPhotoDeck(_photoScope.value.copy(sortOrder = next, randomSeed = seed))
    }

    fun setVideoFolder(path: String?) = viewModelScope.launch {
        settingsRepository.setVideoFolderPath(path)
        loadVideoDeck(_videoScope.value.copy(folderPaths = path?.let { setOf(it) } ?: emptySet()))
    }

    fun toggleVideoRandom() = viewModelScope.launch {
        val next = if (_videoScope.value.sortOrder == "random") "newest" else "random"
        val seed = if (next == "random") nextRandomSeed() else _videoScope.value.randomSeed
        settingsRepository.setVideoSortOrderWithSeed(next, seed)
        loadVideoDeck(_videoScope.value.copy(sortOrder = next, randomSeed = seed))
    }

    fun onPhotoAction(photo: PhotoEntity, action: SwipeAction) = viewModelScope.launch {
        repository.handlePhotoAction(photo, action)
        _photoDeck.value = _photoDeck.value.filterNot { it.mediaStoreId == photo.mediaStoreId }
        refillPhotoDeckIfNeeded()
    }

    fun onPhotoActionWithOptionalMove(photo: PhotoEntity, action: SwipeAction, targetRelativePath: String?) = viewModelScope.launch {
        val s = settingsRepository.settings.first()
        if (targetRelativePath != null && s.autoMoveOnKeepFavorite && action in setOf(SwipeAction.Keep, SwipeAction.Favorite)) {
            repository.movePhotoToFolder(photo, targetRelativePath)
        }
        repository.handlePhotoAction(photo, action)
        _photoDeck.value = _photoDeck.value.filterNot { it.mediaStoreId == photo.mediaStoreId }
        refillPhotoDeckIfNeeded()
    }

    fun onVideoAction(video: VideoEntity, action: SwipeAction) = viewModelScope.launch {
        repository.handleVideoAction(video, action)
        _videoDeck.value = _videoDeck.value.filterNot { it.mediaStoreId == video.mediaStoreId }
        refillVideoDeckIfNeeded()
    }

    fun movePhotoToFolder(photo: PhotoEntity, relativePath: String) = viewModelScope.launch {
        runCatching { repository.movePhotoToFolder(photo, relativePath) }
            .onSuccess { _message.value = it.message }
            .onFailure { _message.value = it.message ?: "移动文件夹失败" }
    }

    fun seedSimilarGroups(limit: Int = 3600) {
        if (similarDetectionJob?.isActive == true) {
            _message.value = "相似照片检测正在后台继续，返回此页面会自动恢复进度"
            return
        }
        similarDetectionJob = viewModelScope.launch {
            val totalHint = dashboard.value.photoCount.takeIf { it > 0 } ?: limit
            _similarDetectionState.value = SimilarDetectionUiState(
                running = true,
                progress = 0.01f,
                stage = "准备读取媒体库",
                processedHint = 0,
                totalHint = totalHint,
                lastResultCount = _similarDetectionState.value.lastResultCount,
            )
            try {
                if (dashboard.value.photoCount == 0) {
                    _similarDetectionState.value = _similarDetectionState.value.copy(stage = "首次检测前正在同步媒体库")
                    runCatching { repository.refreshMediaLibrary() }
                }
                runCatching {
                    repository.generateSimilarGroups(limit = limit) { processed, total, stage ->
                        val safeTotal = total.coerceAtLeast(1)
                        val progress = (processed.toFloat() / safeTotal.toFloat()).coerceIn(0f, 0.96f)
                        _similarDetectionState.value = _similarDetectionState.value.copy(
                            running = true,
                            progress = if (processed >= total && total > 0) 0.96f else progress.coerceAtLeast(0.02f),
                            stage = stage,
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
                            stage = if (count > 0) "检测完成，已生成候选分组" else "检测完成，未发现明显相似照片",
                            processedHint = total,
                            totalHint = total,
                            lastResultCount = count,
                        )
                        _message.value = if (count > 0) "已生成 $count 个相似/模糊/连拍分组" else "没有检测到明显相似照片，可稍后扩大相册权限或重新同步媒体库"
                    }
                    .onFailure { throwable ->
                        _similarDetectionState.value = _similarDetectionState.value.copy(
                            running = false,
                            stage = throwable.message ?: "相似照片检测失败，可点击继续检测重试",
                        )
                        _message.value = throwable.message ?: "相似照片检测失败"
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
        runCatching { repository.restoreTrashItem(id) }
            .onSuccess { _message.value = "已恢复" }
            .onFailure { _message.value = it.message ?: "恢复失败" }
    }

    fun permanentlyDeleteTrash(id: Long) = viewModelScope.launch {
        runCatching { repository.permanentlyDeleteTrashItem(id) }
            .onSuccess { _message.value = "已永久删除" }
            .onFailure { _message.value = it.message ?: "永久删除失败" }
    }

    fun permanentlyDeleteAllTrash() = viewModelScope.launch {
        runCatching { repository.permanentlyDeleteAllTrash() }
            .onSuccess { _message.value = "已处理全部回收站项目" }
            .onFailure { _message.value = it.message ?: "批量删除失败" }
    }

    fun restoreAllTrash() = viewModelScope.launch {
        runCatching { repository.restoreAllTrash() }
            .onSuccess { _message.value = "已恢复全部回收站项目" }
            .onFailure { _message.value = it.message ?: "批量恢复失败" }
    }

    fun undoLastAction() = viewModelScope.launch {
        runCatching { repository.undoLastAction() }
            .onSuccess { ok -> _message.value = if (ok) "已撤销上一步操作" else "没有可撤销操作" }
            .onFailure { _message.value = it.message ?: "撤销失败" }
        loadPhotoDeck(); loadVideoDeck()
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

    fun cycleGestureDirection() = viewModelScope.launch {
        val next = if (settings.value.gestureDirection == GestureDirection.DEFAULT) GestureDirection.REVERSE_VERTICAL else GestureDirection.DEFAULT
        settingsRepository.setGestureDirection(next)
    }

    fun setQuickActionButtons(value: Boolean) = viewModelScope.launch { settingsRepository.setQuickActionButtons(value) }
    fun setSwapShareAndUndo(value: Boolean) = viewModelScope.launch { settingsRepository.setSwapShareAndUndo(value) }
    fun setSwipeSound(value: Boolean) = viewModelScope.launch { settingsRepository.setSwipeSound(value) }
    fun setVideoDefaultMuted(value: Boolean) = viewModelScope.launch { settingsRepository.setVideoDefaultMuted(value) }

    fun cycleVideoDisplayMode() = viewModelScope.launch {
        val values = VideoDisplayMode.entries
        settingsRepository.setVideoDisplayMode(values[(values.indexOf(settings.value.videoDisplayMode) + 1) % values.size])
    }

    fun cycleHapticLevel() = viewModelScope.launch {
        val values = HapticLevel.entries
        settingsRepository.setHapticLevel(values[(values.indexOf(settings.value.hapticLevel) + 1) % values.size])
    }

    fun cycleThemeMode() = viewModelScope.launch {
        val values = ThemeMode.entries
        settingsRepository.setThemeMode(values[(values.indexOf(settings.value.themeMode) + 1) % values.size])
    }

    fun cycleAccentColor() = viewModelScope.launch { settingsRepository.setAccentColor(nextAccentColor(settings.value.accentColor)) }

    fun cycleFolderDisplay() = viewModelScope.launch {
        val next = if (settings.value.folderDisplay == FolderDisplayMode.SINGLE_LINE) FolderDisplayMode.MULTI_LINE else FolderDisplayMode.SINGLE_LINE
        settingsRepository.setFolderDisplay(next)
    }

    fun setImmersiveBackground(value: Boolean) = viewModelScope.launch { settingsRepository.setImmersiveBackground(value) }

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

    fun markPhotoGuideShown() = viewModelScope.launch { settingsRepository.setPhotoGuideShown(true) }
    fun markVideoGuideShown() = viewModelScope.launch { settingsRepository.setVideoGuideShown(true) }
    fun markOnboardingShown() = viewModelScope.launch { settingsRepository.setOnboardingShown(true) }

    fun replayPositionGuides() = viewModelScope.launch {
        settingsRepository.setPhotoGuideShown(false)
        settingsRepository.setVideoGuideShown(false)
        _message.value = "已重置定位式教程，进入照片/视频整理页会再次显示"
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
        val next = if (settings.value.photoCleanMode == PhotoCleanMode.NORMAL) PhotoCleanMode.CONSERVATIVE else PhotoCleanMode.NORMAL
        settingsRepository.setPhotoCleanMode(next)
    }

    private fun AppSettings.toPhotoCleaningScope(): CleaningScope = CleaningScope(
        dateMode = photoDateMode,
        folderPaths = photoFolderPath.takeIf { it.isNotBlank() }?.let { setOf(it) } ?: emptySet(),
        mediaType = photoMediaType,
        sortOrder = photoSortOrder,
        todayInHistory = photoDateMode == "today_history",
        randomSeed = photoRandomSeed,
    )

    private fun AppSettings.toVideoCleaningScope(): CleaningScope = CleaningScope(
        folderPaths = videoFolderPath.takeIf { it.isNotBlank() }?.let { setOf(it) } ?: emptySet(),
        sortOrder = videoSortOrder,
        randomSeed = videoRandomSeed,
    )
}
