package com.futureape.kanleme.ui.screens

import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.futureape.kanleme.data.local.TrashItemEntity
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.util.formatDate
import com.futureape.kanleme.ui.util.formatDuration
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.openMediaInSystemGallery
import com.futureape.kanleme.ui.util.shareMedia
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlin.math.ceil
import com.futureape.kanleme.ui.i18n.Text

@Composable
fun TrashScreen(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
    var previewItem by remember { mutableStateOf<TrashItemEntity?>(null) }
    var selectedMediaType by remember { mutableStateOf("photo") }
    val context = LocalContext.current
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val systemDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) pendingDeleteAction?.invoke()
        pendingDeleteAction = null
    }

    fun requestSystemDeleteAuthorization(targets: List<TrashItemEntity>, onConfirmed: () -> Unit) {
        val uris = targets.mapNotNull { item -> runCatching { Uri.parse(item.uri) }.getOrNull() }
        if (uris.isEmpty()) {
            viewModel.showMessage("媒体地址无效，无法调起系统删除确认")
            return
        }
        runCatching {
            pendingDeleteAction = onConfirmed
            val request = MediaStore.createDeleteRequest(context.contentResolver, uris)
            systemDeleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        }.onFailure {
            pendingDeleteAction = null
            viewModel.showMessage("无法调起系统删除确认，请稍后再试")
        }
    }

    val photoTrashItems = trashItems.filter { it.mediaType != "video" }
    val videoTrashItems = trashItems.filter { it.mediaType == "video" }
    val visibleTrashItems = if (selectedMediaType == "video") videoTrashItems else photoTrashItems

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            ScreenHeader(if (selectedMediaType == "video") "视频回收站" else "照片回收站", "照片和视频分开处理，30 天安全期内可恢复", onBack)
            if (trashItems.isEmpty()) {
                EmptyState("回收站为空", "上滑待删或删除视频后，会先进入这里；你可以恢复或永久删除。", "返回整理", onBack, modifier = Modifier.padding(18.dp))
            } else {
                TrashGalleryHeader(
                    items = trashItems,
                    selectedMediaType = selectedMediaType,
                    onTypeSelected = { selectedMediaType = it; previewItem = null },
                )
                AnimatedContent(
                    targetState = selectedMediaType,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val forward = targetState == "video"
                        (
                            fadeIn(tween(150, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { if (forward) it / 4 else -it / 4 }
                            ).togetherWith(
                            fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                                slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { if (forward) -it / 5 else it / 5 }
                        )
                    },
                    label = "trash_media_type",
                ) { type ->
                    val typedItems = if (type == "video") videoTrashItems else photoTrashItems
                    if (typedItems.isEmpty()) {
                        EmptyState(
                            title = if (type == "video") "视频回收站为空" else "照片回收站为空",
                            message = if (type == "video") "待删视频会单独显示在这里，不会和照片混在一起。" else "待删照片会单独显示在这里，不会和视频混在一起。",
                            actionText = "切换类别",
                            onAction = { selectedMediaType = if (type == "video") "photo" else "video" },
                            modifier = Modifier.fillMaxSize().padding(18.dp),
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(104.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 104.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(typedItems, key = { it.id }) { item ->
                                TrashGridTile(
                                    item = item,
                                    onPreview = { previewItem = item },
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = visibleTrashItems.isNotEmpty(),
            enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) + slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it / 3 },
            exit = fadeOut(tween(130, easing = FastOutSlowInEasing)) + slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            TrashBottomBar(
                items = visibleTrashItems,
                onRestoreAll = { visibleTrashItems.forEach { viewModel.restoreTrash(it.id) } },
                onDeleteAll = {
                    val targets = visibleTrashItems
                    requestSystemDeleteAuthorization(targets) { viewModel.confirmTrashDeleted(targets.map { it.id }) }
                },
            )
        }

        previewItem?.let { item ->
            TrashPreviewOverlay(
                items = visibleTrashItems,
                initialItem = item,
                onClose = { previewItem = null },
                onRestore = { target ->
                    viewModel.restoreTrash(target.id)
                    previewItem = null
                },
                onDelete = { target ->
                    requestSystemDeleteAuthorization(listOf(target)) { viewModel.confirmTrashDeleted(target.id) }
                    previewItem = null
                },
            )
        }
    }
}

@Composable
private fun TrashGalleryHeader(
    items: List<TrashItemEntity>,
    selectedMediaType: String,
    onTypeSelected: (String) -> Unit,
) {
    val photoCount = items.count { it.mediaType != "video" }
    val videoCount = items.size - photoCount
    val selectedCount = if (selectedMediaType == "video") videoCount else photoCount
    val selectedSize = items.filter { if (selectedMediaType == "video") it.mediaType == "video" else it.mediaType != "video" }.sumOf { it.size }
    GlassSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(30.dp), tonalAlpha = 0.72f) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (selectedMediaType == "video") "视频安全回收" else "照片安全回收", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(selectedCount.toString() + " 个项目等待确认", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("预计可释放 " + formatSize(selectedSize), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), contentColor = MaterialTheme.colorScheme.primary) {
                    Text("30 天内可恢复", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrashFilterPill("照片 " + photoCount, selected = selectedMediaType != "video", onClick = { onTypeSelected("photo") })
                TrashFilterPill("视频 " + videoCount, selected = selectedMediaType == "video", onClick = { onTypeSelected("video") })
            }
        }
    }
}

@Composable
private fun TrashFilterPill(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.46f),
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TrashGridTile(item: TrashItemEntity, onPreview: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.76f)
            .trashTileEnter(item.id)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPreview),
    ) {
        AsyncImage(
            model = Uri.parse(item.uri),
            contentDescription = item.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent, Color.Black.copy(alpha = 0.34f)))))
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.50f),
            contentColor = Color.White,
        ) {
            Text("${item.daysLeft()}天", modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
        }
        Icon(
            if (item.mediaType == "video") Icons.Rounded.Movie else Icons.Rounded.Photo,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.42f), CircleShape)
                .padding(4.dp)
                .size(16.dp),
        )
    }
}

