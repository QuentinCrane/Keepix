package com.futureape.kanleme.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.components.MetricPill
import com.futureape.kanleme.ui.util.formatSize
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel

@Composable
fun MeScreen(
    viewModel: KanlemeViewModel,
    contentPadding: PaddingValues,
    onFavorites: () -> Unit,
    onTrash: () -> Unit,
    onSettings: () -> Unit,
    onSimilar: () -> Unit,
    onAchievements: () -> Unit,
    onAnnualReport: () -> Unit,
    onHelp: () -> Unit,
    onPrivacy: () -> Unit,
    onChangelog: () -> Unit,
    onDiagnosis: () -> Unit,
) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val personality = remember(dashboard) { buildPersonality(dashboard) }
    val keptCount = (dashboard.processedCount - dashboard.favoriteCount - dashboard.pendingDeleteCount).coerceAtLeast(0)
    AdaptiveCenter(maxWidth = 1080.dp) {
        LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 58.dp, bottom = contentPadding.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Surface(shape = RoundedCornerShape(999.dp), color = adaptiveSurfaceColor(0.36f), border = BorderStroke(1.dp, adaptiveBorderColor(0.50f))) {
                    Row(Modifier.clickable(onClick = onSettings).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                        Text("设置", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.60f) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(modifier = Modifier.size(58.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("本地相册整理", style = MaterialTheme.typography.headlineSmall)
                        Text("无需登录，所有整理能力本地可用", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { viewModel.refreshLibrary() }, shape = RoundedCornerShape(999.dp)) { Text("同步") }
                }
            }
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth().clickable(onClick = onAnnualReport), shape = RoundedCornerShape(28.dp), tonalAlpha = 0.66f) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Surface(modifier = Modifier.size(58.dp), shape = CircleShape, color = personality.color.copy(alpha = 0.12f)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Person, contentDescription = null, tint = personality.color) }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("你的整理性格", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(personality.title, style = MaterialTheme.typography.headlineSmall)
                        Text(personality.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(progress = { personality.progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = personality.color.copy(alpha = 0.70f), trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                    Surface(modifier = Modifier.size(96.dp), shape = RoundedCornerShape(22.dp), color = adaptiveSurfaceColor(0.42f), border = BorderStroke(1.dp, adaptiveBorderColor(0.48f))) {
                        Box(contentAlignment = Alignment.Center) { Text((personality.progress * 100).toInt().toString() + "%", color = personality.color) }
                    }
                }
            }
        }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.72f) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("  媒体统计", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.refreshLibrary() }) { Icon(Icons.Rounded.Refresh, contentDescription = "刷新") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricPill("总文件", (dashboard.photoCount + dashboard.videoCount).toString(), modifier = Modifier.weight(1f))
                        MetricPill("已处理", (dashboard.progress * 100).toInt().toString() + "%", modifier = Modifier.weight(1f))
                        MetricPill("已移出", dashboard.trashCount.toString(), modifier = Modifier.weight(1f))
                    }
                    StatLine(Icons.Rounded.Image, "照片", dashboard.processedPhotoCount.toString() + "/" + dashboard.photoCount.toString(), if (dashboard.photoCount == 0) 0f else dashboard.processedPhotoCount.toFloat() / dashboard.photoCount)
                    StatLine(Icons.Rounded.Image, "视频", dashboard.processedVideoCount.toString() + "/" + dashboard.videoCount.toString(), if (dashboard.videoCount == 0) 0f else dashboard.processedVideoCount.toFloat() / dashboard.videoCount)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Dot(Color(0xFF5AAF68)); Text("收藏 " + dashboard.favoriteCount, style = MaterialTheme.typography.bodyMedium)
                        Dot(Color(0xFF4CA3CF)); Text("保留 " + keptCount, style = MaterialTheme.typography.bodyMedium)
                        Dot(Color(0xFFE45A5A)); Text("删除 " + dashboard.trashCount, style = MaterialTheme.typography.bodyMedium)
                    }
                    Surface(modifier = Modifier.fillMaxWidth().height(66.dp).clickable(onClick = onAnnualReport), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)) {
                        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text("累计整理成果", style = MaterialTheme.typography.titleLarge)
                                Text("累计清理 " + dashboard.processedCount + " · 释放 " + formatSize(dashboard.userStats?.totalStorageFreed ?: 0L), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
        item { SectionTitleInline("常用功能") }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.66f) {
                Row(Modifier.padding(vertical = 18.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FunctionIcon("收藏", Icons.Rounded.FavoriteBorder, Color(0xFF54A76A), onFavorites)
                    FunctionIcon("回收站", Icons.Rounded.Delete, Color(0xFFD85C5C), onTrash)
                    FunctionIcon("相似", Icons.Rounded.Image, Color(0xFF7A6AA6), onSimilar)
                    FunctionIcon("成就", Icons.Rounded.EmojiEvents, Color(0xFFE4B83A), onAchievements)
                }
            }
        }
        item { SectionTitleInline("服务与支持") }
        item {
            GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), tonalAlpha = 0.76f) {
                Column {
                    SupportRow("反馈与诊断", "进入诊断排障和媒体库维护", Icons.Rounded.Feedback, onDiagnosis)
                    SupportRow("使用帮助", "与设置页使用帮助保持一致", Icons.Rounded.Help, onHelp)
                    SupportRow("整理引导", "查看照片、视频手势和相册规则", Icons.Rounded.Refresh, onHelp)
                    SupportRow("隐私政策", "本地存储、相册权限、删除移动说明", Icons.Rounded.PrivacyTip, onPrivacy)
                    SupportRow("更新日志", "查看版本变化和修复记录", Icons.Rounded.TipsAndUpdates, onChangelog)
                }
            }
        }
    }
    }
}

