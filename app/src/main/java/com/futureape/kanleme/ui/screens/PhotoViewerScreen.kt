package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import coil3.compose.AsyncImage
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.MotionPhotoPlaybackSource
import com.futureape.kanleme.ui.util.photoImageRequest
import com.futureape.kanleme.ui.util.photoContentUri
import com.futureape.kanleme.ui.components.LongImageViewer
import com.futureape.kanleme.ui.components.isLongImageCandidate
import com.futureape.kanleme.ui.util.resolveMotionPhotoPlaybackSource
import com.futureape.kanleme.ui.util.photoMediaKindLabel
import com.futureape.kanleme.ui.util.openPhotoInSystemGallery
import com.futureape.kanleme.ui.components.MorphPanel
import com.futureape.kanleme.ui.components.morphAnchor
import com.futureape.kanleme.ui.util.shareMedia
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    viewModel: KanlemeViewModel,
    initialPhotoId: Long,
    onBack: () -> Unit,
) {
    val timelinePhotos by viewModel.timelinePhotos.collectAsStateWithLifecycle()
    val cleaningDeck by viewModel.photoDeck.collectAsStateWithLifecycle()
    val dayMemoryPhotos by viewModel.photoDayMemoryWindow.collectAsStateWithLifecycle()
    val cleanedHistoryPhotos by viewModel.cleanedPhotoHistory.collectAsStateWithLifecycle()
    val favoritePhotos by viewModel.favoritePhotos.collectAsStateWithLifecycle()
    val todayInHistoryPhotos by viewModel.todayInHistoryPhotos.collectAsStateWithLifecycle()
    val recentPhotos by viewModel.recentPhotos.collectAsStateWithLifecycle()
    val folders by viewModel.photoFolders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showMoveSheet by remember { mutableStateOf(false) }
    var showExifSheet by remember { mutableStateOf(false) }

    val photos = remember(
        cleaningDeck,
        dayMemoryPhotos,
        cleanedHistoryPhotos,
        favoritePhotos,
        todayInHistoryPhotos,
        recentPhotos,
        timelinePhotos,
        initialPhotoId,
    ) {
        val sources = listOf(
            cleaningDeck,
            dayMemoryPhotos,
            cleanedHistoryPhotos,
            favoritePhotos,
            todayInHistoryPhotos,
            recentPhotos,
            timelinePhotos,
        )
        sources.firstOrNull { candidates -> candidates.any { it.id == initialPhotoId } }
            ?: sources.firstOrNull { it.isNotEmpty() }
            ?: emptyList()
    }

    if (photos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.photo_viewer_empty))
        }
        return
    }

    val startIndex = remember(photos, initialPhotoId) {
        photos.indexOfFirst { it.id == initialPhotoId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { photos.size }
    LaunchedEffect(initialPhotoId, photos.size, startIndex) {
        if (photos.isNotEmpty()) pagerState.scrollToPage(startIndex.coerceIn(0, photos.lastIndex))
    }
    val currentPhoto = photos.getOrNull(pagerState.currentPage.coerceIn(0, photos.lastIndex))

    var zoomedPhotoId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pagerState.currentPage) { zoomedPhotoId = null }
    LaunchedEffect(currentPhoto?.id) {
        if (currentPhoto == null) {
            showExifSheet = false
            showMoveSheet = false
            zoomedPhotoId = null
        }
    }

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
            val onZoomChanged: (Boolean) -> Unit = { isZoomed ->
                if (page == pagerState.currentPage) {
                    zoomedPhotoId = if (isZoomed) photo.id else null
                }
            }
            ZoomableViewerPhoto(
                photo = photo,
                modifier = Modifier.fillMaxSize(),
                onZoomChanged = onZoomChanged,
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
                    ViewerAction(icon = Icons.Rounded.FavoriteBorder, label = stringResource(R.string.viewer_action_favorite), modifier = Modifier.weight(1f)) { viewModel.onPhotoAction(photo, SwipeAction.Favorite) }
                    ViewerAction(icon = Icons.Rounded.Folder, label = stringResource(R.string.viewer_action_move), modifier = Modifier.weight(1f)) { showMoveSheet = true }
                    ViewerAction(icon = Icons.Rounded.Delete, label = stringResource(R.string.viewer_action_pending_delete), modifier = Modifier.weight(1f)) { viewModel.onPhotoAction(photo, SwipeAction.Delete) }
                    ViewerAction(icon = Icons.Rounded.Info, label = stringResource(R.string.viewer_action_exif), modifier = Modifier.weight(1f)) { showExifSheet = true }
                    ViewerAction(icon = Icons.Rounded.PhotoLibrary, label = stringResource(R.string.gallery_chooser_title), modifier = Modifier.weight(1f)) { openPhotoInSystemGallery(context, photo) }
                }
            }
        }
    }

    currentPhoto?.let { photo ->
        PhotoExifSheet(
            photo = photo,
            visible = showExifSheet,
            onDismiss = { showExifSheet = false },
        )
    }

    currentPhoto?.let { photo ->
        MoveFolderSheet(
            currentPhoto = photo,
            visible = showMoveSheet,
            knownFolders = folders,
            onDismiss = { showMoveSheet = false },
            onMove = { path ->
                viewModel.movePhotoToFolder(photo, path)
                showMoveSheet = false
            },
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoExifSheet(photo: PhotoEntity, visible: Boolean, onDismiss: () -> Unit) {
    MorphPanel(visible = visible, background = MaterialTheme.colorScheme.surface, onDismiss = onDismiss,
        alignment = Alignment.BottomCenter, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.exif_title), style = MaterialTheme.typography.headlineSmall)
            ExifLine(stringResource(R.string.exif_filename), photo.displayName)
            ExifLine(stringResource(R.string.exif_album), photo.folderName)
            ExifLine(stringResource(R.string.exif_path), viewerPhotoDisplayPath(photo))
            ExifLine(stringResource(R.string.exif_content_uri), photo.uri)
            ExifLine(stringResource(R.string.exif_type), photoMediaKindLabel(photo))
            ExifLine(stringResource(R.string.exif_dimensions), photo.width.toString() + " x " + photo.height.toString())
            ExifLine(stringResource(R.string.exif_size), formatSize(photo.size))
            ExifLine(stringResource(R.string.exif_device), listOfNotNull(photo.exifMake, photo.exifModel).joinToString(" "))
            ExifLine(stringResource(R.string.exif_lens), photo.exifLensModel.orEmpty())
            ExifLine(stringResource(R.string.exif_focal_length), photo.exifFocalLength.orEmpty())
            ExifLine(stringResource(R.string.exif_aperture), photo.exifAperture.orEmpty())
            ExifLine(stringResource(R.string.exif_iso), photo.exifIso.orEmpty())
            ExifLine(stringResource(R.string.exif_shutter), photo.exifExposureTime.orEmpty())
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ExifLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.28f))
        Text(value.ifBlank { stringResource(R.string.value_unknown) }, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(0.72f))
    }
}

