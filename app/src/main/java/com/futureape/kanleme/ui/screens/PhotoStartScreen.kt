package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.futureape.kanleme.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.settings.AppVisualStyle
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.components.NativeFolderExcludeButton
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel

@Composable
fun PhotoStartScreen(
    viewModel: KanlemeViewModel,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val photos by viewModel.photoDeck.collectAsStateWithLifecycle()
    val scope by viewModel.photoScope.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val photoFolders by viewModel.photoFolders.collectAsStateWithLifecycle()
    val haptics = rememberHapticKit(settings)
    var ready by remember { mutableStateOf(false) }
    var showAllPhotoExcludedFolders by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (ready) 1f else 0f, tween(260), label = "photo_start_alpha")
    val lift by animateFloatAsState(if (ready) 0f else 30f, tween(320), label = "photo_start_lift")

    LaunchedEffect(scope.folderPaths, scope.sortOrder, settings.excludedFolderPaths, scope.mediaType, scope.dateMode) {
        viewModel.loadPhotoDeck(scope)
        ready = true
    }

    if (settings.appVisualStyle == AppVisualStyle.IMMERSIVE_PHOTO) {
        KeepixPhotoStartContent(
            photos = photos,
            photoCount = dashboard.photoCount,
            processedPhotoCount = dashboard.processedPhotoCount,
            scopeMediaType = scope.mediaType,
            scopeDateMode = scope.dateMode,
            sortOrder = scope.sortOrder,
            gestureLabel = stringResource(settings.gestureDirection.labelRes),
            excludedCount = settings.excludedFolderPaths.size,
            photoFolders = photoFolders,
            selectedFolder = scope.folderPaths.firstOrNull(),
            excludedFolders = settings.excludedFolderPaths,
            showAllPhotoExcludedFolders = showAllPhotoExcludedFolders,
            onBack = onBack,
            onStart = { haptics.success(); onStart() },
            onToggleRandom = { haptics.tick(); viewModel.togglePhotoRandom() },
            onCycleGesture = { haptics.tick(); viewModel.cycleGestureDirection() },
            onType = { haptics.tick(); viewModel.setPhotoTypeFilter(it) },
            onDate = { haptics.tick(); viewModel.setPhotoDateMode(it) },
            onFolder = { path -> haptics.tick(); viewModel.setPhotoFolder(path) },
            onExclude = { path -> haptics.tick(); viewModel.toggleExcludedFolder(path) },
            onAddExcludedFolder = { path -> haptics.success(); viewModel.addExcludedFolder(path) },
            onToggleAllExcludedFolders = { haptics.tick(); showAllPhotoExcludedFolders = !showAllPhotoExcludedFolders },
        )
        return
    }

    AdaptiveCenter(maxWidth = 1040.dp) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ScreenHeader("照片整理", "开始前可在下方调整范围", onBack) }
            item {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth().alpha(alpha).graphicsLayer { translationY = lift },
                    shape = RoundedCornerShape(30.dp),
                    tonalAlpha = 0.90f,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(startHeroColor())
                            .padding(22.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("整理照片", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (photos.isEmpty()) "点击开始后进入当前筛选队列" else "当前队列已准备 " + photos.size + " 张",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                PhotoMetricCard("剩余", (dashboard.photoCount - dashboard.processedPhotoCount).coerceAtLeast(0).toString(), Modifier.weight(1f))
                                PhotoMetricCard("照片库", dashboard.photoCount.toString(), Modifier.weight(1f))
                                PhotoMetricCard("已整理", dashboard.processedPhotoCount.toString(), Modifier.weight(1f))
                            }
                            Button(
                                onClick = { haptics.success(); onStart() },
                                modifier = Modifier.fillMaxWidth().height(58.dp),
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("开始整理", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
            item { PhotoSectionText("整理范围") }
            item {
                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.86f) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PhotoOptionPill(
                                icon = Icons.Rounded.Shuffle,
                                title = "排序",
                                subtitle = if (scope.sortOrder == "random") "随机" else "最新优先",
                                modifier = Modifier.weight(1f),
                                onClick = { haptics.tick(); viewModel.togglePhotoRandom() },
                            )
                            PhotoOptionPill(
                                icon = Icons.Rounded.Swipe,
                                title = "手势",
                                subtitle = stringResource(settings.gestureDirection.labelRes),
                                modifier = Modifier.weight(1f),
                                onClick = { haptics.tick(); viewModel.cycleGestureDirection() },
                            )
                        }
                        PhotoFilterRail(
                            selectedType = scope.mediaType,
                            selectedDate = scope.dateMode,
                            onType = { haptics.tick(); viewModel.setPhotoTypeFilter(it) },
                            onDate = { haptics.tick(); viewModel.setPhotoDateMode(it) },
                        )
                        PhotoFolderSelectRail(
                            folders = photoFolders,
                            selectedPath = scope.folderPaths.firstOrNull(),
                            excluded = settings.excludedFolderPaths,
                            onFolder = { path -> haptics.tick(); viewModel.setPhotoFolder(path) },
                            onExclude = { path -> haptics.tick(); viewModel.toggleExcludedFolder(path) },
                        )
                    }
                }
            }
            item { PhotoSectionText("排除文件夹") }
            item {
                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.84f) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (settings.excludedFolderPaths.isEmpty()) "当前未排除任何文件夹" else "已排除 " + settings.excludedFolderPaths.size + " 个文件夹",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        NativeFolderExcludeButton(
                            title = "用系统选择器添加排除",
                            subtitle = "被排除的文件夹不会进入照片整理和视频整理队列",
                            onFolderSelected = { path -> haptics.success(); viewModel.addExcludedFolder(path) },
                        )
                        if (photoFolders.size > 12) {
                            Button(
                                onClick = { haptics.tick(); showAllPhotoExcludedFolders = !showAllPhotoExcludedFolders },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(if (showAllPhotoExcludedFolders) "收起文件夹列表" else "展开全部 " + photoFolders.size + " 个文件夹")
                            }
                        }
                        PhotoFolderExcludeRow(
                            folders = if (showAllPhotoExcludedFolders) photoFolders.take(80) else photoFolders.take(12),
                            excluded = settings.excludedFolderPaths,
                            onToggle = { path -> haptics.tick(); viewModel.toggleExcludedFolder(path) },
                        )
                    }
                }
            }
            item { PhotoSectionText("手势说明") }
            item {
                GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.82f) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PhotoFeatureLine("点击图片", "进入大图查看器")
                        PhotoFeatureLine("左右滑动", "保留当前照片")
                        PhotoFeatureLine("上下滑动", stringResource(settings.gestureDirection.guideRes))
                        PhotoFeatureLine("排除文件夹", "被排除内容不会进入整理队列")
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepixPhotoStartContent(
    photos: List<PhotoEntity>,
    photoCount: Int,
    processedPhotoCount: Int,
    scopeMediaType: String,
    scopeDateMode: String,
    sortOrder: String,
    gestureLabel: String,
    excludedCount: Int,
    photoFolders: List<String>,
    selectedFolder: String?,
    excludedFolders: Set<String>,
    showAllPhotoExcludedFolders: Boolean,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onToggleRandom: () -> Unit,
    onCycleGesture: () -> Unit,
    onType: (String) -> Unit,
    onDate: (String) -> Unit,
    onFolder: (String?) -> Unit,
    onExclude: (String) -> Unit,
    onAddExcludedFolder: (String) -> Unit,
    onToggleAllExcludedFolders: () -> Unit,
) {
    val context = LocalContext.current
    val hero = photos.firstOrNull()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hero != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(hero.uri))
                    .memoryCacheKey(hero.uri)
                    .diskCacheKey(hero.uri)
                    .placeholderMemoryCacheKey(hero.uri)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(34.dp)
                    .graphicsLayer {
                        scaleX = 1.12f
                        scaleY = 1.12f
                        alpha = 0.52f
                    },
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.62f),
                        Color.Black.copy(alpha = 0.20f),
                        Color.Black.copy(alpha = 0.40f),
                        Color.Black.copy(alpha = 0.90f),
                    )
                )
            )
        )
        AdaptiveCenter(maxWidth = 760.dp) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        KeepixStartCircle(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, onBack)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("照片", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Black)
                            Text("整理前先确认范围", style = MaterialTheme.typography.titleSmall, color = Color.White.copy(alpha = 0.62f))
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            contentColor = Color.White,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        ) {
                            Text(
                                photos.size.toString() + " 张待看",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                item {
                    KeepixStartDeck(
                        photos = photos,
                        remaining = (photoCount - processedPhotoCount).coerceAtLeast(photos.size),
                        processed = processedPhotoCount,
                        total = photoCount,
                        onStart = onStart,
                    )
                }
                item {
                    KeepixStartPanel(title = "整理范围") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            KeepixStartOption(Icons.Rounded.Shuffle, "排序", if (sortOrder == "random") "随机" else "最新优先", Modifier.weight(1f), onToggleRandom)
                            KeepixStartOption(Icons.Rounded.Swipe, "手势", gestureLabel, Modifier.weight(1f), onCycleGesture)
                        }
                        PhotoFilterRail(
                            selectedType = scopeMediaType,
                            selectedDate = scopeDateMode,
                            onType = onType,
                            onDate = onDate,
                        )
                        PhotoFolderSelectRail(
                            folders = photoFolders,
                            selectedPath = selectedFolder,
                            excluded = excludedFolders,
                            onFolder = onFolder,
                            onExclude = onExclude,
                        )
                    }
                }
                item {
                    KeepixStartPanel(title = "排除文件夹") {
                        Text(
                            if (excludedCount == 0) "当前未排除任何文件夹" else "已排除 " + excludedCount + " 个文件夹",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        NativeFolderExcludeButton(
                            title = "添加排除文件夹",
                            subtitle = "被排除的文件夹不会进入照片和视频整理队列",
                            onFolderSelected = onAddExcludedFolder,
                        )
                        if (photoFolders.size > 12) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onToggleAllExcludedFolders),
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White.copy(alpha = 0.11f),
                                contentColor = Color.White,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (showAllPhotoExcludedFolders) "收起文件夹列表" else "展开全部 " + photoFolders.size + " 个文件夹", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                        PhotoFolderExcludeRow(
                            folders = if (showAllPhotoExcludedFolders) photoFolders.take(80) else photoFolders.take(12),
                            excluded = excludedFolders,
                            onToggle = onExclude,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepixStartDeck(
    photos: List<PhotoEntity>,
    remaining: Int,
    processed: Int,
    total: Int,
    onStart: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (photos.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.84f).height(360.dp),
                shape = RoundedCornerShape(30.dp),
                color = Color.White.copy(alpha = 0.12f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("队列准备中", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val visible = photos.take(4)
            val rotations = listOf(-8f, 7f, -4f, 0f)
            val offsets = listOf(-70.dp, 58.dp, -22.dp, 0.dp)
            visible.reversed().forEachIndexed { reversedIndex, photo ->
                val originalIndex = visible.lastIndex - reversedIndex
                val isTop = originalIndex == 0
                val shape = RoundedCornerShape(28.dp)
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(if (isTop) 0.76f else 0.70f)
                        .aspectRatio(0.72f)
                        .graphicsLayer {
                            rotationZ = rotations.getOrElse(originalIndex) { 0f }
                            translationX = offsets.getOrElse(originalIndex) { 0.dp }.toPx()
                            translationY = (if (isTop) 0.dp else 34.dp).toPx()
                            alpha = if (isTop) 1f else 0.84f
                            scaleX = if (isTop) 1f else 0.95f
                            scaleY = if (isTop) 1f else 0.95f
                        }
                        .clip(shape)
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)), shape),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(photo.uri))
                            .memoryCacheKey(photo.uri)
                            .diskCacheKey(photo.uri)
                            .placeholderMemoryCacheKey(photo.uri)
                            .crossfade(false)
                            .build(),
                        contentDescription = photo.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(78.dp)
                .clickable(onClick = onStart),
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFF171717).copy(alpha = 0.88f),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("开始整理", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Black)
                    Text("剩余 " + remaining + " · 已整理 " + processed + "/" + total.coerceAtLeast(photos.size), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.62f), maxLines = 1)
                }
                Surface(shape = CircleShape, color = Color(0xFF86A7FF), contentColor = Color.White) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.padding(14.dp).size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun KeepixStartCircle(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(52.dp).clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.13f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(27.dp), tint = Color.White.copy(alpha = 0.90f))
        }
    }
}

