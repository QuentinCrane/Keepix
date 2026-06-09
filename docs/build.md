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

## Release 发布构建

Release 构建已启用 R8 代码压缩与资源裁剪：

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

当前签名 Release APK 体积约 6.2 MiB。发布 APK 请使用本机或 CI Secret 中的 release keystore 签名，不要把签名密钥、密码、`local.properties` 或构建产物提交进仓库。

### 本机签名检查

仓库不会公开保存签名密钥或密码。若本机 `local.properties` 中配置了签名参数，可以在 Release 构建后使用 Android SDK `build-tools` 中的 `zipalign` 与 `apksigner` 生成最终 APK：

```powershell
.\gradlew.bat :app:assembleRelease
zipalign -p -f 4 app\build\outputs\apk\release\app-release-unsigned.apk app\build\outputs\apk\release\app-release-aligned.apk
apksigner sign --ks <your-release-keystore> --ks-key-alias <alias> --out app\build\outputs\apk\release\app-release-signed.apk app\build\outputs\apk\release\app-release-aligned.apk
apksigner verify --verbose --print-certs app\build\outputs\apk\release\app-release-signed.apk
```

发布附件建议只上传最终签名 APK，或上传发布用压缩包；不要上传 unsigned / aligned 中间产物。

```text
app/build/outputs/apk/release/app-release-signed.apk
app/build/outputs/release-package/kanleme-v2.3.0-release.zip
```
