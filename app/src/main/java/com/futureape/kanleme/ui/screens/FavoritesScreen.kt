package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.util.MotionPhotoPlaybackSource
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.resolveMotionPhotoPlaybackSource
import com.futureape.kanleme.ui.util.openPhotoInSystemGallery
import com.futureape.kanleme.ui.util.shareMedia
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlinx.coroutines.launch
import com.futureape.kanleme.ui.i18n.Text

@Composable
fun FavoritesScreen(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    val photos by viewModel.favoritePhotos.collectAsStateWithLifecycle()
    val videos by viewModel.favoriteVideos.collectAsStateWithLifecycle()
    var previewPhoto by remember { mutableStateOf<PhotoEntity?>(null) }
    var photoColumnCount by rememberSaveable { mutableStateOf(3) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(top = 36.dp, start = 18.dp, end = 18.dp)) {
            ScreenHeader("我的收藏", "照片 " + photos.size + " 张 · 视频 " + videos.size + " 个", onBack)
            if (photos.isEmpty() && videos.isEmpty()) {
                EmptyState("暂无收藏", "在整理时下滑照片或点击视频收藏按钮，就会出现在这里。", "去同步媒体库", { viewModel.refreshLibrary() })
            } else {
                FavoritePhotosHeader(
                    photoCount = photos.size,
                    videoCount = videos.size,
                    columnCount = photoColumnCount,
                    onColumnCountChange = { photoColumnCount = it },
                )
                Spacer(Modifier.height(12.dp))
                FavoritePhotoMasonryGrid(
                    photos = photos,
                    columnCount = photoColumnCount,
                    modifier = Modifier.weight(1f),
                    onPhotoClick = { previewPhoto = it },
                )
            }
        }

        previewPhoto?.let { photo ->
            FavoritePhotoPreviewOverlay(
                photo = photo,
                onClose = { previewPhoto = null },
                onDelete = {
                    viewModel.onPhotoAction(photo, SwipeAction.Delete)
                    previewPhoto = null
                },
            )
        }
    }
}

@Composable
private fun FavoritePhotosHeader(
    photoCount: Int,
    videoCount: Int,
    columnCount: Int,
    onColumnCountChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("收藏照片 " + photoCount + " 张，收藏视频 " + videoCount + " 个", style = MaterialTheme.typography.titleMedium)
            Text(
                "原比例 · " + columnCount + " 列",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FavoriteColumnSelector(
            selectedColumnCount = columnCount,
            onSelected = onColumnCountChange,
        )
    }
}

@Composable
private fun FavoriteColumnSelector(
    selectedColumnCount: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .width(136.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(2, 3).forEach { count ->
            val selected = selectedColumnCount == count
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onSelected(count) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(count.toString() + " 列", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun FavoritePhotoMasonryGrid(
    photos: List<PhotoEntity>,
    columnCount: Int,
    modifier: Modifier = Modifier,
    onPhotoClick: (PhotoEntity) -> Unit,
) {
    val columns = remember(photos, columnCount) { distributeFavoritePhotos(photos, columnCount) }
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        columns.forEach { columnPhotos ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                columnPhotos.forEach { photo ->
                    FavoritePhotoTile(
                        photo = photo,
                        onClick = { onPhotoClick(photo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritePhotoTile(
    photo: PhotoEntity,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(favoriteOriginalAspectRatio(photo))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
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
            contentScale = ContentScale.Fit,
        )
    }
}

private fun distributeFavoritePhotos(photos: List<PhotoEntity>, requestedColumnCount: Int): List<List<PhotoEntity>> {
    val columnCount = requestedColumnCount.coerceIn(2, 3)
    val columns = List(columnCount) { mutableListOf<PhotoEntity>() }
    val estimatedHeights = FloatArray(columnCount)
    photos.forEach { photo ->
        val targetIndex = estimatedHeights.indices.minByOrNull { estimatedHeights[it] } ?: 0
        columns[targetIndex].add(photo)
        estimatedHeights[targetIndex] += 1f / favoriteOriginalAspectRatio(photo)
    }
    return columns.map { it.toList() }
}

private fun favoriteOriginalAspectRatio(photo: PhotoEntity): Float {
    val width = photo.exifWidth?.takeIf { it > 0 } ?: photo.width.takeIf { it > 0 } ?: 1
    val height = photo.exifHeight?.takeIf { it > 0 } ?: photo.height.takeIf { it > 0 } ?: 1
    return width.toFloat() / height.toFloat()
}

@Composable
private fun FavoritePhotoPreviewOverlay(
    photo: PhotoEntity,
    onClose: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canPlayMotion = (photo.isMotionPhoto || photo.motionPhotoNeedsDetection || photo.isSeparateVideo || !photo.motionVideoUri.isNullOrBlank()) && !photo.isGif
    var motionSource by remember(photo.id) { mutableStateOf<MotionPhotoPlaybackSource.Ready?>(null) }
    var motionLoading by remember(photo.id) { mutableStateOf(false) }

    fun playMotion() {
        if (!canPlayMotion || motionLoading) return
        motionLoading = true
        scope.launch {
            when (val source = resolveMotionPhotoPlaybackSource(context, photo)) {
                is MotionPhotoPlaybackSource.Ready -> motionSource = source
                is MotionPhotoPlaybackSource.Unavailable -> Unit
            }
            motionLoading = false
        }
    }

    BackHandler(onBack = onClose)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val motion = motionSource
        if (motion != null) {
            InlineMotionPhotoPlayer(
                uri = motion.uri,
                modifier = Modifier.fillMaxSize(),
                playOnce = true,
                onEnded = { motionSource = null },
            )
        } else {
            ZoomableFavoritePhoto(
                photo = photo,
                modifier = Modifier.fillMaxSize(),
                onLongPressMotion = ::playMotion,
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.a11y_close_preview), tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(photo.displayName, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    photo.folderName + " · " + formatSize(photo.size) + " · 双击或双指缩放",
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
                if (canPlayMotion) {
                    FavoritePreviewAction(if (motionLoading) "准备中" else "播放实况", Icons.Rounded.PhotoLibrary, ::playMotion)
                } else {
                    FavoritePreviewAction("相册查看", Icons.Rounded.PhotoLibrary) {
                        openPhotoInSystemGallery(context, photo)
                    }
                }
                FavoritePreviewAction("分享", Icons.Rounded.Share) {
                    shareMedia(context, Uri.parse(photo.uri), photo.mimeType, photo.displayName)
                }
                FavoritePreviewAction("待删", Icons.Rounded.Delete, onDelete)
            }
        }
    }
}

@Composable
private fun ZoomableFavoritePhoto(
    photo: PhotoEntity,
    modifier: Modifier = Modifier,
    onLongPressMotion: () -> Unit = {},
) {
    var scale by remember(photo.id) { mutableStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember(photo.id) { mutableStateOf(IntSize.Zero) }

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
            .pointerInput(photo.id) {
                detectTapGestures(
                    onLongPress = { onLongPressMotion() },
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
            .pointerInput(photo.id, containerSize) {
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
            model = Uri.parse(photo.uri),
            contentDescription = photo.displayName,
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
private fun FavoritePreviewAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
            Icon(icon, contentDescription = text)
        }
        Spacer(Modifier.height(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}
