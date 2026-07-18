package com.futureape.kanleme.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import com.futureape.kanleme.R
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.settings.SwipeSoundStyle
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

class PhotoSwipeSoundPlayer(
    context: Context,
    private val enabled: Boolean,
    private val style: SwipeSoundStyle,
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val soundPool: SoundPool? = if (style == SwipeSoundStyle.ORIGINAL_WHOOSH) {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
    } else {
        null
    }
    private var soundId = 0
    private var loaded = false
    private var pendingAction: SwipeAction? = null
    private var released = false

    init {
        soundPool?.let { pool ->
            pool.setOnLoadCompleteListener { _, _, status ->
                loaded = status == 0
                val action = pendingAction
                pendingAction = null
                if (action != null) {
                    if (loaded) playOriginal(action) else playSoft(action)
                }
            }
            soundId = pool.load(context.applicationContext, R.raw.photo_swipe_delete_whoosh, 1)
        }
    }

    fun play(action: SwipeAction) {
        if (!enabled || released) return
        if (style == SwipeSoundStyle.ORIGINAL_WHOOSH) {
            if (soundId == 0 || !loaded) {
                pendingAction = action
            } else {
                playOriginal(action)
            }
            return
        }
        executor.execute {
            if (!released) playSynthesized(action)
        }
    }

    private fun playOriginal(action: SwipeAction) {
        val pool = soundPool ?: return
        val (volume, rate) = when (action) {
            SwipeAction.Keep -> 0.52f to 0.92f
            SwipeAction.Favorite -> 0.56f to 0.84f
            SwipeAction.Delete -> 0.60f to 0.76f
        }
        pool.play(soundId, volume, volume, 1, 0, rate)
    }

    private fun playSoft(action: SwipeAction) {
        executor.execute {
            if (!released) playSynthesized(action)
        }
    }

    private fun playSynthesized(action: SwipeAction) {
        val sampleRate = 44_100
        val durationMs = if (style == SwipeSoundStyle.SOFT_BREEZE) 82 else 54
        val sampleCount = sampleRate * durationMs / 1_000
        val pcm = ShortArray(sampleCount)
        val baseFrequency = when (action) {
            SwipeAction.Keep -> 208.0
            SwipeAction.Favorite -> 262.0
            SwipeAction.Delete -> 156.0
        }
        val amplitude = if (style == SwipeSoundStyle.SOFT_BREEZE) 0.17 else 0.13

        for (index in pcm.indices) {
            val progress = index.toDouble() / sampleCount
            val frequency = if (style == SwipeSoundStyle.SOFT_BREEZE) {
                baseFrequency * (1.08 - progress * 0.30)
            } else {
                baseFrequency * (1.02 - progress * 0.12)
            }
            val phase = 2.0 * PI * frequency * index / sampleRate
            val fundamental = sin(phase)
            val warmth = sin(phase * 0.5) * 0.22
            val attack = (progress / 0.08).coerceAtMost(1.0)
            val release = ((1.0 - progress) / 0.28).coerceAtMost(1.0)
            val envelope = attack * release
            pcm[index] = ((fundamental + warmth) * amplitude * envelope * Short.MAX_VALUE).toInt().toShort()
        }

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        }.getOrNull() ?: return

        runCatching {
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 12L)
        }
        track.release()
    }

    fun release() {
        released = true
        pendingAction = null
        soundPool?.release()
        executor.shutdownNow()
    }
}
