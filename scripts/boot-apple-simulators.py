#!/usr/bin/env python3
"""Select and boot available Apple simulators for Gradle GPU tests."""

import argparse
import json
from pathlib import Path
import re
import subprocess


def select_device(devices, platform):
    candidates = []
    for runtime, runtime_devices in devices.items():
        match = re.fullmatch(
            rf"com\.apple\.CoreSimulator\.SimRuntime\.{re.escape(platform)}-(\d+(?:-\d+)*)",
            runtime,
        )
        if not match:
            continue
        version = tuple(int(part) for part in match[1].split("-"))
        for device in runtime_devices:
            if device.get("isAvailable"):
                candidates.append((device["state"] == "Booted", version, device))
    if not candidates:
        raise RuntimeError(f"No available {platform} simulator; install its runtime in Xcode")
    # Prefer an already running device; otherwise the newest available runtime.
    return max(candidates, key=lambda candidate: candidate[:2])[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--env-file", type=Path, default=Path("build/apple-simulators.env"))
    parser.add_argument("--platform", choices=("iOS", "tvOS"), action="append", help="Default: both platforms")
    args = parser.parse_args()
    devices = json.loads(subprocess.check_output(
        ["xcrun", "simctl", "list", "devices", "available", "--json"], text=True
    ))["devices"]
    selected = [(platform, select_device(devices, platform)) for platform in (args.platform or ("iOS", "tvOS"))]
    variables = []
    for platform, device in selected:
        print(f"{platform}: {device['name']} ({device['udid']})", flush=True)
        if device["state"] != "Booted":
            # A cold hosted CoreSimulator service can take over a minute to accept boot.
            subprocess.run(["xcrun", "simctl", "boot", device["udid"]], check=True, timeout=180)
        subprocess.run(
            ["xcrun", "simctl", "bootstatus", device["udid"], "-b"], check=True, timeout=600
        )
        variable = "DAWN_IOS_SIMULATOR" if platform == "iOS" else "DAWN_TVOS_SIMULATOR"
        variables.append(f"{variable}={device['udid']}")
    args.env_file.parent.mkdir(parents=True, exist_ok=True)
    with args.env_file.open("a") as destination:
        destination.write("\n".join(variables) + "\n")
    print(f"Simulator selection written to {args.env_file}")


if __name__ == "__main__":
    main()
