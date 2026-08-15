package com.futureape.kanleme.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futureape.kanleme.ui.theme.KanlemeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationLabelsExposeClickableTargetsWithMinimumTouchSize() {
        val selectedIndex = mutableIntStateOf(0)
        composeRule.setContent {
            KanlemeTheme {
                FloatingGlassNav(
                    selectedIndex = selectedIndex.intValue,
                    items = listOf(
                        "照片" to Icons.Rounded.PhotoLibrary,
                        "视频" to Icons.Rounded.Movie,
                        "我的" to Icons.Rounded.Person,
                    ),
                    onSelected = { selectedIndex.intValue = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("视频")
            .assertHasClickAction()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, selectedIndex.intValue) }

        composeRule.onNodeWithContentDescription("我的")
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals(2, selectedIndex.intValue) }
    }
}
