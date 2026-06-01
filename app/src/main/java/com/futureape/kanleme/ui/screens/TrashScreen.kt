package com.futureape.kanleme.ui.screens

import android.app.Activity
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
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
    val items by viewModel.trashItems.collectAsStateWithLifecycle()
    var previewItem by remember { mutableStateOf<TrashItemEntity?>(null) }
    val context = LocalContext.current
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val systemDeleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteAction?.invoke()
        }
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
            ScreenHeader("回收站", "共 " + items.size + " 个项目 · 30 天安全期内可恢复", onBack)
            if (items.isEmpty()) {
                EmptyState("回收站为空", "上滑待删或删除视频后，会先进入这里；你可以恢复或永久删除。", "返回整理", onBack, modifier = Modifier.padding(18.dp))
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { requestSystemDeleteAuthorization(items) { viewModel.permanentlyDeleteAllTrash() } }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = null); Text(" 全部永久删除")
                    }
                }
                LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { item ->
                        TrashRow(
                            item = item,
                            onPreview = { previewItem = item },
                            onRestore = { viewModel.restoreTrash(item.id) },
                            onDelete = { requestSystemDeleteAuthorization(listOf(item)) { viewModel.permanentlyDeleteTrash(item.id) } },
                        )
                    }
                }
            }
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
private fun TrashRow(
    item: TrashItemEntity,
    onPreview: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = Uri.parse(item.uri),
                contentDescription = item.displayName,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onPreview),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f)) {
                Text(item.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text("${item.mediaType.toReadable()} · ${formatSize(item.size)} · 约 ${item.daysLeft()} 天后自动清理", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallTrashButton("预览", Icons.Rounded.Visibility, onPreview)
                    SmallTrashButton("恢复", Icons.Rounded.Restore, onRestore)
                    SmallTrashButton("永久删除", Icons.Rounded.DeleteForever, onDelete)
                }
            }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        ZoomableTrashMedia(
            item = item,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "关闭预览", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(item.displayName, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    item.folderName + " · " + formatSize(item.size) + " · 双击或双指缩放",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(14.dp),
            shape = RoundedCornerShape(30.dp),
            tonalAlpha = 0.72f,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TrashPreviewAction("恢复", Icons.Rounded.Restore, onRestore)
                TrashPreviewAction("永久删除", Icons.Rounded.DeleteForever, onDelete)
            }
        }
    }
}

@Composable
private fun ZoomableTrashMedia(
    item: TrashItemEntity,
    modifier: Modifier = Modifier,
) {
    var scale by remember(item.id) { mutableStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember(item.id) { mutableStateOf(IntSize.Zero) }

    fun clampOffset(value: Offset, targetScale: Float): Offset {
        if (targetScale <= 1.01f || containerSize.width <= 0 || containerSize.height <= 0) return Offset.Zero
        val maxX = containerSize.width * (targetScale - 1f) / 2f
        val maxY = containerSize.height * (targetScale - 1f) / 2f
        return Offset(
            x = value.x.coerceIn(-maxX, maxX),
            y = value.y.coerceIn(-maxY, maxY),
        )
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
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
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
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun SmallTrashButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

private fun String.toReadable(): String = if (this == "video") "视频" else "照片"
private fun TrashItemEntity.daysLeft(): Int {
    val left = autoDeleteAt - System.currentTimeMillis()
    return ceil(left / (24.0 * 60.0 * 60.0 * 1000.0)).toInt().coerceAtLeast(0)
}
