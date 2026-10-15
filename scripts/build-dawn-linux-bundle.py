#!/usr/bin/env python3
"""Embed verified Dawn ELF in an old-libc-compatible cinterop static archive.

Build on Linux x86_64; --bundle verifies a previously built bundle on other hosts.
Only this carrier is linked by Kotlin/Native. Dawn's C++ runtime requirements are
checked by dlopen on the consumer host, never by Kotlin/Native's older sysroot.
"""
import argparse
import hashlib
import json
import os
import platform
from pathlib import Path
import re
import shutil
import subprocess


def sha256(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def parse_functions(header):
    declarations = re.findall(r"^WGPU_EXPORT\s+(.+?)\s+(wgpu\w+)\((.*?)\)\s+WGPU_FUNCTION_ATTRIBUTE;", header, re.M)
    exports = re.findall(r"^WGPU_EXPORT[^;]*\b(wgpu\w+)\s*\(", header, re.M)
    if set(exports) != {name for _, name, _ in declarations} or not exports:
        raise ValueError("Unparsed WGPU_EXPORT declaration")
    functions = []
    for result, name, arguments in declarations:
        names = []
        if arguments.strip() not in ("", "void"):
            for argument in arguments.split(","):
                match = re.search(r"\b([a-zA-Z_]\w*)\s*$", argument)
                if not match or "..." in argument or "(" in argument or "[" in argument:
                    raise ValueError(f"Unsupported argument in {name}: {argument}")
                names.append(match.group(1))
        functions.append((result, name, arguments, ", ".join(names)))
    return functions


LOADER = r'''
#define _GNU_SOURCE
#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/syscall.h>
#include <unistd.h>
#include "dawn_header.h"
extern const unsigned char dawn4k_elf_start[], dawn4k_elf_end[];
static pthread_once_t once = PTHREAD_ONCE_INIT;
static void * library;
static char failure[1024];
static void load_library(void) {
    int fd = -1;
    char path[4096];
    int temporary = 0;
#if defined(SYS_memfd_create)
    fd = (int)syscall(SYS_memfd_create, "dawn4k", 1U);
#endif
    if (fd >= 0) {
        snprintf(path, sizeof(path), "/proc/self/fd/%d", fd);
    } else {
        const char *tmp = getenv("TMPDIR");
        if (!tmp || !*tmp) tmp = "/tmp";
        if (snprintf(path, sizeof(path), "%s/dawn4k-XXXXXX", tmp) >= (int)sizeof(path)) {
            snprintf(failure, sizeof(failure), "Dawn runtime: TMPDIR is too long"); return;
        }
        fd = mkstemp(path); temporary = 1;
    }
    if (fd < 0) { snprintf(failure, sizeof(failure), "Dawn runtime: %s", strerror(errno)); return; }
    const unsigned char *data = dawn4k_elf_start;
    size_t remaining = (size_t)(dawn4k_elf_end - dawn4k_elf_start);
    while (remaining) {
        ssize_t written = write(fd, data, remaining);
        if (written < 0 && errno == EINTR) continue;
        if (written <= 0) {
            snprintf(failure, sizeof(failure), "Dawn runtime write: %s", strerror(errno));
            close(fd); if (temporary) unlink(path); return;
        }
        data += written; remaining -= (size_t)written;
    }
    /* Deep binding prevents Dawn's internal API references resolving to our
       forwarding symbols. The handle lives for the process, like System.load. */
    library = dlopen(path, RTLD_NOW | RTLD_LOCAL | RTLD_DEEPBIND);
    if (!library) snprintf(failure, sizeof(failure), "Dawn runtime load: %s", dlerror());
    /* dlopen caches the literal /proc/self/fd/N name. Retain that descriptor
       for the process lifetime so an unrelated later load cannot reuse it and
       accidentally receive Dawn's existing handle. Named temporary files have
       unique paths and can be closed/unlinked after mapping. */
    if (temporary || !library) close(fd);
    if (temporary) unlink(path);
}
'''


def build_carrier(header, library, output):
    output = Path(output)
    output.mkdir(parents=True, exist_ok=True)
    header = Path(header)
    functions = parse_functions(header.read_text())
    # The adjacent full include tree is supplied for the real Dawn header.
    shutil.copyfile(header, output / "dawn_header.h")
    declarations, resolutions, wrappers = [], [], []
    for result, name, arguments, names in functions:
        declarations.append(f"static {result} (*p_{name})({arguments});")
        resolutions.append(f'    p_{name} = ({result} (*)({arguments}))dlsym(library, "{name}");\n'
                           f'    if (!p_{name}) {{ snprintf(failure, sizeof(failure), "Dawn runtime missing {name}"); return; }}')
        returned = "" if result.strip() == "void" else "return "
        wrappers.append(f'{result} {name}({arguments}) {{\n'
                        '    pthread_once(&once, initialize);\n'
                        '    if (failure[0]) { fputs(failure, stderr); abort(); }\n'
                        f'    {returned}p_{name}({names});\n}}')
    source = LOADER + "\n" + "\n".join(declarations) + "\nstatic void initialize(void) {\n    load_library();\n    if (!library) return;\n" + "\n".join(resolutions) + "\n}\n" + (
        'const char * dawn4kDawnLoadError(void) { pthread_once(&once, initialize); return failure[0] ? failure : NULL; }\n'
    ) + "\n".join(wrappers) + "\n"
    (output / "carrier.c").write_text(source)
    # incbin avoids a huge generated C array and retains byte-for-byte ELF identity.
    encoded_path = str(Path(library).resolve()).replace("\\", "\\\\").replace('"', '\\"')
    (output / "payload.S").write_text('.section .rodata\n.balign 16\n.global dawn4k_elf_start, dawn4k_elf_end\ndawn4k_elf_start:\n.incbin "' + encoded_path + '"\ndawn4k_elf_end:\n.section .note.GNU-stack,"",@progbits\n')
    include = header.parent.parent if header.parent.name == "dawn" else header.parent
    compiler = os.environ.get("CC", "cc")
    subprocess.run([compiler, "-c", "-O2", "-fPIC", "-Wall", "-Wextra", "-Werror", "-Wstrict-aliasing=2", "-I" + str(include), str(output / "carrier.c"), "-o", str(output / "carrier.o")], check=True)
    subprocess.run([compiler, "-c", str(output / "payload.S"), "-o", str(output / "payload.o")], check=True)
    subprocess.run([os.environ.get("AR", "ar"), "rcs", str(output / "libdawn4k_dawn.a"), str(output / "carrier.o"), str(output / "payload.o")], check=True)
    symbols = subprocess.check_output([os.environ.get("NM", "nm"), "-g", "--defined-only", str(output / "libdawn4k_dawn.a")], text=True)
    actual = set(re.findall(r"\b(wgpu\w+)$", symbols, re.M))
    if actual != {name for _, name, _, _ in functions}:
        raise ValueError("Carrier export parity mismatch")


def write_identity(output, identity):
    document = {**identity, "archiveSha256": sha256(Path(output) / "libdawn4k_dawn.a")}
    (Path(output) / "bundle.json").write_text(json.dumps(document, indent=2) + "\n")


def verify_bundle(bundle, identity):
    document = json.loads((Path(bundle) / "bundle.json").read_text())
    if any(document.get(key) != value for key, value in identity.items()):
        raise ValueError("Dawn bundle source identity mismatch")
    if document.get("archiveSha256") != sha256(Path(bundle) / "libdawn4k_dawn.a"):
        raise ValueError("Dawn bundle archive checksum mismatch")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--prepared", type=Path, required=True)
    parser.add_argument("--lock", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--bundle", type=Path)
    args = parser.parse_args()
    header = args.prepared / "include/dawn/webgpu.h"
    library = args.prepared / "lib/libwebgpu_dawn.so"
    manifest = json.loads((args.prepared / "manifest.json").read_text())
    lock = json.loads(args.lock.read_text())
    if manifest["dawn"] != {"tag": lock["tag"], "revision": lock["revision"]} or manifest["target"]["kotlinTarget"] != "linuxX64" or manifest["linkage"] != "shared":
        raise ValueError("Prepared Dawn source identity mismatch")
    expected = {item["path"]: item["sha256"] for item in manifest["artifacts"]}
    for file in (header, library):
        if sha256(file) != expected[str(file.relative_to(args.prepared))]:
            raise ValueError(f"Prepared Dawn checksum mismatch: {file}")
    identity = {"target": "linuxX64", "revision": lock["revision"], "headerSha256": sha256(header), "librarySha256": sha256(library), "generatorSha256": sha256(__file__)}
    args.output.mkdir(parents=True, exist_ok=True)
    if args.bundle:
        verify_bundle(args.bundle, identity)
        for name in ("libdawn4k_dawn.a", "bundle.json"):
            shutil.copyfile(args.bundle / name, args.output / name)
    else:
        if platform.system() != "Linux" or platform.machine() not in ("x86_64", "amd64"):
            parser.error("Build on Linux x86_64, or supply --bundle from that host")
        build_carrier(header, library, args.output)
        write_identity(args.output, identity)
    verify_bundle(args.output, identity)
    print(f"Verified relocatable Dawn linuxX64 carrier: {args.output}")


if __name__ == "__main__":
    main()
