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
import com.futureape.kanleme.ui.util.advancedVideoViewIntent
import com.futureape.kanleme.ui.util.openAdvancedVideoExternally
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
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
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val fixtures = mutableListOf<Uri>()
    private lateinit var viewModel: KanlemeViewModel
    private fun label(res: Int) = context.getString(res)

    @Before fun prepare() { hiltRule.inject() }
    @After fun cleanOwnFixtures() {
        fixtures.forEach { context.contentResolver.delete(it, null, null) }
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

    private fun screenshot(name: String) {
        composeRule.waitForIdle()
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
            KanlemeTheme(themeMode = ThemeMode.LIGHT) { AdvancedFeaturesScreen({}, {}, {}) }
        }
        composeRule.onNodeWithText(label(R.string.advanced_photos_later)).assertIsDisplayed()
        screenshot("advanced-hub")
        composeRule.activity.setContent {
            KanlemeTheme(themeMode = ThemeMode.LIGHT) { AdvancedLicensesScreen({}) }
        }
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("TERMS AND CONDITIONS FOR USE", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("advanced-licenses")
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
        screenshot("video-scoped-results")
        composeRule.onNodeWithText(label(R.string.advanced_filter)).performClick()
        composeRule.onNodeWithText(label(R.string.advanced_resolution)).assertIsDisplayed()
        screenshot("video-filters")
    }

    @Test fun playerAdvancesAndReleasesOnCloseAndBackground() {
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
}

private fun <T> android.app.Instrumentation.runOnMainSyncValue(block: () -> T): T {
    var result: Result<T>? = null
    runOnMainSync { result = runCatching(block) }
    return requireNotNull(result).getOrThrow()
}
