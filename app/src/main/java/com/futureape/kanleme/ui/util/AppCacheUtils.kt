package com.futureape.kanleme.ui.util

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.concurrent.thread

const val IMAGE_DISK_CACHE_MAX_BYTES = 32L * 1024L * 1024L
const val MOTION_PREVIEW_CACHE_MAX_BYTES = 24L * 1024L * 1024L
private const val DAILY_REPORT_CACHE_MAX_BYTES = 4L * 1024L * 1024L

private const val TAG = "AppCacheUtils"

fun trimAppCache(context: Context) {
    val appContext = context.applicationContext
    thread(start = true, isDaemon = true, name = "kanleme-cache-trimmer") {
        trimDirectory(File(appContext.cacheDir, "image_cache"), IMAGE_DISK_CACHE_MAX_BYTES)
        trimDirectory(File(appContext.cacheDir, "motion_photo_preview"), MOTION_PREVIEW_CACHE_MAX_BYTES)
        trimDirectory(File(appContext.cacheDir, "daily-reports"), DAILY_REPORT_CACHE_MAX_BYTES)
    }
}

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

private fun trimDirectory(dir: File, maxBytes: Long) {
    val files = dir.walkTopDown().filter { it.isFile }.toList()
    var total = files.sumOf { it.length() }
    files.sortedBy { it.lastModified() }.forEach { file ->
        if (total <= maxBytes) return
        val size = file.length()
        runCatching { if (file.delete()) total -= size }
            .onFailure { Log.w(TAG, "Failed to trim cache entry: " + file.absolutePath, it) }
    }
}
