package com.futureape.kanleme.ui.viewmodel

import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.AdvancedPhoto
import com.futureape.kanleme.data.media.AdvancedPhotoFilter
import com.futureape.kanleme.data.media.PhotoFolder
import com.futureape.kanleme.data.media.PhotoFolderKey
import com.futureape.kanleme.data.media.photoFolderInScope
import com.futureape.kanleme.data.repository.AppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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

data class AdvancedPhotoScanState(
    val photos: List<AdvancedPhoto> = emptyList(),
    val folders: List<PhotoFolder> = emptyList(),
    val selectedFolders: Set<PhotoFolderKey>? = null,
    val loadingFolders: Boolean = false,
    val scanning: Boolean = false,
    val scannedCount: Int = 0,
    val hasScanned: Boolean = false,
    val errorRes: Int? = null,
    val busyUris: Set<String> = emptySet(),
    val includeSubfolders: Boolean = true,
)

data class AdvancedPhotoResults(
    val photos: List<AdvancedPhoto> = emptyList(),
    val totalCount: Int = 0,
    val totalBytes: Long = 0,
    val filter: AdvancedPhotoFilter = AdvancedPhotoFilter(),
)

/** Owned by KanlemeViewModel; independent of cleaning decks and their processing state. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AdvancedPhotoLibrary(private val scope: CoroutineScope, private val repository: AppRepository) {
    private val mutableState = MutableStateFlow(AdvancedPhotoScanState())
    val state = mutableState.asStateFlow()
    private val mutableFilter = MutableStateFlow(AdvancedPhotoFilter())
    val filter = mutableFilter.asStateFlow()
    private var scanJob: Job? = null
    private var folderJob: Job? = null
    private var folderPickerPhoto: AdvancedPhoto? = null
    private var scopePickerIncludeSubfolders = true
    @Volatile private var scanGeneration = 0L

    val results = combine(state, filter.debounce(120)) { state, filter -> state.photos to filter }
        .distinctUntilChanged()
        .mapLatest { (photos, filter) ->
            withContext(Dispatchers.Default) {
                val jobContext = currentCoroutineContext()
                val filtered = filter.apply(photos, System.currentTimeMillis()) { jobContext.ensureActive() }
                AdvancedPhotoResults(filtered, photos.size, filtered.sumOf { it.sizeBytes }, filter)
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 0, replayExpirationMillis = 0), AdvancedPhotoResults())

    fun updateFilter(filter: AdvancedPhotoFilter) { mutableFilter.value = filter }

    fun prepareFolderPicker(photo: AdvancedPhoto) { folderPickerPhoto = photo }
    fun consumeFolderPickerPhoto(): AdvancedPhoto? = folderPickerPhoto.also { folderPickerPhoto = null }
    fun prepareScopeFolderPicker(includeSubfolders: Boolean) { scopePickerIncludeSubfolders = includeSubfolders }
    fun selectSystemFolder(folder: PhotoFolderKey) { selectFolders(setOf(folder), scopePickerIncludeSubfolders) }

    fun beginAction(uri: String): Boolean {
        if (uri in state.value.busyUris) return false
        cancelScan()
        mutableState.update { it.copy(busyUris = it.busyUris + uri) }
        return true
    }

    fun finishAction(uri: String) { mutableState.update { it.copy(busyUris = it.busyUris - uri) } }

    fun removePhoto(uri: String) {
        mutableState.update { it.copy(photos = it.photos.filterNot { photo -> photo.uri == uri }) }
    }

    fun replacePhoto(uri: String, photo: AdvancedPhoto) {
        mutableState.update { state ->
            val inScope = state.selectedFolders == null || photoFolderInScope(photo.folder, state.selectedFolders, state.includeSubfolders)
            state.copy(photos = state.photos.mapNotNull { old ->
                if (old.uri != uri) old else if (inScope) photo else null
            })
        }
        loadFolders()
    }

    fun loadFolders() {
        folderJob?.cancel()
        folderJob = scope.launch {
            mutableState.update { it.copy(loadingFolders = true, errorRes = null) }
            try {
                val folders = repository.advancedPhotoFolders()
                mutableState.update { it.copy(folders = folders, loadingFolders = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(loadingFolders = false, errorRes = errorResource(error)) }
            }
        }
    }

    fun selectFolders(folders: Set<PhotoFolderKey>?, includeSubfolders: Boolean = state.value.includeSubfolders) {
        require(folders == null || folders.isNotEmpty())
        if (folders == state.value.selectedFolders && includeSubfolders == state.value.includeSubfolders) return
        cancelScan()
        mutableState.update { it.copy(selectedFolders = folders, includeSubfolders = includeSubfolders, photos = emptyList(), hasScanned = false,
            scannedCount = 0, errorRes = null) }
    }

    fun scan() {
        cancelScan()
        val generation = scanGeneration
        val selected = state.value.selectedFolders
        val includeSubfolders = state.value.includeSubfolders
        scanJob = scope.launch {
            mutableState.update { it.copy(scanning = true, scannedCount = 0, errorRes = null) }
            try {
                val photos = repository.scanAdvancedPhotos(selected, includeSubfolders) { count ->
                    if (generation == scanGeneration) mutableState.update { it.copy(scannedCount = count) }
                }
                if (generation != scanGeneration) return@launch
                mutableState.update { it.copy(photos = photos, hasScanned = true, scanning = false, scannedCount = photos.size) }
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

    fun invalidateAccess() {
        cancelScan()
        folderJob?.cancel()
        mutableState.value = AdvancedPhotoScanState(selectedFolders = state.value.selectedFolders, includeSubfolders = state.value.includeSubfolders)
        loadFolders()
    }

    fun leave(clearResults: Boolean = true) {
        cancelScan()
        folderJob?.cancel()
        if (clearResults) folderPickerPhoto = null
        mutableState.update {
            if (clearResults) AdvancedPhotoScanState(selectedFolders = it.selectedFolders, includeSubfolders = it.includeSubfolders)
            else it.copy(loadingFolders = false)
        }
    }

    private fun errorResource(error: Exception): Int =
        if (error is SecurityException) R.string.advanced_photo_permission else R.string.advanced_photo_scan_error
}
