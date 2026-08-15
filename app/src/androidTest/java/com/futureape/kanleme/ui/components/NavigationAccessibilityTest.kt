package com.futureape.kanleme.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
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
import org.junit.Assert.assertTrue
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

        val videoNode = composeRule.onNodeWithContentDescription("视频")
        videoNode
            .assertHasClickAction()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        assertTrue(videoNode.fetchSemanticsNode().config.contains(SemanticsProperties.Focused))
        videoNode.performClick()
        composeRule.runOnIdle { assertEquals(1, selectedIndex.intValue) }

        val meNode = composeRule.onNodeWithContentDescription("我的")
        meNode.assertHasClickAction()
        assertTrue(meNode.fetchSemanticsNode().config.contains(SemanticsProperties.Focused))
        meNode.performClick()
        composeRule.runOnIdle { assertEquals(2, selectedIndex.intValue) }
    }

    @Test
    fun tabletRailLabelsExposeFocusableTargets() {
        val selectedIndex = mutableIntStateOf(0)
        composeRule.setContent {
            KanlemeTheme {
                Box(Modifier.width(96.dp).height(420.dp)) {
                    FloatingGlassRail(
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
        }

        val videoNode = composeRule.onNodeWithContentDescription("视频")
        videoNode.assertHasClickAction()
        assertTrue(videoNode.fetchSemanticsNode().config.contains(SemanticsProperties.Focused))
        videoNode.performClick()
        composeRule.runOnIdle { assertEquals(1, selectedIndex.intValue) }
    }
}
