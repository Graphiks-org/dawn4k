#!/usr/bin/env python3
"""Exercise the distributed application's actual entry point without X11."""
from pathlib import Path
import os
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
COMPOSE = ["docker", "compose", "-f", str(ROOT / "dawn4k-demo/docker/compose.yaml")]
if os.environ.get("DAWN_X11_TEST_PROJECT"):
    COMPOSE += ["-p", os.environ["DAWN_X11_TEST_PROJECT"]]


class LinuxEntrypointTest(unittest.TestCase):
    def test_missing_display_reports_launcher_before_awt_initialization(self):
        build = subprocess.run(COMPOSE + ["exec", "-T", "desktop", "/opt/demo/demo.sh",
            ":dawn4k-demo:installDist"], cwd=ROOT, text=True, capture_output=True, timeout=300)
        self.assertEqual(0, build.returncode, build.stdout[-8000:] + build.stderr)
        launch = subprocess.run(COMPOSE + ["exec", "-T", "-u", "demo", "desktop", "env",
            "-u", "DISPLAY", "-u", "WAYLAND_DISPLAY", "-u", "WAYLAND_SOCKET",
            "/workspace/dawn4k-demo/build/install/dawn4k-demo/bin/dawn4k-demo"],
            cwd=ROOT, text=True, capture_output=True, timeout=20)
        self.assertNotEqual(0, launch.returncode)
        self.assertIn("DISPLAY", launch.stderr)
        self.assertIn("WAYLAND", launch.stderr)
        self.assertIn("/opt/demo/demo.sh", launch.stderr)
        self.assertNotIn("HeadlessException", launch.stderr)
        self.assertNotIn("AWTError", launch.stderr)


if __name__ == "__main__":
    unittest.main()
