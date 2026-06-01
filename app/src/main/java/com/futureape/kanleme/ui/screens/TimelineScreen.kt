package com.futureape.kanleme.ui.screens

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
import androidx.compose.material.icons.rounded.PhotoLibrary
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    viewModel: KanlemeViewModel,
    onBack: () -> Unit,
    onOpenPhoto: (PhotoEntity) -> Unit,
) {
    val photos by viewModel.timelinePhotos.collectAsStateWithLifecycle()
    val grouped = remember(photos) { photos.groupBy { dateTitle(it.dateTaken) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 42.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ScreenHeader(
                title = "全相册时间轴",
                subtitle = "按拍摄时间浏览已同步照片，点开后左右滑动查看。",
                onBack = onBack,
            )
        }

        if (photos.isEmpty()) {
            item {
                GlassSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.height(10.dp))
                        Text("还没有照片索引", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "请先同步本机媒体库。Android 14 如果只授予部分照片，这里只会显示已授权内容。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.refreshLibrary() }) { Text("同步本机媒体库") }
                    }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("共 ${photos.size} 张") })
                    AssistChip(onClick = {}, label = { Text("${grouped.size} 天") })
                }
            }
            grouped.forEach { (title, dayPhotos) ->
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
private fun TimelineThumb(
    photo: PhotoEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = Uri.parse(photo.uri),
            contentDescription = photo.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (photo.processingStatus == "favorited") {
            Text(
                text = "已收藏",
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
    return SimpleDateFormat("yyyy年M月d日 E", Locale.CHINA).format(Date(safeMillis))
}
