# 看了么构建说明

这是完整 Android Studio 工程，不是增量补丁。克隆或解压后直接打开根目录即可编译。

## 推荐编译方式

1. 用 Android Studio 打开本目录。
2. 等待 Gradle Sync 完成。
3. 选择 `app` 运行配置。
4. 点击 Run，或使用 Build > Build Bundle(s) / APK(s) > Build APK(s)。

## 命令行编译

Windows 可直接双击 `scripts/windows/` 目录中的：

```text
scripts/windows/一键编译Debug.bat
scripts/windows/build_debug.bat
scripts/windows/compile_debug_apk.bat
```

脚本会优先使用 `gradlew.bat`，没有时尝试系统 `gradle`，再尝试 Android Studio 自带 Gradle。

仓库已包含 Gradle Wrapper，推荐直接执行：

```bash
./gradlew :app:assembleDebug
```

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
```

## 当前工程基线

- Application ID：`com.futureape.kanleme`
- App 名称：`看了么`
- minSdk：30
- targetSdk：35
- compileSdk：35
- Kotlin：2.0.21
- AGP：8.8.2
- Compose：1.8.2
- Material3：1.3.2
- Room：2.7.1
- Hilt：2.56.1
- Media3：1.6.1

## 已遵守的项目约束

- 没有会员、VIP、支付、订阅、授权校验。
- SettingsScreen 不使用 `Column.() -> Unit` DSL receiver 写法。
- 不使用 `matchParentSize()`。
- 不使用 `animateItemPlacement()`。
- 中文字符串不使用 `${mediaLabel}` 这类模板拼接。
- 预测式返回、相册权限、MediaStore 扫描、DataStore 设置、照片/视频整理流、可视化设置页均包含在源码中。

## 注意

本仓库不包含 Android SDK、Gradle 缓存、签名密钥或本机构建产物。首次编译时 Android Studio / Gradle 会下载依赖。
