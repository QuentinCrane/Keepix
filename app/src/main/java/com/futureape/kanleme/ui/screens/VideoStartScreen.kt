package com.futureape.kanleme.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.components.NativeFolderExcludeButton
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel

@Composable
fun VideoStartScreen(
    viewModel: KanlemeViewModel,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val videos by viewModel.videoDeck.collectAsStateWithLifecycle()
    val scope by viewModel.videoScope.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val videoFolders by viewModel.videoFolders.collectAsStateWithLifecycle()
    val haptics = rememberHapticKit(settings)
    var ready by remember { mutableStateOf(false) }
    var showAllVideoExcludedFolders by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (ready) 1f else 0f, tween(260), label = "video_start_alpha")
    val lift by animateFloatAsState(if (ready) 0f else 30f, tween(320), label = "video_start_lift")

    LaunchedEffect(scope.folderPaths, scope.sortOrder, settings.excludedFolderPaths) {
        viewModel.loadVideoDeck(scope)
        ready = true
    }

    AdaptiveCenter(maxWidth = 1040.dp) {
        LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenHeader("视频整理主页", "先选择范围和显示比例，再进入短视频式整理流", onBack) }
        item {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(alpha)
                    .graphicsLayer { translationY = lift },
                shape = RoundedCornerShape(34.dp),
                tonalAlpha = 0.70f,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(startHeroColor())
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                                Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.padding(13.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("短视频式整理", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text("上下刷视频，横向快进，长按 2 倍速", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            VideoMetricCard("剩余待整理", (dashboard.videoCount - dashboard.processedVideoCount).coerceAtLeast(0).toString(), Modifier.weight(1f))
                            VideoMetricCard("视频库", dashboard.videoCount.toString(), Modifier.weight(1f))
                            VideoMetricCard("已整理", dashboard.processedVideoCount.toString(), Modifier.weight(1f))
                        }
                        Button(
                            onClick = { haptics.success(); onStart() },
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("开始视频整理", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        item { SectionText("整理范围") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VideoOptionPill(
                    icon = Icons.Rounded.Shuffle,
                    title = "排序",
                    subtitle = if (scope.sortOrder == "random") "随机" else "最新优先",
                    modifier = Modifier.weight(1f),
                    onClick = { haptics.tick(); viewModel.toggleVideoRandom() },
                )
                VideoOptionPill(
                    icon = Icons.Rounded.AspectRatio,
                    title = "显示比例",
                    subtitle = settings.videoDisplayMode.label,
                    modifier = Modifier.weight(1f),
                    onClick = { haptics.tick(); viewModel.cycleVideoDisplayMode() },
                )
            }
        }
        item {
            FolderSelectRail(
                folders = videoFolders,
                selectedPath = scope.folderPaths.firstOrNull(),
                excluded = settings.excludedFolderPaths,
                onFolder = { path -> haptics.tick(); viewModel.setVideoFolder(path) },
                onExclude = { path -> haptics.tick(); viewModel.toggleExcludedFolder(path) },
            )
        }
        item { SectionText("排除文件夹") }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.76f) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (settings.excludedFolderPaths.isEmpty()) "当前未排除任何文件夹" else "已排除 " + settings.excludedFolderPaths.size + " 个文件夹",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("被排除的文件夹不会进入照片整理和视频整理队列。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NativeFolderExcludeButton(
                        title = "用系统选择器添加排除",
                        subtitle = "调用 Android 原生文件夹选择器，不用手动输入路径",
                        onFolderSelected = { path -> haptics.success(); viewModel.addExcludedFolder(path) },
                    )
                    if (videoFolders.size > 12) {
                        Button(
                            onClick = { haptics.tick(); showAllVideoExcludedFolders = !showAllVideoExcludedFolders },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(if (showAllVideoExcludedFolders) "收起文件夹列表" else "展开全部 " + videoFolders.size + " 个文件夹")
                        }
                    }
                    FolderExcludeRow(
                        folders = if (showAllVideoExcludedFolders) videoFolders.take(80) else videoFolders.take(12),
                        excluded = settings.excludedFolderPaths,
                        onToggle = { path -> haptics.tick(); viewModel.toggleExcludedFolder(path) },
                    )
                }
            }
        }
        item { SectionText("手势说明") }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.70f) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureLine("上下滑动", "切换上一个 / 下一个视频")
                    FeatureLine("左右滑动", "明显横向拖动时快进或快退")
                    FeatureLine("长按屏幕", "临时 2 倍速播放，松开恢复")
                    FeatureLine("显示比例", settings.videoDisplayMode.description)
                }
            }
        }
        }
    }
}

