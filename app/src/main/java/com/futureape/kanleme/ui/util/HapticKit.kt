package com.futureape.kanleme.ui.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.data.settings.HapticLevel

@Stable
class HapticKit(
    private val view: View,
    private val settings: AppSettings,
) {
    private var toneGenerator: ToneGenerator? = null

    fun tick() {
        playTone(ToneGenerator.TONE_PROP_BEEP, 24)
        performLevel(settings.hapticLevel)
    }

    fun threshold() {
        performLevel(settings.hapticLevel, mediumConstant = HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun success() {
        playTone(ToneGenerator.TONE_PROP_ACK, 32)
        performLevel(settings.hapticLevel, strongDouble = true)
    }

    fun keep() {
        action(settings.keepHapticLevel, ToneGenerator.TONE_PROP_BEEP)
    }

    fun delete() {
        action(settings.deleteHapticLevel, ToneGenerator.TONE_PROP_NACK)
    }

    fun favorite() {
        action(settings.favoriteHapticLevel, ToneGenerator.TONE_PROP_ACK)
    }

    fun undo() {
        action(settings.undoHapticLevel, ToneGenerator.TONE_PROP_BEEP)
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun action(level: HapticLevel, toneType: Int) {
        playTone(toneType, 28)
        performLevel(level, strongDouble = true)
    }

    private fun playTone(toneType: Int, durationMs: Int) {
        if (!settings.swipeSound) return
        val generator = toneGenerator ?: runCatching {
            ToneGenerator(AudioManager.STREAM_MUSIC, 58)
        }.getOrNull()?.also { toneGenerator = it }
        if (generator == null) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
            return
        }
        if (runCatching { generator.startTone(toneType, durationMs) }.getOrDefault(false)) return
        view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    private fun performLevel(
        level: HapticLevel,
        mediumConstant: Int = HapticFeedbackConstants.VIRTUAL_KEY,
        strongDouble: Boolean = false,
    ) {
        when (level) {
            HapticLevel.OFF -> Unit
            HapticLevel.LIGHT -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            HapticLevel.MEDIUM -> view.performHapticFeedback(mediumConstant)
            HapticLevel.STRONG -> {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                if (strongDouble) view.postDelayed({ view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }, 42L)
            }
        }
    }
}

@Composable
fun rememberHapticKit(settings: AppSettings): HapticKit {
    val view = LocalView.current
    val hapticKit = remember(view, settings) { HapticKit(view, settings) }
    DisposableEffect(hapticKit) {
        onDispose { hapticKit.release() }
    }
    return hapticKit
}
