package com.futureape.kanleme.performance

import android.os.Trace
import java.util.concurrent.atomic.AtomicInteger

/**
 * Small, dependency-free trace helpers for startup and long-running local work.
 * Async sections are used around suspend calls so the begin/end pair can cross a
 * dispatcher hop without changing application behavior.
 */
internal object StartupTrace {
    private val nextCookie = AtomicInteger(1)

    inline fun <T> section(name: String, block: () -> T): T {
        Trace.beginSection(name)
        return try {
            block()
        } finally {
            Trace.endSection()
        }
    }

    suspend inline fun <T> asyncSection(
        name: String,
        crossinline block: suspend () -> T,
    ): T {
        val cookie = nextCookie.getAndIncrement()
        Trace.beginAsyncSection(name, cookie)
        return try {
            block()
        } finally {
            Trace.endAsyncSection(name, cookie)
        }
    }
}
