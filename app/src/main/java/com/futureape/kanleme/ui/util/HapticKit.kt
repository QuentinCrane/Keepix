package com.futureape.kanleme.ui.util

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.runtime.Composable
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
    fun tick() {
        if (settings.swipeSound) view.playSoundEffect(SoundEffectConstants.CLICK)
        performLevel(settings.hapticLevel)
    }

    fun threshold() {
        performLevel(settings.hapticLevel, mediumConstant = HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun success() {
        if (settings.swipeSound) view.playSoundEffect(SoundEffectConstants.CLICK)
        performLevel(settings.hapticLevel, strongDouble = true)
    }

    fun keep() {
        action(settings.keepHapticLevel)
    }

    fun delete() {
        action(settings.deleteHapticLevel)
    }

    fun favorite() {
        action(settings.favoriteHapticLevel)
    }

    fun undo() {
        action(settings.undoHapticLevel)
    }

    private fun action(level: HapticLevel) {
        if (settings.swipeSound) view.playSoundEffect(SoundEffectConstants.CLICK)
        performLevel(level, strongDouble = true)
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
    return remember(view, settings) { HapticKit(view, settings) }
}
