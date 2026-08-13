package com.futureape.kanleme.ui.screens

import android.os.SystemClock
import android.os.Trace
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Media-free player lifecycle observation for Perfetto/debug diagnostics.
 * It deliberately does not retain video ids or URIs.
 */
internal object VideoPlaybackMetrics {
    private const val TRACE_NAME = "kanleme.video.playerToReady"
    private val nextCookie = AtomicInteger(1)
    private val created = AtomicLong()
    private val ready = AtomicLong()
    private val errors = AtomicLong()
    private val released = AtomicLong()
    private val totalReadyMillis = AtomicLong()

    fun startSession(): Session {
        created.incrementAndGet()
        val cookie = nextCookie.getAndIncrement()
        Trace.beginAsyncSection(TRACE_NAME, cookie)
        return Session(cookie, SystemClock.elapsedRealtime())
    }

    internal fun snapshot(): Snapshot = Snapshot(
        created = created.get(),
        ready = ready.get(),
        errors = errors.get(),
        released = released.get(),
        totalReadyMillis = totalReadyMillis.get(),
    )

    internal class Session internal constructor(
        private val cookie: Int,
        private val startedAtMillis: Long,
    ) : Player.Listener {
        private var readyRecorded = false
        private var traceClosed = false

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && !readyRecorded) {
                readyRecorded = true
                ready.incrementAndGet()
                totalReadyMillis.addAndGet((SystemClock.elapsedRealtime() - startedAtMillis).coerceAtLeast(0L))
                closeTrace()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            errors.incrementAndGet()
            closeTrace()
        }

        fun release() {
            released.incrementAndGet()
            closeTrace()
        }

        private fun closeTrace() {
            if (traceClosed) return
            traceClosed = true
            Trace.endAsyncSection(TRACE_NAME, cookie)
        }
    }

    internal data class Snapshot(
        val created: Long,
        val ready: Long,
        val errors: Long,
        val released: Long,
        val totalReadyMillis: Long,
    )
}
