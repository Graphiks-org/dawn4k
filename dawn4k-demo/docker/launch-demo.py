#!/usr/bin/env python3
"""Run Gradle with the compositor environment, software Vulkan, and durable logs."""
import json
import os
from pathlib import Path
import signal
import subprocess
import sys
from desktop_environment import validate_desktop_environment


def select_icd(directory):
    candidates = sorted(Path(directory).glob("lvp*.json"))
    if len(candidates) != 1:
        raise RuntimeError(f"expected one lavapipe Vulkan ICD in {directory}, found {candidates}")
    return str(candidates[0])


def run_gradle(root, tasks, environment):
    root = Path(root)
    output = root / "dawn4k-demo/build/linux-desktop/demo.log"
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w") as log:
        child = subprocess.Popen(
            ["bash", str(root / "gradlew"), *tasks, "-Pdawn.targets=linuxArm64", "--console=plain", "--no-daemon"],
            cwd=root, env=environment, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, start_new_session=True,
        )
        def forward(number, _frame):
            if child.poll() is None:
                try:
                    os.killpg(child.pid, number)
                except ProcessLookupError:
                    pass
        original_signals = {number: signal.signal(number, forward)
                            for number in (signal.SIGTERM, signal.SIGINT)}
        try:
            for line in child.stdout:
                sys.stdout.write(line)
                sys.stdout.flush()
                log.write(line)
                log.flush()
            return child.wait()
        finally:
            child.stdout.close()
            if child.poll() is None:
                forward(signal.SIGTERM, None)
                try:
                    child.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    forward(signal.SIGKILL, None)
                    child.wait()
            for number, handler in original_signals.items():
                signal.signal(number, handler)


def main():
    environment = dict(os.environ, **json.loads(Path("/run/user/1000/desktop-env.json").read_text()))
    backend = environment.get("DAWN_DESKTOP_BACKEND", "x11")
    environment = validate_desktop_environment(environment, backend)
    icd = select_icd("/usr/share/vulkan/icd.d")
    environment.update(VK_DRIVER_FILES=icd, VK_ICD_FILENAMES=icd, SKIKO_RENDER_API="SOFTWARE")
    return run_gradle("/workspace", sys.argv[1:] or default_tasks(backend), environment)


def default_tasks(backend):
    if backend == "wayland":
        return [":dawn4k-demo:run"]
    if backend == "x11":
        return [":dawn4k-demo:run", "--args=--platform=x11"]
    raise RuntimeError(f"unknown desktop backend: {backend}")


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as failure:
        print(f"[demo launcher] {failure}", file=sys.stderr)
        sys.exit(1)
