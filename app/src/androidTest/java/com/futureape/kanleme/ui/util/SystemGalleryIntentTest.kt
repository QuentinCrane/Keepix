package com.futureape.kanleme.ui.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.futureape.kanleme.data.local.PhotoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemGalleryIntentTest {
    @Test
    fun motionPhotoOpensOriginalImageWithItsStorageVolumeAndReadGrant() {
        val context = RecordingContext(Activity())
        val uri = "content://media/1234-5678/images/media/42"

        openPhotoInSystemGallery(context, photo(uri).copy(
            isMotionPhoto = true,
            isSeparateVideo = true,
            motionVideoUri = "content://media/1234-5678/video/media/43",
        ))

        val intent = context.intents.single()
        assertEquals(MediaStore.ACTION_REVIEW, intent.action)
        assertEquals(Uri.parse(uri), intent.data)
        assertEquals("image/jpeg", intent.type)
        assertEquals(intent.data, intent.clipData!!.getItemAt(0).uri)
        assertEquals(intent.data, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertFalse(intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION != 0)
        assertFalse(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun unsupportedReviewFallsBackToViewOfTheSameImage() {
        val context = RecordingContext(Activity()) { intent ->
            if (intent.action == MediaStore.ACTION_REVIEW) throw ActivityNotFoundException()
        }
        val uri = "content://media/external_primary/images/media/42"

        openPhotoInSystemGallery(context, photo(uri))

        assertEquals(listOf(MediaStore.ACTION_REVIEW, Intent.ACTION_VIEW), context.intents.map { it.action })
        assertTrue(context.intents.all { it.data == Uri.parse(uri) && it.type == "image/jpeg" })
    }

    @Test
    fun rejectedExactMimeRetriesBroadMimeWithTheOriginalUri() {
        val context = RecordingContext(Activity()) { intent ->
            if (intent.action == MediaStore.ACTION_REVIEW) throw ActivityNotFoundException()
            if (intent.type == "image/jpeg") throw SecurityException()
        }
        val uri = "content://media/external/images/media/42"

        openPhotoInSystemGallery(context, photo(uri))

        assertEquals(3, context.intents.size)
        assertEquals(Intent.ACTION_VIEW, context.intents.last().action)
        assertEquals("image/*", context.intents.last().type)
        assertTrue(context.intents.all { it.data == Uri.parse(uri) })
    }

    @Test
    fun missingStoredUriUsesMediaStoreIdAndApplicationContextGetsNewTaskFlag() {
        val context = RecordingContext(ApplicationProvider.getApplicationContext<Context>())

        openPhotoInSystemGallery(context, photo(""))

        val intent = context.intents.single()
        assertEquals(Uri.parse("content://media/external/images/media/42"), intent.data)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    private class RecordingContext(
        base: Context,
        private val onLaunch: (Intent) -> Unit = {},
    ) : ContextWrapper(base) {
        val intents = mutableListOf<Intent>()

        override fun startActivity(intent: Intent) {
            intents += intent
            onLaunch(intent)
        }
    }

    private fun photo(uri: String) = PhotoEntity(
        id = 7,
        mediaStoreId = 42,
        uri = uri,
        displayName = "original.jpg",
        size = 1024,
        dateTaken = 1,
        dateAdded = 1,
        dateModified = 1,
        relativePath = "DCIM/Camera/",
        folderPath = "DCIM/Camera/",
        folderName = "Camera",
        mimeType = "image/jpeg",
        width = 100,
        height = 100,
    )
}
