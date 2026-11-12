package com.futureape.kanleme.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.futureape.kanleme.R
import kotlinx.coroutines.launch

internal val OnboardingText = Color(0xFFF4F7FA)
internal val OnboardingMuted = Color(0xFFB6C1CF)
internal val OnboardingAccentBlue = Color(0xFF8FA8FF)

private data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val icon: ImageVector,
    val accent: Color,
    @StringRes val body: Int,
    @StringRes val bullets: List<Int>,
)

private val OnboardingPages = listOf(
    OnboardingPage(
        title = R.string.onboarding_home_title,
        subtitle = R.string.onboarding_home_subtitle,
        icon = Icons.Rounded.PhotoLibrary,
        accent = OnboardingAccentBlue,
        body = R.string.onboarding_home_body,
        bullets = listOf(R.string.onboarding_home_bullet_1, R.string.onboarding_home_bullet_2, R.string.onboarding_home_bullet_3),
    ),
    OnboardingPage(
        title = R.string.onboarding_photo_title,
        subtitle = R.string.onboarding_photo_subtitle,
        icon = Icons.Rounded.Swipe,
        accent = Color(0xFFB79CFF),
        body = R.string.onboarding_photo_body,
        bullets = listOf(R.string.onboarding_photo_bullet_1, R.string.onboarding_photo_bullet_2, R.string.onboarding_photo_bullet_3, R.string.onboarding_photo_bullet_4),
    ),
    OnboardingPage(
        title = R.string.onboarding_video_title,
        subtitle = R.string.onboarding_video_subtitle,
        icon = Icons.Rounded.PlayCircle,
        accent = Color(0xFF7FD8C4),
        body = R.string.onboarding_video_body,
        bullets = listOf(R.string.onboarding_video_bullet_1, R.string.onboarding_video_bullet_2, R.string.onboarding_video_bullet_3),
    ),
    OnboardingPage(
        title = R.string.onboarding_privacy_title,
        subtitle = R.string.onboarding_privacy_subtitle,
        icon = Icons.Rounded.Security,
        accent = Color(0xFF93D9A3),
        body = R.string.onboarding_privacy_body,
        bullets = listOf(R.string.onboarding_privacy_bullet_1, R.string.onboarding_privacy_bullet_2, R.string.onboarding_privacy_bullet_3),
    ),
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { OnboardingPages.size })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    val accent by animateColorAsState(
        targetValue = OnboardingPages[page].accent,
        animationSpec = tween(360),
        label = "onboarding_accent",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(onboardingBackgroundBrush())
            .systemBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        val compact = maxHeight < 720.dp

        OnboardingGlow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-80).dp, y = (-60).dp)
                .size(300.dp),
            color = accent,
            alpha = 0.30f,
        )
        OnboardingGlow(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 80.dp)
                .size(340.dp),
            color = accent,
            alpha = 0.20f,
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp),
            shape = RoundedCornerShape(36.dp),
            color = Color.White.copy(alpha = 0.08f),
            contentColor = OnboardingText,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (compact) 18.dp else 28.dp, vertical = if (compact) 14.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.onboarding_page_counter, page + 1, OnboardingPages.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnboardingMuted,
                        modifier = Modifier.weight(1f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OnboardingPages.indices.forEach { index ->
                            OnboardingDot(
                                selected = index == page,
                                accent = accent,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        if (page < OnboardingPages.lastIndex) {
                            TextButton(onClick = onFinish) {
                                Text(
                                    text = stringResource(R.string.onboarding_skip),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = OnboardingMuted,
                                )
                            }
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                ) { index ->
                    OnboardingPageContent(OnboardingPages[index], compact)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (page > 0) {
                        OutlinedButton(
                            onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                            modifier = Modifier.weight(1f).height(if (compact) 48.dp else 54.dp),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnboardingText),
                        ) {
                            Text(stringResource(R.string.onboarding_previous))
                        }
                    }
                    Button(
                        onClick = {
                            if (page < OnboardingPages.lastIndex) {
                                scope.launch { pagerState.animateScrollToPage(page + 1) }
                            } else {
                                onFinish()
                            }
                        },
                        modifier = Modifier.weight(1f).height(if (compact) 48.dp else 54.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color(0xFF08111C),
                        ),
                    ) {
                        Text(stringResource(if (page < OnboardingPages.lastIndex) R.string.onboarding_next else R.string.onboarding_start))
                    }
                }

                Text(
                    text = stringResource(R.string.onboarding_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnboardingMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, compact: Boolean) {
    val accent = page.accent
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            OnboardingGlow(
                modifier = Modifier.size(if (compact) 110.dp else 132.dp),
                color = accent,
                alpha = 0.22f,
            )
            Surface(
                modifier = Modifier.size(if (compact) 72.dp else 88.dp),
                shape = RoundedCornerShape(if (compact) 24.dp else 28.dp),
                color = accent.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        page.icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 34.dp else 42.dp),
                        tint = accent,
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(page.title),
                style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                color = OnboardingText,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(page.subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = OnboardingMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            page.bullets.forEach { bullet ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = accent,
                    )
                    Text(
                        stringResource(bullet),
                        style = MaterialTheme.typography.titleMedium,
                        color = OnboardingText,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingDot(selected: Boolean, accent: Color, onClick: () -> Unit) {
    val width by animateDpAsState(
        targetValue = if (selected) 26.dp else 8.dp,
        animationSpec = tween(240),
        label = "onboarding_dot_width",
    )
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 10.dp)
            .size(width = width, height = 8.dp)
            .background(if (selected) accent else Color.White.copy(alpha = 0.26f), RoundedCornerShape(999.dp)),
    )
}

@Composable
internal fun OnboardingGlow(modifier: Modifier = Modifier, color: Color, alpha: Float) {
    Box(
        modifier = modifier.background(
            Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent)),
            CircleShape,
        ),
    )
}

internal fun onboardingBackgroundBrush(): Brush = Brush.verticalGradient(
    listOf(
        Color(0xFF14223A),
        Color(0xFF07111D),
        Color.Black,
    )
)
