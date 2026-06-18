package com.futureape.kanleme.ui.util

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.concurrent.thread

const val IMAGE_DISK_CACHE_MAX_BYTES = 64L * 1024L * 1024L
const val MOTION_PREVIEW_CACHE_MAX_BYTES = 64L * 1024L * 1024L

private const val TAG = "AppCacheUtils"

fun clearAppCacheOnExit(context: Context) {
    val appContext = context.applicationContext
    thread(
        start = true,
        isDaemon = false,
        name = "kanleme-cache-cleaner",
    ) {
        listOfNotNull(appContext.cacheDir, appContext.externalCacheDir).forEach { dir ->
            runCatching { dir.deleteChildren() }
                .onFailure { Log.w(TAG, "Failed to clear cache directory: " + dir.absolutePath, it) }
        }
    }
}

private fun File.deleteChildren() {
    listFiles()?.forEach { child ->
        runCatching { child.deleteRecursively() }
            .onFailure { Log.w(TAG, "Failed to delete cache entry: " + child.absolutePath, it) }
    }
}
