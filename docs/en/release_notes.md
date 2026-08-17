# Keepix 2.3.4

Version name: `2.3.4`, version code: `60`

## What's included

- Local-first operation with no account, subscription, or cloud sync
- Continuous photo and video cleanup with keep, favorite, delete, and undo
- App trash before permanent deletion through Android's system confirmation
- Device-local favorites, similar photos, Today in History, statistics, and settings
- Adaptive layouts for phones and tablets

## 2026-08-18 update

- Photo/video swipe guide dialogs now use a dark glass style with localizable labels and button text.
- Onboarding now uses a fixed dark gradient theme with a compact, scrollable layout on short screens.
- The video organizer live preview in Settings now adapts its height to the container width and shows the dark action rail and bottom components.
- The video organizer's top progress pill can be hidden by settings; top/bottom gradient overlays are only drawn while the chrome is visible, reducing occlusion of the video.
- A release APK was signed locally with the historical release keystore (v2 signature, certificate SHA-256 `ade29388…dbee`); signing and metadata are recorded in `docs/发布候选验证记录_2026-08-14.md`.

## Privacy boundary

Photos, videos, cleanup records, and settings are processed and stored on the device. Nothing is uploaded to a server.