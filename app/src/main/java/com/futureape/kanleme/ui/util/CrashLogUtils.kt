package com.futureape.kanleme.ui.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CRASH_LOG_DIR = "crash_logs"
private const val MAX_CRASH_LOGS = 8

fun installCrashLogger(context: Context) {
    val appContext = context.applicationContext
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        writeCrashLog(appContext, thread, throwable)
        previous?.uncaughtException(thread, throwable)
    }
}

fun shareCrashLogs(context: Context): Boolean {
    val logFile = mergedCrashLogFile(context) ?: return false
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", logFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(context.contentResolver, "看了么闪退日志", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "导出闪退日志"))
    return true
}

private fun writeCrashLog(context: Context, thread: Thread, throwable: Throwable) {
    runCatching {
        val dir = File(context.filesDir, CRASH_LOG_DIR).apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        File(dir, "crash-" + stamp + ".txt").writeText(crashText(thread, throwable))
        dir.listFiles { file -> file.isFile && file.name.startsWith("crash-") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_CRASH_LOGS)
            ?.forEach { it.delete() }
    }
}

private fun mergedCrashLogFile(context: Context): File? {
    val dir = File(context.filesDir, CRASH_LOG_DIR)
    val logs = dir.listFiles { file -> file.isFile && file.name.startsWith("crash-") }
        ?.sortedByDescending { it.lastModified() }
        ?: return null
    if (logs.isEmpty()) return null
    val out = File(dir, "kanleme-crash-logs.txt")
    out.writeText(logs.joinToString(separator = "\n\n") { it.readText() })
    return out
}

private fun crashText(thread: Thread, throwable: Throwable): String {
    val stack = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
    return buildString {
        appendLine("time=" + SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()))
        appendLine("thread=" + thread.name)
        appendLine("device=" + Build.MANUFACTURER + " " + Build.MODEL)
        appendLine("android=" + Build.VERSION.RELEASE + " sdk=" + Build.VERSION.SDK_INT)
        appendLine()
        append(stack)
    }
}
