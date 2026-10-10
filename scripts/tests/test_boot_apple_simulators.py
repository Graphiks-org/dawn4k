import importlib.util
from pathlib import Path
import unittest


spec = importlib.util.spec_from_file_location(
    "boot_apple_simulators", Path(__file__).parents[1] / "boot-apple-simulators.py"
)
boot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(boot)


class SelectDeviceTest(unittest.TestCase):
    def test_selects_latest_available_runtime_for_each_platform(self):
        devices = {
            "com.apple.CoreSimulator.SimRuntime.iOS-9-0": [self.device("old")],
            "com.apple.CoreSimulator.SimRuntime.iOS-27-0": [self.device("ios")],
            "com.apple.CoreSimulator.SimRuntime.tvOS-27-0": [self.device("tv")],
        }
        self.assertEqual("ios", boot.select_device(devices, "iOS")["udid"])
        self.assertEqual("tv", boot.select_device(devices, "tvOS")["udid"])

    def test_reuses_booted_available_device_before_starting_another(self):
        devices = {
            "com.apple.CoreSimulator.SimRuntime.iOS-26-0": [self.device("running", "Booted")],
            "com.apple.CoreSimulator.SimRuntime.iOS-27-0": [self.device("shutdown")],
        }
        self.assertEqual("running", boot.select_device(devices, "iOS")["udid"])

    def test_unavailable_device_is_never_selected(self):
        devices = {
            "com.apple.CoreSimulator.SimRuntime.iOS-27-0": [
                self.device("broken", "Booted", False), self.device("working")
            ]
        }
        self.assertEqual("working", boot.select_device(devices, "iOS")["udid"])

    def test_missing_platform_fails_explicitly(self):
        with self.assertRaisesRegex(RuntimeError, "No available tvOS simulator"):
            boot.select_device({
                "com.apple.CoreSimulator.SimRuntime.iOS-27-0": [self.device("ios")]
            }, "tvOS")

    @staticmethod
    def device(udid, state="Shutdown", available=True):
        return {"udid": udid, "name": udid, "state": state, "isAvailable": available}


if __name__ == "__main__":
    unittest.main()
