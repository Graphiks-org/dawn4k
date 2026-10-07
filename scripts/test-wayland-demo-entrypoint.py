#!/usr/bin/env python3
"""Test the actual distributed application, not only the host factory."""
import json
import os
from pathlib import Path
import queue
import subprocess
import threading
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
COMPOSE = ["docker", "compose", "-f", str(ROOT / "dawn4k-demo/docker/compose.yaml"),
           "-f", str(ROOT / "dawn4k-demo/docker/compose.wayland.yaml")]
APP = "/workspace/dawn4k-demo/build/install/dawn4k-demo/bin/dawn4k-demo"


def desktop_command(arguments, changes=None, compose=None):
    code = """import json, os, sys
environment = dict(os.environ, **json.load(open('/run/user/1000/desktop-env.json')))
for key, value in json.loads(sys.argv[1]).items():
    if value is None: environment.pop(key, None)
    else: environment[key] = value
os.execvpe(sys.argv[2], sys.argv[2:], environment)
"""
    return (compose or COMPOSE) + ["exec", "-T", "-u", "demo", "desktop", "python3", "-c", code,
                      json.dumps(changes or {}), *arguments]


class WaylandEntrypointTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        build = subprocess.run(COMPOSE + ["exec", "-T", "desktop", "/opt/demo/demo.sh", ":dawn4k-demo:installDist"],
                               cwd=ROOT, text=True, capture_output=True, timeout=240)
        if build.returncode:
            raise AssertionError(build.stdout[-8000:] + build.stderr)

    def test_missing_endpoints_and_forced_x11_fail_before_awt(self):
        for args, changes in (([], {"DISPLAY": None, "WAYLAND_DISPLAY": None, "WAYLAND_SOCKET": None}),
                              (["--platform=x11"], {"DISPLAY": None})):
            result = subprocess.run(desktop_command([APP, *args], changes), cwd=ROOT,
                                    text=True, capture_output=True, timeout=10)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("DISPLAY", result.stderr)
            self.assertNotIn("HeadlessException", result.stderr)
            self.assertNotIn("AWTError", result.stderr)

    def test_stale_wayland_never_falls_back_to_advertised_x11(self):
        result = subprocess.run(desktop_command([APP], {"WAYLAND_DISPLAY": "/nonexistent/dawn4k-wayland", "DISPLAY": ":0"}),
                                cwd=ROOT, text=True, capture_output=True, timeout=10)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Wayland", result.stdout + result.stderr)
        self.assertNotIn("display backend: X11", result.stdout)
        self.assertNotIn("HeadlessException", result.stderr)

    def test_automatic_wayland_presents_and_compositor_close_exits_cleanly(self):
        lines = queue.Queue()
        output = []
        process = subprocess.Popen(desktop_command([APP], {"DISPLAY": None, "WAYLAND_SOCKET": None}),
                                   cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        def read_output():
            for line in process.stdout:
                output.append(line)
                lines.put(line)
            lines.put(None)
        reader = threading.Thread(target=read_output, daemon=True)
        reader.start()
        try:
            while True:
                line = lines.get(timeout=60)
                self.assertIsNotNone(line, "".join(output))
                if "frame 1 rendered" in line: break
            trace = "".join(output)
            self.assertIn("display backend: Wayland", trace)
            self.assertIn("llvmpipe", trace)
            tree = json.loads(subprocess.check_output(desktop_command(["swaymsg", "-t", "get_tree"]), text=True))
            def nodes(node):
                yield node
                for child in node.get("nodes", []) + node.get("floating_nodes", []): yield from nodes(child)
            # pid is the container JVM, not the Mac's docker-exec process.
            pid_line = next(line for line in output if "[demo] process pid=" in line)
            pid = int(pid_line.split("pid=")[1].strip())
            window = next(n for n in nodes(tree) if n.get("pid") == pid)
            self.assertEqual("org.graphiks.dawn4k.demo", window.get("app_id"))
            self.assertIsNone(window.get("window"))
            self.assertEqual("xdg_shell", window.get("shell"))
            subprocess.run(desktop_command(["swaymsg", f"[con_id={window['id']}] kill"]), check=True, timeout=5)
            self.assertEqual(0, process.wait(timeout=10), "".join(output))
        finally:
            if process.poll() is None:
                process.terminate()
                try: process.wait(timeout=10)
                except subprocess.TimeoutExpired: process.kill(); process.wait(timeout=5)
            reader.join(timeout=5)
            process.stdout.close()

    @unittest.skipUnless(os.environ.get("DAWN_WAYLAND_DISCONNECT_TESTS") == "1", "opt in to a disposable desktop")
    def test_compositor_disconnection_is_an_error_and_exits_within_ten_seconds(self):
        disposable = COMPOSE + ["-p", "dawn4k-wayland-disconnect"]
        environment = dict(os.environ, DAWN_WAYLAND_PORT="6082")
        process = None
        reader = None
        output = []
        try:
            subprocess.run(disposable + ["up", "-d", "--build", "--wait", "--wait-timeout", "60"],
                           cwd=ROOT, env=environment, check=True, capture_output=True, timeout=240)
            workspace = ROOT / ".superpowers/sdd/2026-10-07-wayland-native"
            with tempfile.TemporaryDirectory(dir=workspace) as directory:
                distribution = str(Path(directory) / "dist")
                subprocess.run(COMPOSE + ["cp", "desktop:/workspace/dawn4k-demo/build/install/dawn4k-demo", distribution],
                               check=True, capture_output=True, timeout=30)
                subprocess.run(disposable + ["cp", distribution, "desktop:/opt/demo/test-dist"],
                               check=True, capture_output=True, timeout=30)
            lines = queue.Queue()
            process = subprocess.Popen(desktop_command(["/opt/demo/test-dist/bin/dawn4k-demo"], compose=disposable),
                                       cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            def read_output():
                for line in process.stdout:
                    output.append(line)
                    lines.put(line)
                lines.put(None)
            reader = threading.Thread(target=read_output, daemon=True)
            reader.start()
            while True:
                line = lines.get(timeout=60)
                self.assertIsNotNone(line, "".join(output))
                if "frame 1 rendered" in line: break
            # Freeze only this test supervisor so it cannot kill the JVM before
            # the application has a chance to demonstrate its own cleanup.
            disconnect = """import os, pathlib, signal
for path in pathlib.Path('/proc').iterdir():
    if not path.name.isdigit(): continue
    try:
        args = (path / 'cmdline').read_bytes().split(b'\\0')
        if b'/opt/demo/start-desktop.py' in args: os.kill(int(path.name), signal.SIGSTOP)
        if (path / 'comm').read_text().strip() == 'sway': os.kill(int(path.name), signal.SIGTERM)
    except (FileNotFoundError, ProcessLookupError): pass
"""
            subprocess.run(disposable + ["exec", "-T", "desktop", "python3", "-c", disconnect], check=True, timeout=5)
            self.assertNotEqual(0, process.wait(timeout=10))
            reader.join(timeout=5)
            self.assertIn("Wayland connection", "".join(output))
        finally:
            if process is not None and process.poll() is None:
                process.terminate()
                try: process.wait(timeout=5)
                except subprocess.TimeoutExpired: process.kill(); process.wait(timeout=5)
            if reader is not None: reader.join(timeout=5)
            if process is not None: process.stdout.close()
            resume = """import os, pathlib, signal
for path in pathlib.Path('/proc').iterdir():
    if not path.name.isdigit(): continue
    try:
        if b'/opt/demo/start-desktop.py' in (path / 'cmdline').read_bytes().split(b'\\0'):
            os.kill(int(path.name), signal.SIGCONT)
    except (FileNotFoundError, ProcessLookupError): pass
"""
            subprocess.run(disposable + ["exec", "-T", "desktop", "python3", "-c", resume],
                           capture_output=True, timeout=5)
            subprocess.run(disposable + ["down", "--timeout", "5"], capture_output=True, timeout=30)


if __name__ == "__main__":
    unittest.main()
