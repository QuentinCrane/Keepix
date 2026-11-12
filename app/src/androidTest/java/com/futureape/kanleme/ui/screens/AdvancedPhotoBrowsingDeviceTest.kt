package com.futureape.kanleme.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.futureape.kanleme.R
import com.futureape.kanleme.data.media.AdvancedPhoto
import com.futureape.kanleme.data.media.PhotoFolderKey
import com.futureape.kanleme.ui.theme.KanlemeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdvancedPhotoBrowsingDeviceTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun previewNavigatesFilteredPhotosBySwipeAndButtons() {
        val photos = (1..3).map { index ->
            AdvancedPhoto("content://test/$index", "photo-$index.jpg", PhotoFolderKey("external_primary", "Pictures/"),
                100, 1200, 900, 0, "image/jpeg", 0)
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.setContent {
            var index by remember { mutableIntStateOf(0) }
            KanlemeTheme {
                AdvancedPhotoPreviewDialog(photos[index], false, {}, {}, {}, {}, index, photos.size,
                    onPrevious = if (index > 0) ({ index-- }) else null,
                    onNext = if (index < photos.lastIndex) ({ index++ }) else null)
            }
        }
        composeRule.onNodeWithText("photo-1.jpg").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.advanced_photo_next)).performClick()
        composeRule.onNodeWithText("photo-2.jpg").assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.advanced_photo_previous)).performClick()
        composeRule.onNodeWithText("photo-1.jpg").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.advanced_photo_cover, "photo-1.jpg"))
            .performTouchInput { swipeLeft() }
        composeRule.onNodeWithText("photo-2.jpg").assertIsDisplayed()
    }
}
