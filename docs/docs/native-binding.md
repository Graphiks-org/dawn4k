# Native Dawn binding

`dawn4k-native` is the low-level binding to Google's [Dawn](https://dawn.googlesource.com/dawn)
WebGPU implementation. It exposes the raw C API in the package
`org.graphiks.dawn4k.raw` and depends only on
[kffi](https://central.sonatype.com/artifact/org.graphiks/kffi); it does **not**
depend on the higher-level `webgpu-api` module.

## Layers

| Layer | Location | Role |
| --- | --- | --- |
| Raw C API | `dawn4k-native/generated/src` | kextract output; never edited by hand |
| JVM bootstrap | generated `KextractNativeBootstrap` | extracts and loads the bundled shared library |
| Kotlin/Native cinterop | `dawn4k-native/src/nativeInterop/cinterop/dawn.def` | internal `webgpu.native` package linked against the static library |
| ABI oracle | `tests/abi/dawn_abi.c` | measures the real C layout |
| Smoke | `native-smoke` | creates a real adapter, device and buffer |

The generated sources are versioned. Ordinary compilation consumes them and never
runs kextract.

## Generation

Inputs are pinned:

- `bindings/dawn.lock.json` — Dawn release `v8077.0.0`, tag `chromium/8077`,
  revision `531028367c60ce07251ec0231c1b95bedb4495bc` and the SHA-256 of each archive.
- `bindings/kextract.version` — the kextract commit used to generate.
- `bindings/callback-bindings.yml` — the validated callback contracts.
- `bindings/generation.json` — provenance (hashes, JVM bundles, callback inventory).

Reproduce the generated sources with:

```bash
bash scripts/generate-dawn-bindings.sh
```

The script builds the pinned kextract in a dedicated, git-ignored checkout, then
regenerates into `dawn4k-native/build/regenerated/src`. Review that diff and promote
it into `dawn4k-native/generated/src`. `:dawn4k-native:generateBindingsFromHeader`
is never part of the ordinary compile graph.

## Native libraries

`./gradlew :dawn4k-native:prepareDawn` downloads, verifies and extracts the archives
under `dawn4k-native/build/native/<target>/<linkage>/`. Extraction refuses absolute
paths, traversal, non file/directory entries, checksum drift and manifest
inconsistency.

- **JVM** links the shared library at runtime. Both bundles ship in the JAR:
  `darwin-aarch64/libwebgpu_dawn.dylib` and `linux-x86-64/libwebgpu_dawn.so`. The
  generated bootstrap verifies their SHA-256 and extracts them to a cache directory.
  Run with `--enable-native-access=ALL-UNNAMED`.
- **Kotlin/Native** links the static library (`libwebgpu_dawn.a`) plus the platform
  system libraries: the Apple frameworks (`Metal`, `Foundation`, `CoreGraphics`,
  `QuartzCore`, `IOKit`, `IOSurface`) or Linux `-lpthread -ldl -lm`.
- **Android/JVM** uses kffi's Android engine; the verified shared libraries are
  bundled under `dawn4k-native/src/androidMain/jniLibs/<abi>/` (`arm64-v8a`,
  `x86_64`). Signatures outside kffi-android's fixed-shape baseline ride the
  generic `callGeneric` path.

## ABI verification

```bash
./gradlew :dawn4k-native:verifyDawnAbi
```

compiles `tests/abi/dawn_abi.c` against the original header, runs it, and compares
sizes, alignments and field offsets with the layout baked into the generated JVM
bindings. `dumpGeneratedAbi` writes the same schema from the generated sources.
Reports land in `dawn4k-native/build/reports/abi/`.

## Smoke status

```bash
./gradlew :native-smoke:runJvmSmoke
./gradlew :native-smoke:runSmokeMacosArm64
./gradlew :native-smoke:runSmokeLinuxX64
```

Each task creates a real instance, requests an adapter and device through
`AllowProcessEvents` registrations, creates a 16-byte mapped buffer, writes four
`uint32` values and releases everything. The process exits non-zero on a missing
adapter/device, a timeout or an invalid result; a timeout is never a skip.

Verified on macOS ARM64 (Metal): the JVM and the static Kotlin/Native executables
both report `status: passed`, `callbackCount: 2`, `bufferSize: 16`. The Linux tasks
must run on a Linux x64 host with a Vulkan driver; a Linux Kotlin/Native executable
cannot be produced by the macOS toolchain because its sysroot predates the glibc
symbols the Linux Dawn archive references. The Android/JVM target is verified by
compilation (`:dawn4k-native:compileAndroidMain`); an Android runtime smoke needs an
emulator or device and is not part of CI.
