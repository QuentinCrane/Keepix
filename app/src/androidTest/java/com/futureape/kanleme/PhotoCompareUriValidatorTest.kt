package com.futureape.kanleme

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoCompareUriValidatorTest {
    @Test
    fun collectsAndDeduplicatesStreamAndClipUris() {
        val first = Uri.parse("content://photos/1")
        val second = Uri.parse("content://photos/2")
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(first, second))
            clipData = ClipData.newRawUri("shared", first).apply {
                addItem(ClipData.Item(second))
            }
        }

        assertEquals(
            listOf(first, second),
            PhotoCompareUriValidator.collectCandidateUris(intent),
        )
    }

    @Test
    fun rejectsNonContentUrisBeforeOpeningAProvider() {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        val candidates = listOf(
            Uri.parse("file:///sdcard/private.jpg"),
            Uri.parse("https://example.com/photo.jpg"),
            Uri.parse("content://photos/valid-but-unavailable"),
        )

        assertTrue(PhotoCompareUriValidator.validate(resolver, candidates).isEmpty())
        assertFalse(PhotoCompareUriValidator.isContentUri(Uri.parse("file:///sdcard/private.jpg")))
        assertTrue(PhotoCompareUriValidator.isContentUri(Uri.parse("content://photos/1")))
    }

    @Test
    fun rejectsMoreThanFourCandidates() {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        val candidates = (1..5).map { Uri.parse("content://photos/" + it) }

        assertTrue(PhotoCompareUriValidator.validate(resolver, candidates).isEmpty())
    }
}
