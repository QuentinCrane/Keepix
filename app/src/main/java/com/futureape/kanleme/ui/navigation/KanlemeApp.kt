package com.futureape.kanleme.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NamedNavArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.FloatingGlassNav
import com.futureape.kanleme.ui.components.FloatingGlassRail
import com.futureape.kanleme.ui.components.LiquidBackground
import com.futureape.kanleme.ui.components.LocalMorphRegistry
import com.futureape.kanleme.ui.components.LocalMorphOwner
import com.futureape.kanleme.ui.components.LocalMorphAnimationsEnabled
import com.futureape.kanleme.data.settings.PhotoCleanMode
import com.futureape.kanleme.ui.components.MorphRegistry
import com.futureape.kanleme.ui.components.MorphDestination
import com.futureape.kanleme.ui.screens.CleanHomeScreen
import com.futureape.kanleme.ui.screens.FavoritesScreen
import com.futureape.kanleme.ui.screens.MeScreen
import com.futureape.kanleme.ui.screens.AdvancedFeaturesScreen
import com.futureape.kanleme.ui.screens.AdvancedVideoScreen
import com.futureape.kanleme.ui.screens.AdvancedLicensesScreen
import com.futureape.kanleme.ui.screens.MediaPermissionGate
import com.futureape.kanleme.ui.screens.OnboardingScreen
import com.futureape.kanleme.ui.screens.PhotoHistoryScreen
import com.futureape.kanleme.ui.screens.PhotoCleanScreen
import com.futureape.kanleme.ui.screens.PhotoViewerScreen
import com.futureape.kanleme.ui.screens.SettingsScreen
import com.futureape.kanleme.ui.screens.SimilarPhotosScreen
import com.futureape.kanleme.ui.screens.TimelineScreen
import com.futureape.kanleme.ui.screens.TrashScreen
import com.futureape.kanleme.ui.screens.TodayInHistoryScreen
import com.futureape.kanleme.ui.screens.AnnualReportScreen
import com.futureape.kanleme.ui.screens.AchievementsScreen
import com.futureape.kanleme.ui.screens.VideoCleanScreen
import com.futureape.kanleme.ui.i18n.asString
import com.futureape.kanleme.ui.i18n.uiText
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlinx.coroutines.delay

