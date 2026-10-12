# Native Dawn binding

`dawn4k-native` is the low-level binding to Google's [Dawn](https://dawn.googlesource.com/dawn)
WebGPU implementation. It exposes the raw C API in the package
`org.graphiks.dawn4k.native` and depends only on
[kffi](https://central.sonatype.com/artifact/org.graphiks/kffi); it does **not**
depend on the higher-level `webgpu-api` module.

## Layers

| Layer | Location | Role |
| --- | --- | --- |
| Raw C API | `dawn4k-native/generated/src` | kextract output; never edited by hand |
| JVM bootstrap | generated `KextractNativeBootstrap` | extracts and loads the bundled shared library |
| Kotlin/Native cinterop | `dawn4k-native/src/nativeInterop/cinterop/dawn.def` | internal `webgpu.native` package linked against the static library |

The generated sources are versioned. Ordinary compilation consumes them and never
runs kextract.

## Generation

Inputs are pinned:

- `bindings/dawn.lock.json` — Dawn release `v8077.0.0`, tag `chromium/8077`,
  revision `531028367c60ce07251ec0231c1b95bedb4495bc` and the SHA-256 of each archive.
- `bindings/kextract.version` — the kextract commit used to generate.
- `bindings/callback-bindings.yml` — the validated callback contracts.

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
- **Windows/JVM** uses the pinned `mingwX64/shared/bin/webgpu_dawn.dll` and
  its bundled MSVC runtime DLLs as external libraries. Gradle JVM tests and
  demo execution set `java.library.path` and `PATH` to the verified directory.
  The demo distribution ships them under `lib/native/windows`; its Windows
  launcher configures both paths. The generated raw sources remain unchanged.
  Dawn contexts preload the system shader compiler from Windows `System32`.
- **Kotlin/Native** links the static library (`libwebgpu_dawn.a`) plus the platform
  system libraries: the Apple frameworks (`Metal`, `Foundation`, `CoreGraphics`,
  `QuartzCore`, `IOKit`, `IOSurface`) for macOS, iOS and tvOS (device + simulator),
  or Linux `-lpthread -ldl -lm`.
- **Android/JVM** uses kffi's Android engine; `:dawn4k-native:extractAndroidNativeLibs`
  extracts the verified shared libraries into the git-ignored
  `dawn4k-native/src/androidMain/jniLibs/<abi>/` (`arm64-v8a`, `x86_64`) before
  Android packaging. Signatures outside kffi-android's fixed-shape baseline ride
  the generic `callGeneric` path.

Not yet available, pending upstream work:

- **Kotlin/Native `androidNative*`** — the dawn-packer Android archives are built
  with a newer libc++ than Kotlin/Native links, so the K/N Android targets do not
  link (see dawn-packer's `docs/spikes/android.md`).

## Generated layout report

```bash
./gradlew :dawn4k-native:dumpGeneratedAbi
```

writes sizes, alignments and field offsets from the generated JVM sources to
`dawn4k-native/build/reports/abi/generated.json`. This report does not independently
verify the layouts against the native C header.
