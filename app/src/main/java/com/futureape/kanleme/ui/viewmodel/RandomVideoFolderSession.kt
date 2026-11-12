package com.futureape.kanleme.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import com.futureape.kanleme.data.media.FolderPlaybackVideo
import com.futureape.kanleme.data.media.isLocalVideoFolderTree
import com.futureape.kanleme.data.media.scanFolderVideos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RandomFolderError { NOT_LOCAL, NO_ACCESS, READ_FAILED }

data class RandomVideoFolderState(
    val folder: Uri? = null,
    val folderName: String = "",
    val includeSubfolders: Boolean = true,
    val scanning: Boolean = false,
    val scannedCount: Int = 0,
    val hasScanned: Boolean = false,
    val videos: List<FolderPlaybackVideo> = emptyList(),
    val playerVisible: Boolean = false,
    val error: RandomFolderError? = null,
)

/** Session data survives configuration changes and is discarded on leaving the page. */
class RandomVideoFolderSession(private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(RandomVideoFolderState())
    val state = mutableState.asStateFlow()
    private var scanJob: Job? = null
    @Volatile private var generation = 0L

    fun setIncludeSubfolders(include: Boolean) {
        mutableState.update { it.copy(includeSubfolders = include) }
    }

    fun selectFolder(resolver: ContentResolver, folder: Uri) {
        if (!isLocalVideoFolderTree(folder)) {
            mutableState.update { it.copy(error = RandomFolderError.NOT_LOCAL) }
            return
        }
        startScan(resolver, folder)
    }

    fun rescan(resolver: ContentResolver) {
        state.value.folder?.let { startScan(resolver, it) }
    }

    private fun startScan(resolver: ContentResolver, folder: Uri) {
        cancelScan()
        val currentGeneration = generation
        val include = state.value.includeSubfolders
        mutableState.update {
            it.copy(folder = folder, folderName = "", scanning = true, scannedCount = 0,
                hasScanned = false, videos = emptyList(), playerVisible = false, error = null)
        }
        scanJob = scope.launch {
            try {
                val result = scanFolderVideos(resolver, folder, include) { count ->
                    mutableState.update {
                        if (generation == currentGeneration) it.copy(scannedCount = count) else it
                    }
                }
                val shuffled = withContext(Dispatchers.Default) { result.videos.shuffled() }
                if (generation == currentGeneration) mutableState.update {
                    it.copy(folderName = result.folderName, scanning = false, hasScanned = true,
                        scannedCount = result.videos.size, videos = shuffled,
                        playerVisible = result.videos.isNotEmpty())
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == currentGeneration) mutableState.update {
                    it.copy(scanning = false, error = if (error is SecurityException) RandomFolderError.NO_ACCESS else RandomFolderError.READ_FAILED)
                }
            }
        }
    }

    fun cancelScan() {
        generation++
        scanJob?.cancel()
        scanJob = null
        mutableState.update { it.copy(scanning = false) }
    }

    fun closePlayer() { mutableState.update { it.copy(playerVisible = false) } }

    fun openPlayer() {
        mutableState.update { if (it.videos.isNotEmpty() && !it.scanning) it.copy(playerVisible = true) else it }
    }

    fun leave() {
        cancelScan()
        mutableState.value = RandomVideoFolderState()
    }
}
