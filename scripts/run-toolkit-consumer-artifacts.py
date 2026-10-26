#!/usr/bin/env python3
"""Execute standalone published consumers on matching Linux/Android hosts."""
import argparse
import os
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

import toolkit_ci


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
            # Transient "could not get idle state" dumps must be retried, not fatal.
            subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/toolkit-consumer.xml"], check=False, capture_output=True)
            document = subprocess.check_output(["adb", "shell", "cat", "/sdcard/toolkit-consumer.xml"], text=True)
            texts = [node.get("text", "") for node in ET.fromstring(document).iter("node")]
            if any("Failed:" in text for text in texts):
                raise RuntimeError(texts)
            if any(f"Compute: {toolkit_ci.COMPUTE_RESULT}" in text and "triangle presented" in text for text in texts):
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
        marker = toolkit_ci.COMPUTE_RESULT if name == "compute" else toolkit_ci.SAMPLE_FRAMES_MARKER
        toolkit_ci.run_and_require(directory / f"{name}.kexe", marker, env)

    run("compute")
    with toolkit_ci.headless_display(env, display=":92", socket="dawn-consumer-wayland") as wayland:
        run("triangle")
        wayland()
        run("triangle")
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
