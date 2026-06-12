package com.futureape.kanleme.ui.screens

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
import com.futureape.kanleme.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.i18n.asString
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel

@Composable
fun SimilarPhotosScreen(viewModel: KanlemeViewModel, onBack: () -> Unit, onOpenPhoto: (Long) -> Unit = {}) {
    val groups by viewModel.similarGroups.collectAsStateWithLifecycle()
    val detection by viewModel.similarDetectionState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
        ScreenHeader("相似照片", "后台检测重复、连拍、截图和模糊照片，可离开页面后回来查看进度", onBack)
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
                title = if (detection.running) "正在后台检测" else "还没有相似组",
                message = if (detection.running) {
                    "检测会继续在后台运行，返回首页或锁屏后再回来也能继续看到进度。"
                } else {
                    "点击后会读取当前相册，计算感知哈希、清晰度评分和连拍 / 截图聚类，生成可批量整理的分组。"
                },
                actionText = if (detection.running) "检测中…" else if (detection.progress > 0f && detection.progress < 1f) "继续检测" else "开始检测",
                onAction = { if (!detection.running) viewModel.continueSimilarDetection() },
                modifier = Modifier.padding(18.dp),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("已生成 " + groups.size + " 组候选结果", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Button(onClick = { if (!detection.running) viewModel.continueSimilarDetection() }, enabled = !detection.running) {
                    if (detection.running) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("重新检测")
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
                                Text("平均相似度 ${(group.averageSimilarity * 100).toInt()}% · 最佳照片 ID ${group.bestPhotoId}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { onOpenPhoto(group.bestPhotoId) }) { Text("查看") }
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
                    Text(if (running) "后台检测中" else "相似照片检测", style = MaterialTheme.typography.titleMedium)
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
                        Text(if (progress > 0f && progress < 1f) "继续" else "开始")
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("约 ${(progress * 100).toInt().coerceIn(0, 100)}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                val hint = when {
                    running && totalHint > 0 -> "约 $processedHint/$totalHint"
                    lastResultCount > 0 -> "上次生成 $lastResultCount 组"
                    else -> "可后台运行"
                }
                Text(hint, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (running) {
                Spacer(Modifier.height(2.dp))
                Text("离开页面不会打断当前检测；再次进入会恢复显示这个进度。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun String.toReadableGroupType(): String = when (this) {
    "similar" -> "相似照片"
    "screenshot" -> "相似截图"
    "burst" -> "连拍照片"
    "duplicate" -> "重复照片"
    "blur" -> "模糊照片"
    else -> this
}
