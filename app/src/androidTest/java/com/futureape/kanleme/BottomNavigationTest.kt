package com.futureape.kanleme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futureape.kanleme.ui.components.FloatingGlassNav
import com.futureape.kanleme.ui.theme.KanlemeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryDestinationsAreVisible() {
        composeRule.setContent {
            KanlemeTheme {
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

        composeRule.onNodeWithText("照片").assertIsDisplayed()

        composeRule.setContent {
            KanlemeTheme {
                FloatingGlassNav(
                    selectedIndex = 2,
                    items = listOf(
                        "照片" to Icons.Rounded.PhotoLibrary,
                        "视频" to Icons.Rounded.Movie,
                        "我的" to Icons.Rounded.Person,
                    ),
                    onSelected = {},
                )
            }
        }
        composeRule.onNodeWithText("我的").assertIsDisplayed()
    }
}
