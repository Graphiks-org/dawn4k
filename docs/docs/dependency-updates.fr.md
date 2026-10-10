# Mises à jour des dépendances

Comment faire évoluer `:dawn4k` quand la snapshot du contrat WebGPU ou la
release native de Dawn évolue. Deux côtés d'entrées figées doivent rester
cohérents : le contrat WebGPU Kotlin (`webgpu-api` / `webgpu-descriptors`) et
les bindings Dawn générés (`:dawn4k-native`).

## Entrées figées

| Entrée | Fige |
| --- | --- |
| `bindings/dawn.lock.json` | la release Dawn (`v8077.0.0`, tag `chromium/8077`, révision et SHA-256 de chaque archive) servie à la fois à kextract et aux bibliothèques préconstruites |
| `bindings/kextract.version` | le commit kextract qui a généré `org.graphiks.dawn4k.native` |
| `bindings/callback-bindings.yml` | les contrats de callbacks validés que kextract émet |
| Gradle | `org.graphiks:webgpu-api:0.1.0-SNAPSHOT`, `org.graphiks:webgpu-descriptors:0.1.0-SNAPSHOT` (le contrat), `org.graphiks:suite-acid-tests:0.1.0-SNAPSHOT` (portée test) |

La snapshot résolue est ce contre quoi le backend compile — les jars sources du
cache Gradle, pas un clone master.

## Coordonner une évolution de snapshot

Quand les artefacts du contrat ou les bindings générés bougent :

```bash
./gradlew --refresh-dependencies :dawn4k:compileKotlinJvm :dawn4k:jvmTest
DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k:jvmTest
```

La première commande re-résout les snapshots et lance les tests unitaires et
ceux dépendant d'un adapter. La seconde exige un adapter : les tests GPU doivent
s'exécuter, pas seulement signaler son absence. Terminer avec les autres cibles :

```bash
DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k:macosArm64Test
./gradlew :dawn4k:compileKotlinLinuxX64
```

(Les binaires de test Linux ne se lient que sur un hôte Linux ;
`compileKotlinLinuxX64` est la barrière Linux atteignable ailleurs — voir
`dawn4k/build.gradle.kts`.)

## Quand un trou de symbole apparaît

Une snapshot résolue peut nommer un membre que la release Dawn figée ne sert
pas — ou les bindings générés peuvent exposer un symbole natif que le contrat
ne modélise pas. **Un trou de symbole est un incrément amont, jamais un stub
local :**

1. ouvrir l'incrément en amont — la snapshot du contrat WebGPU (webgpu-api /
   webgpu-descriptors) et/ou le prébuild Dawn (`dawn.lock.json`,
   `kextract.version` via le pipeline dawn-packer) ;
2. régénérer les bindings et consommer la nouvelle snapshot/release ;
3. relancer les barrières ci-dessus.

Aucune méthode obligatoire du contrat résolu ne peut être reportée comme
« unsupported » pour garder un rapport vert : le backend implémente chaque
membre que la snapshot définit, et un membre impossible à servir est un bug de
coordination à corriger en amont, pas une décision locale. Les champs acceptés
et ignorés (documentés dans les mappers, p. ex. l'indicateur `xrCompatible`
déprécié par la spec ou les compilation hints sans contrepartie Dawn) sont
exactement cela — des décisions de consommation documentées, pas des méthodes
reportées.
