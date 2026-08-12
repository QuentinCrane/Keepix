# 设置项“存储—消费—验收”真值表

这张表是 C01 的基线。`AppSettings` 是 DataStore 读出的运行时快照；表中的“状态”只描述当前代码，不把“设置页能保存”误判成“业务已消费”。每个 `AppSettings` 字段必须在表中有且仅有一行，`AppSettingsTruthTableTest` 会在字段新增或重复时失败。

状态含义：

- `ACTIVE`：已有真实运行时消费者，仍需按验收项回归
- `PREVIEW_ONLY`：目前主要影响设置页预览，照片/视频真实页面尚未接通
- `STORAGE_ONLY`：能存储和读取，但当前没有可证明的真实页面消费者

| 字段 | 默认值 | DataStore key / 写入口 | 当前运行时消费者 | 状态 | 最小验收 |
| --- | --- | --- | --- | --- | --- |
| `deleteMode` | `PENDING_CONFIRM` | `delete_mode` / `setDeleteMode` | ViewModel 删除动作与系统回收站授权 | ACTIVE | 两种模式分别走预期删除路径 |
| `photoBatchSize` | `30` | `photo_batch_size` / `setPhotoBatchSize` | 照片会话目标与 Deck 加载 | ACTIVE | 修改后新会话目标和进度一致 |
| `videoBatchSize` | `20` | `video_batch_size` / `setVideoBatchSize` | 视频会话目标与 Deck 加载 | ACTIVE | 修改后新会话目标和进度一致 |
| `autoMoveOnKeepFavorite` | `true` | `auto_move_on_keep_favorite` / `setAutoMoveOnKeepFavorite` | 保留/收藏后的归档移动入口 | ACTIVE | 开关关闭时不发起移动授权 |
| `similarDetection` | `false` | `similar_detection` / `setSimilarDetection` | 相似照片开关触发当前 ViewModel 任务 | ACTIVE（待 C08 持久化） | 开关、关闭、进程重建行为有明确定义 |
| `swipeSensitivity` | `STANDARD` | `swipe_sensitivity` / `setSwipeSensitivity` | 卡片阈值、视频快进阈值 | ACTIVE | 三档阈值与手势方向均可复现 |
| `gestureDirection` | `DEFAULT` | `gesture_direction` / `setGestureDirection` | 照片卡片上下动作映射 | ACTIVE | 反向后 UI 提示和实际动作一致 |
| `quickActionButtons` | `true` | `quick_action_buttons` / `setQuickActionButtons` | 照片整理页底部保留/收藏/待删快捷按钮 | ACTIVE | 关闭后仍可用滑动手势和其他入口 |
| `swapShareAndUndo` | `false` | `swap_share_and_undo` / `setSwapShareAndUndo` | 照片整理页底部分享/撤销位置 | ACTIVE | 只交换视觉顺序，不交换业务回调 |
| `swipeSound` | `true` | `swipe_sound` / `setSwipeSound` | `PhotoSwipeSoundPlayer` | ACTIVE | 保留/收藏/待删均遵守开关 |
| `swipeSoundStyle` | `SOFT_BREEZE` | `swipe_sound_style` / `setSwipeSoundStyle` | `PhotoSwipeSoundPlayer` | ACTIVE | 三种音色选择和回退路径有效 |
| `videoDefaultMuted` | `true` | `video_default_muted` / `setVideoDefaultMuted` | 视频清理页播放器初始静音 | ACTIVE | 首次进入、切页、手动恢复声音 |
| `videoDisplayMode` | `IMMERSIVE_CROP` | `video_display_mode` / `setVideoDisplayMode` | 视频播放器 resize mode | ACTIVE | 三种比例在手机/平板可见 |
| `hapticLevel` | `MEDIUM` | `haptic_level` / `setHapticLevel` | 通用 `HapticKit` | ACTIVE | 关闭和三档强度均不崩溃 |
| `keepHapticLevel` | `MEDIUM` | `keep_haptic_level` / `setKeepHapticLevel` | `HapticKit.keep` | ACTIVE | 保留动作使用独立强度 |
| `deleteHapticLevel` | `MEDIUM` | `delete_haptic_level` / `setDeleteHapticLevel` | `HapticKit.delete` | ACTIVE | 待删动作使用独立强度 |
| `favoriteHapticLevel` | `MEDIUM` | `favorite_haptic_level` / `setFavoriteHapticLevel` | `HapticKit.favorite` | ACTIVE | 收藏动作使用独立强度 |
| `undoHapticLevel` | `MEDIUM` | `undo_haptic_level` / `setUndoHapticLevel` | `HapticKit.undo` | ACTIVE | 撤销动作使用独立强度 |
| `themeMode` | `SYSTEM` | `theme_mode` / `setThemeMode` | `MainActivity` / `KanlemeTheme` | ACTIVE | 系统/浅色/深色重建后保持 |
| `todayInHistoryEntryMode` | `MEMORY_PAGE` | `today_in_history_entry_mode` / `setTodayInHistoryEntryMode` | 收藏/往年今日入口 | ACTIVE | 两种入口都能到达且返回正确 |
| `accentColor` | `0xFFC7ECFE` | `accent_color` / `setAccentColor` | `MainActivity` / 主题色 | ACTIVE | 重启后主题色保持 |
| `folderDisplay` | `SINGLE_LINE` | `folder_display` / `setFolderDisplay` | 文件夹显示组件 | ACTIVE | 长文件夹名在两种模式都可读 |
| `immersiveBackground` | `true` | `immersive_background` / `setImmersiveBackground` | 照片整理页模糊背景图层 | ACTIVE | 开关背景图层，不改变卡片和手势 |
| `photoCleanMode` | `NORMAL` | `photo_clean_mode` / `setPhotoCleanMode` | 照片整理页视觉/诊断/预取策略 | ACTIVE | 三种模式都有可观察结果且不改变动作语义 |
| `homeMediaTab` | `photo` | `home_media_tab` / `setHomeMediaTab` | 首页照片/视频页签与时间轴 | ACTIVE | 切换后首页和时间轴一致 |
| `photoDateMode` | `all` | `photo_date_mode` / `setPhotoDateMode` | 照片 `CleaningScope` | ACTIVE | 范围筛选和重启保持 |
| `photoMediaType` | `all` | `photo_media_type` / `setPhotoMediaType` | 照片类型筛选 | ACTIVE | 类型筛选不串视频 |
| `photoSortOrder` | `random` | `photo_sort_order` / `setPhotoSortOrderWithSeed` | 照片 Deck 查询 | ACTIVE | 随机种子和最新排序可复现 |
| `photoRandomSeed` | `1` | `photo_random_seed` / 随机排序写入 | 照片 Deck 随机顺序 | ACTIVE | 刷新/续接不无故换序 |
| `photoFolderPath` | `""` | `photo_folder_path` / `setPhotoFolderPath` | 照片 `CleaningScope` | ACTIVE | 文件夹筛选和排除规则一致 |
| `videoDateMode` | `all` | `video_date_mode` / `setVideoDateMode` | 视频 `CleaningScope` | ACTIVE | 范围筛选和重启保持 |
| `videoSortOrder` | `random` | `video_sort_order` / `setVideoSortOrderWithSeed` | 视频 Deck 查询 | ACTIVE | 随机种子和最新排序可复现 |
| `videoRandomSeed` | `1` | `video_random_seed` / 随机排序写入 | 视频 Deck 随机顺序 | ACTIVE | 切页/续接不无故换序 |
| `videoFolderPath` | `""` | `video_folder_path` / `setVideoFolderPath` | 视频 `CleaningScope` | ACTIVE | 文件夹筛选和排除规则一致 |
| `photoGuideShown` | `false` | `photo_guide_shown` / `setPhotoGuideShown` | 照片整理引导显示 | ACTIVE | 关闭/完成后不重复弹出 |
| `videoGuideShown` | `false` | `video_guide_shown` / `setVideoGuideShown` | 视频整理引导显示 | ACTIVE | 关闭/完成后不重复弹出 |
| `excludedFolderPaths` | `emptySet()` | `excluded_folder_paths` / 增删/清空方法 | 扫描后的照片/视频队列过滤 | ACTIVE | 两种媒体均排除且可恢复 |
| `onboardingShown` | `false` | `onboarding_shown` / `setOnboardingShown` | 根导航门禁 | ACTIVE | 首次流程、进程重建、返回路径 |
| `photoFocusMode` | `false` | `photo_focus_mode` / `setPhotoFocusMode` | 照片整理页 chrome 显示 | ACTIVE | 专注模式仍保留等价操作入口 |
| `photoShowTopBar` | `true` | `photo_show_top_bar` / `setPhotoShowTopBar` | 照片整理页顶部进度条 | ACTIVE | 关闭后顶部进度条隐藏，队列和手势不变 |
| `photoShowFilterChips` | `true` | `photo_show_filter_chips` / `setPhotoShowFilterChips` | 照片整理页类型/时间筛选菜单 | ACTIVE | 关闭后筛选入口隐藏，当前筛选仍保持 |
| `photoShowFolderChips` | `true` | `photo_show_folder_chips` / `setPhotoShowFolderChips` | 照片整理页信息栏文件夹入口 | ACTIVE | 关闭后文件夹不可点击但日期信息保留 |
| `photoShowActionRail` | `true` | `photo_show_action_rail` / `setPhotoShowActionRail` | 照片整理页分享/撤销操作栏 | ACTIVE | 关闭后通过更多操作菜单保留等价入口 |
| `photoShowInfoBar` | `true` | `photo_show_info_bar` / `setPhotoShowInfoBar` | 照片整理页底部日期/文件夹信息栏 | ACTIVE | 关闭后信息栏隐藏，卡片手势和操作不变 |
| `photoShowGestureHint` | `true` | `photo_show_gesture_hint` / `setPhotoShowGestureHint` | 照片整理页边缘滑动光效 | ACTIVE | 关闭后不显示光效，动作仍提交 |
| `photoShowShuffleButton` | `true` | `photo_show_shuffle_button` / `setPhotoShowShuffleButton` | 照片整理页顶部重新随机按钮 | ACTIVE | 关闭后按钮隐藏，固定种子语义不变 |
| `videoShowTopBar` | `true` | `video_show_top_bar` / `setVideoShowTopBar` | 视频 chrome 可见性 | ACTIVE | 关闭后仍有可发现控制入口 |
| `videoShowActionRail` | `true` | `video_show_action_rail` / `setVideoShowActionRail` | 视频右侧操作栏 | ACTIVE | 隐藏后保留等价手势/菜单 |
| `videoShowInfoPanel` | `true` | `video_show_info_panel` / `setVideoShowInfoPanel` | 视频底部信息面板 | ACTIVE | 关闭后进度条仍按设置显示 |
| `videoShowFolderChips` | `true` | `video_show_folder_chips` / `setVideoShowFolderChips` | 视频整理页顶部“指定归档”文件夹按钮 | ACTIVE | 关闭后隐藏按钮，队列和手势不变 |
| `videoShowProgressBar` | `true` | `video_show_progress_bar` / `setVideoShowProgressBar` | 视频底部进度条 | ACTIVE | 横向拖动和隐藏状态一致 |
| `videoShowShuffleButton` | `true` | `video_show_shuffle_button` / `setVideoShowShuffleButton` | 视频顶部随机按钮 | ACTIVE | 点击只改变顺序，不改变动作身份 |
| `videoChromeVisible` | `true` | `video_chrome_visible` / `setVideoChromeVisible` | 视频播放器 chrome 总开关 | ACTIVE | 播放器生命周期和控制入口不丢失 |

## 当前结论

照片与视频整理页的设置消费缺口已闭合；C04 的具体视觉与模式决策记录在 `docs/photo_clean_modes.md`。后续 C05 继续澄清连续 Deck 的“每批数量”语义，不把设置页预览截图单独作为功能完成证据。
