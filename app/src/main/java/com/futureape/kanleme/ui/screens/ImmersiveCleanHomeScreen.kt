package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import com.futureape.kanleme.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.data.repository.CleaningScope
import com.futureape.kanleme.data.repository.DashboardStats
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.components.AdaptiveWidthInfo
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.photoDisplayAspectRatio
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.util.shareDailyReportImage
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Keepix immersive home visual path. Do not put legacy Liquid Glass UI in this file.
@Composable
internal fun ImmersiveCleanHomeScreen(
    contentPadding: PaddingValues,
    dashboard: DashboardStats,
    settings: AppSettings,
    photoScope: CleaningScope,
    videoScope: CleaningScope,
    photos: List<PhotoEntity>,
    videos: List<VideoEntity>,
    photoPreparing: Boolean,
    videoPreparing: Boolean,
    selectedIsPhoto: Boolean,
    onPhotoTab: () -> Unit,
    onVideoTab: () -> Unit,
    onPhoto: () -> Unit,
    onVideo: () -> Unit,
    onTimeline: () -> Unit,
    onTrash: () -> Unit,
    onFavorites: () -> Unit,
    onToday: () -> Unit,
    onSettings: () -> Unit,
    onPhotoType: (String) -> Unit,
    onPhotoDate: (String) -> Unit,
    onPhotoSort: (String) -> Unit,
    onPhotoBatch: (Int) -> Unit,
    onVideoDate: (String) -> Unit,
    onVideoSort: (String) -> Unit,
    onVideoBatch: (Int) -> Unit,
    onDailyReport: () -> Unit,
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var menuPanel by remember { mutableStateOf(KeepixMenuPanel.ROOT) }
    LaunchedEffect(menuExpanded) {
        if (!menuExpanded) menuPanel = KeepixMenuPanel.ROOT
    }
    val previews = if (selectedIsPhoto) {
        photos.take(3).map { KeepixPreviewItem(it.uri, it.displayName, false, photoDisplayAspectRatio(it, minRatio = 0.42f, maxRatio = 2.20f)) }
    } else {
        videos.take(3).map { KeepixPreviewItem(it.uri, it.displayName, true, keepixPhotoAspectRatio(it.width, it.height)) }
    }
    val heroUri = previews.firstOrNull()?.uri
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (heroUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(Uri.parse(heroUri))
                    .memoryCacheKey(heroUri + "#home_blur")
                    .diskCacheKey(heroUri)
                    .placeholderMemoryCacheKey(heroUri + "#home_deck")
                    .size(1080, 1080)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(34.dp)
                    .graphicsLayer {
                        scaleX = 1.12f
                        scaleY = 1.12f
                        alpha = 0.58f
                    },
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.54f),
                        Color.Black.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.40f),
                        Color.Black.copy(alpha = 0.88f),
                    )
                )
            )
        )
        AnimatedVisibility(
            visible = menuExpanded,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(140)),
            modifier = Modifier.fillMaxSize().zIndex(2f),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.24f))
            )
        }
        AdaptiveCenter(maxWidth = 760.dp) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 22.dp,
                        end = 22.dp,
                        top = 58.dp,
                        bottom = contentPadding.calculateBottomPadding(),
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().zIndex(2f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            Box(Modifier.height(50.dp), contentAlignment = Alignment.CenterStart) {
                                if (!menuExpanded) {
                                    KeepixQueueSelector(
                                        selectedIsPhoto = selectedIsPhoto,
                                        mediaType = photoScope.mediaType,
                                        onClick = { menuExpanded = true },
                                    )
                                }
                            }
                        }
                    }
                    item {
                        KeepixHomeDeck(
                            items = previews,
                            preparing = if (selectedIsPhoto) photoPreparing else videoPreparing,
                            selectedIsPhoto = selectedIsPhoto,
                            onClick = if (selectedIsPhoto) onPhoto else onVideo,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = menuExpanded,
                    enter = fadeIn(tween(160)) + expandVertically(tween(240), expandFrom = Alignment.Top),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(180), shrinkTowards = Alignment.Top),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 22.dp, top = 58.dp)
                        .fillMaxWidth(0.70f)
                        .zIndex(3f),
                ) {
                    KeepixQueueMenu(
                        modifier = Modifier.fillMaxWidth(),
                        selectedIsPhoto = selectedIsPhoto,
                        mediaType = photoScope.mediaType,
                        photoScope = photoScope,
                        videoScope = videoScope,
                        settings = settings,
                        panel = menuPanel,
                        onToggle = { menuExpanded = false },
                        onPanel = { menuPanel = it },
                        onPhotoTab = {
                            onPhotoTab()
                            menuPanel = KeepixMenuPanel.ROOT
                        },
                        onVideoTab = {
                            onVideoTab()
                            menuExpanded = false
                            menuPanel = KeepixMenuPanel.ROOT
                        },
                        onPhotoType = {
                            onPhotoType(it)
                            menuExpanded = false
                        },
                        onPhotoDate = {
                            onPhotoDate(it)
                            menuExpanded = false
                        },
                        onPhotoSort = {
                            onPhotoSort(it)
                            menuExpanded = false
                        },
                        onPhotoBatch = {
                            onPhotoBatch(it)
                            menuExpanded = false
                        },
                        onVideoDate = {
                            onVideoDate(it)
                            menuExpanded = false
                        },
                        onVideoSort = {
                            onVideoSort(it)
                            menuExpanded = false
                        },
                        onVideoBatch = {
                            onVideoBatch(it)
                            menuExpanded = false
                        },
                    )
                }
            }
        }
    }
}