@Composable
private fun KeepixStartPanel(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.82f), fontWeight = FontWeight.Bold)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF111111).copy(alpha = 0.88f),
            contentColor = Color.White,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun KeepixStartOption(icon: ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(72.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.07f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFF86A7FF))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.60f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun PhotoMetricCard(title: String, value: String, modifier: Modifier) {
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
private fun PhotoOptionPill(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
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
private fun PhotoFilterRail(selectedType: String, selectedDate: String, onType: (String) -> Unit, onDate: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("all" to "全部", "normal" to "普通", "screenshot" to "截图", "selfie" to "自拍", "motion" to "实况", "gif" to "GIF", "long" to "长图").forEach { item ->
                PhotoSmallChip(label = item.second, selected = selectedType == item.first, onClick = { onType(item.first) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("all" to "全部时间", "seven_days" to "最近7天", "month" to "本月", "year" to "今年").forEach { item ->
                PhotoSmallChip(label = item.second, selected = selectedDate == item.first, onClick = { onDate(item.first) })
            }
        }
    }
}

@Composable
private fun PhotoFolderSelectRail(
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
        PhotoSelectableFolderChip("全部照片", selected = selectedPath == null, excluded = false, onClick = { onFolder(null) }, onExclude = null)
        folders.take(18).forEach { path ->
            val label = path.trim('/').substringAfterLast('/').ifBlank { path }
            PhotoSelectableFolderChip(
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
private fun PhotoFolderExcludeRow(folders: List<String>, excluded: Set<String>, onToggle: (String) -> Unit) {
    if (folders.isEmpty()) {
        Text("同步媒体库后会在这里显示可排除的照片文件夹。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        folders.forEach { path ->
            val label = path.trim('/').substringAfterLast('/').ifBlank { path }
            PhotoSelectableFolderChip(label, selected = false, excluded = folderRuleMatchesForUi(path, excluded), onClick = { onToggle(path) }, onExclude = null)
        }
    }
}

@Composable
private fun PhotoSelectableFolderChip(
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
private fun PhotoSmallChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.height(42.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.82f) else quietContainerColor(0.30f),
        border = BorderStroke(1.dp, quietBorderColor(0.40f)),
    ) {
        Box(Modifier.padding(horizontal = 15.dp), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun PhotoSectionText(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
}

@Composable
private fun PhotoFeatureLine(title: String, body: String) {
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
