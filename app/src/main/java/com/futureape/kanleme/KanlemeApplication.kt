package com.futureape.kanleme

import android.app.Application
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.disk.DiskCache
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.memory.MemoryCache
import com.futureape.kanleme.ui.util.IMAGE_DISK_CACHE_MAX_BYTES
import com.futureape.kanleme.ui.util.installCrashLogger
import com.futureape.kanleme.ui.util.trimAppCache
import com.futureape.kanleme.performance.StartupTrace
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import java.io.File

@HiltAndroidApp
class KanlemeApplication : Application(), ImageLoaderFactory, Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.INFO)
            .build()

    override fun onCreate() {
        StartupTrace.section("kanleme.application.onCreate") {
            super.onCreate()
            installCrashLogger(this)
            trimAppCache(this)
        }
    }

    override fun newImageLoader(): ImageLoader = StartupTrace.section("kanleme.imageLoader.create") {
        ImageLoader.Builder(this)
            .crossfade(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.30)
                    .build()
            }
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
}
