# Keepix Privacy Notes

This document is the English companion to the Chinese privacy notes. The short version: Keepix reads only what the user allows, keeps cleanup state on the device, and does not send photos or videos to a server.

## Data Processed

Keepix may read or store the following data locally:

- MediaStore records for photos and videos, such as URI, file name, size, timestamp, dimensions, duration, and folder path.
- Cleanup state such as keep, favorite, trash, restore, and folder exclusions.
- Local statistics such as cleanup counts, estimated space to free, and achievement progress.
- Local settings such as theme, gestures, vibration, and default cleanup behavior.
- Cached fingerprints and grouping results for similar-photo detection.
- Motion-photo detection results, including local metadata clues or same-name companion video references.

These records are stored mainly in the device's Room database and DataStore, not in a remote account.

## Permissions

- `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO`: read user-authorized photo and video lists.
- `READ_MEDIA_VISUAL_USER_SELECTED`: support partial media access on Android 14 and later.
- `READ_EXTERNAL_STORAGE`: compatibility for Android 12 and earlier.
- `ACCESS_MEDIA_LOCATION`: read location metadata when the user allows it.
- Media write or delete permissions: only when the user moves, restores, sends to system trash, or permanently deletes media.
- `VIBRATE`: feedback for cleanup actions, navigation, and bottom dock switching.

## What It Does Not Do

- No forced login.
- No photo or video uploads.
- No cloud sync.
- No ad tracking.
- No membership, VIP, payment, subscription, or remote authorization checks.
- No bypass of Android's confirmation flow for permanent deletion.

## Deletion and Recovery

Items marked for deletion should go through the app trash or the system trash flow first. Permanent deletion triggers Android's system confirmation and may make recovery impossible through Keepix afterward.

The estimated "space to free" shown on the home screen, cleanup screens, and trash screen comes from file-size estimates for items marked for deletion. Actual freed space depends on the final storage state after the system completes deletion.

## Development and Release Notes

- Do not commit real media, signing keys, passwords, `local.properties`, or private local folders.
- Use your own release keystore for publishing APKs.
- Keep privacy boundaries visible in release notes and store listings.

