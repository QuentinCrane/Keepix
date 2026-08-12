package com.futureape.kanleme.ui.screens

import androidx.compose.runtime.Immutable
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.data.settings.PhotoCleanMode

/**
 * Runtime consequences of the photo cleaning settings.
 *
 * The settings page can describe these values without pretending that a label
 * change is a feature. The policy is also deliberately small so it can be
 * tested without composing a full cleaning screen.
 */
@Immutable
data class PhotoCleanRuntimePolicy(
    val showImmersiveBackground: Boolean,
    val animateEntry: Boolean,
    val showDiagnostics: Boolean,
    val prefetchCount: Int,
)

fun AppSettings.toPhotoCleanRuntimePolicy(): PhotoCleanRuntimePolicy = when (photoCleanMode) {
    PhotoCleanMode.NORMAL -> PhotoCleanRuntimePolicy(
        showImmersiveBackground = immersiveBackground,
        animateEntry = true,
        showDiagnostics = false,
        prefetchCount = 3,
    )
    PhotoCleanMode.DIAGNOSTIC -> PhotoCleanRuntimePolicy(
        showImmersiveBackground = immersiveBackground,
        animateEntry = true,
        showDiagnostics = true,
        prefetchCount = 3,
    )
    PhotoCleanMode.PERFORMANCE -> PhotoCleanRuntimePolicy(
        showImmersiveBackground = false,
        animateEntry = false,
        showDiagnostics = false,
        prefetchCount = 1,
    )
}
