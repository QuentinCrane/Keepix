package com.futureape.kanleme.ui.screens

import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlin.math.ceil

@Composable
fun TrashScreen(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
    var previewItem by remember { mutableStateOf<TrashItemEntity?>(null) }
    val context = LocalContext.current
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val systemDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) pendingDeleteAction?.invoke()
        pendingDeleteAction = null
    }

    fun requestSystemDeleteAuthorization(targets: List<TrashItemEntity>, onConfirmed: () -> Unit) {
        val uris = targets.mapNotNull { item -> runCatching { Uri.parse(item.uri) }.getOrNull() }
        if (uris.isEmpty()) {
            onConfirmed()
            return
        }
        runCatching {
            pendingDeleteAction = onConfirmed
            val request = MediaStore.createDeleteRequest(context.contentResolver, uris)
            systemDeleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
        }.onFailure {
            pendingDeleteAction = null
            onConfirmed()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
            ScreenHeader("系统回收站", "以照片墙查看待恢复内容，30 天安全期内可处理", onBack)
            if (trashItems.isEmpty()) {
                EmptyState("回收站为空", "上滑待删或删除视频后，会先进入这里；你可以恢复或永久删除。", "返回整理", onBack, modifier = Modifier.padding(18.dp))
            } else {
                TrashGalleryHeader(items = trashItems)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(104.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(trashItems, key = { it.id }) { item ->
                        TrashGridTile(
                            item = item,
                            onPreview = { previewItem = item },
                        )
                    }
                }
            }
        }

        if (trashItems.isNotEmpty()) {
            TrashBottomBar(
                items = trashItems,
                onRestoreAll = { viewModel.restoreAllTrash() },
                onDeleteAll = { requestSystemDeleteAuthorization(trashItems) { viewModel.permanentlyDeleteAllTrash() } },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        previewItem?.let { item ->
            TrashPreviewOverlay(
                item = item,
                onClose = { previewItem = null },
                onRestore = {
                    viewModel.restoreTrash(item.id)
                    previewItem = null
                },
                onDelete = {
                    requestSystemDeleteAuthorization(listOf(item)) { viewModel.permanentlyDeleteTrash(item.id) }
                    previewItem = null
                },
            )
        }
    }
}

@Composable
private fun TrashGalleryHeader(items: List<TrashItemEntity>) {
    val photoCount = items.count { it.mediaType != "video" }
    val videoCount = items.size - photoCount
    val totalSize = items.sumOf { it.size }
    GlassSurface(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(30.dp), tonalAlpha = 0.72f) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("安全回收", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("${items.size} 个项目等待确认", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("预计可释放 " + formatSize(totalSize), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), contentColor = MaterialTheme.colorScheme.primary) {
                    Text("30 天内可恢复", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TrashFilterPill("全部 " + items.size, selected = true)
                TrashFilterPill("照片 $photoCount")
                TrashFilterPill("视频 $videoCount")
            }
        }
    }
}

@Composable
private fun TrashFilterPill(text: String, selected: Boolean = false) {
    Surface(
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

@Composable
private fun TrashPreviewOverlay(
    item: TrashItemEntity,
    onClose: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ZoomableTrashMedia(item = item, modifier = Modifier.fillMaxSize())
        Row(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "关闭预览", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(item.displayName, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(item.folderName + " · " + formatSize(item.size) + " · 双击或双指缩放", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(14.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF0B2431).copy(alpha = 0.92f),
            contentColor = Color(0xFFBEEBFF),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7CC6F2).copy(alpha = 0.32f)),
        ) {
            Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                TrashPreviewAction("恢复", Icons.Rounded.Restore, onRestore)
                TrashPreviewAction("永久删除", Icons.Rounded.DeleteForever, onDelete)
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

@Composable
private fun TrashPreviewAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
            Icon(icon, contentDescription = text)
        }
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color(0xFFBEEBFF))
    }
}

private fun TrashItemEntity.daysLeft(): Int {
    val left = autoDeleteAt - System.currentTimeMillis()
    return ceil(left / (24.0 * 60.0 * 60.0 * 1000.0)).toInt().coerceAtLeast(0)
}
