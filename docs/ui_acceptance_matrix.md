# 看了么 UI 设备与截图验收矩阵

这份矩阵是 E08 的执行入口。截图必须在目标 route、标题、权限状态和 fixture 已确认后生成；动态媒体区域可以遮罩，但不能遮罩布局、焦点或控件错误。

## 当前状态

- 代码级窗口矩阵：已提交 `e4b5941`，覆盖 320/360/390/600/840dp × 字体 1.0/1.3/2.0 的 `AdaptiveWidthInfo` 边界
- Android 设备：API30 `KeepixApi30` 与 API36 `medium_phone` AVD 已执行组件级导航 golden；当前没有连接物理设备
- 组件/页面组截图 golden：`NavigationGoldenTest` 与 `ScreenGoldenTest` 已覆盖手机/平板、light/dark，每个 API 各 8 个 PNG，共 16 个；资产位于 `app/src/androidTest/assets/goldens/`
- 真机截图、TalkBack、D-pad、旋转、分屏和折叠状态：`not-run`
- 未连接物理设备时不得把 Compose 编译成功、AVD 组件 golden、旧 APK 或历史截图记为真机通过

## 尺寸与状态矩阵

| 组别 | 窗口宽度 | 字体缩放 | 目标形态 | 必查问题 |
| --- | ---: | ---: | --- | --- |
| C1 | 320dp | 1.0 / 1.3 / 2.0 | 紧凑手机 | 顶部横向控件、底部操作、中文/英文截断 |
| C2 | 360dp | 1.0 / 1.3 / 2.0 | 常见手机 | 进度、相册、撤销和 Keep/Favorite/Delete 不重叠 |
| C3 | 390dp | 1.0 / 1.3 / 2.0 | 宽手机 | 引导层定位、视频信息面板、手势区域 |
| M1 | 600dp | 1.0 / 1.3 / 2.0 | 中等窗口/分屏 | 控件是否错误缩小，列表与内容区是否保持可读 |
| E1 | 840dp | 1.0 / 1.3 / 2.0 | 平板展开 | rail/two-pane、焦点顺序、横屏内容宽度 |

每个宽度至少覆盖：浅色、深色；中文、英文；有数据、空态、加载中；照片清理、视频清理、我的、设置、回收站和大图查看器。旋转、自由窗口、分屏和折叠状态变化需要重复检查 route 与状态是否保持。

## 核心路径与稳定状态

| route/区域 | 稳定 fixture | 截图前断言 | 交互验收 |
| --- | --- | --- | --- |
| 首页 | 合成照片/视频数量 | 首页标题、当前 tab、权限已就绪 | 键盘/D-pad 可到照片、视频、我的 |
| 照片整理 | 至少 3 张合成照片 | 照片整理标题、顶部筛选、当前照片名称 | Keep/Favorite/Delete、撤销、打开大图 |
| 视频整理 | 至少 2 个本地视频描述 | 视频整理标题、播放器页已稳定 | 播放/暂停、前后 10 秒、显示控件、动作 rail |
| 我的/设置 | 空态与默认设置 | tab、设置标题和开关状态 | 焦点顺序、开关选中状态、返回 |
| 回收站 | 1 个待删照片、1 个待删视频 | 回收站标题、媒体类型标签 | 恢复、永久删除确认、返回 |
| 大图查看器 | 1 张合成照片 | 文件名、日期、当前页码 | 返回、分享、动作按钮、缩放 |

## 执行与证据

在固定设备/API/主题/字体设置下执行：

```powershell
adb devices
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleRelease --offline --console=plain --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --offline --console=plain --no-daemon
adb exec-out screencap -p > artifacts/ui-acceptance/<device>/<route>.png
```

每次记录设备型号、Android/API、窗口 dp、字体缩放、主题、locale、Git SHA、fixture 标识、route、截图时间和测试结果。组件或页面组 golden 失败时保存 actual/expected/diff；截图目录和包含真实媒体的产物不得提交。当前 golden 只覆盖导航组件和 `MeScreen` 页面组，不代表下方其他业务页面已通过。

## 通过条件

只有以下条件都满足才能把矩阵行从 `not-run` 改为 `pass`：

1. route、标题、权限和 fixture 断言通过
2. 截图在稳定语义节点后生成，不依赖固定 sleep 或坐标盲点
3. TalkBack 能完成核心动作，D-pad/键盘能遍历按钮与开关
4. 触控目标至少 48dp，字体 2.0 和英文长文本没有遮挡或不可达控件
5. actual/expected/diff 和运行元数据可追溯到同一 Git SHA
