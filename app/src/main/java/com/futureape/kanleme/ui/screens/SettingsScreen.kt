package com.futureape.kanleme.ui.screens

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.futureape.kanleme.BuildConfig
import com.futureape.kanleme.data.settings.AppSettings
import com.futureape.kanleme.data.settings.AppVisualStyle
import com.futureape.kanleme.data.settings.DeleteMode
import com.futureape.kanleme.data.settings.FolderDisplayMode
import com.futureape.kanleme.data.settings.GestureDirection
import com.futureape.kanleme.data.settings.HapticLevel
import com.futureape.kanleme.data.settings.PhotoCleanMode
import com.futureape.kanleme.data.settings.SwipeSensitivity
import com.futureape.kanleme.data.settings.ThemeMode
import com.futureape.kanleme.data.settings.VideoDisplayMode
import com.futureape.kanleme.R
import com.futureape.kanleme.ui.components.AdaptiveCenter
import com.futureape.kanleme.ui.components.GlassSurface
import com.futureape.kanleme.ui.components.NativeFolderExcludeButton
import com.futureape.kanleme.ui.util.rememberHapticKit
import com.futureape.kanleme.ui.util.shareCrashLogs
import com.futureape.kanleme.ui.viewmodel.KanlemeViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.futureape.kanleme.ui.i18n.Text

private enum class SettingsPage(val title: String) {
    HOME("设置"),
    CLEANING_DISPLAY("整理页面显示"),
    ORGANIZE("整理方式"),
    EXCLUDED_FOLDERS("排除文件夹"),
    EXPERIENCE("操作体验"),
    APPEARANCE("外观显示"),
    DATA("数据与更新"),
    CHANGELOG("更新日志"),
    PRIVACY("隐私政策"),
    HELP("使用帮助"),
    DIAGNOSIS("高级设置"),
}

private enum class SettingsSheet {
    MOVE_PERMISSION,
    PHOTO_CLEAN_MODE,
    HAPTIC,
}

