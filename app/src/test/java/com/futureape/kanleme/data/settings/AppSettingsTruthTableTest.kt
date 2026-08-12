package com.futureape.kanleme.data.settings

import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private enum class ConsumerStatus { ACTIVE, PREVIEW_ONLY, STORAGE_ONLY }

private data class TruthEntry(
    val fieldName: String,
    val storageKey: String,
    val status: ConsumerStatus,
)

private val truthEntries = listOf(
    TruthEntry("deleteMode", "delete_mode", ConsumerStatus.ACTIVE),
    TruthEntry("photoBatchSize", "photo_batch_size", ConsumerStatus.ACTIVE),
    TruthEntry("videoBatchSize", "video_batch_size", ConsumerStatus.ACTIVE),
    TruthEntry("autoMoveOnKeepFavorite", "auto_move_on_keep_favorite", ConsumerStatus.ACTIVE),
    TruthEntry("similarDetection", "similar_detection", ConsumerStatus.ACTIVE),
    TruthEntry("swipeSensitivity", "swipe_sensitivity", ConsumerStatus.ACTIVE),
    TruthEntry("gestureDirection", "gesture_direction", ConsumerStatus.ACTIVE),
    TruthEntry("quickActionButtons", "quick_action_buttons", ConsumerStatus.STORAGE_ONLY),
    TruthEntry("swapShareAndUndo", "swap_share_and_undo", ConsumerStatus.STORAGE_ONLY),
    TruthEntry("swipeSound", "swipe_sound", ConsumerStatus.ACTIVE),
    TruthEntry("swipeSoundStyle", "swipe_sound_style", ConsumerStatus.ACTIVE),
    TruthEntry("videoDefaultMuted", "video_default_muted", ConsumerStatus.ACTIVE),
    TruthEntry("videoDisplayMode", "video_display_mode", ConsumerStatus.ACTIVE),
    TruthEntry("hapticLevel", "haptic_level", ConsumerStatus.ACTIVE),
    TruthEntry("keepHapticLevel", "keep_haptic_level", ConsumerStatus.ACTIVE),
    TruthEntry("deleteHapticLevel", "delete_haptic_level", ConsumerStatus.ACTIVE),
    TruthEntry("favoriteHapticLevel", "favorite_haptic_level", ConsumerStatus.ACTIVE),
    TruthEntry("undoHapticLevel", "undo_haptic_level", ConsumerStatus.ACTIVE),
    TruthEntry("themeMode", "theme_mode", ConsumerStatus.ACTIVE),
    TruthEntry("todayInHistoryEntryMode", "today_in_history_entry_mode", ConsumerStatus.ACTIVE),
    TruthEntry("accentColor", "accent_color", ConsumerStatus.ACTIVE),
    TruthEntry("folderDisplay", "folder_display", ConsumerStatus.ACTIVE),
    TruthEntry("immersiveBackground", "immersive_background", ConsumerStatus.STORAGE_ONLY),
    TruthEntry("photoCleanMode", "photo_clean_mode", ConsumerStatus.STORAGE_ONLY),
    TruthEntry("homeMediaTab", "home_media_tab", ConsumerStatus.ACTIVE),
    TruthEntry("photoDateMode", "photo_date_mode", ConsumerStatus.ACTIVE),
    TruthEntry("photoMediaType", "photo_media_type", ConsumerStatus.ACTIVE),
    TruthEntry("photoSortOrder", "photo_sort_order", ConsumerStatus.ACTIVE),
    TruthEntry("photoRandomSeed", "photo_random_seed", ConsumerStatus.ACTIVE),
    TruthEntry("photoFolderPath", "photo_folder_path", ConsumerStatus.ACTIVE),
    TruthEntry("videoDateMode", "video_date_mode", ConsumerStatus.ACTIVE),
    TruthEntry("videoSortOrder", "video_sort_order", ConsumerStatus.ACTIVE),
    TruthEntry("videoRandomSeed", "video_random_seed", ConsumerStatus.ACTIVE),
    TruthEntry("videoFolderPath", "video_folder_path", ConsumerStatus.ACTIVE),
    TruthEntry("photoGuideShown", "photo_guide_shown", ConsumerStatus.ACTIVE),
    TruthEntry("videoGuideShown", "video_guide_shown", ConsumerStatus.ACTIVE),
    TruthEntry("excludedFolderPaths", "excluded_folder_paths", ConsumerStatus.ACTIVE),
    TruthEntry("onboardingShown", "onboarding_shown", ConsumerStatus.ACTIVE),
    TruthEntry("photoFocusMode", "photo_focus_mode", ConsumerStatus.ACTIVE),
    TruthEntry("photoShowTopBar", "photo_show_top_bar", ConsumerStatus.PREVIEW_ONLY),
    TruthEntry("photoShowFilterChips", "photo_show_filter_chips", ConsumerStatus.PREVIEW_ONLY),
    TruthEntry("photoShowFolderChips", "photo_show_folder_chips", ConsumerStatus.PREVIEW_ONLY),
    TruthEntry("photoShowActionRail", "photo_show_action_rail", ConsumerStatus.STORAGE_ONLY),
    TruthEntry("photoShowInfoBar", "photo_show_info_bar", ConsumerStatus.PREVIEW_ONLY),
    TruthEntry("photoShowGestureHint", "photo_show_gesture_hint", ConsumerStatus.PREVIEW_ONLY),
    TruthEntry("photoShowShuffleButton", "photo_show_shuffle_button", ConsumerStatus.PREVIEW_ONLY),
    TruthEntry("videoShowTopBar", "video_show_top_bar", ConsumerStatus.ACTIVE),
    TruthEntry("videoShowActionRail", "video_show_action_rail", ConsumerStatus.ACTIVE),
    TruthEntry("videoShowInfoPanel", "video_show_info_panel", ConsumerStatus.ACTIVE),
    TruthEntry("videoShowFolderChips", "video_show_folder_chips", ConsumerStatus.STORAGE_ONLY),
    TruthEntry("videoShowProgressBar", "video_show_progress_bar", ConsumerStatus.ACTIVE),
    TruthEntry("videoShowShuffleButton", "video_show_shuffle_button", ConsumerStatus.ACTIVE),
    TruthEntry("videoChromeVisible", "video_chrome_visible", ConsumerStatus.ACTIVE),
)

class AppSettingsTruthTableTest {
    @Test
    fun everyAppSettingsFieldHasOneStorageAndConsumerEntry() {
        val appSettingsFields = AppSettings::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
            .map { it.name }
            .toSet()

        assertEquals(appSettingsFields, truthEntries.map { it.fieldName }.toSet())
        assertEquals(truthEntries.size, truthEntries.map { it.fieldName }.toSet().size)
        assertEquals(truthEntries.size, truthEntries.map { it.storageKey }.toSet().size)
    }

    @Test
    fun knownUnimplementedPreferencesRemainExplicitlyMarked() {
        val byField = truthEntries.associateBy { it.fieldName }

        assertEquals(ConsumerStatus.STORAGE_ONLY, byField.getValue("quickActionButtons").status)
        assertEquals(ConsumerStatus.STORAGE_ONLY, byField.getValue("swapShareAndUndo").status)
        assertEquals(ConsumerStatus.STORAGE_ONLY, byField.getValue("immersiveBackground").status)
        assertEquals(ConsumerStatus.STORAGE_ONLY, byField.getValue("photoCleanMode").status)
        assertTrue(byField.getValue("photoShowTopBar").status == ConsumerStatus.PREVIEW_ONLY)
        assertEquals(ConsumerStatus.STORAGE_ONLY, byField.getValue("videoShowFolderChips").status)
    }
}
