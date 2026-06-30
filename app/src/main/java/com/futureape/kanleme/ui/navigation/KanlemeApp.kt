package com.futureape.kanleme.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.futureape.kanleme.R
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
import com.futureape.kanleme.ui.i18n.asString
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlinx.coroutines.delay

@Composable
fun KanlemeApp(initialShortcutTarget: String?, shortcutNonce: Long = 0L, viewModel: KanlemeViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val messageText = message?.asString()
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

    LaunchedEffect(initialShortcutTarget, shortcutNonce) {
        when (initialShortcutTarget) {
            "photo" -> {
                viewModel.startPhotoCleaningSession()
                navController.navigate(Destinations.PHOTO) { launchSingleTop = true }
            }
            "video" -> {
                viewModel.startVideoCleaningSession()
                navController.navigate(Destinations.VIDEO) { launchSingleTop = true }
            }
        }
    }

    // Deck loading is triggered after MediaStore permission is confirmed and the library is synced.
    // Keeping this pre-load makes returning users see cached Room data immediately while the new scan runs.
    LaunchedEffect(Unit) {
        viewModel.loadPhotoDeck()
        viewModel.loadVideoDeck()
    }

    LaunchedEffect(messageText) {
        val text = messageText ?: return@LaunchedEffect
        visibleMessage = text
        delay(2100)
        if (visibleMessage == text) visibleMessage = null
        viewModel.clearMessage()
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
                enterTransition = {
                    fadeIn(tween(140)) + slideIntoContainer(topLevelSlideDirection(initialState.destination.route, targetState.destination.route), tween(280))
                },
                exitTransition = {
                    fadeOut(tween(120)) + slideOutOfContainer(topLevelSlideDirection(initialState.destination.route, targetState.destination.route), tween(280))
                },
                popEnterTransition = {
                    fadeIn(tween(140)) + slideIntoContainer(topLevelSlideDirection(initialState.destination.route, targetState.destination.route), tween(280))
                },
                popExitTransition = {
                    fadeOut(tween(120)) + slideOutOfContainer(topLevelSlideDirection(initialState.destination.route, targetState.destination.route), tween(280))
                },
            ) {
                composable(
                    Destinations.HOME,
                    popEnterTransition = {
                        if (initialState.destination.route == Destinations.PHOTO) {
                            fadeIn(tween(180, easing = LinearOutSlowInEasing)) +
                                scaleIn(tween(240, easing = FastOutSlowInEasing), initialScale = 0.985f)
                        } else {
                            fadeIn(tween(140)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(280))
                        }
                    },
                ) {
                    CleanHomeScreen(
                        viewModel = viewModel,
                        contentPadding = PaddingValues(bottom = homeBottomPadding),
                        onPhoto = {
                            viewModel.startPhotoCleaningSession()
                            navController.navigate(Destinations.PHOTO)
                        },
                        onVideo = {
                            viewModel.startVideoCleaningSession()
                            navController.navigate(Destinations.VIDEO)
                        },
                        onTimeline = { navController.navigate(Destinations.TIMELINE) },
                        onTrash = { navController.navigate(Destinations.TRASH) },
                        onFavorites = { navController.navigate(Destinations.FAVORITES) },
                        onToday = { navController.navigate(Destinations.TODAY) },
                        onSettings = { navController.navigate(Destinations.SETTINGS) },
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
                composable(
                    Destinations.PHOTO,
                    enterTransition = {
                        fadeIn(tween(120, easing = LinearOutSlowInEasing)) +
                            scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.985f)
                    },
                    exitTransition = {
                        fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                            scaleOut(tween(180, easing = FastOutSlowInEasing), targetScale = 1.01f)
                    },
                    popEnterTransition = { fadeIn(tween(120)) },
                    popExitTransition = {
                        fadeOut(tween(170, easing = FastOutSlowInEasing)) +
                            scaleOut(tween(240, easing = FastOutSlowInEasing), targetScale = 0.94f)
                    },
                ) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        PhotoCleanScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenPhoto = { photo -> navController.navigate(Destinations.photoViewer(photo.id)) },
                            onBatchFinished = {
                                navController.navigate(Destinations.TRASH) {
                                    popUpTo(Destinations.HOME) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
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
                composable(
                    Destinations.VIDEO,
                    enterTransition = {
                        fadeIn(tween(140, easing = LinearOutSlowInEasing)) +
                            slideIntoContainer(topLevelSlideDirection(initialState.destination.route, targetState.destination.route), tween(280, easing = FastOutSlowInEasing))
                    },
                    exitTransition = {
                        fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                            slideOutOfContainer(topLevelSlideDirection(initialState.destination.route, targetState.destination.route), tween(280, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        fadeIn(tween(140, easing = LinearOutSlowInEasing)) +
                            slideIntoContainer(topLevelSlideDirection(initialState.destination.route, targetState.destination.route), tween(280, easing = FastOutSlowInEasing))
                    },
                    popExitTransition = {
                        fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                            slideOutOfContainer(topLevelSlideDirection(initialState.destination.route, targetState.destination.route), tween(280, easing = FastOutSlowInEasing))
                    },
                ) {
                    VideoCleanScreen(
                        viewModel,
                        bottomContentPadding = if (useSideRail) 28.dp else 112.dp,
                        onBack = {
                            viewModel.setHomeMediaTab("photo")
                            navController.popBackStack(Destinations.HOME, false)
                        },
                    )
                }
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
                    enterTransition = { fadeIn(tween(160, easing = LinearOutSlowInEasing)) },
                    exitTransition = { fadeOut(tween(120, easing = FastOutSlowInEasing)) },
                    popEnterTransition = { fadeIn(tween(120, easing = LinearOutSlowInEasing)) },
                    popExitTransition = { fadeOut(tween(150, easing = FastOutSlowInEasing)) },
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
                composable(Destinations.SETTINGS) {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                }
                composable(
                    route = Destinations.SETTINGS_WITH_PAGE,
                    arguments = listOf(navArgument(Destinations.SETTINGS_PAGE_ARG) { type = NavType.StringType }),
                ) { entry ->
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            initialPage = entry.arguments?.getString(Destinations.SETTINGS_PAGE_ARG),
                        )
                    }
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

            if (current in setOf(Destinations.HOME, Destinations.VIDEO, Destinations.ME)) {
                val navItems = listOf(
                    "照片" to Icons.Rounded.PhotoLibrary,
                    "视频" to Icons.Rounded.Movie,
                    "我的" to Icons.Rounded.Person,
                )
                val selectedNavIndex = when (current) {
                    Destinations.VIDEO -> 1
                    Destinations.ME -> 2
                    else -> 0
                }
                val onNavSelected: (Int) -> Unit = { index ->
                    when (index) {
                        1 -> {
                            if (current != Destinations.VIDEO) {
                                haptic.tick()
                                viewModel.startVideoCleaningSession()
                                navController.navigate(Destinations.VIDEO) {
                                    launchSingleTop = true
                                }
                            }
                        }
                        else -> {
                            val target = if (index == 2) Destinations.ME else Destinations.HOME
                            if (index == 0 && settings.homeMediaTab != "photo") {
                                haptic.tick()
                                viewModel.setHomeMediaTab("photo")
                            } else if (target != current) {
                                haptic.tick()
                            }
                            navController.navigate(target) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
                if (useSideRail) {
                    FloatingGlassRail(
                        selectedIndex = selectedNavIndex,
                        items = navItems,
                        onSelected = onNavSelected,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 10.dp, top = 92.dp, bottom = 92.dp),
                    )
                } else {
                    FloatingGlassNav(
                        selectedIndex = selectedNavIndex,
                        items = navItems,
                        onSelected = onNavSelected,
                        onAdd = {},
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 18.dp),
                    )
                }
            }
            NonBlockingStatusChip(
                message = visibleMessage,
                onDismiss = {
                    visibleMessage = null
                    viewModel.clearMessage()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 42.dp),
            )
        }
            }
        }
    }
}

@Composable
private fun NonBlockingStatusChip(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shownMessage by remember { mutableStateOf("") }
    LaunchedEffect(message) {
        if (message != null) shownMessage = message
    }
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(tween(180)) { -it } + fadeIn(tween(140)),
        exit = slideOutVertically(tween(160)) { -it } + fadeOut(tween(120)),
        modifier = modifier,
    ) {
        if (shownMessage.isNotBlank()) {
            Surface(
                modifier = Modifier.clickable(onClick = onDismiss),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 8.dp,
            ) {
                Text(
                    text = shownMessage,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun topLevelSlideDirection(
    fromRoute: String?,
    toRoute: String?,
): AnimatedContentTransitionScope.SlideDirection {
    val from = topLevelRouteIndex(fromRoute)
    val to = topLevelRouteIndex(toRoute)
    return if (from != null && to != null && to < from) {
        AnimatedContentTransitionScope.SlideDirection.Right
    } else {
        AnimatedContentTransitionScope.SlideDirection.Left
    }
}

private fun topLevelRouteIndex(route: String?): Int? = when (route) {
    Destinations.HOME -> 0
    Destinations.VIDEO -> 1
    Destinations.ME -> 2
    else -> null
}


