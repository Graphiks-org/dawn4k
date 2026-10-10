import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


MODULE_PATH = Path(__file__).with_name("demo-mobile-environment.py")
SPEC = importlib.util.spec_from_file_location("demo_mobile_environment", MODULE_PATH)
environment = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(environment)


class MobileEnvironmentTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)

    def image(self, abi="arm64-v8a", tag="google_apis", properties_abi=None):
        image = self.root / "sdk/system-images/android-35" / tag / abi
        image.mkdir(parents=True)
        (image / "system.img").write_bytes(b"fixture")
        (image / "source.properties").write_text(
            f"AndroidVersion.ApiLevel=35\nSystemImage.Abi={properties_abi or abi}\n")
        return image

    def test_unavailable_ios_device_is_not_a_qualification_candidate(self):
        devices = {"devices": {"ios-test": [{"udid": "A", "isAvailable": False, "name": "Phone"}]}}
        with self.assertRaisesRegex(ValueError, "unavailable"):
            environment.select_ios_device(devices, "ios-test")

    def test_ambiguous_ios_selection_requires_explicit_udid(self):
        devices = {"devices": {"ios-test": [
            {"udid": "A", "isAvailable": True, "name": "Phone"},
            {"udid": "B", "isAvailable": True, "name": "Phone"}]}}
        with self.assertRaisesRegex(ValueError, "explicit.*UDID"):
            environment.select_ios_device(devices, "ios-test")
        self.assertEqual("B", environment.select_ios_device(devices, "ios-test", "B"))

    def test_explicit_udid_from_wrong_runtime_is_rejected(self):
        devices = {"devices": {"other": [{"udid": "A", "isAvailable": True}]}}
        with self.assertRaisesRegex(ValueError, "unavailable"):
            environment.select_ios_device(devices, "ios-test", "A")

    def test_arm64_image_cannot_satisfy_x86_64_requirement(self):
        self.image()
        with self.assertRaisesRegex(ValueError, "x86_64"):
            environment.select_android_image(self.root / "sdk", 35, "x86_64")

    def test_directory_name_cannot_override_actual_image_abi(self):
        self.image(properties_abi="x86_64")
        with self.assertRaisesRegex(ValueError, "ABI"):
            environment.select_android_image(self.root / "sdk", 35, "arm64-v8a")

    def test_android_image_must_include_system_image(self):
        image = self.image()
        (image / "system.img").unlink()
        with self.assertRaisesRegex(ValueError, "system.img"):
            environment.select_android_image(self.root / "sdk", 35, "arm64-v8a")

    def test_matching_android_image_is_selected_without_modification(self):
        image = self.image()
        before = {p.name: p.read_bytes() for p in image.iterdir()}
        self.assertEqual(image, environment.select_android_image(self.root / "sdk", 35, "arm64-v8a"))
        self.assertEqual(before, {p.name: p.read_bytes() for p in image.iterdir()})

    def test_multiple_android_tags_require_explicit_tag(self):
        self.image()
        selected = self.image(tag="google_apis_playstore")
        with self.assertRaisesRegex(ValueError, "explicit.*tag"):
            environment.select_android_image(self.root / "sdk", 35, "arm64-v8a")
        self.assertEqual(selected, environment.select_android_image(
            self.root / "sdk", 35, "arm64-v8a", tag="google_apis_playstore"))

    def test_missing_emulator_produces_diagnostic_before_probing(self):
        with self.assertRaisesRegex(ValueError, "emulator"):
            environment.sdk_binary(self.root / "sdk", "emulator/emulator")

    def test_sdk_binary_must_be_executable(self):
        binary = self.root / "sdk/emulator/emulator"
        binary.parent.mkdir(parents=True)
        binary.write_text("#!/bin/sh\nexit 0\n")
        with self.assertRaisesRegex(ValueError, "executable"):
            environment.sdk_binary(self.root / "sdk", "emulator/emulator")
        binary.chmod(0o755)
        self.assertEqual(binary, environment.sdk_binary(self.root / "sdk", "emulator/emulator"))

    def test_arm64_compiler_cannot_satisfy_x86_64_inventory(self):
        binary = self.root / "sdk/ndk/test/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android24-clang"
        binary.parent.mkdir(parents=True)
        binary.write_text("#!/bin/sh\nexit 0\n")
        binary.chmod(0o755)
        with self.assertRaisesRegex(ValueError, "x86_64"):
            environment.ndk_compiler(self.root / "sdk", "test", "x86_64")

    def test_x86_64_inventory_selects_its_actual_ndk_compiler(self):
        binary = self.root / "sdk/ndk/test/toolchains/llvm/prebuilt/darwin-x86_64/bin/x86_64-linux-android24-clang"
        binary.parent.mkdir(parents=True)
        binary.write_text("#!/bin/sh\nexit 0\n")
        binary.chmod(0o755)
        self.assertEqual(binary, environment.ndk_compiler(self.root / "sdk", "test", "x86_64"))

    def test_cli_invalid_sdk_does_not_modify_user_avd_files(self):
        home = self.root / "home"
        avd = home / ".android/avd/user.avd/config.ini"
        avd.parent.mkdir(parents=True)
        avd.write_bytes(b"hw.ramSize=4096\n")
        report = self.root / "report.json"
        result = subprocess.run([sys.executable, str(MODULE_PATH), "--sdk", str(self.root / "absent"),
            "--home", str(home), "--offline", "--ios-runtime", "unavailable-fixture",
            "--report", str(report)], text=True, capture_output=True, timeout=20)
        self.assertEqual(1, result.returncode, result.stdout + result.stderr)
        self.assertEqual(b"hw.ramSize=4096\n", avd.read_bytes())
        self.assertEqual([avd], list(home.rglob("config.ini")))
        self.assertFalse(json.loads(report.read_text())["prerequisitesAvailable"])

    def test_bounded_subprocess_reports_exit_failure(self):
        with self.assertRaisesRegex(ValueError, "exit.*7"):
            environment.run_text([sys.executable, "-c", "raise SystemExit(7)"], timeout=5)

    def test_timeout_cannot_hang_environment_inventory(self):
        with self.assertRaisesRegex(ValueError, "timeout"):
            environment.run_text([sys.executable, "-c", "import time;time.sleep(10)"], timeout=0.05)

    def test_publication_mismatch_cannot_be_reported_as_consistent_dependency(self):
        root = '<metadata><versioning><snapshot><timestamp>20261006.180513</timestamp></snapshot></versioning></metadata>'
        platform = '<metadata><versioning><snapshot><timestamp>20261007.180513</timestamp></snapshot></versioning></metadata>'
        with self.assertRaisesRegex(ValueError, "publication"):
            environment.require_same_publication({"suite-demos": root, "suite-demos-android": platform})


if __name__ == "__main__":
    unittest.main()