@Composable
fun SettingsScreen(
    viewModel: KanlemeViewModel,
    onBack: () -> Unit,
    initialPage: String? = null,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val photoFolders by viewModel.photoFolders.collectAsStateWithLifecycle()
    val videoFolders by viewModel.videoFolders.collectAsStateWithLifecycle()
    val allFolders = (photoFolders + videoFolders).distinct().sorted()
    val haptics = rememberHapticKit(settings)
    var pageName by rememberSaveable(initialPage) {
        mutableStateOf(
            initialPage
                ?.let { runCatching { SettingsPage.valueOf(it) }.getOrNull() }
                ?.takeIf { it != SettingsPage.HOME }
                ?.name
                ?: SettingsPage.HOME.name
        )
    }
    val page = runCatching { SettingsPage.valueOf(pageName) }.getOrDefault(SettingsPage.HOME)
    var customizeTab by rememberSaveable { mutableStateOf("photo") }
    var manualExcludePath by remember { mutableStateOf("") }
    var showAllFolderRules by remember { mutableStateOf(false) }
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }
    var predictiveBackCompleting by remember { mutableStateOf(false) }
    var activeSheetName by rememberSaveable { mutableStateOf<String?>(null) }
    val activeSheet = activeSheetName?.let { runCatching { SettingsSheet.valueOf(it) }.getOrNull() }

    PredictiveBackHandler(enabled = page != SettingsPage.HOME && activeSheet == null) { backEvents ->
        try {
            backEvents.collect { event -> predictiveBackProgress = event.progress }
            predictiveBackCompleting = true
            pageName = SettingsPage.HOME.name
        } catch (_: CancellationException) {
            // Gesture cancelled; keep the current secondary page.
        } finally {
            predictiveBackProgress = 0f
        }
    }

    fun goTo(target: SettingsPage) {
        predictiveBackCompleting = false
        pageName = target.name
    }

    LaunchedEffect(page, predictiveBackCompleting) {
        if (page == SettingsPage.HOME && predictiveBackCompleting) {
            predictiveBackCompleting = false
        }
    }

    val baseSettingsColorScheme = MaterialTheme.colorScheme
    val settingsColorScheme = if (settings.appVisualStyle == AppVisualStyle.IMMERSIVE_PHOTO) {
        keepixIosSettingsColorScheme(baseSettingsColorScheme)
    } else {
        baseSettingsColorScheme
    }

    MaterialTheme(colorScheme = settingsColorScheme) {
        AdaptiveCenter(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            maxWidth = 760.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                if (page != SettingsPage.HOME && predictiveBackProgress > 0.001f && !predictiveBackCompleting) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .graphicsLayer {
                                translationX = -56f * (1f - predictiveBackProgress)
                                val scale = 0.985f + predictiveBackProgress * 0.015f
                                scaleX = scale
                                scaleY = scale
                                alpha = (0.58f + predictiveBackProgress * 0.42f).coerceIn(0f, 1f)
                            },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 34.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            SettingsHeader(
                                title = SettingsPage.HOME.title,
                                subtitle = "一级菜单",
                                onBack = {},
                            )
                        }
                        item {
                            SettingsPageContent(
                                page = SettingsPage.HOME,
                                settings = settings,
                                allFolders = allFolders,
                                customizeTab = customizeTab,
                                onSelectCustomizeTab = {},
                                manualExcludePath = manualExcludePath,
                                onManualExcludePathChange = {},
                                showAllFolderRules = showAllFolderRules,
                                onToggleShowAllFolderRules = {},
                                onManualExcludeConsumed = {},
                                goTo = {},
                                openSheet = {},
                                onTick = {},
                                onSuccess = {},
                                viewModel = viewModel,
                            )
                        }
                    }
                }
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        val returningHome = targetState == SettingsPage.HOME
                        val enteringChild = initialState == SettingsPage.HOME && targetState != SettingsPage.HOME
                        if (predictiveBackCompleting && returningHome) {
                            EnterTransition.None.togetherWith(ExitTransition.None)
                        } else {
                            val enter = fadeIn(tween(150, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { width ->
                                    when {
                                        enteringChild -> width / 4
                                        returningHome -> -width / 7
                                        targetState.ordinal > initialState.ordinal -> width / 7
                                        else -> -width / 7
                                    }
                                }
                            val exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                                slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { width ->
                                    when {
                                        returningHome -> width / 4
                                        enteringChild -> -width / 8
                                        targetState.ordinal > initialState.ordinal -> -width / 8
                                        else -> width / 8
                                    }
                                }
                            enter.togetherWith(exit)
                        }
                    },
                    label = "settings_page_transition",
                ) { animatedPage ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .graphicsLayer {
                                translationX = predictiveBackProgress * size.width * 0.82f
                                val scale = 1f - predictiveBackProgress * 0.035f
                                scaleX = scale
                                scaleY = scale
                                alpha = 1f - predictiveBackProgress * 0.18f
                            },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 34.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            SettingsHeader(
                                title = animatedPage.title,
                                subtitle = if (animatedPage == SettingsPage.HOME) "一级菜单" else "二级设置 · 修改后立即生效",
                                onBack = {
                                    haptics.tick()
                                    if (animatedPage == SettingsPage.HOME) onBack() else pageName = SettingsPage.HOME.name
                                },
                            )
                        }
                        item {
                            SettingsPageContent(
                                page = animatedPage,
                                settings = settings,
                                allFolders = allFolders,
                                customizeTab = customizeTab,
                                onSelectCustomizeTab = { customizeTab = it; haptics.tick() },
                                manualExcludePath = manualExcludePath,
                                onManualExcludePathChange = { manualExcludePath = it },
                                showAllFolderRules = showAllFolderRules,
                                onToggleShowAllFolderRules = { showAllFolderRules = !showAllFolderRules },
                                onManualExcludeConsumed = { manualExcludePath = "" },
                                goTo = { target -> haptics.tick(); goTo(target) },
                                openSheet = { sheet -> haptics.tick(); activeSheetName = sheet.name },
                                onTick = { haptics.tick() },
                                onSuccess = { haptics.success() },
                                viewModel = viewModel,
                            )
                        }
                    }
                }
            }
        }
        if (activeSheet != null) {
            SettingsBottomSheet(
                sheet = activeSheet,
                settings = settings,
                onDismiss = { activeSheetName = null },
                onTick = { haptics.tick() },
                onSuccess = { haptics.success() },
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun SettingsPageContent(
    page: SettingsPage,
    settings: AppSettings,
    allFolders: List<String>,
    customizeTab: String,
    onSelectCustomizeTab: (String) -> Unit,
    manualExcludePath: String,
    onManualExcludePathChange: (String) -> Unit,
    showAllFolderRules: Boolean,
    onToggleShowAllFolderRules: () -> Unit,
    onManualExcludeConsumed: () -> Unit,
    goTo: (SettingsPage) -> Unit,
    openSheet: (SettingsSheet) -> Unit,
    onTick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: KanlemeViewModel,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        when (page) {
            SettingsPage.HOME -> {
                SettingsGroup(listOf<@Composable () -> Unit>(
                    { SettingsRow(Icons.Rounded.Layers, "整理方式", "范围、排序、批次数量、删除模式", color = Color(0xFF86A7FF), showIcon = false, onClick = { goTo(SettingsPage.ORGANIZE) }) },
                    { SettingsRow(Icons.Rounded.Visibility, "整理页面显示", cleaningDisplaySummary(settings), color = Color(0xFF86A7FF), showIcon = false, onClick = { goTo(SettingsPage.CLEANING_DISPLAY) }) },
                    { SettingsRow(Icons.Rounded.TouchApp, "操作体验", stringResource(settings.swipeSensitivity.labelRes) + " · " + stringResource(settings.gestureDirection.labelRes), color = Color(0xFFD44C84), showIcon = false, onClick = { goTo(SettingsPage.EXPERIENCE) }) },
                    { SettingsSwitchRow(Icons.Rounded.Vibration, "震动反馈", stringResource(settings.hapticLevel.labelRes), checked = settings.hapticLevel != HapticLevel.OFF, onCheckedChange = { onTick(); viewModel.setHapticLevel(if (it) HapticLevel.MEDIUM else HapticLevel.OFF) }, color = Color(0xFF86A7FF), showIcon = false) },
                    { SettingsSwitchRow(Icons.Rounded.Visibility, "照片光效", if (settings.photoShowGestureHint) "删除和收藏时显示光效" else "已关闭", checked = settings.photoShowGestureHint, onCheckedChange = { onTick(); viewModel.setPhotoShowGestureHint(it) }, color = Color(0xFF86A7FF), showIcon = false) },
                    { SettingsSwitchRow(Icons.Rounded.Movie, "视频进度条", if (settings.videoShowProgressBar) "细条" else "隐藏", checked = settings.videoShowProgressBar, onCheckedChange = { onTick(); viewModel.setVideoShowProgressBar(it) }, color = Color(0xFFB884FF), showIcon = false) },
                    { SettingsRow(Icons.Rounded.Block, "排除文件夹", if (settings.excludedFolderPaths.isEmpty()) "单独管理" else "已排除 " + settings.excludedFolderPaths.size + " 个文件夹", color = Color(0xFFE46A62), showIcon = false, onClick = { goTo(SettingsPage.EXCLUDED_FOLDERS) }) },
                    { SettingsRow(Icons.Rounded.Palette, "界面风格", visualStyleLabel(settings.appVisualStyle), color = Color(0xFF8FA0FF), showIcon = false, onClick = { goTo(SettingsPage.APPEARANCE) }) },
                    { SettingsRow(Icons.Rounded.CleaningServices, "数据与更新", "媒体库维护、更新日志、帮助、隐私", color = Color(0xFF6D9E65), showIcon = false, onClick = { goTo(SettingsPage.DATA) }) },
                ))
                SectionTitle("服务与支持")
                SettingsGroup(listOf<@Composable () -> Unit>(
                    { SettingsRow(Icons.AutoMirrored.Rounded.Help, "常见问题", "", color = Color(0xFF8FA0FF), showIcon = false, onClick = { goTo(SettingsPage.HELP) }) },
                    { SettingsRow(Icons.Rounded.Info, "隐私说明", "", color = Color(0xFF8FA0FF), showIcon = false, onClick = { goTo(SettingsPage.PRIVACY) }) },
                    { SettingsRow(Icons.Rounded.Info, "更新日志", BuildConfig.VERSION_NAME, color = Color(0xFF8FA0FF), showIcon = false, onClick = { goTo(SettingsPage.CHANGELOG) }) },
                    { SettingsRow(Icons.Rounded.Tune, "高级设置", "诊断和清理模式", color = Color(0xFF8FA0FF), showIcon = false, onClick = { goTo(SettingsPage.DIAGNOSIS) }) },
                ))
            }

            SettingsPage.CLEANING_DISPLAY -> {
                OrganizerDisplayCustomizer(
                    settings = settings,
                    selectedTab = customizeTab,
                    onSelectTab = onSelectCustomizeTab,
                    onTogglePhotoFocusMode = { onTick(); viewModel.setPhotoFocusMode(!settings.photoFocusMode) },
                    onTogglePhotoTopBar = { onTick(); viewModel.setPhotoShowTopBar(!settings.photoShowTopBar) },
                    onTogglePhotoFilterChips = { onTick(); viewModel.setPhotoShowFilterChips(!settings.photoShowFilterChips) },
                    onTogglePhotoFolderChips = { onTick(); viewModel.setPhotoShowFolderChips(!settings.photoShowFolderChips) },
                    onTogglePhotoInfoBar = { onTick(); viewModel.setPhotoShowInfoBar(!settings.photoShowInfoBar) },
                    onTogglePhotoGestureHint = { onTick(); viewModel.setPhotoShowGestureHint(!settings.photoShowGestureHint) },
                    onTogglePhotoShuffleButton = { onTick(); viewModel.setPhotoShowShuffleButton(!settings.photoShowShuffleButton) },
                    onToggleVideoTopBar = { onTick(); viewModel.setVideoShowTopBar(!settings.videoShowTopBar) },
                    onToggleVideoActionRail = { onTick(); viewModel.setVideoShowActionRail(!settings.videoShowActionRail) },
                    onToggleVideoInfoPanel = { onTick(); viewModel.setVideoShowInfoPanel(!settings.videoShowInfoPanel) },
                    onToggleVideoProgressBar = { onTick(); viewModel.setVideoShowProgressBar(!settings.videoShowProgressBar) },
                    onToggleVideoShuffleButton = { onTick(); viewModel.setVideoShowShuffleButton(!settings.videoShowShuffleButton) },
                )
            }

            SettingsPage.ORGANIZE -> {
                SectionTitle("照片默认")
                SettingsGroup(listOf<@Composable () -> Unit>(
                    { SettingsSegmentedRow(Icons.Rounded.CalendarToday, "整理范围", organizerDateModeLabel(settings.photoDateMode), settingsDateModeOptions(), settings.photoDateMode, color = Color(0xFF86A7FF), onSelected = { onTick(); viewModel.setPhotoDateMode(it) }) },
                    { SettingsSegmentedRow(Icons.Rounded.Image, "照片类型", settingsPhotoTypeLabel(settings.photoMediaType), settingsPhotoTypeOptions(), settings.photoMediaType, color = Color(0xFF86A7FF), onSelected = { onTick(); viewModel.setPhotoTypeFilter(it) }) },
                    { SettingsSegmentedRow(Icons.Rounded.Tune, "排序", settingsSortLabel(settings.photoSortOrder), settingsSortOptions(), settings.photoSortOrder, color = Color(0xFF86A7FF), onSelected = { onTick(); viewModel.setPhotoSortOrder(it) }) },
                    { SettingsStepperRow(Icons.Rounded.Layers, "每组数量", settings.photoBatchSize.toString() + " 张", color = Color(0xFF86A7FF), onDecrease = { onTick(); viewModel.setPhotoBatchSize(settings.photoBatchSize - 10) }, onIncrease = { onTick(); viewModel.setPhotoBatchSize(settings.photoBatchSize + 10) }) },
                ))
                SectionTitle("视频默认")
                SettingsGroup(listOf<@Composable () -> Unit>(
                    { SettingsSegmentedRow(Icons.Rounded.CalendarToday, "整理范围", organizerDateModeLabel(settings.videoDateMode), settingsDateModeOptions(), settings.videoDateMode, color = Color(0xFFB8E95E), onSelected = { onTick(); viewModel.setVideoDateMode(it) }) },
                    { SettingsSegmentedRow(Icons.Rounded.Tune, "排序", settingsSortLabel(settings.videoSortOrder), settingsSortOptions(), settings.videoSortOrder, color = Color(0xFFB8E95E), onSelected = { onTick(); viewModel.setVideoSortOrder(it) }) },
                    { SettingsStepperRow(Icons.Rounded.Layers, "每组数量", settings.videoBatchSize.toString() + " 个", color = Color(0xFFB8E95E), onDecrease = { onTick(); viewModel.setVideoBatchSize(settings.videoBatchSize - 10) }, onIncrease = { onTick(); viewModel.setVideoBatchSize(settings.videoBatchSize + 10) }) },
                    { SettingsSwitchRow(Icons.Rounded.Movie, "视频进度条", if (settings.videoShowProgressBar) "细条常驻显示" else "已隐藏", checked = settings.videoShowProgressBar, onCheckedChange = { onTick(); viewModel.setVideoShowProgressBar(it) }, color = Color(0xFFB8E95E)) },
                ))
                SectionTitle("整理方式")
                SettingsGroup(listOf<@Composable () -> Unit>(
                    { SettingsSegmentedRow(Icons.Rounded.Delete, "删除模式", stringResource(settings.deleteMode.labelRes), deleteModeOptions(), settings.deleteMode.name, color = Color(0xFFDD5A56), onSelected = { onTick(); viewModel.setDeleteMode(DeleteMode.valueOf(it)) }) },
                    { SettingsSwitchRow(Icons.AutoMirrored.Rounded.DriveFileMove, "移动到文件夹时", if (settings.autoMoveOnKeepFavorite) "保留和收藏时允许指定归档移动" else "关闭后只记录整理状态，不移动原文件", checked = settings.autoMoveOnKeepFavorite, onCheckedChange = { onTick(); viewModel.setAutoMoveOnKeepFavorite(it) }, color = Color(0xFF55A6C8)) },
                    { SettingsSwitchRow(Icons.Rounded.BrokenImage, "相似照片检测", "自动检测连拍、截图和相似照片", checked = settings.similarDetection, onCheckedChange = { onTick(); viewModel.setSimilarDetection(it) }, badge = "测试") },
                ))
            }

            SettingsPage.EXCLUDED_FOLDERS -> {
                SectionTitle("排除文件夹")
                val rows = mutableListOf<@Composable () -> Unit>()
                rows.add {
                    NativeFolderExcludeButton(
                        title = "用系统选择器添加排除",
                        subtitle = "优先调用安卓原生文件夹选择器；下方输入框仅作备用",
                        onFolderSelected = { path -> onSuccess(); viewModel.addExcludedFolder(path) },
                    )
                }
                rows.add {
                    ManualExcludeFolderEditor(
                        value = manualExcludePath,
                        onValueChange = onManualExcludePathChange,
                        onAdd = {
                            val input = manualExcludePath.trim()
                            if (input.isNotBlank()) {
                                onSuccess()
                                viewModel.addExcludedFolder(input)
                                onManualExcludeConsumed()
                            }
                        },
                    )
                }
                rows.add {
                    SettingsRow(
                        Icons.Rounded.Block,
                        "排除状态",
                        if (settings.excludedFolderPaths.isEmpty()) "未排除，照片和视频都会进入整理队列" else "已排除 " + settings.excludedFolderPaths.size + " 个文件夹；点已选项可取消",
                        onClick = { onTick(); if (settings.excludedFolderPaths.isNotEmpty()) viewModel.clearExcludedFolders() },
                        color = Color(0xFFB64040),
                    )
                }
                settings.excludedFolderPaths.sorted().forEach { folderPath ->
                    val label = folderDisplayName(folderPath)
                    rows.add {
                        SettingsSwitchRow(
                            Icons.Rounded.Block,
                            label,
                            "系统路径：" + folderPath,
                            checked = true,
                            onCheckedChange = { onTick(); viewModel.toggleExcludedFolder(folderPath) },
                            color = Color(0xFFB64040),
                        )
                    }
                }
                val availableFolders = allFolders.filterNot { folderRuleMatchesForUi(it, settings.excludedFolderPaths) }
                if (availableFolders.isNotEmpty()) {
                    val visibleFolders = if (showAllFolderRules) availableFolders.take(120) else availableFolders.take(10)
                    rows.add {
                        SettingsRow(
                            Icons.Rounded.Folder,
                            if (showAllFolderRules) "收起可选文件夹" else "展开可选文件夹",
                            "当前显示 " + visibleFolders.size + " / " + availableFolders.size + " 个，可点右侧开关排除",
                            onClick = { onTick(); onToggleShowAllFolderRules() },
                            color = Color(0xFF5E8DB4),
                        )
                    }
                    visibleFolders.forEach { folderPath ->
                        val label = folderDisplayName(folderPath)
                        rows.add {
                            SettingsSwitchRow(
                                Icons.Rounded.Folder,
                                label,
                                "系统路径：" + folderPath,
                                checked = false,
                                onCheckedChange = { onTick(); viewModel.toggleExcludedFolder(folderPath) },
                                color = Color(0xFF5E8DB4),
                            )
                        }
                    }
                }
                if (allFolders.isEmpty()) {
                    rows.add { SettingsRow(Icons.Rounded.Info, "暂无文件夹", "先同步媒体库后，这里会列出照片和视频文件夹；也可以先手动输入相机文件夹名称", onClick = { onSuccess(); viewModel.refreshLibrary() }) }
                }
                SettingsGroup(rows)
            }

            SettingsPage.EXPERIENCE -> {
                SectionTitle("操作体验")
                SettingsGroup(listOf<@Composable () -> Unit>(
                    { SettingsSegmentedRow(Icons.Rounded.Tune, "滑动灵敏度", stringResource(settings.swipeSensitivity.labelRes), swipeSensitivityOptions(), settings.swipeSensitivity.name, color = Color(0xFFD44C84), onSelected = { onTick(); viewModel.setSwipeSensitivity(SwipeSensitivity.valueOf(it)) }) },
                    { SettingsSegmentedRow(Icons.Rounded.TouchApp, "手势方向", stringResource(settings.gestureDirection.labelRes), gestureDirectionOptions(), settings.gestureDirection.name, color = Color(0xFFD44C84), onSelected = { onTick(); viewModel.setGestureDirection(GestureDirection.valueOf(it)) }) },
                    { SettingsSwitchRow(Icons.Rounded.MusicNote, "滑动音效", if (settings.swipeSound) "已开启" else "已关闭", checked = settings.swipeSound, onCheckedChange = { onTick(); viewModel.setSwipeSound(it) }, color = Color(0xFFE8A93B)) },
                    { SettingsSwitchRow(Icons.Rounded.MusicNote, "打开视频默认静音", "进入视频整理时默认静音，点侧边栏音量按钮可恢复声音", checked = settings.videoDefaultMuted, onCheckedChange = { onTick(); viewModel.setVideoDefaultMuted(it) }, color = Color(0xFFE8A93B)) },
                    { SettingsSegmentedRow(Icons.Rounded.AspectRatio, "视频显示比例", stringResource(settings.videoDisplayMode.labelRes), videoDisplayModeOptions(), settings.videoDisplayMode.name, color = Color(0xFF5E8DB4), onSelected = { onTick(); viewModel.setVideoDisplayMode(VideoDisplayMode.valueOf(it)) }) },
                    { SettingsSegmentedRow(Icons.Rounded.Vibration, "默认震动强度", stringResource(settings.hapticLevel.labelRes), hapticLevelOptions(), settings.hapticLevel.name, color = Color(0xFF55A6C8), onSelected = { onSuccess(); viewModel.setHapticLevel(HapticLevel.valueOf(it)) }) },
                ))
            }

            SettingsPage.APPEARANCE -> {
                SectionTitle("外观显示")
                SettingsGroup(listOf<@Composable () -> Unit>(
                    { SettingsSegmentedRow(Icons.Rounded.Palette, "界面风格", visualStyleSummary(settings.appVisualStyle), visualStyleOptions(), settings.appVisualStyle.name, onSelected = { onTick(); viewModel.setAppVisualStyle(AppVisualStyle.valueOf(it)) }) },
                    { SettingsSegmentedRow(Icons.Rounded.Palette, "主题模式", stringResource(settings.themeMode.labelRes), themeModeOptions(), settings.themeMode.name, onSelected = { onTick(); viewModel.setThemeMode(ThemeMode.valueOf(it)) }) },
                    { SettingsColorRow(Icons.Rounded.ColorLens, "主题色", "选择应用高亮色", accentColorOptions(), settings.accentColor, onSelected = { onTick(); viewModel.setAccentColor(it) }) },
                    { SettingsSegmentedRow(Icons.Rounded.Folder, "文件夹显示", stringResource(settings.folderDisplay.labelRes), folderDisplayOptions(), settings.folderDisplay.name, onSelected = { onTick(); viewModel.setFolderDisplay(FolderDisplayMode.valueOf(it)) }) },
                    { SettingsSwitchRow(Icons.Rounded.Palette, "沉浸背景", "整理时以前一张照片的模糊效果作为背景", checked = settings.immersiveBackground, onCheckedChange = { onTick(); viewModel.setImmersiveBackground(it) }) },
                ))
            }

            SettingsPage.DATA -> {
                SectionTitle("数据与更新")
                SettingsGroup(listOf<@Composable () -> Unit>(
                    { SettingsRow(Icons.Rounded.Info, "更新日志", "当前版本 " + BuildConfig.VERSION_NAME + " · 查看完整开发记录", color = Color(0xFFE3B13B), onClick = { goTo(SettingsPage.CHANGELOG) }) },
                    { SettingsRow(Icons.Rounded.Info, "隐私政策", "本地相册权限、删除移动规则和数据存储说明", color = Color(0xFF55A6C8), onClick = { goTo(SettingsPage.PRIVACY) }) },
                    { SettingsRow(Icons.Rounded.TouchApp, "使用帮助", "照片整理、视频整理、筛选、相册和排除文件夹", color = Color(0xFF6D9E65), onClick = { goTo(SettingsPage.HELP) }) },
                    { SettingsRow(Icons.Rounded.TouchApp, "重新播放定位式教程", "再次进入照片/视频整理页时，按页面位置逐项指示", color = Color(0xFF7A6AA6), onClick = { onSuccess(); viewModel.replayPositionGuides() }) },
                    { SettingsRow(Icons.Rounded.CleaningServices, "媒体库维护", "重新扫描照片和视频索引", onClick = { onSuccess(); viewModel.refreshLibrary() }) },
                    { SettingsRow(Icons.Rounded.Info, "版本信息", "版本 " + BuildConfig.VERSION_NAME + " · versionCode " + BuildConfig.VERSION_CODE, onClick = { goTo(SettingsPage.CHANGELOG) }) },
                ))
            }

            SettingsPage.CHANGELOG -> { SectionTitle("更新日志"); ChangelogContent() }
            SettingsPage.PRIVACY -> { SectionTitle("隐私政策"); PrivacyPolicyContent() }
            SettingsPage.HELP -> { SectionTitle("使用帮助"); HelpContent() }
            SettingsPage.DIAGNOSIS -> {
                SectionTitle("高级设置")
                SettingsGroup(listOf<@Composable () -> Unit>(
                    { SettingsSegmentedRow(Icons.Rounded.CleaningServices, "照片清理模式", stringResource(settings.photoCleanMode.labelRes), photoCleanModeOptions(), settings.photoCleanMode.name, onSelected = { onSuccess(); viewModel.setPhotoCleanMode(PhotoCleanMode.valueOf(it)) }) },
                    { SettingsRow(Icons.Rounded.Info, "导出闪退日志", "自动记录最近 8 次闪退，方便后续 debug", onClick = { onTick(); if (!shareCrashLogs(context)) viewModel.showMessage("暂无闪退日志") }) },
                ))
                Text(stringResource(settings.photoCleanMode.descriptionRes), modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun visualStyleLabel(style: AppVisualStyle): String = when (style) {
    AppVisualStyle.LIQUID_GLASS -> "经典玻璃"
    AppVisualStyle.IMMERSIVE_PHOTO -> "沉浸照片"
}

private fun visualStyleSummary(style: AppVisualStyle): String = when (style) {
    AppVisualStyle.LIQUID_GLASS -> "保留当前 Liquid Glass 布局"
    AppVisualStyle.IMMERSIVE_PHOTO -> "暗色沉浸布局，照片优先，控制收进胶囊"
}

private fun cleaningDisplaySummary(settings: AppSettings): String {
    val photo = if (settings.photoFocusMode) "照片专注" else "完整控制"
    val video = if (settings.videoShowProgressBar) "视频细进度" else "视频极简"
    return photo + " · " + video
}

private fun settingsSortLabel(order: String): String = if (order == "newest") "最新在前" else "随机"

private fun settingsDateModeOptions(): List<Pair<String, String>> = listOf(
    "all" to "全部",
    "year" to "今年",
    "month" to "本月",
    "seven_days" to "7天",
    "today_history" to "那天",
)

private fun settingsSortOptions(): List<Pair<String, String>> = listOf(
    "random" to "随机",
    "newest" to "最新",
)

private fun settingsPhotoTypeLabel(type: String): String = when (type) {
    "normal" -> "普通照片"
    "screenshot" -> "截屏"
    "selfie" -> "自拍"
    "motion" -> "实况"
    "long" -> "长图"
    "gif" -> "动图"
    "raw" -> "RAW"
    else -> "全部照片"
}

private fun settingsPhotoTypeOptions(): List<Pair<String, String>> = listOf(
    "all" to "全部",
    "normal" to "普通",
    "screenshot" to "截图",
    "selfie" to "自拍",
    "motion" to "实况",
    "gif" to "GIF",
    "long" to "长图",
    "raw" to "RAW",
)

private fun visualStyleOptions(): List<Pair<String, String>> = listOf(
    AppVisualStyle.IMMERSIVE_PHOTO.name to "沉浸",
    AppVisualStyle.LIQUID_GLASS.name to "玻璃",
)

@Composable
private fun themeModeOptions(): List<Pair<String, String>> = ThemeMode.entries.map { it.name to stringResource(it.labelRes) }

@Composable
private fun folderDisplayOptions(): List<Pair<String, String>> = FolderDisplayMode.entries.map { it.name to stringResource(it.labelRes) }

@Composable
private fun deleteModeOptions(): List<Pair<String, String>> = DeleteMode.entries.map { it.name to stringResource(it.labelRes) }

@Composable
private fun swipeSensitivityOptions(): List<Pair<String, String>> = SwipeSensitivity.entries.map { it.name to stringResource(it.labelRes) }

@Composable
private fun gestureDirectionOptions(): List<Pair<String, String>> = GestureDirection.entries.map { it.name to stringResource(it.labelRes) }

@Composable
private fun videoDisplayModeOptions(): List<Pair<String, String>> = VideoDisplayMode.entries.map { it.name to stringResource(it.labelRes) }

@Composable
private fun hapticLevelOptions(): List<Pair<String, String>> = HapticLevel.entries.map { it.name to stringResource(it.labelRes) }

@Composable
private fun photoCleanModeOptions(): List<Pair<String, String>> = PhotoCleanMode.entries.map { it.name to stringResource(it.labelRes) }

private fun accentColorOptions(): List<Pair<Long, String>> = listOf(
    0xFFC7ECFE to "冰蓝",
    0xFFDDF5FF to "浅蓝",
    0xFFF4FBFF to "雾白",
    0xFFEAF8FF to "清透",
    0xFFE8F0F7 to "冷灰",
)

private val KeepixIosSettingsBackground = Color(0xFF000000)
private val KeepixIosSettingsSurface = Color(0xFF0B0B0D)
private val KeepixIosSettingsGroupedSurface = Color(0xFF1C1C1E)
private val KeepixIosSettingsSeparator = Color(0xFF38383A)

private fun keepixIosSettingsColorScheme(base: ColorScheme): ColorScheme = base.copy(
    background = KeepixIosSettingsBackground,
    onBackground = Color.White,
    surface = KeepixIosSettingsSurface,
    onSurface = Color.White,
    surfaceVariant = KeepixIosSettingsGroupedSurface,
    onSurfaceVariant = Color(0xFFA1A1A6),
    outline = KeepixIosSettingsSeparator,
    outlineVariant = Color(0xFF2C2C2E),
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF123B65),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF64D2FF),
    onSecondary = Color.Black,
    tertiary = Color(0xFFBF5AF2),
    onTertiary = Color.White,
    error = Color(0xFFFF453A),
    onError = Color.White,
    surfaceTint = Color.Transparent,
)

@Composable
private fun isKeepixIosSettingsStyle(): Boolean =
    MaterialTheme.colorScheme.background == KeepixIosSettingsBackground &&
        MaterialTheme.colorScheme.surfaceVariant == KeepixIosSettingsGroupedSurface

@Composable
private fun SettingsHeader(title: String, subtitle: String, onBack: () -> Unit) {
    val isHome = title == SettingsPage.HOME.title
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = if (isHome) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!isHome) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Surface(
            onClick = onBack,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isHome) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.a11y_back),
                    modifier = Modifier.size(if (isHome) 28.dp else 26.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsBottomSheet(
    sheet: SettingsSheet,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onTick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: KanlemeViewModel,
) {
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var sheetBackProgress by remember { mutableFloatStateOf(0f) }

    fun requestClose() {
        if (!closing) {
            closing = true
            visible = false
        }
    }

    LaunchedEffect(Unit) {
        visible = true
    }
    LaunchedEffect(closing) {
        if (closing) {
            delay(230)
            onDismiss()
        }
    }

    PredictiveBackHandler(enabled = true) { backEvents ->
        try {
            backEvents.collect { event -> sheetBackProgress = event.progress }
            requestClose()
        } catch (_: CancellationException) {
            sheetBackProgress = 0f
            closing = false
            visible = true
        }
    }

    Dialog(
        onDismissRequest = ::requestClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(170)),
                exit = fadeOut(tween(190)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.34f * (1f - sheetBackProgress * 0.55f)))
                        .clickable(onClick = ::requestClose)
                )
            }
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(240)) { it / 3 } + fadeIn(tween(160)),
                exit = slideOutVertically(tween(220)) { it / 2 } + fadeOut(tween(160)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = size.height * 0.22f * sheetBackProgress
                            val scale = 1f - sheetBackProgress * 0.025f
                            scaleX = scale
                            scaleY = scale
                            alpha = 1f - sheetBackProgress * 0.26f
                        }
                        .clickable(onClick = {})
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(42.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)),
                        )
                        when (sheet) {
                            SettingsSheet.MOVE_PERMISSION -> MovePermissionSheet(settings, onTick, viewModel)
                            SettingsSheet.PHOTO_CLEAN_MODE -> PhotoCleanModeSheet(settings, onSuccess, viewModel)
                            SettingsSheet.HAPTIC -> HapticSettingsSheet(settings, onSuccess, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovePermissionSheet(
    settings: AppSettings,
    onTick: () -> Unit,
    viewModel: KanlemeViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "移动到文件夹时",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleLarge,
        )
        SheetToggleCard(
            icon = Icons.Rounded.Folder,
            title = "文件移动功能",
            subtitle = if (settings.autoMoveOnKeepFavorite) "已开启，指定归档会先确认再移动当前媒体" else "点击开启后，可使用指定归档和移动到文件夹功能",
            checked = settings.autoMoveOnKeepFavorite,
            onCheckedChange = {
                onTick()
                viewModel.setAutoMoveOnKeepFavorite(it)
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (settings.autoMoveOnKeepFavorite) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f) else MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                if (settings.autoMoveOnKeepFavorite) "开启后，整理页点“指定归档”选择文件夹并确认，就会移动当前照片或视频并进入下一项" else "请先开启文件移动功能，才能使用移动到文件夹",
                style = MaterialTheme.typography.bodyMedium,
                color = if (settings.autoMoveOnKeepFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PhotoCleanModeSheet(
    settings: AppSettings,
    onSuccess: () -> Unit,
    viewModel: KanlemeViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("选择照片清理模式", style = MaterialTheme.typography.titleLarge)
            Text(
                "如果清理时出现卡顿或卡住，可以切换模式帮助判断问题",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PhotoCleanMode.entries.forEach { mode ->
            SettingsOptionCard(
                icon = when (mode) {
                    PhotoCleanMode.NORMAL -> Icons.Rounded.CheckCircle
                    PhotoCleanMode.DIAGNOSTIC -> Icons.Rounded.SettingsBackupRestore
                    PhotoCleanMode.PERFORMANCE -> Icons.Rounded.Block
                },
                title = stringResource(mode.labelRes),
                subtitle = stringResource(mode.descriptionRes),
                selected = settings.photoCleanMode == mode,
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    onSuccess()
                    viewModel.setPhotoCleanMode(mode)
                },
            )
        }
    }
}

@Composable
private fun HapticSettingsSheet(
    settings: AppSettings,
    onSuccess: () -> Unit,
    viewModel: KanlemeViewModel,
) {
    val hapticEnabled = listOf(
        settings.keepHapticLevel,
        settings.deleteHapticLevel,
        settings.favoriteHapticLevel,
        settings.undoHapticLevel,
    ).any { it != HapticLevel.OFF }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "震动反馈设置",
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleLarge,
        )
        SheetToggleCard(
            icon = Icons.Rounded.Vibration,
            title = "震动反馈",
            subtitle = if (hapticEnabled) "已开启，滑动时提供触觉反馈" else "已关闭，滑动时不提供触觉反馈",
            checked = hapticEnabled,
            onCheckedChange = {
                onSuccess()
                val level = if (it) HapticLevel.MEDIUM else HapticLevel.OFF
                viewModel.setHapticLevel(level)
                viewModel.setKeepHapticLevel(level)
                viewModel.setDeleteHapticLevel(level)
                viewModel.setFavoriteHapticLevel(level)
                viewModel.setUndoHapticLevel(level)
            },
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.44f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text("为不同操作预览震动反馈强度，点击可立即切换当前全局强度", style = MaterialTheme.typography.bodyMedium)
        }
        SettingsGroup(listOf<@Composable () -> Unit>(
            { HapticActionRow("保留", Color(0xFF4C8FF7), settings.keepHapticLevel, onSuccess) { viewModel.setKeepHapticLevel(it) } },
            { HapticActionRow("删除", Color(0xFFE74C4C), settings.deleteHapticLevel, onSuccess) { viewModel.setDeleteHapticLevel(it) } },
            { HapticActionRow("收藏", Color(0xFF54B766), settings.favoriteHapticLevel, onSuccess) { viewModel.setFavoriteHapticLevel(it) } },
            { HapticActionRow("撤销", Color(0xFFF0A63A), settings.undoHapticLevel, onSuccess) { viewModel.setUndoHapticLevel(it) } },
        ))
    }
}

@Composable
private fun SheetToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon, MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon, color)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun HapticActionRow(
    label: String,
    color: Color,
    selectedLevel: HapticLevel,
    onPreview: () -> Unit,
    onLevelSelected: (HapticLevel) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconTile(Icons.Rounded.CheckCircle, color)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        HapticLevelChip("关", HapticLevel.OFF, selectedLevel == HapticLevel.OFF, Color(0xFF8A8F98), onPreview, onLevelSelected)
        HapticLevelChip("默认", HapticLevel.MEDIUM, selectedLevel == HapticLevel.MEDIUM, color, onPreview, onLevelSelected)
        HapticLevelChip("轻", HapticLevel.LIGHT, selectedLevel == HapticLevel.LIGHT, color, onPreview, onLevelSelected)
        HapticLevelChip("强", HapticLevel.STRONG, selectedLevel == HapticLevel.STRONG, color, onPreview, onLevelSelected)
    }
}