@Composable
fun KanlemeApp(initialShortcutTarget: String?, shortcutNonce: Long = 0L, viewModel: KanlemeViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val morphRegistry = remember { MorphRegistry() }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val pendingMediaAuthorization by viewModel.pendingMediaAuthorization.collectAsStateWithLifecycle()
    val mediaAuthorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onMediaAuthorizationResult(result.resultCode == Activity.RESULT_OK)
    }
    val messageText = message?.asString()
    var visibleMessage by remember { mutableStateOf<String?>(null) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val todayInHistoryPhotos by viewModel.todayInHistoryPhotos.collectAsStateWithLifecycle()
    val todayInHistoryVideos by viewModel.todayInHistoryVideos.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    backStack?.let { morphRegistry.navigateTo(it.id) }
    val current = backStack?.destination?.route ?: Destinations.HOME
    val windowSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowSize.width.toDp() }
    val screenHeightDp = with(density) { windowSize.height.toDp() }
    // 平板竖屏仍使用底部 Dock；只有横屏或超宽屏才切换到左侧 Rail。
    val useSideRail = screenWidthDp >= 840.dp && screenWidthDp > screenHeightDp && current in setOf(Destinations.HOME, Destinations.ME)
    val homeBottomPadding = if (useSideRail) 30.dp else 108.dp
    val haptic = rememberHapticKit(settings)
    val openTodayInHistory = {
        if (todayInHistoryPhotos.isNotEmpty() || todayInHistoryVideos.isNotEmpty()) {
            navController.navigate(Destinations.TODAY) { launchSingleTop = true }
        } else {
            viewModel.showMessage(uiText(R.string.message_today_history_empty))
        }
    }
    val transitionBackdrop = Color.Black

    LaunchedEffect(pendingMediaAuthorization?.request?.operationId) {
        val pending = pendingMediaAuthorization ?: return@LaunchedEffect
        runCatching {
            mediaAuthorizationLauncher.launch(
                IntentSenderRequest.Builder(pending.request.intentSender).build(),
            )
        }.onFailure {
            viewModel.onMediaAuthorizationLaunchFailed()
        }
    }

    LaunchedEffect(initialShortcutTarget, shortcutNonce) {
        when (NavigationContract.routeForShortcut(initialShortcutTarget)) {
            Destinations.PHOTO -> {
                viewModel.startPhotoCleaningSession()
                navController.navigate(Destinations.PHOTO) { launchSingleTop = true }
            }
            Destinations.VIDEO -> {
                viewModel.startVideoCleaningSession()
                navController.navigate(Destinations.VIDEO) { launchSingleTop = true }
            }
        }
    }

    LaunchedEffect(messageText) {
        val text = messageText ?: return@LaunchedEffect
        visibleMessage = text
        delay(2100)
        if (visibleMessage == text) visibleMessage = null
        viewModel.clearMessage()
    }

    LiquidBackground(
        modifier = Modifier.fillMaxSize().background(transitionBackdrop),
        backgroundColor = transitionBackdrop,
    ) {
        if (!settings.onboardingShown) {
            OnboardingScreen(onFinish = { viewModel.markOnboardingShown() })
        } else {
            MediaPermissionGate(
                onPermissionReady = { access, accessLabel ->
                    viewModel.onMediaAccessReady(access, accessLabel)
                },
            ) {
            CompositionLocalProvider(LocalMorphRegistry provides morphRegistry, LocalMorphOwner provides (backStack?.id ?: "root"),
                LocalMorphAnimationsEnabled provides (settings.photoCleanMode != PhotoCleanMode.PERFORMANCE)) {
            Box(Modifier.fillMaxSize().background(transitionBackdrop)) {
            NavHost(
                navController = navController,
                startDestination = Destinations.HOME,
                modifier = Modifier.fillMaxSize()
                    .background(transitionBackdrop),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                morphComposable(Destinations.HOME) {
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
                        onToday = openTodayInHistory,
                        onSettings = { navController.navigate(Destinations.SETTINGS) },
                    )
                }
                morphComposable(Destinations.PHOTO) {
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
                morphComposable(Destinations.VIDEO) {
                    VideoCleanScreen(
                        viewModel,
                        bottomContentPadding = if (useSideRail) 28.dp else 112.dp,
                        onBack = {
                            viewModel.setHomeMediaTab("photo")
                            navController.popBackStack(Destinations.HOME, false)
                        },
                    )
                }
                morphComposable(Destinations.TIMELINE) {
                    TimelineScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenPhoto = { photo -> navController.navigate(Destinations.photoViewer(photo.id)) },
                    )
                }
                morphComposable(
                    route = Destinations.VIEWER,
                    arguments = listOf(navArgument(Destinations.VIEWER_ARG_PHOTO_ID) { type = NavType.LongType }),
                ) { entry ->
                    PhotoViewerScreen(
                        viewModel = viewModel,
                        initialPhotoId = entry.arguments?.getLong(Destinations.VIEWER_ARG_PHOTO_ID) ?: 0L,
                        onBack = { navController.popBackStack() },
                    )
                }
                morphComposable(Destinations.SIMILAR) { SimilarPhotosScreen(viewModel, onBack = { navController.popBackStack() }, onOpenPhoto = { id -> navController.navigate(Destinations.photoViewer(id)) }) }
                morphComposable(Destinations.ME) {
                    MeScreen(
                        viewModel = viewModel,
                        contentPadding = PaddingValues(bottom = homeBottomPadding),
                        onFavorites = { navController.navigate(Destinations.FAVORITES) },
                        onTrash = { navController.navigate(Destinations.TRASH) },
                        onHistory = { navController.navigate(Destinations.PHOTO_HISTORY) },
                        onAdvanced = { navController.navigate(Destinations.ADVANCED) },
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
                morphComposable(Destinations.FAVORITES) { FavoritesScreen(viewModel, onBack = { navController.popBackStack() }, onToday = openTodayInHistory) }
                morphComposable(Destinations.ADVANCED) {
                    AdvancedFeaturesScreen(
                        onBack = { navController.popBackStack() },
                        onVideos = { navController.navigate(Destinations.ADVANCED_VIDEO) },
                        onLicenses = { navController.navigate(Destinations.OPEN_SOURCE) },
                    )
                }
                morphComposable(Destinations.ADVANCED_VIDEO) {
                    AdvancedVideoScreen(viewModel, onBack = { navController.popBackStack() })
                }
                morphComposable(Destinations.OPEN_SOURCE) {
                    AdvancedLicensesScreen(onBack = { navController.popBackStack() })
                }
                morphComposable(Destinations.TRASH) { TrashScreen(viewModel, onBack = { navController.popBackStack() }, onToday = openTodayInHistory) }
                morphComposable(Destinations.PHOTO_HISTORY) {
                    PhotoHistoryScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenPhoto = { photo -> navController.navigate(Destinations.photoViewer(photo.id)) },
                    )
                }
                morphComposable(Destinations.SETTINGS) {
                    Box(Modifier.fillMaxSize().background(transitionBackdrop)) {
                        SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
                    }
                }
                morphComposable(
                    route = Destinations.SETTINGS_WITH_PAGE,
                    arguments = listOf(navArgument(Destinations.SETTINGS_PAGE_ARG) { type = NavType.StringType }),
                ) { entry ->
                    Box(Modifier.fillMaxSize().background(transitionBackdrop)) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            initialPage = entry.arguments?.getString(Destinations.SETTINGS_PAGE_ARG),
                        )
                    }
                }
                morphComposable(Destinations.TODAY) {
                    TodayInHistoryScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenPhoto = { photo -> navController.navigate(Destinations.photoViewer(photo.id)) },
                    )
                }
                morphComposable(Destinations.ACHIEVEMENTS) { AchievementsScreen(viewModel, onBack = { navController.popBackStack() }) }
                morphComposable(Destinations.ANNUAL) { AnnualReportScreen(viewModel, onBack = { navController.popBackStack() }) }
            }

            if (current in setOf(Destinations.HOME, Destinations.VIDEO, Destinations.ME) && morphRegistry.modalDepth == 0) {
                val navItems = listOf(
                    stringResource(R.string.nav_photos) to Icons.Rounded.PhotoLibrary,
                    stringResource(R.string.nav_videos) to Icons.Rounded.Movie,
                    stringResource(R.string.nav_me) to Icons.Rounded.Person,
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
                            if (index == 0 && current == Destinations.VIDEO) {
                                viewModel.setHomeMediaTab("photo")
                                if (!navController.popBackStack(Destinations.HOME, false)) {
                                    navController.navigate(Destinations.HOME) { launchSingleTop = true }
                                }
                            } else {
                                navController.navigate(target) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
                            .zIndex(100f)
                            .padding(start = 10.dp, top = 92.dp, bottom = 92.dp),
                    )
                } else {
                    FloatingGlassNav(
                        selectedIndex = selectedNavIndex,
                        items = navItems,
                        onSelected = onNavSelected,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(100f)
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
                    .statusBarsPadding()
                    .padding(top = 12.dp),
            )
        }
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

private fun NavGraphBuilder.morphComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(route = route, arguments = arguments) { entry ->
        val animatedScope = this
        val background = if (route in setOf(Destinations.PHOTO, Destinations.VIDEO, Destinations.VIEWER,
                Destinations.ADVANCED, Destinations.ADVANCED_VIDEO, Destinations.OPEN_SOURCE))
            Color.Black else MaterialTheme.colorScheme.background
        val window = LocalWindowInfo.current.containerSize
        val density = LocalDensity.current
        val useRail = with(density) { window.width.toDp() >= 840.dp } && window.width > window.height &&
            route in setOf(Destinations.HOME, Destinations.ME)
        // Keep the morph's coordinate space fixed when a destination changes navigation chrome.
        MorphDestination(entry.id, animatedScope, background, imageDestination = route == Destinations.VIEWER) {
            Box(Modifier.fillMaxSize().padding(start = if (useRail) 92.dp else 0.dp)) { content(animatedScope, entry) }
        }
    }
}
