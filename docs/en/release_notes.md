# 看了么 2.3.5

Version name: `2.3.5` · Version code: `61` · Android 11 or later

## Advanced photo and video filters

- Find photos by folder, type, resolution, size, format, orientation, and date, then browse the matching results. Photo previews support double-tap and pinch zoom, and long images use a tiled viewer.
- Filter videos by folder, resolution, size, duration, format, orientation, and estimated bitrate. Switch between list and grid views and move between matching videos in the player.
- Open results in another app, select a destination folder to move them, or send them to the app trash. Moving is subject to Android permissions and storage-volume restrictions; deletion first goes through the app trash.

## Random folder playback

- Select a local folder with the system picker and choose whether to include subfolders. Discovered videos play in a shuffled loop with previous and next controls.
- Reopen the current list after closing the player, or rescan to update and reshuffle it. Unplayable videos are skipped when possible, with a retry option if all items fail consecutively.

## Playback and cleanup experience

- Manually switch the player between portrait and landscape. Rotation preserves playback position, playback state, volume, and speed; closing the player restores the previous orientation setting.
- Double-tap to seek, drag to scrub, hold for temporary fast playback, and adjust player brightness and volume from the sides of the screen. Gesture instructions are available in the more menu.
- Filter pages show the number of active conditions, offer a clear-search button, and adapt grid columns to the available width. Cleanup, onboarding, and preview interactions have also been refined.

## Notes

- When upgrading from versions 2.2.8–2.3.4, existing favorites, trash entries, and cleanup records are migrated. Original legacy statistics are retained; details the old version never recorded cannot be reconstructed.
- If a much older database is unsupported or contains conflicting data that cannot be merged safely, the app may not open until the issue is resolved. The old database is not automatically erased; do not clear app data and contact the maintainer.
- Photo and video filters scan items authorized in the system media library. Random folder playback uses the system folder grant and does not require videos to be indexed by MediaStore.
- Some tablets may display landscape playback in a compatibility window. Split-screen or device policies can restrict rotation; the player provides feedback if the request is ignored.
- Photos, videos, and cleanup records are processed locally and are not uploaded to a server. Items sent to the app trash can be reviewed or restored; permanent deletion still requires system confirmation.
