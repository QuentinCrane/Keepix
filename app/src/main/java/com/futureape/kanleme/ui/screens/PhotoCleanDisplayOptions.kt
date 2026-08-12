package com.futureape.kanleme.ui.screens

import androidx.compose.runtime.Immutable
import com.futureape.kanleme.data.settings.AppSettings

/**
 * Read-only display contract for the photo cleaning surface.
 *
 * Keeping this separate from the full settings object makes it harder for the
 * screen to accidentally depend on unrelated persistence fields and gives each
 * visibility preference one place to be tested.
 */
@Immutable
data class PhotoCleanDisplayOptions(
    val showTopBar: Boolean = true,
    val showFilterChips: Boolean = true,
    val showFolderChips: Boolean = true,
    val showActionRail: Boolean = true,
    val showInfoBar: Boolean = true,
    val showGestureHint: Boolean = true,
    val showShuffleButton: Boolean = true,
)

fun AppSettings.toPhotoCleanDisplayOptions(): PhotoCleanDisplayOptions =
    PhotoCleanDisplayOptions(
        showTopBar = photoShowTopBar,
        showFilterChips = photoShowFilterChips,
        showFolderChips = photoShowFolderChips,
        showActionRail = photoShowActionRail,
        showInfoBar = photoShowInfoBar,
        showGestureHint = photoShowGestureHint,
        showShuffleButton = photoShowShuffleButton,
    )
