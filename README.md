# ZVX

A clean, modern Android media player with a modular multi-language foundation.

## Included in this foundation

- Browse local video and audio collections through MediaStore, with search, Video / Audio / Recent / Favorites filters, folder filtering and sorting by newest, name, duration or size.
- Add or remove local media favorites; favorites persist on-device and can be cleared from the in-app settings.
- Import M3U playlists with absolute HTTP(S), content:// and file:// locations, plus relative URL/local paths resolved against the playlist location where the provider grants access to sibling files; mixed playlists are filtered to the first item's media type when opened.
- Persist library filter and sorting choices across app launches; manage recent history and favorites from the settings dialog.
- Open dedicated playback history and favorites screens, replay saved items, remove individual entries and clear each collection without deleting media files.
- Adjust external SubRip and WebVTT subtitle timing from −10 to +10 seconds in 250 ms steps; timing preference persists locally and cue timestamps are rewritten without changing the original subtitle file.
- Show MediaStore video thumbnails and embedded audio album artwork where available.
- Request media permissions only when the user chooses to build the library; Android 14+ selected-video access is supported.
- Remember the last 50 played local items and resume playback position on-device.
- Open individual local files through Android's system document picker (Storage Access Framework).
- Play direct HTTP and HTTPS video URLs.
- HLS and MPEG-DASH streaming through Media3 modules.
- Progressive playback for common media containers when a device has a compatible decoder.
- Play, pause, seek, skip ±10 seconds with buttons or double-tap left/right on the video, and change playback speed (0.75×–2×).
- On-screen gestures: horizontal swipe to preview and seek; drag vertically on the left to adjust brightness or the right to adjust media volume; app brightness is restored when leaving the player.
- Playlist playback from the currently filtered library, with an in-player queue editor, reorder up/down, remove items, clear upcoming items, previous / next controls, shuffle and repeat modes.
- Save the current playback queue and playing item on-device; restore the last queue from the library screen after relaunch.
- Switch the interface between English, Simplified Chinese and Japanese; language preference is stored locally.
- Audio-focused presentation with framed album artwork, artist, album and current track position.
- Video frame sizing modes: Fit, Fill and Crop; foreground playback sleep timer with 15 / 30 / 45 / 60 minute presets.
- Persist playback speed, aspect mode and subtitle appearance preferences on-device.
- Background audio playback through Android MediaSessionService with media notification controls; playlist navigation is exposed to system media controls.
- One-tap immersive landscape playback with transient system bars revealed by swipe; restores the previous orientation and system bars when leaving the player.
- Picture-in-picture mode for video on Android 8.0+.
- Switch between supported embedded audio tracks and subtitle tracks.
- Disable embedded subtitles or load an external SRT, VTT, TTML, SSA or ASS subtitle file; adjust subtitle text size and background visibility.
- Original dark ZVX interface and GitHub Actions debug APK build.

## Language responsibilities

| Language | Responsibility | Runtime |
|---|---|---|
| Kotlin | Android app, Compose UI, library and playback orchestration | Android |
| Java | Android platform interoperability bridge and native API boundary | Android |
| C++17 | Small JNI native core for low-level media utilities; designed to grow into optimized native processing | Android NDK |
| Python 3 | Repository validation and developer/CI tooling; not bundled into the APK | Development / CI |

The native C++ layer is intentionally small at this stage. Media decoding and playback remain on AndroidX Media3; adding C++ does not magically add codecs or replace the playback engine. Future native components should be introduced only where profiling or a concrete feature justifies them.

## Build

Use JDK 17, Android SDK Platform 36, Android NDK 27.2.12479018 and CMake 3.22.1. Open the repository in Android Studio and sync Gradle.

Build the Android app with:

`gradle assembleDebug`

Run the Python repository checks with:

`python3 -m unittest discover -s tools/tests -v`

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Network security

ZVX accepts HTTP and HTTPS media URLs for compatibility. HTTP is unencrypted; prefer HTTPS and only open streams from trusted sources.

## License

MIT. See LICENSE.