@Composable
private fun HapticLevelChip(
    label: String,
    level: HapticLevel,
    selected: Boolean,
    color: Color,
    onPreview: () -> Unit,
    onLevelSelected: (HapticLevel) -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f))
            .clickable {
                onPreview()
                onLevelSelected(level)
            }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ChangelogContent() {
    ContentCard(
        title = "看了么开发更新记录",
        subtitle = "当前版本：" + BuildConfig.VERSION_NAME,
        sections = listOf(
            "v2.3.3 · 今日整理贴纸与导出" to listOf(
                "首页恢复当年今日和今日整理两个并列入口，今日整理不再常驻占位，点击后以贴纸弹窗展示当天整理小结。",
                "今日整理贴纸支持导出为图片，通过系统分享面板发送，不需要额外存储权限，也不会上传照片或视频。",
                "修复首页今日整理入口误接到年度报告的问题，年度报告入口仍保留在我的页面。",
            ),
            "v2.3.2 · 照片整理卡组动画与队列稳定性" to listOf(
                "照片整理卡组的下一张照片会在当前照片拖动时更明显地上浮、放大并准备接位，松手确认后再用短动画补到最终位置，减少突然蹦上来的感觉。",
                "当前照片划走后改为独立飞出动画，下一张照片不再依赖透明淡入接管，降低和下下张重叠、瞬间摆正或闪烁的概率。",
                "修复部分照片显示时出现黑边过多或类似玻璃外框的问题，整理卡片优先按照片真实比例完整展示，并对长图、截图和宽图做更合适的比例边界。",
                "优化照片保留、收藏、待删后的队列移除与补牌顺序，减少操作后卡住、闪退或已经划走的照片又突然回到整理队列的情况。",
            ),
            "v2.2.9 · 动态照片、GIF、成就陈列柜与发布优化" to listOf(
                "媒体库扫描会识别 image/gif、HEIC/HEIF/WEBP 动态照片线索，以及部分国产厂商常见的实况 / 动态 / Live / Motion 命名与伴随短视频文件。",
                "照片整理、收藏、回收站、时间线和大图预览统一使用支持 GIF/动画图片的加载器，GIF 不再只显示第一帧。",
                "照片整理页新增 GIF 筛选入口，实况照片和 GIF 会在卡片上显示标签，底部信息也会标出媒体类型。",
                "照片整理卡片会在可解析时首次自动静音播放一次实况片段，播放结束后回到静态照片；后续可长按照片再次播放，避免一直循环干扰整理。",
                "实况解析改为非侵入式：媒体库扫描阶段只读取 MediaStore 字段，不再打开大图文件；只有当前卡片需要播放时才尝试提取视频片段，避免授权后卡住、扫不到媒体。",
                "刷新媒体库时照片和视频独立扫描，一侧查询异常不会拖垮另一侧，首次授权后更稳定。",
                "照片整理页参考官网展示风格做轻量视觉优化：顶部保留核心操作、可释放空间和进度胶囊；照片信息统一收口到底部栏，避免卡片内外重复。",
                "照片预览页、收藏预览和照片网格补充长按播放实况；识别到同名伴随短视频或嵌入式 Motion Photo 视频段时会以内置播放器播放一次。",
                "成就系统升级为 Steam 风格的本地成就陈列柜，增加完成率、XP、稀有度、分类筛选和解锁 / 未完成状态。",
                "相似照片检测升级为 DCT pHash + dHash + aHash + 颜色距离的多特征候选桶，减少大相册误判和后半段聚类耗时。",
                "发布包启用 R8 代码裁剪和资源裁剪，并移除不再需要的一键编译 bat 脚本，降低发布体积。",
                "当年今日和回收站改为长方形照片墙式展示，减少文件名、路径等信息干扰，更接近看照片而不是看列表。",
                "照片 / 视频整理页和首页开始显示已标记待删内容预计可释放的存储空间，并统一改用主题色提示，不再用红色制造删除警告感。",
                "首页照片 / 视频切换加入滑块动画和触感反馈，不再只是两个静态按钮切换。",
                "照片大图预览底部 Dock 改为 Keepix 蓝色主题暗色胶囊，和当前主题更统一。",
            ),
            "v2.2.8 · 视频控制与设置切换修复" to listOf(
                "视频整理页删除底部播放 / 暂停胶囊，只保留侧边栏播放 / 暂停入口，避免和底部信息区、进度条重叠。",
                "单击视频区域切换播放 / 暂停，左右滑切换沉浸观看和显示按钮，观看区域更干净。",
                "设置页二级菜单取消双页面同时滑入滑出，改为单页面内容替换，避免切换时新旧内容交叉重叠显示。",
            ),
            "v2.2.7 · 相似照片真实进度与防卡住" to listOf(
                "相似照片检测不再使用最高 88% 的模拟进度，改为根据已处理照片数量实时更新进度。",
                "检测会复用已经计算过的缩略指纹，继续检测时不必从第一张照片重新开始。",
                "跳过 RAW、GIF 等不适合快速缩略检测的文件，单张失败不会阻塞整体进度。",
                "相似聚类从全量两两比较改为哈希桶候选比较，大相册检测时更不容易长时间卡在一个百分比。",
            ),
            "v2.2.6 · 视频控制轻量化" to listOf(
                "视频整理页移除居中大号播放 / 暂停按钮，改为靠近底部的小型半透明胶囊，减少观看遮挡。",
                "右侧视频操作栏整体缩窄，播放、静音、收藏、待删、分享和保留按钮尺寸更轻，和现有毛玻璃组件风格保持一致。",
                "播放 / 暂停状态仍保留触感反馈，但不再用突兀的大按钮打断视频观看。",
            ),
            "v2.2.5 · 记忆页、系统删除、视频暂停与后台检测" to listOf(
                "当年今日升级为照片 / 视频混合记忆页，按年份折叠分组，展开后显示多张媒体的类型、时间、文件夹、大小和整理状态。",
                "视频整理页加入播放 / 暂停控制，暂停状态不会再被当前页刷新逻辑强制覆盖。",
                "回收站永久删除接入 Android MediaStore 系统删除授权确认，单项和全部永久删除都会先弹出系统确认。",
                "相似照片检测改为后台进度式任务，页面离开后继续运行，返回后可恢复查看进度，并支持继续检测。",
            ),
            "v2.2.3 · 发布前稳定性、随机与定位式教程" to listOf(
                "平板竖屏恢复底部 Dock，只有横屏或超宽屏才切换左侧 Rail，避免竖屏首页被挤到左侧。",
                "相似照片检测加入进行中状态、空结果提示和重新检测入口，并优化感知哈希解码，降低大图检测时卡死或闪退概率。",
                "照片和视频首次整理默认进入随机队列，随机种子在开始整理和重新随机时都会刷新并持久化。",
                "新手指引改为定位式覆盖层，直接对着顶部操作栏、中间内容区、底部信息栏和相册/操作按钮进行指示。",
                "设置二级菜单去掉行项目淡入闪烁，只保留页面级滑入滑出和预测式返回反馈。",
                "照片整理页降低背景模糊和预加载层数，减少切图时的解码压力，为发布版优化流畅度。",
            ),
            "v2.2.2 · 收藏预览与完整更新日志" to listOf(
                "我的收藏中的照片支持点击进入全屏预览，补齐双指缩放、双击放大 / 复位和放大后拖动查看细节。",
                "收藏预览底部加入相册查看、分享、待删操作，交互方式与回收站预览保持一致。",
                "更新日志按前期“看了么app”迭代记录重新补全，从首页、照片 / 视频整理、平板适配、深色模式、排除文件夹、可视化自定义到当前稳定性修复均有说明。",
            ),
            "v2.2.1 · 回收站预览与防误触" to listOf(
                "回收站列表中的照片支持直接点按预览，预览页支持双指缩放、双击放大和放大后拖动查看细节。",
                "照片整理页手势判定加入方向锁定和主轴比例判断，斜向滑动不再轻易触发待删。",
                "待删手势提高触发阈值，并把提示文案改为松手待删、松手收藏、松手保留，降低误触成本。",
            ),
            "v2.2.0 · 相册查看、缩放与触感" to listOf(
                "照片放大页支持双指缩放、双击放大 / 复位和放大后拖动查看细节。",
                "用相册查看时优先用标准 MediaStore 图片 Uri，并加入 ACTION_VIEW、REVIEW 和授权兜底。",
                "底部 Dock 切换加入轻微震动反馈，平板侧边 Dock 同步支持。",
                "修复视频扫描中潜在重复变量定义，降低后续编译踩坑概率。",
            ),
            "v2.1.9 · 稳定性与导航联动" to listOf(
                "修复放大图片后调用系统相册时不能稳定定位当前图片的问题，补充 ClipData、EXTRA_STREAM、Uri 授权和系统查看兜底逻辑。",
                "优化视频整理页播放器生命周期，只为当前视频创建播放器，减少连续刷视频和重新随机时的闪退概率。",
                "整理页面实时预览同步最新紧凑布局，照片页不再显示旧右侧按钮栏，视频页重新随机进入第一行。",
                "设置二级页面加入滑入、淡出切换动画，并接入预测式返回进度反馈。",
                "我的页服务与支持入口与设置页使用帮助、隐私政策、更新日志、诊断排障统一跳转。",
                "首页关键数字首次出现时加入滚动数字效果，并为主整理数字加入轻微震动反馈。",
                "统一媒体格式化和文件夹规则工具函数，清理重复定义导致的 R8 / 编译歧义。",
            ),
            "v2.1.8 · 队列与文档修复" to listOf(
                "将照片连续整理缓冲从 180 扩大到 1200，视频缓冲从 120 扩大到 600，避免用户误以为一轮只能整理少量文件。",
                "照片整理页重新改为视觉居中摆放，不再因为底部信息栏导致主体图片偏下。",
                "设置页补齐更新日志、隐私政策和使用帮助，并修正版本信息展示。",
                "增强 Windows 一键编译脚本，优先使用 Gradle Wrapper，也会尝试系统 Gradle 和 Android Studio 自带 Gradle。",
            ),
            "v2.1.7 · 紧凑整理页" to listOf(
                "照片整理页改为图片、图片信息、剩余数量为主的极简结构。",
                "删除照片页右侧三个操作按钮，避免和滑动手势重合。",
                "保留、重新随机、图片筛选、时间筛选压缩到顶部一行，筛选项改为点击后展开。",
                "右下角分享入口替换为相册选择入口，减少整理页常驻内容。",
                "视频整理页将重新随机放入第一行顶部操作区。",
            ),
            "v2.1.6 · 随机与结构修复" to listOf(
                "修复重新随机看起来没有变化的问题，随机种子改为参与乘法扰动，而不是只做整体平移。",
                "修复重新随机后顶部控件被撑乱的问题。",
                "整理照片页和视频页 Compose 嵌套结构，减少括号不匹配风险。",
                "视频重新随机时加入准备态，避免短暂闪出暂无队列。",
            ),
            "v2.1.5 · 查看与队列体验" to listOf(
                "往年今日支持点开具体图片查看，不再只能停留在缩略展示。",
                "整理页点击图片可进入放大查看，并尽量接入系统相册可识别的 Uri。",
                "进入整理页时减少先显示暂无队列再显示图片的闪烁。",
                "照片整理页接入更多相册属性信息，为后续相册查看、文件夹选择和信息展示做准备。",
            ),
            "v2.1.4 · 可视化自定义" to listOf(
                "设置页加入一级菜单和二级菜单，避免所有开关堆在同一层。",
                "整理页组件支持实时可视化预览，不再只面对一堆开关。",
                "照片和视频整理页均加入手动重新随机能力。",
                "可视化预览开始覆盖顶部栏、筛选、文件夹、重新随机、信息栏、手势提示等组件。",
            ),
            "v2.1.3 · 深色模式与排除文件夹" to listOf(
                "修复深色模式下部分卡片、文字和背景白光不一致的问题，减少傻黑和白色光晕。",
                "修复夜间模式下文字看不见、组件背景与整体不统一的问题。",
                "排除文件夹支持在照片和视频整理入口中配置。",
                "优先调用安卓原生文件夹选择器，手动输入仅作为备用。",
            ),
            "v2.1.2 · 照片 / 视频切换" to listOf(
                "首页整理入口支持通过顶部选项切换照片整理或视频整理。",
                "照片与视频切换时加入淡入淡出，减少背景突然闪烁。",
                "照片和视频整理流程拆分，但保持统一的整理入口和操作语言。",
            ),
            "v2.1.1 · 平板与首页细节" to listOf(
                "适配更宽屏幕下的整理入口和首页卡片布局。",
                "优化平板宽度下照片、视频整理页面的卡片排布，避免仍按手机布局过度留白。",
                "开始整理按钮加厚，提升点击感和视觉重量。",
                "替换应用图标资源，并统一部分页面的主色调。",
            ),
            "v2.1.0 · 首页、我的页与基础功能整合" to listOf(
                "首页聚合照片、视频、回收站、收藏、最近项目和常用整理入口。",
                "我的页面加入服务与支持、功能入口和数据概览，后续逐步与设置页内容对齐。",
                "底部 Dock 和大屏侧边导航形成基础导航框架。",
            ),
            "v2.0.0 · 本地照片与视频整理框架" to listOf(
                "基于本地 MediaStore 扫描照片和视频，建立应用内索引。",
                "照片整理支持保留、收藏、待删和撤销上一操作。",
                "视频整理采用上下刷视频的整理体验，并提供收藏、待删等快速动作。",
                "提供相似照片、时间线、最近照片、往年今日、回收站和收藏等基础页面。",
            ),
            "早期版本 · 看了么基础体验" to listOf(
                "围绕相册清理建立轻量化的看图整理体验，而不是复杂文件管理器。",
                "以本地处理为核心，不加入账号、会员、支付或云端同步。",
                "逐步确立照片 / 视频双整理、手势操作、原生相册联动和可视化设置的产品方向。",
            ),
        ),
    )
}

