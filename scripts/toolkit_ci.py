#!/usr/bin/env python3
"""Shared plumbing for the toolkit CI scripts.

One source for the published module list, the example output markers the
consumer scripts assert on, and the Xvfb/Weston orchestration the Linux GPU
suites and the published-consumer runs depend on.
"""
import contextlib
from pathlib import Path
import subprocess
import tempfile
import time

MODULES = ["dawn4k-toolkit-core", "dawn4k-toolkit-surface", "dawn4k-toolkit-glfw"]
COMPUTE_RESULT = "[2, 4, 6, 8]"
SAMPLE_FRAMES_MARKER = "TOOLKIT_SAMPLE frames=60"

XVFB_SCREEN = "800x600x24"
# The single Weston argument list every Linux CI consumer runs under; the
# geometry matches the Xvfb screen so presentation surfaces resize identically.
WESTON_ARGUMENTS = ("--backend=headless-backend.so", "--renderer=pixman", "--idle-time=0", "--width=800", "--height=600")


def run_and_require(binary, marker, env=None):
    """Run one published consumer binary, print its output, require success."""
    binary = Path(binary).resolve()
    binary.chmod(0o755)
    result = subprocess.run([str(binary)], env=env, capture_output=True, text=True, timeout=180)
    print(result.stdout, flush=True)
    if result.returncode:
        print(result.stderr, flush=True)
    assert result.returncode == 0, f"{binary.name} exited with code {result.returncode}"
    assert marker in result.stdout, f"{binary.name} did not report its marker: {marker}"
    return result


@contextlib.contextmanager
def headless_display(env, display=":91", socket="dawn-wayland", weston_extra_args=()):
    """Serve X11 through Xvfb immediately and Weston on demand for Wayland phases.

    All servers share one temporary XDG runtime directory; the Xvfb and Weston
    sockets are awaited for up to 30 seconds (polled every 0.05 s). Teardown
    stops servers in reverse startup order — terminate, five seconds to exit,
    then kill — and prints every server log first.
    """
    with tempfile.TemporaryDirectory(prefix="dawn-toolkit-display-") as temporary:
        runtime = Path(temporary)
        runtime.chmod(0o700)
        env.update(DISPLAY=display, XDG_RUNTIME_DIR=temporary)
        servers = []

        def start(command, name, wait_for):
            log = (runtime / name).open("w+")
            process = subprocess.Popen(command, env=env, stdout=log, stderr=subprocess.STDOUT)
            servers.append((process, log))
            deadline = time.monotonic() + 30
            while not wait_for.exists():
                if process.poll() is not None or time.monotonic() > deadline:
                    log.flush(); log.seek(0)
                    raise RuntimeError(log.read())
                time.sleep(0.05)

        def begin_wayland():
            env.update(WAYLAND_DISPLAY=socket, XDG_SESSION_TYPE="wayland", DAWN_WAYLAND_TESTS="1")
            start(["weston", *WESTON_ARGUMENTS, f"--socket={socket}", *weston_extra_args], "wayland.log", runtime / socket)

        try:
            start(["Xvfb", display, "-screen", "0", XVFB_SCREEN, "-nolisten", "tcp", "-ac"],
                  "x11.log", Path(f"/tmp/.X11-unix/X{display.lstrip(':')}"))
            env.update(XDG_SESSION_TYPE="x11", DAWN_WAYLAND_TESTS="0")
            env.pop("WAYLAND_DISPLAY", None)
            yield begin_wayland
        finally:
            for process, log in reversed(servers):
                process.terminate()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    process.kill(); process.wait()
                log.flush(); log.seek(0)
                print(log.read(), flush=True)
                log.close()
