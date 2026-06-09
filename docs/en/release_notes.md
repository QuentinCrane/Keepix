# Keepix Release Notes Draft

This document is the English copy deck for GitHub Release publishing. Before publishing, make sure the `versionCode` and `versionName` in `app/build.gradle.kts` still match the tag you plan to release.

## GitHub Release Fields

### Tag

```text
v2.3.0
```

### Release Title

```text
Keepix 2.3.0 - Modern cleanup workspace with staged video keep
```

### Release Body

```markdown
## Highlights

- The home screen now uses a more modern white and light-blue cleanup workspace, supports horizontal switching between photo cleanup and video cleanup, and removes decorative elements that do not carry real information.
- Recent photo and video entry points are back on the home screen, reducing unnecessary clutter.
- Photo cleanup now uses stronger edge-action feedback, a compact progress pill, fullscreen year and month filters, and a clearer undo entry point.
- Video cleanup uses staged keep behavior: swiping forward temporarily keeps the previous item, swiping back lets you review it again, and exiting the flow commits the remaining staged items as kept.
- Video cleanup also uses a draggable tool strip with count, undo, sound, favorite, delete, and share actions.
- Photo and video trash are now separated, with preview actions tailored to the current media type.
- The large photo viewer now shows media info, thumbnails, and actions for favorite, move, delete, EXIF, and return to cleanup.
- Settings are reorganized around only the entries that actually do something, with more consistent typography and card density.
- Release builds continue to use R8 shrinking and resource shrinking, with a signed APK size of about 6.2 MiB.

## Privacy and Permissions

- All core cleanup flows stay on the device.
- No forced login, no photo or video uploads, and no accounts, memberships, payments, ads, subscriptions, cloud sync, or remote authorization checks.
- Permanent deletion of public media still uses Android's system authorization flow.
- Cleanup state, trash, favorites, statistics, and settings stay in Room and DataStore on the device.

## Install Notes

Install the signed APK on Android 11 or later. When the app launches for the first time, grant photo or video access as requested. On Android 14 and later, partial media access will only show the media the user allowed.

## Known Notes

- Different vendor galleries may vary in support for direct media opening, system trash, and folder moves.
- If folder moves hit write authorization limits, confirm the Android prompt when it appears.
- Similar-photo detection can take time on very large libraries, and results stay on the device.
```

### Attachments

Upload the final signed APK or a release zip:

```text
app/build/outputs/apk/release/app-release-signed.apk
app/build/outputs/release-package/kanleme-v2.3.0-release.zip
```

Do not upload:

- `app-release-unsigned.apk`
- `app-release-aligned.apk`
- `*.idsig`
- `local.properties`
- `local-private/`
- `*.keystore`, `*.jks`, `*.p12`, `*.pfx`
- Any intermediate build artifact outside APK or AAB outputs

## Pre-release Checklist

- [ ] `git status --short` does not show `local.properties`, `local-private/`, signing keys, APK, AAB, or `app/build/`
- [ ] `README.md`, `docs/en/build.md`, and `docs/en/release_notes.md` match the release wording
- [ ] `.\gradlew.bat :app:assembleDebug --console=plain` passes
- [ ] `.\gradlew.bat :app:assembleRelease --console=plain` passes
- [ ] The final APK has been `zipalign`ed
- [ ] The final APK has been signed with the release keystore
- [ ] `apksigner verify --verbose --print-certs app\build\outputs\apk\release\app-release-signed.apk` passes
- [ ] Real-device verification covers photo cleanup, video cleanup, trash, favorites, settings, media permissions, and permanent-delete confirmation

