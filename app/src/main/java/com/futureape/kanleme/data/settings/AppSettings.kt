package com.futureape.kanleme.data.settings

import com.futureape.kanleme.R
import androidx.compose.runtime.Immutable
import androidx.annotation.StringRes

@Immutable
data class AppSettings(
    val deleteMode: DeleteMode = DeleteMode.PENDING_CONFIRM,
    val photoBatchSize: Int = 30,
    val videoBatchSize: Int = 20,
    val autoMoveOnKeepFavorite: Boolean = true,
    val similarDetection: Boolean = false,
    val swipeSensitivity: SwipeSensitivity = SwipeSensitivity.STANDARD,
    val gestureDirection: GestureDirection = GestureDirection.DEFAULT,
    val quickActionButtons: Boolean = true,
    val swapShareAndUndo: Boolean = false,
    val swipeSound: Boolean = true,
    val swipeSoundStyle: SwipeSoundStyle = SwipeSoundStyle.SOFT_BREEZE,
    val videoDefaultMuted: Boolean = true,
    val videoDisplayMode: VideoDisplayMode = VideoDisplayMode.IMMERSIVE_CROP,
    val hapticLevel: HapticLevel = HapticLevel.MEDIUM,
    val keepHapticLevel: HapticLevel = HapticLevel.MEDIUM,
    val deleteHapticLevel: HapticLevel = HapticLevel.MEDIUM,
    val favoriteHapticLevel: HapticLevel = HapticLevel.MEDIUM,
    val undoHapticLevel: HapticLevel = HapticLevel.MEDIUM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val todayInHistoryEntryMode: TodayInHistoryEntryMode = TodayInHistoryEntryMode.MEMORY_PAGE,
    val accentColor: Long = 0xFFC7ECFE,
    val folderDisplay: FolderDisplayMode = FolderDisplayMode.SINGLE_LINE,
    val immersiveBackground: Boolean = true,
    val photoCleanMode: PhotoCleanMode = PhotoCleanMode.NORMAL,
    val homeMediaTab: String = "photo",
    val photoDateMode: String = "all",
    val photoMediaType: String = "all",
    val photoSortOrder: String = "random",
    val photoRandomSeed: Long = 1L,
    val photoFolderPath: String = "",
    val videoDateMode: String = "all",
    val videoSortOrder: String = "random",
    val videoRandomSeed: Long = 1L,
    val videoFolderPath: String = "",
    val photoGuideShown: Boolean = false,
    val videoGuideShown: Boolean = false,
    val excludedFolderPaths: Set<String> = emptySet(),
    val onboardingShown: Boolean = false,
    val photoFocusMode: Boolean = false,
    val photoShowTopBar: Boolean = true,
    val photoShowFilterChips: Boolean = true,
    val photoShowFolderChips: Boolean = true,
    val photoShowActionRail: Boolean = true,
    val photoShowInfoBar: Boolean = true,
    val photoShowGestureHint: Boolean = true,
    val photoShowShuffleButton: Boolean = true,
    val videoShowTopBar: Boolean = true,
    val videoShowActionRail: Boolean = true,
    val videoShowInfoPanel: Boolean = true,
    val videoShowFolderChips: Boolean = true,
    val videoShowProgressBar: Boolean = true,
    val videoShowShuffleButton: Boolean = true,
    val videoChromeVisible: Boolean = true,
) {
    val photoThreshold: Float get() = swipeSensitivity.thresholdScale
    val videoSeekThresholdPx: Float get() = 80f * swipeSensitivity.thresholdScale
}

enum class DeleteMode(@StringRes val labelRes: Int, @StringRes val descriptionRes: Int) {
    PENDING_CONFIRM(R.string.delete_mode_pending_confirm, R.string.delete_mode_pending_confirm_desc),
    SYSTEM_TRASH(R.string.delete_mode_system_trash, R.string.delete_mode_system_trash_desc),
}

enum class SwipeSensitivity(@StringRes val labelRes: Int, val thresholdScale: Float) {
    GENTLE(R.string.swipe_sensitivity_gentle, 0.72f),
    STANDARD(R.string.swipe_sensitivity_standard, 1.0f),
    STABLE(R.string.swipe_sensitivity_stable, 1.28f),
}

enum class GestureDirection(@StringRes val labelRes: Int, @StringRes val guideRes: Int) {
    DEFAULT(R.string.gesture_direction_default, R.string.gesture_direction_default_guide),
    REVERSE_VERTICAL(R.string.gesture_direction_reverse_vertical, R.string.gesture_direction_reverse_vertical_guide),
}

enum class VideoDisplayMode(@StringRes val labelRes: Int, @StringRes val descriptionRes: Int) {
    IMMERSIVE_CROP(R.string.video_display_mode_immersive_crop, R.string.video_display_mode_immersive_crop_desc),
    FIT_SCREEN(R.string.video_display_mode_fit_screen, R.string.video_display_mode_fit_screen_desc),
    FILL_WIDTH(R.string.video_display_mode_fill_width, R.string.video_display_mode_fill_width_desc),
}

enum class HapticLevel(@StringRes val labelRes: Int) {
    OFF(R.string.haptic_level_off),
    LIGHT(R.string.haptic_level_light),
    MEDIUM(R.string.haptic_level_medium),
    STRONG(R.string.haptic_level_strong),
}

enum class SwipeSoundStyle(@StringRes val labelRes: Int, @StringRes val descriptionRes: Int) {
    SOFT_BREEZE(R.string.swipe_sound_soft_breeze, R.string.swipe_sound_soft_breeze_desc),
    LOW_TAP(R.string.swipe_sound_low_tap, R.string.swipe_sound_low_tap_desc),
    ORIGINAL_WHOOSH(R.string.swipe_sound_original_whoosh, R.string.swipe_sound_original_whoosh_desc),
}

enum class ThemeMode(@StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_mode_system),
    LIGHT(R.string.theme_mode_light),
    DARK(R.string.theme_mode_dark),
}

enum class TodayInHistoryEntryMode {
    MEMORY_PAGE,
    PINCH_MEMORY,
}

enum class FolderDisplayMode(@StringRes val labelRes: Int) {
    SINGLE_LINE(R.string.folder_display_single_line),
    MULTI_LINE(R.string.folder_display_multi_line),
}

enum class PhotoCleanMode(@StringRes val labelRes: Int, @StringRes val descriptionRes: Int) {
    NORMAL(R.string.photo_clean_mode_normal, R.string.photo_clean_mode_normal_desc),
    DIAGNOSTIC(R.string.photo_clean_mode_diagnostic, R.string.photo_clean_mode_diagnostic_desc),
    PERFORMANCE(R.string.photo_clean_mode_performance, R.string.photo_clean_mode_performance_desc),
}

fun nextBatchSize(current: Int): Int = when (current) {
    10 -> 20
    20 -> 30
    30 -> 40
    40 -> 60
    else -> 10
}

fun nextAccentColor(current: Long): Long {
    val palette = listOf(0xFFC7ECFE, 0xFFDDF5FF, 0xFFF4FBFF, 0xFFEAF8FF, 0xFFE8F0F7)
    val index = palette.indexOf(current).takeIf { it >= 0 } ?: 0
    return palette[(index + 1) % palette.size]
}
