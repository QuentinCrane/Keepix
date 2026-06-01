package com.futureape.kanleme.data.settings

import androidx.compose.runtime.Immutable

@Immutable
data class AppSettings(
    val deleteMode: DeleteMode = DeleteMode.PENDING_CONFIRM,
    val photoBatchSize: Int = 20,
    val videoBatchSize: Int = 20,
    val autoMoveOnKeepFavorite: Boolean = true,
    val similarDetection: Boolean = false,
    val swipeSensitivity: SwipeSensitivity = SwipeSensitivity.STANDARD,
    val gestureDirection: GestureDirection = GestureDirection.DEFAULT,
    val quickActionButtons: Boolean = true,
    val swapShareAndUndo: Boolean = false,
    val swipeSound: Boolean = true,
    val videoDefaultMuted: Boolean = false,
    val videoDisplayMode: VideoDisplayMode = VideoDisplayMode.IMMERSIVE_CROP,
    val hapticLevel: HapticLevel = HapticLevel.MEDIUM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: Long = 0xFF2F6886,
    val folderDisplay: FolderDisplayMode = FolderDisplayMode.SINGLE_LINE,
    val immersiveBackground: Boolean = true,
    val photoCleanMode: PhotoCleanMode = PhotoCleanMode.NORMAL,
    val homeMediaTab: String = "photo",
    val photoDateMode: String = "all",
    val photoMediaType: String = "all",
    val photoSortOrder: String = "random",
    val photoRandomSeed: Long = 1L,
    val photoFolderPath: String = "",
    val videoSortOrder: String = "random",
    val videoRandomSeed: Long = 1L,
    val videoFolderPath: String = "",
    val photoGuideShown: Boolean = false,
    val videoGuideShown: Boolean = false,
    val excludedFolderPaths: Set<String> = emptySet(),
    val onboardingShown: Boolean = false,
    val photoShowTopBar: Boolean = true,
    val photoShowFilterChips: Boolean = true,
    val photoShowFolderChips: Boolean = true,
    val photoShowActionRail: Boolean = true,
    val photoShowInfoBar: Boolean = true,
    val photoShowGestureHint: Boolean = true,
    val photoShowShuffleButton: Boolean = true,
    val videoShowTopBar: Boolean = true,
    val videoShowActionRail: Boolean = true,
    val videoShowInfoPanel: Boolean = true,
    val videoShowFolderChips: Boolean = true,
    val videoShowProgressBar: Boolean = true,
    val videoShowShuffleButton: Boolean = true,
) {
    val photoThreshold: Float get() = swipeSensitivity.thresholdScale * if (photoCleanMode == PhotoCleanMode.CONSERVATIVE) 1.18f else 1f
    val videoSeekThresholdPx: Float get() = 80f * swipeSensitivity.thresholdScale
}

enum class DeleteMode(val label: String, val description: String) {
    PENDING_CONFIRM("待删确认", "先放入待删列表，确认后再处理"),
    SYSTEM_TRASH("系统回收站", "尝试移入系统回收站，不额外限制"),
}

enum class SwipeSensitivity(val label: String, val thresholdScale: Float) {
    GENTLE("灵敏", 0.72f),
    STANDARD("标准", 1.0f),
    STABLE("稳一点", 1.28f),
}

enum class GestureDirection(val label: String, val guide: String) {
    DEFAULT("上删下藏", "上滑待删，下滑收藏，左右保留"),
    REVERSE_VERTICAL("上藏下删", "上滑收藏，下滑待删，左右保留"),
}

enum class VideoDisplayMode(val label: String, val description: String) {
    IMMERSIVE_CROP("沉浸裁切", "像短视频平台一样铺满屏幕，横屏视频会被裁切"),
    FIT_SCREEN("完整比例", "保留视频原始比例，横屏视频不会被强制竖屏裁切"),
    FILL_WIDTH("铺满屏宽", "优先铺满屏幕宽度，适合横屏和混合素材"),
}

enum class HapticLevel(val label: String) {
    OFF("关闭"),
    LIGHT("轻微"),
    MEDIUM("细腻"),
    STRONG("明显"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

enum class FolderDisplayMode(val label: String) {
    SINGLE_LINE("单行显示"),
    MULTI_LINE("多行显示"),
}

enum class PhotoCleanMode(val label: String, val description: String) {
    NORMAL("正常使用", "日常使用推荐，不额外记录，也不改变清理方式"),
    CONSERVATIVE("保守确认", "待删动作会更谨慎，适合误触较多时使用"),
}

fun nextBatchSize(current: Int): Int = when (current) {
    10 -> 20
    20 -> 40
    40 -> 60
    else -> 10
}

fun nextAccentColor(current: Long): Long {
    val palette = listOf(0xFF2F6886, 0xFF7CC6F2, 0xFF5F8D6B, 0xFF7A6AA6, 0xFFD86B86, 0xFFE39E42)
    val index = palette.indexOf(current).takeIf { it >= 0 } ?: 0
    return palette[(index + 1) % palette.size]
}
