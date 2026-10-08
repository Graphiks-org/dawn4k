import hashlib
import io
from pathlib import Path
import tarfile
import tempfile
import unittest

from qualification_runtime import verify_archive, stage_runtime


class QualificationRuntimeTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)

    def archive(self, members):
        archive = self.root / "candidate.tar.gz"
        with tarfile.open(archive, "w:gz") as output:
            for name, data in members:
                info = tarfile.TarInfo(name)
                info.size = len(data)
                info.mode = 0o755 if name.endswith("bin/java") else 0o644
                output.addfile(info, io.BytesIO(data))
        return archive, hashlib.sha512(archive.read_bytes()).hexdigest()

    def test_corrupt_archive_does_not_replace_existing_runtime(self):
        archive = self.root / "corrupt.tar.gz"
        archive.write_bytes(b"corrupt")
        destination = self.root / "runtime"
        destination.mkdir()
        (destination / "keep").write_text("existing")
        with self.assertRaisesRegex(ValueError, "checksum"):
            verify_archive(archive, "0" * 128)
        self.assertEqual("existing", (destination / "keep").read_text())

    def test_staging_verifies_checksum_before_creating_runtime(self):
        archive, _ = self.archive([("jbr/bin/java", b"java"), ("jbr/release", b"JAVA_VERSION=25")])
        destination = self.root / "runtime"
        with self.assertRaisesRegex(ValueError, "checksum"):
            stage_runtime(archive, destination, expected_sha512="0" * 128)
        self.assertFalse(destination.exists())

    def test_rejects_traversal_and_absolute_members_without_writing_outside(self):
        for name in ("../escaped", "/escaped"):
            archive, digest = self.archive([(name, b"bad"), ("jbr/bin/java", b"java"), ("jbr/release", b"25")])
            with self.assertRaises((ValueError, tarfile.FilterError)):
                stage_runtime(archive, self.root / "runtime", expected_sha512=digest)
            self.assertFalse((self.root / "runtime").exists())
            self.assertFalse((self.root / "escaped").exists())

    def test_rejects_symlink_escape(self):
        archive = self.root / "candidate.tar.gz"
        with tarfile.open(archive, "w:gz") as output:
            link = tarfile.TarInfo("jbr/link")
            link.type = tarfile.SYMTYPE
            link.linkname = "/etc"
            output.addfile(link)
        digest = hashlib.sha512(archive.read_bytes()).hexdigest()
        with self.assertRaises((ValueError, tarfile.FilterError)):
            stage_runtime(archive, self.root / "runtime", expected_sha512=digest)
        self.assertFalse((self.root / "runtime").exists())

    def test_valid_runtime_is_promoted_with_executable_java(self):
        archive, digest = self.archive([("jbr/bin/java", b"#!/bin/sh\nexit 0\n"), ("jbr/release", b"JAVA_VERSION=25")])
        destination = self.root / "runtime"
        verify_archive(archive, digest)
        self.assertEqual(destination, stage_runtime(archive, destination, expected_sha512=digest))
        self.assertEqual(b"JAVA_VERSION=25", (destination / "release").read_bytes())
        self.assertTrue((destination / "bin/java").stat().st_mode & 0o111)

    def test_missing_runtime_markers_cannot_be_promoted(self):
        archive, digest = self.archive([("jbr/readme", b"not a runtime")])
        with self.assertRaisesRegex(ValueError, "bin/java.*release"):
            stage_runtime(archive, self.root / "runtime", expected_sha512=digest)
        self.assertFalse((self.root / "runtime").exists())

    def test_valid_archive_cannot_replace_existing_directory(self):
        archive, digest = self.archive([("jbr/bin/java", b"java"), ("jbr/release", b"25")])
        destination = self.root / "runtime"
        destination.mkdir()
        (destination / "keep").write_text("existing")
        with self.assertRaises(FileExistsError):
            stage_runtime(archive, destination, expected_sha512=digest)
        self.assertEqual("existing", (destination / "keep").read_text())


if __name__ == "__main__":
    unittest.main()
