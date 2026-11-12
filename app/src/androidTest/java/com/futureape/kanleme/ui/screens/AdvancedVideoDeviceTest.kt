package com.futureape.kanleme.ui.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.futureape.kanleme.MainActivity
import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.*
import com.futureape.kanleme.data.settings.ThemeMode
import com.futureape.kanleme.ui.theme.KanlemeTheme
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import com.futureape.kanleme.ui.navigation.KanlemeApp
import com.futureape.kanleme.ui.util.advancedVideoViewIntent
import com.futureape.kanleme.ui.util.openAdvancedVideoExternally
import com.futureape.kanleme.ui.util.AdvancedVideoThumbnails
import com.futureape.kanleme.data.repository.AppRepository
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.local.ProcessingStatus
import com.futureape.kanleme.data.local.DeletionStatus
import com.futureape.kanleme.data.settings.AppSettingsRepository
import com.futureape.kanleme.data.settings.DeleteMode
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject
import kotlin.system.measureNanoTime

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
@androidx.annotation.OptIn(UnstableApi::class)
class AdvancedVideoDeviceTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var scanner: AdvancedVideoScanner
    @Inject lateinit var repository: AppRepository
    @Inject lateinit var settingsRepository: AppSettingsRepository
    @Inject lateinit var database: com.futureape.kanleme.data.local.AppDatabase
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val fixtures = mutableListOf<Uri>()
    private lateinit var viewModel: KanlemeViewModel
    private lateinit var originalDeleteMode: DeleteMode
    private fun label(res: Int) = context.getString(res)

    private fun grantMediaReadAccess() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= 33) {
            listOf("android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_IMAGES")
        } else listOf("android.permission.READ_EXTERNAL_STORAGE")
        permissions.forEach { permission ->
            instrumentation.uiAutomation.executeShellCommand("pm grant " + context.packageName + " " + permission).close()
        }
    }

    @Before fun prepare() {
        hiltRule.inject()
        originalDeleteMode = runBlocking { settingsRepository.settings.first().deleteMode }
    }
    @After fun cleanOwnFixtures() {
        fixtures.forEach { context.contentResolver.delete(it, null, null) }
        runBlocking { settingsRepository.setDeleteMode(originalDeleteMode) }
        AdvancedVideoThumbnails.clear()
    }

    private fun insertVideo(path: String, name: String): Uri {
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, path)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }))
        fixtures += uri
        resolver.openOutputStream(uri)!!.use { output ->
            instrumentation.context.assets.open("advanced-video/synthetic.mp4").use { it.copyTo(output) }
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
        return uri
    }

    private fun showVideos() {
        composeRule.activity.setContent {
            KanlemeTheme(themeMode = ThemeMode.DARK) {
                val vm: KanlemeViewModel = hiltViewModel()
                SideEffect { viewModel = vm }
                AdvancedVideoScreen(vm, onBack = {})
            }
        }
    }

    private fun screenshot(name: String, awaitCompose: Boolean = true) {
        if (awaitCompose) composeRule.waitForIdle()
        instrumentation.waitForIdleSync()
        SystemClock.sleep(300) // Wait for the system compositor, including separate Dialog windows.
        val directory = File(context.getExternalFilesDir(null), "advanced-validation").apply { mkdirs() }
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        File(directory, name + ".png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun advancedEntryHubAndOfflineLicenseRender() {
        var entered = false
        composeRule.activity.setContent {
            KanlemeTheme(themeMode = ThemeMode.LIGHT) {
                val vm: KanlemeViewModel = hiltViewModel()
                MeScreen(vm, androidx.compose.foundation.layout.PaddingValues(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
                    onAdvanced = { entered = true })
            }
        }
        composeRule.onNodeWithText(label(R.string.advanced_title)).performScrollTo().performClick()
        assertTrue(entered)
        screenshot("me-advanced-entry")
        composeRule.activity.setContent {
            KanlemeTheme(themeMode = ThemeMode.LIGHT) { AdvancedFeaturesScreen({}, {}, {}, {}, {}) }
        }
        composeRule.onNodeWithText(label(R.string.advanced_photo_title)).assertIsDisplayed()
        screenshot("advanced-hub")
        composeRule.activity.setContent {
            KanlemeTheme(themeMode = ThemeMode.LIGHT) { AdvancedLicensesScreen({}) }
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("TERMS AND CONDITIONS FOR USE", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("advanced-licenses")
    }

    @Test fun realNavigationOpensVideoAndLicenseAndReturnsToHub() {
        fun settleMorph() {
            composeRule.mainClock.advanceTimeBy(1_000)
            composeRule.waitForIdle()
            instrumentation.waitForIdleSync()
        }
        composeRule.activity.setContent {
            KanlemeTheme(themeMode = ThemeMode.DARK) {
                val vm: KanlemeViewModel = hiltViewModel()
                LaunchedEffect(vm) { vm.markOnboardingShown() }
                KanlemeApp(initialShortcutTarget = null, viewModel = vm)
            }
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithContentDescription(label(R.string.nav_me)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(label(R.string.nav_me)).performClick()
        settleMorph()
        composeRule.onNodeWithText(label(R.string.advanced_title)).performScrollTo()
        // LazyList's scroll-to only considers its viewport; reveal the last row above the floating dock.
        composeRule.onRoot().performTouchInput { swipeUp(startY = height * 0.8f, endY = height * 0.4f) }
        composeRule.onNodeWithText(label(R.string.advanced_title)).performClick()
        settleMorph()
        composeRule.onNodeWithText(label(R.string.advanced_video_title)).performClick()
        settleMorph()
        composeRule.onNodeWithText(label(R.string.advanced_search)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(label(R.string.a11y_back)).performClick()
        settleMorph()
        composeRule.onNodeWithText(label(R.string.advanced_licenses)).performClick()
        settleMorph()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("TERMS AND CONDITIONS FOR USE", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription(label(R.string.a11y_back)).performClick()
        settleMorph()
        composeRule.onNodeWithText(label(R.string.advanced_photo_title)).assertIsDisplayed()
    }

    private fun allViews(root: View): Sequence<View> = sequence {
        yield(root)
        if (root is ViewGroup) for (index in 0 until root.childCount) yieldAll(allViews(root.getChildAt(index)))
    }
    private fun currentPlayerView(): PlayerView? = WindowInspector.getGlobalWindowViews()
        .asSequence().flatMap(::allViews).filterIsInstance<PlayerView>().firstOrNull()

    @Test fun scopedScannerAndFolderPickerUseRealMediaStore() = runBlocking {
        val folderA = "Movies/kanleme-advanced/A/"
        val folderB = "Movies/kanleme-advanced/B/"
        insertVideo(folderA, "advanced-one.mp4")
        insertVideo(folderB, "advanced-two.mp4")
        val folders = scanner.folders()
        val key = folders.first { it.key.relativePath == folderA }.key
        val progress = mutableListOf<Int>()
        val scanned = scanner.scan(setOf(key)) { progress += it }
        assertEquals(1, scanned.size)
        assertEquals(key, scanned.single().folder)
        assertEquals("advanced-one.mp4", scanned.single().name)
        assertTrue(scanned.single().width > 0 && scanned.single().durationMs > 0)
        assertEquals(1, progress.last())
        assertTrue(scanner.scan(null) {}.map { it.name }.containsAll(listOf("advanced-one.mp4", "advanced-two.mp4")))
        assertTrue(runCatching { scanner.scan(emptySet()) {} }.isFailure)

        showVideos()
        composeRule.waitUntil(10_000) { ::viewModel.isInitialized && !viewModel.advancedVideos.state.value.loadingFolders }
        composeRule.onNodeWithText(label(R.string.advanced_select_folders)).performClick()
        composeRule.onNodeWithText(folderA).performClick()
        composeRule.onNodeWithText(label(R.string.advanced_apply)).performClick()
        composeRule.onNodeWithText(label(R.string.advanced_scan)).performClick()
        composeRule.waitUntil(10_000) { viewModel.advancedVideos.state.value.hasScanned }
        assertEquals(listOf("advanced-one.mp4"), viewModel.advancedVideos.state.value.videos.map { it.name })
        composeRule.onNodeWithText("advanced-one.mp4").assertExists()
        composeRule.onNodeWithText("advanced-two.mp4").assertDoesNotExist()
        composeRule.onNodeWithText("advanced-one.mp4").performScrollTo()
        composeRule.waitUntil(10_000) { AdvancedVideoThumbnails.cachedBytes() > 0 }
        assertTrue(AdvancedVideoThumbnails.cachedBytes() <= 8 * 1024 * 1024)
        screenshot("video-scoped-results")
        composeRule.onNodeWithText(label(R.string.advanced_filter)).performClick()
        composeRule.onNodeWithText(label(R.string.advanced_resolution)).assertIsDisplayed()
        screenshot("video-filters")
    }

    @Test fun playerAdvancesAndReleasesOnCloseAndBackground() {
        grantMediaReadAccess()
        insertVideo("Movies/kanleme-advanced/playback/", "advanced-playback.mp4")
        showVideos()
        composeRule.onNodeWithText(label(R.string.advanced_scan)).performClick()
        composeRule.waitUntil(10_000) { ::viewModel.isInitialized && viewModel.advancedVideos.state.value.hasScanned }
        composeRule.onNodeWithText("advanced-playback.mp4").performScrollTo().performClick()
        var position = 0L
        composeRule.waitUntil(20_000) {
            composeRule.runOnIdle { position = currentPlayerView()?.player?.currentPosition ?: 0L }
            position > 300
        }
        screenshot("video-playing")
        composeRule.onNodeWithText(label(R.string.advanced_mute)).performClick()
        composeRule.runOnIdle { assertEquals(0f, currentPlayerView()?.player?.volume ?: -1f, 0f) }
        val active = composeRule.runOnIdle { requireNotNull(currentPlayerView()?.player) }
        composeRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        instrumentation.waitForIdleSync()
        assertEquals(androidx.media3.common.Player.STATE_IDLE, instrumentation.runOnMainSyncValue { active.playbackState })
        assertNull(instrumentation.runOnMainSyncValue { currentPlayerView() }?.player)
        composeRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        composeRule.waitUntil(10_000) { composeRule.runOnIdle { currentPlayerView()?.player != null } }
        assertTrue(composeRule.runOnIdle { currentPlayerView()?.player !== active })
        composeRule.onAllNodesWithText(label(R.string.advanced_back)).onLast().performClick()
        composeRule.waitUntil(5_000) { composeRule.runOnIdle { currentPlayerView() == null } }
        screenshot("video-viewer-closed")
    }

    @Test fun manualRotationRestoresPlaybackAndThePreviousActivityOrientation() {
        val originalOnboarding = runBlocking { settingsRepository.settings.first().onboardingShown }
        try {
            grantMediaReadAccess()
            runBlocking { settingsRepository.setOnboardingShown(true) }
            insertVideo("Movies/RotationTest/", "rotation-test.mp4")
            composeRule.waitUntil(30_000) {
                composeRule.onAllNodesWithContentDescription(label(R.string.nav_me)).fetchSemanticsNodes().isNotEmpty()
            }
            val originalOrientation = composeRule.activity.requestedOrientation
            composeRule.onNodeWithContentDescription(label(R.string.nav_me)).performClick()
            composeRule.onNodeWithText(label(R.string.advanced_title)).performScrollTo()
            composeRule.onRoot().performTouchInput { swipeUp(startY = height * 0.8f, endY = height * 0.4f) }
            composeRule.onNodeWithText(label(R.string.advanced_title)).performClick()
            composeRule.onNodeWithText(label(R.string.advanced_video_title)).performClick()
            composeRule.onNodeWithText(label(R.string.advanced_scan)).performClick()
            composeRule.waitUntil(30_000) {
                composeRule.onAllNodesWithText("rotation-test.mp4").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("rotation-test.mp4").performScrollTo().performClick()
            composeRule.waitUntil(30_000) { instrumentation.runOnMainSyncValue { currentPlayerView()?.player != null } }
            composeRule.onNodeWithText(label(R.string.advanced_mute)).performClick()
            composeRule.onNodeWithText(label(R.string.advanced_speed), substring = true).performClick()
            composeRule.onNodeWithText("1.5×").performClick()
            composeRule.runOnIdle { currentPlayerView()?.player?.apply { seekTo(1_200); pause() } }
            composeRule.onNodeWithContentDescription(label(R.string.advanced_rotate_landscape)).performClick()
            composeRule.waitUntil(30_000) {
                composeRule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE &&
                    instrumentation.runOnMainSyncValue { currentPlayerView()?.player != null }
            }
            composeRule.onNodeWithContentDescription(label(R.string.advanced_rotate_portrait)).assertIsDisplayed()
            composeRule.runOnIdle {
                val restored = requireNotNull(currentPlayerView()?.player)
                assertFalse(restored.playWhenReady)
                assertTrue("Playback position should survive rotation", restored.currentPosition >= 1_000)
                assertEquals(0f, restored.volume, 0f)
                assertEquals(1.5f, restored.playbackParameters.speed, 0f)
            }
            screenshot("advanced-player-landscape")
            composeRule.onNodeWithContentDescription(label(R.string.advanced_rotate_portrait)).performClick()
            composeRule.waitUntil(30_000) {
                composeRule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
            }
            composeRule.onNodeWithContentDescription(label(R.string.advanced_rotate_landscape)).performClick()
            composeRule.waitUntil(30_000) {
                composeRule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            }
            instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            composeRule.waitUntil(30_000) {
                composeRule.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
            }
            composeRule.onNodeWithContentDescription(label(R.string.advanced_rotate_landscape)).assertIsDisplayed()
            composeRule.onNodeWithText(label(R.string.advanced_back)).performClick()
            composeRule.waitUntil(10_000) { composeRule.activity.requestedOrientation == originalOrientation }
        } finally {
            runBlocking { settingsRepository.setOnboardingShown(originalOnboarding) }
        }
    }

    @Test fun filteredVideosCanBeBrowsedWithoutReturningToTheList() {
        grantMediaReadAccess()
        insertVideo("Movies/AdjacentTest/", "adjacent-test-a.mp4")
        insertVideo("Movies/AdjacentTest/", "adjacent-test-b.mp4")
        showVideos()
        composeRule.onNodeWithText(label(R.string.advanced_scan)).performClick()
        composeRule.waitUntil(30_000) { ::viewModel.isInitialized && viewModel.advancedVideos.state.value.hasScanned }
        composeRule.onNodeWithText(label(R.string.advanced_search)).performTextInput("adjacent-test")
        composeRule.waitUntil(30_000) { viewModel.advancedVideos.results.value.videos.size == 2 }
        val ordered = viewModel.advancedVideos.results.value.videos
        composeRule.onNodeWithText(ordered.first().name).performScrollTo().performClick()
        composeRule.waitUntil(30_000) { instrumentation.runOnMainSyncValue { currentPlayerView()?.player != null } }
        composeRule.onNodeWithText(label(R.string.advanced_mute)).performClick()
        composeRule.onNodeWithText(label(R.string.random_video_next)).performClick()
        composeRule.waitUntil(30_000) {
            instrumentation.runOnMainSyncValue {
                currentPlayerView()?.player?.currentMediaItem?.localConfiguration?.uri.toString() == ordered.last().uri
            }
        }
        composeRule.runOnIdle { assertEquals(0f, requireNotNull(currentPlayerView()?.player).volume, 0f) }
        composeRule.onNodeWithText(label(R.string.random_video_previous)).performClick()
        composeRule.waitUntil(30_000) {
            instrumentation.runOnMainSyncValue {
                currentPlayerView()?.player?.currentMediaItem?.localConfiguration?.uri.toString() == ordered.first().uri
            }
        }
    }

    @Test fun largeMetadataLibraryRecordsFilterLatency() {
        val now = System.currentTimeMillis()
        val videos = (1..50_000).map { AdvancedVideo("content://media/external_primary/video/media/" + it,
            "clip-" + (50_001 - it) + ".mp4", VideoFolderKey("external_primary", "Movies/"),
            it * 1024L, 1920, 1080, 90_000, now - it * 1000L, "video/mp4") }
        val filter = AdvancedVideoFilter(resolution = VideoResolution.FULL_HD, sort = VideoSort.NAME)
        repeat(3) { assertEquals(50_000, filter.apply(videos, now).size) }
        val timings = (1..7).map { measureNanoTime { assertEquals(50_000, filter.apply(videos, now).size) } / 1_000_000.0 }.sorted()
        android.util.Log.i("AdvancedVideoPerf", "synthetic_metadata=50000 median_ms=" + timings[3] + " max_ms=" + timings.last())
        // This is a scale/correctness test; emulator wall-clock numbers are not physical-device budgets.
        assertEquals(50_000, filter.apply(videos, now).size)
    }

    @Test fun externalViewerGetsOnlyOneReadOnlyContentUri(): Unit = runBlocking {
        val path = "Movies/kanleme-advanced/external/"
        insertVideo(path, "advanced-external.mp4")
        val folder = scanner.folders().first { it.key.relativePath == path }.key
        val video = scanner.scan(setOf(folder)) {}.single()
        val intent = advancedVideoViewIntent(video)
        assertEquals(android.content.Intent.ACTION_VIEW, intent.action)
        assertEquals("content", intent.data?.scheme)
        assertEquals(video.uri, intent.clipData?.getItemAt(0)?.uri.toString())
        assertEquals("video/mp4", intent.type)
        assertTrue(intent.flags and android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(0, intent.flags and android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        showVideos()
        composeRule.runOnIdle { openAdvancedVideoExternally(composeRule.activity, video) }
        val deadline = SystemClock.uptimeMillis() + 10_000
        var opened = false
        while (SystemClock.uptimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root != null && root.packageName != context.packageName) { opened = true; break }
            SystemClock.sleep(100)
        }
        assertTrue("System chooser should open", opened)
        screenshot("external-viewer-chooser")
        // Return through system Back; no login, network action or external-app mutation is performed.
        instrumentation.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
    }

    @Test fun deleteUsesAppTrashAndCanRestoreExistingFavorite(): Unit = runBlocking {
        insertVideo("Movies/kanleme-advanced/trash/", "advanced-trash.mp4")
        val video = scanner.scan(null) {}.first { it.name == "advanced-trash.mp4" }
        val entity = repository.prepareAdvancedVideoAction(video)
        repository.handleVideoAction(entity, SwipeAction.Favorite)
        settingsRepository.setDeleteMode(DeleteMode.SYSTEM_TRASH)
        showVideos()
        composeRule.onNodeWithText(label(R.string.advanced_scan)).performClick()
        composeRule.waitUntil(10_000) { ::viewModel.isInitialized && viewModel.advancedVideos.state.value.hasScanned }
        composeRule.onNodeWithText(label(R.string.advanced_delete)).performScrollTo().performClick()
        screenshot("video-delete-confirmation")
        composeRule.onNodeWithText(label(R.string.advanced_delete_confirm)).performClick()
        composeRule.waitUntil(10_000) {
            viewModel.advancedVideos.state.value.videos.none { it.uri == video.uri } &&
                viewModel.advancedVideos.state.value.busyUris.isEmpty()
        }
        assertNull(viewModel.pendingMediaAuthorization.value)
        val trash = repository.observeTrash().first().first { it.mediaId == entity.id && it.mediaType == "video" }
        assertEquals(DeletionStatus.PENDING, database.videoDao().byId(entity.id)?.deletionStatus)
        context.contentResolver.openInputStream(video.uri.let(Uri::parse))!!.use { assertTrue(it.read() >= 0) }
        assertTrue(repository.scanAdvancedVideos(null) {}.none { it.uri == video.uri })
        repository.restoreTrashItem(trash.id)
        val restored = repository.prepareAdvancedVideoAction(video)
        assertEquals(entity.id, restored.id)
        assertEquals(ProcessingStatus.FAVORITED, restored.processingStatus)
        assertEquals(DeletionStatus.NONE, restored.deletionStatus)
    }

    @Test fun moveCancellationKeepsVideoAndApprovalUpdatesRealPath(): Unit = runBlocking {
        val original = "Movies/kanleme-advanced/move-source/"
        val target = "Movies/kanleme-advanced/move-target/"
        insertVideo(original, "advanced-move.mp4")
        insertVideo(target, "advanced-target-placeholder.mp4")
        val video = scanner.scan(null) {}.first { it.name == "advanced-move.mp4" }
        showVideos()
        composeRule.waitForIdle()
        composeRule.runOnIdle { viewModel.advancedVideos.selectFolders(setOf(video.folder)); viewModel.advancedVideos.scan() }
        composeRule.waitUntil(10_000) { viewModel.advancedVideos.state.value.hasScanned }
        composeRule.onNodeWithText(label(R.string.advanced_move)).performScrollTo().performClick()
        assertTrue(clickNativeText("move-target"))
        screenshot("video-system-folder-picker", awaitCompose = false)
        assertTrue(clickNativeText("Use this folder", "USE THIS FOLDER", "使用此文件夹"))
        assertTrue(clickNativeText("Allow", "ALLOW", "允许"))
        screenshot("video-system-folder-selected")
        composeRule.waitUntil(10_000) { viewModel.pendingMediaAuthorization.value != null }
        composeRule.runOnIdle { viewModel.onMediaAuthorizationResult(false) }
        assertTrue(viewModel.advancedVideos.state.value.busyUris.isEmpty())
        assertEquals(original, viewModel.advancedVideos.state.value.videos.single().folder.relativePath)
        composeRule.runOnIdle { viewModel.moveAdvancedVideo(video, target) }
        composeRule.waitUntil(10_000) { viewModel.pendingMediaAuthorization.value != null }
        // The fixture is owned by the tested app. Simulate the shell's approval callback,
        // then verify the same repository MediaStore update used after real user consent.
        composeRule.runOnIdle { viewModel.onMediaAuthorizationResult(true) }
        composeRule.waitUntil(10_000) { viewModel.advancedVideos.state.value.busyUris.isEmpty() }
        context.contentResolver.query(Uri.parse(video.uri), arrayOf(MediaStore.Video.Media.RELATIVE_PATH), null, null, null)!!.use {
            assertTrue(it.moveToFirst()); assertEquals(target, it.getString(0))
        }
        assertTrue(viewModel.advancedVideos.state.value.videos.isEmpty()) // Moved outside the selected scope.
        val moved = repository.prepareAdvancedVideoAction(video.copy(folder = video.folder.copy(relativePath = target)), requireUnchanged = false)
        assertEquals(target, moved.folderPath)
    }

    @Test fun staleVideoMetadataBlocksDestructiveActions(): Unit = runBlocking {
        insertVideo("Movies/kanleme-advanced/stale/", "advanced-stale.mp4")
        val video = scanner.scan(null) {}.first { it.name == "advanced-stale.mp4" }
        assertTrue(runCatching { repository.prepareAdvancedVideoAction(video.copy(name = "stale-name.mp4")) }.isFailure)
        assertTrue(runCatching { repository.prepareAdvancedVideoAction(video.copy(sizeBytes = video.sizeBytes + 1)) }.isFailure)
        context.contentResolver.openInputStream(Uri.parse(video.uri))!!.use { assertTrue(it.read() >= 0) }
    }

    @Test fun coverCacheIsBoundedAndReleasesAllCachedBytes(): Unit = runBlocking {
        insertVideo("Movies/kanleme-advanced/covers/", "advanced-cover.mp4")
        val video = scanner.scan(null) {}.first { it.name == "advanced-cover.mp4" }
        repeat(80) { version ->
            val cover = AdvancedVideoThumbnails.load(context, video.copy(dateModifiedMs = video.dateModifiedMs + version))
            assertNotNull(cover)
            assertTrue(cover!!.width <= 256 && cover.height <= 160)
            assertTrue(AdvancedVideoThumbnails.cachedBytes() <= 8 * 1024 * 1024)
        }
        assertTrue(AdvancedVideoThumbnails.cachedBytes() > 0)
        AdvancedVideoThumbnails.clear()
        assertEquals(0, AdvancedVideoThumbnails.cachedBytes())
    }

    private fun clickNativeText(vararg texts: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root != null) for (text in texts) {
                val nodes = root.findAccessibilityNodeInfosByText(text)
                for (node in nodes) {
                    var target: android.view.accessibility.AccessibilityNodeInfo? = node
                    while (target != null && !target.isClickable) target = target.parent
                    if (target?.isEnabled == true && target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)) {
                        SystemClock.sleep(250)
                        return true
                    }
                }
            }
            SystemClock.sleep(100)
        }
        return false
    }

    @Test fun recursiveMediaStoreScopesEscapeWildcardsAndAvoidDuplicateRows(): Unit = runBlocking {
        val parent = "Movies/kanleme-advanced/Scope%_/"
        val child = parent + "Child/"
        insertVideo(parent, "advanced-parent.mp4")
        insertVideo(child, "advanced-child.mp4")
        insertVideo("Movies/kanleme-advanced/ScopeAA/", "advanced-unrelated.mp4")
        val folders = scanner.folders()
        val parentKey = folders.first { it.key.relativePath == parent }.key
        val childKey = folders.first { it.key.relativePath == child }.key
        val recursive = scanner.scan(setOf(parentKey, childKey)) {}
        assertEquals(setOf("advanced-parent.mp4", "advanced-child.mp4"), recursive.map { it.name }.toSet())
        assertEquals(2, recursive.size)
        assertEquals(listOf("advanced-parent.mp4"), scanner.scan(setOf(parentKey), includeSubfolders = false) {}.map { it.name })
    }
}

private fun <T> android.app.Instrumentation.runOnMainSyncValue(block: () -> T): T {
    var result: Result<T>? = null
    runOnMainSync { result = runCatching(block) }
    return requireNotNull(result).getOrThrow()
}
