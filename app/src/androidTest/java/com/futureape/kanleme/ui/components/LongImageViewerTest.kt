package com.futureape.kanleme.ui.components

import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LongImageViewerTest {
    @get:Rule val composeRule = createComposeRule()
    private val files = mutableListOf<File>()
    private val mediaUris = mutableListOf<Uri>()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun cleanUp() {
        mediaUris.forEach { context.contentResolver.delete(it, null, null) }
        files.forEach { it.delete() }
    }

    @Test
    fun longPngRendersAtReadableWidthAndScrollsToTheEnd() {
        val image = saveToMediaStore(makeImage(rotated = false))
        val zoomed = AtomicBoolean(false)
        showViewer(image) { zoomed.set(it) }

        waitForColor(Color.RED, x = 40)
        // The narrow white stripe survives: this is not an enlarged whole-image thumbnail.
        waitForColor(Color.WHITE, x = 82)
        val node = composeRule.onNodeWithContentDescription("long image")
        repeat(12) { node.performTouchInput { swipeUp(durationMillis = 80) } }
        waitForColor(Color.GREEN, x = 40)
        repeat(12) { node.performTouchInput { swipeUp(durationMillis = 80) } }
        waitForColor(Color.BLUE, x = 40)

        node.performTouchInput { doubleClick(center) }
        composeRule.waitUntil(5_000) { zoomed.get() }
        node.performTouchInput { doubleClick(center) }
        composeRule.waitUntil(5_000) { !zoomed.get() }
    }

    @Test
    fun exifRotatedImageUsesItsDisplayOrientation() {
        showViewer(makeImage(rotated = true))
        waitForColor(Color.RED, x = 40)
        repeat(12) {
            composeRule.onNodeWithContentDescription("long image")
                .performTouchInput { swipeUp(durationMillis = 80) }
        }
        waitForColor(Color.GREEN, x = 40)
    }

    @Test
    fun staticWebpCanUseTheRegionViewer() {
        showViewer(makeImage(rotated = false, webp = true), mimeType = "image/webp")
        waitForColor(Color.RED, x = 40)
        waitForColor(Color.WHITE, x = 82)
    }

    @Test
    fun animatedWebpKeepsTheExistingPreview() {
        // Two-frame 20 x 100 WebP; the original preview is responsible for its animation.
        val bytes = android.util.Base64.decode(
            "UklGRoQAAABXRUJQVlA4WAoAAAACAAAAEwAAYwAAQU5JTQYAAAAAAAAAAABBTk1GKAAAAAAAAAAAABMAAGMAAGQAAAJWUDhMDwAAAC8TwBgABxD9j/4HIqL/AQBBTk1GKAAAAAAAAAAAABMAAGMAAGQAAABWUDhMDwAAAC8TwBgABxDR//4HIqL/AQA=",
            android.util.Base64.DEFAULT,
        )
        val file = File.createTempFile("long-image-animated", ".webp", context.cacheDir).also {
            files += it
            it.writeBytes(bytes)
        }
        showViewer(file, mimeType = "image/webp")
        waitForFallback()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun verticalScrollAndZoomCoexistWithHorizontalPaging() {
        val image = makeImage(rotated = false)
        val zoomed = AtomicBoolean(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val pager = rememberPagerState { 2 }
                var lockPager by remember { mutableStateOf(false) }
                LaunchedEffect(pager.currentPage) { lockPager = false }
                HorizontalPager(
                    state = pager,
                    userScrollEnabled = !lockPager,
                    modifier = Modifier.size(400.dp, 600.dp),
                ) { page ->
                    if (page == 0) {
                        LongImageViewer(
                            uri = Uri.fromFile(image),
                            description = "long image",
                            modifier = Modifier.size(400.dp, 600.dp),
                            topInset = 0.dp,
                            bottomInset = 0.dp,
                            onHorizontalPanChanged = {
                                if (pager.currentPage == 0) {
                                    lockPager = it
                                    zoomed.set(it)
                                }
                            },
                            fallback = { Text("existing preview") },
                        )
                    } else {
                        Box(Modifier.size(400.dp, 600.dp).semantics { contentDescription = "next page" }) {
                            Text("next image")
                        }
                    }
                }
            }
        }
        waitForColor(Color.RED, x = 40)
        val node = composeRule.onNodeWithContentDescription("long image")
        node.performTouchInput { swipeUp() }
        node.assertIsDisplayed()
        node.performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("next image").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("next page").performTouchInput { swipeRight() }
        waitForColor(Color.RED, x = 40)
        node.performTouchInput { doubleClick(center) }
        composeRule.waitUntil(5_000) { zoomed.get() }
        node.performTouchInput { swipeLeft() }
        node.assertIsDisplayed()
        node.performTouchInput { doubleClick(center) }
        composeRule.waitUntil(5_000) { !zoomed.get() }
        node.performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("next image").assertIsDisplayed()
    }

    @Test
    fun unsupportedImageReturnsToExistingPreview() {
        val file = File.createTempFile("long-image-invalid", ".png", context.cacheDir).also {
            files += it
            it.writeText("not an image")
        }
        showViewer(file)
        waitForFallback()
    }

    private fun waitForFallback() {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("existing preview"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("existing preview").assertIsDisplayed()
    }

    private fun showViewer(file: File, mimeType: String = "", onZoom: (Boolean) -> Unit = {}) {
        showViewer(Uri.fromFile(file), mimeType, onZoom)
    }

    private fun showViewer(uri: Uri, mimeType: String = "", onZoom: (Boolean) -> Unit = {}) {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.size(400.dp, 600.dp).background(androidx.compose.ui.graphics.Color.Black)) {
                    LongImageViewer(
                        uri = uri,
                        description = "long image",
                        modifier = Modifier.size(400.dp, 600.dp),
                        mimeType = mimeType,
                        topInset = 0.dp,
                        bottomInset = 0.dp,
                        onHorizontalPanChanged = onZoom,
                        fallback = { Text("existing preview") },
                    )
                }
            }
        }
    }

    private fun waitForColor(expected: Int, x: Int) {
        var observed = Color.BLACK
        composeRule.waitUntil(20_000) {
            val nodes = composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("long image"))
                .fetchSemanticsNodes()
            if (nodes.isEmpty()) return@waitUntil false
            val bitmap = composeRule.onNodeWithContentDescription("long image").captureToImage().asAndroidBitmap()
            if (bitmap.width <= x || bitmap.height == 0) return@waitUntil false
            observed = bitmap.getPixel(x, bitmap.height / 2)
            colorNear(observed, expected)
        }
        assertTrue("Expected color $expected, got $observed", colorNear(observed, expected))
    }

    private fun colorNear(actual: Int, expected: Int): Boolean =
        kotlin.math.abs(Color.red(actual) - Color.red(expected)) < 35 &&
            kotlin.math.abs(Color.green(actual) - Color.green(expected)) < 35 &&
            kotlin.math.abs(Color.blue(actual) - Color.blue(expected)) < 35

    private fun saveToMediaStore(file: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "kanleme-long-viewer-test-" + System.nanoTime() + ".png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KanlemeTest")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        mediaUris += uri
        requireNotNull(resolver.openOutputStream(uri)).use { output -> file.inputStream().use { it.copyTo(output) } }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        return uri
    }

    private fun makeImage(rotated: Boolean, webp: Boolean = false): File {
        val width = if (rotated) 12_000 else 400
        val height = if (rotated) 400 else 12_000
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        listOf(Color.RED, Color.GREEN, Color.BLUE).forEachIndexed { index, color ->
            paint.color = color
            if (rotated) canvas.drawRect(index * 4_000f, 0f, (index + 1) * 4_000f, 400f, paint)
            else canvas.drawRect(0f, index * 4_000f, 400f, (index + 1) * 4_000f, paint)
        }
        if (!rotated) {
            paint.color = Color.WHITE
            canvas.drawRect(80f, 0f, 85f, 12_000f, paint)
        }
        val file = File.createTempFile("long-image", if (rotated) ".jpg" else if (webp) ".webp" else ".png", context.cacheDir)
        files += file
        val format = if (rotated) Bitmap.CompressFormat.JPEG else if (webp) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.PNG
        file.outputStream().use { bitmap.compress(format, 100, it) }
        bitmap.recycle()
        if (rotated) {
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
        }
        return file
    }
}
