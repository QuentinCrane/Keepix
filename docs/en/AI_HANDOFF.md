# Keepix AI Handoff Notes

This document is a quick handoff for the next maintainer or AI assistant. It only records information that is safe to keep public, and it excludes signing keys, passwords, private paths, and real media.

## Current Version

- App name: Keepix / 看了么
- Package name: `com.futureape.kanleme`
- Current version: `2.3.0`
- `versionCode`: `56`
- Minimum OS: Android 11, `minSdk = 30`
- Target OS: `targetSdk = 35`

## Current Functional State

- The home screen uses a modern white and light-blue style, with horizontal switching between photo cleanup and video cleanup, plus recent media entry points.
- The home screen only keeps elements that provide information or action value.
- Photo cleanup includes edge-action feedback, a compact progress pill, fullscreen year and month filters, an undo entry point, and a large viewer.
- Video cleanup uses staged keep behavior: swiping down confirms keep for the current item and advances, swiping back returns to the previous item for review, and leaving the flow commits remaining staged items as kept.
- The video tool strip is a draggable, single-piece control group with count, undo, sound, favorite, delete, and share actions.
- Trash is split into photo and video views, with preview actions that match the current scenario.
- Settings are organized around the entries that actually affect cleanup, display, media scope, haptics, appearance, or maintenance.

## Maintenance Priorities

1. Recheck Android 13+ `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` behavior, plus Android 14+ partial media access, on a real device.
2. Keep folder moves ready for `MediaStore.createWriteRequest()` if the target device requires user confirmation for media the app did not create.
3. Stress-test similar-photo detection on large libraries, especially fingerprint cache reuse, candidate bucket count, background progress, and memory usage.
4. Verify photo and video edge actions, time filter animation, fullscreen and ratio switching, and predictive back on a real device.
5. Before release, confirm that all public docs, release text, and screenshots still match the actual app behavior.

## Must Follow

- Do not introduce accounts, memberships, VIP features, payments, subscriptions, ads, cloud sync, or remote authorization checks.
- Do not upload user photos or videos to a server.
- Permanent deletion of public media must go through Android's system authorization flow.
- Media should go to the in-app trash first, then the user confirms deletion.
- Do not commit `local.properties`, `local-private/`, signing keys, APK or AAB outputs, `app/build/`, or real media.
- If Room entities, DAOs, or the database version changes, update `app/schemas/` too.

## Public Doc Entry Points

- `README.md`: English project overview, feature summary, quick start, and release summary.
- `README.zh-CN.md`: Chinese project overview and Chinese doc links.
- `docs/en/technical_overview.md`: English technical overview, product scope, and boundaries.
- `docs/technical_overview.md`: Chinese technical overview.
- `docs/en/build.md`: English build guide.
- `docs/build.md`: Chinese build guide.
- `docs/en/release_notes.md`: English release draft.
- `docs/release_notes.md`: Chinese release draft.
- `docs/en/privacy.md`: English privacy notes.
- `docs/privacy.md`: Chinese privacy notes.

## Build and Release

Common verification commands:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:assembleRelease --console=plain
```

Current local release artifact paths:

```text
app/build/outputs/apk/release/app-release-signed.apk
app/build/outputs/release-package/kanleme-v2.3.0-release.zip
```

These artifacts should be uploaded as GitHub Release attachments, not committed to Git.

