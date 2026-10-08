#!/usr/bin/env python3
"""Stage the locked qualification JVM without changing a running desktop JVM."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import tarfile
import tempfile
from urllib.request import urlopen

LOCK = Path(__file__).resolve().parents[1] / "qualification/jbr-arm64.json"


def verify_archive(path: Path, expected_sha512: str) -> None:
    if not re.fullmatch(r"[0-9a-f]{128}", expected_sha512):
        raise ValueError("invalid SHA-512 checksum")
    digest = hashlib.sha512()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    if digest.hexdigest() != expected_sha512:
        raise ValueError(f"runtime checksum mismatch: {digest.hexdigest()}")


def stage_runtime(archive: Path, destination: Path, expected_sha512: str | None = None) -> Path:
    digest = expected_sha512 or json.loads(LOCK.read_text())["sha512"]
    verify_archive(archive, digest)
    destination = destination.absolute()
    if destination.exists() or destination.is_symlink():
        raise FileExistsError(f"runtime destination already exists: {destination}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix=".jbr-staging-", dir=destination.parent) as temporary:
        root = Path(temporary)
        with tarfile.open(archive, "r:gz") as source:
            members = source.getmembers()
            for member in members:
                path = Path(member.name)
                if path.is_absolute() or ".." in path.parts:
                    raise ValueError(f"unsafe runtime archive path: {member.name}")
            source.extractall(root, members=members, filter="data")
        candidates = [p for p in root.iterdir() if p.is_dir() and not p.is_symlink()
                      and (p / "bin/java").is_file() and (p / "release").is_file()]
        if len(candidates) != 1 or not os.access(candidates[0] / "bin/java", os.X_OK):
            raise ValueError("runtime archive must contain executable bin/java and release")
        # A concurrent installer must not replace an already-created directory.
        if destination.exists() or destination.is_symlink():
            raise FileExistsError(f"runtime destination already exists: {destination}")
        candidates[0].rename(destination)
    return destination


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    lock = json.loads(LOCK.read_text())
    args.destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix=".jbr-download-", dir=args.destination.parent) as temporary:
        archive = Path(temporary) / "candidate.tar.gz"
        with urlopen(lock["url"], timeout=60) as source, archive.open("wb") as target:
            for block in iter(lambda: source.read(1024 * 1024), b""):
                target.write(block)
        print(stage_runtime(archive, args.destination, lock["sha512"]), flush=True)


if __name__ == "__main__":
    main()
