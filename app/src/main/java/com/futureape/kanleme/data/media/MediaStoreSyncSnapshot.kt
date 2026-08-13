package com.futureape.kanleme.data.media

/**
 * The MediaStore cursor that a completed catalog sync can resume from.
 *
 * The generation is only comparable while the version and volume stay the same.
 */
data class MediaStoreSyncSnapshot(
    val volumeName: String,
    val mediaStoreVersion: String,
    val generation: Long,
)
