package com.futureape.kanleme.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.futureape.kanleme.ui.components.FloatingGlassNav
import com.futureape.kanleme.ui.components.FloatingGlassRail
import com.futureape.kanleme.ui.components.LiquidBackground
import com.futureape.kanleme.ui.screens.CleanHomeScreen
import com.futureape.kanleme.ui.screens.FavoritesScreen
import com.futureape.kanleme.ui.screens.MeScreen
import com.futureape.kanleme.ui.screens.MediaPermissionGate
import com.futureape.kanleme.ui.screens.OnboardingScreen
import com.futureape.kanleme.ui.screens.PhotoCleanScreen
import com.futureape.kanleme.ui.screens.PhotoStartScreen
import com.futureape.kanleme.ui.screens.PhotoViewerScreen
import com.futureape.kanleme.ui.screens.SettingsScreen
import com.futureape.kanleme.ui.screens.SimilarPhotosScreen
import com.futureape.kanleme.ui.screens.TimelineScreen
import com.futureape.kanleme.ui.screens.TrashScreen
import com.futureape.kanleme.ui.screens.TodayInHistoryScreen
import com.futureape.kanleme.ui.screens.AnnualReportScreen
import com.futureape.kanleme.ui.screens.AchievementsScreen
import com.futureape.kanleme.ui.screens.VideoCleanScreen
import com.futureape.kanleme.ui.screens.VideoStartScreen
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlinx.coroutines.delay

