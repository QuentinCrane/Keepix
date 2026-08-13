package com.futureape.kanleme.data.media

import com.futureape.kanleme.data.local.MediaSyncStateEntity
import com.futureape.kanleme.data.local.MediaSyncCheckpointState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaStoreSyncSnapshotTest {
    private val current = MediaStoreSyncSnapshot(
        volumeName = "external",
        mediaStoreVersion = "version-2",
        generation = 100L,
    )

    private fun previous(
        volumeName: String = "external",
        mediaStoreVersion: String = "version-2",
        lastGeneration: Long = 80L,
        permissionScopeFingerprint: String = "images=true;videos=true;selected=false",
    ) = MediaSyncStateEntity(
        syncKey = volumeName,
        volumeName = volumeName,
        mediaStoreVersion = mediaStoreVersion,
        lastGeneration = lastGeneration,
        permissionScopeFingerprint = permissionScopeFingerprint,
        updatedAt = 1L,
    )

    @Test
    fun matchingStateReturnsExclusiveGenerationCursor() {
        assertEquals(
            80L,
            current.incrementalAfterGeneration(
                previous(),
                "images=true;videos=true;selected=false",
                allowIncrementalSync = true,
            ),
        )
    }

    @Test
    fun firstSyncAndChangedIdentityRequireFullScan() {
        val scope = "images=true;videos=true;selected=false"
        assertNull(current.incrementalAfterGeneration(null, scope, allowIncrementalSync = true))
        assertNull(current.incrementalAfterGeneration(previous(mediaStoreVersion = "version-1"), scope, allowIncrementalSync = true))
        assertNull(current.incrementalAfterGeneration(previous(permissionScopeFingerprint = "images=true;videos=false;selected=false"), scope, allowIncrementalSync = true))
        assertNull(current.incrementalAfterGeneration(previous(lastGeneration = 101L), scope, allowIncrementalSync = true))
        assertNull(current.incrementalAfterGeneration(previous(), scope, allowIncrementalSync = false))
        assertNull(
            current.incrementalAfterGeneration(
                previous().copy(checkpointState = MediaSyncCheckpointState.RUNNING),
                scope,
                allowIncrementalSync = true,
            ),
        )
    }
}
