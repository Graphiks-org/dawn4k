# Binding natif Dawn

`dawn4k-native` est le binding bas niveau vers l'implémentation WebGPU
[Dawn](https://dawn.googlesource.com/dawn) de Google. Il expose l'API C brute dans
le package `org.graphiks.dawn4k.raw` et ne dépend que de
[kffi](https://central.sonatype.com/artifact/org.graphiks/kffi) ; il **ne** dépend
**pas** du module de plus haut niveau `webgpu-api`.

## Couches

| Couche | Emplacement | Rôle |
| --- | --- | --- |
| API C brute | `dawn4k-native/generated/src` | sortie de kextract ; jamais éditée à la main |
| Bootstrap JVM | `KextractNativeBootstrap` généré | extrait et charge la bibliothèque partagée embarquée |
| cinterop Kotlin/Native | `dawn4k-native/src/nativeInterop/cinterop/dawn.def` | package interne `webgpu.native` lié à la bibliothèque statique |
| Oracle ABI | `tests/abi/dawn_abi.c` | mesure la disposition C réelle |
| Smoke | `native-smoke` | crée un vrai adapter, device et buffer |

Les sources générées sont versionnées. La compilation ordinaire les consomme et
n'exécute jamais kextract.

## Génération

Les entrées sont figées :

- `bindings/dawn.lock.json` — release Dawn `v8077.0.0`, tag `chromium/8077`,
  révision `531028367c60ce07251ec0231c1b95bedb4495bc` et le SHA-256 de chaque archive.
- `bindings/kextract.version` — le commit kextract utilisé pour générer.
- `bindings/callback-bindings.yml` — les contrats de callbacks validés.

Reproduire les sources générées avec :

```bash
bash scripts/generate-dawn-bindings.sh
```

Le script construit le kextract figé dans un checkout dédié et ignoré par git, puis
régénère dans `dawn4k-native/build/regenerated/src`. Relire ce diff et le promouvoir
dans `dawn4k-native/generated/src`. `:dawn4k-native:generateBindingsFromHeader` ne
fait jamais partie du graphe de compilation ordinaire.

## Bibliothèques natives

`./gradlew :dawn4k-native:prepareDawn` télécharge, vérifie et extrait les archives
sous `dawn4k-native/build/native/<target>/<linkage>/`. L'extraction refuse les
chemins absolus, la traversée, les entrées qui ne sont ni fichier ni répertoire, la
dérive de checksum et les incohérences de manifeste.

- **JVM** lie la bibliothèque partagée au runtime. Les deux bundles sont embarqués
  dans le JAR : `darwin-aarch64/libwebgpu_dawn.dylib` et
  `linux-x86-64/libwebgpu_dawn.so`. Le bootstrap généré vérifie leur SHA-256 et les
  extrait dans un répertoire de cache. Exécuter avec
  `--enable-native-access=ALL-UNNAMED`.
- **Kotlin/Native** lie la bibliothèque statique (`libwebgpu_dawn.a`) plus les
  bibliothèques système de la plateforme : les frameworks Apple (`Metal`,
  `Foundation`, `CoreGraphics`, `QuartzCore`, `IOKit`, `IOSurface`) pour macOS, iOS
  et tvOS (device + simulateur), ou `-lpthread -ldl -lm` sous Linux.
- **Android/JVM** utilise le moteur Android de kffi ;
  `:dawn4k-native:extractAndroidNativeLibs` extrait les bibliothèques partagées
  vérifiées dans `dawn4k-native/src/androidMain/jniLibs/<abi>/` (ignoré par git ;
  `arm64-v8a`, `x86_64`) avant le packaging Android. Les signatures hors de la base
  de formes fixes de kffi-android passent par le chemin générique `callGeneric`.

Pas encore disponible, en attente de travaux amont :

- **Kotlin/Native `androidNative*`** — les archives Android de dawn-packer sont
  construites avec un libc++ plus récent que celui lié par Kotlin/Native, donc les
  cibles Android K/N ne se lient pas (voir `docs/spikes/android.md` de dawn-packer).

## Vérification ABI

```bash
./gradlew :dawn4k-native:verifyDawnAbi
```

compile `tests/abi/dawn_abi.c` contre l'en-tête d'origine, l'exécute, puis compare
les tailles, les alignements et les offsets de champs avec la disposition inscrite
dans les bindings JVM générés. `dumpGeneratedAbi` écrit le même schéma depuis les
sources générées. Les rapports atterrissent dans `dawn4k-native/build/reports/abi/`.

## Statut du smoke

```bash
./gradlew :native-smoke:runJvmSmoke
./gradlew :native-smoke:runSmokeMacosArm64
./gradlew :native-smoke:runSmokeLinuxX64
```

Chaque tâche crée une vraie instance, demande un adapter et un device via les
enregistrements `AllowProcessEvents`, crée un buffer mappé de 16 octets, écrit quatre
valeurs `uint32` et libère le tout. Le processus sort en erreur en cas
d'adapter/device manquant, de timeout ou de résultat invalide ; un timeout n'est
jamais un skip.

Vérifié sur macOS ARM64 (Metal) : les exécutables JVM et Kotlin/Native statique
rapportent tous deux `status: passed`, `callbackCount: 2`, `bufferSize: 16`. Les
tâches Linux doivent s'exécuter sur un hôte Linux x64 avec un pilote Vulkan ; un
exécutable Kotlin/Native Linux ne peut pas être produit par la toolchain macOS car
son sysroot précède les symboles glibc référencés par l'archive Linux de Dawn. La
cible Android/JVM est vérifiée par compilation (`:dawn4k-native:compileAndroidMain`) ;
un smoke runtime Android nécessite un émulateur ou un device et ne fait pas partie
de la CI.
