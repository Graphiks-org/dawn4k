#!/usr/bin/env python3
"""Run required toolkit suites and collect their actual execution evidence."""
import argparse
import os
from pathlib import Path
import platform
import subprocess

import toolkit_ci

MODULES = toolkit_ci.MODULES


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", required=True)
    parser.add_argument("--kind", choices=("desktop", "apple", "android", "compile"), required=True)
    parser.add_argument("--backend", required=True)
    parser.add_argument("--gradle-arg", action="append", default=[])
    args = parser.parse_args()
    output = Path("build/toolkit-coverage")
    output.mkdir(parents=True, exist_ok=True)
    env = os.environ.copy()
    env.update(DAWN_REQUIRE_ADAPTER="1", DAWN_TEST_BACKEND=args.backend, DAWN_DESKTOP_TESTS="1", DAWN_TOOLKIT_TARGET=args.target)
    gradle = ["gradlew.bat"] if platform.system() == "Windows" else ["./gradlew"]
    gradle += ["--no-daemon", "--console=plain", "--stacktrace", "--max-workers=2", *args.gradle_arg]

    def execute(modules, suite, surface, kind="gpu"):
        directory = output / f"{args.target}-{surface}-{kind}"
        directory.mkdir(exist_ok=True)
        log = directory / "gradle.log"
        with log.open("w") as stream:
            result = subprocess.run([*gradle, *[f":{module}:{suite}" for module in modules]], env=env, stdout=stream, stderr=subprocess.STDOUT)
            if args.kind == "android":
                subprocess.run(["adb", "logcat", "-d", "-v", "brief"], env=env, stdout=stream, stderr=subprocess.STDOUT, check=True)
        print(log.read_text(), flush=True)
        result.check_returncode()
        command = ["python3", "scripts/collect-toolkit-coverage.py", "--output", str(directory), "--target", args.target,
                   "--presentation", surface, "--backend", args.backend, "--suite", suite, "--log", str(log), "--kind", kind]
        for module in modules:
            command += ["--module", module]
        subprocess.run(command, check=True, env=env)

    if args.kind == "compile":
        execute(MODULES[:2], "compileKotlin" + args.target[0].upper() + args.target[1:], "none", "compile")
    elif args.kind == "android":
        subprocess.run(["adb", "logcat", "-c"], check=True)
        execute([MODULES[0]], "connectedAndroidDeviceTest", "headless")
        execute([MODULES[1]], "connectedAndroidDeviceTest", "android")
    elif args.kind == "apple":
        execute([MODULES[0]], args.target + "Test", "headless")
        execute([MODULES[1]], args.target + "Test", "metal")
    else:
        suite = "jvmTest" if args.target.startswith("jvm-") else args.target + "Test"
        if "linux" not in args.target:
            execute([MODULES[0]], suite, "headless")
            execute(MODULES[1:], suite, "win32" if "mingw" in args.target else "metal")
            return
        env.update(LIBGL_ALWAYS_SOFTWARE="1", LP_NUM_THREADS="2")
        with toolkit_ci.headless_display(env, socket="dawn-toolkit-wayland") as wayland:
            execute([MODULES[0]], suite, "headless")
            execute(MODULES[1:], suite, "x11")
            wayland()
            execute(MODULES[1:], suite, "wayland")


if __name__ == "__main__":
    main()
