# Keepix Build Guide

Use Android Studio or the Gradle Wrapper from the project root.

The current app version is `2.3.5`, `versionCode` is `61`, `minSdk = 30`, and both `compileSdk` and `targetSdk` are `36`.

## Debug Build

```powershell
.\gradlew.bat :app:assembleDebug
```

## Release Build

```powershell
.\gradlew.bat :app:assembleRelease
```

## Benchmark and Baseline Profile Configuration Build

These tasks only verify that the benchmark test APKs and Baseline Profile configuration compile and package. They do not generate a real profile or performance measurements without a connected device:

```powershell
.\gradlew.bat :benchmark:assembleBenchmarkRelease :benchmark:assembleNonMinifiedRelease
```

For profile generation and TTID/TTFD comparisons on a fixed device, follow the [Baseline Profile status](../baseline_profile_status.md) instructions.

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

The default Release output is an unsigned APK. Use your own release keystore from local machine storage or CI secrets for publishing; the signed size depends on the build and signing configuration. Do not commit signing keys, passwords, `local.properties`, or build artifacts.

### Local signing check

If your local `local.properties` already contains signing parameters, you can finish the release APK with Android SDK build-tools:

```powershell
.\gradlew.bat :app:assembleRelease
zipalign -p -f 4 app\build\outputs\apk\release\app-release-unsigned.apk app\build\outputs\apk\release\app-release-aligned.apk
apksigner sign --ks <your-release-keystore> --ks-key-alias <alias> --out app\build\outputs\apk\release\app-release-signed.apk app\build\outputs\apk\release\app-release-aligned.apk
apksigner verify --verbose --print-certs app\build\outputs\apk\release\app-release-signed.apk
```

Upload only the final signed APK or a reviewed release package. Do not upload `app-release-unsigned.apk`, aligned intermediate files, or `app/build/`.
