import unittest
from pathlib import Path
import verify_multilang


class MultiLanguageLayoutTest(unittest.TestCase):
    def test_required_files_exist(self):
        missing = [item for item in verify_multilang.required_files()
                   if not (verify_multilang.ROOT / item).is_file()]
        self.assertEqual([], missing)

    def test_native_sources_are_wired(self):
        cmake = (verify_multilang.ROOT / "app/src/main/cpp/CMakeLists.txt").read_text()
        gradle = (verify_multilang.ROOT / "app/build.gradle.kts").read_text()
        self.assertIn("add_library(zvx_native SHARED", cmake)
        self.assertIn("externalNativeBuild", gradle)


if __name__ == "__main__":
    unittest.main()
