package com.futureape.kanleme.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.futureape.kanleme.ui.components.GlassSurface

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            title = "看了么",
            subtitle = "像刷内容一样整理相册",
            icon = Icons.Rounded.PhotoLibrary,
            body = "照片和视频是并列入口：照片用卡片滑动整理，视频用短视频流上下切换。先看一遍规则，再授权相册。",
            bullets = listOf("照片：左右保留，上下收藏或待删", "视频：上下切换，左右快进快退", "所有待删内容先进入确认区，避免误删"),
        ),
        OnboardingPage(
            title = "照片整理手势",
            subtitle = "轻滑、确认、再处理",
            icon = Icons.Rounded.Swipe,
            body = "点击照片可以放大查看；滑动到边缘会出现明显的保留、收藏、待删提示。",
            bullets = listOf("左右滑：保留", "上滑 / 下滑：按设置执行收藏或待删", "可在设置里排除不想整理的文件夹"),
        ),
        OnboardingPage(
            title = "视频整理方式",
            subtitle = "短视频式浏览，不强制一种比例",
            icon = Icons.Rounded.PlayCircle,
            body = "视频有独立主页，可先切换排序、文件夹、显示比例，再进入沉浸式整理流。",
            bullets = listOf("上下滑切换视频", "长按 2 倍速，松开恢复", "显示比例可选沉浸裁切 / 完整比例 / 铺满屏宽"),
        ),
        OnboardingPage(
            title = "隐私与安全",
            subtitle = "本地整理，谨慎删除",
            icon = Icons.Rounded.Security,
            body = "看了么以本地 MediaStore 和 Room 为主，默认在本机完成整理与统计。",
            bullets = listOf("整理性格基于本机真实整理记录生成", "排除文件夹会同时影响照片和视频", "删除默认进入待删确认，不直接物理删除"),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(onboardingBackgroundBrush())
            .padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            tonalAlpha = 0.82f,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pages.indices.forEach { index ->
                        Surface(
                            modifier = Modifier.size(width = if (index == page) 28.dp else 9.dp, height = 9.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = if (index == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        ) {}
                    }
                }

                AnimatedContent(
                    targetState = pages[page],
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "onboarding_page",
                ) { item ->
                    OnboardingPageCard(item)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (page > 0) {
                        OutlinedButton(
                            onClick = { page -= 1 },
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(999.dp),
                        ) { Text("上一步") }
                    }
                    Button(
                        onClick = {
                            if (page < pages.lastIndex) page += 1 else onFinish()
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(999.dp),
                    ) { Text(if (page < pages.lastIndex) "下一步" else "开始使用") }
                }

                Text(
                    text = "首次说明之后，进入照片/视频整理页会出现对着真实页面位置的指示；也可在设置中重新播放定位式教程。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageCard(page: OnboardingPage) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, onboardingBorderColor()),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(page.icon, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(page.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(page.subtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
        Text(page.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            page.bullets.forEach { bullet ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {}
                    Text(bullet, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val body: String,
    val bullets: List<String>,
)


@Composable
private fun onboardingBackgroundBrush(): Brush {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    return if (oledDark) {
        Brush.verticalGradient(listOf(Color.Black, Color.Black))
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
            )
        )
    }
}

@Composable
private fun onboardingBorderColor(): Color {
    val oledDark = MaterialTheme.colorScheme.background.luminance() < 0.03f
    return if (oledDark) MaterialTheme.colorScheme.outline.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.48f)
}
