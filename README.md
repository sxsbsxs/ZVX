# ZVX

A clean, modern Android video player built with Kotlin, Jetpack Compose and AndroidX Media3.

## Included in this foundation

- Open local videos through Android's system document picker (Storage Access Framework).
- Play direct HTTP and HTTPS video URLs.
- HLS and MPEG-DASH streaming through Media3 modules.
- Progressive playback for common media containers when a device has a compatible decoder.
- Play, pause, seek, skip ±10 seconds and change playback speed (0.75×–2×).
- Embedded subtitle and audio tracks exposed by Media3.
- An original dark ZVX interface.
- GitHub Actions workflow that builds and uploads a debug APK.

Codec and container support depends on the Android device and source stream. A file extension alone cannot guarantee that a device can decode a file.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- AndroidX Media3 / ExoPlayer 1.11.1
- Gradle Kotlin DSL
- GitHub Actions

## Build

Open the repository in Android Studio, use JDK 17 and install Android SDK Platform 36. Allow Gradle to sync, then run the app configuration on a device or emulator.

With Gradle 8.13 installed, run `gradle assembleDebug` . The APK is written to app/build/outputs/apk/debug/app-debug.apk.

## Network security

ZVX accepts HTTP and HTTPS media URLs for compatibility. HTTP is unencrypted; prefer HTTPS and only open streams from trusted sources.

## License

MIT. See LICENSE.
