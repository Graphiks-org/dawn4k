# dawn4k — Dawn (WebGPU) desktop bindings

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-purple?logo=kotlin)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.6.1-blue?logo=gradle)](https://gradle.org)
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

This project is **dawn4k**: a Kotlin Multiplatform binding to Google's Dawn (WebGPU) C API for **Desktop (JVM, macOS ARM64, Linux x64)**. It exposes the raw Dawn API produced by kextract and a real adapter/device/buffer smoke consumer, built on kffi.

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

- **`:dawn4k-native`** — generated low-level Dawn (WebGPU) bindings for desktop (JVM, macOS ARM64, Linux x64), the C ABI oracle and the cinterop/linkage configuration. Depends only on kffi.
- **`:native-smoke`** — a real consumer that creates a Dawn instance, adapter, device and a mapped buffer on JVM and Kotlin/Native.

The generated sources live in `dawn4k-native/generated/src` and are produced by the pinned kextract; ordinary compilation never runs the generator. See [Native Dawn binding](docs/docs/native-binding.md).

---

## ⚡ CI/CD Workflow

The GitHub Actions pipeline ([ci.yml](file:///.github/workflows/ci.yml)) implements a dual-speed system optimized for bandwidth and compute time:

- **Fast-Track (Feature branches)**: Compiles and tests only the local JVM target (`./gradlew :dawn4k-native:jvmTest`).
- **Deep-Testing (Branches / Pull Requests to `master`)**: Runs the full test suite (`./gradlew allTests`) on all simulators and target platforms to validate code quality before production.

---

## 🛠️ Useful Development Commands

### Run local tests (JVM Fast-Track)
```bash
./gradlew :dawn4k-native:jvmTest
```

### Run all tests (All targets)
```bash
./gradlew allTests
```

### Generate Gradle Wrapper
```bash
gradle wrapper
```
