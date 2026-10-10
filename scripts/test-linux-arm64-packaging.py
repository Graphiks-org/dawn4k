#!/usr/bin/env python3
"""Exercise the real Gradle download/staging/JAR path, not its source text."""
import hashlib
import json
from pathlib import Path
import subprocess
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[1]


class LinuxArm64PackagingTest(unittest.TestCase):
    def test_jar_contains_verified_arm64_shared_library(self):
        output = ROOT / "build/linux-arm64-packaging.log"
        output.parent.mkdir(parents=True, exist_ok=True)
        with output.open("w") as log:
            result = subprocess.run(
                [str(ROOT / "gradlew"), ":dawn4k-native:jvmJar",
                 "-Pdawn.targets=linuxArm64", "--console=plain"],
                cwd=ROOT, stdout=log, stderr=subprocess.STDOUT,
            )
        self.assertEqual(0, result.returncode, output.read_text()[-8000:])
        jars = list((ROOT / "dawn4k-native/build/libs").glob("*-jvm*.jar"))
        jars = [jar for jar in jars if not jar.name.endswith("-sources.jar")]
        self.assertEqual(1, len(jars), str(jars))
        with zipfile.ZipFile(jars[0]) as jar:
            library = jar.read("linux-aarch64/libwebgpu_dawn.so")
        self.assertEqual(b"\x7fELF", library[:4])
        self.assertEqual(183, int.from_bytes(library[18:20], "little"), "must be AArch64 ELF")
        extracted = ROOT / "dawn4k-native/build/native/linuxArm64/shared"
        manifest = json.loads((extracted / "manifest.json").read_text())
        # The download task already validates archive/release/manifest hashes.
        self.assertEqual((extracted / "lib/libwebgpu_dawn.so").read_bytes(), library)
        self.assertEqual("linuxArm64", manifest["target"]["kotlinTarget"])
        print("Packaged AArch64 Dawn SHA-256:", hashlib.sha256(library).hexdigest())


if __name__ == "__main__":
    unittest.main()
