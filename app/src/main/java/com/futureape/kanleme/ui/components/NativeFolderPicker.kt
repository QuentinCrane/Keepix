package com.futureape.kanleme.ui.components

import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.futureape.kanleme.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile

@Composable
fun NativeFolderExcludeButton(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onFolderSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull().orEmpty()
            val documentName = runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull().orEmpty()
            val rule = folderRuleFromNativeSelection(treeDocumentId, documentName, uri.toString())
            if (rule.isNotBlank()) onFolderSelected(rule)
        }
    }
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    val container = if (oledDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    val border = if (oledDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.62f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    Surface(
        modifier = modifier.fillMaxWidth().clickable { launcher.launch(null) },
        shape = RoundedCornerShape(22.dp),
        color = container,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = if (oledDark) 0.18f else 0.14f)) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.padding(9.dp).size(22.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun folderRuleFromNativeSelection(treeDocumentId: String, documentName: String, fallback: String): String {
    val body = treeDocumentId.substringAfter(':', missingDelimiterValue = treeDocumentId)
        .replace(':', '/')
        .replace('\\', '/')
        .trim('/')
    if (body.isNotBlank() && body != "primary") return body
    val name = documentName.replace('\\', '/').trim('/')
    if (name.isNotBlank()) return name
    return fallback.substringAfterLast('/').substringAfterLast(':').trim()
}
