package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.data.settings.FolderDisplayMode
import com.futureape.kanleme.ui.components.AdaptiveWidthInfo
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.util.formatDate
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel

@Composable
fun PhotoCleanScreen(
    viewModel: KanlemeViewModel,
    onBack: () -> Unit,
    onOpenPhoto: (PhotoEntity) -> Unit,
) {
    val deck by viewModel.photoDeck.collectAsStateWithLifecycle()
    val deckPreparing by viewModel.photoDeckPreparing.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val typeStats by viewModel.photoTypeStats.collectAsStateWithLifecycle()
    val scope by viewModel.photoScope.collectAsStateWithLifecycle()
    val folders by viewModel.photoFolders.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val haptics = rememberHapticKit(settings)
    val context = LocalContext.current
    var showGuide by remember { mutableStateOf(false) }
    var albumPickerExpanded by remember { mutableStateOf(false) }

    val folderPairs = remember(folders) {
        folders.mapNotNull { path ->
            val label = path.trim('/').substringAfterLast('/').ifBlank { return@mapNotNull null }
            label to path
        }.distinctBy { it.first }.take(18)
    }
    var selectedFolder by remember(folderPairs) { mutableStateOf("指定归档") }
    val selectedTargetPath = folderPairs.firstOrNull { it.first == selectedFolder }?.second

    LaunchedEffect(Unit) {
        // Do not reload a non-empty deck when returning from the viewer.
        // In random mode, reloading would rebuild the deck and make the user lose
        // the first random sequence they were already browsing.
        if (deck.isEmpty() && !deckPreparing) viewModel.loadPhotoDeck(scope)
    }
    LaunchedEffect(deck.isEmpty(), deckPreparing, dashboard.photoCount, dashboard.processedPhotoCount, scope) {
        if (deck.isEmpty() && !deckPreparing && dashboard.processedPhotoCount < dashboard.photoCount) viewModel.loadPhotoDeck(scope)
    }
    LaunchedEffect(deck, settings.photoGuideShown) {
        if (deck.isNotEmpty() && !settings.photoGuideShown) showGuide = true
        deck.take(3).forEach { photo ->
            context.imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(Uri.parse(photo.uri))
                    .memoryCacheKey(photo.uri)
                    .diskCacheKey(photo.uri)
                    .build()
            )
        }
    }

    if (deck.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            ScreenHeader("照片整理", "连续整理模式，不按固定轮次停止", onBack)
            Box(Modifier.fillMaxSize().padding(18.dp), contentAlignment = Alignment.Center) {
                if (deckPreparing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        CircularProgressIndicator()
                        Text("正在准备整理队列", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("会直接进入当前随机 / 最新顺序，不再先闪出暂无队列", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    EmptyState(
                        title = "当前筛选下暂无待整理照片",
                        message = "连续整理不会按轮次停止；这里为空通常表示当前筛选、排除文件夹或相册权限下已经没有可整理内容。",
                        actionText = "重新读取队列",
                        onAction = { haptics.success(); if (dashboard.photoCount == 0) viewModel.refreshLibrary() else viewModel.loadPhotoDeck(scope) },
                    )
                }
            }
        }
        return
    }

    val currentPhoto = deck.first()
    val remainingPhotos = (dashboard.photoCount - dashboard.processedPhotoCount).coerceAtLeast(deck.size)
    val folderLabels = remember(folderPairs, settings.folderDisplay) {
        val base = folderPairs.map { it.first }.ifEmpty { listOf("Camera (DCIM)", "Screenshots", "Downloads") }
        if (settings.folderDisplay == FolderDisplayMode.SINGLE_LINE) base.take(12) else base.take(24)
    }
    val dimAlpha by animateFloatAsState(targetValue = if (showGuide) 0.58f else 0f, label = "guide_dim")

    fun perform(photo: PhotoEntity, action: SwipeAction) {
        haptics.success()
        viewModel.onPhotoActionWithOptionalMove(photo, action, selectedTargetPath)
    }

    Box(Modifier.fillMaxSize()) {
        if (settings.immersiveBackground) {
            Crossfade(
                targetState = currentPhoto.uri,
                animationSpec = tween(durationMillis = 560),
                label = "photo_background_crossfade",
            ) { photoUri ->
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(photoUri))
                            .memoryCacheKey(photoUri)
                            .diskCacheKey(photoUri)
                            .placeholderMemoryCacheKey(photoUri)
                            .crossfade(false)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(22.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        val oledDark = MaterialTheme.colorScheme.background == Color.Black
        val scrimColors = if (oledDark) {
            listOf(
                Color.Black.copy(alpha = if (settings.immersiveBackground) 0.30f else 1.00f),
                Color.Black.copy(alpha = if (settings.immersiveBackground) 0.22f else 1.00f),
                Color.Black.copy(alpha = if (settings.immersiveBackground) 0.45f else 1.00f),
            )
        } else {
            listOf(
                Color.White.copy(alpha = if (settings.immersiveBackground) 0.40f else 0.90f),
                MaterialTheme.colorScheme.primary.copy(alpha = if (settings.immersiveBackground) 0.16f else 0.07f),
                Color.Black.copy(alpha = if (settings.immersiveBackground) 0.26f else 0.02f),
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(scrimColors)))

        AdaptiveWidthInfo { _, isExpanded ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = if (isExpanded) 34.dp else 18.dp, vertical = 10.dp),
            ) {
                PhotoDeckStage(
                    photos = deck.take(3),
                    settings = settings,
                    haptics = haptics,
                    modifier = if (isExpanded) {
                        Modifier
                            .align(Alignment.Center)
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                    } else {
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                    },
                    onOpen = { photo -> haptics.tick(); onOpenPhoto(photo) },
                    onAction = { photo, action -> perform(photo, action) },
                )

                if (settings.photoShowTopBar || settings.photoShowShuffleButton || settings.photoShowFilterChips) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                    ) {
                        PhotoOrganizerTopControls(
                            remainingCount = remainingPhotos,
                            mediaCounts = listOf(
                                "全部" to typeStats.all.toString(),
                                "普通照片" to typeStats.normal.toString(),
                                "截图" to typeStats.screenshot.toString(),
                                "自拍" to typeStats.selfie.toString(),
                                "实况" to typeStats.motion.toString(),
                                "长图" to typeStats.longImage.toString(),
                            ),
                            selectedMediaIndex = listOf("all", "normal", "screenshot", "selfie", "motion", "long").indexOf(scope.mediaType).coerceAtLeast(0),
                            dateMode = scope.dateMode,
                            randomEnabled = scope.sortOrder == "random",
                            showKeep = settings.photoShowTopBar,
                            showShuffle = settings.photoShowShuffleButton,
                            showFilters = settings.photoShowFilterChips,
                            showRemaining = settings.photoShowTopBar,
                            onKeep = { perform(currentPhoto, SwipeAction.Keep) },
                            onShuffle = { haptics.threshold(); viewModel.reshufflePhotoCleaningSession() },
                            onMediaSelected = { index ->
                                val type = listOf("all", "normal", "screenshot", "selfie", "motion", "long").getOrElse(index) { "all" }
                                haptics.tick()
                                viewModel.setPhotoTypeFilter(type)
                            },
                            onDateSelected = { index ->
                                haptics.tick()
                                when (index) {
                                    0 -> viewModel.setPhotoDateMode("all")
                                    1 -> viewModel.setPhotoDateMode("seven_days")
                                    2 -> viewModel.setPhotoDateMode("month")
                                    3 -> viewModel.setPhotoDateMode("year")
                                }
                            },
                        )
                    }
                }

                if (settings.photoShowInfoBar || settings.photoShowFolderChips) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    ) {
                        CompactPhotoInfoBar(
                            remaining = remainingPhotos,
                            photo = currentPhoto,
                            selectedAlbum = selectedFolder,
                            showInfo = settings.photoShowInfoBar,
                            showAlbum = settings.photoShowFolderChips,
                            onOpen = { haptics.tick(); onOpenPhoto(currentPhoto) },
                            onAlbumClick = { haptics.tick(); albumPickerExpanded = !albumPickerExpanded },
                        )
                        androidx.compose.animation.AnimatedVisibility(visible = albumPickerExpanded && settings.photoShowFolderChips) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                FolderChipRail(
                                    folders = listOf("指定归档") + folderLabels,
                                    selected = selectedFolder,
                                    onSelected = {
                                        haptics.tick(); selectedFolder = it; albumPickerExpanded = false
                                        if (it != "指定归档") viewModel.setPhotoFolder(folderPairs.firstOrNull { pair -> pair.first == it }?.second) else viewModel.setPhotoFolder(null)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (settings.excludedFolderPaths.isNotEmpty()) {
                                    Text(
                                        "已排除 " + settings.excludedFolderPaths.size + " 个文件夹",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (dimAlpha > 0.01f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimAlpha)))
        }
        androidx.compose.animation.AnimatedVisibility(visible = showGuide, modifier = Modifier.fillMaxSize()) {
            PhotoPositionGuideOverlay(onDismiss = { haptics.tick(); showGuide = false; viewModel.markPhotoGuideShown() })
        }
    }
}

private data class PositionGuideStep(
    val title: String,
    val body: String,
    val targetAlignment: Alignment,
    val cardAlignment: Alignment,
    val targetWidth: androidx.compose.ui.unit.Dp,
    val targetHeight: androidx.compose.ui.unit.Dp,
    val targetPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
)

@Composable
private fun PhotoPositionGuideOverlay(onDismiss: () -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val steps = listOf(
        PositionGuideStep(
            title = "顶部操作栏",
            body = "这里是一行核心动作：保留当前图、重新随机、图片筛选、时间筛选和剩余数量。筛选点开才展开，不会长期占行。",
            targetAlignment = Alignment.TopCenter,
            cardAlignment = Alignment.BottomCenter,
            targetWidth = 356.dp,
            targetHeight = 54.dp,
            targetPadding = androidx.compose.foundation.layout.PaddingValues(top = 44.dp),
        ),
        PositionGuideStep(
            title = "中间照片卡片",
            body = "点按图片进入大图；明显左右滑是保留，明显上下滑才会收藏或待删。斜向拖动不会轻易触发破坏性操作。",
            targetAlignment = Alignment.Center,
            cardAlignment = Alignment.BottomCenter,
            targetWidth = 318.dp,
            targetHeight = 430.dp,
        ),
        PositionGuideStep(
            title = "底部图片信息",
            body = "这里显示拍摄时间、文件夹、大小和剩余数量；点按信息条也能进入大图查看细节。",
            targetAlignment = Alignment.BottomCenter,
            cardAlignment = Alignment.TopCenter,
            targetWidth = 342.dp,
            targetHeight = 76.dp,
            targetPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 30.dp),
        ),
        PositionGuideStep(
            title = "右下角相册按钮",
            body = "点击相册可以展开目标文件夹，选择后保留/收藏会按设置自动归档。",
            targetAlignment = Alignment.BottomEnd,
            cardAlignment = Alignment.TopCenter,
            targetWidth = 74.dp,
            targetHeight = 74.dp,
            targetPadding = androidx.compose.foundation.layout.PaddingValues(end = 18.dp, bottom = 30.dp),
        ),
    )
    val step = steps[stepIndex.coerceIn(0, steps.lastIndex)]
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)))
        Box(
            modifier = Modifier
                .align(step.targetAlignment)
                .padding(step.targetPadding)
                .size(width = step.targetWidth, height = step.targetHeight)
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), RoundedCornerShape(28.dp)),
        )
        Surface(
            modifier = Modifier
                .align(step.cardAlignment)
                .padding(horizontal = 22.dp, vertical = 40.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("步骤 " + (stepIndex + 1) + "/" + steps.size, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(step.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(step.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    if (stepIndex > 0) {
                        Button(onClick = { stepIndex -= 1 }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(999.dp)) { Text("上一步") }
                    }
                    Button(
                        onClick = { if (stepIndex < steps.lastIndex) stepIndex += 1 else onDismiss() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                    ) { Text(if (stepIndex < steps.lastIndex) "下一步" else "知道了") }
                }
            }
        }
    }
}

private enum class PhotoQuickPanel { MEDIA, DATE }

@Composable
private fun PhotoOrganizerTopControls(
    remainingCount: Int,
    mediaCounts: List<Pair<String, String>>,
    selectedMediaIndex: Int,
    dateMode: String,
    randomEnabled: Boolean,
    showKeep: Boolean = true,
    showShuffle: Boolean = true,
    showFilters: Boolean = true,
    showRemaining: Boolean = true,
    onKeep: () -> Unit,
    onShuffle: () -> Unit,
    onMediaSelected: (Int) -> Unit,
    onDateSelected: (Int) -> Unit,
) {
    var openPanel by remember { mutableStateOf<PhotoQuickPanel?>(null) }
    val dateLabel = when (dateMode) {
        "seven_days" -> "最近7天"
        "month" -> "本月"
        "year" -> "今年"
        else -> "全部时间"
    }
    val mediaLabel = mediaCounts.getOrNull(selectedMediaIndex)?.first ?: "全部"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showKeep) {
                CompactControlPill(
                    title = "保留",
                    subtitle = "当前图",
                    icon = Icons.Rounded.CheckCircle,
                    onClick = onKeep,
                )
            }
            if (showShuffle) {
                CompactControlPill(
                    title = "重新随机",
                    subtitle = if (randomEnabled) "已随机" else "随机",
                    icon = Icons.Rounded.Swipe,
                    selected = randomEnabled,
                    onClick = onShuffle,
                )
            }
            if (showFilters) {
                CompactControlPill(
                    title = "图片筛选",
                    subtitle = mediaLabel,
                    icon = Icons.Rounded.FilterAlt,
                    selected = openPanel == PhotoQuickPanel.MEDIA,
                    onClick = { openPanel = if (openPanel == PhotoQuickPanel.MEDIA) null else PhotoQuickPanel.MEDIA },
                )
                CompactControlPill(
                    title = "时间筛选",
                    subtitle = dateLabel,
                    icon = Icons.Rounded.CalendarToday,
                    selected = openPanel == PhotoQuickPanel.DATE,
                    onClick = { openPanel = if (openPanel == PhotoQuickPanel.DATE) null else PhotoQuickPanel.DATE },
                )
            }
            if (showRemaining) RemainingCountPill(remainingCount)
        }

        androidx.compose.animation.AnimatedVisibility(visible = showFilters && openPanel == PhotoQuickPanel.MEDIA) {
            FilterChipRail(
                chips = mediaCounts,
                selectedIndex = selectedMediaIndex,
                onSelected = { index -> onMediaSelected(index); openPanel = null },
            )
        }
        androidx.compose.animation.AnimatedVisibility(visible = showFilters && openPanel == PhotoQuickPanel.DATE) {
            FilterChipRail(
                chips = listOf(
                    "全部时间" to "",
                    "最近7天" to "",
                    "本月" to "",
                    "今年" to "",
                ),
                selectedIndex = listOf("all", "seven_days", "month", "year").indexOf(dateMode).coerceAtLeast(0),
                onSelected = { index -> onDateSelected(index); openPanel = null },
            )
        }
    }
}

