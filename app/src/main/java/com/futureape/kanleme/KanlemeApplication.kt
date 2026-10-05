package com.futureape.kanleme

import android.app.Application
import android.content.ComponentCallbacks2
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.futureape.kanleme.ui.util.IMAGE_DISK_CACHE_MAX_BYTES
import com.futureape.kanleme.ui.util.ImageLoadMetrics
import com.futureape.kanleme.ui.util.imageMemoryCacheMaxBytes
import com.futureape.kanleme.ui.util.LocalMediaThumbnailFetcher
import com.futureape.kanleme.ui.util.LocalMediaThumbnailKeyer
import com.futureape.kanleme.ui.util.isLowRamDevice
import com.futureape.kanleme.ui.util.installCrashLogger
import com.futureape.kanleme.ui.util.trimAppCache
import com.futureape.kanleme.performance.StartupTrace
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import java.io.File
import okio.Path.Companion.toPath

@HiltAndroidApp
class KanlemeApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    private var sharedImageLoader: ImageLoader? = null

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

    @Synchronized
    override fun newImageLoader(context: android.content.Context): ImageLoader {
        sharedImageLoader?.let { return it }
        return StartupTrace.section("kanleme.imageLoader.create") {
            val loader = ImageLoader.Builder(context)
                .crossfade(false)
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizeBytes(imageMemoryCacheMaxBytes(Runtime.getRuntime().maxMemory(), isLowRamDevice()))
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(File(context.cacheDir, "image_cache").path.toPath())
                        .maxSizeBytes(IMAGE_DISK_CACHE_MAX_BYTES)
                        .build()
                }
                .eventListenerFactory(ImageLoadMetrics.eventListenerFactory)
                .components {
                    add(LocalMediaThumbnailKeyer())
                    add(LocalMediaThumbnailFetcher.Factory())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
            sharedImageLoader = loader
            loader
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val cache = sharedImageLoader?.memoryCache ?: return
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> cache.clear()
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> cache.trimToSize(cache.maxSize / 4)
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> cache.trimToSize(cache.maxSize / 2)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        sharedImageLoader?.memoryCache?.clear()
    }
}
