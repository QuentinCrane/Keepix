# Baseline Profile 状态

## 已提交

- `:benchmark` 使用 AndroidX Baseline Profile Gradle plugin `1.4.1`
- `BaselineProfileGenerator` 覆盖冷启动、底部导航、照片/视频快捷入口、滑动动作和设置入口
- `:app` 使用 Profile Installer `1.4.1`，并通过 `baselineProfile(project(":benchmark"))` 消费生成结果
- Release 仍保持 R8 与资源压缩；未开启每次 Release 自动启动设备测试

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

本机当前没有连接 Android 设备，D02 的设备生成、profile 打包检查和 TTID/TTFD 对照尚未运行。构建验证只能证明 Gradle 配置和生成器可编译，不能替代设备收益验证。
