package com.futureape.kanleme.ui.viewmodel

import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.AdvancedVideo
import com.futureape.kanleme.data.media.AdvancedVideoFilter
import com.futureape.kanleme.data.media.VideoFolder
import com.futureape.kanleme.data.media.VideoFolderKey
import com.futureape.kanleme.data.repository.AppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AdvancedVideoScanState(
    val videos: List<AdvancedVideo> = emptyList(),
    val folders: List<VideoFolder> = emptyList(),
    val selectedFolders: Set<VideoFolderKey>? = null,
    val loadingFolders: Boolean = false,
    val scanning: Boolean = false,
    val scannedCount: Int = 0,
    val hasScanned: Boolean = false,
    val errorRes: Int? = null,
)

data class AdvancedVideoResults(
    val videos: List<AdvancedVideo> = emptyList(),
    val totalCount: Int = 0,
    val totalBytes: Long = 0,
    val filter: AdvancedVideoFilter = AdvancedVideoFilter(),
)

/** Owned by KanlemeViewModel; independent of cleaning decks and their processing state. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AdvancedVideoLibrary(private val scope: CoroutineScope, private val repository: AppRepository) {
    private val mutableState = MutableStateFlow(AdvancedVideoScanState())
    val state = mutableState.asStateFlow()
    private val mutableFilter = MutableStateFlow(AdvancedVideoFilter())
    val filter = mutableFilter.asStateFlow()
    private var scanJob: Job? = null
    private var folderJob: Job? = null
    @Volatile private var scanGeneration = 0L

    val results = combine(state, filter.debounce(120)) { state, filter -> state.videos to filter }
        .distinctUntilChanged() // Scan progress and folder-list changes must not re-sort old results.
        .mapLatest { (videos, filter) ->
            withContext(Dispatchers.Default) {
                val jobContext = currentCoroutineContext()
                val filtered = filter.apply(videos, System.currentTimeMillis()) { jobContext.ensureActive() }
                AdvancedVideoResults(filtered, videos.size, filtered.sumOf { it.sizeBytes }, filter)
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0), AdvancedVideoResults())

    fun updateFilter(filter: AdvancedVideoFilter) { mutableFilter.value = filter }

    fun loadFolders() {
        folderJob?.cancel()
        folderJob = scope.launch {
            mutableState.update { it.copy(loadingFolders = true, errorRes = null) }
            try {
                val folders = repository.advancedVideoFolders()
                mutableState.update { it.copy(folders = folders, loadingFolders = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(loadingFolders = false, errorRes = errorResource(error)) }
            }
        }
    }

    fun selectFolders(folders: Set<VideoFolderKey>?) {
        require(folders == null || folders.isNotEmpty())
        if (folders == state.value.selectedFolders) return
        cancelScan()
        mutableState.update { it.copy(selectedFolders = folders, videos = emptyList(), hasScanned = false,
            scannedCount = 0, errorRes = null) }
    }

    fun scan() {
        cancelScan()
        val generation = scanGeneration
        val selected = state.value.selectedFolders
        scanJob = scope.launch {
            mutableState.update { it.copy(scanning = true, scannedCount = 0, errorRes = null) }
            try {
                val videos = repository.scanAdvancedVideos(selected) { count ->
                    if (generation == scanGeneration) mutableState.update { it.copy(scannedCount = count) }
                }
                if (generation != scanGeneration) return@launch
                mutableState.update { it.copy(videos = videos, hasScanned = true, scanning = false,
                    scannedCount = videos.size) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == scanGeneration) {
                    mutableState.update { it.copy(scanning = false, errorRes = errorResource(error)) }
                }
            }
        }
    }

    fun cancelScan() {
        scanGeneration++
        scanJob?.cancel()
        scanJob = null
        mutableState.update { it.copy(scanning = false) }
    }

    /** Permission sets can change on resume without changing Android's permission booleans. */
    fun invalidateAccess() {
        cancelScan()
        folderJob?.cancel()
        mutableState.value = AdvancedVideoScanState(selectedFolders = state.value.selectedFolders)
        loadFolders()
    }

    fun leave(clearResults: Boolean = true) {
        cancelScan()
        folderJob?.cancel()
        mutableState.update {
            if (clearResults) AdvancedVideoScanState(selectedFolders = it.selectedFolders)
            else it.copy(loadingFolders = false)
        }
    }

    private fun errorResource(error: Exception): Int =
        if (error is SecurityException) R.string.advanced_video_permission else R.string.advanced_scan_error
}
