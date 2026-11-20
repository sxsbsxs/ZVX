#!/usr/bin/env python3
"""Lightweight repository checks for the ZVX multi-language layout.

Python is a developer/CI tool here; it is not embedded in the Android APK.
Run: python3 -m unittest discover -s tools/tests -v
"""
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def required_files():
    return [
        "app/src/main/java/com/zvx/player/MainActivity.kt",
        "app/src/main/java/com/zvx/player/platform/NativeMediaBridge.java",
        "app/src/main/cpp/CMakeLists.txt",
        "app/src/main/cpp/zvx_native.cpp",
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
    checks = {
        "CMake builds the native library": "add_library(zvx_native SHARED" in cmake,
        "Gradle configures externalNativeBuild": 'path = file("src/main/cpp/CMakeLists.txt")' in gradle,
        "Java bridge loads the native library": 'System.loadLibrary("zvx_native")' in java,
        "JNI exports the Java bridge methods": "nativeClampSeekPosition" in cpp and "nativeEngineVersion" in cpp,
    }
    failed = [label for label, passed in checks.items() if not passed]
    for label, passed in checks.items():
        print(f"[{'OK' if passed else 'FAIL'}] {label}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
