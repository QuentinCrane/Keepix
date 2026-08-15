# 测试策略与运行说明

验证快照：2026-08-15。本文记录当前仓库真实存在的测试层级、命令和证据边界；它不把构建成功写成设备验收成功。

## 当前测试栈

- `app/src/test`：JUnit4 本地单元测试，覆盖设置真值表、清理事件/会话、媒体同步、相似检测任务契约、统计、缓存和媒体 fixture 目录。
- `app/src/androidTest`：AndroidJUnit4/Hilt runner、Compose UI test、Room testing 和系统相关契约测试。
- `benchmark`：AndroidX Macrobenchmark 与 Baseline Profile generator，覆盖冷启动、底部导航、照片/视频入口、滑动动作和设置入口。
- 当前没有引入 Robolectric、第三方截图 golden 框架、UI Automator、Jacoco 或 Gradle Managed Devices；E08 使用仓库内轻量的 `NavigationGoldenTest` 完成导航组件 golden 对比，不应把它扩展解释为完整页面截图或设备验收。

## 本地命令

Windows 使用 `gradlew.bat`，macOS/Linux 使用 `./gradlew`。

```powershell
# 快速 JVM 单测
.\gradlew.bat :app:testDebugUnitTest --offline --console=plain --no-daemon --max-workers=1

# AndroidTest 源码和 Hilt/Room 生成编译（不运行设备测试）
.\gradlew.bat :app:compileDebugAndroidTestKotlin --offline --console=plain --no-daemon --max-workers=1

# 静态检查
.\gradlew.bat :app:lintDebug --offline --console=plain --no-daemon --max-workers=1

# 规划中的 app 发布门禁
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleRelease --offline --configuration-cache --console=plain --no-daemon --max-workers=1

# Benchmark 两个测试 APK 变体
.\gradlew.bat :benchmark:assembleBenchmarkRelease :benchmark:assembleNonMinifiedRelease --offline --configuration-cache --console=plain --no-daemon --max-workers=1
```

`compileDebugAndroidTestKotlin` 只证明 AndroidTest 可编译；要运行测试需要连接设备或已配置的 AVD：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
adb devices
```

## API 30、API 35、API 36 设备执行

2026-08-14 在本机已有的 API 35 Google Play x86_64 system image 上建立临时 `KeepixApi35` AVD，设备为 `sdk_gphone64_x86_64`、Android 15/API 35、1080x2400、420 dpi。执行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --offline --configuration-cache --console=plain --no-daemon --max-workers=1
```

结果：明确返回 `Exit code: 0`，25 tests、0 failures、0 errors、0 skipped。该次运行证明当前 AndroidTest、Hilt runner、WorkManager 可用性、Compose 测试和 Room 测试能在 API35 模拟器执行；它不是 API30/API36 矩阵、真机/OEM 或系统 UI 验收。运行时工作树仍保留四个用户未提交的 UI 文件，它们未被本轮触碰或暂存，因此该结果也不等同于干净 Release 候选验收。

同日继续在两个目标 API AVD 上执行同一门禁：

| 设备 | 结果 | 报告 |
| --- | --- | --- |
| `KeepixApi30` / Android 11 API 30 / `sdk_gphone_x86_64` / 1080×2400 / 420 dpi | `Exit code: 0`，`BUILD SUCCESSFUL`，36 tests、0 failures、0 errors、0 skipped | `app/build/outputs/androidTest-results/connected/debug/TEST-KeepixApi30(AVD) - 11-_app-.xml` |
| `medium_phone` / Android 16 API 36 / `sdk_gphone64_x86_64` / 1080×2400 / 420 dpi | `Exit code: 0`，`BUILD SUCCESSFUL`，36 tests、0 failures、0 errors、0 skipped | `app/build/outputs/androidTest-results/connected/debug/TEST-medium_phone(AVD) - 16-_app-.xml` |

API 30 手动授权观察到 `READ_EXTERNAL_STORAGE` 的允许/拒绝状态；API 36 手动观察到 full、partial（`READ_MEDIA_VISUAL_USER_SELECTED` only）和 denied 三态，应用在 partial 状态显示 `Selected media access` 与 `Re-select access`。这些是 debug AVD 上的启动/授权观察，不等于物理设备、OEM、TalkBack/D-pad、删除系统授权或完整发布候选验收。详细记录见[发布候选验证记录](发布候选验证记录_2026-08-14.md)。

## Baseline Profile 与性能

连接固定物理设备后，按 [Baseline Profile 状态](baseline_profile_status.md) 生成 profile，并记录无 profile、partial compilation、full compilation 三组 TTID/TTFD、首帧、PSS、帧时间、温度和电量。API30 AVD 的 Macrobenchmark 已被 AndroidX 以 `EMULATOR` 拒绝；`performance_baseline.md` 中没有真实数值前，状态必须保持 `not-run`。

Benchmark 构建通过不等于 profile 已生成，也不等于性能收益已证明；主机 JVM/Dart/Gradle 时间不能替代同一 Android 设备的前后对照。

## UI、窗口与无障碍验收

规划矩阵覆盖 320/360/390/600/840dp、字体/显示 1.0/1.3/2.0、横竖屏、分屏/自由窗口、浅色/深色、高对比、触控、返回手势、TalkBack 和平板键盘/D-pad。`AdaptiveRenderSmokeTest` 在 API30 AVD 上对 15 个宽度/字体组合完成真实 Compose 节点渲染与尺寸断言；`NavigationAccessibilityTest` 已覆盖手机胶囊和大屏侧栏，`MediaActionAccessibilityTest` 覆盖 Keep/Favorite/Delete，验证导航和核心操作节点可点击、带焦点语义且触控区域不小于 48dp。`NavigationGoldenTest` 已在 API30/API36 AVD 上对手机/平板侧栏的 light/dark 四种组合完成 8 个 actual/expected 对比，golden 资产位于 `app/src/androidTest/assets/goldens/`。这些组件级结果仍不能替代完整业务页面、动画稳定节点、全字体/窗口设备矩阵或连接设备的 TalkBack/D-pad 结果。

截图目录中的图片是文档/历史参考，不自动证明当前 route、主题、数据 fixture 或设备布局正确。

## 媒体与系统路径

媒体兼容范围和隐私边界见 [媒体 fixture 目录](media_fixture_catalog.md)。合成 Motion Photo 尾部有 JVM 覆盖；HEIC/HEIF、RAW、OEM Motion Photo、长尾视频和系统 MediaStore 权限仍需获许可样本与设备执行。

删除/恢复/移动、局部媒体权限、系统授权取消/失败、WorkManager 重启和 Media3/OEM 解码均属于设备或系统 UI 路径，不能仅由 JVM 测试或 Release APK 存在推导完成。

## 当前证据状态

截至本快照，`lintDebug`、JVM 单测、AndroidTest 编译、API30/API35/API36 AVD 上的 AndroidTest、app Release 门禁和 benchmark APK 构建均有明确成功退出码；API30/API36 的基础启动与权限状态也已观察。API30/API36 最新复跑均为 36/36，包含 MediaStore URI、WorkManager 进程恢复/约束、渲染矩阵、手机/侧栏导航和 Keep/Favorite/Delete 焦点语义测试，以及导航组件 golden 对比。最终设备矩阵验收仍保持未完成，因为 OEM/系统授权失败路径、TalkBack/D-pad 人工验收、完整字体/窗口设备矩阵、完整页面与动画截图、Media3 和真实性能数据尚未闭环。
