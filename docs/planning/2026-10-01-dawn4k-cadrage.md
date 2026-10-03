# Cadrage dawn4k — suivi de préparation

Demande : préparer un plan, sans implémenter, pour un monorepo comprenant
`dawn4k-native` (bindings Dawn bas niveau via kextract/kffi) et `dawn4k`
(implémentation de Graphiks-org/WebGPU), consommant dawn-packer et réutilisant
la suite de tests acides de WebGPU.

Ce fichier suit la préparation ; ce n'est pas une spécification approuvée.

- [x] Classer le chantier : architectural, deux couches et un runner de validation.
- [x] Explorer le template local, son historique et les dépôts de référence.
- [x] Vérifier la présence d'un graphe existant : absent. Aucune génération de
      cartographie ni installation d'outil dans ce travail de cadrage.
- [x] Clarifier la matrice de cibles et le périmètre du premier livrable :
      l'utilisateur a choisi desktop d'abord (JVM desktop + Kotlin/Native
      macosArm64 et linuxX64 ; Windows et mobile ensuite).
- [x] Présenter les approches possibles et la recommandation.
- [ ] Faire valider les sections du design : modules, FFI, ressources/asynchronisme,
      compatibilité API, validation et distribution.
- [x] Écrire une spécification de travail jointe au plan directement demandé
      le 2026-10-02 ; son statut proposé ne présume pas son approbation.
- [x] Relire la spécification : cohérence, ambiguïtés, limites et critères de succès.
- [ ] Faire relire et approuver la spécification écrite.
- [x] Utiliser writing-plans pour produire trois plans d'implémentation liés,
      avec gates d'acceptation indépendants : native, backend, suite/distribution.

Le compagnon visuel n'est proposé que si une question bénéficie réellement
d'une représentation visuelle. Aucune exécution d'implémentation n'est autorisée
par la seule demande de plan.

## Références examinées

- Dépôt local : commit initial `9398c29`, module `shared` de template KMP.
- Graphiks-org/WebGPU : master `2608e3f5202f8386bf391274083419808b99d857`.
- Graphiks-org/dawn-packer : master `2ba660f56c8dfe150aed6743240e13bbd2025d24`.
- wgpu4k/wgpu4k-native : main `1263d9c28be13e6a8ccf04c47c37dc3e49dbe9f8`.

## Constats établis

- dawn-packer documente un pin Dawn `chromium/8077`, des archives par cible et
  linkage, `SHA256SUMS`, `index.json` et un manifeste contenant la révision.
  Le header C réel est `dawn/webgpu.h` ; `webgpu/webgpu.h` est un shim.
- WebGPU publie/déclare des modules API, descriptors, suite-core et suite-acid-tests.
  La disponibilité des métadonnées snapshots a été vérifiée ; la résolution et
  compilation Gradle feront partie de l'implémentation.
- suite-acid-tests déclare JVM, JS, Wasm JS, linuxX64 et macosArm64 ; étendre le
  binding à d'autres cibles n'étend pas automatiquement les artefacts de suite.
- AcidContext reçoit un device et une fabrique suspendue d'adaptateurs frais.
  Les ressources du runner et celles des cas ont des propriétaires distincts.
- Les statuts de suite sont passed, failed, unsupported et not-run. Une feature
  optionnelle absente n'est pas équivalente à une API de base non implémentée.
- wgpu4k-native génère explicitement ses bindings via kextract, versionne les
  sources produites et consomme `org.graphiks:kffi` au runtime.
- Android Kotlin/Native reste un risque ouvert dans dawn-packer (libc++/NDK).
- dawn-packer ne fournit pas macosX64 dans sa matrice documentée ; watchOS est
  abandonné. Ne pas recopier la matrice de wgpu4k-native.
- La release publique dawn-packer `v8077.0.0` existe et contient 27 assets.
  Utiliser ce tag vérifié, pas `v0.1.0` donné à titre d'exemple dans consumption.md.
