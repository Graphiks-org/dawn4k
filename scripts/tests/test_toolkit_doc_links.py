import importlib.util
from pathlib import Path
import tempfile
import unittest


spec = importlib.util.spec_from_file_location("toolkit_doc_links", Path(__file__).parents[1] / "normalize-toolkit-doc-links.py")
links = importlib.util.module_from_spec(spec)
spec.loader.exec_module(links)


class GeneratedApiLinks(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name) / "api"
        self.package = self.root / "dawn4k/org.graphiks.dawn4k"
        self.package.mkdir(parents=True)
        self.page = self.package / "index.md"
        self.page.write_text("# Package\n")
        self.constructor = "../../dawn4k/org.graphiks.dawn4k/-dawn-config/--root--.md"
        self.private = "../../dawn4k/org.graphiks.dawn4k.internal/-dawn-runtime/index.md"
        self.class_page = self.package / "-dawn-config/index.md"
        self.class_page.parent.mkdir()
        self.class_page.write_text("# DawnConfig\n## Constructors\n")

    def test_inline_constructor_points_to_its_existing_class_page(self):
        self.assertEqual(self.constructor.replace("--root--", "index"), links.rewrite_target(self.root, self.page, self.constructor))

    def test_html_constructor_link_uses_the_same_class_destination(self):
        target = self.constructor.replace(".md", ".html") + "#old-constructor-anchor"
        self.assertEqual(self.constructor.replace("--root--.md", "index.html"), links.rewrite_target(self.root, self.page, target))

    def test_known_non_public_type_is_plain_text(self):
        self.assertIsNone(links.rewrite_target(self.root, self.page, self.private))

    def test_existing_private_named_page_keeps_its_link(self):
        path = self.root / "dawn4k/org.graphiks.dawn4k.internal/-dawn-runtime/index.md"
        path.parent.mkdir(parents=True)
        path.write_text("# Explicitly documented type\n")
        self.assertEqual(self.private, links.rewrite_target(self.root, self.page, self.private))

    def test_unknown_missing_public_link_is_preserved_for_strict_validation(self):
        target = "-missing-public-type/index.md"
        self.assertEqual(target, links.rewrite_target(self.root, self.page, target))

    def test_constructor_without_a_class_page_is_preserved(self):
        target = "-missing-public-type/--root--.md"
        self.assertEqual(target, links.rewrite_target(self.root, self.page, target))

    def test_external_and_outside_api_links_are_untouched(self):
        targets = ["https://example.org/--root--.md", "../../../outside/--root--.md"]
        outside = self.root.parent / "outside/index.md"
        outside.parent.mkdir()
        outside.write_text("# Outside\n")
        for target in targets:
            self.assertEqual(target, links.rewrite_target(self.root, self.page, target))

    def test_markdown_and_html_keep_labels_and_are_idempotent(self):
        text = f'[DawnConfig]({self.constructor}) and [`DawnRuntime`]({self.private})\n'
        text += f'<a href="{self.private.replace(".md", ".html")}"><span>DawnRuntime</span></a>'
        expected = f'[DawnConfig]({self.constructor.replace("--root--", "index")}) and `DawnRuntime`\n<span>DawnRuntime</span>'
        normalized = links.normalize_markdown(self.root, self.page, text)
        self.assertEqual(expected, normalized)
        self.assertEqual(normalized, links.normalize_markdown(self.root, self.page, normalized))


if __name__ == "__main__":
    unittest.main(verbosity=2)
