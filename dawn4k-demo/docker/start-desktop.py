#!/usr/bin/env python3
"""Own the headless compositor and remote-viewing processes."""
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
import threading
import time
from desktop_environment import validate_desktop_environment

STOPPING = threading.Event()


def extract_display(environment):
    for item in environment:
        if item.startswith("DISPLAY=") and item[8:]:
            return item[8:]
    raise RuntimeError("Sway has not provided DISPLAY for XWayland")


def require_services_alive(processes):
    for name, process in processes.items():
        code = process.poll()
        if code is not None:
            raise RuntimeError(f"{name} exited with status {code}")


def wait_for_ready(probe, processes, timeout=30.0, interval=0.1):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        require_services_alive(processes)
        if STOPPING.is_set():
            raise InterruptedError("desktop shutdown requested")
        result = probe()
        if result:
            return result
        STOPPING.wait(interval)
    raise TimeoutError("desktop readiness timed out; inspect Sway/XWayland logs")


def stop_services(processes, timeout=5.0):
    children = list(reversed(list(processes.values())))
    for process in children:
        if process.poll() is None:
            try:
                os.killpg(process.pid, signal.SIGTERM)
            except ProcessLookupError:
                pass
    deadline = time.monotonic() + timeout
    for process in children:
        try:
            process.wait(timeout=max(0.01, deadline - time.monotonic()))
        except subprocess.TimeoutExpired:
            try:
                os.killpg(process.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            process.wait()


def install_signal_handlers():
    for number in (signal.SIGTERM, signal.SIGINT):
        signal.signal(number, lambda *_: STOPPING.set())


def supervise(processes):
    try:
        while not STOPPING.wait(0.2):
            require_services_alive(processes)
    finally:
        stop_services(processes)


def start(processes, name, command, environment=None):
    print(f"[desktop] starting {name}", flush=True)
    processes[name] = subprocess.Popen(command, env=environment, start_new_session=True)


def main():
    install_signal_handlers()
    runtime = Path(os.environ["XDG_RUNTIME_DIR"])
    env_file = runtime / "desktop-env.json"
    env_file.unlink(missing_ok=True)
    processes = {}
    try:
        backend = os.environ.get("DAWN_DESKTOP_BACKEND", "x11")
        if backend not in ("x11", "wayland"):
            raise RuntimeError(f"unknown desktop backend: {backend}")
        config = "/etc/demo/sway-wayland.conf" if backend == "wayland" else "/etc/demo/sway.conf"
        start(processes, "sway", ["dbus-run-session", "--", "sway", "-c", config])

        def compositor_environment():
            sockets = list(runtime.glob("sway-ipc.*.sock"))
            if not sockets:
                return None
            socket = str(sockets[0])
            result = subprocess.run(
                ["swaymsg", "-s", socket, "exec", "python3 /opt/demo/export-environment.py"],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=2,
            )
            if result.returncode or not env_file.is_file():
                return None
            environment = json.loads(env_file.read_text())
            try:
                environment = validate_desktop_environment(dict(os.environ, **environment), backend)
            except RuntimeError:
                return None
            probe = subprocess.run(["wayland-info"], env=environment,
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=2)
            return environment if probe.returncode == 0 else None

        environment = wait_for_ready(compositor_environment, processes)
        start(processes, "wayvnc", ["wayvnc", "-C", "/etc/demo/wayvnc.conf", "127.0.0.1", "5900"], environment)
        start(processes, "websockify", ["websockify", "--web=/usr/share/novnc", "0.0.0.0:6080", "127.0.0.1:5900"], environment)
        endpoint = environment['DISPLAY'] if backend == "x11" else environment['WAYLAND_DISPLAY']
        print(f"[desktop] ready: {backend} {endpoint}, browser port 6080", flush=True)
        supervise(processes)
        return 0
    except InterruptedError:
        return 0
    except Exception as failure:
        print(f"[desktop] {failure}", file=sys.stderr, flush=True)
        return 1
    finally:
        stop_services(processes)


if __name__ == "__main__":
    sys.exit(main())
