package com.futureape.kanleme

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.futureape.kanleme.ui.screens.PhotoCompareScreen
import com.futureape.kanleme.ui.theme.KanlemeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class PhotoCompareActivity : ComponentActivity() {
    private var sharedUris by mutableStateOf<List<Uri>>(emptyList())
    private var validating by mutableStateOf(false)
    private var validationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KanlemeTheme {
                PhotoCompareScreen(
                    uris = sharedUris,
                    isValidating = validating,
                    onBack = { finish() },
                )
            }
        }
        validateIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        validateIntent(intent)
    }

    private fun validateIntent(intent: Intent?) {
        validationJob?.cancel()
        sharedUris = emptyList()
        validating = true
        validationJob = lifecycleScope.launch {
            val candidates = PhotoCompareUriValidator.collectCandidateUris(intent)
            val validUris = withContext(Dispatchers.IO) {
                PhotoCompareUriValidator.validate(contentResolver, candidates)
            }
            sharedUris = validUris
            validating = false
        }
    }
}