@Composable
fun KanlemeApp(initialShortcutTarget: String?, viewModel: KanlemeViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var visibleMessage by remember { mutableStateOf<String?>(null) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Destinations.HOME
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val screenHeightDp = configuration.screenHeightDp
    // 平板竖屏仍使用底部 Dock；只有横屏或超宽屏才切换到左侧 Rail。
    val useSideRail = screenWidthDp >= 840 && screenWidthDp > screenHeightDp && current in setOf(Destinations.HOME, Destinations.ME)
    val homeBottomPadding = if (useSideRail) 30.dp else 108.dp
    val haptic = rememberHapticKit(settings)

    LaunchedEffect(initialShortcutTarget) {
        when (initialShortcutTarget) {
            "photo" -> navController.navigate(Destinations.PHOTO_START)
            "video" -> navController.navigate(Destinations.VIDEO_START)
        }
    }

    // Deck loading is triggered after MediaStore permission is confirmed and the library is synced.
    // Keeping this pre-load makes returning users see cached Room data immediately while the new scan runs.
    LaunchedEffect(Unit) {
        viewModel.loadPhotoDeck()
        viewModel.loadVideoDeck()
    }

    LaunchedEffect(message) {
        val currentMessage = message ?: return@LaunchedEffect
        visibleMessage = currentMessage
        viewModel.clearMessage()
        delay(2100)
        if (visibleMessage == currentMessage) visibleMessage = null
    }

    LiquidBackground(modifier = Modifier.fillMaxSize()) {
        if (!settings.onboardingShown) {
            OnboardingScreen(onFinish = { viewModel.markOnboardingShown() })
        } else {
            MediaPermissionGate(
                onPermissionReady = { accessKey, accessLabel ->
                    viewModel.onMediaAccessReady(accessKey, accessLabel)
                },
            ) {
            Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Destinations.HOME,
                modifier = if (useSideRail) Modifier.fillMaxSize().padding(start = 92.dp) else Modifier.fillMaxSize(),
                enterTransition = { fadeIn(tween(140)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(280)) },
                exitTransition = { fadeOut(tween(120)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(280)) },
                popEnterTransition = { fadeIn(tween(140)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(280)) },
                popExitTransition = { fadeOut(tween(120)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(280)) },
            ) {
                composable(Destinations.HOME) {
                    CleanHomeScreen(
                        viewModel = viewModel,
                        contentPadding = PaddingValues(bottom = homeBottomPadding),
                        onPhoto = { navController.navigate(Destinations.PHOTO_START) },
                        onVideo = { navController.navigate(Destinations.VIDEO_START) },
                        onTimeline = { navController.navigate(Destinations.TIMELINE) },
                        onTrash = { navController.navigate(Destinations.TRASH) },
                        onFavorites = { navController.navigate(Destinations.FAVORITES) },
                        onToday = { navController.navigate(Destinations.TODAY) },
                        onReport = { navController.navigate(Destinations.ANNUAL) },
                    )
                }
                composable(Destinations.PHOTO_START) {
                    PhotoStartScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onStart = {
                            viewModel.startPhotoCleaningSession()
                            navController.navigate(Destinations.PHOTO)
                        },
                    )
                }
                composable(Destinations.PHOTO) {
                    PhotoCleanScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenPhoto = { photo -> navController.navigate(Destinations.photoViewer(photo.id)) },
                    )
                }
                composable(Destinations.VIDEO_START) {
                    VideoStartScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onStart = {
                            viewModel.startVideoCleaningSession()
                            navController.navigate(Destinations.VIDEO)
                        },
                    )
                }
                composable(Destinations.VIDEO) { VideoCleanScreen(viewModel, onBack = { navController.popBackStack() }) }
                composable(Destinations.TIMELINE) {
                    TimelineScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenPhoto = { photo -> navController.navigate(Destinations.photoViewer(photo.id)) },
                    )
                }
                composable(
                    route = Destinations.VIEWER,
                    arguments = listOf(navArgument(Destinations.VIEWER_ARG_PHOTO_ID) { type = NavType.LongType }),
                ) { entry ->
                    PhotoViewerScreen(
                        viewModel = viewModel,
                        initialPhotoId = entry.arguments?.getLong(Destinations.VIEWER_ARG_PHOTO_ID) ?: 0L,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Destinations.SIMILAR) { SimilarPhotosScreen(viewModel, onBack = { navController.popBackStack() }, onOpenPhoto = { id -> navController.navigate(Destinations.photoViewer(id)) }) }
                composable(Destinations.ME) {
                    MeScreen(
                        viewModel = viewModel,
                        contentPadding = PaddingValues(bottom = homeBottomPadding),
                        onFavorites = { navController.navigate(Destinations.FAVORITES) },
                        onTrash = { navController.navigate(Destinations.TRASH) },
                        onSettings = { navController.navigate(Destinations.SETTINGS) },
                        onSimilar = { navController.navigate(Destinations.SIMILAR) },
                        onAchievements = { navController.navigate(Destinations.ACHIEVEMENTS) },
                        onAnnualReport = { navController.navigate(Destinations.ANNUAL) },
                        onHelp = { navController.navigate(Destinations.settingsPage("HELP")) },
                        onPrivacy = { navController.navigate(Destinations.settingsPage("PRIVACY")) },
                        onChangelog = { navController.navigate(Destinations.settingsPage("CHANGELOG")) },
                        onDiagnosis = { navController.navigate(Destinations.settingsPage("DIAGNOSIS")) },
                    )
                }
                composable(Destinations.FAVORITES) { FavoritesScreen(viewModel, onBack = { navController.popBackStack() }) }
                composable(Destinations.TRASH) { TrashScreen(viewModel, onBack = { navController.popBackStack() }) }
                composable(Destinations.SETTINGS) { SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() }) }
                composable(
                    route = Destinations.SETTINGS_WITH_PAGE,
                    arguments = listOf(navArgument(Destinations.SETTINGS_PAGE_ARG) { type = NavType.StringType }),
                ) { entry ->
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        initialPage = entry.arguments?.getString(Destinations.SETTINGS_PAGE_ARG),
                    )
                }
                composable(Destinations.TODAY) {
                    TodayInHistoryScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenPhoto = { photo -> navController.navigate(Destinations.photoViewer(photo.id)) },
                    )
                }
                composable(Destinations.ACHIEVEMENTS) { AchievementsScreen(viewModel, onBack = { navController.popBackStack() }) }
                composable(Destinations.ANNUAL) { AnnualReportScreen(viewModel, onBack = { navController.popBackStack() }) }
            }

            NonBlockingStatusChip(
                message = visibleMessage,
                onDismiss = { visibleMessage = null },
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 42.dp),
            )

            if (current in setOf(Destinations.HOME, Destinations.ME)) {
                val navItems = listOf("整理" to Icons.Rounded.PhotoLibrary, "我的" to Icons.Rounded.Person)
                val onNavSelected: (Int) -> Unit = { index ->
                    val target = if (index == 0) Destinations.HOME else Destinations.ME
                    if (target != current) haptic.tick()
                    navController.navigate(target) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                if (useSideRail) {
                    FloatingGlassRail(
                        selectedIndex = if (current == Destinations.ME) 1 else 0,
                        items = navItems,
                        onSelected = onNavSelected,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 10.dp, top = 92.dp, bottom = 92.dp),
                    )
                } else {
                    FloatingGlassNav(
                        selectedIndex = if (current == Destinations.ME) 1 else 0,
                        items = navItems,
                        onSelected = onNavSelected,
                        onAdd = {
                            haptic.tick()
                            navController.navigate(Destinations.PHOTO_START)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 18.dp),
                    )
                }
            }
        }
            }
        }
    }
}


@Composable
private fun NonBlockingStatusChip(message: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.animation.AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(120)) + slideInVertically(tween(180)) { -it / 2 },
        exit = fadeOut(tween(160)) + slideOutVertically(tween(180)) { -it / 2 },
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.clickable(onClick = onDismiss),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.82f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Text(
                text = (message.orEmpty() + "  ·  轻点关闭"),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}
