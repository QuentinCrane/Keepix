package com.futureape.kanleme.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.futureape.kanleme.R
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.settings.SwipeSoundStyle

/**
 * Plays bundled, pre-recorded swipe sounds. No swipe sound is synthesized at runtime.
 * Sources are the three Universfield swoosh files selected by the product owner.
 */
class PhotoSwipeSoundPlayer(
    context: Context,
    private val enabled: Boolean,
    style: SwipeSoundStyle,
) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .build()
    private var soundId = 0
    private var loaded = false
    private var pendingPlay = false
    private var released = false

    init {
        soundPool.setOnLoadCompleteListener { _, _, status ->
            loaded = status == 0
            if (pendingPlay && loaded && !released) {
                pendingPlay = false
                playLoaded()
            }
        }
        soundId = soundPool.load(context.applicationContext, style.soundRes, 1)
    }

    fun play(@Suppress("UNUSED_PARAMETER") action: SwipeAction) {
        if (!enabled || released) return
        if (loaded) {
            playLoaded()
        } else {
            pendingPlay = true
        }
    }

    private fun playLoaded() {
        // Each source is authored with its own motion and pitch contour. Keep its native rate.
        soundPool.play(soundId, 0.46f, 0.46f, 1, 0, 1f)
    }

    fun release() {
        released = true
        pendingPlay = false
        soundPool.release()
    }
}

private val SwipeSoundStyle.soundRes: Int
    get() = when (this) {
        SwipeSoundStyle.SOFT_BREEZE -> R.raw.photo_swipe_soft_glide_source
        SwipeSoundStyle.LOW_TAP -> R.raw.photo_swipe_short_glide_source
        SwipeSoundStyle.ORIGINAL_WHOOSH -> R.raw.photo_swipe_descending_glide_source
    }
