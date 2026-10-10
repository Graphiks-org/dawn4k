# dawn4k — Dawn (WebGPU) desktop bindings

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-purple?logo=kotlin)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.8.0-blue?logo=gradle)](https://gradle.org)
[![Dawn](https://img.shields.io/badge/Dawn-chromium%2F8077-purple)](https://dawn.googlesource.com/dawn)
[![Java](https://img.shields.io/badge/Java-25-red?logo=openjdk)](https://openjdk.org)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-blue?logo=github-actions)](https://github.com/features/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Contributing](https://img.shields.io/badge/Contributing-guide-purple)](CONTRIBUTING.md)
[![Projet: Planning](https://img.shields.io/badge/Statut-Planning-blue)](https://github.com)
[![Projet: Incubating](https://img.shields.io/badge/Statut-Incubating-orange)](https://github.com)
[![Projet: Stable](https://img.shields.io/badge/Statut-Stable-green)](https://github.com)
[![Projet: Deprecated](https://img.shields.io/badge/Statut-Deprecated-red)](https://github.com)
[![Projet: Archived](https://img.shields.io/badge/Statut-Archived-lightgrey)](https://github.com)

---

This project is **dawn4k**: a Kotlin Multiplatform binding to Google's Dawn (WebGPU) C API for **Desktop (JVM, macOS ARM64, Linux x64)**. It exposes the raw Dawn API produced by kextract, built on kffi.

---

<!-- ==========================================
     BADGES DE STATUT DE PROJET PERSONNALISABLES
     Décommentez/copiez simplement le badge correspondant au statut actuel de votre projet.
     ========================================== -->

<!-- STATUT : EN PLANIFICATION (PLANNING) -->
<!-- [![Projet: Planning](https://img.shields.io/badge/Statut-Planning-blue)](https://github.com) -->

<!-- STATUT : INCUBATION / EN DÉVELOPPEMENT (INCUBATING) -->
<!-- [![Projet: Incubating](https://img.shields.io/badge/Statut-Incubating-orange)](https://github.com) -->

<!-- STATUT : STABLE / PRÊT PRODUCTION (STABLE) -->
<!-- [![Projet: Stable](https://img.shields.io/badge/Statut-Stable-green)](https://github.com) -->

<!-- STATUT : DEPRÉCIÉ (DEPRECATED) -->
<!-- [![Projet: Deprecated](https://img.shields.io/badge/Statut-Deprecated-red)](https://github.com) -->

<!-- STATUT : ARCHIVÉ (ARCHIVED) -->
<!-- [![Projet: Archived](https://img.shields.io/badge/Statut-Archived-lightgrey)](https://github.com) -->

## 🤝 Contribuer / Contributing

Les contributions sont les bienvenues ! Consultez :

Contributions are welcome! See:

- [🇬🇧 Contributing Guide](CONTRIBUTING.md)
- [🇬🇧 Code of Conduct](CODE_OF_CONDUCT.md) / [🇫🇷 Code de Conduite](CODE_OF_CONDUCT.fr.md)
- [🇬🇧 Security Policy](SECURITY.md) / [🇫🇷 Politique de Sécurité](SECURITY.fr.md)
- [🇬🇧 Support](SUPPORT.md) / [🇫🇷 Assistance](SUPPORT.fr.md)
- [Changelog](CHANGELOG.md)

---

## 🏗️ Project Architecture

- **`:dawn4k-native`** — generated low-level Dawn (WebGPU) bindings for JVM, Android, iOS/tvOS (device + simulator), macOS ARM64 and Linux x64, the C ABI oracle and the cinterop/linkage configuration. Depends only on kffi.
- **`:dawn4k`** — WebGPU wrappers with caller-owned threading. No worker or implicit event pump: call `DawnContext.processEvents()`, drain teardown before context close, and opt into `DawnConfig(implicitDeviceSynchronization = true)` for concurrent shared-device calls. See the [usage guide](docs/docs/getting-started.md) ([FR](docs/docs/getting-started.fr.md)).

The generated sources live in `dawn4k-native/generated/src` and are produced by the pinned kextract; ordinary compilation never runs the generator. See [Native Dawn binding](docs/docs/native-binding.md).

---

## ⚡ CI/CD Workflow

The GitHub Actions pipeline ([ci.yml](file:///.github/workflows/ci.yml)) implements a dual-speed system optimized for bandwidth and compute time:

- **Fast-Track (Feature branches)**: Compiles and tests the JVM targets (`./gradlew jvmTest`), including adapter-dependent tests.
- **Deep-Testing (Branches / Pull Requests to `master`)**: Runs the full test suite (`./gradlew allTests`) on all simulators and target platforms to validate code quality before production.
- **CPU Vulkan (all branches and pull requests)**: Runs `:dawn4k:jvmTest` and `:dawn4k:linuxX64Test` on Ubuntu with Mesa lavapipe. Adapter availability is mandatory; a missing driver/adapter fails the job.

---

## 🛠️ Useful Development Commands

### Run local tests (JVM Fast-Track)
```bash
./gradlew :dawn4k-native:jvmTest
```

### Run all tests (All targets)
On macOS, GPU tests need booted iOS/tvOS simulators with access to Metal services.
The helper selects available devices dynamically; Gradle runs them without
`simctl --standalone` (which makes Metal return a null device).

```bash
python3 scripts/boot-apple-simulators.py
set -a
source build/apple-simulators.env
set +a
./gradlew allTests
```

### Adapter-dependent tests

GPU tests run through the standard test tasks, without Gradle class filters.
They probe adapter availability before executing. Only Dawn's adapter-request
`Unavailable` status permits non-execution, with a named
`WARNING: GPU_TEST_NOT_EXECUTED` diagnostic. JVM reports an ignored test via a
JUnit assumption. Kotlin/Native has no dynamic assumption API: it returns before
the test body, but its standard report may count that invocation as passed.
These warnings mean **no GPU coverage**, not a successful GPU assertion.
Adapter errors, device failures, assertion failures and native crashes remain failures.

Set `DAWN_REQUIRE_ADAPTER=1` to fail instead of allowing missing adapters.
`DAWN_TEST_BACKEND` optionally selects `Metal`, `Vulkan` or `D3D12`; unset uses
Dawn's default selection. A CPU Vulkan adapter can exercise buffer, compute and
offscreen rendering tests, but does not replace Apple Metal or window-system coverage.

```bash
DAWN_TEST_BACKEND=Vulkan DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k:jvmTest :dawn4k:linuxX64Test
```

On Linux, install `libvulkan1`, `mesa-vulkan-drivers` and `vulkan-tools`, then
point `VK_DRIVER_FILES` to the installed `lvp_icd*.json` to select Mesa lavapipe.
Verify it with `vulkaninfo --summary` and a real Dawn buffer readback test.

### Generate Gradle Wrapper
```bash
gradle wrapper
```
