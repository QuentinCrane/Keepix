package com.futureape.kanleme.data.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaAccessStateTest {
    @Test
    fun fullAccessRequiresBothPhotoAndVideoGrants() {
        val access = MediaAccessState(
            canReadImages = true,
            canReadVideos = true,
            canReadSelectedVisualMedia = false,
        )

        assertTrue(access.isGranted)
        assertTrue(access.isFullVisualAccess)
        assertFalse(access.isPartialVisualAccess)
    }

    @Test
    fun selectedMediaAccessIsPartialEvenWhenOnlyOneTypeIsFull() {
        val access = MediaAccessState(
            canReadImages = true,
            canReadVideos = false,
            canReadSelectedVisualMedia = true,
        )

        assertTrue(access.isGranted)
        assertFalse(access.isFullVisualAccess)
        assertTrue(access.isPartialVisualAccess)
    }

    @Test
    fun imageOnlyAndVideoOnlyStatesHaveDistinctKeys() {
        val images = MediaAccessState(true, false, false)
        val videos = MediaAccessState(false, true, false)

        assertTrue(images.isGranted)
        assertTrue(videos.isGranted)
        assertFalse(images.key == videos.key)
    }
}
