# Testing and Validation Guide

Validation snapshot: 2026-08-14. This document records the test layers, commands, and evidence boundaries that actually exist in the repository. A successful build is not treated as device acceptance.

## Current test stack

- `app/src/test`: JUnit 4 local unit tests for settings truth tables, cleanup events and sessions, media sync, similar-analysis contracts, statistics, cache policy, and the media fixture catalog.
- `app/src/androidTest`: AndroidJUnit4/Hilt runner, Compose UI tests, Room testing, and system-facing contract tests.
- `benchmark`: AndroidX Macrobenchmark and Baseline Profile generator covering cold startup, bottom navigation, photo/video entry points, swipe actions, and settings.
- Robolectric, screenshot golden tooling, UI Automator, Jacoco, and Gradle Managed Devices are not currently configured; their results must not be inferred from the existing build.

## Local commands

Windows uses `gradlew.bat`; macOS/Linux uses `./gradlew`.

```powershell
# Fast JVM unit tests
.\gradlew.bat :app:testDebugUnitTest --offline --console=plain --no-daemon --max-workers=1

# Compile AndroidTest sources and Hilt/Room generated code (does not run on a device)
.\gradlew.bat :app:compileDebugAndroidTestKotlin --offline --console=plain --no-daemon --max-workers=1

# Static checks
.\gradlew.bat :app:lintDebug --offline --console=plain --no-daemon --max-workers=1

# Planned app release gate
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleRelease --offline --configuration-cache --console=plain --no-daemon --max-workers=1

# Two benchmark test APK variants
.\gradlew.bat :benchmark:assembleBenchmarkRelease :benchmark:assembleNonMinifiedRelease --offline --configuration-cache --console=plain --no-daemon --max-workers=1
```

`compileDebugAndroidTestKotlin` only proves that AndroidTest compiles. Running the tests requires a connected device or configured AVD:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
adb devices
```

## API 30, API 35, and API 36 device execution

On 2026-08-14, a temporary `KeepixApi35` AVD was created from the existing API 35 Google Play x86_64 system image. The device was `sdk_gphone64_x86_64`, Android 15/API 35, 1080x2400 at 420 dpi. The run used:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --offline --configuration-cache --console=plain --no-daemon --max-workers=1
```

Result: explicit `Exit code: 0`, 25 tests, 0 failures, 0 errors, and 0 skipped. This proves that the current AndroidTest, Hilt runner, WorkManager availability, Compose tests, and Room tests can execute on an API 35 emulator; it is not the API 30/API 36 matrix, a physical-device/OEM check, or system-UI acceptance. The four user-owned unstaged UI files were still present and untouched during this run, so the result is not equivalent to clean Release-candidate acceptance. A later API 30 run added the MediaStore URI contract test and completed 29 tests.

The same gate was then executed on the two target API AVDs:

| Device | Result | Report |
| --- | --- | --- |
| `KeepixApi30` / Android 11 API 30 / `sdk_gphone_x86_64` / 1080x2400 / 420 dpi | `Exit code: 0`, `BUILD SUCCESSFUL`, latest run 29 tests, 0 failures, 0 errors, 0 skipped | `app/build/outputs/androidTest-results/connected/debug/TEST-KeepixApi30(AVD) - 11-_app-.xml` |
| `medium_phone` / Android 16 API 36 / `sdk_gphone64_x86_64` / 1080x2400 / 420 dpi | `Exit code: 0`, `BUILD SUCCESSFUL`, 30 tests, 0 failures, 0 errors, 0 skipped | `app/build/outputs/androidTest-results/connected/debug/TEST-medium_phone(AVD) - 16-_app-.xml` |

Manual permission observations: API 30 reached the home screen with `READ_EXTERNAL_STORAGE` granted and returned to the authorization screen after denial. API 36 covered full access, partial access with `READ_MEDIA_VISUAL_USER_SELECTED` only (the app displayed `Selected media access` and `Re-select access`), and denied access. These are debug-AVD startup and permission observations, not physical-device/OEM, TalkBack/D-pad, delete-authorization, or full release-candidate acceptance. See the [release-candidate validation record](发布候选验证记录_2026-08-14.md) for the detailed matrix.

## Baseline Profile and performance

With a fixed physical device connected, follow [Baseline Profile status](../baseline_profile_status.md) and record three groups: no profile, partial compilation, and full compilation. The API 30 AVD Macrobenchmark run was rejected by AndroidX with `EMULATOR`; record TTID/TTFD, first frame, PSS, frame time, temperature, and battery only from a valid physical-device run. Until real values exist, `performance_baseline.md` must remain `not-run`.

Passing the benchmark build does not prove that a profile was generated or that it improves startup. Host JVM/Gradle timings do not replace before/after measurements on the same Android device.

## UI, windows, and accessibility

The planning matrix covers 320/360/390/600/840dp, font/display scales 1.0/1.3/2.0, portrait/landscape, split/freeform windows, light/dark/high-contrast modes, touch, back gestures, TalkBack, and tablet keyboard/D-pad input. The code has adaptive-width and Compose/AndroidTest contracts, but no committed screenshot golden set or connected-device TalkBack/D-pad result.

Images under `docs/screenshots` are documentation or historical references; they do not automatically prove the current route, theme, fixture, or device layout.

## Media and system paths

See the [media fixture catalog](../media_fixture_catalog.md) for compatibility scope and privacy boundaries. The JVM suite covers a synthetic Motion Photo tail; HEIC/HEIF, RAW, OEM Motion Photo, long-tail video, and MediaStore permission behavior still require licensed samples and a device.

Deletion/restore/move, partial media permissions, system authorization cancellation/failure, WorkManager restart, and Media3/OEM decoding are device or system-UI paths. They cannot be inferred from JVM tests or the existence of a Release APK.

## Current evidence status

As of this snapshot, `lintDebug`, JVM unit tests, AndroidTest compilation, API 30/API 35/API 36 AVD AndroidTest execution, the app Release gate, and benchmark APK builds have explicit successful exit codes. The latest API 30 and API 36 runs were both 30/30 and include the MediaStore URI contract, WorkManager process-recovery/constraint checks, adaptive rendering, and navigation accessibility checks. API 30/API 36 startup and permission-state observations are also recorded. Final device-matrix acceptance remains pending because OEM/system-authorization failure paths, accessibility/window matrices, Media3, screenshot goldens, and real performance data are not closed.
