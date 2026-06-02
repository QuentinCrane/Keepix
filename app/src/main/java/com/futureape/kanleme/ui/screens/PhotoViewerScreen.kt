package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onSizeChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.util.MotionPhotoPlaybackSource
import com.futureape.kanleme.ui.util.openPhotoInSystemGallery
import com.futureape.kanleme.ui.util.resolveMotionPhotoPlaybackSource
import com.futureape.kanleme.ui.util.photoMediaKindLabel
import com.futureape.kanleme.ui.util.shareMedia
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    viewModel: KanlemeViewModel,
    initialPhotoId: Long,
    onBack: () -> Unit,
) {
    val timelinePhotos by viewModel.timelinePhotos.collectAsStateWithLifecycle()
    val cleaningDeck by viewModel.photoDeck.collectAsStateWithLifecycle()
    val folders by viewModel.photoFolders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMoveSheet by remember { mutableStateOf(false) }

    val photos = remember(cleaningDeck, timelinePhotos, initialPhotoId) {
        if (cleaningDeck.any { it.id == initialPhotoId }) cleaningDeck else timelinePhotos
    }

    if (photos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("没有可查看的照片")
        }
        return
    }

    val startIndex = remember(photos, initialPhotoId) {
        photos.indexOfFirst { it.id == initialPhotoId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { photos.size }
    LaunchedEffect(initialPhotoId, photos.size) {
        if (photos.isNotEmpty()) pagerState.scrollToPage(startIndex.coerceIn(0, photos.lastIndex))
    }
    val currentPhoto by remember { derivedStateOf { photos.getOrNull(pagerState.currentPage) } }

    var zoomedPhotoId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pagerState.currentPage) { zoomedPhotoId = null }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            userScrollEnabled = zoomedPhotoId == null,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val photo = photos[page]
            ZoomableViewerPhoto(
                photo = photo,
                modifier = Modifier.fillMaxSize(),
                onZoomChanged = { isZoomed ->
                    zoomedPhotoId = if (isZoomed) photo.id else null
                },
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 36.dp, start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "返回", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = currentPhoto?.displayName ?: "照片",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = "${pagerState.currentPage + 1} / ${photos.size} · ${currentPhoto?.folderName.orEmpty()} · ${currentPhoto?.let { photoMediaKindLabel(it) }.orEmpty()}",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        currentPhoto?.let { photo ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(14.dp),
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFF0B2431).copy(alpha = 0.92f),
                contentColor = Color(0xFFBEEBFF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7CC6F2).copy(alpha = 0.34f)),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ViewerAction(icon = Icons.Rounded.PhotoLibrary, label = if (photo.isMotionPhoto || photo.isGif) "系统查看" else "相册查看") {
                        openPhotoInSystemGallery(context, photo)
                    }
                    ViewerAction(icon = Icons.Rounded.Folder, label = "移动") { showMoveSheet = true }
                    ViewerAction(icon = Icons.Rounded.Favorite, label = "收藏") { viewModel.onPhotoAction(photo, SwipeAction.Favorite) }
                    ViewerAction(icon = Icons.Rounded.Delete, label = "待删") { viewModel.onPhotoAction(photo, SwipeAction.Delete) }
                    ViewerAction(icon = Icons.Rounded.Share, label = "分享") {
                        shareMedia(context, Uri.parse(photo.uri), photo.mimeType, photo.displayName)
                    }
                }
            }
        }
    }

    if (showMoveSheet && currentPhoto != null) {
        MoveFolderSheet(
            currentPhoto = currentPhoto!!,
            knownFolders = folders,
            onDismiss = { showMoveSheet = false },
            onMove = { path ->
                viewModel.movePhotoToFolder(currentPhoto!!, path)
                showMoveSheet = false
            },
        )
    }
}


