package com.futureape.kanleme.ui.screens

import com.futureape.kanleme.ui.components.morphAnchor
import com.futureape.kanleme.ui.util.photoDisplayAspectRatio
import com.futureape.kanleme.ui.util.photoThumbnailImageRequest
import androidx.compose.runtime.remember

import android.net.Uri
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.util.formatDuration
import com.futureape.kanleme.ui.util.openVideoInSystemGallery
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    viewModel: KanlemeViewModel,
    onBack: () -> Unit,
    onOpenPhoto: (PhotoEntity) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val photos by viewModel.timelinePhotos.collectAsStateWithLifecycle()
    val videos by viewModel.timelineVideos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val showingVideos = settings.homeMediaTab == "video"
    val groupedPhotos = remember(photos) { photos.groupBy { dateTitle(it.dateTaken) } }
    val groupedVideos = remember(videos) { videos.groupBy { dateTitle(it.dateTaken) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 42.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = stringResource(if (showingVideos) R.string.timeline_all_videos_title else R.string.timeline_all_photos_title),
                subtitle = stringResource(if (showingVideos) R.string.timeline_videos_subtitle else R.string.timeline_photos_subtitle),
                onBack = onBack,
            )
        }

        if (!showingVideos && photos.isEmpty()) {
            item {
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.timeline_empty_photos_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.timeline_empty_photos_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.refreshLibrary() }) { Text(stringResource(R.string.timeline_sync)) }
                    }
                }
            }
        } else if (showingVideos && videos.isEmpty()) {
            item {
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.Movie, contentDescription = null)
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.timeline_empty_videos_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.timeline_empty_videos_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.refreshLibrary() }) { Text(stringResource(R.string.timeline_sync)) }
                    }
                }
            }
        } else if (showingVideos) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.timeline_count_items, videos.size)) })
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.timeline_count_days, groupedVideos.size)) })
                }
            }
            groupedVideos.forEach { (title, dayVideos) ->
                item(key = "video-header-$title") {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
                items(dayVideos.chunked(2), key = { row -> "videos-" + row.joinToString("-") { it.id.toString() } }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { video ->
                            TimelineVideoThumb(
                                video = video,
                                onClick = { openVideoInSystemGallery(context, video) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(2 - row.size) {
                            Spacer(Modifier.weight(1f).width(0.dp))
                        }
                    }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.timeline_count_photos, photos.size)) })
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.timeline_count_days, groupedPhotos.size)) })
                }
            }
            groupedPhotos.forEach { (title, dayPhotos) ->
                item(key = "header-$title") {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
                items(dayPhotos.chunked(3), key = { row -> row.joinToString("-") { it.id.toString() } }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { photo ->
                            TimelineThumb(
                                photo = photo,
                                onClick = { onOpenPhoto(photo) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) {
                            Spacer(Modifier.weight(1f).width(0.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineVideoThumb(
    video: VideoEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = Uri.parse(video.uri),
            contentDescription = video.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        GlassSurface(
            modifier = Modifier.align(Alignment.TopStart).padding(7.dp),
            shape = RoundedCornerShape(999.dp),
            tonalAlpha = 0.34f,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.height(14.dp), tint = androidx.compose.ui.graphics.Color.White)
                Text(formatDuration(video.duration), color = androidx.compose.ui.graphics.Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TimelineThumb(
    photo: PhotoEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val thumbnail = remember(photo.uri, photo.updatedAt) { photoThumbnailImageRequest(context, photo) }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .morphAnchor("timeline-photo:" + photo.id, 18.dp, image = thumbnail, imageAspectRatio = photoDisplayAspectRatio(photo))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = thumbnail,
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (photo.processingStatus == "favorited") {
            Text(
                text = stringResource(R.string.timeline_favorited),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            )
        }
    }
}

private fun dateTitle(epochMillis: Long): String {
    val safeMillis = epochMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
    return DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(Date(safeMillis))
}
