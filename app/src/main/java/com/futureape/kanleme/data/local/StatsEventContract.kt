package com.futureape.kanleme.data.local

object CleanupEventType {
    const val KEPT = "kept"
    const val FAVORITED = "favorited"
    const val MOVED_TO_APP_TRASH = "moved_to_app_trash"
    const val RESTORED = "restored"
    const val COMPENSATED = "compensated"
    const val PERMANENTLY_DELETED = "permanently_deleted"
    const val ARCHIVED_MOVED = "archived_moved"
}

object CleanupEventSourceType {
    const val ORGANIZER = "organizer"
    const val UNDO = "undo"
    const val TRASH = "trash"
    const val ARCHIVE = "archive"
}

data class CleanupEventMetrics(
    val photoDelta: Int = 0,
    val videoDelta: Int = 0,
    val affectedMediaCount: Int = 1,
    val mediaSizeBytes: Long = 0L,
    val sizeDeltaBytes: Long = 0L,
    val favoriteDelta: Int = 0,
    val actionCount: Int = 1,
) {
    fun compensation(): CleanupEventMetrics = copy(
        photoDelta = -photoDelta,
        videoDelta = -videoDelta,
        sizeDeltaBytes = -sizeDeltaBytes,
        favoriteDelta = -favoriteDelta,
        actionCount = 1,
    )
}

fun cleanupEventTypeForAction(action: String): String = when (action) {
    "Keep" -> CleanupEventType.KEPT
    "Favorite" -> CleanupEventType.FAVORITED
    "Delete" -> CleanupEventType.MOVED_TO_APP_TRASH
    else -> CleanupEventType.COMPENSATED
}
