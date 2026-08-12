package com.futureape.kanleme.ui.navigation

/**
 * Stable entry mapping for external shortcuts and the primary navigation shell.
 *
 * The shortcut target is an intent-level value; callers must use a real route
 * returned here instead of inventing an intermediate start destination.
 */
object NavigationContract {
    val primaryRoutes = listOf(
        Destinations.HOME,
        Destinations.VIDEO,
        Destinations.ME,
    )

    fun routeForShortcut(target: String?): String? = when (target) {
        "photo" -> Destinations.PHOTO
        "video" -> Destinations.VIDEO
        else -> null
    }
}
