package com.futureape.kanleme.ui.screens

import com.futureape.kanleme.ui.components.morphAnchor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.i18n.asString
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import com.futureape.kanleme.R

@Composable
fun SimilarPhotosScreen(viewModel: KanlemeViewModel, onBack: () -> Unit, onOpenPhoto: (Long) -> Unit = {}) {
    val screenState by viewModel.similarPhotosUiState.collectAsStateWithLifecycle()
    val groups = screenState.groups
    val detection = screenState.detection
    Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
        ScreenHeader(stringResource(R.string.similar_title), stringResource(R.string.similar_subtitle), onBack)
        SimilarDetectionProgressCard(
            running = detection.running,
            progress = detection.progress,
            stage = detection.stage.asString(),
            processedHint = detection.processedHint,
            totalHint = detection.totalHint,
            lastResultCount = detection.lastResultCount,
            onStart = { viewModel.seedSimilarGroups() },
            onContinue = { viewModel.continueSimilarDetection() },
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
        if (groups.isEmpty()) {
            EmptyState(
                title = stringResource(if (detection.running) R.string.similar_running_empty_title else R.string.similar_empty_title),
                message = if (detection.running) {
                    stringResource(R.string.similar_running_empty_body)
                } else {
                    stringResource(R.string.similar_empty_body)
                },
                actionText = stringResource(
                    if (detection.running) R.string.similar_action_running
                    else if (detection.progress > 0f && detection.progress < 1f) R.string.similar_action_continue
                    else R.string.similar_action_start,
                ),
                onAction = { if (!detection.running) viewModel.continueSimilarDetection() },
                modifier = Modifier.padding(18.dp),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.similar_groups_generated, groups.size), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Button(onClick = { if (!detection.running) viewModel.continueSimilarDetection() }, enabled = !detection.running) {
                    if (detection.running) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(stringResource(R.string.similar_redetect))
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(groups, key = { it.id }) { group ->
                    GlassSurface(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(group.type.toReadableGroupType(), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.similar_average_best, (group.averageSimilarity * 100).toInt(), group.bestPhotoId), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(modifier = Modifier.morphAnchor("similar:" + group.id), onClick = { onOpenPhoto(group.bestPhotoId) }) { Text(stringResource(R.string.similar_view)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimilarDetectionProgressCard(
    running: Boolean,
    progress: Float,
    stage: String,
    processedHint: Int,
    totalHint: Int,
    lastResultCount: Int,
    onStart: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), tonalAlpha = 0.74f) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(if (running) R.string.similar_detection_running else R.string.similar_detection_title), style = MaterialTheme.typography.titleMedium)
                    Text(stage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { if (progress > 0f && progress < 1f) onContinue() else onStart() },
                    enabled = !running,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    if (running) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(if (progress > 0f && progress < 1f) R.string.similar_continue else R.string.similar_start))
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.similar_progress_percent, (progress * 100).toInt().coerceIn(0, 100)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                val hint = when {
                    running && totalHint > 0 -> stringResource(R.string.similar_progress_items, processedHint, totalHint)
                    lastResultCount > 0 -> stringResource(R.string.similar_previous_groups, lastResultCount)
                    else -> stringResource(R.string.similar_background_capable)
                }
                Text(hint, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (running) {
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.similar_background_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun String.toReadableGroupType(): String = when (this) {
    "similar" -> stringResource(R.string.similar_group_similar)
    "screenshot" -> stringResource(R.string.similar_group_screenshot)
    "burst" -> stringResource(R.string.similar_group_burst)
    "duplicate" -> stringResource(R.string.similar_group_duplicate)
    "blur" -> stringResource(R.string.similar_group_blur)
    else -> this
}