@Composable
private fun VideoMetricCard(title: String, value: String, modifier: Modifier) {
    Surface(
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(22.dp),
        color = quietContainerColor(0.30f),
        border = BorderStroke(1.dp, quietBorderColor(0.36f)),
    ) {
        Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.Center) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun VideoOptionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(76.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = quietContainerColor(0.34f),
        border = BorderStroke(1.dp, quietBorderColor(0.42f)),
    ) {
        Row(Modifier.padding(horizontal = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun FolderSelectRail(
    folders: List<String>,
    selectedPath: String?,
    excluded: Set<String>,
    onFolder: (String?) -> Unit,
    onExclude: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SelectableFolderChip("全部视频", selected = selectedPath == null, excluded = false, onClick = { onFolder(null) }, onExclude = null)
        folders.take(18).forEach { path ->
            val label = path.trim('/').substringAfterLast('/').ifBlank { path }
            SelectableFolderChip(
                label = label,
                selected = selectedPath == path,
                excluded = folderRuleMatchesForUi(path, excluded),
                onClick = { onFolder(path) },
                onExclude = { onExclude(path) },
            )
        }
    }
}

@Composable
private fun FolderExcludeRow(folders: List<String>, excluded: Set<String>, onToggle: (String) -> Unit) {
    if (folders.isEmpty()) {
        Text("同步媒体库后会在这里显示可排除的视频文件夹。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        folders.forEach { path ->
            val label = path.trim('/').substringAfterLast('/').ifBlank { path }
            SelectableFolderChip(label, selected = false, excluded = folderRuleMatchesForUi(path, excluded), onClick = { onToggle(path) }, onExclude = null)
        }
    }
}

@Composable
private fun SelectableFolderChip(
    label: String,
    selected: Boolean,
    excluded: Boolean,
    onClick: () -> Unit,
    onExclude: (() -> Unit)?,
) {
    val bg = when {
        excluded -> Color(0xFFE66A6A).copy(alpha = 0.20f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        else -> quietContainerColor(0.30f)
    }
    Surface(
        modifier = Modifier.height(44.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, if (excluded) Color(0xFFE66A6A).copy(alpha = 0.42f) else quietBorderColor(0.42f)),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(if (excluded) Icons.Rounded.Block else Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (excluded) Color(0xFFB64040) else MaterialTheme.colorScheme.primary)
            Text(label.take(18), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (onExclude != null) {
                Surface(
                    modifier = Modifier.clickable(onClick = onExclude),
                    shape = RoundedCornerShape(999.dp),
                    color = if (excluded) Color(0xFFE66A6A).copy(alpha = 0.22f) else quietContainerColor(0.22f),
                ) {
                    Text(if (excluded) "恢复" else "排除", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SectionText(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
}

@Composable
private fun FeatureLine(title: String, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
            Icon(Icons.Rounded.Swipe, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun startHeroColor(): Color {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    return if (oledDark) {
        Color(0xFF050505)
    } else {
        MaterialTheme.colorScheme.surface
    }
}

@Composable
private fun quietContainerColor(lightAlpha: Float = 0.30f): Color {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    return if (oledDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f) else Color.White.copy(alpha = lightAlpha)
}

@Composable
private fun quietBorderColor(lightAlpha: Float = 0.38f): Color {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    return if (oledDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.58f) else Color.White.copy(alpha = lightAlpha)
}