@Composable
private fun ZoomableViewerPhoto(
    photo: PhotoEntity,
    modifier: Modifier = Modifier,
    onZoomChanged: (Boolean) -> Unit,
) {
    var scale by remember(photo.id) { mutableStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember(photo.id) { mutableStateOf(IntSize.Zero) }
    var motionSource by remember(photo.id) { mutableStateOf<MotionPhotoPlaybackSource.Ready?>(null) }
    var motionLoading by remember(photo.id) { mutableStateOf(false) }
    var motionError by remember(photo.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canPlayMotion = photo.isMotionPhoto && !photo.isGif

    fun clampOffset(value: Offset, targetScale: Float): Offset {
        if (targetScale <= 1.01f || containerSize.width <= 0 || containerSize.height <= 0) return Offset.Zero
        val maxX = containerSize.width * (targetScale - 1f) / 2f
        val maxY = containerSize.height * (targetScale - 1f) / 2f
        return Offset(
            x = value.x.coerceIn(-maxX, maxX),
            y = value.y.coerceIn(-maxY, maxY),
        )
    }

    LaunchedEffect(scale, motionSource) {
        onZoomChanged(scale > 1.01f || motionSource != null)
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(photo.id, motionSource) {
                if (motionSource == null) {
                    detectTapGestures(
                        onLongPress = {
                            if (canPlayMotion && !motionLoading) {
                                motionLoading = true
                                motionError = null
                                scope.launch {
                                    when (val source = resolveMotionPhotoPlaybackSource(context, photo)) {
                                        is MotionPhotoPlaybackSource.Ready -> motionSource = source
                                        is MotionPhotoPlaybackSource.Unavailable -> motionError = source.reason
                                    }
                                    motionLoading = false
                                }
                            }
                        },
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
            }
            .pointerInput(photo.id, containerSize, motionSource) {
                if (motionSource == null) {
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
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (motionSource != null) {
            MotionPhotoVideoPlayer(
                uri = motionSource!!.uri,
                modifier = Modifier.fillMaxSize(),
                onClose = { motionSource = null },
            )
        } else {
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

            if (canPlayMotion && scale <= 1.01f) {
                GlassSurface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 106.dp, start = 18.dp, end = 18.dp),
                    shape = RoundedCornerShape(999.dp),
                    tonalAlpha = 0.74f,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (motionLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("正在准备实况", style = MaterialTheme.typography.labelMedium)
                        } else {
                            FilledTonalIconButton(
                                onClick = {
                                    motionLoading = true
                                    motionError = null
                                    scope.launch {
                                        when (val source = resolveMotionPhotoPlaybackSource(context, photo)) {
                                            is MotionPhotoPlaybackSource.Ready -> motionSource = source
                                            is MotionPhotoPlaybackSource.Unavailable -> motionError = source.reason
                                        }
                                        motionLoading = false
                                    }
                                },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "播放实况")
                            }
                            Text("长按图片或点此播放实况", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            motionError?.let { message ->
                GlassSurface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 98.dp, start = 18.dp, end = 18.dp),
                    shape = RoundedCornerShape(22.dp),
                    tonalAlpha = 0.86f,
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun MotionPhotoVideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }
    DisposableEffect(player, onClose) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) onClose()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    this.player = player
                }
            },
            update = { view -> view.player = player },
        )
        FilledTonalIconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 92.dp, end = 18.dp)
                .size(42.dp),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "关闭实况")
        }
    }
}

@Composable
private fun ViewerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
            Icon(icon, contentDescription = label)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFBEEBFF))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveFolderSheet(
    currentPhoto: PhotoEntity,
    knownFolders: List<String>,
    onDismiss: () -> Unit,
    onMove: (String) -> Unit,
) {
    val presets = listOf(
        "Pictures/回留精选/",
        "Pictures/回留待整理/",
        "DCIM/回留相册/",
    )
    val options = (presets + knownFolders).distinct().filter { it.isNotBlank() }.take(12)
    var customPath by remember(currentPhoto.id) { mutableStateOf(currentPhoto.relativePath ?: "Pictures/回留精选/") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("移动到文件夹", style = MaterialTheme.typography.headlineSmall)
            Text(
                "目标路径使用相册相对路径，例如 Pictures/回留精选/。系统可能会要求照片写入授权。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            options.forEach { path ->
                TextButton(onClick = { onMove(path) }, modifier = Modifier.fillMaxWidth()) {
                    Text(path)
                }
            }
            OutlinedTextField(
                value = customPath,
                onValueChange = { customPath = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("自定义相册路径") },
                singleLine = true,
            )
            Button(
                onClick = { onMove(customPath) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("移动到该文件夹")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
