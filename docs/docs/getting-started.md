# Getting Started — dawn4k

How to work on **dawn4k**, the Kotlin Multiplatform binding to Google's Dawn
(WebGPU) C API for desktop (JVM, macOS ARM64, Linux x64).

## Prerequisites

- JDK 25 and the repository Gradle wrapper (`9.6.1`).
- A macOS ARM64 or Linux x64 host.
- A C toolchain (`cc`) for the ABI oracle and the callback helper.
- Optional: LLVM (`brew --prefix llvm`) if you need to rebuild the pinned
  kextract generator.

## Modules

- **`:dawn4k-native`** — generated low-level Dawn bindings (`org.graphiks.dawn4k.raw`),
  the C ABI oracle and the cinterop/linkage configuration. Depends only on kffi.
- **`:native-smoke`** — real consumer creating an instance, adapter, device and a
  mapped buffer on JVM and Kotlin/Native.

See [Native Dawn binding](native-binding.md) for generation, linkage and smoke status.

## Common commands

```bash
./gradlew :dawn4k-native:jvmTest        # fast JVM tests
./gradlew allTests                      # host targets
./gradlew :dawn4k-native:compileAndroidMain  # Android/JVM target compiles
./gradlew :dawn4k-native:verifyDawnAbi  # C ABI oracle vs generated layout
./gradlew :native-smoke:runJvmSmoke     # real GPU smoke (JVM)
./gradlew :native-smoke:runSmokeMacosArm64
./gradlew :native-smoke:runSmokeLinuxX64  # on a Linux + Vulkan host
```

## Regenerating the bindings

```bash
bash scripts/generate-dawn-bindings.sh
```

The script builds the pinned kextract in a dedicated, git-ignored checkout and
regenerates into `dawn4k-native/build/regenerated/src`. Review that diff, then
promote it into the versioned `dawn4k-native/generated/src`. Ordinary compilation
never runs the generator.

## Customizing the build

- `settings.gradle.kts` — `rootProject.name`.
- `build.gradle.kts` — `group` and version.
- `buildSrc/.../kmp-library.gradle.kts` — desktop targets and toolchain.
- `buildSrc/.../kmp-publish.gradle.kts` — publication coordinates and POM fields.
- `buildSrc/.../kmp-dokka.gradle.kts` — Dokka module name and source link.
- `docs/mkdocs.yml` — site metadata.
- `LICENSE` — copyright holder.

## Final verification

- [ ] `./gradlew allTests` succeeds.
- [ ] `./gradlew :dawn4k-native:verifyDawnAbi` succeeds.
- [ ] `./gradlew :native-smoke:runJvmSmoke` (and the K/N smoke on the host) passes.
- [ ] `mkdocs build -f docs/mkdocs.yml` works.