private enum class KeepixMenuPanel {
    ROOT,
    RANGE,
    SORT,
    BATCH,
}

private data class KeepixPreviewItem(
    val uri: String,
    val name: String,
    val video: Boolean,
    val aspectRatio: Float,
)

private fun keepixPhotoAspectRatio(width: Int, height: Int): Float =
    ((width.takeIf { it > 0 } ?: 1).toFloat() / (height.takeIf { it > 0 } ?: 1).toFloat()).coerceIn(0.42f, 2.20f)

@Composable
private fun KeepixQueueSelector(
    selectedIsPhoto: Boolean,
    mediaType: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(50.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF2D2D2D).copy(alpha = 0.72f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(19.dp), tint = Color.White.copy(alpha = 0.84f))
            Icon(if (selectedIsPhoto) Icons.Rounded.PhotoLibrary else Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
            Text(
                if (selectedIsPhoto) photoTypeMenuLabel(mediaType) else "视频",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun KeepixQueueMenu(
    modifier: Modifier,
    selectedIsPhoto: Boolean,
    mediaType: String,
    photoScope: CleaningScope,
    videoScope: CleaningScope,
    settings: AppSettings,
    panel: KeepixMenuPanel,
    onToggle: () -> Unit,
    onPanel: (KeepixMenuPanel) -> Unit,
    onPhotoTab: () -> Unit,
    onVideoTab: () -> Unit,
    onPhotoType: (String) -> Unit,
    onPhotoDate: (String) -> Unit,
    onPhotoSort: (String) -> Unit,
    onPhotoBatch: (Int) -> Unit,
    onVideoDate: (String) -> Unit,
    onVideoSort: (String) -> Unit,
    onVideoBatch: (Int) -> Unit,
) {
    val activeBatch = if (selectedIsPhoto) settings.photoBatchSize else settings.videoBatchSize
    val activeDateMode = if (selectedIsPhoto) photoScope.dateMode else videoScope.dateMode
    val activeSortOrder = if (selectedIsPhoto) photoScope.sortOrder else videoScope.sortOrder
    val togglePanel: (KeepixMenuPanel) -> Unit = { target ->
        onPanel(if (panel == target) KeepixMenuPanel.ROOT else target)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF363636).copy(alpha = 0.76f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(19.dp), tint = Color.White.copy(alpha = 0.84f))
                Icon(if (selectedIsPhoto) Icons.Rounded.PhotoLibrary else Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                Text(
                    if (selectedIsPhoto) photoTypeMenuLabel(mediaType) else "视频",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp).height(1.dp).background(Color.White.copy(alpha = 0.10f)))
            KeepixMenuRow("照片", selectedIsPhoto && photoScope.mediaType == "all", Icons.Rounded.PhotoLibrary) { onPhotoType("all") }
            KeepixMenuRow("截屏", selectedIsPhoto && photoScope.mediaType == "screenshot", Icons.Rounded.Image) { onPhotoType("screenshot") }
            KeepixMenuRow("自拍", selectedIsPhoto && photoScope.mediaType == "selfie", Icons.Rounded.Image) { onPhotoType("selfie") }
            KeepixMenuRow("实况", selectedIsPhoto && photoScope.mediaType == "motion", Icons.Rounded.PlayCircle) { onPhotoType("motion") }
            KeepixMenuRow("动图", selectedIsPhoto && photoScope.mediaType == "gif", Icons.Rounded.Image) { onPhotoType("gif") }
            KeepixMenuRow("视频", !selectedIsPhoto, Icons.Rounded.Movie, onClick = onVideoTab)
            Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
            KeepixMenuRow(
                title = "整理范围",
                selected = false,
                icon = Icons.Rounded.CalendarToday,
                value = dateModeMenuLabel(activeDateMode),
                showChevron = true,
                expanded = panel == KeepixMenuPanel.RANGE,
                onClick = { togglePanel(KeepixMenuPanel.RANGE) },
            )
            AnimatedVisibility(visible = panel == KeepixMenuPanel.RANGE) {
                KeepixChoiceList(
                    options = listOf("all", "year", "month", "seven_days", "today_history").map { it to dateModeMenuLabel(it) },
                    selected = activeDateMode,
                    onSelect = { if (selectedIsPhoto) onPhotoDate(it) else onVideoDate(it) },
                )
            }
            KeepixMenuRow(
                title = "排序",
                selected = false,
                icon = Icons.Rounded.Share,
                value = if (activeSortOrder == "newest") "最新在前" else "随机",
                showChevron = true,
                expanded = panel == KeepixMenuPanel.SORT,
                onClick = { togglePanel(KeepixMenuPanel.SORT) },
            )
            AnimatedVisibility(visible = panel == KeepixMenuPanel.SORT) {
                KeepixChoiceList(
                    options = listOf("random" to "随机", "newest" to "最新在前"),
                    selected = activeSortOrder,
                    onSelect = { if (selectedIsPhoto) onPhotoSort(it) else onVideoSort(it) },
                )
            }
            KeepixMenuRow(
                title = "每组数量",
                selected = false,
                icon = Icons.Rounded.Tune,
                value = activeBatch.toString(),
                showChevron = true,
                expanded = panel == KeepixMenuPanel.BATCH,
                onClick = { togglePanel(KeepixMenuPanel.BATCH) },
            )
            AnimatedVisibility(visible = panel == KeepixMenuPanel.BATCH) {
                KeepixBatchSlider(
                    value = activeBatch,
                    onSelect = { size -> if (selectedIsPhoto) onPhotoBatch(size) else onVideoBatch(size) },
                )
            }
        }
    }
}

@Composable
private fun KeepixBatchSlider(value: Int, onSelect: (Int) -> Unit) {
    var localValue by remember(value) { mutableStateOf(value.coerceIn(1, 100).toFloat()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("每组数量", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.62f))
            Text((localValue + 0.5f).toInt().coerceIn(1, 100).toString(), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },
            valueRange = 1f..100f,
            steps = 98,
            onValueChangeFinished = { onSelect((localValue + 0.5f).toInt().coerceIn(1, 100)) },
        )
    }
}

