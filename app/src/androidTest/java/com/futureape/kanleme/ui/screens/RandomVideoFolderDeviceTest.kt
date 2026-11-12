package com.futureape.kanleme.ui.screens

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.FolderPlaybackVideo
import com.futureape.kanleme.ui.theme.KanlemeTheme
import com.futureape.kanleme.ui.viewmodel.RandomVideoFolderSession
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(UnstableApi::class)
class RandomVideoFolderDeviceTest {
    @get:Rule val composeRule = createComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private val fixtures = mutableListOf<Uri>()
    private val folderName = "KlmTest" + (System.nanoTime() % 1_000_000)
    private lateinit var session: RandomVideoFolderSession

    @After
    fun cleanup() {
        fixtures.forEach { context.contentResolver.delete(it, null, null) }
    }

    @Test
    fun systemPickerStartsPlaybackAndRescansOnlyTheRequestedFolderScope() {
        val folder = "Movies/" + folderName + "/"
        insertVideo(folder, "direct-one.mp4")
        insertVideo(folder, "direct-two.mp4")
        insertVideo(folder + "Nested/", "nested.mp4")
        insertVideo("Movies/" + folderName + "Other/", "outside.mp4")
        composeRule.setContent {
            val scope = rememberCoroutineScope()
            val testSession = remember { RandomVideoFolderSession(scope) }
            SideEffect { session = testSession }
            KanlemeTheme { RandomVideoFolderContent(testSession, onBack = {}, onPickerError = {}) }
        }
        composeRule.onNodeWithText(label(R.string.random_video_choose)).performClick()
        assertTrue("Folder should appear in the system picker", clickNativeText(folderName))
        assertTrue(clickNativeText("Use this folder", "USE THIS FOLDER", "使用此文件夹", "使用這個資料夾"))
        assertTrue(clickNativeText("Allow", "ALLOW", "允许", "允許"))
        composeRule.waitUntil(60_000) { ::session.isInitialized && session.state.value.playerVisible }
        assertEquals(setOf("direct-one.mp4", "direct-two.mp4", "nested.mp4"),
            session.state.value.videos.map { it.name }.toSet())
        awaitPlaying()
        val initial = playerIndex()
        composeRule.onNodeWithText(label(R.string.random_video_next)).performClick()
        composeRule.waitUntil(30_000) { playerIndex() != initial }
        val next = playerIndex()
        composeRule.onNodeWithText(label(R.string.random_video_previous)).performClick()
        composeRule.waitUntil(30_000) { playerIndex() != next }
        composeRule.onNodeWithText(label(R.string.advanced_back)).performClick()
        composeRule.waitUntil(5_000) { !session.state.value.playerVisible }
        val originalQueue = session.state.value.videos
        composeRule.onNodeWithText(label(R.string.random_video_play_list)).performClick()
        composeRule.waitUntil(10_000) { session.state.value.playerVisible }
        assertEquals(originalQueue, session.state.value.videos)
        awaitPlaying()
        composeRule.onNodeWithText(label(R.string.advanced_back)).performClick()
        composeRule.waitUntil(5_000) { !session.state.value.playerVisible }
        composeRule.onNodeWithText(label(R.string.advanced_include_subfolders)).performClick()
        composeRule.onNodeWithText(label(R.string.random_video_rescan)).performClick()
        composeRule.waitUntil(60_000) { session.state.value.playerVisible }
        assertEquals(setOf("direct-one.mp4", "direct-two.mp4"), session.state.value.videos.map { it.name }.toSet())
        awaitPlaying()
        val beforeAutoAdvance = playerIndex()
        composeRule.waitUntil(45_000) { playerIndex() != beforeAutoAdvance }
        assertNotEquals(beforeAutoAdvance, playerIndex())
    }

    @Test
    fun corruptVideoIsSkippedAndPlaybackContinues() {
        val broken = insertVideo("Movies/" + folderName + "/", "broken.mp4", valid = false)
        val playable = insertVideo("Movies/" + folderName + "/", "playable.mp4")
        showPlayer(listOf(broken, playable))
        composeRule.waitUntil(45_000) { playerIndex() == 1 && isPlaying() }
        assertTrue(isPlaying())
        val revisitedBroken = AtomicBoolean(false)
        onMain {
            val observedPlayer = requireNotNull(currentPlayer())
            observedPlayer.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (observedPlayer.currentMediaItemIndex == 0) revisitedBroken.set(true)
                }
            })
        }
        composeRule.waitUntil(45_000) { revisitedBroken.get() && playerIndex() == 1 && isPlaying() }
    }

    @Test
    fun allUnplayableVideosStopAndOfferRetry() {
        val brokenA = insertVideo("Movies/" + folderName + "/", "broken-a.mp4", valid = false)
        val brokenB = insertVideo("Movies/" + folderName + "/", "broken-b.mp4", valid = false)
        showPlayer(listOf(brokenA, brokenB))
        composeRule.waitUntil(45_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText(label(R.string.random_video_playback_error)))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(label(R.string.advanced_retry)).assertIsDisplayed()
    }

    private fun showPlayer(videos: List<FolderPlaybackVideo>) {
        composeRule.setContent { KanlemeTheme { RandomFolderVideoViewer(videos, onDismiss = {}) } }
    }

    private fun insertVideo(path: String, name: String, valid: Boolean = true): FolderPlaybackVideo {
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, path)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }))
        fixtures += uri
        requireNotNull(resolver.openOutputStream(uri)).use { output ->
            if (valid) instrumentation.context.assets.open("advanced-video/synthetic.mp4").use { it.copyTo(output) }
            else output.write("not a playable video".toByteArray())
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
        return FolderPlaybackVideo(uri.toString(), name, "video/mp4")
    }

    private fun label(id: Int) = context.getString(id)
    private fun allViews(root: View): Sequence<View> = sequence {
        yield(root)
        if (root is ViewGroup) for (index in 0 until root.childCount) yieldAll(allViews(root.getChildAt(index)))
    }
    private fun currentPlayer(): Player? = WindowInspector.getGlobalWindowViews()
        .asSequence().flatMap(::allViews).filterIsInstance<PlayerView>().firstOrNull { it.isShown && it.player != null }?.player
    private fun playerIndex(): Int = onMain { currentPlayer()?.currentMediaItemIndex ?: -1 }
    private fun isPlaying(): Boolean = onMain { currentPlayer()?.isPlaying == true }
    private fun awaitPlaying() { composeRule.waitUntil(45_000) { playerIndex() >= 0 && isPlaying() } }

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return requireNotNull(result).getOrThrow()
    }

    private fun clickNativeText(vararg texts: String): Boolean {
        val deadline = SystemClock.uptimeMillis() + 45_000
        while (SystemClock.uptimeMillis() < deadline) {
            val root = instrumentation.uiAutomation.rootInActiveWindow
            if (root != null) for (text in texts) {
                for (node in root.findAccessibilityNodeInfosByText(text)) {
                    if (!node.text.toString().equals(text, ignoreCase = true)) continue
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
}
