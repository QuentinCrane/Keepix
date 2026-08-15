package com.futureape.kanleme.ui.screens

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.futureape.kanleme.MainActivity
import com.futureape.kanleme.data.settings.ThemeMode
import com.futureape.kanleme.ui.theme.KanlemeTheme
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ScreenGoldenTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mePhoneLightGolden() = assertMeGolden(surface = "phone", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun mePhoneDarkGolden() = assertMeGolden(surface = "phone", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun meTabletLightGolden() = assertMeGolden(surface = "tablet", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun meTabletDarkGolden() = assertMeGolden(surface = "tablet", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun homePhoneLightGolden() = assertHomeGolden(surface = "phone", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun homePhoneDarkGolden() = assertHomeGolden(surface = "phone", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun homeTabletLightGolden() = assertHomeGolden(surface = "tablet", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun homeTabletDarkGolden() = assertHomeGolden(surface = "tablet", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun timelinePhoneLightGolden() = assertTimelineGolden(surface = "phone", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun timelinePhoneDarkGolden() = assertTimelineGolden(surface = "phone", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun timelineTabletLightGolden() = assertTimelineGolden(surface = "tablet", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun timelineTabletDarkGolden() = assertTimelineGolden(surface = "tablet", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun todayHistoryPhoneLightGolden() = assertTodayHistoryGolden(surface = "phone", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun todayHistoryPhoneDarkGolden() = assertTodayHistoryGolden(surface = "phone", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun todayHistoryTabletLightGolden() = assertTodayHistoryGolden(surface = "tablet", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun todayHistoryTabletDarkGolden() = assertTodayHistoryGolden(surface = "tablet", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun favoritesPhoneLightGolden() = assertFavoritesGolden(surface = "phone", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun favoritesPhoneDarkGolden() = assertFavoritesGolden(surface = "phone", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun favoritesTabletLightGolden() = assertFavoritesGolden(surface = "tablet", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun favoritesTabletDarkGolden() = assertFavoritesGolden(surface = "tablet", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun trashPhoneLightGolden() = assertTrashGolden(surface = "phone", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun trashPhoneDarkGolden() = assertTrashGolden(surface = "phone", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun trashTabletLightGolden() = assertTrashGolden(surface = "tablet", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun trashTabletDarkGolden() = assertTrashGolden(surface = "tablet", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun settingsPhoneLightGolden() = assertSettingsGolden(surface = "phone", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun settingsPhoneDarkGolden() = assertSettingsGolden(surface = "phone", theme = "dark", themeMode = ThemeMode.DARK)

    @Test
    fun settingsTabletLightGolden() = assertSettingsGolden(surface = "tablet", theme = "light", themeMode = ThemeMode.LIGHT)

    @Test
    fun settingsTabletDarkGolden() = assertSettingsGolden(surface = "tablet", theme = "dark", themeMode = ThemeMode.DARK)

    private fun assertMeGolden(surface: String, theme: String, themeMode: ThemeMode) = assertScreenGolden("me", surface, theme, themeMode) { viewModel ->
        MeScreen(
            viewModel = viewModel,
            contentPadding = PaddingValues(),
            onFavorites = {},
            onTrash = {},
            onHistory = {},
            onSettings = {},
            onSimilar = {},
            onAchievements = {},
            onAnnualReport = {},
            onHelp = {},
            onPrivacy = {},
            onChangelog = {},
            onDiagnosis = {},
        )
    }

    private fun assertHomeGolden(surface: String, theme: String, themeMode: ThemeMode) = assertScreenGolden("home", surface, theme, themeMode) { viewModel ->
        CleanHomeScreen(
            viewModel = viewModel,
            contentPadding = PaddingValues(),
            onPhoto = {},
            onVideo = {},
            onTimeline = {},
            onTrash = {},
            onFavorites = {},
            onToday = {},
            onSettings = {},
        )
    }

    private fun assertTimelineGolden(surface: String, theme: String, themeMode: ThemeMode) = assertScreenGolden("timeline", surface, theme, themeMode) { viewModel ->
        TimelineScreen(viewModel = viewModel, onBack = {}, onOpenPhoto = {})
    }

    private fun assertTodayHistoryGolden(surface: String, theme: String, themeMode: ThemeMode) = assertScreenGolden("today_history", surface, theme, themeMode) { viewModel ->
        TodayInHistoryScreen(viewModel = viewModel, onBack = {}, onOpenPhoto = {})
    }

    private fun assertFavoritesGolden(surface: String, theme: String, themeMode: ThemeMode) = assertScreenGolden("favorites", surface, theme, themeMode) { viewModel ->
        FavoritesScreen(viewModel = viewModel, onBack = {}, onToday = {})
    }

    private fun assertTrashGolden(surface: String, theme: String, themeMode: ThemeMode) = assertScreenGolden("trash", surface, theme, themeMode) { viewModel ->
        TrashScreen(viewModel = viewModel, onBack = {}, onToday = {})
    }

    private fun assertSettingsGolden(surface: String, theme: String, themeMode: ThemeMode) = assertScreenGolden("settings", surface, theme, themeMode) { viewModel ->
        SettingsScreen(viewModel = viewModel, onBack = {})
    }

    private fun assertScreenGolden(
        screen: String,
        surface: String,
        theme: String,
        themeMode: ThemeMode,
        content: @Composable (KanlemeViewModel) -> Unit,
    ) {
        val assetName = "screen_${screen}_${surface}_${theme}_api${Build.VERSION.SDK_INT}.png"
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val testContext = InstrumentationRegistry.getInstrumentation().context
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                    KanlemeTheme(themeMode = themeMode) {
                        Box(
                            modifier = Modifier
                                .width(if (surface == "phone") 390.dp else 840.dp)
                                .height(780.dp)
                                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                                .testTag("screen-golden-root"),
                        ) {
                            val viewModel: KanlemeViewModel = hiltViewModel()
                            content(viewModel)
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val actual = composeRule.onNodeWithTag("screen-golden-root").captureToImage().asAndroidBitmap()
        val outputDir = File(targetContext.filesDir, "screenshot-golden")
        outputDir.mkdirs()
        val actualFile = File(outputDir, "$assetName.actual.png")
        actualFile.outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }

        if (InstrumentationRegistry.getArguments().getString("kanleme.screenshotMode") == "capture") {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$assetName.actual.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/kanleme-goldens")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = requireNotNull(
                targetContext.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
            ) { "Unable to create capture asset $assetName" }
            targetContext.contentResolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "Unable to open capture asset $assetName" }
                actual.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            targetContext.contentResolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )
            return
        }

        val expected = testContext.assets.open("goldens/$assetName").use(BitmapFactory::decodeStream)
        assertEquals("Golden width mismatch for $assetName", expected.width, actual.width)
        assertEquals("Golden height mismatch for $assetName", expected.height, actual.height)

        val diff = Bitmap.createBitmap(actual.width, actual.height, Bitmap.Config.ARGB_8888)
        val pixelCount = actual.width * actual.height
        val expectedPixels = IntArray(pixelCount)
        val actualPixels = IntArray(pixelCount)
        val diffPixels = IntArray(pixelCount)
        expected.getPixels(expectedPixels, 0, actual.width, 0, 0, actual.width, actual.height)
        actual.getPixels(actualPixels, 0, actual.width, 0, 0, actual.width, actual.height)
        var mismatchedPixels = 0
        var maxChannelDelta = 0
        for (index in 0 until pixelCount) {
            val expectedPixel = expectedPixels[index]
            val actualPixel = actualPixels[index]
            val delta = max(
                max(abs(Color.red(expectedPixel) - Color.red(actualPixel)), abs(Color.green(expectedPixel) - Color.green(actualPixel))),
                max(abs(Color.blue(expectedPixel) - Color.blue(actualPixel)), abs(Color.alpha(expectedPixel) - Color.alpha(actualPixel))),
            )
            maxChannelDelta = max(maxChannelDelta, delta)
            if (delta > 12) {
                mismatchedPixels++
                diffPixels[index] = Color.RED
            } else {
                diffPixels[index] = Color.TRANSPARENT
            }
        }
        diff.setPixels(diffPixels, 0, actual.width, 0, 0, actual.width, actual.height)
        val mismatchRatio = mismatchedPixels.toDouble() / (actual.width * actual.height).toDouble()
        if (mismatchRatio > 0.002 || maxChannelDelta > 48) {
            val diffFile = File(outputDir, "$assetName.diff.png")
            diffFile.outputStream().use { diff.compress(Bitmap.CompressFormat.PNG, 100, it) }
            assertTrue(
                "Golden mismatch for $assetName: ratio=$mismatchRatio maxChannelDelta=$maxChannelDelta actual=$actualFile",
                false,
            )
        }
    }
}
