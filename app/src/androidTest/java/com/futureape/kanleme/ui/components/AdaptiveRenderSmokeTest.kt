package com.futureape.kanleme.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futureape.kanleme.ui.theme.KanlemeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveRenderSmokeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationRendersAtEveryAcceptanceWidthAndFontScale() {
        val widths = listOf(320, 360, 390, 600, 840)
        val fontScales = listOf(1f, 1.3f, 2f)
        val widthDp = mutableIntStateOf(widths.first())
        val fontScale = mutableFloatStateOf(fontScales.first())

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale.floatValue)) {
                KanlemeTheme {
                    Box(
                        modifier = Modifier
                            .width(widthDp.intValue.dp)
                            .height(120.dp)
                            .testTag("adaptive-render-root"),
                    ) {
                        FloatingGlassNav(
                            selectedIndex = 0,
                            items = listOf(
                                "照片" to Icons.Rounded.PhotoLibrary,
                                "视频" to Icons.Rounded.Movie,
                                "我的" to Icons.Rounded.Person,
                            ),
                            onSelected = {},
                        )
                    }
                }
            }
        }

        fontScales.forEach { scale ->
            widths.forEach { width ->
                composeRule.runOnIdle {
                    fontScale.floatValue = scale
                    widthDp.intValue = width
                }
                composeRule.waitForIdle()

                val node = composeRule.onNodeWithTag("adaptive-render-root")
                node.assertIsDisplayed()
                val image = node.captureToImage()
                assertEquals("Unexpected rendered width at fontScale=$scale", width, image.width)
                assertTrue("Rendered node must have pixels at fontScale=$scale", image.height > 0)
            }
        }
    }
}