@Composable
private fun PrivacyPolicyContent() {
    ContentCard(
        title = "隐私政策",
        subtitle = "Keepix 以本地相册整理为核心，不包含账号、会员、支付、广告追踪或云端同步。",
        sections = listOf(
            "1. 我们会访问哪些权限" to listOf(
                "照片和视频权限：用于读取系统 MediaStore，建立本地媒体索引，并在整理、收藏、回收站、当年今日、相似照片检测等页面展示缩略图或预览。",
                "媒体写入 / 删除授权：仅在你主动移动照片、放入系统回收站、恢复或永久删除时调用 Android 系统授权弹窗。",
                "文件夹选择权限：仅在你主动选择排除文件夹或目标归档相册时调用系统选择器。",
                "应用不会在后台偷偷读取通讯录、定位、麦克风、摄像头或其他与相册整理无关的权限。",
            ),
            "2. 数据如何处理" to listOf(
                "媒体文件本身保留在你的设备系统相册中，Keepix 不会上传、复制或同步你的照片和视频到服务器。",
                "应用会在本机数据库保存媒体索引、整理状态、收藏状态、待删状态、相似图片指纹、排除文件夹和界面偏好。",
                "相似照片检测、GIF 显示、实况照片播放和当年今日匹配均在本机完成，不依赖远程识别服务。",
                "实况照片播放可能会临时在应用缓存中生成可播放的视频片段，该缓存只用于本机预览。",
            ),
            "3. 删除、恢复与释放空间" to listOf(
                "待删内容会先进入应用内回收站或系统回收站流程，具体行为取决于你的系统版本和应用设置。",
                "永久删除会调用 Android 系统删除授权；确认后文件可能无法通过 Keepix 恢复，请在操作前确认。",
                "首页和整理页展示的可释放空间来自本机已标记待删媒体的文件大小估算，实际释放空间以系统删除完成后的结果为准。",
            ),
            "4. 备份与导出" to listOf(
                "如果你使用备份功能，导出的文件可能包含媒体路径、整理状态、排除规则和界面偏好。",
                "备份文件由你自己选择保存、分享或删除，请不要发送给不可信对象。",
                "卸载应用可能会删除应用内部保存的数据库、设置和缓存，但不会主动删除系统相册中的原始媒体文件。",
            ),
            "5. 第三方与网络" to listOf(
                "当前版本不包含账号登录、会员支付、广告 SDK 或第三方行为追踪。",
                "应用核心整理功能不需要网络；若未来加入联网功能，应在功能说明和隐私政策中单独说明。",
            ),
            "6. 你的控制权" to listOf(
                "你可以随时在系统设置中撤销照片和视频权限。",
                "你可以在应用内刷新媒体库、清理排除文件夹、恢复回收站内容或删除应用缓存。",
                "如果不希望某些目录进入整理队列，请在设置或整理入口中添加排除文件夹。",
            ),
        ),
    )
}

