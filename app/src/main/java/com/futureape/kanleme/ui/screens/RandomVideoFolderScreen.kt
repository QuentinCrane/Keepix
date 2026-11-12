package com.futureape.kanleme.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.util.advancedVideoInitialFolder
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import com.futureape.kanleme.ui.viewmodel.RandomFolderError
import com.futureape.kanleme.ui.viewmodel.RandomVideoFolderSession

@Composable
fun RandomVideoFolderScreen(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    RandomVideoFolderContent(viewModel.randomVideoFolder, onBack, onPickerError = {
        viewModel.showMessage(R.string.random_video_picker_error)
    })
}

@Composable
internal fun RandomVideoFolderContent(session: RandomVideoFolderSession, onBack: () -> Unit, onPickerError: () -> Unit) {
    val context = LocalContext.current
    val resolver = remember(context) { context.applicationContext.contentResolver }
    val state by session.state.collectAsStateWithLifecycle()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        if (folder != null) session.selectFolder(resolver, folder)
    }

    DisposableEffect(session, context) {
        onDispose { if (!context.isRandomVideoConfigurationChange()) session.leave() }
    }

    AdvancedPage(stringResource(R.string.random_video_title), onBack, stringResource(R.string.random_video_subtitle)) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ProfilePanel(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(stringResource(R.string.random_video_intro), style = MaterialTheme.typography.bodyLarge)
                        if (state.folder != null && state.folderName.isNotBlank()) {
                            Text(stringResource(R.string.random_video_folder, state.folderName), style = MaterialTheme.typography.titleMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(enabled = !state.scanning) {
                                session.setIncludeSubfolders(!state.includeSubfolders)
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = state.includeSubfolders,
                                onCheckedChange = session::setIncludeSubfolders,
                                enabled = !state.scanning,
                            )
                            Text(stringResource(R.string.advanced_include_subfolders))
                        }
                        Button(
                            onClick = {
                                try {
                                    folderPicker.launch(advancedVideoInitialFolder())
                                } catch (_: Exception) {
                                    onPickerError()
                                }
                            },
                            enabled = !state.scanning,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(if (state.folder == null) R.string.random_video_choose else R.string.random_video_choose_another))
                        }
                        if (state.folder != null && !state.scanning) {
                            if (state.videos.isNotEmpty()) {
                                Button(onClick = session::openPlayer, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.random_video_play_list))
                                }
                            }
                            OutlinedButton(onClick = { session.rescan(resolver) }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.random_video_rescan))
                            }
                        }
                        if (state.scanning) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(stringResource(R.string.random_video_scanning, state.scannedCount))
                            TextButton(onClick = session::cancelScan) { Text(stringResource(R.string.advanced_cancel)) }
                        }
                        if (state.hasScanned) {
                            Text(if (state.videos.isEmpty()) stringResource(R.string.random_video_empty)
                                else stringResource(R.string.random_video_found, state.videos.size))
                        }
                        state.error?.let { error ->
                            val message = when (error) {
                                RandomFolderError.NOT_LOCAL -> R.string.advanced_local_folder_only
                                RandomFolderError.NO_ACCESS -> R.string.random_video_no_access
                                RandomFolderError.READ_FAILED -> R.string.random_video_read_error
                            }
                            Text(stringResource(message), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (state.playerVisible && state.videos.isNotEmpty()) {
        RandomFolderVideoViewer(state.videos, onDismiss = session::closePlayer)
    }
}

private tailrec fun Context.isRandomVideoConfigurationChange(): Boolean = when (this) {
    is Activity -> isChangingConfigurations
    is ContextWrapper -> baseContext.isRandomVideoConfigurationChange()
    else -> false
}
