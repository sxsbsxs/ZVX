import math
import shutil
import struct
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NATIVE_DIR = ROOT / "app/src/main/cpp"
HARNESS = ROOT / "tools/tests/native_audio_dsp_harness.cpp"
COMPILER = shutil.which("g++") or shutil.which("clang++")


@unittest.skipUnless(COMPILER, "A C++17 compiler is required for native audio DSP tests")
class NativeAudioDspTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp_dir = tempfile.TemporaryDirectory(prefix="zvx-audio-dsp-")
        cls.binary = Path(cls.temp_dir.name) / "audio_dsp_test"
        subprocess.run(
            [
                COMPILER, "-std=c++17", "-O2", "-Wall", "-Wextra", "-Werror",
                "-I", str(NATIVE_DIR),
                str(NATIVE_DIR / "audio_dsp.cpp"),
                str(HARNESS),
                "-o", str(cls.binary),
            ],
            check=True, capture_output=True, text=True,
        )

    @classmethod
    def tearDownClass(cls):
        cls.temp_dir.cleanup()

    def process(self, samples, profile, sample_rate=48_000, channels=2):
        packed = struct.pack("<" + "h" * len(samples), *samples)
        result = subprocess.run(
            [str(self.binary), str(sample_rate), str(channels), str(profile)],
            input=packed, check=True, capture_output=True,
        )
        return list(struct.unpack("<" + "h" * (len(result.stdout) // 2), result.stdout))

    @staticmethod
    def stereo_signal(frame_count=12_000, sample_rate=48_000):
        samples = []
        for frame in range(frame_count):
            time_s = frame / sample_rate
            left = (0.42 * math.sin(2 * math.pi * 110 * time_s)
                    + 0.28 * math.sin(2 * math.pi * 1_000 * time_s)
                    + 0.18 * math.sin(2 * math.pi * 7_000 * time_s))
            right = (0.36 * math.sin(2 * math.pi * 180 * time_s)
                     + 0.24 * math.sin(2 * math.pi * 1_600 * time_s)
                     + 0.20 * math.sin(2 * math.pi * 6_000 * time_s))
            samples.extend((round(left * 24_000), round(right * 24_000)))
        return samples

    def test_flat_profile_is_bit_exact_passthrough(self):
        source = self.stereo_signal(frame_count=2_000)
        self.assertEqual(source, self.process(source, 0))

    def test_warm_vocal_and_bright_profiles_change_pcm_differently(self):
        source = self.stereo_signal()
        outputs = [self.process(source, profile) for profile in (1, 2, 3)]
        self.assertTrue(all(output != source for output in outputs))
        self.assertEqual(3, len({tuple(output) for output in outputs}))

    def test_output_stays_inside_pcm16_range_on_full_scale_input(self):
        source = [32767 if index % 2 == 0 else -32768 for index in range(8192)]
        for profile in (1, 2, 3):
            output = self.process(source, profile, channels=2)
            self.assertEqual(len(source), len(output))
            self.assertTrue(all(-32768 <= sample <= 32767 for sample in output))

    def test_partial_interleaved_frame_is_rejected(self):
        result = subprocess.run(
            [str(self.binary), "48000", "2", "1"],
            input=struct.pack("<h", 100), capture_output=True,
        )
        self.assertEqual(2, result.returncode)
