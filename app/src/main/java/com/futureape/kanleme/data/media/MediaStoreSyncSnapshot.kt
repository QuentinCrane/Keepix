package com.futureape.kanleme.data.media

import com.futureape.kanleme.data.local.MediaSyncStateEntity

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

/**
 * Returns the exclusive generation cursor only when the stored cursor is safe to compare.
 * A null result deliberately means that the caller must perform a full scan.
 */
fun MediaStoreSyncSnapshot.incrementalAfterGeneration(
    previous: MediaSyncStateEntity?,
    permissionScopeFingerprint: String,
): Long? {
    if (previous == null) return null
    if (previous.volumeName != volumeName) return null
    if (previous.mediaStoreVersion != mediaStoreVersion) return null
    if (previous.permissionScopeFingerprint != permissionScopeFingerprint) return null
    if (previous.lastGeneration < 0L || previous.lastGeneration > generation) return null
    return previous.lastGeneration
}
