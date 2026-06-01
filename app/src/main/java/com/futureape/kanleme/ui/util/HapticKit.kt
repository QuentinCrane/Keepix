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
        when (settings.hapticLevel) {
            HapticLevel.OFF -> Unit
            HapticLevel.LIGHT -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            HapticLevel.MEDIUM -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            HapticLevel.STRONG -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun threshold() {
        when (settings.hapticLevel) {
            HapticLevel.OFF -> Unit
            HapticLevel.LIGHT -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            HapticLevel.MEDIUM -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            HapticLevel.STRONG -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    fun success() {
        if (settings.swipeSound) view.playSoundEffect(SoundEffectConstants.CLICK)
        when (settings.hapticLevel) {
            HapticLevel.OFF -> Unit
            HapticLevel.LIGHT -> view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            HapticLevel.MEDIUM -> view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            HapticLevel.STRONG -> {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                view.postDelayed({ view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }, 42L)
            }
        }
    }
}

@Composable
fun rememberHapticKit(settings: AppSettings): HapticKit {
    val view = LocalView.current
    return remember(view, settings) { HapticKit(view, settings) }
}
