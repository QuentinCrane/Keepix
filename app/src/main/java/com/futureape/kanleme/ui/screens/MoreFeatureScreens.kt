package com.futureape.kanleme.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.futureape.kanleme.data.local.PhotoEntity
import com.futureape.kanleme.data.local.ProcessingStatus
import com.futureape.kanleme.data.local.TrashItemEntity
import com.futureape.kanleme.data.local.VideoEntity
import com.futureape.kanleme.data.repository.AchievementUi
import com.futureape.kanleme.ui.components.EmptyState
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.components.MetricPill
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.util.openVideoInSystemGallery
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private sealed class TodayMemoryItem {
    abstract val stableId: String
    abstract val year: Int
    abstract val dateTaken: Long
    abstract val displayName: String
    abstract val folderName: String
    abstract val size: Long
    abstract val uri: String
    abstract val status: String
    abstract val isVideo: Boolean

    data class PhotoItem(val photo: PhotoEntity) : TodayMemoryItem() {
        override val stableId: String = "photo-" + photo.id
        override val year: Int = yearOf(photo.dateTaken)
        override val dateTaken: Long = photo.dateTaken
        override val displayName: String = photo.displayName
        override val folderName: String = photo.folderName
        override val size: Long = photo.size
        override val uri: String = photo.uri
        override val status: String = photo.processingStatus
        override val isVideo: Boolean = false
    }

    data class VideoItem(val video: VideoEntity) : TodayMemoryItem() {
        override val stableId: String = "video-" + video.id
        override val year: Int = yearOf(video.dateTaken)
        override val dateTaken: Long = video.dateTaken
        override val displayName: String = video.displayName
        override val folderName: String = video.folderName
        override val size: Long = video.size
        override val uri: String = video.uri
        override val status: String = video.processingStatus
        override val isVideo: Boolean = true
    }
}

