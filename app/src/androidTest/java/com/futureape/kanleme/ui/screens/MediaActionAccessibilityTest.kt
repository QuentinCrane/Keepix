package com.futureape.kanleme.ui.screens

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.theme.KanlemeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaActionAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cleanupActionsAreFocusableAndKeepTheirActionIdentity() {
        val actions = mutableListOf<String>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.setContent {
            KanlemeTheme {
                MediaActionRow(
                    onKeep = { actions += "keep" },
                    onFavorite = { actions += "favorite" },
                    onDelete = { actions += "delete" },
                )
            }
        }

        listOf(
            R.string.swipe_action_keep to "keep",
            R.string.swipe_action_favorite to "favorite",
            R.string.swipe_action_delete to "delete",
        ).forEach { (labelRes, action) ->
            val node = composeRule.onNodeWithContentDescription(context.getString(labelRes))
            node.assertHasClickAction()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
            assertTrue(node.fetchSemanticsNode().config.contains(SemanticsProperties.Focused))
            node.performClick()
            composeRule.runOnIdle { assertEquals(action, actions.last()) }
        }
    }
}
