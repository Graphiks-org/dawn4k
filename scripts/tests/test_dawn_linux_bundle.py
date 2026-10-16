"""Executable Linux packaging regressions; requires cc/ar/nm, no GPU or Gradle."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


class DawnLinuxBundleTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        spec = importlib.util.spec_from_file_location("bundle", Path(__file__).parents[1] / "build-dawn-linux-bundle.py")
        self.bundle = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.bundle)
        self.header = self.root / "fake.h"
        self.header.write_text("""typedef struct { long a, b, c; } Result;
typedef long (*Callback)(long);
#define WGPU_EXPORT
#define WGPU_FUNCTION_ATTRIBUTE
WGPU_EXPORT Result wgpuResult(long value, Callback callback) WGPU_FUNCTION_ATTRIBUTE;
WGPU_EXPORT void wgpuObserve(long * value) WGPU_FUNCTION_ATTRIBUTE;
""")

    def run_cc(self, *args):
        subprocess.run([os.environ.get("CC", "cc"), "-Wall", "-Wextra", "-Werror", *map(str, args)], check=True)

    def build(self, code):
        src = self.root / "vendor.c"
        src.write_text('#include "fake.h"\n' + code)
        library = self.root / "vendor.so"
        self.run_cc("-shared", "-fPIC", src, "-o", library)
        output = self.root / "bundle"
        self.bundle.build_carrier(self.header, library, output)
        return output

    def consumer(self, output, code):
        src = self.root / "consumer.c"
        src.write_text('#include "fake.h"\n#include <pthread.h>\n#include <stdio.h>\n#include <string.h>\n#include <assert.h>\nextern const char * dawn4kDawnLoadError(void);\n' + code)
        binary = self.root / "consumer"
        self.run_cc(src, output / "libdawn4k_dawn.a", "-ldl", "-lpthread", "-o", binary)
        result = subprocess.run([str(binary)], capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stderr)
        return result.stdout

    def test_typed_struct_callback_concurrent_forwarding_and_relocation(self):
        output = self.build("Result wgpuResult(long n, Callback cb) { return (Result){n, cb(n), n+2}; }\nvoid wgpuObserve(long *n) { *n += 3; }")
        (self.root / "vendor.so").unlink()
        stdout = self.consumer(output, """static long callback(long n) { return n * 2; }
static void * worker(void * unused) {
    (void)unused;
    for (long i = 0; i < 1000; ++i) {
        Result r = wgpuResult(i, callback); long n = i; wgpuObserve(&n);
        assert(r.a == i && r.b == i*2 && r.c == i+2 && n == i+3);
    }
    return NULL;
}
int main(void) {
    pthread_t threads[8];
    for (int i=0; i<8; ++i) assert(!pthread_create(&threads[i], NULL, worker, NULL));
    for (int i=0; i<8; ++i) assert(!pthread_join(threads[i], NULL));
    assert(!dawn4kDawnLoadError()); puts("portable forwarding passed");
}
""")
        self.assertIn("portable forwarding passed", stdout)

    def test_missing_export_is_reported_before_first_downcall(self):
        output = self.build("Result wgpuResult(long n, Callback cb) { return (Result){n, cb(n), n}; }")
        self.assertIn("wgpuObserve", self.consumer(output, 'int main(void) { const char *e=dawn4kDawnLoadError(); assert(e); puts(e); }'))

    def test_later_proc_fd_load_cannot_alias_the_dawn_handle(self):
        output = self.build("Result wgpuResult(long n, Callback cb) { return (Result){n, cb(n), n}; }\nvoid wgpuObserve(long *n) { *n += 3; }")
        probe = self.root / "probe.c"
        probe.write_text("long probeIdentity(void) { return 42; }\n")
        library = self.root / "probe.so"
        self.run_cc("-shared", "-fPIC", probe, "-o", library)
        code = r'''
#include <dlfcn.h>
#include <fcntl.h>
#include <unistd.h>
int main(void) {
    assert(!dawn4kDawnLoadError());
    int fd = open(LIBRARY, O_RDONLY); assert(fd >= 0);
    char path[100]; snprintf(path, sizeof(path), "/proc/self/fd/%d", fd);
    void *probe = dlopen(path, RTLD_NOW); assert(probe);
    long (*identity)(void) = (long (*)(void))dlsym(probe, "probeIdentity");
    assert(identity && identity() == 42);
    dlclose(probe); close(fd); puts("independent proc-fd library passed");
}
'''.replace("LIBRARY", json.dumps(str(library)))
        self.assertIn("independent proc-fd library passed", self.consumer(output, code))

    def test_invalid_elf_is_reported_without_aborting_bootstrap(self):
        library = self.root / "invalid.so"
        library.write_bytes(b"invalid ELF")
        output = self.root / "bundle"
        self.bundle.build_carrier(self.header, library, output)
        self.assertIn("Dawn", self.consumer(output, 'int main(void) { const char *e=dawn4kDawnLoadError(); assert(e); puts(e); }'))

    def test_unparsed_export_is_rejected(self):
        self.header.write_text(self.header.read_text() + '\nWGPU_EXPORT int wgpuVariadic(int n, ...) WGPU_FUNCTION_ATTRIBUTE;\n')
        with self.assertRaisesRegex(ValueError, "wgpuVariadic"):
            self.bundle.parse_functions(self.header.read_text())

    def test_modified_bundle_is_rejected(self):
        output = self.build("Result wgpuResult(long n, Callback cb) { return (Result){n, cb(n), n}; }\nvoid wgpuObserve(long *n) { *n += 3; }")
        identity = {"target": "linuxX64", "headerSha256": "header", "librarySha256": "library", "generatorSha256": "generator"}
        self.bundle.write_identity(output, identity)
        self.bundle.verify_bundle(output, identity)
        with (output / "libdawn4k_dawn.a").open("ab") as stream:
            stream.write(b"modified")
        with self.assertRaisesRegex(ValueError, "checksum"):
            self.bundle.verify_bundle(output, identity)

    def test_generated_function_pointers_pass_strict_aliasing_diagnostics(self):
        output = self.build("Result wgpuResult(long n, Callback cb) { return (Result){n, cb(n), n}; }\nvoid wgpuObserve(long *n) { *n += 3; }")
        self.run_cc("-fsyntax-only", "-O2", "-Wstrict-aliasing=2", output / "carrier.c")


if __name__ == "__main__":
    unittest.main()
