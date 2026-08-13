package com.futureape.kanleme.data.media

import com.futureape.kanleme.data.local.MediaSyncStateEntity
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
            current.incrementalAfterGeneration(previous(), "images=true;videos=true;selected=false"),
        )
    }

    @Test
    fun firstSyncAndChangedIdentityRequireFullScan() {
        val scope = "images=true;videos=true;selected=false"
        assertNull(current.incrementalAfterGeneration(null, scope))
        assertNull(current.incrementalAfterGeneration(previous(mediaStoreVersion = "version-1"), scope))
        assertNull(current.incrementalAfterGeneration(previous(permissionScopeFingerprint = "images=true;videos=false;selected=false"), scope))
        assertNull(current.incrementalAfterGeneration(previous(lastGeneration = 101L), scope))
    }
}
