#!/usr/bin/env python3
"""Copy real test evidence into a portable, strictly validated execution report."""
import argparse
import importlib.util
import json
from pathlib import Path
import shutil


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--target", required=True)
    parser.add_argument("--presentation", required=True)
    parser.add_argument("--backend", required=True)
    parser.add_argument("--suite", required=True, help="Gradle test task or native compile target")
    parser.add_argument("--module", action="append", required=True)
    parser.add_argument("--log", type=Path, required=True)
    parser.add_argument("--kind", choices=("gpu", "compile"), default="gpu")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(args.log, args.output / "execution.log")
    records = []
    for module in args.module:
        record = {"module": module, "target": args.target, "presentation": args.presentation,
                  "kind": args.kind, "backend": args.backend, "adapterRequired": True}
        if args.kind == "compile":
            record.update(status="passed", evidence=["execution.log"], backend=None, adapterRequired=False)
        else:
            folder = Path(module) / "build"
            candidates = (sorted((folder / "outputs/androidTest-results/connected").rglob("*.xml")) if args.suite == "connectedAndroidDeviceTest"
                          else sorted((folder / "test-results" / args.suite).glob("*.xml")))
            if not candidates:
                raise RuntimeError(f"Missing actual {module}/{args.suite} JUnit reports")
            destination = args.output / module
            destination.mkdir(exist_ok=True)
            record["junit"] = []
            for index, path in enumerate(candidates):
                copied = destination / f"{index}-{path.name}"
                shutil.copyfile(path, copied)
                record["junit"].append(copied.relative_to(args.output).as_posix())
            record["stdout"] = ["execution.log"]
        records.append(record)
    manifest = args.output / "summary.json"
    manifest.write_text(json.dumps({"schema": 1, "results": records}, indent=2) + "\n")
    # Fail the producing job as well as the aggregate job; no empty/skip reports.
    spec = importlib.util.spec_from_file_location("coverage", Path(__file__).with_name("verify-toolkit-coverage.py"))
    verifier = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(verifier)
    resolved = [verifier.resolve_report_paths(record, args.output) for record in records]
    verifier.verify_coverage(resolved, [(r["module"], r["target"], r["presentation"], r["kind"]) for r in records])
    print(f"Verified {len(records)} execution suites: {manifest}")


if __name__ == "__main__":
    main()
