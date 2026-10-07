import importlib.util
import os
from pathlib import Path
import signal
import subprocess
import sys
import tempfile
import time
import unittest

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))


def load_script(name):
    path = HERE / name
    if not path.is_file():
        raise AssertionError(f"Missing desktop implementation: {path}")
    spec = importlib.util.spec_from_file_location(name.replace("-", "_"), path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class DesktopTest(unittest.TestCase):
    def test_wayland_desktop_does_not_require_display(self):
        desktop = load_script("start-desktop.py")
        result = desktop.validate_desktop_environment(
            {"WAYLAND_DISPLAY": "wayland-1", "XDG_RUNTIME_DIR": "/run/user/1000",
             "DISPLAY": ":9", "XAUTHORITY": "/stale", "WAYLAND_SOCKET": "7"}, "wayland")
        self.assertNotIn("DISPLAY", result)
        self.assertNotIn("XAUTHORITY", result)
        self.assertNotIn("WAYLAND_SOCKET", result)
        self.assertEqual("wayland-1", result["WAYLAND_DISPLAY"])

    def test_missing_or_unknown_backend_fails_instead_of_reusing_stale_environment(self):
        desktop = load_script("start-desktop.py")
        for backend, environment, diagnostic in (
            ("wayland", {"DISPLAY": ":0"}, "WAYLAND"),
            ("x11", {"WAYLAND_DISPLAY": "wayland-1"}, "DISPLAY"),
            ("unknown", {"DISPLAY": ":0"}, "backend"),
        ):
            with self.assertRaisesRegex(RuntimeError, diagnostic):
                desktop.validate_desktop_environment(environment, backend)

    def test_default_launcher_forces_x11_only_in_the_x11_desktop(self):
        launcher = load_script("launch-demo.py")
        self.assertEqual([":dawn4k-demo:run", "--args=--platform=x11"], launcher.default_tasks("x11"))
        self.assertEqual([":dawn4k-demo:run"], launcher.default_tasks("wayland"))

    def test_health_rejects_stale_compositor_even_if_http_could_be_available(self):
        health = load_script("check-desktop.py")
        with self.assertRaises(subprocess.CalledProcessError):
            with tempfile.TemporaryDirectory() as directory:
                executable = Path(directory) / "swaymsg"
                executable.write_text("#!/bin/sh\nexit 7\n")
                executable.chmod(0o755)
                health.check_services({"PATH": directory, "SWAYSOCK": "/stale",
                    "WAYLAND_DISPLAY": "wayland-1", "XDG_RUNTIME_DIR": directory}, "wayland")

    def test_display_comes_from_sway_not_a_guessed_number(self):
        desktop = load_script("start-desktop.py")
        self.assertEqual(":3", desktop.extract_display(["OTHER=x", "DISPLAY=:3"]))
        with self.assertRaisesRegex(RuntimeError, "DISPLAY"):
            desktop.extract_display(["WAYLAND_DISPLAY=wayland-1"])

    def test_failed_service_names_itself(self):
        desktop = load_script("start-desktop.py")
        with subprocess.Popen([sys.executable, "-c", "raise SystemExit(7)"]) as child:
            child.wait(timeout=5)
            with self.assertRaisesRegex(RuntimeError, "sway.*7"):
                desktop.require_services_alive({"sway": child})

    def test_readiness_has_a_deadline(self):
        desktop = load_script("start-desktop.py")
        started = time.monotonic()
        with self.assertRaisesRegex(TimeoutError, "desktop"):
            desktop.wait_for_ready(lambda: None, {}, timeout=0.03, interval=0.005)
        self.assertLess(time.monotonic() - started, 1)

    def test_shutdown_stops_and_reaps_owned_children(self):
        desktop = load_script("start-desktop.py")
        child = subprocess.Popen([sys.executable, "-c", "import time; time.sleep(60)"],
                                 start_new_session=True)
        try:
            desktop.stop_services({"child": child}, timeout=1)
            self.assertIsNotNone(child.poll())
        finally:
            if child.poll() is None:
                child.kill()
                child.wait()

    def test_sigterm_cleans_up_children(self):
        load_script("start-desktop.py")
        code = f"""
import importlib.util, subprocess, sys
sys.path.insert(0, {str(HERE)!r})
spec = importlib.util.spec_from_file_location('desktop', {str(HERE / 'start-desktop.py')!r})
desktop = importlib.util.module_from_spec(spec)
spec.loader.exec_module(desktop)
child = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(60)'], start_new_session=True)
desktop.install_signal_handlers()
print(child.pid, flush=True)
desktop.supervise({{'child': child}})
"""
        with subprocess.Popen([sys.executable, "-c", code], stdout=subprocess.PIPE, text=True) as parent:
            child_pid = int(parent.stdout.readline())
            parent.send_signal(signal.SIGTERM)
            self.assertEqual(0, parent.wait(timeout=10))
            with self.assertRaises(ProcessLookupError):
                os.kill(child_pid, 0)

    def test_icd_selection_rejects_missing_and_ambiguous_drivers(self):
        launcher = load_script("launch-demo.py")
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            with self.assertRaisesRegex(RuntimeError, "lavapipe"):
                launcher.select_icd(root)
            (root / "lvp_icd.aarch64.json").write_text("{}")
            self.assertEqual(str(root / "lvp_icd.aarch64.json"), launcher.select_icd(root))
            (root / "lvp_other.json").write_text("{}")
            with self.assertRaisesRegex(RuntimeError, "lavapipe"):
                launcher.select_icd(root)

    def test_launcher_retains_failure_code_and_output(self):
        launcher = load_script("launch-demo.py")
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "gradlew").write_text("echo controlled-gradle-failure; exit 23\n")
            self.assertEqual(23, launcher.run_gradle(root, [":task"], dict(os.environ)))
            self.assertEqual("controlled-gradle-failure\n",
                             (root / "dawn4k-demo/build/linux-desktop/demo.log").read_text())


if __name__ == "__main__":
    unittest.main()
