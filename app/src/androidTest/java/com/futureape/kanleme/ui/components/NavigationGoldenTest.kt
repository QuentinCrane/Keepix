package com.futureape.kanleme.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.futureape.kanleme.data.settings.ThemeMode
import com.futureape.kanleme.ui.theme.KanlemeTheme
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationGoldenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun phoneLightGolden() = assertGolden(surface = "phone", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun phoneDarkGolden() = assertGolden(surface = "phone", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun tabletLightGolden() = assertGolden(surface = "tablet", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun tabletDarkGolden() = assertGolden(surface = "tablet", theme = "dark", themeMode = ThemeMode.DARK)

    private fun assertGolden(surface: String, theme: String, themeMode: ThemeMode) {
        val assetName = "navigation_${surface}_${theme}_api${Build.VERSION.SDK_INT}.png"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                KanlemeTheme(themeMode = themeMode) {
                    Box(
                        modifier = Modifier
                            .width(if (surface == "phone") 390.dp else 96.dp)
                            .height(if (surface == "phone") 96.dp else 360.dp)
                            .background(MaterialTheme.colorScheme.background)
                            .testTag("golden-root"),
                    ) {
                        if (surface == "phone") {
                            FloatingGlassNav(
                                selectedIndex = 0,
                                items = navigationItems(),
                                onSelected = {},
                            )
                        } else {
                            FloatingGlassRail(
                                selectedIndex = 0,
                                items = navigationItems(),
                                onSelected = {},
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val actual = composeRule.onNodeWithTag("golden-root").captureToImage().asAndroidBitmap()
        val outputDir = File(context.filesDir, "screenshot-golden")
        outputDir.mkdirs()
        val actualFile = File(outputDir, "$assetName.actual.png")
        actualFile.outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }

        if (InstrumentationRegistry.getArguments().getString("kanleme.screenshotMode") == "capture") return

        val expected = testContext.assets.open("goldens/$assetName").use(BitmapFactory::decodeStream)
        assertEquals("Golden width mismatch for $assetName", expected.width, actual.width)
        assertEquals("Golden height mismatch for $assetName", expected.height, actual.height)

        val diff = Bitmap.createBitmap(actual.width, actual.height, Bitmap.Config.ARGB_8888)
        var mismatchedPixels = 0
        var maxChannelDelta = 0
        for (y in 0 until actual.height) {
            for (x in 0 until actual.width) {
                val expectedPixel = expected.getPixel(x, y)
                val actualPixel = actual.getPixel(x, y)
                val delta = max(
                    max(abs(Color.red(expectedPixel) - Color.red(actualPixel)), abs(Color.green(expectedPixel) - Color.green(actualPixel))),
                    max(abs(Color.blue(expectedPixel) - Color.blue(actualPixel)), abs(Color.alpha(expectedPixel) - Color.alpha(actualPixel))),
                )
                maxChannelDelta = max(maxChannelDelta, delta)
                if (delta > 12) {
                    mismatchedPixels++
                    diff.setPixel(x, y, Color.RED)
                } else {
                    diff.setPixel(x, y, Color.TRANSPARENT)
                }
            }
        }
        val mismatchRatio = mismatchedPixels.toDouble() / (actual.width * actual.height).toDouble()
        if (mismatchRatio > 0.002 || maxChannelDelta > 48) {
            File(outputDir, "$assetName.diff.png").outputStream().use { diff.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue(
                "Golden mismatch for $assetName: ratio=$mismatchRatio maxChannelDelta=$maxChannelDelta actual=$actualFile",
                false,
            )
        }
    }

    private fun navigationItems() = listOf(
        "photo" to Icons.Rounded.PhotoLibrary,
        "video" to Icons.Rounded.Movie,
        "me" to Icons.Rounded.Person,
    )
}
