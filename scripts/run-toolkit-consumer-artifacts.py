#!/usr/bin/env python3
"""Execute standalone published consumers on matching Linux/Android hosts."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile
import time
import xml.etree.ElementTree as ET


def android(apk):
    package = "org.graphiks.dawn4k.toolkit.examples.app"
    subprocess.run(["adb", "install", "-r", str(apk)], check=True)
    subprocess.run(["adb", "logcat", "-c"], check=True)
    component = subprocess.check_output(["adb", "shell", "cmd", "package", "resolve-activity", "--brief", package], text=True).strip().splitlines()[-1]
    if "/" not in component:
        raise RuntimeError("No consumer launcher activity")

    def start_and_require_triangle():
        subprocess.run(["adb", "shell", "am", "start", "-W", "-n", component], check=True)
        deadline = time.monotonic() + 90
        while time.monotonic() < deadline:
            subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/toolkit-consumer.xml"], check=True, capture_output=True)
            document = subprocess.check_output(["adb", "shell", "cat", "/sdcard/toolkit-consumer.xml"], text=True)
            texts = [node.get("text", "") for node in ET.fromstring(document).iter("node")]
            if any("Failed:" in text for text in texts):
                raise RuntimeError(texts)
            if any("Compute: [2, 4, 6, 8]" in text and "triangle presented" in text for text in texts):
                print("MINIFIED_CONSUMER compute=[2,4,6,8] triangle=presented", flush=True)
                return
            time.sleep(1)
        raise RuntimeError("Minified Maven consumer did not render its triangle")

    try:
        start_and_require_triangle()
        subprocess.run(["adb", "shell", "input", "keyevent", "KEYCODE_HOME"], check=True)
        deadline = time.monotonic() + 30
        while True:
            log = subprocess.check_output(["adb", "logcat", "-d", "-v", "brief"], text=True)
            if "TOOLKIT_SAMPLE android-view-retired" in log:
                break
            if time.monotonic() > deadline:
                raise RuntimeError("Minified consumer did not acknowledge surface retirement")
            time.sleep(0.25)
        start_and_require_triangle()
        print("MINIFIED_CONSUMER retirement-and-resume=passed", flush=True)
    finally:
        subprocess.run(["adb", "shell", "am", "force-stop", package], check=True)
        subprocess.run(["adb", "shell", "rm", "-f", "/sdcard/toolkit-consumer.xml"], check=True)


def linux(directory):
    env = os.environ.copy()
    env.update(DAWN_REQUIRE_ADAPTER="1", DAWN_TEST_BACKEND="Vulkan", DAWN_EXAMPLE_FRAMES="60", LIBGL_ALWAYS_SOFTWARE="1", LP_NUM_THREADS="2")

    def run(name):
        binary = (directory / f"{name}.kexe").resolve()
        binary.chmod(0o755)
        result = subprocess.run([str(binary)], env=env, capture_output=True, text=True, timeout=180)
        print(result.stdout, flush=True)
        if result.returncode:
            print(result.stderr, flush=True)
        result.check_returncode()
        expected = "[2, 4, 6, 8]" if name == "compute" else "TOOLKIT_SAMPLE frames=60"
        if expected not in result.stdout:
            raise RuntimeError(f"Missing actual Maven consumer result: {name}")

    run("compute")
    servers = []
    with tempfile.TemporaryDirectory(prefix="dawn-consumer-") as temporary:
        runtime = Path(temporary)
        runtime.chmod(0o700)
        env.update(DISPLAY=":92", XDG_RUNTIME_DIR=temporary)

        def start(command, name, socket):
            log = (runtime / name).open("w+")
            process = subprocess.Popen(command, env=env, stdout=log, stderr=subprocess.STDOUT)
            servers.append((process, log))
            deadline = time.monotonic() + 30
            while not socket.exists():
                if process.poll() is not None or time.monotonic() > deadline:
                    log.flush(); log.seek(0)
                    raise RuntimeError(log.read())
                time.sleep(0.05)

        try:
            start(["Xvfb", ":92", "-screen", "0", "800x600x24", "-nolisten", "tcp", "-ac"], "x11.log", Path("/tmp/.X11-unix/X92"))
            env.update(XDG_SESSION_TYPE="x11", DAWN_WAYLAND_TESTS="0")
            env.pop("WAYLAND_DISPLAY", None)
            run("triangle")
            env.update(WAYLAND_DISPLAY="dawn-consumer-wayland", XDG_SESSION_TYPE="wayland", DAWN_WAYLAND_TESTS="1")
            start(["weston", "--backend=headless-backend.so", "--renderer=pixman", "--idle-time=0", "--socket=dawn-consumer-wayland"], "wayland.log", runtime / "dawn-consumer-wayland")
            run("triangle")
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
    print("MAVEN_CONSUMER Linux compute/X11/Wayland passed", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--linux-directory", type=Path)
    args = parser.parse_args()
    if bool(args.apk) == bool(args.linux_directory):
        parser.error("select --apk or --linux-directory")
    if args.apk:
        android(args.apk)
    else:
        linux(args.linux_directory)


if __name__ == "__main__":
    main()
