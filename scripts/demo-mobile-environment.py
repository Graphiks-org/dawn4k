#!/usr/bin/env python3
"""Read-only mobile prerequisite inventory. Never boot, create or modify a profile."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
from urllib.request import urlopen
import xml.etree.ElementTree as ET


REPOSITORY = "https://central.sonatype.com/repository/maven-snapshots/org/graphiks"


def select_ios_device(devices: dict, runtime: str, udid: str | None = None) -> str:
    available = [device for device in devices.get("devices", {}).get(runtime, [])
                 if device.get("isAvailable") is True and (udid is None or device.get("udid") == udid)]
    if not available:
        raise ValueError(f"iOS runtime/device unavailable: {runtime}, UDID={udid}")
    if len(available) != 1:
        raise ValueError(f"multiple iOS devices match {runtime}; supply an explicit --ios-udid UDID")
    return available[0]["udid"]


def read_properties(path: Path) -> dict:
    result = {}
    for line in path.read_text().splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            result[key.strip()] = value.strip()
    return result


def select_android_image(sdk: Path, api: int, abi: str, tag: str | None = None) -> Path:
    if abi not in ("arm64-v8a", "x86_64") or api < 24:
        raise ValueError(f"unsupported qualification Android API/ABI: {api}/{abi}")
    directory = sdk / "system-images" / f"android-{api}"
    candidates = sorted(directory.glob(f"{tag or '*'}/{abi}"))
    if not candidates:
        raise ValueError(f"Android image unavailable: API{api}/{tag or '*'}/{abi} in {sdk}")
    if len(candidates) != 1:
        raise ValueError("multiple Android images match; supply an explicit --android-tag")
    image = candidates[0]
    properties_path = image / "source.properties"
    if not properties_path.is_file():
        raise ValueError(f"Android image is missing source.properties: {image}")
    properties = read_properties(properties_path)
    if properties.get("SystemImage.Abi") != abi:
        raise ValueError(f"Android image ABI mismatch: {image}")
    if properties.get("AndroidVersion.ApiLevel") != str(api):
        raise ValueError(f"Android image API mismatch: {image}")
    if not (image / "system.img").is_file():
        raise ValueError(f"Android image is missing system.img: {image}")
    return image


def sdk_binary(sdk: Path, relative: str) -> Path:
    binary = sdk / relative
    if not binary.is_file() or not os.access(binary, os.X_OK):
        raise ValueError(f"missing executable SDK binary: {binary}")
    return binary


def run_text(command: list[str], timeout: float = 15) -> str:
    try:
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE,
                                stderr=subprocess.PIPE, timeout=timeout, check=False)
    except subprocess.TimeoutExpired as failure:
        raise ValueError(f"inventory command timeout: {command}") from failure
    except OSError as failure:
        raise ValueError(f"inventory command unavailable: {command}: {failure}") from failure
    if result.returncode != 0:
        raise ValueError(f"inventory command exit {result.returncode}: {command}: {result.stderr.strip()}")
    return result.stdout.strip() or result.stderr.strip()


def ndk_compiler(sdk: Path, version: str, abi: str) -> Path:
    prefixes = {"arm64-v8a": "aarch64", "x86_64": "x86_64"}
    if abi not in prefixes:
        raise ValueError(f"unsupported NDK ABI: {abi}")
    ndk = sdk / "ndk" / version
    compilers = list(ndk.glob(f"toolchains/llvm/prebuilt/*/bin/{prefixes[abi]}-linux-android24-clang"))
    if len(compilers) != 1 or not os.access(compilers[0], os.X_OK):
        raise ValueError(f"missing unambiguous Android {abi} NDK compiler: {ndk}")
    return compilers[0]


def require_same_publication(metadata: dict[str, str]) -> str:
    timestamps = set()
    for artifact, text in metadata.items():
        timestamp = ET.fromstring(text).findtext("./versioning/snapshot/timestamp")
        if not timestamp:
            raise ValueError(f"missing snapshot publication timestamp: {artifact}")
        timestamps.add(timestamp)
    if len(timestamps) != 1:
        raise ValueError(f"inconsistent suite-demos snapshot publication: {sorted(timestamps)}")
    return timestamps.pop()


def fetch_bytes(url: str) -> bytes:
    with urlopen(url, timeout=30) as response:
        data = response.read(8 * 1024 * 1024 + 1)
    if len(data) > 8 * 1024 * 1024:
        raise ValueError(f"dependency metadata too large: {url}")
    return data


def dependency_inventory() -> dict:
    names = ("suite-demos", "suite-demos-android", "suite-demos-iosarm64", "suite-demos-iossimulatorarm64")
    xml = {}
    artifacts = {}
    modules = {}
    for name in names:
        base = f"{REPOSITORY}/{name}/0.1.0-SNAPSHOT"
        xml_url = f"{base}/maven-metadata.xml"
        raw = fetch_bytes(xml_url)
        xml[name] = raw.decode("utf-8")
        candidates = [version.findtext("value") for version in ET.fromstring(raw).findall(
            "./versioning/snapshotVersions/snapshotVersion")
            if version.findtext("extension") == "module" and not version.findtext("classifier")]
        if len(candidates) != 1 or not candidates[0]:
            raise ValueError(f"missing/unambiguous Gradle module snapshot: {name}")
        version = candidates[0]
        module_url = f"{base}/{name}-{version}.module"
        module = fetch_bytes(module_url)
        modules[name] = json.loads(module)
        artifacts[name] = {"metadataUrl": xml_url, "metadataSha256": hashlib.sha256(raw).hexdigest(),
                           "moduleUrl": module_url, "moduleSha256": hashlib.sha256(module).hexdigest(),
                           "version": version}
    timestamp = require_same_publication(xml)
    platforms = {variant.get("attributes", {}).get("org.jetbrains.kotlin.platform.type")
                 for variant in modules["suite-demos"].get("variants", [])}
    native_targets = {variant.get("attributes", {}).get("org.jetbrains.kotlin.native.target")
                      for variant in modules["suite-demos"].get("variants", [])}
    if not {"androidJvm", "jvm", "common"}.issubset(platforms) or not {
            "ios_arm64", "ios_simulator_arm64"}.issubset(native_targets):
        raise ValueError("suite-demos root module is missing required KMP variants")
    return {"publication": timestamp, "artifacts": artifacts, "variantsAvailable": True,
            "resolvedByGradle": False, "nativeExecutionQualified": False}


def inventory(args) -> dict:
    report = {"prerequisitesAvailable": False, "nativeExecutionQualified": False,
              "mode": "read-only-inventory", "failures": []}
    sdk = args.sdk.expanduser().absolute()
    try:
        adb = sdk_binary(sdk, "platform-tools/adb")
        emulator = sdk_binary(sdk, "emulator/emulator")
        image = select_android_image(sdk, args.android_api, args.android_abi, args.android_tag)
        compiler = ndk_compiler(sdk, args.ndk, args.android_abi)
        report["android"] = {"sdk": str(sdk), "api": args.android_api, "abi": args.android_abi,
            "image": str(image), "ndk": args.ndk, "compiler": str(compiler),
            "adbVersion": run_text([str(adb), "version"]),
            "emulatorVersion": run_text([str(emulator), "-version"]),
            "existingAvds": sorted(p.name for p in (args.home / ".android/avd").glob("*.ini")),
            "profileCreated": False, "deviceBooted": False}
    except (ValueError, OSError) as failure:
        report["failures"].append(str(failure))
    try:
        xcrun = shutil.which("xcrun")
        xcodebuild = shutil.which("xcodebuild")
        if not xcrun or not xcodebuild:
            raise ValueError("Xcode/xcrun unavailable")
        devices = json.loads(run_text([xcrun, "simctl", "list", "devices", "available", "--json"]))
        udid = select_ios_device(devices, args.ios_runtime, args.ios_udid)
        report["ios"] = {"xcodeVersion": run_text([xcodebuild, "-version"]),
            "runtime": args.ios_runtime, "inventoryCandidateUdid": udid,
            "deviceBooted": False, "profileCreated": False}
    except (ValueError, OSError) as failure:
        report["failures"].append(str(failure))
    if not args.offline:
        try:
            report["dependency"] = dependency_inventory()
        except (ValueError, OSError, ET.ParseError) as failure:
            report["failures"].append(f"dependency metadata: {failure}")
    else:
        report["dependency"] = {"checked": False, "reason": "offline inventory"}
    report["prerequisitesAvailable"] = not report["failures"]
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--home", type=Path, default=Path.home())
    parser.add_argument("--sdk", type=Path, default=Path(os.getenv("ANDROID_HOME",
                        str(Path.home() / "Library/Android/sdk"))))
    parser.add_argument("--android-api", type=int, default=35)
    parser.add_argument("--android-abi", default="arm64-v8a")
    parser.add_argument("--android-tag", default="google_apis")
    parser.add_argument("--ndk", default="28.2.13676358")
    parser.add_argument("--ios-runtime", default="com.apple.CoreSimulator.SimRuntime.iOS-26-5")
    parser.add_argument("--ios-udid")
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    report = inventory(args)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))
    return 0 if report["prerequisitesAvailable"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
