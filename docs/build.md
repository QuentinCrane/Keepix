# 构建说明

建议直接使用 Android Studio，或在项目根目录通过 Gradle Wrapper 构建。Windows 使用 `gradlew.bat`，macOS / Linux 使用 `./gradlew`。

当前应用版本为 `2.3.5`，`versionCode` 为 `61`，`minSdk = 30`，`compileSdk = 36`，`targetSdk = 36`。

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

## Benchmark 与 Baseline Profile 配置构建

这两个任务只验证 benchmark 测试 APK 和 Baseline Profile 配置能够编译打包，不会在没有连接设备时生成真实 profile 或性能数据：

```powershell
.\gradlew.bat :benchmark:assembleBenchmarkRelease :benchmark:assembleNonMinifiedRelease
```

连接固定设备后，生成 profile 和 TTID/TTFD 对照请按 [Baseline Profile 状态](baseline_profile_status.md) 执行。

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK 输出位置：

```text
app/build/outputs/apk/release/
```

## Release 发布构建

Release 构建已启用 R8 代码压缩与资源裁剪：

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

默认 Release 构建产物是未签名 APK。发布 APK 请使用本机或 CI Secret 中的 release keystore 签名；签名后的大小取决于构建与签名配置，不在这里固定声明。不要把签名密钥、密码、`local.properties` 或构建产物提交进仓库。

### 本机签名检查

仓库不会公开保存签名密钥或密码。若本机 `local.properties` 中配置了签名参数，可以在 Release 构建后使用 Android SDK `build-tools` 中的 `zipalign` 与 `apksigner` 生成最终 APK：

```powershell
.\gradlew.bat :app:assembleRelease
zipalign -p -f 4 app\build\outputs\apk\release\app-release-unsigned.apk app\build\outputs\apk\release\app-release-aligned.apk
apksigner sign --ks <your-release-keystore> --ks-key-alias <alias> --out app\build\outputs\apk\release\app-release-signed.apk app\build\outputs\apk\release\app-release-aligned.apk
apksigner verify --verbose --print-certs app\build\outputs\apk\release\app-release-signed.apk
```

发布附件只上传最终签名 APK 或经过审阅的发布压缩包；不要上传 `app-release-unsigned.apk`、aligned 中间产物或 `app/build/`。

```text
app/build/outputs/apk/release/app-release-signed.apk
```
