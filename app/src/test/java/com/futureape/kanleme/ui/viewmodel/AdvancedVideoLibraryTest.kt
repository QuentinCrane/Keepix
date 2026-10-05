package com.futureape.kanleme.ui.viewmodel

import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.AdvancedVideo
import com.futureape.kanleme.data.media.VideoFolder
import com.futureape.kanleme.data.media.VideoFolderKey
import com.futureape.kanleme.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class AdvancedVideoLibraryTest {
    private val folder = VideoFolderKey("external_primary", "Movies/")
    private val video = AdvancedVideo("content://media/external_primary/video/media/1", "sample.mp4", folder,
        100, 320, 180, 3000, 1, "video/mp4")

    @Suppress("UNCHECKED_CAST")
    private fun repository(scan: ((Int) -> Unit) -> List<AdvancedVideo>): AppRepository =
        Proxy.newProxyInstance(AppRepository::class.java.classLoader, arrayOf(AppRepository::class.java)) { _, method, args ->
            when (method.name) {
                "advancedVideoFolders" -> listOf(VideoFolder(folder, 1))
                "scanAdvancedVideos" -> scan(args[2] as (Int) -> Unit)
                else -> throw UnsupportedOperationException(method.name)
            }
        } as AppRepository

    @Test fun movedVideoLeavesSelectedScopeButAllScopeKeepsUpdatedTitle() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val library = AdvancedVideoLibrary(scope, repository { listOf(video) })
            library.selectFolders(setOf(folder), includeSubfolders = false)
            library.scan()
            val moved = video.copy(name = "renamed.mp4", folder = folder.copy(relativePath = "Movies/Sorted/"))
            library.replaceVideo(video.uri, moved)
            assertTrue(library.state.value.videos.isEmpty())
            library.selectFolders(null)
            library.scan()
            library.replaceVideo(video.uri, moved)
            assertEquals(listOf(moved), library.state.value.videos)
        } finally { scope.cancel() }
    }

    @Test fun duplicateActionsAreRejectedAndFinishedActionCanRetry() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val library = AdvancedVideoLibrary(scope, repository { listOf(video) })
            assertTrue(library.beginAction(video.uri))
            assertFalse(library.beginAction(video.uri))
            library.finishAction(video.uri)
            assertTrue(library.beginAction(video.uri))
        } finally { scope.cancel() }
    }

    @Test fun leavingReleasesFullMetadataAndFoldersButKeepsScope() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val library = AdvancedVideoLibrary(scope, repository { listOf(video) })
            library.selectFolders(setOf(folder))
            library.loadFolders()
            library.scan()
            assertEquals(1, library.state.value.videos.size)
            assertEquals(1, library.state.value.folders.size)
            library.leave()
            assertTrue(library.state.value.videos.isEmpty())
            assertTrue(library.state.value.folders.isEmpty())
            assertFalse(library.state.value.hasScanned)
            assertEquals(setOf(folder), library.state.value.selectedFolders)
        } finally { scope.cancel() }
    }

    @Test fun configurationChangePreservesCompletedScan() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val library = AdvancedVideoLibrary(scope, repository { listOf(video) })
            library.scan()
            library.leave(clearResults = false)
            assertEquals(listOf(video), library.state.value.videos)
            assertTrue(library.state.value.hasScanned)
        } finally { scope.cancel() }
    }

    @Test fun lateNonCooperativeScanCannotPublishAfterCancellation() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            lateinit var library: AdvancedVideoLibrary
            library = AdvancedVideoLibrary(scope, repository { progress ->
                progress(100)
                library.cancelScan()
                progress(200)
                listOf(video)
            })
            library.scan()
            assertFalse(library.state.value.scanning)
            assertFalse(library.state.value.hasScanned)
            assertTrue(library.state.value.videos.isEmpty())
            assertEquals(100, library.state.value.scannedCount)
        } finally { scope.cancel() }
    }

    @Test fun permissionFailureHasActionableErrorAndDoesNotPublishResults() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val library = AdvancedVideoLibrary(scope, repository { throw SecurityException() })
            library.scan()
            assertEquals(R.string.advanced_video_permission, library.state.value.errorRes)
            assertFalse(library.state.value.scanning)
            assertFalse(library.state.value.hasScanned)
        } finally { scope.cancel() }
    }
}
