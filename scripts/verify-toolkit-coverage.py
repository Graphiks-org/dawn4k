#!/usr/bin/env python3
"""Check real GPU execution separately from compilation and publication variants."""
import argparse
import hashlib
import importlib.util
import json
import re
import sys
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
import zipfile

import toolkit_ci

MODULES = toolkit_ci.MODULES
COMMON_TARGETS = {"jvm", "android", "macosArm64", "linuxX64", "iosArm64", "iosSimulatorArm64", "iosX64", "tvosArm64", "tvosSimulatorArm64"}
DECLARED = {MODULES[0]: COMMON_TARGETS, MODULES[1]: COMMON_TARGETS, MODULES[2]: {"jvm", "macosArm64", "linuxX64"}}
NATIVE_NAMES = {"macos_arm64": "macosArm64", "linux_x64": "linuxX64", "ios_arm64": "iosArm64", "ios_simulator_arm64": "iosSimulatorArm64", "ios_x64": "iosX64", "tvos_arm64": "tvosArm64", "tvos_simulator_arm64": "tvosSimulatorArm64"}
GPU_TESTS = {
    MODULES[0]: {"headlessComputeReturnsExpectedValues", "offscreenTriangleHasExpectedPixel"},
    MODULES[1]: {"compatibleSurfaceSelectsPresentingAdapter", "presentationResizePreservesDevice", "frameCancellationReleasesViewThenTexture"},
}

class VerificationError(ValueError):
    pass

def verify_android_manifest_package(manifest):
    package = ET.fromstring(manifest).get("package", "")
    keywords = set("""abstract assert boolean break byte case catch char class const continue
        default do double else enum extends final finally float for goto if implements import
        instanceof int interface long native new package private protected public return short
        static strictfp super switch synchronized this throw throws transient try void volatile
        while true false null _""".split())
    if not package or any(part in keywords or not part.replace("$", "_").isidentifier()
                          for part in package.split(".")):
        raise VerificationError(f"Published AAR manifest has an invalid Java package: {package}")

def verify_runtime_archives(root, version):
    def archive(module, extension):
        path = root / "org/graphiks" / module / version / f"{module}-{version}.{extension}"
        if not path.is_file():
            raise VerificationError(f"Missing published runtime artifact: {path}")
        return zipfile.ZipFile(path)

    def require_elf(container, name, machine):
        data = container.read(name)
        if data[:6] != b"\x7fELF\x02\x01" or int.from_bytes(data[18:20], "little") != machine:
            raise VerificationError(f"Wrong native ABI: {name}")

    with archive("dawn4k-native-jvm", "jar") as jar:
        require_elf(jar, "linux-x86-64/libwebgpu_dawn.so", 62)
        require_elf(jar, "linux-aarch64/libwebgpu_dawn.so", 183)
        apple = jar.read("darwin-aarch64/libwebgpu_dawn.dylib")
        if apple[:4] != b"\xcf\xfa\xed\xfe" or int.from_bytes(apple[4:8], "little") != 0x100000c:
            raise VerificationError("Missing macOS ARM64 Dawn runtime")
        # The published manifest is the single source for the Windows runtime dependency
        # set (Task 10): the build fails unless its names cover every staged DLL besides
        # the Dawn library itself. Verify that same invariant against the published jar
        # instead of restating the name list a fourth time here.
        manifest = dict(line.split("=", 1) for line in jar.read("dawn4k/windows-runtime.properties").decode().splitlines() if line)
        staged = {name.split("/")[-1] for name in jar.namelist() if name.startswith("win32-x86-64/") and name.endswith(".dll")}
        if set(manifest) != staged - {"webgpu_dawn.dll"}:
            raise VerificationError(f"Windows runtime manifest does not match the staged DLLs: {sorted(manifest)} vs {sorted(staged)}")
        for name in sorted(staged):
            windows = jar.read(f"win32-x86-64/{name}")
            pe = int.from_bytes(windows[60:64], "little")
            if windows[:2] != b"MZ" or windows[pe:pe+4] != b"PE\x00\x00" or int.from_bytes(windows[pe+4:pe+6], "little") != 0x8664:
                raise VerificationError(f"Missing Windows x64 runtime: {name}")
            if name in manifest and hashlib.sha256(windows).hexdigest() != manifest[name]:
                raise VerificationError(f"Windows runtime dependency checksum mismatch: {name}")
    for module, library in (("dawn4k-native-android", "webgpu_dawn"), ("dawn4k-toolkit-surface-android", "dawn4k_toolkit_window")):
        with archive(module, "aar") as aar:
            verify_android_manifest_package(aar.read("AndroidManifest.xml"))
            for abi, machine in (("arm64-v8a", 183), ("x86_64", 62)):
                require_elf(aar, f"jni/{abi}/lib{library}.so", machine)
            rules = "\n".join(aar.read(name).decode() for name in aar.namelist() if "proguard" in name.lower())
            if module == "dawn4k-native-android":
                if "org.graphiks.dawn4k.native.*Trampoline" not in rules or "public static void dispatch(...);" not in rules:
                    raise VerificationError("Raw AAR does not publish its JNI upcall consumer keep rules")
            if module == "dawn4k-toolkit-surface-android":
                if "org.graphiks.dawn4k.toolkit.surface.AndroidNativeWindow" not in rules or "native <methods>" not in rules:
                    raise VerificationError("Surface AAR does not publish its JNI consumer keep rules")

