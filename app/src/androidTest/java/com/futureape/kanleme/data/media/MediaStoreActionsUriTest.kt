package com.futureape.kanleme.data.media

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaStoreActionsUriTest {
    @Test
    fun isValidMediaStoreUri_rejectsFileAndAuthorityLessUris() {
        assertFalse(MediaStoreActions.isValidMediaStoreUri(Uri.parse("file:///private/photo.jpg")))
        assertFalse(MediaStoreActions.isValidMediaStoreUri(Uri.parse("content:/missing-authority")))
        assertTrue(MediaStoreActions.isValidMediaStoreUri(Uri.parse("content://media/external/images/media/1")))
    }
}