@Composable
private fun SectionTitleInline(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
}

@Composable
private fun StatLine(icon: ImageVector, label: String, value: String, progress: Float) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f), trackColor = MaterialTheme.colorScheme.surfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(8.dp).background(color, CircleShape))
}

@Composable
private fun FunctionIcon(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, badge: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.clickable(onClick = onClick)) {
        Box {
            Surface(modifier = Modifier.size(62.dp), shape = CircleShape, color = color.copy(alpha = 0.14f)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp)) }
            }
            if (badge != null) {
                Box(Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SupportRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


private data class PersonalityUi(
    val title: String,
    val description: String,
    val progress: Float,
    val color: Color,
)

private fun buildPersonality(dashboard: com.futureape.kanleme.data.repository.DashboardStats): PersonalityUi {
    val processed = dashboard.processedCount.coerceAtLeast(0)
    if (processed < 5) {
        return PersonalityUi(
            title = "正在形成中",
            description = "再整理 " + (5 - processed).coerceAtLeast(0) + " 个媒体后，会根据真实操作生成画像。",
            progress = (processed / 5f).coerceIn(0f, 1f),
            color = Color(0xFF7CC6F2),
        )
    }
    val favoriteRatio = dashboard.favoriteCount.toFloat() / processed.coerceAtLeast(1).toFloat()
    val deleteRatio = dashboard.pendingDeleteCount.toFloat() / processed.coerceAtLeast(1).toFloat()
    val videoRatio = dashboard.processedVideoCount.toFloat() / processed.coerceAtLeast(1).toFloat()
    val undoRatio = (dashboard.userStats?.totalUndoCount ?: 0).toFloat() / processed.coerceAtLeast(1).toFloat()
    return when {
        undoRatio > 0.12f -> PersonalityUi("谨慎校准型", "你会反复确认结果，撤销和回看比例更高，适合保守清理模式。", processedForUi(processed), Color(0xFF7A6AA6))
        deleteRatio > 0.45f -> PersonalityUi("果断断舍离型", "你更愿意清出空间，待删比例明显高于收藏比例。", processedForUi(processed), Color(0xFFE66A6A))
        favoriteRatio > 0.35f -> PersonalityUi("珍藏策展型", "你更常把照片留下来收藏，整理方式偏向筛选记忆。", processedForUi(processed), Color(0xFFE6A63E))
        videoRatio > 0.38f -> PersonalityUi("影像流整理型", "你处理的视频占比较高，更适合短视频式连续整理。", processedForUi(processed), Color(0xFF68A7D8))
        else -> PersonalityUi("稳健平衡型", "你的保留、收藏、待删比较均衡，适合持续小批量整理。", processedForUi(processed), Color(0xFF5AAF68))
    }
}

private fun processedForUi(value: Int): Float = (value / 100f).coerceIn(0.08f, 1f)


@Composable
private fun adaptiveSurfaceColor(lightAlpha: Float): Color {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    return if (oledDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f) else Color.White.copy(alpha = lightAlpha)
}

@Composable
private fun adaptiveBorderColor(lightAlpha: Float): Color {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    return if (oledDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.58f) else Color.White.copy(alpha = lightAlpha)
}