private fun viewerPhotoDisplayPath(photo: PhotoEntity): String {
    val basePath = photo.relativePath
        ?.replace('\\', '/')
        ?.trim()
        ?.trim('/')
        ?.takeIf { it.isNotBlank() }
        ?: photo.folderPath
            .replace('\\', '/')
            .trim()
            .trim('/')
            .takeIf { it.isNotBlank() }
    return listOfNotNull(basePath, photo.displayName.takeIf { it.isNotBlank() }).joinToString("/")
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
        modifier = modifier.morphAnchor("viewer:" + label, 24.dp, icon = icon).clickable(onClick = onClick),
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
    var longPreviewFailed by remember(photo.id, photo.uri) { mutableStateOf(false) }
    var longPreviewZoomed by remember(photo.id, photo.uri) { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val canPlayMotion = (photo.isMotionPhoto || photo.motionPhotoNeedsDetection || photo.isSeparateVideo || !photo.motionVideoUri.isNullOrBlank()) && !photo.isGif
    val useLongPreview = !longPreviewFailed && !photo.isGif &&
        isLongImageCandidate(photo.width, photo.height, photo.mimeType)

    fun playMotion() {
        if (!canPlayMotion || motionLoading) return
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

    fun clampOffset(value: Offset, targetScale: Float): Offset {
        if (targetScale <= 1.01f || containerSize.width <= 0 || containerSize.height <= 0) return Offset.Zero
        val maxX = containerSize.width * (targetScale - 1f) / 2f
        val maxY = containerSize.height * (targetScale - 1f) / 2f
        return Offset(
            x = value.x.coerceIn(-maxX, maxX),
            y = value.y.coerceIn(-maxY, maxY),
        )
    }

    LaunchedEffect(scale, motionSource, useLongPreview) {
        if (motionSource != null || !useLongPreview) {
            onZoomChanged(scale > 1.01f || motionSource != null)
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(photo.id, motionSource, useLongPreview) {
                if (motionSource == null && !useLongPreview) {
                    detectTapGestures(
                        onLongPress = { playMotion() },
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
            .pointerInput(photo.id, containerSize, motionSource, useLongPreview) {
                if (motionSource == null && !useLongPreview) {
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
        val motion = motionSource
        if (motion != null) {
            MotionPhotoVideoPlayer(
                uri = motion.uri,
                modifier = Modifier.fillMaxSize(),
                onClose = { motionSource = null },
            )
        } else {
            if (useLongPreview) {
                LongImageViewer(
                    uri = photoContentUri(photo),
                    description = photo.displayName,
                    modifier = Modifier.fillMaxSize(),
                    mimeType = photo.mimeType,
                    bottomInset = if (canPlayMotion) 175.dp else 110.dp,
                    onHorizontalPanChanged = { zoomed ->
                        longPreviewZoomed = zoomed
                        onZoomChanged(zoomed)
                    },
                    onLongPress = ::playMotion,
                    fallback = { LaunchedEffect(photo.id) { longPreviewFailed = true } },
                )
            } else {
                AsyncImage(
                    model = photoImageRequest(context, photo, "viewer_photo", 1440, 1920),
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

            if (canPlayMotion && scale <= 1.01f && (!useLongPreview || !longPreviewZoomed)) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 106.dp, start = 18.dp, end = 18.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (motionLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(stringResource(R.string.motion_preparing), style = MaterialTheme.typography.labelMedium)
                        } else {
                            FilledTonalIconButton(
                                onClick = ::playMotion,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.a11y_play_motion))
                            }
                            Text(stringResource(R.string.motion_play_hint), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            motionError?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 98.dp, start = 18.dp, end = 18.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Black.copy(alpha = 0.78f),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
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
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
            volume = 1f
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }
    val currentOnClose by rememberUpdatedState(onClose)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) currentOnClose()
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
    visible: Boolean,
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

    MorphPanel(visible = visible, background = MaterialTheme.colorScheme.surface, onDismiss = onDismiss,
        alignment = Alignment.BottomCenter, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.move_folder_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.move_folder_body),
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
                label = { Text(stringResource(R.string.move_folder_custom_path)) },
                singleLine = true,
            )
            Button(
                onClick = { onMove(customPath) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.move_folder_action))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
