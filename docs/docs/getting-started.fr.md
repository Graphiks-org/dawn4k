# Démarrage — dawn4k

Comment travailler sur **dawn4k**, le binding Kotlin Multiplatform vers l'API C de
Google Dawn (WebGPU) pour le desktop (JVM, macOS ARM64, Linux x64).

## Prérequis

- JDK 25 et le wrapper Gradle du dépôt (`9.8.0`).
- Un hôte macOS ARM64 ou Linux x64.
- Une toolchain C (`cc`) pour l'oracle ABI et le helper de callbacks.
- Optionnel : LLVM (`brew --prefix llvm`) si vous devez reconstruire le générateur
  kextract figé.

## Modules

- **`:dawn4k-native`** — bindings Dawn bas niveau générés (`org.graphiks.dawn4k.native`),
  l'oracle ABI C et la configuration cinterop/linkage. Ne dépend que de kffi.
Voir [Binding natif Dawn](native-binding.md) pour la génération et la liaison.

## Commandes courantes

```bash
./gradlew :dawn4k-native:jvmTest        # tests JVM rapides
./gradlew allTests                      # cibles hôte
./gradlew :dawn4k-native:compileAndroidMain  # la cible Android/JVM compile
./gradlew :dawn4k-native:compileKotlinIosArm64  # la cible iOS compile
```

## Régénérer les bindings

```bash
bash scripts/generate-dawn-bindings.sh
```

Le script construit le kextract figé dans un checkout dédié et ignoré par git, puis
régénère dans `dawn4k-native/build/regenerated/src`. Relire ce diff, puis le
promouvoir dans `dawn4k-native/generated/src` (versionné). La compilation ordinaire
n'exécute jamais le générateur.

## Personnaliser le build

- `settings.gradle.kts` — `rootProject.name`.
- `build.gradle.kts` — `group` et version.
- `buildSrc/.../kmp-library.gradle.kts` — cibles desktop et toolchain.
- `buildSrc/.../kmp-publish.gradle.kts` — coordonnées de publication et champs POM.
- `buildSrc/.../kmp-dokka.gradle.kts` — nom de module Dokka et lien source.
- `docs/mkdocs.yml` — métadonnées du site.
- `LICENSE` — détenteur des droits.

## Vérification finale

- [ ] `./gradlew allTests` réussit.
- [ ] `mkdocs build -f docs/mkdocs.yml` fonctionne.
