package com.futureape.kanleme

import android.app.Application
import android.os.Build
import coil.disk.DiskCache
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.futureape.kanleme.ui.util.IMAGE_DISK_CACHE_MAX_BYTES
import com.futureape.kanleme.ui.util.installCrashLogger
import com.futureape.kanleme.ui.util.trimAppCache
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class KanlemeApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        installCrashLogger(this)
        trimAppCache(this)
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .crossfade(false)
        .diskCache {
            DiskCache.Builder()
                .directory(File(cacheDir, "image_cache"))
                .maxSizeBytes(IMAGE_DISK_CACHE_MAX_BYTES)
                .build()
        }
        .components {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }
        .build()
}
