import sys
import unittest
from pathlib import Path

TOOLS_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS_DIR))
import verify_multilang


class MultiLanguageLayoutTest(unittest.TestCase):
    def test_required_files_exist(self):
        missing = [item for item in verify_multilang.required_files()
                   if not (verify_multilang.ROOT / item).is_file()]
        self.assertEqual([], missing)

    def test_native_sources_are_wired(self):
        cmake = (verify_multilang.ROOT / "app/src/main/cpp/CMakeLists.txt").read_text()
        gradle = (verify_multilang.ROOT / "app/build.gradle.kts").read_text()
        java = (verify_multilang.ROOT / "app/src/main/java/com/zvx/player/platform/NativeMediaBridge.java").read_text()
        kotlin = (verify_multilang.ROOT / "app/src/main/java/com/zvx/player/ui/PlayerScreen.kt").read_text()
        service = (verify_multilang.ROOT / "app/src/main/java/com/zvx/player/PlaybackService.kt").read_text()
        manifest = (verify_multilang.ROOT / "app/src/main/AndroidManifest.xml").read_text()
        self.assertIn("add_library(zvx_native SHARED", cmake)
        self.assertIn("externalNativeBuild", gradle)
        self.assertIn("nativeClampSeekPosition", java)
        self.assertIn("NativeMediaBridge.clampSeekPosition", kotlin)
        self.assertIn("onDoubleTap", kotlin)
        self.assertIn("+10 seconds", kotlin)
        self.assertIn("detectDragGestures", kotlin)
        self.assertIn("setStreamVolume", kotlin)
        self.assertIn("attributes.screenBrightness = originalScreenBrightness", kotlin)
        self.assertIn("MediaSession.Builder", service)
        self.assertIn("MediaController.Builder", kotlin)
        self.assertIn("SessionToken", kotlin)
        self.assertIn("enterPictureInPictureMode", kotlin)
        self.assertIn("SCREEN_ORIENTATION_LANDSCAPE", kotlin)
        self.assertIn("BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE", kotlin)
        self.assertIn("WindowInsetsCompat.Type.systemBars()", kotlin)
        self.assertIn("setMediaItems(playlist, selectedIndex, startPosition)", kotlin)
        self.assertIn("seekToPreviousMediaItem", kotlin)
        self.assertIn("seekToNextMediaItem", kotlin)
        self.assertIn("shuffleModeEnabled", kotlin)
        self.assertIn("Playback queue", kotlin)
        self.assertIn("RESIZE_MODE_FILL", kotlin)
        self.assertIn("RESIZE_MODE_ZOOM", kotlin)
        self.assertIn("sleepTimerMinutes", kotlin)
        self.assertIn("Sleep timer finished", kotlin)
        self.assertIn("setFixedTextSize", kotlin)
        self.assertIn("CaptionStyleCompat", kotlin)
        self.assertIn("subtitleBackgroundEnabled", kotlin)
        self.assertIn("horizontalSeekDelta", kotlin)
        self.assertIn("Seek to ", kotlin)
        self.assertIn("moveMediaItem(index, index - 1)", kotlin)
        self.assertIn("removeMediaItem(index)", kotlin)
        self.assertIn("Clear queue", kotlin)
        home = (verify_multilang.ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text()
        self.assertIn("MediaFavoritesStore.toggle", home)
        self.assertIn("FAVORITES", home)
        self.assertIn("parseM3uPlaylist(content)", home)
        self.assertIn("Import M3U playlist", home)
        home = (verify_multilang.ROOT / "app/src/main/java/com/zvx/player/ui/HomeScreen.kt").read_text()
        self.assertIn("List<MediaEntry>", home)
        self.assertIn("PictureInPictureParams.Builder", kotlin)
        self.assertIn('android:supportsPictureInPicture="true"', manifest)
        self.assertIn("foregroundServiceType=\"mediaPlayback\"", manifest)


if __name__ == "__main__":
    unittest.main()
