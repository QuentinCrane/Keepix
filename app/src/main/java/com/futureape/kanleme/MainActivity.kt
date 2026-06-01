package com.futureape.kanleme

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import com.futureape.kanleme.ui.navigation.KanlemeApp
import com.futureape.kanleme.ui.theme.KanlemeTheme
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
                    KanlemeApp(initialShortcutTarget = extractShortcutTarget(intent), viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractShortcutTarget(intent: Intent?): String? {
        if (intent?.action != ACTION_CLEAN) return null
        return intent.getStringExtra(EXTRA_SHORTCUT_TARGET)
    }

    companion object {
        const val ACTION_CLEAN = "com.futureape.kanleme.ACTION_CLEAN"
        const val EXTRA_SHORTCUT_TARGET = "shortcut_target"
    }
}
