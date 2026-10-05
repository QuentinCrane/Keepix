package com.futureape.kanleme.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.futureape.kanleme.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AdvancedLicensesScreen(onBack: () -> Unit) {
    val resources = LocalResources.current
    val uriHandler = LocalUriHandler.current
    val license by produceState("", resources) {
        value = withContext(Dispatchers.IO) {
            resources.openRawResource(R.raw.apache_2_0).bufferedReader().use { it.readText() }
        }
    }
    AdvancedPage(stringResource(R.string.advanced_licenses), onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(stringResource(R.string.advanced_license_credit), style = MaterialTheme.typography.bodyLarge) }
            item {
                TextButton(onClick = { runCatching { uriHandler.openUri("https://github.com/androidx/media/tree/1.11.0") } }) {
                    Text(stringResource(R.string.advanced_source))
                }
            }
            item { Text(stringResource(R.string.advanced_license_text), style = MaterialTheme.typography.titleMedium) }
            item { SelectionContainer { Text(license, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) } }
        }
    }
}