- La dernière release GitHub WebGPU examinée est `0.0.9`. Les chemins
  `suite-acid-tests/build.gradle.kts` et `suite-core/.../AcidContext.kt` sont absents
  de ce tag (HTTP 404). Ce tag ne suffit donc pas comme baseline de la suite
  examinée sur master. L'utilisateur a depuis retenu la publication Maven
  `0.1.0-SNAPSHOT` évolutive, vérifiée indépendamment des releases GitHub.
- `docs/acid-coverage.md` sur WebGPU master annonce 123 cas, dont 118 obligatoires
  et 5 optionnels. Cette suite valide son catalogue, pas la conformité WebGPU
  complète ; elle ne remplace pas les tests ABI/callbacks du binding bas niveau.
- Le template utilise Kotlin 2.4.10 ; WebGPU master utilise Kotlin 2.4.20.
  L'alignement du compilateur et des klibs doit faire partie du premier incrément.

## Politique WebGPU retenue : 0.1.0-SNAPSHOT évolutive

Correction utilisateur du 2026-10-02 : rester sur `0.1.0-SNAPSHOT`, sans pins
horodatés, afin de faire évoluer conjointement WebGPU et dawn4k lorsque les API
publiques changent. Cette décision remplace la proposition précédente de figer
les artefacts du run indiqué.

- Dépendances : `org.graphiks:webgpu-api:0.1.0-SNAPSHOT`,
  `org.graphiks:webgpu-descriptors:0.1.0-SNAPSHOT`,
  `org.graphiks:suite-core:0.1.0-SNAPSHOT` et
  `org.graphiks:suite-acid-tests:0.1.0-SNAPSHOT`.
- Dépôt : `https://central.sonatype.com/repository/maven-snapshots/`, filtré
  sur les modules WebGPU nécessaires et leurs variantes.
- Ne pas imposer de contraintes horodatées aux racines, variantes ou transitives
  WebGPU ; ne pas verrouiller leur contenu au point de bloquer les republications.
- Prévoir un rafraîchissement explicite des dépendances après une publication
  WebGPU, puis la compilation et la suite acide dans dawn4k.
- Conserver dans les rapports de validation l'identité des artefacts réellement
  résolus (versions horodatées/empreintes et commit source lorsque disponible)
  comme provenance d'exécution, pas comme contrainte de résolution.
- Une coordonnée SNAPSHOT stable ne garantit pas un contenu identique entre
  exécutions ; les changements d'API sont volontairement coordonnés entre projets.

Le run de référence
https://github.com/Graphiks-org/WebGPU/actions/runs/36626933869/job/110595901346
a été vérifié successful sur le commit
`2608e3f5202f8386bf391274083419808b99d857`. Les métadonnées Maven des quatre
racines et de leurs variantes JVM, macosArm64 et linuxX64 ont répondu HTTP 200.
Cela confirme une publication disponible, sans en faire un pin permanent et
sans constituer encore une vérification de résolution ou de compilation Gradle.

## Documents rédigés le 2026-10-02

- Spécification de travail : `docs/superpowers/specs/2026-10-02-dawn4k-design.md`.
- Index : `docs/superpowers/plans/2026-10-02-dawn4k.md`.
- Bas niveau : `docs/superpowers/plans/2026-10-02-dawn4k-01-native.md`.
- Backend : `docs/superpowers/plans/2026-10-02-dawn4k-02-backend.md`.
- Suite/distribution : `docs/superpowers/plans/2026-10-02-dawn4k-03-suite.md`.

La demande d'écrire le plan a été exécutée sans lancer d'implémentation et sans
interpréter cette demande comme une approbation des artefacts nouvellement
rédigés. Leur revue et le choix d'une méthode d'exécution restent ouverts.

Les plans ont ensuite été révisés à la demande de l'utilisateur pour retirer
les tests d'infrastructure (build/configuration, téléchargement, génération,
packaging/publication, provenance et politiques CI). Les tests du binding,
du comportement WebGPU/callbacks et du runner acide sont conservés ; les tâches
d'infrastructure restent présentes, sans suite de tests dédiée.
