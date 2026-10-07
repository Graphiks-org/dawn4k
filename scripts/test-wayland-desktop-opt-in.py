#!/usr/bin/env python3
"""Wayland live tests cannot reuse a prior gated-out run."""
import subprocess
import unittest
import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("entrypoint", ROOT / "scripts/test-wayland-demo-entrypoint.py")
entrypoint = importlib.util.module_from_spec(spec)
spec.loader.exec_module(entrypoint)


class WaylandOptInTest(unittest.TestCase):
    def run_window_tests(self, enabled):
        result = subprocess.run(entrypoint.COMPOSE + ["exec", "-T", "-e", f"DAWN_WAYLAND_TESTS={enabled}",
            "desktop", "/opt/demo/demo.sh", ":dawn4k-demo:test", "--tests", "*WaylandDemoWindowTest"],
            cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=180)
        self.assertEqual(0, result.returncode, result.stdout[-8000:])
        return subprocess.check_output(entrypoint.COMPOSE + ["exec", "-T", "desktop", "cat",
            "/workspace/dawn4k-demo/build/test-results/test/TEST-org.graphiks.dawn4k.demo.WaylandDemoWindowTest.xml"], text=True)

    def test_opt_in_really_presents_after_opt_out(self):
        self.assertNotIn("frame 1 rendered", self.run_window_tests("0"))
        self.assertIn("frame 1 rendered", self.run_window_tests("1"))


if __name__ == "__main__":
    unittest.main()
