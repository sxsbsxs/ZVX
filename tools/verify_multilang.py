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
    }
    for label, passed in checks.items():
        print(f"[{'OK' if passed else 'FAIL'}] {label}")
    return 0 if all(checks.values()) else 1


if __name__ == "__main__":
    sys.exit(main())
