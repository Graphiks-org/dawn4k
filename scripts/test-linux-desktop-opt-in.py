#!/usr/bin/env python3
"""An opt-in desktop test must really run after an otherwise identical dry run."""
from pathlib import Path
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
COMPOSE = ["docker", "compose", "-f", str(ROOT / "dawn4k-demo/docker/compose.yaml")]


class DesktopOptInTest(unittest.TestCase):
    def run_window_tests(self, enabled):
        result = subprocess.run(COMPOSE + ["exec", "-T", "-e", f"DAWN_DESKTOP_TESTS={enabled}",
            "desktop", "/opt/demo/demo.sh", ":dawn4k-demo:test", "--tests", "*LinuxDemoWindowTest"],
            cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        self.assertEqual(0, result.returncode, result.stdout[-8000:])
        report = subprocess.run(COMPOSE + ["exec", "-T", "desktop", "cat",
            "/workspace/dawn4k-demo/build/test-results/test/TEST-org.graphiks.dawn4k.demo.LinuxDemoWindowTest.xml"],
            cwd=ROOT, text=True, capture_output=True, check=True)
        return report.stdout

    def test_opt_in_cannot_reuse_the_no_desktop_test_result(self):
        self.assertNotIn("frame 1 rendered", self.run_window_tests("0"))
        self.assertIn("frame 1 rendered", self.run_window_tests("1"),
                      "Gradle reused a gated-out run instead of exercising the actual desktop")


if __name__ == "__main__":
    unittest.main()
