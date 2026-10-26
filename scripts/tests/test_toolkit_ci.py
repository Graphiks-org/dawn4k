"""Shared CI plumbing regressions: module list, example markers, consumer runner."""
import contextlib
import importlib.util
import io
from pathlib import Path
import stat
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))  # scripts under test import their sibling toolkit_ci module.
spec = importlib.util.spec_from_file_location("toolkit_ci", SCRIPTS / "toolkit_ci.py")
toolkit_ci = importlib.util.module_from_spec(spec)
spec.loader.exec_module(toolkit_ci)


class ToolkitConstants(unittest.TestCase):
    def test_published_module_list_and_example_markers_come_from_one_source(self):
        self.assertEqual(["dawn4k-toolkit-core", "dawn4k-toolkit-surface", "dawn4k-toolkit-glfw"], toolkit_ci.MODULES)
        self.assertEqual("[2, 4, 6, 8]", toolkit_ci.COMPUTE_RESULT)
        self.assertEqual("TOOLKIT_SAMPLE frames=60", toolkit_ci.SAMPLE_FRAMES_MARKER)

    def test_weston_uses_one_headless_argument_list_with_the_canonical_geometry(self):
        arguments = list(toolkit_ci.WESTON_ARGUMENTS)
        for required in ("--backend=headless-backend.so", "--renderer=pixman", "--idle-time=0", "--width=800", "--height=600"):
            self.assertIn(required, arguments)


class RunAndRequire(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)

    def executable(self, code):
        binary = self.root / "consumer.kexe"
        binary.write_text(f"#!{sys.executable}\n{code}\n")
        binary.chmod(0o644)  # run_and_require must make the published binary executable itself.
        return binary

    def run_quietly(self, binary, marker):
        captured = io.StringIO()
        with contextlib.redirect_stdout(captured):
            result = toolkit_ci.run_and_require(binary, marker)
        return result, captured.getvalue()

    def test_successful_run_prints_output_and_passes(self):
        binary = self.executable(f'print("prefix {toolkit_ci.SAMPLE_FRAMES_MARKER} suffix")')
        result, printed = self.run_quietly(binary, toolkit_ci.SAMPLE_FRAMES_MARKER)
        self.assertEqual(0, result.returncode)
        self.assertIn(toolkit_ci.SAMPLE_FRAMES_MARKER, printed)
        self.assertTrue(binary.stat().st_mode & stat.S_IXUSR)

    def test_failing_executable_is_rejected_with_its_stderr_surfaced(self):
        binary = self.executable('import sys; sys.stderr.write("boom\\n"); sys.exit(3)')
        captured = io.StringIO()
        with contextlib.redirect_stdout(captured), self.assertRaises(AssertionError):
            toolkit_ci.run_and_require(binary, toolkit_ci.COMPUTE_RESULT)
        self.assertIn("boom", captured.getvalue())

    def test_executable_without_the_marker_is_rejected(self):
        binary = self.executable('print("ran fine, reported nothing")')
        with self.assertRaises(AssertionError):
            self.run_quietly(binary, toolkit_ci.SAMPLE_FRAMES_MARKER)


if __name__ == "__main__":
    unittest.main(verbosity=2)
