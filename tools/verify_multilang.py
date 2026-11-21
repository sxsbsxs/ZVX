#!/usr/bin/env python3
"""Repository architecture checks for the ZVX multi-language Android app."""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def required_files():
    return [
        "app/src/main/java/com/zvx/player/MainActivity.kt",
        "app/src/main/java/com/zvx/player/platform/NativeMediaBridge.java",
        "app/src/main/cpp/CMakeLists.txt",
        "app/src/main/cpp/zvx_native.cpp",
        "app/src/main/java/com/zvx/player/ui/PlayerScreen.kt",
        "app/src/main/java/com/zvx/player/PlaybackService.kt",
        "app/build.gradle.kts",
        "app/src/main/java/com/zvx/player/data/LibraryPreferencesStore.kt",
    ]


def main():
    missing = [path for path in required_files() if not (ROOT / path).is_file()]
    if missing:
        print("Missing required multi-language files:")
        print("\n".join(f" - {item}" for item in missing))
        return 1

    cmake = (ROOT / "app/src/main/cpp/CMakeLists.txt").read_text()
    gradle = (ROOT / "app/build.gradle.kts").read_text()
    java = (ROOT / "app/src/main/java/com/zvx/player/platform/NativeMediaBridge.java").read_text()
    cpp = (ROOT / "app/src/main/cpp/zvx_native.cpp").read_text()
    kotlin = (ROOT / "app/src/main/java/com/zvx/player/ui/PlayerScreen.kt").read_text()
    manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
    service = (ROOT / "app/src/main/java/com/zvx/player/PlaybackService.kt").read_text()
    checks = {
        "CMake builds the native library": "add_library(zvx_native SHARED" in cmake,
        "Gradle configures CMake and the NDK": 'path = file("src/main/cpp/CMakeLists.txt")' in gradle and "abiFilters" in gradle,
        "Java bridge loads the native library": 'System.loadLibrary("zvx_native")' in java,
        "JNI exports both Java bridge methods": "nativeClampSeekPosition" in cpp and "nativeEngineVersion" in cpp,
        "Kotlin player calls the Java/C++ seek bridge": "NativeMediaBridge.clampSeekPosition" in kotlin,
        "Player supports double-tap seeking": "onDoubleTap" in kotlin and "−10 seconds" in kotlin and "+10 seconds" in kotlin,
        "Player supports vertical brightness and volume gestures": "detectDragGestures" in kotlin and "screenBrightness" in kotlin and "setStreamVolume" in kotlin,
        "Player restores app brightness after exit": "originalScreenBrightness" in kotlin and "attributes.screenBrightness = originalScreenBrightness" in kotlin,
        "Media session service is registered": "MediaSessionService" in manifest and "foregroundServiceType=\"mediaPlayback\"" in manifest and "MediaSession.Builder" in service,
        "Player UI connects through MediaController": "MediaController.Builder" in kotlin and "SessionToken" in kotlin and "releaseFuture(controllerFuture)" in kotlin,
        "Video player exposes Android PiP": "enterPictureInPictureMode" in kotlin and "PictureInPictureParams.Builder" in kotlin and 'android:supportsPictureInPicture="true"' in manifest,
        "Video player supports immersive landscape mode": "SCREEN_ORIENTATION_LANDSCAPE" in kotlin and "WindowInsetsCompat.Type.systemBars()" in kotlin and "BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE" in kotlin,
        "Library hands a same-type playlist to the player": "onPlayMedia: (String, Boolean, List<MediaEntry>) -> Unit" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text() and "setMediaItems(playlist, selectedIndex, startPosition)" in kotlin,
        "Player supports queue browsing, previous/next, shuffle and repeat": "Playback queue" in kotlin and "seekToPreviousMediaItem" in kotlin and "seekToNextMediaItem" in kotlin and "shuffleModeEnabled" in kotlin and "REPEAT_MODE_ALL" in kotlin,
        "Player exposes display sizing and sleep timer options": "RESIZE_MODE_FILL" in kotlin and "RESIZE_MODE_ZOOM" in kotlin and "sleepTimerMinutes" in kotlin and "Sleep timer finished" in kotlin,
        "Subtitle style controls update the subtitle renderer": "setFixedTextSize" in kotlin and "CaptionStyleCompat" in kotlin and "subtitleBackgroundEnabled" in kotlin,
        "Playback settings persist locally": "PlaybackSettingsStore.load" in kotlin and "PlaybackSettingsStore.save" in kotlin and (ROOT / "app/src/main/java/com/zvx/player/data/PlaybackSettingsStore.kt").is_file(),
        "Player supports horizontal swipe-to-seek": "gestureMode == 1" in kotlin and "horizontalSeekDelta" in kotlin and "Seek to " in kotlin,
        "Queue can be reordered, edited and cleared": "moveMediaItem(index, index - 1)" in kotlin and "removeMediaItem(index)" in kotlin and "Clear queue" in kotlin,
        "Media library supports persistent favorites": "MediaFavoritesStore.toggle" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text() and "FAVORITES" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text(),
        "Home screen can import M3U playlists": "parseM3uPlaylist(content, selectedUri)" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text() and "Import M3U playlist" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text(),
        "M3U supports relative URL and document paths": "URI(playlistUri.toString()).resolve(path)" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text() and "resolveDocumentRelativeUri" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text() and "DocumentsContract.buildDocumentUri" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text(),
        "Settings can clear history and favorites": "PlaybackHistoryStore.clear(context)" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text() and "MediaFavoritesStore.clear(context)" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text(),
        "Library filter and sorting preferences persist": "LibraryPreferencesStore.load(context)" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text() and "LibraryPreferencesStore.save(context" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text(),
        "Dedicated playback history allows individual removal": "PlaybackHistoryScreen(" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text() and "PlaybackHistoryStore.remove(context, uri)" in (ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text() and "fun remove(context: Context, uri: String)" in (ROOT / "app/src/main/java/com/zvx/player/data/PlaybackHistoryStore.kt").read_text(),
    }
    for label, passed in checks.items():
        print(f"[{'OK' if passed else 'FAIL'}] {label}")
    return 0 if all(checks.values()) else 1


if __name__ == "__main__":
    sys.exit(main())
