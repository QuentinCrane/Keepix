package com.futureape.kanleme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import com.futureape.kanleme.ui.navigation.KanlemeApp
import com.futureape.kanleme.ui.theme.KanlemeTheme
import com.futureape.kanleme.ui.util.clearAppCacheOnExit
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var shortcutTarget by mutableStateOf<String?>(null)
    private var shortcutNonce by mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleShortcutIntent(intent)
        setContent {
            val viewModel: KanlemeViewModel = hiltViewModel()
            val settings = viewModel.settings.collectAsStateWithLifecycle().value
            val baseDensity = LocalDensity.current
            val screenWidthDp = LocalConfiguration.current.screenWidthDp
            val densityScale = when {
                screenWidthDp >= 840 -> 1.02f
                screenWidthDp >= 600 -> 0.98f
                else -> 0.90f
            }
            val fontScale = when {
                screenWidthDp >= 600 -> 1.00f
                else -> 0.98f
            }
            val compactDensity = Density(
                density = baseDensity.density * densityScale,
                fontScale = baseDensity.fontScale * fontScale,
            )
            KanlemeTheme(themeMode = settings.themeMode, accentColor = settings.accentColor) {
                CompositionLocalProvider(LocalDensity provides compactDensity) {
                    KanlemeApp(
                        initialShortcutTarget = shortcutTarget,
                        shortcutNonce = shortcutNonce,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) clearAppCacheOnExit(this)
    }

    private fun extractShortcutTarget(intent: Intent?): String? {
        if (intent?.action != ACTION_CLEAN) return null
        return intent.getStringExtra(EXTRA_SHORTCUT_TARGET)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val target = extractShortcutTarget(intent) ?: return
        shortcutTarget = target
        shortcutNonce = System.nanoTime()
    }

    companion object {
        const val ACTION_CLEAN = "com.futureape.kanleme.ACTION_CLEAN"
        const val EXTRA_SHORTCUT_TARGET = "shortcut_target"
    }
}
