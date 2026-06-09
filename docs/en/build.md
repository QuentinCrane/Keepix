# Keepix Build Guide

Use Android Studio or the Gradle Wrapper from the project root.

## Debug Build

```powershell
.\gradlew.bat :app:assembleDebug
```

## Release Build

```powershell
.\gradlew.bat :app:assembleRelease
```

## Output Paths

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release APK:

```text
app/build/outputs/apk/release/
```

## Release Signing

Release builds keep R8 shrinking and resource shrinking enabled.

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

The signed Release APK is currently about 6.2 MiB. Use your own release keystore from local machine storage or CI secrets, and do not commit signing keys, passwords, `local.properties`, or build artifacts.

### Local signing check

If your local `local.properties` already contains signing parameters, you can finish the release APK with Android SDK build-tools:

```powershell
.\gradlew.bat :app:assembleRelease
zipalign -p -f 4 app\build\outputs\apk\release\app-release-unsigned.apk app\build\outputs\apk\release\app-release-aligned.apk
apksigner sign --ks <your-release-keystore> --ks-key-alias <alias> --out app\build\outputs\apk\release\app-release-signed.apk app\build\outputs\apk\release\app-release-aligned.apk
apksigner verify --verbose --print-certs app\build\outputs\apk\release\app-release-signed.apk
```

Only upload the final signed APK or a release zip. Do not upload unsigned or aligned intermediate files.