@Composable
private fun KeepixSubMenuHeader(title: String, onBack: () -> Unit) {
    KeepixMenuRow(
        title = title,
        selected = false,
        icon = Icons.Rounded.KeyboardArrowDown,
        value = "返回",
        onClick = onBack,
    )
    Box(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp).height(1.dp).background(Color.White.copy(alpha = 0.12f)))
}

@Composable
private fun KeepixChoiceList(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        options.forEach { (value, label) ->
            KeepixMenuRow(
                title = label,
                selected = value == selected,
                icon = Icons.Rounded.Tune,
                onClick = { onSelect(value) },
            )
        }
    }
}

@Composable
private fun KeepixMenuRow(
    title: String,
    selected: Boolean,
    icon: ImageVector,
    value: String? = null,
    showChevron: Boolean = false,
    expanded: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent,
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (selected) "✓" else "", modifier = Modifier.width(16.dp), style = MaterialTheme.typography.titleMedium, color = Color.White)
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp), tint = Color.White.copy(alpha = 0.88f))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.62f), maxLines = 1)
            }
            if (showChevron) {
                Text(if (expanded) "⌃" else "›", style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.56f), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun photoTypeMenuLabel(type: String): String = when (type) {
    "screenshot" -> "截屏"
    "selfie" -> "自拍"
    "motion" -> "实况"
    "gif" -> "动图"
    "raw" -> "RAW"
    "long" -> "长图"
    "normal" -> "照片"
    else -> "照片"
}

private fun dateModeMenuLabel(mode: String): String = when (mode) {
    "seven_days" -> "最近 7 天"
    "month" -> "最近 1 个月"
    "year" -> "最近 1 年"
    "today_history" -> "回到那天"
    else -> if (mode.startsWith("d:")) mode.removePrefix("d:").replace("-", "/") else "全部"
}

private fun nextDateMode(mode: String): String = when (mode) {
    "all" -> "year"
    "year" -> "month"
    "month" -> "seven_days"
    else -> "all"
}

@Composable
private fun KeepixHomeCircle(icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.13f),
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.White.copy(alpha = 0.86f))
        }
    }
}

