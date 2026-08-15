# Baseline Profile 状态

## 已提交

- `:benchmark` 使用 AndroidX Baseline Profile Gradle plugin `1.4.1`
- `BaselineProfileGenerator` 覆盖冷启动、底部导航、照片/视频快捷入口、滑动动作和设置入口
- `:app` 使用 Profile Installer `1.4.1`，并通过 `baselineProfile(project(":benchmark"))` 消费生成结果
- Release 仍保持 R8 与资源压缩；未开启每次 Release 自动启动设备测试

## 本机配置构建验证（2026-08-14）

在未连接设备的当前环境执行：

```powershell
.\gradlew.bat :benchmark:assembleBenchmarkRelease :benchmark:assembleNonMinifiedRelease --offline --configuration-cache --console=plain --no-daemon --max-workers=1
```

结果：明确返回 `Exit code: 0`，`BUILD SUCCESSFUL in 1m 59s`，62 个 actionable tasks 中 52 个执行、10 个 up-to-date。该结果证明 benchmark 测试 APK 的两个 Release 变体和当前依赖配置可编译打包，不证明 Baseline Profile 已在设备上生成或带来启动收益。

## 设备执行尝试（2026-08-14）

在 API30 `KeepixApi30` AVD（`sdk_gphone_x86_64`、Android 11、1080×2400、420 dpi）上执行了官方 connected benchmark 任务：

```powershell
.\gradlew.bat :benchmark:connectedBenchmarkReleaseAndroidTest --offline --no-configuration-cache --console=plain --no-daemon --max-workers=1
```

结果不能计入性能通过：`BaselineProfileGenerator` 在该 benchmark 变体被跳过，`CriticalJourneyBenchmark.coldStartup` 明确失败并报告 `ERRORS (not suppressed): EMULATOR`。AndroidX 明确要求真实性能使用物理设备，不能通过压制 `EMULATOR` 错误把 AVD 数值当作用户设备证据。

此前 `nonMinifiedRelease` 的 XML 还记录了生成器的实现缺口：`Process com.futureape.kanleme never flushed profiles ... only killed it with scope.killProcess`。提交 `cc777f6` 已让生成器兼容中英文界面，并将快捷入口的强杀改为 `MacrobenchmarkScope.killProcess()`；该提交的 `:benchmark:compileBenchmarkReleaseKotlin` 已明确成功，但物理设备上的最终 profile 生成和打包验证仍未运行。

## 生成与验收

连接固定的 Android 设备后，在仓库根目录执行：

```powershell
./gradlew.bat :app:generateBaselineProfile `
    '-Pandroidx.baselineprofile.skipgeneration=false' `
    '-Pkanleme.baselineProfile.useConnectedDevices=true'
./gradlew.bat :app:assembleRelease
```

至少记录以下三组对照：无 profile、partial compilation、full compilation；每组报告冷启动 TTID/TTFD、照片首帧和设备/API/温度/电量。生成器会使用测试路径中的系统启动命令，不读取或上传用户照片、视频，也不向仓库写入猜测规则。

## 当前限制

本机只有 AVD 设备执行证据，且被 AndroidX 的 `EMULATOR` 质量门禁拒绝；D02 的物理设备生成、profile 打包检查和 TTID/TTFD 对照尚未运行。上面的构建与 AVD 尝试只能证明 Gradle 配置、生成器编译和失败边界，不能替代真实性能收益验证。
