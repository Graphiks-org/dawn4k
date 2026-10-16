"""Verify foreign Linux bundles on any host without a compiler or GPU."""
import hashlib
import importlib.util
import json
from pathlib import Path, PureWindowsPath
import sys
import tempfile
import unittest
from unittest.mock import patch


class WindowsRelativePath(type(Path())):
    """Use Windows manifest lookup paths while retaining real local file I/O."""
    def relative_to(self, *args, **kwargs):
        return PureWindowsPath(super().relative_to(*args, **kwargs))


class DawnLinuxBundleHostTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        script = Path(__file__).parents[1] / "build-dawn-linux-bundle.py"
        spec = importlib.util.spec_from_file_location("bundle_host", script)
        self.bundle = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.bundle)
        self.prepared = self.root / "prepared"
        self.source = self.root / "bundle"
        self.output = self.root / "output"
        self.source.mkdir()
        self.lock = self.root / "lock.json"
        dawn = {"tag": "fixture", "revision": "fixture-revision"}
        self.lock.write_text(json.dumps(dawn))
        contents = {"include/dawn/webgpu.h": b"verified header", "lib/libwebgpu_dawn.so": b"verified ELF"}
        artifacts = []
        for name, data in contents.items():
            file = self.prepared / name
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_bytes(data)
            artifacts.append({"path": name, "sha256": hashlib.sha256(data).hexdigest()})
        (self.prepared / "manifest.json").write_text(json.dumps({
            "dawn": dawn, "target": {"kotlinTarget": "linuxX64"},
            "linkage": "shared", "artifacts": artifacts,
        }))
        self.archive = b"verified foreign archive"
        (self.source / "libdawn4k_dawn.a").write_bytes(self.archive)
        (self.source / "bundle.json").write_text(json.dumps({
            "target": "linuxX64", "revision": dawn["revision"],
            "headerSha256": artifacts[0]["sha256"],
            "librarySha256": artifacts[1]["sha256"],
            "generatorSha256": hashlib.sha256(script.read_bytes()).hexdigest(),
            "archiveSha256": hashlib.sha256(self.archive).hexdigest(),
        }))

    def verify(self, path_type=Path):
        argv = ["build-dawn-linux-bundle.py", "--prepared", str(self.prepared),
                "--lock", str(self.lock), "--bundle", str(self.source), "--output", str(self.output)]
        with patch.object(sys, "argv", argv), patch.object(self.bundle, "Path", path_type):
            self.bundle.main()

    def assert_copied_bundle(self):
        self.assertEqual(self.archive, (self.output / "libdawn4k_dawn.a").read_bytes())
        self.assertEqual((self.source / "bundle.json").read_bytes(), (self.output / "bundle.json").read_bytes())

    def test_native_host_verifies_and_copies_foreign_bundle(self):
        self.verify()
        self.assert_copied_bundle()

    def test_windows_host_verifies_and_copies_posix_manifest_bundle(self):
        self.verify(WindowsRelativePath)
        self.assert_copied_bundle()

    def test_windows_host_rejects_modified_prepared_library(self):
        (self.prepared / "lib/libwebgpu_dawn.so").write_bytes(b"modified ELF")
        with self.assertRaisesRegex(ValueError, "Prepared Dawn checksum mismatch"):
            self.verify(WindowsRelativePath)
        self.assertFalse(self.output.exists())


if __name__ == "__main__":
    unittest.main()