@Composable
private fun HelpContent() {
    ContentCard(
        title = "使用帮助",
        subtitle = "按整理流程阅读即可，所有操作都围绕照片、视频、筛选和回收站展开。",
        sections = listOf(
            "v2.2.5 · 记忆页、系统删除、视频暂停与后台检测" to listOf(
                "当年今日升级为照片 / 视频混合记忆页，按年份折叠分组，展开后显示多张媒体的类型、时间、文件夹、大小和整理状态。",
                "视频整理页加入播放 / 暂停控制，暂停状态不会再被当前页刷新逻辑强制覆盖。",
                "回收站永久删除接入 Android MediaStore 系统删除授权确认，单项和全部永久删除都会先弹出系统确认。",
                "相似照片检测改为后台进度式任务，页面离开后继续运行，返回后可恢复查看进度，并支持继续检测。",
            ),
            "1. 首次使用" to listOf(
                "进入应用后先授予照片和视频权限，应用会从系统 MediaStore 建立本地索引。",
                "如果首页数量不准确，可以进入设置的数据与更新，点击媒体库维护重新扫描。",
            ),
            "2. 定位式新手教程" to listOf(
                "首次进入照片/视频整理页时，会出现覆盖在真实页面上的位置指示，而不是普通文字手册。",
                "每一步会框出对应区域，例如顶部操作栏、中间图片/视频区域、底部信息栏和相册/操作按钮。",
                "可在设置的数据与更新中点击重新播放定位式教程，再次进入整理页即可重看。",
            ),
            "3. 照片整理" to listOf(
                "主区域只保留当前图片、图片信息和剩余文件数量，减少干扰。",
                "顶部保留用于快速保留当前图片；重新随机会重新生成随机队列。",
                "图片筛选可切换全部、普通照片、截图、自拍、实况、长图。",
                "时间筛选可切换全部时间、最近 7 天、本月和今年。",
                "点按图片或信息栏可以放大查看。",
            ),
            "4. 手势操作" to listOf(
                "横向滑动用于快速保留。",
                "纵向滑动用于收藏或待删，具体方向可在设置中切换。",
                "不想误触时，可以降低滑动灵敏度或先使用顶部按钮操作。",
            ),
            "5. 相册与排除文件夹" to listOf(
                "右下角相册按钮用于选择目标相册，开启自动归档后，保留 / 收藏时可移动到对应文件夹。",
                "排除文件夹用于让截图、微信、下载等目录不进入整理队列。",
                "优先使用系统文件夹选择器添加排除；手动输入适合系统选择器不可用时兜底。",
            ),
            "6. 视频整理" to listOf(
                "视频整理页采用上下刷视频的形式。",
                "重新随机已经放到顶部第一行，点击即可重新生成视频顺序。",
                "可在设置中调整视频显示比例、默认静音和进度条显示。",
            ),
            "7. 收藏与预览" to listOf(
                "我的收藏会集中显示整理时收藏过的照片和视频。",
                "收藏照片可以点击进入全屏预览，支持双击放大、双指缩放和放大后拖动。",
                "收藏预览底部提供相册查看、分享和待删操作。",
            ),
            "8. 回收站与撤销" to listOf(
                "误操作后优先使用撤销上一操作。",
                "待删内容可以在回收站中恢复或永久删除。",
                "回收站照片也支持全屏预览和缩放查看。",
                "永久删除前请确认，因为部分系统删除操作无法直接恢复。",
            ),
            "9. 常见问题" to listOf(
                "看到的队列数量不是相册总数：整理页会使用连续缓冲队列，后台会自动补充后续内容。",
                "重新随机变化不明显：请确认当前筛选下文件数量足够，数量太少时随机结果会有限。",
                "某些图片不显示：刷新媒体库，并检查该目录是否被加入排除文件夹。",
                "深色模式文字看不清：切换主题色或主题模式后返回页面重新进入。",
            ),
        ),
    )
}

