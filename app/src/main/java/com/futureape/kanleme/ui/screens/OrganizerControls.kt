package com.futureape.kanleme.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.futureape.kanleme.data.repository.SwipeAction
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.util.formatSize
import java.util.Calendar
import java.util.Locale
import com.futureape.kanleme.ui.i18n.Text

@Composable
fun OrganizerProgressPill(
    progressPercent: Int,
    remainingCount: Int,
    remainingLabel: String,
    releasableBytes: Long,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val percent = progressPercent.coerceIn(0, 100)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (compact) 10.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (compact) Arrangement.Center else Arrangement.SpaceBetween,
            ) {
                Text(
                    percent.toString() + "%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (!compact) {
                    Text(
                        "剩余 " + remainingCount + " " + remainingLabel + " · 可释放 " + formatSize(releasableBytes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrganizerDatePickerAnimatedOverlay(
    visible: Boolean,
    title: String,
    currentMode: String,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = slideInVertically(tween(260)) { it / 5 } + fadeIn(tween(180)),
        exit = slideOutVertically(tween(220)) { it / 5 } + fadeOut(tween(170)),
    ) {
        OrganizerDatePickerOverlay(
            title = title,
            currentMode = currentMode,
            onApply = onApply,
            onDismiss = onDismiss,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrganizerDatePickerOverlay(
    title: String,
    currentMode: String,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { Calendar.getInstance() }
    val currentYear = remember { now.get(Calendar.YEAR) }
    var selectedYear by remember(currentMode) { mutableIntStateOf(parseYear(currentMode) ?: currentYear) }
    val selectedMonths = remember(currentMode) { mutableStateListOf<String>().apply { addAll(parseMonths(currentMode)) } }
    val years = remember(currentYear) { (currentYear downTo currentYear - 14).toList() }
    val months = remember { (1..12).toList() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.a11y_back))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        organizerDateModeLabel(currentMode),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onApply("all"); onDismiss() }) {
                    Text("全部")
                }
            }

            Text(
                "选择年份",
                modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(188.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(years) { year ->
                    val selected = year == selectedYear
                    Surface(
                        modifier = Modifier.combinedClickable(
                            onClick = { selectedYear = year },
                            onLongClick = {
                                selectedYear = year
                                val monthsInYear = (1..12).map { monthToken(year, it) }
                                if (selectedMonths.any { it in monthsInYear }) {
                                    selectedMonths.removeAll(monthsInYear.toSet())
                                } else {
                                    selectedMonths.addAll(monthsInYear.filterNot { it in selectedMonths })
                                }
                            },
                        ),
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.26f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
                    ) {
                        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                            Text(year.toString(), fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        selectedYear.toString() + " 年",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        if (selectedMonths.isEmpty()) "点选月份，长按月份可多选" else "已选 " + selectedMonths.size + " 个月",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { onApply("y:" + selectedYear); onDismiss() }) {
                    Text("整年")
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(months) { month ->
                    val token = monthToken(selectedYear, month)
                    val selected = token in selectedMonths
                    Surface(
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (selectedMonths.isEmpty()) {
                                    onApply("ym:" + token)
                                    onDismiss()
                                } else if (selected) {
                                    selectedMonths.remove(token)
                                } else {
                                    selectedMonths.add(token)
                                }
                            },
                            onLongClick = {
                                if (selected) selectedMonths.remove(token) else selectedMonths.add(token)
                            },
                        ),
                        shape = RoundedCornerShape(22.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.30f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(month.toString() + "月", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (selected) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.padding(4.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val sorted = selectedMonths.distinct().sorted()
                    if (sorted.isNotEmpty()) onApply("multiym:" + sorted.joinToString(",")) else onApply("all")
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                enabled = selectedMonths.isNotEmpty(),
            ) {
                Text("确定多选")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrganizerFolderPickerOverlay(
    visible: Boolean = true,
    title: String,
    folders: List<String>,
    onArchive: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pendingFolder by remember { mutableStateOf<String?>(null) }
    val folderItems = remember(folders) {
        folders
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase(Locale.getDefault()) }
    }
    LaunchedEffect(visible) {
        if (!visible) pendingFolder = null
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + slideInVertically(tween(220)) { it / 5 },
        exit = fadeOut(tween(130)) + slideOutVertically(tween(180)) { it / 5 },
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.a11y_close))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("选择后确认，当前媒体会直接归档并进入下一项", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(48.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(folderItems) { path ->
                        val label = folderLabel(path)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(onClick = { pendingFolder = path }),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Icon(
                                        Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(9.dp),
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val target = pendingFolder
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingFolder = null },
            icon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
            title = { Text("确定归档？") },
            text = { Text("移动到 " + folderLabel(target) + " 后，会直接进入下一项") },
            confirmButton = {
                TextButton(onClick = {
                    pendingFolder = null
                    onArchive(target)
                    onDismiss()
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFolder = null }) {
                    Text("取消")
                }
            },
        )
    }
}

fun folderLabel(path: String): String =
    path.trim('/').substringAfterLast('/').ifBlank { "相册" }

fun organizerDateModeLabel(mode: String): String = when {
    mode == "all" -> "全部时间"
    mode == "seven_days" -> "最近 7 天"
    mode == "month" -> "本月"
    mode == "year" -> "今年"
    mode.startsWith("y:") -> mode.removePrefix("y:") + " 年"
    mode.startsWith("ym:") -> {
        val parts = mode.removePrefix("ym:").split("-")
        val year = parts.getOrNull(0).orEmpty()
        val month = parts.getOrNull(1).orEmpty().toIntOrNull()?.toString().orEmpty()
        if (year.isNotBlank() && month.isNotBlank()) year + " 年 " + month + " 月" else "指定月份"
    }
    mode.startsWith("d:") -> mode.removePrefix("d:").replace("-", "/")
    mode.startsWith("multiym:") -> "多选月份 " + mode.removePrefix("multiym:").split(",").count { it.isNotBlank() }
    mode == "today_history" -> "当年今日"
    else -> "全部时间"
}

fun actionFeedbackColor(action: SwipeAction?, fallback: Color): Color = when (action) {
    SwipeAction.Delete -> Color(0xFFE74C4C)
    SwipeAction.Favorite -> Color(0xFFE6A63E)
    SwipeAction.Keep -> Color(0xFF74A7FF)
    null -> fallback
}

private fun parseYear(mode: String): Int? = when {
    mode.startsWith("y:") -> mode.removePrefix("y:").toIntOrNull()
    mode.startsWith("ym:") -> mode.removePrefix("ym:").substringBefore("-").toIntOrNull()
    mode.startsWith("multiym:") -> mode.removePrefix("multiym:").substringBefore(",").substringBefore("-").toIntOrNull()
    else -> null
}

private fun parseMonths(mode: String): List<String> = when {
    mode.startsWith("ym:") -> listOf(mode.removePrefix("ym:")).filter { it.matches(Regex("\\d{4}-\\d{2}")) }
    mode.startsWith("multiym:") -> mode.removePrefix("multiym:").split(",").filter { it.matches(Regex("\\d{4}-\\d{2}")) }
    else -> emptyList()
}

private fun monthToken(year: Int, month: Int): String =
    year.toString() + "-" + month.toString().padStart(2, '0')
