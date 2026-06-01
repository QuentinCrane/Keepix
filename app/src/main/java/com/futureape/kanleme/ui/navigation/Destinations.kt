package com.futureape.kanleme.ui.navigation

object Destinations {
    const val HOME = "home"
    const val PHOTO_START = "photo_start"
    const val PHOTO = "photo_clean"
    const val VIDEO_START = "video_start"
    const val VIDEO = "video"
    const val TIMELINE = "timeline"
    const val VIEWER_ARG_PHOTO_ID = "photoId"
    const val VIEWER = "viewer/{$VIEWER_ARG_PHOTO_ID}"
    const val SIMILAR = "similar"
    const val ME = "me"
    const val FAVORITES = "favorites"
    const val TRASH = "trash"
    const val SETTINGS = "settings"
    const val SETTINGS_PAGE_ARG = "settingsPage"
    const val SETTINGS_WITH_PAGE = "settings/{$SETTINGS_PAGE_ARG}"
    const val TODAY = "today"
    const val ACHIEVEMENTS = "achievements"
    const val ANNUAL = "annual"

    fun photoViewer(photoId: Long): String = "viewer/$photoId"
    fun settingsPage(page: String): String = "settings/$page"
}