@Composable
private fun ContentCard(
    title: String,
    subtitle: String,
    sections: List<Pair<String, List<String>>>,
) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.82f) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            sections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(section.first, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    section.second.forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganizerDisplayCustomizer(
    settings: AppSettings,
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    onTogglePhotoFocusMode: () -> Unit,
    onTogglePhotoTopBar: () -> Unit,
    onTogglePhotoFilterChips: () -> Unit,
    onTogglePhotoFolderChips: () -> Unit,
    onTogglePhotoInfoBar: () -> Unit,
    onTogglePhotoGestureHint: () -> Unit,
    onTogglePhotoShuffleButton: () -> Unit,
    onToggleVideoTopBar: () -> Unit,
    onToggleVideoActionRail: () -> Unit,
    onToggleVideoInfoPanel: () -> Unit,
    onToggleVideoProgressBar: () -> Unit,
    onToggleVideoShuffleButton: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsGroup(listOf<@Composable () -> Unit>(
            {
                SettingsSwitchRow(
                    Icons.Rounded.Visibility,
                    "照片整理辅助控件",
                    if (settings.photoFocusMode) "整理页只保留照片和一个显示控件入口" else "整理页显示筛选、进度、相册和图片信息",
                    checked = !settings.photoFocusMode,
                    onCheckedChange = { onTogglePhotoFocusMode() },
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        ))
        GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), tonalAlpha = 0.82f) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("实时预览", style = MaterialTheme.typography.titleLarge)
                Text("这是实际整理页的缩略排布预览。点顶部、筛选、文件夹、重新随机、右侧操作栏、底部信息等区域即可显示或隐藏；隐藏后会出现“显示某组件”的回填按钮。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TwoSegmentSelector(selectedTab = selectedTab, onSelectTab = onSelectTab)
                if (selectedTab == "photo") {
                    PhotoOrganizerMockPreview(
                        settings = settings,
                        onToggleTopBar = onTogglePhotoTopBar,
                        onToggleFilterChips = onTogglePhotoFilterChips,
                        onToggleFolderChips = onTogglePhotoFolderChips,
                        onToggleInfoBar = onTogglePhotoInfoBar,
                        onToggleGestureHint = onTogglePhotoGestureHint,
                        onToggleShuffleButton = onTogglePhotoShuffleButton,
                    )
                } else {
                    VideoOrganizerMockPreview(
                        settings = settings,
                        onToggleTopBar = onToggleVideoTopBar,
                        onToggleActionRail = onToggleVideoActionRail,
                        onToggleInfoPanel = onToggleVideoInfoPanel,
                        onToggleProgressBar = onToggleVideoProgressBar,
                        onToggleShuffleButton = onToggleVideoShuffleButton,
                    )
                }
            }
        }
        if (selectedTab == "photo") {
            ComponentPalette(
                title = "照片整理页组件",
                items = listOf(
                    ComponentToggleItem("顶部进度/筛选", settings.photoShowTopBar, onTogglePhotoTopBar),
                    ComponentToggleItem("图片/时间筛选", settings.photoShowFilterChips, onTogglePhotoFilterChips),
                    ComponentToggleItem("相册按钮", settings.photoShowFolderChips, onTogglePhotoFolderChips),
                    ComponentToggleItem("图片信息", settings.photoShowInfoBar, onTogglePhotoInfoBar),
                    ComponentToggleItem("手势提示", settings.photoShowGestureHint, onTogglePhotoGestureHint),
                    ComponentToggleItem("重新随机按钮", settings.photoShowShuffleButton, onTogglePhotoShuffleButton),
                ),
            )
        } else {
            ComponentPalette(
                title = "视频整理页组件",
                items = listOf(
                    ComponentToggleItem("顶部进度", settings.videoShowTopBar, onToggleVideoTopBar),
                    ComponentToggleItem("右侧操作栏", settings.videoShowActionRail, onToggleVideoActionRail),
                    ComponentToggleItem("底部信息", settings.videoShowInfoPanel, onToggleVideoInfoPanel),
                    ComponentToggleItem("进度条", settings.videoShowProgressBar, onToggleVideoProgressBar),
                    ComponentToggleItem("重新随机按钮", settings.videoShowShuffleButton, onToggleVideoShuffleButton),
                ),
            )
        }
    }
}

