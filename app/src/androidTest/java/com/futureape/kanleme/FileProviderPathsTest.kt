package com.futureape.kanleme

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileProviderPathsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val authority = context.packageName + ".fileprovider"

    @Test
    fun onlyPurposeBuiltDirectoriesAreShareable() {
        assertNotNull(
            FileProvider.getUriForFile(
                context,
                authority,
                File(context.cacheDir, "daily-reports/report.png"),
            ),
        )
        assertNotNull(
            FileProvider.getUriForFile(
                context,
                authority,
                File(context.filesDir, "crash_logs/crash.txt"),
            ),
        )

        assertProviderRejects(File(context.cacheDir, "image_cache/private.jpg"))
        assertProviderRejects(File(context.filesDir, "private.db"))
    }

    private fun assertProviderRejects(file: File) {
        try {
            FileProvider.getUriForFile(context, authority, file)
            throw AssertionError("FileProvider unexpectedly exposed " + file.path)
        } catch (_: IllegalArgumentException) {
            // Expected: the path is outside the explicit sharing roots.
        }
    }
}
