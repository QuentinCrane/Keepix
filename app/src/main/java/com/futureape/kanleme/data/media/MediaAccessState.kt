package com.futureape.kanleme.data.media

/**
 * A snapshot of the media visibility granted by the Android system.
 *
 * The selected-media grant can stay granted while the user changes the selected
 * items in Settings. Callers must therefore refresh this snapshot on resume and
 * not persist it as the source of truth.
 */
data class MediaAccessState(
    val canReadImages: Boolean,
    val canReadVideos: Boolean,
    val canReadSelectedVisualMedia: Boolean,
) {
    val isGranted: Boolean get() = canReadImages || canReadVideos || canReadSelectedVisualMedia
    val isFullVisualAccess: Boolean get() = canReadImages && canReadVideos
    val isPartialVisualAccess: Boolean get() = !isFullVisualAccess && canReadSelectedVisualMedia
    val key: String get() = "images=$canReadImages;videos=$canReadVideos;selected=$canReadSelectedVisualMedia"
}
