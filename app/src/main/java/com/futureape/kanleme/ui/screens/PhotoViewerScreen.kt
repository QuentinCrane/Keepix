package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
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
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.MotionPhotoPlaybackSource
import com.futureape.kanleme.ui.util.resolveMotionPhotoPlaybackSource
import com.futureape.kanleme.ui.util.photoMediaKindLabel
import com.futureape.kanleme.ui.util.shareMedia
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import com.futureape.kanleme.ui.i18n.Text

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
    var showExifSheet by remember { mutableStateOf(false) }

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

        currentPhoto?.let { photo ->
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 34.dp, start = 10.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.a11y_back), tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        text = formatViewerDateTime(photo.dateTaken),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                    )
                    Text(
                        text = photo.displayName + "  ·  " + photo.folderName,
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                }
                IconButton(onClick = { shareMedia(context, Uri.parse(photo.uri), photo.mimeType, photo.displayName) }) {
                    Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.a11y_share), tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ViewerAction(icon = Icons.Rounded.FavoriteBorder, label = "收藏照片", modifier = Modifier.weight(1f)) { viewModel.onPhotoAction(photo, SwipeAction.Favorite) }
                    ViewerAction(icon = Icons.Rounded.Folder, label = "移动", modifier = Modifier.weight(1f)) { showMoveSheet = true }
                    ViewerAction(icon = Icons.Rounded.Delete, label = "加入待删区", modifier = Modifier.weight(1f)) { viewModel.onPhotoAction(photo, SwipeAction.Delete) }
                    ViewerAction(icon = Icons.Rounded.Info, label = "EXIF信息", modifier = Modifier.weight(1f)) { showExifSheet = true }
                    ViewerAction(icon = Icons.Rounded.PhotoLibrary, label = "回到整理照片", modifier = Modifier.weight(1f)) { onBack() }
                }
            }
        }
    }

    if (showExifSheet && currentPhoto != null) {
        PhotoExifSheet(
            photo = currentPhoto!!,
            onDismiss = { showExifSheet = false },
        )
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoExifSheet(photo: PhotoEntity, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("EXIF 信息", style = MaterialTheme.typography.headlineSmall)
            ExifLine("文件名", photo.displayName)
            ExifLine("相册", photo.folderName)
            ExifLine("类型", photoMediaKindLabel(photo))
            ExifLine("尺寸", photo.width.toString() + " x " + photo.height.toString())
            ExifLine("大小", formatSize(photo.size))
            ExifLine("设备", listOfNotNull(photo.exifMake, photo.exifModel).joinToString(" ").ifBlank { "未知" })
            ExifLine("镜头", photo.exifLensModel ?: "未知")
            ExifLine("焦距", photo.exifFocalLength ?: "未知")
            ExifLine("光圈", photo.exifAperture ?: "未知")
            ExifLine("ISO", photo.exifIso ?: "未知")
            ExifLine("快门", photo.exifExposureTime ?: "未知")
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ExifLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.28f))
        Text(value.ifBlank { "未知" }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(0.72f))
    }
}

private fun formatViewerDateTime(timeMillis: Long): String {
    if (timeMillis <= 0L) return ""
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(Date(timeMillis))
}

@Composable
private fun ViewerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(31.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.78f),
            maxLines = 2,
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
                                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.a11y_play_motion))
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
            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.a11y_close_motion))
        }
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
        "Pictures/Kanleme/Favorites/",
        "Pictures/Kanleme/ToReview/",
        "DCIM/Kanleme/",
    )
    val options = (presets + knownFolders).distinct().filter { it.isNotBlank() }.take(12)
    var customPath by remember(currentPhoto.id) { mutableStateOf(currentPhoto.relativePath ?: "Pictures/Kanleme/Favorites/") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("移动到文件夹", style = MaterialTheme.typography.headlineSmall)
            Text(
                "目标路径使用相册相对路径，例如 Pictures/Kanleme/Favorites/。系统可能会要求照片写入授权。",
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
