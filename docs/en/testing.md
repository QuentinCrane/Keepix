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

## Baseline Profile and performance

With a fixed device connected, follow [Baseline Profile status](../baseline_profile_status.md) and record three groups: no profile, partial compilation, and full compilation. Record TTID/TTFD, first frame, PSS, frame time, temperature, and battery. Until real values exist, `performance_baseline.md` must remain `not-run`.

Passing the benchmark build does not prove that a profile was generated or that it improves startup. Host JVM/Gradle timings do not replace before/after measurements on the same Android device.

## UI, windows, and accessibility

The planning matrix covers 320/360/390/600/840dp, font/display scales 1.0/1.3/2.0, portrait/landscape, split/freeform windows, light/dark/high-contrast modes, touch, back gestures, TalkBack, and tablet keyboard/D-pad input. The code has adaptive-width and Compose/AndroidTest contracts, but no committed screenshot golden set or connected-device TalkBack/D-pad result.

Images under `docs/screenshots` are documentation or historical references; they do not automatically prove the current route, theme, fixture, or device layout.

## Media and system paths

See the [media fixture catalog](../media_fixture_catalog.md) for compatibility scope and privacy boundaries. The JVM suite covers a synthetic Motion Photo tail; HEIC/HEIF, RAW, OEM Motion Photo, long-tail video, and MediaStore permission behavior still require licensed samples and a device.

Deletion/restore/move, partial media permissions, system authorization cancellation/failure, WorkManager restart, and Media3/OEM decoding are device or system-UI paths. They cannot be inferred from JVM tests or the existence of a Release APK.

## Current evidence status

As of this snapshot, `lintDebug`, JVM unit tests, AndroidTest compilation, the app Release gate, and benchmark APK builds have explicit successful exit codes. `adb devices` has no connected device and there is no API 30/API 36 AVD, so final device acceptance remains pending.
