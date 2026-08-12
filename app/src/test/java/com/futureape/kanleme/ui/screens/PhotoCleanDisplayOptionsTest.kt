package com.futureape.kanleme.ui.screens

import com.futureape.kanleme.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoCleanDisplayOptionsTest {
    @Test
    fun defaultsMirrorPhotoSettings() {
        assertEquals(
            PhotoCleanDisplayOptions(),
            AppSettings().toPhotoCleanDisplayOptions(),
        )
    }

    @Test
    fun everyPhotoVisibilityPreferenceReachesTheDisplayContract() {
        val options = AppSettings(
            photoShowTopBar = false,
            photoShowFilterChips = false,
            photoShowFolderChips = false,
            photoShowActionRail = false,
            photoShowInfoBar = false,
            photoShowGestureHint = false,
            photoShowShuffleButton = false,
        ).toPhotoCleanDisplayOptions()

        assertEquals(PhotoCleanDisplayOptions(false, false, false, false, false, false, false), options)
    }
}
