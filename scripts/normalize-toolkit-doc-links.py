#!/usr/bin/env python3
"""Normalize MkDocs links to inline constructors and known non-public backend types."""
import argparse
from pathlib import Path
import re
from urllib.parse import unquote, urlsplit, urlunsplit


# Both classes are internal in the backend. The renderer nevertheless links their
# names when it displays the containing public class's internal constructor.
NON_PUBLIC_PAGES = {
    "dawn4k/org.graphiks.dawn4k.internal/-dawn-runtime/index.md",
    "dawn4k/org.graphiks.dawn4k.internal/-device-session/index.md",
}

MARKDOWN_LINK = re.compile(r"\[([^\]\n]+)\]\(([^\s)]+)\)")
HTML_LINK = re.compile(r"(<a\b[^>]*?\bhref=)([\"'])(.*?)(\2[^>]*>)(.*?)</a>", re.DOTALL)

def rewrite_target(api_root, page, target):
    url = urlsplit(target)
    if url.scheme or url.netloc or not url.path or url.path.startswith("/"):
        return target
    path = Path(unquote(url.path))
    if path.suffix not in (".md", ".html"):
        return target
    root = api_root.resolve()
    source = (page.parent / path).resolve().with_suffix(".md")
    if not source.is_relative_to(root) or source.is_file():
        return target
    if source.relative_to(root).as_posix() in NON_PUBLIC_PAGES:
        return None
    if source.name == "--root--.md" and source.with_name("index.md").is_file():
        # Constructors are rendered inline on the class page, not a separate page.
        return urlunsplit(url._replace(path=path.with_name("index" + path.suffix).as_posix(), fragment=""))
    return target


def normalize_markdown(api_root, page, text):
    def markdown(match):
        label, target = match.groups()
        rewritten = rewrite_target(api_root, page, target)
        return label if rewritten is None else f"[{label}]({rewritten})"

    def html(match):
        before, quote, target, after, label = match.groups()
        rewritten = rewrite_target(api_root, page, target)
        return label if rewritten is None else f"{before}{quote}{rewritten}{after}{label}</a>"

    return HTML_LINK.sub(html, MARKDOWN_LINK.sub(markdown, text))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-root", type=Path, required=True)
    args = parser.parse_args()
    root = args.api_root.resolve()
    if not (root / "index.md").is_file():
        raise SystemExit(f"Missing generated API root: {root}")
    changed = 0
    for page in root.rglob("*.md"):
        text = page.read_text()
        normalized = normalize_markdown(root, page, text)
        if normalized != text:
            page.write_text(normalized)
            changed += 1
    print(f"Normalized generated constructor/non-public type links in {changed} API pages")


if __name__ == "__main__":
    main()