private data class ComponentToggleItem(
    val label: String,
    val enabled: Boolean,
    val onToggle: () -> Unit,
)

@Composable
private fun TwoSegmentSelector(selectedTab: String, onSelectTab: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentButton("照片整理", selectedTab == "photo", Modifier.weight(1f)) { onSelectTab("photo") }
        SegmentButton("视频整理", selectedTab == "video", Modifier.weight(1f)) { onSelectTab("video") }
    }
}

@Composable
private fun SegmentButton(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "settings_segment_background",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "settings_segment_content",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.985f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "settings_segment_scale",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = contentColor)
    }
}

@Composable
private fun PhotoOrganizerMockPreview(
    settings: AppSettings,
    onToggleTopBar: () -> Unit,
    onToggleFilterChips: () -> Unit,
    onToggleFolderChips: () -> Unit,
    onToggleInfoBar: () -> Unit,
    onToggleGestureHint: () -> Unit,
    onToggleShuffleButton: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(468.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f), RoundedCornerShape(30.dp))
            .padding(14.dp),
    ) {
        PreviewAreaTitle(
            "照片整理页实时布局",
            "新版布局以中间图片为核心：顶部一行操作，底部只放信息和相册入口。",
        )

        if (settings.photoShowGestureHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(174.dp)
                    .height(248.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.11f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f), RoundedCornerShape(32.dp))
                    .clickable(onClick = onToggleGestureHint),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(118.dp).height(166.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)))
                    Text("图片居中", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("点击图片放大", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            PreviewHiddenBlock("图片手势提示已隐藏", onToggleGestureHint, Modifier.align(Alignment.Center).width(190.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 58.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (settings.photoShowTopBar) PreviewComponentBlock("顶部进度", "42% · 剩余 120", onToggleTopBar, true, Color(0xFF67A9D6), Modifier.weight(1f))
                else PreviewHiddenBlock("顶部进度/筛选", onToggleTopBar, Modifier.weight(1f))
                if (settings.photoShowShuffleButton) PreviewComponentBlock("重新随机", "同一行", onToggleShuffleButton, true, Color(0xFFD89A45), Modifier.weight(1f))
                else PreviewHiddenBlock("重新随机", onToggleShuffleButton, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (settings.photoShowFilterChips) {
                    PreviewComponentBlock("图片筛选", "点开后展开", onToggleFilterChips, true, Color(0xFF8CA7FF), Modifier.weight(1f))
                    PreviewComponentBlock("时间筛选", "点开后展开", onToggleFilterChips, true, Color(0xFF8CA7FF), Modifier.weight(1f))
                } else {
                    PreviewHiddenBlock("图片筛选", onToggleFilterChips, Modifier.weight(1f))
                    PreviewHiddenBlock("时间筛选", onToggleFilterChips, Modifier.weight(1f))
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (settings.photoShowInfoBar) PreviewComponentBlock("图片信息", "日期 · 文件夹 · 大小", onToggleInfoBar, true, Color(0xFFB47DE2), Modifier.weight(1f))
            else PreviewHiddenBlock("图片信息已隐藏", onToggleInfoBar, Modifier.weight(1f))
            if (settings.photoShowFolderChips) PreviewComponentBlock("相册", "替代分享按钮", onToggleFolderChips, true, Color(0xFF72B778), Modifier.width(96.dp))
            else PreviewHiddenBlock("相册按钮", onToggleFolderChips, Modifier.width(96.dp))
        }
    }
}

@Composable
private fun VideoOrganizerMockPreview(
    settings: AppSettings,
    onToggleTopBar: () -> Unit,
    onToggleActionRail: () -> Unit,
    onToggleInfoPanel: () -> Unit,
    onToggleProgressBar: () -> Unit,
    onToggleShuffleButton: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(468.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(30.dp))
            .padding(14.dp),
    ) {
        PreviewAreaTitle("视频整理页实时布局", "重新随机已经进入第一行；播放器只为当前页创建，减少闪退概率。")
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 58.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (settings.videoShowTopBar) PreviewDarkComponentBlock("顶部进度", "12/600 · 待删 3", onToggleTopBar, Modifier.weight(1f))
            else PreviewDarkHiddenBlock("顶部进度已隐藏", onToggleTopBar, Modifier.weight(1f))
            if (settings.videoShowShuffleButton) PreviewDarkComponentBlock("重新随机", "第一行", onToggleShuffleButton, Modifier.width(116.dp))
            else PreviewDarkHiddenBlock("随机按钮", onToggleShuffleButton, Modifier.width(116.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(156.dp)
                .height(260.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.White.copy(alpha = 0.10f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.width(108.dp).height(190.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.16f)))
                Text("短视频画面", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }

        if (settings.videoShowActionRail) {
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).width(76.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PreviewMiniAction("声音", Color(0xFF67A9D6), onToggleActionRail)
                PreviewMiniAction("收藏", Color(0xFFE0A342), onToggleActionRail)
                PreviewMiniAction("待删", Color(0xFFE36A6A), onToggleActionRail)
                PreviewMiniAction("分享", Color(0xFF93D08B), onToggleActionRail)
            }
        } else {
            PreviewDarkHiddenBlock("右侧操作栏", onToggleActionRail, Modifier.align(Alignment.CenterEnd).width(96.dp))
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (settings.videoShowInfoPanel) PreviewDarkComponentBlock("底部信息", "00:42 · 18MB · 长按 2 倍速", onToggleInfoPanel, Modifier.fillMaxWidth())
            else PreviewDarkHiddenBlock("底部信息已隐藏", onToggleInfoPanel, Modifier.fillMaxWidth())
            if (settings.videoShowProgressBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.20f))
                        .clickable(onClick = onToggleProgressBar),
                ) {
                    Box(Modifier.fillMaxWidth(0.46f).height(18.dp).background(MaterialTheme.colorScheme.primary))
                }
            } else {
                PreviewDarkHiddenBlock("进度条已隐藏", onToggleProgressBar, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PreviewAreaTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreviewComponentBlock(label: String, detail: String, onClick: () -> Unit, enabled: Boolean, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = if (enabled) 0.18f else 0.06f))
            .border(1.dp, color.copy(alpha = if (enabled) 0.42f else 0.18f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun PreviewHiddenBlock(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label + " · 点击显示", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreviewDarkComponentBlock(label: String, detail: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.70f), maxLines = 1)
        }
    }
}

@Composable
private fun PreviewDarkHiddenBlock(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label + " · 点击显示", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.70f))
    }
}

