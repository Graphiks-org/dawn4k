"""Coverage verifier regressions; moved next to the other script tests."""
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
import zipfile

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))  # the verifier imports its sibling toolkit_ci module.
spec = importlib.util.spec_from_file_location("verify_toolkit_coverage", SCRIPTS / "verify-toolkit-coverage.py")
verify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)


def elf(machine):
    return b"\x7fELF\x02\x01" + bytes(12) + machine.to_bytes(2, "little")


def pe_image():
    return b"MZ" + bytes(58) + (64).to_bytes(4, "little") + b"PE\x00\x00" + (0x8664).to_bytes(2, "little")


def macos_dylib():
    return b"\xcf\xfa\xed\xfe" + (0x100000C).to_bytes(4, "little")


class CoverageFixtures(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.required = [(verify.MODULES[0], "tvosSimulatorArm64", "headless", "gpu")]
        self.junit = self.directory / "TEST-core.xml"
        self.stdout = self.directory / "execution.log"
        suite = ET.Element("testsuite", tests="2", failures="0", errors="0", skipped="0")
        for name in sorted(verify.GPU_TESTS[verify.MODULES[0]]): ET.SubElement(suite, "testcase", name=name)
        ET.ElementTree(suite).write(self.junit)
        self.stdout.write_text("TOOLKIT_GPU target=tvosSimulatorArm64 backend=Metal required=1 device=real-test-fixture\n")
        self.record = {"module": verify.MODULES[0], "target": "tvosSimulatorArm64", "presentation": "headless", "kind": "gpu", "backend": "Metal", "adapterRequired": True, "junit": [str(self.junit)], "stdout": [str(self.stdout)]}

    def test_rejectsMissingTarget(self):
        with self.assertRaises(verify.VerificationError): verify.verify_coverage([], self.required)
        verify.verify_coverage([self.record], self.required)

    def test_rejectsJavaKeywordInAndroidManifestPackage(self):
        with self.assertRaises(verify.VerificationError):
            verify.verify_android_manifest_package(b'<manifest package="org.graphiks.dawn4k.native"/>')
        verify.verify_android_manifest_package(b'<manifest package="org.graphiks.dawn4k.bindings"/>')

    def test_windowsReportPathsArePortableAcrossHosts(self):
        nested = self.directory / "windows-module"
        nested.mkdir()
        self.junit.replace(nested / self.junit.name)
        self.stdout.replace(nested / self.stdout.name)
        (nested / self.stdout.name).write_text("TOOLKIT_GPU backend=D3D12 required=1\n")
        report = self.directory / "summary.json"
        record = dict(self.record, target="jvm-mingwX64", backend="D3D12",
                      junit=["windows-module\\TEST-core.xml"], stdout=["windows-module\\execution.log"])
        report.write_text(json.dumps({"schema": 1, "results": [record]}))
        result = subprocess.run([sys.executable, str(SCRIPTS / "verify-toolkit-coverage.py"), "--report", str(report),
                                 "--require", f"{verify.MODULES[0]}:jvm-mingwX64:headless:gpu"], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_reportDirCollectsNestedSummaries(self):
        nested = self.directory / "coverage" / "job"
        nested.mkdir(parents=True)
        report = nested / "summary.json"
        report.write_text(json.dumps({"schema": 1, "results": [self.record]}))
        command = [sys.executable, str(SCRIPTS / "verify-toolkit-coverage.py"), "--report-dir", str(self.directory / "coverage"),
                   "--require", f"{verify.MODULES[0]}:tvosSimulatorArm64:headless:gpu"]
        result = subprocess.run(command, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        missing = subprocess.run(command[:-1] + [f"{verify.MODULES[0]}:jvm-mingwX64:win32:gpu"], capture_output=True, text=True)
        self.assertNotEqual(missing.returncode, 0, "a requirement absent from the directory must fail")

    def test_rejectsSkippedRequiredGpuSuite(self):
        tree = ET.parse(self.junit)
        ET.SubElement(tree.getroot().find("testcase"), "skipped")
        tree.getroot().set("skipped", "1")
        tree.write(self.junit)
        with self.assertRaises(verify.VerificationError): verify.verify_coverage([self.record], self.required)

    def test_distinguishesDeviceCompileFromGpuExecution(self):
        record = dict(self.record, target="tvosArm64", kind="compile", presentation="none")
        with self.assertRaises(verify.VerificationError): verify.verify_coverage([record], self.required)
        with self.assertRaises(verify.VerificationError): verify.verify_coverage([record], [(verify.MODULES[0], "tvosArm64", "none", "gpu")])

    def test_rejectsX11FallbackReportedAsWayland(self):
        suite = ET.Element("testsuite", tests="3", failures="0", errors="0", skipped="0")
        for name in sorted(verify.GPU_TESTS[verify.MODULES[1]]): ET.SubElement(suite, "testcase", name=name)
        ET.ElementTree(suite).write(self.junit)
        self.stdout.write_text("TOOLKIT_GPU backend=Vulkan required=1\nTOOLKIT_PRESENTATION kind=x11\n")
        record = dict(self.record, module=verify.MODULES[1], target="jvm-linuxX64", presentation="wayland", backend="Vulkan")
        with self.assertRaises(verify.VerificationError):
            verify.verify_coverage([record], [(verify.MODULES[1], "jvm-linuxX64", "wayland", "gpu")])
        self.stdout.write_text("TOOLKIT_GPU backend=Vulkan required=1\nTOOLKIT_PRESENTATION kind=wayland\n")
        verify.verify_coverage([record], [(verify.MODULES[1], "jvm-linuxX64", "wayland", "gpu")])

    def test_publishedMetadataContainsOnlyDeclaredVariants(self):
        def metadata(targets):
            reverse = {v: k for k, v in verify.NATIVE_NAMES.items()}
            variants = [{"attributes": {"org.jetbrains.kotlin.platform.type": "common"}}]
            for target in targets:
                attributes = {"org.jetbrains.kotlin.platform.type": "androidJvm" if target == "android" else "jvm" if target == "jvm" else "native"}
                if target in reverse: attributes["org.jetbrains.kotlin.native.target"] = reverse[target]
                variants.append({"attributes": attributes})
            return {"variants": variants}
        documents = {name: metadata(targets) for name, targets in verify.DECLARED.items()}
        verify.verify_metadata(documents)
        documents[verify.MODULES[2]] = metadata(verify.COMMON_TARGETS)
        with self.assertRaises(verify.VerificationError): verify.verify_metadata(documents)
        documents[verify.MODULES[2]] = metadata(verify.DECLARED[verify.MODULES[2]])
        documents[verify.MODULES[0]] = metadata(verify.COMMON_TARGETS - {"tvosSimulatorArm64"})
        with self.assertRaises(verify.VerificationError): verify.verify_metadata(documents)


class WindowsRuntimeArchives(unittest.TestCase):
    """The published manifest is the single source for the Windows dependency set."""

    version = "9.9.9"

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name) / "maven"

    def stage_jvm_jar(self, dependencies, digests=None, unmatched_manifest_names=(), unmanifested_dlls=(), dawn_dll=True):
        path = self.root / "org/graphiks/dawn4k-native-jvm" / self.version / f"dawn4k-native-jvm-{self.version}.jar"
        path.parent.mkdir(parents=True)
        contents = {
            "linux-x86-64/libwebgpu_dawn.so": elf(62),
            "linux-aarch64/libwebgpu_dawn.so": elf(183),
            "darwin-aarch64/libwebgpu_dawn.dylib": macos_dylib(),
        }
        windows = (*dependencies, *unmanifested_dlls)
        if dawn_dll:
            windows = ("webgpu_dawn.dll", *windows)
        for name in windows:
            contents[f"win32-x86-64/{name}"] = pe_image()
        manifest = []
        for name in dependencies:
            digest = (digests or {}).get(name) or hashlib.sha256(contents[f"win32-x86-64/{name}"]).hexdigest()
            manifest.append(f"{name}={digest}")
        manifest += [f"{name}={'0' * 64}" for name in unmatched_manifest_names]
        contents["dawn4k/windows-runtime.properties"] = "\n".join(manifest) + "\n"
        with zipfile.ZipFile(path, "w") as jar:
            for name, data in contents.items():
                jar.writestr(name, data)

    def stage_aar(self, module, library, rules):
        path = self.root / "org/graphiks" / module / self.version / f"{module}-{self.version}.aar"
        path.parent.mkdir(parents=True)
        with zipfile.ZipFile(path, "w") as aar:
            aar.writestr("AndroidManifest.xml", '<manifest package="org.graphiks.dawn4k.dawn"/>')
            for abi, machine in (("arm64-v8a", 183), ("x86_64", 62)):
                aar.writestr(f"jni/{abi}/lib{library}.so", elf(machine))
            aar.writestr("proguard-rules.pro", rules)

    def stage_all(self, dependencies, digests=None, unmatched_manifest_names=(), unmanifested_dlls=(), dawn_dll=True):
        self.stage_jvm_jar(dependencies, digests, unmatched_manifest_names, unmanifested_dlls, dawn_dll)
        self.stage_aar("dawn4k-native-android", "webgpu_dawn",
                       "org.graphiks.dawn4k.native.*Trampoline\npublic static void dispatch(...);\n")
        self.stage_aar("dawn4k-toolkit-surface-android", "dawn4k_toolkit_window",
                       "org.graphiks.dawn4k.toolkit.surface.AndroidNativeWindow\nnative <methods>\n")

    def test_consistent_archives_verify(self):
        self.stage_all(("vcruntime140.dll", "msvcp140_atomic_wait.dll"))
        verify.verify_runtime_archives(self.root, self.version)

    def test_missing_windows_dawn_runtime_is_rejected(self):
        self.stage_all(("vcruntime140.dll", "msvcp140_atomic_wait.dll"), dawn_dll=False)
        with self.assertRaisesRegex(verify.VerificationError, "Missing Windows x64 runtime: webgpu_dawn.dll"):
            verify.verify_runtime_archives(self.root, self.version)

    def test_staged_dll_missing_from_the_manifest_is_rejected(self):
        self.stage_all(("vcruntime140.dll", "msvcp140_atomic_wait.dll"), unmanifested_dlls=("msvcp140.dll",))
        with self.assertRaisesRegex(verify.VerificationError, "does not match the staged DLLs"):
            verify.verify_runtime_archives(self.root, self.version)

    def test_manifest_entry_without_a_staged_dll_is_rejected(self):
        self.stage_all(("vcruntime140.dll",), unmatched_manifest_names=("msvcp140.dll",))
        with self.assertRaisesRegex(verify.VerificationError, "does not match the staged DLLs"):
            verify.verify_runtime_archives(self.root, self.version)

    def test_dependency_checksum_mismatch_is_rejected(self):
        self.stage_all(("vcruntime140.dll", "msvcp140_atomic_wait.dll"),
                       {"vcruntime140.dll": "0" * 64})
        with self.assertRaisesRegex(verify.VerificationError, "checksum mismatch"):
            verify.verify_runtime_archives(self.root, self.version)


if __name__ == "__main__":
    unittest.main(verbosity=2)