@Composable
fun TodayInHistoryScreen(
    viewModel: KanlemeViewModel,
    onBack: () -> Unit,
    onOpenPhoto: (PhotoEntity) -> Unit,
) {
    val photos by viewModel.todayInHistoryPhotos.collectAsStateWithLifecycle()
    val videos by viewModel.todayInHistoryVideos.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val memories = remember(photos, videos) {
        (photos.map { TodayMemoryItem.PhotoItem(it) } + videos.map { TodayMemoryItem.VideoItem(it) })
            .sortedByDescending { it.dateTaken }
    }
    val grouped = remember(memories) { memories.groupBy { it.year }.toSortedMap(compareByDescending { it }) }
    var expandedYears by remember(grouped.keys) { mutableStateOf(grouped.keys.take(1).toSet()) }

    Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
        ScreenHeader("当年今日", "按年份回看往年今天的照片和视频，展开后可查看状态", onBack)
        if (memories.isEmpty()) {
            EmptyState(
                "今天暂无往年记忆",
                "同步媒体库后，会自动按月日匹配往年今日的照片和视频。",
                "同步媒体库",
                { viewModel.refreshLibrary() },
                modifier = Modifier.padding(18.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    TodaySummaryCard(
                        yearCount = grouped.size,
                        photoCount = photos.size,
                        videoCount = videos.size,
                    )
                }
                grouped.forEach { (year, items) ->
                    item(key = "year-$year") {
                        TodayYearHeader(
                            year = year,
                            count = items.size,
                            photoCount = items.count { !it.isVideo },
                            videoCount = items.count { it.isVideo },
                            expanded = year in expandedYears,
                            onToggle = {
                                expandedYears = if (year in expandedYears) expandedYears - year else expandedYears + year
                            },
                        )
                    }
                    if (year in expandedYears) {
                        items(items, key = { it.stableId }) { item ->
                            TodayMemoryRow(
                                item = item,
                                onClick = {
                                    when (item) {
                                        is TodayMemoryItem.PhotoItem -> onOpenPhoto(item.photo)
                                        is TodayMemoryItem.VideoItem -> openVideoInSystemGallery(context, item.video)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaySummaryCard(yearCount: Int, photoCount: Int, videoCount: Int) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), tonalAlpha = 0.78f) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("找到 $yearCount 个年份的往年今日", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$photoCount 张照片 · $videoCount 个视频", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TodayYearHeader(
    year: Int,
    count: Int,
    photoCount: Int,
    videoCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    GlassSurface(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.70f) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (expanded) Icons.Rounded.ExpandMore else Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(year.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("$count 个媒体 · $photoCount 张照片 · $videoCount 个视频", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (expanded) "收起" else "展开", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TodayMemoryRow(item: TodayMemoryItem, onClick: () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.62f) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(76.dp).clip(RoundedCornerShape(18.dp))) {
                AsyncImage(
                    model = Uri.parse(item.uri),
                    contentDescription = item.displayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Icon(
                    if (item.isVideo) Icons.Rounded.Movie else Icons.Rounded.Photo,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.46f), CircleShape).padding(4.dp).size(16.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatTodayMemoryDate(item.dateTaken) + " · " + item.folderName + " · " + formatSize(item.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                TodayStatusPill(item.status, item.isVideo)
            }
        }
    }
}

@Composable
private fun TodayStatusPill(status: String, isVideo: Boolean) {
    val readable = when (status) {
        ProcessingStatus.KEPT -> "已保留"
        ProcessingStatus.FAVORITED -> "已收藏"
        ProcessingStatus.UNPROCESSED -> "未整理"
        else -> status
    }
    val icon = when (status) {
        ProcessingStatus.KEPT -> Icons.Rounded.CheckCircle
        ProcessingStatus.FAVORITED -> Icons.Rounded.Favorite
        else -> if (isVideo) Icons.Rounded.Movie else Icons.Rounded.Photo
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
        Text(readable, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private fun yearOf(timeMillis: Long): Int = Calendar.getInstance().apply { timeInMillis = timeMillis }.get(Calendar.YEAR)
private fun formatTodayMemoryDate(timeMillis: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timeMillis))

@Composable
fun AnnualReportScreen(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    val report by viewModel.annualReport.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.buildAnnualReport() }
    Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
        ScreenHeader("年度报告", "生成本地整理总结，不上传任何数据", onBack)
        val r = report
        if (r == null) {
            EmptyState("正在生成年度报告", "首次生成需要读取本地统计。", "刷新", { viewModel.buildAnnualReport() }, modifier = Modifier.padding(18.dp))
        } else {
            LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), tonalAlpha = 0.74f) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                            Text("${r.year} 看了么年度整理报告", style = MaterialTheme.typography.headlineMedium)
                            Text(r.styleTitle, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                            Text(r.styleDescription, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricPill("照片", r.photoCount.toString(), Modifier.weight(1f))
                        MetricPill("视频", r.videoCount.toString(), Modifier.weight(1f))
                        MetricPill("已处理", r.clearedCount.toString(), Modifier.weight(1f))
                    }
                }
                item {
                    GlassSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ReportLine(Icons.Rounded.Favorite, "收藏", r.favoriteCount.toString())
                            ReportLine(Icons.Rounded.Storage, "释放空间", formatSize(r.freedBytes))
                            ReportLine(Icons.Rounded.Folder, "最常整理文件夹", r.topFolder)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportLine(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun AchievementsScreen(viewModel: KanlemeViewModel, onBack: () -> Unit) {
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadAchievements() }
    Column(Modifier.fillMaxSize().padding(top = 36.dp)) {
        ScreenHeader("成就系统", "60+ 个成就，四级难度，本地计算", onBack)
        LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(achievements, key = { it.id }) { achievement ->
                AchievementRow(achievement)
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementUi) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), tonalAlpha = if (item.unlocked) 0.82f else 0.58f) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = if (item.unlocked) 0.24f else 0.10f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(item.difficulty, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth().height(7.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                Text("${item.current}/${item.target}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
