package com.futureape.kanleme.ui.util

import android.content.Context
import android.media.AudioManager
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.settings.SwipeSoundStyle

/**
 * Plays platform-provided feedback for a committed swipe. No third-party audio binary is
 * bundled, so system volume and sound-effect settings remain the source of truth.
 */
class PhotoSwipeSoundPlayer(
    context: Context,
    private val enabled: Boolean,
    style: SwipeSoundStyle,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val soundEffect = style.soundEffect
    private var released = false

    fun play(@Suppress("UNUSED_PARAMETER") action: SwipeAction) {
        if (!enabled || released) return
        runCatching { audioManager.playSoundEffect(soundEffect) }
    }

    fun release() {
        released = true
    }
}

private val SwipeSoundStyle.soundEffect: Int
    get() = when (this) {
        SwipeSoundStyle.SOFT_BREEZE -> AudioManager.FX_FOCUS_NAVIGATION_RIGHT
        SwipeSoundStyle.LOW_TAP -> AudioManager.FX_KEY_CLICK
        SwipeSoundStyle.ORIGINAL_WHOOSH -> AudioManager.FX_FOCUS_NAVIGATION_UP
    }