def verify_metadata(metadata):
    if set(metadata) != set(MODULES):
        raise VerificationError("publication must include exactly core, surface and GLFW")
    for module, expected in DECLARED.items():
        targets = set()
        for variant in metadata[module].get("variants", []):
            attributes = variant.get("attributes", {})
            platform = attributes.get("org.jetbrains.kotlin.platform.type")
            if platform == "common" or attributes.get("org.gradle.category") == "documentation":
                continue
            if platform in ("jvm", "androidJvm"):
                targets.add("android" if platform == "androidJvm" else "jvm")
            elif platform == "native":
                native = attributes.get("org.jetbrains.kotlin.native.target")
                if native not in NATIVE_NAMES:
                    raise VerificationError(f"{module}: unexpected native variant {native}")
                targets.add(NATIVE_NAMES[native])
            else:
                raise VerificationError(f"{module}: unexpected Kotlin platform {platform}")
        if targets != expected:
            raise VerificationError(f"{module}: missing {sorted(expected - targets)}, unexpected {sorted(targets - expected)}")

def verify_coverage(records, required):
    for module, target, presentation, kind in required:
        matches = [record for record in records if (record.get("module"), record.get("target"), record.get("presentation"), record.get("kind")) == (module, target, presentation, kind)]
        if len(matches) != 1:
            raise VerificationError(f"{module}/{target}/{presentation}/{kind}: expected one result, found {len(matches)}")
        record = matches[0]
        if kind == "compile":
            evidence = record.get("evidence", [])
            if record.get("status") != "passed" or not evidence:
                raise VerificationError(f"{module}/{target}: missing successful compile evidence")
            task = "compileKotlin" + target[0].upper() + target[1:]
            logs = "\n".join(Path(path).read_text() for path in evidence)
            if "BUILD SUCCESSFUL" not in logs or f":{module}:{task}" not in logs:
                raise VerificationError(f"{module}/{target}: compile task did not complete")
            if re.search(rf":{re.escape(module)}:{re.escape(task)} (NO-SOURCE|SKIPPED|FAILED)", logs):
                raise VerificationError(f"{module}/{target}: no actual compilation evidence")
            continue
        if kind != "gpu" or record.get("adapterRequired") is not True or not record.get("backend"):
            raise VerificationError(f"{module}/{target}: GPU execution needs a required adapter and backend")
        reports = record.get("junit", [])
        if not reports:
            raise VerificationError(f"{module}/{target}: missing JUnit reports")
        names = set()
        output = []
        for path in reports:
            root = ET.parse(path).getroot()
            for suite in root.iter("testsuite"):
                if any(int(suite.get(key, "0")) for key in ("failures", "errors", "skipped")):
                    raise VerificationError(f"{module}/{target}: failed or skipped suite: {path}")
            for case in root.iter("testcase"):
                if any(case.find(key) is not None for key in ("failure", "error", "skipped")):
                    raise VerificationError(f"{module}/{target}: failed or skipped test: {case.get('name')}")
                names.add(case.get("name", "").split("[")[0].split("(")[0].rsplit(".", 1)[-1])
            output += [node.text or "" for node in root.iter("system-out")]
        expected = GPU_TESTS.get(module, {"liveWindowRendersAndCloses" if target.startswith("jvm-") else "nativeWindowUsesSameLifecycleContract"})
        if not expected <= names:
            raise VerificationError(f"{module}/{target}: missing GPU cases {sorted(expected - names)}")
        output += [Path(path).read_text() for path in record.get("stdout", [])]
        marker = "TOOLKIT_GLFW" if module == MODULES[2] else "TOOLKIT_GPU"
        if not any(marker in line and "required=1" in line and f"backend={record['backend']}" in line for text in output for line in text.splitlines()):
            raise VerificationError(f"{module}/{target}: missing real adapter execution marker")
        if module != MODULES[0] and not any(f"TOOLKIT_PRESENTATION kind={presentation}" in line for text in output for line in text.splitlines()):
            raise VerificationError(f"{module}/{target}: missing actual {presentation} presentation witness")