@Composable
private fun CompactControlPill(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.height(42.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = background,
        contentColor = content,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (selected) 0.10f else 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = content)
            Column(verticalArrangement = Arrangement.Center) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.72f), maxLines = 1)
            }
        }
    }
}

@Composable
private fun RemainingCountPill(remainingCount: Int) {
    Surface(
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("剩余", style = MaterialTheme.typography.labelLarge)
            Text(remainingCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("张", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CompactPhotoInfoBar(
    remaining: Int,
    photo: PhotoEntity,
    selectedAlbum: String,
    showInfo: Boolean = true,
    showAlbum: Boolean = true,
    onOpen: () -> Unit,
    onAlbumClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showInfo) {
            Surface(
                modifier = Modifier.weight(1f).height(66.dp).clickable(onClick = onOpen),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            ) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center) {
                    Text(
                        "剩余 " + remaining + " 张 · " + formatDate(photo.dateTaken),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        photo.folderName + " · " + formatSize(photo.size) + " · 点按查看大图",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (showAlbum) {
            Surface(
                modifier = Modifier.size(62.dp).clickable(onClick = onAlbumClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                contentColor = MaterialTheme.colorScheme.primary,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.Folder, contentDescription = "选择相册：" + selectedAlbum, modifier = Modifier.size(20.dp))
                    Text("相册", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

