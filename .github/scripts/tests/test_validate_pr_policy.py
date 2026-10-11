import importlib.util
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[3]
spec = importlib.util.spec_from_file_location(
    "validate_pr_policy", ROOT / ".github/scripts/validate_pr_policy.py"
)
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


class CommitPolicyTest(unittest.TestCase):
    def setUp(self):
        self.policy = validator.load_policy(ROOT / ".github/contributing-policy.toml")

    def test_accepts_approved_scopes(self):
        for scope in ("ci", "build", "test"):
            with self.subTest(scope=scope):
                self.assertEqual([], validator.validate_commit_subjects(
                    [f"fix({scope}): correct configuration"], self.policy
                ))

    def test_accepts_git_revert_of_valid_commit(self):
        self.assertEqual([], validator.validate_commit_subjects(
            ['Revert "fix(dawn4k): remove deprecation markers from unreleased API"'],
            self.policy,
        ))

    def test_revert_does_not_bypass_original_commit_validation(self):
        for subject in (
            'Revert "invalid message"',
            'Revert "fix(native): invalid scope"',
            'Revert "unknown(ci): invalid type"',
            'Revert "fix(ci): missing closing quote',
        ):
            with self.subTest(subject=subject):
                self.assertTrue(validator.validate_commit_subjects([subject], self.policy))

    def test_rejects_unapproved_scopes(self):
        for scope in ("native", "tests"):
            with self.subTest(scope=scope):
                self.assertTrue(validator.validate_commit_subjects(
                    [f"fix({scope}): unsupported scope"], self.policy
                ))

    def test_pr_title_must_still_be_conventional(self):
        self.assertTrue(validator.validate_title('Revert "fix(ci): configuration"', self.policy))


if __name__ == "__main__":
    unittest.main()
