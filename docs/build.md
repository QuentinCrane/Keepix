# 构建说明

建议直接使用 Android Studio，或在项目根目录通过 Gradle Wrapper 构建。Windows 使用 `gradlew.bat`，macOS / Linux 使用 `./gradlew`。

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

## 常用命令

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:kapt
```

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK 输出位置：

```text
app/build/outputs/apk/release/
```

## v2.2.9 发布构建

Release 构建已启用 R8 代码压缩与资源裁剪：

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

本轮发布优化后，Release APK 体积从约 54 MB 降至约 6.1 MB。发布 APK 请使用本机或 CI Secret 中的 release keystore 签名，不要把签名密钥、密码、`local.properties` 或构建产物提交进仓库。
