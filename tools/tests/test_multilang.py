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
        self.assertIn("add_library(zvx_native SHARED", cmake)
        self.assertIn("externalNativeBuild", gradle)
        self.assertIn("nativeClampSeekPosition", java)
        self.assertIn("NativeMediaBridge.clampSeekPosition", kotlin)
        self.assertIn("onDoubleTap", kotlin)
        self.assertIn("+10 seconds", kotlin)


if __name__ == "__main__":
    unittest.main()