def resolve_report_paths(record, base):
    """Normalize one record's evidence paths against its report manifest location."""
    record = dict(record)
    for key in ("junit", "stdout", "evidence"):
        portable = [item.replace("\\", "/") for item in record.get(key, [])]
        record[key] = [str((base / item).resolve()) if not Path(item).is_absolute() else item for item in portable]
    return record

def required_suites():
    requirements = []
    desktop = ["jvm-macosArm64", "jvm-linuxX64", "jvm-linuxArm64", "jvm-mingwX64", "macosArm64", "linuxX64"]
    mobile = ["android", "iosSimulatorArm64", "iosX64", "tvosSimulatorArm64"]
    for module in MODULES:
        for target in desktop + ([] if module == MODULES[2] else mobile):
            surfaces = ["headless"] if module == MODULES[0] else (
                ["x11", "wayland"] if "linux" in target else
                ["android"] if target == "android" else
                ["win32"] if "mingw" in target else ["metal"])
            requirements += [(module, target, surface, "gpu") for surface in surfaces]
        if module != MODULES[2]:
            requirements += [(module, target, "none", "compile") for target in ["iosArm64", "tvosArm64"]]
    return requirements

def self_test():
    """Relay --self-test to the fixtures moved into scripts/tests/; CI still calls this."""
    path = Path(__file__).with_name("tests") / "test_toolkit_coverage.py"
    spec = importlib.util.spec_from_file_location("test_toolkit_coverage", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    suite = unittest.defaultTestLoader.loadTestsFromModule(module)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    sys.exit(0 if result.wasSuccessful() else 1)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--report", action="append", type=Path, default=[])
    parser.add_argument("--require", action="append", default=[], help="module:target:presentation:gpu|compile; defaults to the full CI matrix")
    parser.add_argument("--metadata-root", type=Path)
    parser.add_argument("--version", default="1.0.0-SNAPSHOT")
    args = parser.parse_args()
    if args.self_test:
        self_test()
    if args.metadata_root:
        documents = {}
        for module in MODULES:
            path = args.metadata_root / "org/graphiks" / module / args.version / f"{module}-{args.version}.module"
            documents[module] = json.loads(path.read_text())
        verify_metadata(documents)
        verify_runtime_archives(args.metadata_root, args.version)
        counts = " ".join(f"{module.rsplit('-', 1)[-1]}={len(targets)}" for module, targets in DECLARED.items())
        print(f"Published Kotlin variants: {counts}")
        print("Verified four JVM runtimes, both Android ABIs and JNI consumer keep rules")
    if args.report:
        records = []
        for path in args.report:
            manifest = json.loads(path.read_text())
            if manifest.get("schema") != 1: raise VerificationError(f"unsupported coverage schema: {path}")
            for record in manifest["results"]:
                records.append(resolve_report_paths(record, path.parent))
        required = [tuple(value.split(":")) for value in args.require] or required_suites()
        verify_coverage(records, required)
        print(f"Verified {len(required)} required execution/compile suites")
    elif not args.metadata_root:
        parser.error("provide --report, --metadata-root or --self-test")

if __name__ == "__main__":
    main()
