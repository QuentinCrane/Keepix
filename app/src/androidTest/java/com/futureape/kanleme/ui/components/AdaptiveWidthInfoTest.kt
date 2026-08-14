package com.futureape.kanleme.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveWidthInfoTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun widthAndFontScaleMatrixKeepsWindowClassBoundaries() {
        val widths = listOf(320, 360, 390, 600, 840)
        val fontScales = listOf(1f, 1.3f, 2f)
        val widthDp = mutableIntStateOf(widths.first())
        val fontScale = mutableFloatStateOf(fontScales.first())

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale.floatValue)) {
                Box(Modifier.width(widthDp.intValue.dp).height(120.dp)) {
                    AdaptiveWidthInfo { isMediumOrExpanded, isExpanded ->
                        Text(
                            text = when {
                                isExpanded -> "expanded"
                                isMediumOrExpanded -> "medium"
                                else -> "compact"
                            },
                            modifier = Modifier.testTag("adaptive-window-class"),
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

                val expected = when {
                    width >= 840 -> "expanded"
                    width >= 600 -> "medium"
                    else -> "compact"
                }
                composeRule.onNodeWithTag("adaptive-window-class").assertTextEquals(expected)
            }
        }
    }
}