@Composable
private fun PreviewMiniAction(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.20f))
            .border(1.dp, color.copy(alpha = 0.34f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun ComponentPalette(title: String, items: List<ComponentToggleItem>) {
    val enabledNames = items.filter { it.enabled }.joinToString("、") { it.label }.ifBlank { "未显示任何组件" }
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), tonalAlpha = 0.82f) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text("当前显示：" + enabledNames, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.chunked(2).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { item ->
                            ComponentTogglePill(item = item, modifier = Modifier.weight(1f))
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentTogglePill(item: ComponentToggleItem, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (item.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, if (item.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .clickable(onClick = item.onToggle)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text((if (item.enabled) "显示 · " else "隐藏 · ") + item.label, style = MaterialTheme.typography.labelLarge, color = if (item.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PreviewTag(text: String, modifier: Modifier, onClick: () -> Unit, enabled: Boolean) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.90f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PreviewDarkTag(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun HiddenComponentButton(visible: Boolean, text: String, modifier: Modifier, onClick: () -> Unit) {
    if (visible) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.26f), RoundedCornerShape(999.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SmallPreviewChip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun PreviewCircle(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private fun customizationSummary(settings: AppSettings): String {
    val photo = enabledCount(listOf(settings.photoShowTopBar, settings.photoShowFilterChips, settings.photoShowFolderChips, settings.photoShowInfoBar, settings.photoShowGestureHint, settings.photoShowShuffleButton))
    val video = enabledCount(listOf(settings.videoShowTopBar, settings.videoShowActionRail, settings.videoShowInfoPanel, settings.videoShowProgressBar, settings.videoShowShuffleButton))
    val photoMode = if (settings.photoFocusMode) "照片辅助控件已隐藏" else "照片显示 " + photo + "/6"
    return photoMode + " · 视频显示 " + video + "/5"
}

private fun enabledCount(values: List<Boolean>): Int = values.count { it }

private fun folderDisplayName(folderPath: String): String {
    val normalized = folderPath.trim().replace('\\', '/').trim('/')
    val name = normalized.substringAfterLast('/').ifBlank { normalized }
    return when (name.lowercase()) {
        "camera" -> "相机文件夹"
        "dcim" -> "相册主文件夹"
        "screenshots", "screenshot" -> "截图文件夹"
        "download", "downloads" -> "下载文件夹"
        "pictures" -> "图片文件夹"
        "movies" -> "视频文件夹"
        "weixin", "wechat" -> "微信文件夹"
        "qq" -> "QQ 文件夹"
        else -> if (name.isBlank()) "未命名文件夹" else name
    }
}

@Composable
private fun ManualExcludeFolderEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconTile(Icons.Rounded.Add, MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("手动新增排除文件夹", style = MaterialTheme.typography.titleMedium)
                Text("可输入完整路径或文件夹名，例如：相机文件夹、截图文件夹、下载文件夹", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("输入要排除的文件夹") },
            )
            Button(onClick = onAdd) { Text("添加") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val iosStyle = isKeepixIosSettingsStyle()
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = if (iosStyle) 22.dp else 24.dp, top = if (iosStyle) 6.dp else 2.dp),
        fontWeight = if (iosStyle) FontWeight.SemiBold else FontWeight.Normal,
    )
}

@Composable
private fun SettingsGroup(rows: List<@Composable () -> Unit>) {
    val iosStyle = isKeepixIosSettingsStyle()
    val shape = RoundedCornerShape(if (iosStyle) 18.dp else 26.dp)
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(vertical = if (iosStyle) 0.dp else 2.dp)) {
            rows.forEachIndexed { index, row ->
                row()
                if (index != rows.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = if (iosStyle) 58.dp else 22.dp, end = if (iosStyle) 0.dp else 22.dp)
                            .height(1.dp)
                            .background(
                                if (iosStyle) {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.46f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                }
                            )
                    )
                }
            }
        }
    }
    if (iosStyle) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = androidx.compose.foundation.BorderStroke(0.7.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)),
            content = content,
        )
    } else {
        GlassSurface(modifier = Modifier.fillMaxWidth(), shape = shape, tonalAlpha = 0.88f, content = content)
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    SettingsRow(icon = icon, title = title, subtitle = subtitle, color = color, onClick = onClick)
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    trailingContent: (@Composable () -> Unit)? = null,
    showIcon: Boolean = true,
    onClick: () -> Unit,
) {
    val iosStyle = isKeepixIosSettingsStyle()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (iosStyle) 18.dp else 22.dp,
                vertical = if (iosStyle) if (showIcon) 14.dp else 16.dp else if (showIcon) 15.dp else 19.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (iosStyle) 14.dp else 12.dp),
    ) {
        if (showIcon) IconTile(icon, color)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (iosStyle) FontWeight.SemiBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (badge != null) BadgeText(badge)
            }
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        trailingContent?.invoke() ?: Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f))
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    badge: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    showIcon: Boolean = true,
) {
    val iosStyle = isKeepixIosSettingsStyle()
    val rowBackground by animateColorAsState(
        targetValue = if (!iosStyle && checked) color.copy(alpha = 0.075f) else Color.Transparent,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "settings_switch_row_background",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (iosStyle) 0.dp else 10.dp, vertical = if (iosStyle) 0.dp else 4.dp)
            .clip(RoundedCornerShape(if (iosStyle) 0.dp else 20.dp))
            .background(rowBackground)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = if (iosStyle) 18.dp else 12.dp, vertical = if (iosStyle) if (showIcon) 14.dp else 16.dp else if (showIcon) 11.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (iosStyle) 14.dp else 12.dp),
    ) {
        if (showIcon) IconTile(icon, color)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (iosStyle) FontWeight.SemiBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (badge != null) BadgeText(badge)
            }
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = { onCheckedChange(it) })
    }
}

@Composable
private fun SettingsSegmentedRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    color: Color = MaterialTheme.colorScheme.primary,
    onSelected: (String) -> Unit,
) {
    val iosStyle = isKeepixIosSettingsStyle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (iosStyle) 18.dp else 22.dp, vertical = if (iosStyle) 14.dp else 15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (iosStyle) 14.dp else 12.dp)) {
            IconTile(icon, color)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (iosStyle) FontWeight.SemiBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clip(RoundedCornerShape(999.dp))
                .background(if (iosStyle) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.84f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (value, label) ->
                SegmentButton(
                    text = label,
                    selected = value == selectedValue,
                    modifier = Modifier.width(86.dp),
                    onClick = { onSelected(value) },
                )
            }
        }
    }
}

@Composable
private fun SettingsColorRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<Pair<Long, String>>,
    selectedColor: Long,
    color: Color = MaterialTheme.colorScheme.primary,
    onSelected: (Long) -> Unit,
) {
    val iosStyle = isKeepixIosSettingsStyle()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (iosStyle) 18.dp else 22.dp, vertical = if (iosStyle) 14.dp else 15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (iosStyle) 14.dp else 12.dp)) {
            IconTile(icon, color)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (iosStyle) FontWeight.SemiBold else FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { (value, label) ->
                ColorSwatchButton(
                    colorValue = value,
                    label = label,
                    selected = value == selectedColor,
                    onClick = { onSelected(value) },
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchButton(
    colorValue: Long,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val iosStyle = isKeepixIosSettingsStyle()
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.20f),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "settings_color_swatch_border",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.96f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "settings_color_swatch_scale",
    )
    Column(
        modifier = Modifier
            .width(68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (iosStyle) {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (selected) 0.90f else 0.54f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (selected) 0.70f else 0.42f)
                }
            )
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(colorValue), RoundedCornerShape(999.dp))
                .border(1.dp, Color.White.copy(alpha = 0.70f), RoundedCornerShape(999.dp)),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsStepperRow(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val iosStyle = isKeepixIosSettingsStyle()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (iosStyle) 18.dp else 22.dp, vertical = if (iosStyle) 14.dp else 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (iosStyle) 14.dp else 12.dp),
    ) {
        IconTile(icon, color)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = if (iosStyle) FontWeight.SemiBold else FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (iosStyle) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.84f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(Icons.Rounded.Remove, "减少", onDecrease)
            Box(Modifier.width(58.dp), contentAlignment = Alignment.Center) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            StepperButton(Icons.Rounded.Add, "增加", onIncrease)
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun IconTile(icon: ImageVector, color: Color) {
    val iosStyle = isKeepixIosSettingsStyle()
    Box(
        modifier = Modifier
            .size(if (iosStyle) 32.dp else 40.dp)
            .background(
                if (iosStyle) color.copy(alpha = 0.92f) else color.copy(alpha = 0.10f),
                RoundedCornerShape(if (iosStyle) 8.dp else 13.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (iosStyle) Color.White else color.copy(alpha = 0.88f),
            modifier = Modifier.size(if (iosStyle) 18.dp else 21.dp),
        )
    }
}

@Composable
private fun BadgeText(text: String) {
    Box(Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ColorDot(color: Long) {
    Box(Modifier.size(34.dp).background(Color(color), RoundedCornerShape(999.dp)))
}

@Composable
private fun ThemeModeDots(selectedIndex: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(28.dp)
                    .background(
                        if (i == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(999.dp),
                    )
            )
        }
    }
}
