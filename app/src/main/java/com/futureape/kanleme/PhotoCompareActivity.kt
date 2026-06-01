package com.futureape.kanleme

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import com.futureape.kanleme.ui.screens.PhotoCompareScreen
import com.futureape.kanleme.ui.theme.KanlemeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PhotoCompareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val uris = extractUris(intent)
        setContent {
            KanlemeTheme {
                PhotoCompareScreen(uris = uris, onBack = { finish() })
            }
        }
    }

    private fun extractUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            Intent.ACTION_SEND_MULTIPLE -> IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> emptyList()
        }
    }
}
