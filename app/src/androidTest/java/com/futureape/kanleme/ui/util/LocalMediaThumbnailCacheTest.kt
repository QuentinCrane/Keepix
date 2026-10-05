package com.futureape.kanleme.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LocalMediaThumbnailCacheTest {
    @Test fun persistentThumbnailReusesTheGeneratedImageAndInvalidatesByVersion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val disk = DiskCache.Builder().directory(context.cacheDir.resolve("thumb-test-" + UUID.randomUUID()).path.toPath())
            .maxSizeBytes(1024 * 1024).build()
        var generated = 0
        var readable = true
        val loader = ImageLoader.Builder(context).diskCache(disk).components {
            add(LocalMediaThumbnailKeyer())
            add(LocalMediaThumbnailFetcher.Factory(
                load = { _, _ ->
                    generated++
                    Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(if (generated == 1) Color.RED else Color.BLUE)
                    }
                },
                validate = { _, _ -> if (!readable) throw SecurityException("Source access revoked") },
            ))
        }.build()
        fun request(version: String) = ImageRequest.Builder(context)
            .data(LocalMediaThumbnail(Uri.parse("content://media/test/images/media/1"), version, 2, 2))
            .memoryCachePolicy(CachePolicy.DISABLED).size(2, 2).build()
        try {
            assertTrue(loader.execute(request("v1")) is SuccessResult)
            val warm = loader.execute(request("v1")) as SuccessResult
            assertEquals(1, generated)
            assertEquals(Color.RED, (warm.image as BitmapImage).bitmap.getPixel(0, 0))
            assertTrue(loader.execute(request("v2")) is SuccessResult)
            assertEquals(2, generated)
            readable = false
            val denied = loader.execute(request("v1")) as ErrorResult
            assertTrue(denied.throwable is SecurityException)
            assertEquals(2, generated)
            readable = true
            val v1 = LocalMediaThumbnail(Uri.parse("content://media/test/images/media/1"), "v1", 2, 2)
            val editor = checkNotNull(disk.openEditor(v1.diskKey))
            disk.fileSystem.write(editor.metadata) { writeUtf8("local-thumbnail-v1") }
            disk.fileSystem.write(editor.data) { writeUtf8("broken thumbnail") }
            editor.commit()
            assertTrue(loader.execute(request("v1")) is SuccessResult)
            assertEquals(3, generated)
        } finally {
            disk.clear()
            loader.shutdown()
            disk.shutdown()
        }
    }
}