@Composable
private fun KeepixHomeDeck(
    items: List<KeepixPreviewItem>,
    preparing: Boolean,
    selectedIsPhoto: Boolean,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val deckClickSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val visibleItems = items.take(3)
    val visibleKey = visibleItems.joinToString("|") { it.uri }
    val deckEntryOffsets = remember { List(3) { Animatable(0f) } }
    val deckEntryStartPx = with(density) { 420.dp.toPx() }
    val deckOpenProgress = remember { Animatable(0f) }
    var opening by remember { mutableStateOf(false) }
    LaunchedEffect(visibleKey, deckEntryStartPx) {
        if (visibleKey.isNotEmpty()) {
            deckEntryOffsets.forEachIndexed { index, offset ->
                offset.snapTo(deckEntryStartPx + index * with(density) { 42.dp.toPx() })
            }
            deckEntryOffsets.forEachIndexed { index, offset ->
                launch {
                    offset.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 620 + index * 60,
                            delayMillis = index * 85,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(650.dp)
            .graphicsLayer {
                val progress = deckOpenProgress.value
                scaleX = 1f + progress * 0.14f
                scaleY = 1f + progress * 0.14f
            }
            .clickable(
                interactionSource = deckClickSource,
                indication = null,
                onClick = {
                    if (opening) return@clickable
                    opening = true
                    scope.launch {
                        deckOpenProgress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
                        onClick()
                        delay(260)
                        deckOpenProgress.snapTo(0f)
                        opening = false
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (visibleItems.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth(0.88f)
                    .height(500.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (preparing) {
                        if (selectedIsPhoto) "正在准备照片" else "正在准备视频"
                    } else {
                        if (selectedIsPhoto) "当前没有待整理照片" else "当前没有待整理视频"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.62f),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            val rotations = listOf(6f, -7f, 9f)
            val offsetsX = listOf(18.dp, -78.dp, 86.dp)
            val offsetsY = listOf(-8.dp, 76.dp, 118.dp)
            val fills = listOf(0.88f, 0.84f, 0.78f)
            visibleItems.reversed().forEachIndexed { reversedIndex, item ->
                val originalIndex = visibleItems.lastIndex - reversedIndex
                val isTop = originalIndex == 0
                val shape = RoundedCornerShape(22.dp)
                val entryStart = deckEntryStartPx + originalIndex * with(density) { 42.dp.toPx() }
                val entryOffset = deckEntryOffsets.getOrNull(originalIndex)?.value ?: 0f
                val entryProgress = (1f - entryOffset / entryStart.coerceAtLeast(1f)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(fills.getOrElse(originalIndex) { if (isTop) 0.86f else 0.90f })
                        .aspectRatio(item.aspectRatio)
                        .graphicsLayer {
                            rotationZ = rotations.getOrElse(originalIndex) { 0f }
                            translationX = offsetsX.getOrElse(originalIndex) { 0.dp }.toPx()
                            translationY = offsetsY.getOrElse(originalIndex) { 0.dp }.toPx() + entryOffset
                            alpha = entryProgress
                            val baseScale = if (isTop) 1f else 0.96f
                            val entryScale = 0.92f + entryProgress * 0.08f
                            scaleX = baseScale * entryScale
                            scaleY = baseScale * entryScale
                        }
                        .clip(shape),
                ) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(Uri.parse(item.uri))
                            .memoryCacheKey(item.uri + "#home_deck")
                            .diskCacheKey(item.uri)
                            .placeholderMemoryCacheKey(item.uri + "#home_deck")
                            .size(900, 1200)
                            .crossfade(false)
                            .build(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.28f))
                            )
                        )
                    )
                    if (item.video) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.46f),
                            contentColor = Color.White,
                        ) {
                            Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.padding(8.dp).size(24.dp))
                        }
                    }
                }
            }
        }
}
}