@Composable
private fun Modifier.trashTileEnter(key: Any): Modifier {
    var entered by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) { entered = true }
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "trash_tile_alpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.96f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "trash_tile_scale",
    )
    return this.graphicsLayer {
        this.alpha = alpha
        scaleX = scale
        scaleY = scale
    }
}

@Composable
private fun TrashBottomBar(
    items: List<TrashItemEntity>,
    onRestoreAll: () -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(14.dp),
        shape = RoundedCornerShape(30.dp),
        tonalAlpha = 0.80f,
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TrashBottomAction(
                text = "恢复全部",
                icon = Icons.Rounded.Restore,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = onRestoreAll,
            )
            TrashBottomAction(
                text = "永久删除",
                icon = Icons.Rounded.DeleteForever,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
                onClick = onDeleteAll,
            )
        }
    }
}

@Composable
private fun TrashBottomAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(54.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashPreviewOverlay(
    items: List<TrashItemEntity>,
    initialItem: TrashItemEntity,
    onClose: () -> Unit,
    onRestore: (TrashItemEntity) -> Unit,
    onDelete: (TrashItemEntity) -> Unit,
) {
    BackHandler(onBack = onClose)
    if (items.isEmpty()) return
    val context = LocalContext.current
    val startIndex = items.indexOfFirst { it.id == initialItem.id }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = startIndex) { items.size }
    val currentItem = items.getOrNull(pagerState.currentPage) ?: initialItem

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            key = { page -> items.getOrNull(page)?.id ?: page },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = items.getOrNull(page) ?: return@HorizontalPager
            ZoomableTrashMedia(item = item, modifier = Modifier.fillMaxSize())
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(92.dp)
                .background(Color.Black.copy(alpha = 0.46f))
                .statusBarsPadding()
                .padding(horizontal = 12.dp),
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.a11y_close_preview), tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Text(
                (pagerState.currentPage + 1).toString() + " / " + items.size.toString(),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).clickable {
                    openMediaInSystemGallery(
                        context = context,
                        uri = Uri.parse(currentItem.uri),
                        mimeType = currentItem.mimeType,
                        title = currentItem.displayName,
                    )
                },
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.10f),
                contentColor = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("在相册中查看", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.68f),
                            Color.Black.copy(alpha = 0.95f),
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 96.dp, bottom = 18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    formatDate(currentItem.dateTaken),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    trashPreviewSubtitle(currentItem),
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TrashPreviewWideAction(
                        text = "分享",
                        icon = Icons.Rounded.Share,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { shareMedia(context, Uri.parse(currentItem.uri), currentItem.mimeType, currentItem.displayName) },
                    )
                    TrashPreviewWideAction(
                        text = "恢复",
                        icon = Icons.Rounded.Restore,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onRestore(currentItem) },
                    )
                    TrashPreviewWideAction(
                        text = "永久删除",
                        icon = Icons.Rounded.DeleteForever,
                        color = Color(0xFFC7332F),
                        filled = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onDelete(currentItem) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableTrashMedia(item: TrashItemEntity, modifier: Modifier = Modifier) {
    var scale by remember(item.id) { mutableStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember(item.id) { mutableStateOf(IntSize.Zero) }

    fun clampOffset(value: Offset, targetScale: Float): Offset {
        if (targetScale <= 1.01f || containerSize.width <= 0 || containerSize.height <= 0) return Offset.Zero
        val maxX = containerSize.width * (targetScale - 1f) / 2f
        val maxY = containerSize.height * (targetScale - 1f) / 2f
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(item.id) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                    },
                )
            }
            .pointerInput(item.id, containerSize) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedPointers = event.changes.count { it.pressed }
                        val shouldHandleInImage = pressedPointers > 1 || scale > 1.01f
                        if (shouldHandleInImage) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
                            val nextOffset = if (nextScale <= 1.01f) Offset.Zero else clampOffset(offset + panChange, nextScale)
                            scale = nextScale
                            offset = nextOffset
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = Uri.parse(item.uri),
            contentDescription = item.displayName,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
            contentScale = ContentScale.Fit,
        )
    }
}

private fun trashPreviewSubtitle(item: TrashItemEntity): String {
    val sizePart = formatSize(item.size)
    val dimensionPart = if (item.width > 0 && item.height > 0) item.width.toString() + " x " + item.height.toString() else null
    val durationPart = item.duration?.takeIf { it > 0L }?.let { formatDuration(it) }
    return listOfNotNull(item.displayName, dimensionPart, durationPart, sizePart)
        .joinToString("  ·  ")
}

@Composable
private fun TrashPreviewWideAction(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(58.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (filled) color else Color.Transparent,
        contentColor = if (filled) Color.White else color,
        border = if (filled) null else androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.46f)),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.size(7.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

private fun TrashItemEntity.daysLeft(): Int {
    val left = autoDeleteAt - System.currentTimeMillis()
    return ceil(left / (24.0 * 60.0 * 60.0 * 1000.0)).toInt().coerceAtLeast(0)
}
