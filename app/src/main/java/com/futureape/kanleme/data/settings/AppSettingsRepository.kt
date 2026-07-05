package com.futureape.kanleme.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.kanlemeSettingsDataStore by preferencesDataStore(name = "kanleme_settings")

@Singleton
class AppSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val deleteMode = stringPreferencesKey("delete_mode")
        val photoBatchSize = intPreferencesKey("photo_batch_size")
        val videoBatchSize = intPreferencesKey("video_batch_size")
        val autoMoveOnKeepFavorite = booleanPreferencesKey("auto_move_on_keep_favorite")
        val similarDetection = booleanPreferencesKey("similar_detection")
        val swipeSensitivity = stringPreferencesKey("swipe_sensitivity")
        val gestureDirection = stringPreferencesKey("gesture_direction")
        val quickActionButtons = booleanPreferencesKey("quick_action_buttons")
        val swapShareAndUndo = booleanPreferencesKey("swap_share_and_undo")
        val swipeSound = booleanPreferencesKey("swipe_sound")
        val videoDefaultMuted = booleanPreferencesKey("video_default_muted")
        val videoDisplayMode = stringPreferencesKey("video_display_mode")
        val hapticLevel = stringPreferencesKey("haptic_level")
        val keepHapticLevel = stringPreferencesKey("keep_haptic_level")
        val deleteHapticLevel = stringPreferencesKey("delete_haptic_level")
        val favoriteHapticLevel = stringPreferencesKey("favorite_haptic_level")
        val undoHapticLevel = stringPreferencesKey("undo_haptic_level")
        val themeMode = stringPreferencesKey("theme_mode")
        val todayInHistoryEntryMode = stringPreferencesKey("today_in_history_entry_mode")
        val accentColor = longPreferencesKey("accent_color")
        val folderDisplay = stringPreferencesKey("folder_display")
        val immersiveBackground = booleanPreferencesKey("immersive_background")
        val photoCleanMode = stringPreferencesKey("photo_clean_mode")
        val homeMediaTab = stringPreferencesKey("home_media_tab")
        val photoDateMode = stringPreferencesKey("photo_date_mode")
        val photoMediaType = stringPreferencesKey("photo_media_type")
        val photoSortOrder = stringPreferencesKey("photo_sort_order")
        val photoRandomSeed = longPreferencesKey("photo_random_seed")
        val photoFolderPath = stringPreferencesKey("photo_folder_path")
        val videoDateMode = stringPreferencesKey("video_date_mode")
        val videoSortOrder = stringPreferencesKey("video_sort_order")
        val videoRandomSeed = longPreferencesKey("video_random_seed")
        val videoFolderPath = stringPreferencesKey("video_folder_path")
        val photoGuideShown = booleanPreferencesKey("photo_guide_shown")
        val videoGuideShown = booleanPreferencesKey("video_guide_shown")
        val excludedFolderPaths = stringSetPreferencesKey("excluded_folder_paths")
        val onboardingShown = booleanPreferencesKey("onboarding_shown")
        val photoFocusMode = booleanPreferencesKey("photo_focus_mode")
        val photoShowTopBar = booleanPreferencesKey("photo_show_top_bar")
        val photoShowFilterChips = booleanPreferencesKey("photo_show_filter_chips")
        val photoShowFolderChips = booleanPreferencesKey("photo_show_folder_chips")
        val photoShowActionRail = booleanPreferencesKey("photo_show_action_rail")
        val photoShowInfoBar = booleanPreferencesKey("photo_show_info_bar")
        val photoShowGestureHint = booleanPreferencesKey("photo_show_gesture_hint")
        val photoShowShuffleButton = booleanPreferencesKey("photo_show_shuffle_button")
        val videoShowTopBar = booleanPreferencesKey("video_show_top_bar")
        val videoShowActionRail = booleanPreferencesKey("video_show_action_rail")
        val videoShowInfoPanel = booleanPreferencesKey("video_show_info_panel")
        val videoShowFolderChips = booleanPreferencesKey("video_show_folder_chips")
        val videoShowProgressBar = booleanPreferencesKey("video_show_progress_bar")
        val videoShowShuffleButton = booleanPreferencesKey("video_show_shuffle_button")
        val videoChromeVisible = booleanPreferencesKey("video_chrome_visible")
        val organizerDateModePreviewFixApplied = booleanPreferencesKey("organizer_date_mode_preview_fix_applied")
    }

    val settings: Flow<AppSettings> = context.kanlemeSettingsDataStore.data.map { it.toAppSettings() }

    suspend fun loadStartupSettings(): AppSettings {
        val current = context.kanlemeSettingsDataStore.data.first()
        if (current[Keys.organizerDateModePreviewFixApplied] == true) return current.toAppSettings()
        context.kanlemeSettingsDataStore.edit { prefs ->
            // Older builds accidentally persisted organizer preview date ranges.
            prefs[Keys.organizerDateModePreviewFixApplied] = true
            prefs[Keys.photoDateMode] = "all"
            prefs[Keys.videoDateMode] = "all"
        }
        return context.kanlemeSettingsDataStore.data.first().toAppSettings()
    }

    suspend fun setDeleteMode(value: DeleteMode) = editString(Keys.deleteMode, value.name)
    suspend fun setPhotoBatchSize(value: Int) = context.kanlemeSettingsDataStore.edit { it[Keys.photoBatchSize] = value }
    suspend fun setVideoBatchSize(value: Int) = context.kanlemeSettingsDataStore.edit { it[Keys.videoBatchSize] = value }
    suspend fun setAutoMoveOnKeepFavorite(value: Boolean) = editBoolean(Keys.autoMoveOnKeepFavorite, value)
    suspend fun setSimilarDetection(value: Boolean) = editBoolean(Keys.similarDetection, value)
    suspend fun setSwipeSensitivity(value: SwipeSensitivity) = editString(Keys.swipeSensitivity, value.name)
    suspend fun setGestureDirection(value: GestureDirection) = editString(Keys.gestureDirection, value.name)
    suspend fun setQuickActionButtons(value: Boolean) = editBoolean(Keys.quickActionButtons, value)
    suspend fun setSwapShareAndUndo(value: Boolean) = editBoolean(Keys.swapShareAndUndo, value)
    suspend fun setSwipeSound(value: Boolean) = editBoolean(Keys.swipeSound, value)
    suspend fun setVideoDefaultMuted(value: Boolean) = editBoolean(Keys.videoDefaultMuted, value)
    suspend fun setVideoDisplayMode(value: VideoDisplayMode) = editString(Keys.videoDisplayMode, value.name)
    suspend fun setHapticLevel(value: HapticLevel) = editString(Keys.hapticLevel, value.name)
    suspend fun setKeepHapticLevel(value: HapticLevel) = editString(Keys.keepHapticLevel, value.name)
    suspend fun setDeleteHapticLevel(value: HapticLevel) = editString(Keys.deleteHapticLevel, value.name)
    suspend fun setFavoriteHapticLevel(value: HapticLevel) = editString(Keys.favoriteHapticLevel, value.name)
    suspend fun setUndoHapticLevel(value: HapticLevel) = editString(Keys.undoHapticLevel, value.name)
    suspend fun setThemeMode(value: ThemeMode) = editString(Keys.themeMode, value.name)
    suspend fun setTodayInHistoryEntryMode(value: TodayInHistoryEntryMode) = editString(Keys.todayInHistoryEntryMode, value.name)
    suspend fun setAccentColor(value: Long) = context.kanlemeSettingsDataStore.edit { it[Keys.accentColor] = value }
    suspend fun setFolderDisplay(value: FolderDisplayMode) = editString(Keys.folderDisplay, value.name)
    suspend fun setImmersiveBackground(value: Boolean) = editBoolean(Keys.immersiveBackground, value)
    suspend fun setPhotoCleanMode(value: PhotoCleanMode) = editString(Keys.photoCleanMode, value.name)
    suspend fun setHomeMediaTab(value: String) = editString(Keys.homeMediaTab, sanitizeTab(value))
    suspend fun setPhotoDateMode(value: String) = editString(Keys.photoDateMode, sanitizeDateMode(value))
    suspend fun setPhotoMediaType(value: String) = editString(Keys.photoMediaType, sanitizePhotoMediaType(value))
    suspend fun setPhotoSortOrder(value: String) = editString(Keys.photoSortOrder, sanitizeSortOrder(value))
    suspend fun setPhotoSortOrderWithSeed(value: String, seed: Long) = context.kanlemeSettingsDataStore.edit { prefs ->
        prefs[Keys.photoSortOrder] = sanitizeSortOrder(value)
        if (value == "random") prefs[Keys.photoRandomSeed] = seed
    }
    suspend fun setPhotoFolderPath(value: String?) = editString(Keys.photoFolderPath, value.orEmpty())
    suspend fun setVideoDateMode(value: String) = editString(Keys.videoDateMode, sanitizeDateMode(value))
    suspend fun setVideoSortOrder(value: String) = editString(Keys.videoSortOrder, sanitizeSortOrder(value))
    suspend fun setVideoSortOrderWithSeed(value: String, seed: Long) = context.kanlemeSettingsDataStore.edit { prefs ->
        prefs[Keys.videoSortOrder] = sanitizeSortOrder(value)
        if (value == "random") prefs[Keys.videoRandomSeed] = seed
    }
    suspend fun setVideoFolderPath(value: String?) = editString(Keys.videoFolderPath, value.orEmpty())
    suspend fun setPhotoGuideShown(value: Boolean) = editBoolean(Keys.photoGuideShown, value)
    suspend fun setVideoGuideShown(value: Boolean) = editBoolean(Keys.videoGuideShown, value)
    suspend fun setExcludedFolderPaths(value: Set<String>) = context.kanlemeSettingsDataStore.edit { it[Keys.excludedFolderPaths] = value }
    suspend fun toggleExcludedFolder(path: String) = context.kanlemeSettingsDataStore.edit { prefs ->
        val normalized = normalizeFolderRule(path)
        if (normalized.isNotBlank()) {
            val current = prefs[Keys.excludedFolderPaths] ?: emptySet()
            prefs[Keys.excludedFolderPaths] = if (normalized in current) current - normalized else current + normalized
        }
    }

    suspend fun addExcludedFolder(path: String) = context.kanlemeSettingsDataStore.edit { prefs ->
        val normalized = normalizeFolderRule(path)
        if (normalized.isNotBlank()) {
            val current = prefs[Keys.excludedFolderPaths] ?: emptySet()
            prefs[Keys.excludedFolderPaths] = current + normalized
        }
    }
    suspend fun clearExcludedFolders() = context.kanlemeSettingsDataStore.edit { it[Keys.excludedFolderPaths] = emptySet() }
    suspend fun setOnboardingShown(value: Boolean) = editBoolean(Keys.onboardingShown, value)
    suspend fun setPhotoFocusMode(value: Boolean) = editBoolean(Keys.photoFocusMode, value)
    suspend fun setPhotoShowTopBar(value: Boolean) = editBoolean(Keys.photoShowTopBar, value)
    suspend fun setPhotoShowFilterChips(value: Boolean) = editBoolean(Keys.photoShowFilterChips, value)
    suspend fun setPhotoShowFolderChips(value: Boolean) = editBoolean(Keys.photoShowFolderChips, value)
    suspend fun setPhotoShowActionRail(value: Boolean) = editBoolean(Keys.photoShowActionRail, value)
    suspend fun setPhotoShowInfoBar(value: Boolean) = editBoolean(Keys.photoShowInfoBar, value)
    suspend fun setPhotoShowGestureHint(value: Boolean) = editBoolean(Keys.photoShowGestureHint, value)
    suspend fun setPhotoShowShuffleButton(value: Boolean) = editBoolean(Keys.photoShowShuffleButton, value)
    suspend fun setVideoShowTopBar(value: Boolean) = editBoolean(Keys.videoShowTopBar, value)
    suspend fun setVideoShowActionRail(value: Boolean) = editBoolean(Keys.videoShowActionRail, value)
    suspend fun setVideoShowInfoPanel(value: Boolean) = editBoolean(Keys.videoShowInfoPanel, value)
    suspend fun setVideoShowFolderChips(value: Boolean) = editBoolean(Keys.videoShowFolderChips, value)
    suspend fun setVideoShowProgressBar(value: Boolean) = editBoolean(Keys.videoShowProgressBar, value)
    suspend fun setVideoShowShuffleButton(value: Boolean) = editBoolean(Keys.videoShowShuffleButton, value)
    suspend fun setVideoChromeVisible(value: Boolean) = editBoolean(Keys.videoChromeVisible, value)

    private fun normalizeFolderRule(path: String): String {
        val cleaned = path.trim().replace('\\', '/').trim('/')
        return if (cleaned.isBlank()) "" else cleaned + "/"
    }

    private fun sanitizeTab(value: String?): String = if (value == "video") "video" else "photo"

    private fun sanitizeSortOrder(value: String?): String = if (value == "newest") "newest" else "random"

    private fun sanitizeDateMode(value: String?): String {
        val safe = value.orEmpty().trim()
        if (safe in setOf("all", "seven_days", "month", "year", "today_history")) return safe
        if (Regex("""y:\d{4}""").matches(safe)) return safe
        if (Regex("""ym:\d{4}-\d{2}""").matches(safe)) return safe
        if (Regex("""d:\d{4}-\d{2}-\d{2}""").matches(safe)) return safe
        if (Regex("""multiym:(\d{4}-\d{2})(,\d{4}-\d{2})*""").matches(safe)) return safe
        return "all"
    }

    private fun sanitizePhotoMediaType(value: String?): String = when (value) {
        "normal", "screenshot", "selfie", "motion", "long", "gif", "raw" -> value
        else -> "all"
    }

    private fun sanitizeTodayInHistoryEntryMode(value: String?): TodayInHistoryEntryMode = when (value) {
        TodayInHistoryEntryMode.PINCH_MEMORY.name, "PHOTO_CLEANING" -> TodayInHistoryEntryMode.PINCH_MEMORY
        TodayInHistoryEntryMode.MEMORY_PAGE.name -> TodayInHistoryEntryMode.MEMORY_PAGE
        else -> TodayInHistoryEntryMode.MEMORY_PAGE
    }

    private fun Preferences.toAppSettings(): AppSettings =
        AppSettings(
            deleteMode = this[Keys.deleteMode].toEnum(DeleteMode.PENDING_CONFIRM),
            photoBatchSize = this[Keys.photoBatchSize] ?: 30,
            videoBatchSize = this[Keys.videoBatchSize] ?: 20,
            autoMoveOnKeepFavorite = this[Keys.autoMoveOnKeepFavorite] ?: true,
            similarDetection = this[Keys.similarDetection] ?: false,
            swipeSensitivity = this[Keys.swipeSensitivity].toEnum(SwipeSensitivity.STANDARD),
            gestureDirection = this[Keys.gestureDirection].toEnum(GestureDirection.DEFAULT),
            quickActionButtons = this[Keys.quickActionButtons] ?: true,
            swapShareAndUndo = this[Keys.swapShareAndUndo] ?: false,
            swipeSound = this[Keys.swipeSound] ?: true,
            videoDefaultMuted = this[Keys.videoDefaultMuted] ?: true,
            videoDisplayMode = this[Keys.videoDisplayMode].toEnum(VideoDisplayMode.IMMERSIVE_CROP),
            hapticLevel = this[Keys.hapticLevel].toEnum(HapticLevel.MEDIUM),
            keepHapticLevel = this[Keys.keepHapticLevel].toEnum(HapticLevel.MEDIUM),
            deleteHapticLevel = this[Keys.deleteHapticLevel].toEnum(HapticLevel.MEDIUM),
            favoriteHapticLevel = this[Keys.favoriteHapticLevel].toEnum(HapticLevel.MEDIUM),
            undoHapticLevel = this[Keys.undoHapticLevel].toEnum(HapticLevel.MEDIUM),
            themeMode = this[Keys.themeMode].toEnum(ThemeMode.SYSTEM),
            todayInHistoryEntryMode = sanitizeTodayInHistoryEntryMode(this[Keys.todayInHistoryEntryMode]),
            accentColor = this[Keys.accentColor] ?: 0xFFC7ECFE,
            folderDisplay = this[Keys.folderDisplay].toEnum(FolderDisplayMode.SINGLE_LINE),
            immersiveBackground = this[Keys.immersiveBackground] ?: true,
            photoCleanMode = this[Keys.photoCleanMode].toEnum(PhotoCleanMode.NORMAL),
            homeMediaTab = sanitizeTab(this[Keys.homeMediaTab]),
            photoDateMode = sanitizeDateMode(this[Keys.photoDateMode]),
            photoMediaType = sanitizePhotoMediaType(this[Keys.photoMediaType]),
            photoSortOrder = sanitizeSortOrder(this[Keys.photoSortOrder]),
            photoRandomSeed = this[Keys.photoRandomSeed] ?: 1L,
            photoFolderPath = this[Keys.photoFolderPath].orEmpty(),
            videoDateMode = sanitizeDateMode(this[Keys.videoDateMode]),
            videoSortOrder = sanitizeSortOrder(this[Keys.videoSortOrder]),
            videoRandomSeed = this[Keys.videoRandomSeed] ?: 1L,
            videoFolderPath = this[Keys.videoFolderPath].orEmpty(),
            photoGuideShown = this[Keys.photoGuideShown] ?: false,
            videoGuideShown = this[Keys.videoGuideShown] ?: false,
            excludedFolderPaths = this[Keys.excludedFolderPaths] ?: emptySet(),
            onboardingShown = this[Keys.onboardingShown] ?: false,
            photoFocusMode = this[Keys.photoFocusMode] ?: false,
            photoShowTopBar = this[Keys.photoShowTopBar] ?: true,
            photoShowFilterChips = this[Keys.photoShowFilterChips] ?: true,
            photoShowFolderChips = this[Keys.photoShowFolderChips] ?: true,
            photoShowActionRail = this[Keys.photoShowActionRail] ?: true,
            photoShowInfoBar = this[Keys.photoShowInfoBar] ?: true,
            photoShowGestureHint = this[Keys.photoShowGestureHint] ?: true,
            photoShowShuffleButton = this[Keys.photoShowShuffleButton] ?: true,
            videoShowTopBar = this[Keys.videoShowTopBar] ?: true,
            videoShowActionRail = this[Keys.videoShowActionRail] ?: true,
            videoShowInfoPanel = this[Keys.videoShowInfoPanel] ?: true,
            videoShowFolderChips = this[Keys.videoShowFolderChips] ?: true,
            videoShowProgressBar = this[Keys.videoShowProgressBar] ?: true,
            videoShowShuffleButton = this[Keys.videoShowShuffleButton] ?: true,
            videoChromeVisible = this[Keys.videoChromeVisible] ?: true,
        )

    private suspend fun editString(key: androidx.datastore.preferences.core.Preferences.Key<String>, value: String) {
        context.kanlemeSettingsDataStore.edit { it[key] = value }
    }

    private suspend fun editBoolean(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        context.kanlemeSettingsDataStore.edit { it[key] = value }
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
