package com.futureape.kanleme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.futureape.kanleme.ui.navigation.KanlemeApp
import com.futureape.kanleme.ui.theme.KanlemeTheme
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import com.futureape.kanleme.performance.StartupTrace
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var shortcutTarget by mutableStateOf<String?>(null)
    private var shortcutNonce by mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupTrace.section("kanleme.activity.onCreate") {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)
            // A configuration change recreates the Activity with the original shortcut Intent.
            // Replaying it would unexpectedly navigate away from the restored destination.
            if (savedInstanceState == null) handleShortcutIntent(intent)
            setContent {
                val viewModel: KanlemeViewModel = hiltViewModel()
                val settings = viewModel.settings.collectAsStateWithLifecycle().value
                KanlemeTheme(themeMode = settings.themeMode, accentColor = settings.accentColor) {
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
