# Keepix Technical Overview

This document is the English companion to the Chinese technical overview. It captures the product scope, architecture notes, and maintenance boundaries that are too detailed for the README.

## Product Positioning

Keepix is a local-first Android workspace for cleaning up photos and videos. It is not a cloud album, social feed, or AI retouching app.

## Product Goals

### Fast cleanup

Users should be able to decide quickly whether a photo or video should be kept, favorited, deleted, or skipped.

### Safe deletion

Cleanup apps should protect users from accidental loss. The intended flow is:

```text
media file
-> user decision
-> app trash
-> second confirmation
-> restore or permanent delete
```

Permanent deletion of public media should always go through Android's system authorization flow.

### Local first

- No forced login.
- No cloud sync.
- No photo or video uploads.
- No server dependency for the core cleanup workflow.
- No membership, VIP, payment, ads, or remote authorization checks.

### Native Android first

Prefer Android platform capabilities over custom replacements:

- MediaStore for photo and video scanning.
- Storage Access Framework for folder selection.
- System intents for opening specific media.
- Predictive back.
- Material 3 and Jetpack Compose.
- Room and DataStore.

## Current Release State

Version `2.3.0` continues the cleanup-workspace direction:

- The home screen uses a modern white and light-blue visual style, supports horizontal switching between photo and video cleanup, and restores the recent media entry points.
- Photo cleanup uses edge-triggered action feedback, a compact progress pill, fullscreen year and month filters, and a fullscreen viewer.
- Video cleanup uses staged keep behavior, a draggable tool strip, and fullscreen time filters with enter and exit animation.
- Trash is split into separate photo and video views.
- Settings are grouped by common settings, media scope, and support or maintenance.
- Release builds keep R8 shrinking and resource shrinking enabled, with a signed APK size of about 6.2 MiB.

## Home Screen

The home screen is the app's main entry point. It should stay light, modern, and quick to navigate.

- Horizontal switching between photo cleanup and video cleanup.
- Recent photo and video entry points.
- Favorites and trash entry points.
- Today in History.
- Today cleanup summary.
- Media count overview.
- Estimated space that can be freed.
- No decorative geometry that exists only for visual noise.

Design expectations:

- Main buttons should feel solid and easy to tap.
- Cards should stay clear and readable.
- Dark mode should avoid harsh white flashes or odd gradients.
- Phone layouts should stay compact.
- Tablet layouts should reflow by width instead of stretching the phone UI.

## Photo Cleanup

Photo cleanup is one of the core flows.

- Load local photos.
- Build a swipe cleanup deck.
- Preview images.
- Keep, skip, delete, favorite, and undo.
- Filter by year or month.
- Exclude folders.
- Move items to a target folder when needed.
- Open a larger preview.
- Play GIFs and preview motion or live photos.

Behavior notes:

- The current card may lazily load motion or live-photo metadata to keep first entry fast.
- The bottom bar should keep media information visible instead of repeating it on the image itself.
- Edge actions should appear only when the card reaches the corresponding screen edge.

## Video Cleanup

Video cleanup should feel like an immersive playback flow, not a plain list.

- Load local videos.
- Create or reuse an ExoPlayer instance.
- Play or preview the current item.
- Move to the next video with staged keep behavior.
- Let the user go back and re-evaluate a previous item.
- Commit any still-staged items as kept when leaving the cleanup flow.
- Filter by year or month.
- Exclude folders.
- Move items to a target folder when needed.
- Open the system video viewer when appropriate.

Behavior notes:

- Navigating through videos should not immediately and permanently rewrite media state.
- True deletion must still go through the app trash or Android authorization flow.
- The tool strip should stay draggable as one unit so it does not cover the bottom of the screen.

## Settings

Settings are organized by user intent:

```text
Settings
├── Common Settings
├── Media Scope
└── Support and Maintenance
```

The goal is to keep only the settings that actually affect behavior. Empty or decorative entries should not exist.

## Interaction Notes

- Respect predictive back.
- Keep transition animations consistent between screens.
- Use fullscreen enter and exit animation for time filters.
- Keep layout density readable on phones and tablets.
- Keep the app local-first at every layer.

