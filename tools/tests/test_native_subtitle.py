import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NATIVE_DIR = ROOT / "app/src/main/cpp"
HARNESS = ROOT / "tools/tests/native_subtitle_harness.cpp"
COMPILER = shutil.which("g++") or shutil.which("clang++")


@unittest.skipUnless(COMPILER, "A C++17 compiler is required for native regression tests")
class NativeSubtitleTimelineTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp_dir = tempfile.TemporaryDirectory(prefix="zvx-native-tests-")
        cls.binary = Path(cls.temp_dir.name) / "subtitle_timeline_test"
        subprocess.run(
            [
                COMPILER, "-std=c++17", "-Wall", "-Wextra", "-Werror",
                "-I", str(NATIVE_DIR),
                str(NATIVE_DIR / "subtitle_timeline.cpp"),
                str(HARNESS),
                "-o", str(cls.binary),
            ],
            check=True, capture_output=True, text=True,
        )

    @classmethod
    def tearDownClass(cls):
        cls.temp_dir.cleanup()

    def shift(self, source, offset_ms):
        result = subprocess.run(
            [str(self.binary), str(offset_ms)],
            input=source.encode("utf-8"),
            check=True,
            capture_output=True,
        )
        return result.stdout.decode("utf-8")

    def test_subrip_positive_delay_preserves_crlf(self):
        source = "1\r\n00:00:01,100 --> 00:00:02,600\r\nCaption\r\n"
        expected = "1\r\n00:00:02,350 --> 00:00:03,850\r\nCaption\r\n"
        self.assertEqual(expected, self.shift(source, 1250))

    def test_webvtt_negative_delay_clamps_at_zero_and_keeps_settings(self):
        source = "WEBVTT\n\n00:00:00.500 --> 00:00:01.250 align:start position:50%\nText\n"
        expected = "WEBVTT\n\n00:00:00.000 --> 00:00:00.350 align:start position:50%\nText\n"
        self.assertEqual(expected, self.shift(source, -900))

    def test_hours_and_comma_decimal_are_supported(self):
        source = "01:02:03,004 --> 01:02:04,004\n"
        expected = "01:02:04,004 --> 01:02:05,004\n"
        self.assertEqual(expected, self.shift(source, 1000))

    def test_malformed_non_cue_lines_are_untouched(self):
        source = "NOTE 00:00:01.000 --> not a cue\nnot a timestamp\n"
        self.assertEqual(source, self.shift(source, 500))

    def test_two_field_clock_is_supported(self):
        source = "10:01.500 --> 10:02.000\n"
        expected = "10:02.000 --> 10:02.500\n"
        self.assertEqual(expected, self.shift(source, 500))

    def test_zero_offset_is_exact_no_op(self):
        source = "WEBVTT\r\n\r\n00:00:01.000 --> 00:00:02.000\r\n"
        self.assertEqual(source, self.shift(source, 0))
