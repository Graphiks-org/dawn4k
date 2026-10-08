#!/usr/bin/env python3
"""Build-functional gate for the demo's actual KMP targets, not source text alone."""
import argparse
from pathlib import Path
import subprocess
import os
import zipfile
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--discovery-only", action="store_true")
    parser.add_argument("--bytecode-only", action="store_true")
    parser.add_argument("--d8", action="store_true")
    parser.add_argument("--report", type=Path, default=Path("build/demo-kmp/functional.log"))
    args = parser.parse_args()
    args.report.parent.mkdir(parents=True, exist_ok=True)
    if args.d8:
        sdk = Path(os.getenv("ANDROID_HOME", str(Path.home() / "Library/Android/sdk")))
        d8 = sdk / "build-tools/37.0.0/d8"
        platforms = [p for p in (sdk / "platforms").glob("android-37*/android.jar") if p.is_file()]
        assert len(platforms) == 1, "D8 requires an unambiguous installed API37 android.jar"
        android = platforms[0]
        assert d8.is_file() and android.is_file(), "D8/API37 prerequisite missing"
        destination = Path("build/demo-kmp/d8")
        destination.mkdir(parents=True, exist_ok=True)
        jars = []
        for module in ("dawn4k", "dawn4k-native", "dawn4k-demo"):
            directory = Path(module) / "build/classes/kotlin/android/main"
            classes = list(directory.rglob("*.class"))
            assert classes, f"no compiled Android bytecode: {module}"
            jar = destination / f"{module}-android.jar"
            with zipfile.ZipFile(jar, "w", zipfile.ZIP_DEFLATED) as output:
                for path in classes:
                    output.write(path, path.relative_to(directory))
            jars.append(str(jar))
        result = subprocess.run([str(d8), "--lib", str(android), "--min-api", "24",
                                 "--output", str(destination), *jars], text=True,
                                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=120)
        (destination / "d8.log").write_text(result.stdout)
        assert result.returncode == 0, f"actual D8 failed; see {destination}/d8.log"
        dex = destination / "classes.dex"
        assert dex.is_file() and dex.read_bytes().startswith(b"dex\n"), "no actual D8 output"
        print("D8 compiled demo/backend/native Android classes; APK/runtime qualification still pending")
        return
    if args.bytecode_only:
        for module in ("dawn4k", "dawn4k-native", "dawn4k-demo"):
            classes = list((Path(module) / "build/classes/kotlin/android/main").rglob("*.class"))
            assert classes, f"no actual Android class output in {module}"
            versions = {int.from_bytes(p.read_bytes()[6:8], "big") for p in classes}
            assert max(versions) <= 61, f"{module} Android bytecode targets desktop Java: {sorted(versions)}"
            print(module, "Android class versions:", sorted(versions))
        return
    tasks = ("compileCommonMainKotlinMetadata", "compileKotlinJvm",
             "compileAndroidMain", "compileKotlinIosArm64", "compileKotlinIosSimulatorArm64",
             "run", "test", "installDist", "runComposeWaylandProbe")
    result = subprocess.run(["./gradlew", ":dawn4k-demo:tasks", "--all", "--console=plain"],
                            text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=600)
    args.report.write_text(result.stdout)
    assert result.returncode == 0, f"Gradle discovery failed; see {args.report}"
    names = {line.split(" ")[0] for line in result.stdout.splitlines() if line and not line.startswith(" ")}
    missing = set(tasks) - names
    assert not missing, f"missing demo KMP/compatibility tasks: {sorted(missing)}"
    if not args.discovery_only:
        compiled = subprocess.run(["./gradlew", *(f":dawn4k-demo:{task}" for task in tasks[:5]),
                                   "--console=plain"], text=True, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, timeout=600)
        with args.report.open("a") as output:
            output.write(compiled.stdout)
        assert compiled.returncode == 0, f"demo platform compilation failed; see {args.report}"
        filtered = subprocess.run(["./gradlew", ":dawn4k-demo:test", "--tests", "*ParticleFrameClockTest*",
                                   "--console=plain"], text=True, stdout=subprocess.PIPE,
                                  stderr=subprocess.STDOUT, timeout=600)
        with args.report.open("a") as output:
            output.write(filtered.stdout)
        assert filtered.returncode == 0, "legacy test alias failed to forward --tests"
        reports = list(Path("dawn4k-demo/build/test-results/jvmTest").glob("TEST-*.xml"))
        assert reports and all("ParticleFrameClockTest" in ET.parse(path).getroot().attrib["name"]
                               for path in reports), "legacy test alias ignored its include filter"
    print("demo KMP build-functional gate passed")


if __name__ == "__main__":
    main()
