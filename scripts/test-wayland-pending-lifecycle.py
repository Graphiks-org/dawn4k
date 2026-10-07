#!/usr/bin/env python3
"""Deterministic real-GPU startup disconnect in an isolated disposable desktop."""
import importlib.util
import os
from pathlib import Path
import subprocess
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("entrypoint", ROOT / "scripts/test-wayland-demo-entrypoint.py")
entrypoint = importlib.util.module_from_spec(spec)
spec.loader.exec_module(entrypoint)
COMPOSE = entrypoint.COMPOSE + ["-p", "dawn4k-wayland-disconnect"]


class PendingLifecycleTest(unittest.TestCase):
    def test_disconnect_after_real_capabilities_before_controls_initialization(self):
        environment = dict(os.environ, DAWN_WAYLAND_PORT="6082")
        try:
            subprocess.run(COMPOSE + ["up", "-d", "--build", "--wait", "--wait-timeout", "60"],
                           env=environment, cwd=ROOT, check=True, capture_output=True, timeout=240)
            subprocess.run(COMPOSE + ["exec", "-T", "desktop", "touch", "/opt/demo/disconnect-fixture"],
                           check=True, capture_output=True, timeout=5)
            result = subprocess.run(COMPOSE + ["exec", "-T", "-e", "DAWN_WAYLAND_TESTS=1",
                "-e", "DAWN_WAYLAND_PENDING_TESTS=1", "desktop", "/opt/demo/demo.sh",
                ":dawn4k-demo:test", "--tests",
                "*WaylandLifecycleCheckpointTest.disconnectAfterCapabilityNegotiationCannotBeErasedByInitialization"],
                cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=420)
            self.assertEqual(0, result.returncode, result.stdout[-10000:])
            xml = subprocess.check_output(COMPOSE + ["exec", "-T", "desktop", "cat",
                "/workspace/dawn4k-demo/build/test-results/test/TEST-org.graphiks.dawn4k.demo.WaylandLifecycleCheckpointTest.xml"],
                text=True, timeout=5)
            report = ET.fromstring(xml)
            self.assertEqual("1", report.get("tests"))
            self.assertEqual("0", report.get("failures"))
            self.assertEqual("0", report.get("errors"))
            self.assertIn("Wayland Capabilities checkpoint", report.findtext("system-out") or "")
        finally:
            # The Kotlin fixture suspends only its own supervisor while asserting
            # application cleanup; resume it after Gradle has written its report.
            resume = """import os, pathlib, signal
for path in pathlib.Path('/proc').iterdir():
    if not path.name.isdigit(): continue
    try:
        if b'/opt/demo/start-desktop.py' in (path / 'cmdline').read_bytes().split(b'\\0'):
            os.kill(int(path.name), signal.SIGCONT)
    except (FileNotFoundError, ProcessLookupError): pass
"""
            subprocess.run(COMPOSE + ["exec", "-T", "desktop", "python3", "-c", resume], capture_output=True, timeout=5)
            subprocess.run(COMPOSE + ["down", "--timeout", "5"], capture_output=True, timeout=30)


if __name__ == "__main__":
    unittest.main()
