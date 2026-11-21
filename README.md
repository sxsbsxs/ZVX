# ZVX

A clean, modern Android media player with a modular multi-language foundation.

## Included in this foundation

- Browse local video and audio collections through MediaStore, with search, Video / Audio / Recent filters, folder filtering and sorting by newest, name, duration or size.
- Show MediaStore video thumbnails and embedded audio album artwork where available.
- Request media permissions only when the user chooses to build the library; Android 14+ selected-video access is supported.
- Remember the last 50 played local items and resume playback position on-device.
- Open individual local files through Android's system document picker (Storage Access Framework).
- Play direct HTTP and HTTPS video URLs.
- HLS and MPEG-DASH streaming through Media3 modules.
- Progressive playback for common media containers when a device has a compatible decoder.
- Play, pause, seek, skip ±10 seconds with buttons or double-tap left/right on the video, and change playback speed (0.75×–2×).
- Switch between supported embedded audio tracks and subtitle tracks.
- Disable embedded subtitles or load an external SRT, VTT, TTML, SSA or ASS subtitle file.
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
