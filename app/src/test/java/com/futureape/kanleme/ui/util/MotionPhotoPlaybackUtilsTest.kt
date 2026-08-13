package com.futureape.kanleme.ui.util

import com.futureape.kanleme.data.media.DynamicPhotoDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPhotoPlaybackUtilsTest {
    @Test
    fun cacheIdentityIsStableAndChangesWhenMediaVersionChanges() {
        val first = motionPhotoCacheIdentity(
            stableMediaKey = "image:7:100",
            mediaStoreId = 7L,
            uri = "content://media/7",
            size = 42_000L,
            dateModified = 100L,
            motionVideoOffset = 30_000L,
            motionVideoSize = 12_000L,
        )
        val same = motionPhotoCacheIdentity(
            stableMediaKey = "image:7:100",
            mediaStoreId = 7L,
            uri = "content://media/7",
            size = 42_000L,
            dateModified = 100L,
            motionVideoOffset = 30_000L,
            motionVideoSize = 12_000L,
        )
        val changed = motionPhotoCacheIdentity(
            stableMediaKey = "image:7:101",
            mediaStoreId = 7L,
            uri = "content://media/7",
            size = 42_100L,
            dateModified = 101L,
            motionVideoOffset = 30_000L,
            motionVideoSize = 12_100L,
        )

        assertEquals(first, same)
        assertNotEquals(first, changed)
        assertEquals(64, first.length)
        assertTrue(first.all { it in "0123456789abcdef" })
    }

    @Test
    fun syntheticMotionTailFixtureRequiresMovieBoxAfterFtyp() {
        val valid = ByteArray(128)
        valid[0] = 0
        valid[1] = 0
        valid[2] = 0
        valid[3] = 16
        "ftypisom".toByteArray().copyInto(valid, 4)
        "moov".toByteArray().copyInto(valid, 32)

        val damaged = valid.copyOf().apply {
            "xxxx".toByteArray().copyInto(this, 32)
        }

        assertNotNull(DynamicPhotoDetector.findEmbeddedMp4Start(valid))
        assertNull(DynamicPhotoDetector.findEmbeddedMp4Start(damaged))
    }
}
