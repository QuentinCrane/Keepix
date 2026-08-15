package com.futureape.kanleme.data.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicPhotoDetectorTest {
    @Test
    fun vendorNamesAreRecognizedWithoutTreatingGifAsMotionPhoto() {
        assertTrue(DynamicPhotoDetector.hasMotionNameHint("MVIMG_0001.jpg", "DCIM/Camera"))
        assertTrue(DynamicPhotoDetector.hasMotionNameHint("IMG_0002.jpg", "DCIM/动态照片"))
        assertTrue(DynamicPhotoDetector.isPossiblyAnimatedStill("image/heic", "IMG_0003.heic"))
        assertTrue(DynamicPhotoDetector.isGif("image/gif", "motion.gif"))
        assertTrue(DynamicPhotoDetector.isPossiblyAnimatedStill("image/gif", "motion.gif"))
    }

    @Test
    fun stillImageBrandsDoNotBecomeEmbeddedVideo() {
        val heicContainer = boxFixture("heic")
        val avifContainer = boxFixture("avif")

        assertFalse(DynamicPhotoDetector.findEmbeddedMp4Start(heicContainer) != null)
        assertFalse(DynamicPhotoDetector.findEmbeddedMp4Start(avifContainer) != null)
    }

    @Test
    fun choosesTheLastValidMovieSegmentAfterImagePayload() {
        val fixture = ByteArray(192)
        writeMovieBox(fixture, 8, "isom")
        writeMovieBox(fixture, 96, "mp42")

        assertNotNull(DynamicPhotoDetector.findEmbeddedMp4Start(fixture))
        assertTrue(DynamicPhotoDetector.findEmbeddedMp4Start(fixture) == 96)
    }

    private fun boxFixture(brand: String): ByteArray = ByteArray(64).also {
        writeMovieBox(it, 8, brand)
    }

    private fun writeMovieBox(bytes: ByteArray, start: Int, brand: String) {
        bytes[start] = 0
        bytes[start + 1] = 0
        bytes[start + 2] = 0
        bytes[start + 3] = 16
        "ftyp".toByteArray().copyInto(bytes, start + 4)
        brand.padEnd(4, ' ').take(4).toByteArray().copyInto(bytes, start + 8)
        "moov".toByteArray().copyInto(bytes, start + 24)
    }
}
