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
import androidx.compose.ui.res.stringResource
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.R

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            title = R.string.onboarding_home_title,
            subtitle = R.string.onboarding_home_subtitle,
            icon = Icons.Rounded.PhotoLibrary,
            body = R.string.onboarding_home_body,
            bullets = listOf(R.string.onboarding_home_bullet_1, R.string.onboarding_home_bullet_2, R.string.onboarding_home_bullet_3),
        ),
        OnboardingPage(
            title = R.string.onboarding_photo_title,
            subtitle = R.string.onboarding_photo_subtitle,
            icon = Icons.Rounded.Swipe,
            body = R.string.onboarding_photo_body,
            bullets = listOf(R.string.onboarding_photo_bullet_1, R.string.onboarding_photo_bullet_2, R.string.onboarding_photo_bullet_3),
        ),
        OnboardingPage(
            title = R.string.onboarding_video_title,
            subtitle = R.string.onboarding_video_subtitle,
            icon = Icons.Rounded.PlayCircle,
            body = R.string.onboarding_video_body,
            bullets = listOf(R.string.onboarding_video_bullet_1, R.string.onboarding_video_bullet_2, R.string.onboarding_video_bullet_3),
        ),
        OnboardingPage(
            title = R.string.onboarding_privacy_title,
            subtitle = R.string.onboarding_privacy_subtitle,
            icon = Icons.Rounded.Security,
            body = R.string.onboarding_privacy_body,
            bullets = listOf(R.string.onboarding_privacy_bullet_1, R.string.onboarding_privacy_bullet_2, R.string.onboarding_privacy_bullet_3),
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
                        ) {
                            Text(stringResource(R.string.onboarding_previous))
                        }
                    }
                    Button(
                        onClick = {
                            if (page < pages.lastIndex) page += 1 else onFinish()
                        },
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(stringResource(if (page < pages.lastIndex) R.string.onboarding_next else R.string.onboarding_start))
                    }
                }

                Text(
                    text = stringResource(R.string.onboarding_footer),
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
            Text(stringResource(page.title), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(page.subtitle), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
        Text(stringResource(page.body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            page.bullets.forEach { bullet ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {}
                    Text(stringResource(bullet), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class OnboardingPage(
    @androidx.annotation.StringRes val title: Int,
    @androidx.annotation.StringRes val subtitle: Int,
    val icon: ImageVector,
    @androidx.annotation.StringRes val body: Int,
    @androidx.annotation.StringRes val bullets: List<Int>,
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
