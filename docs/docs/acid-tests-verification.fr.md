# Validation par les tests acid

Le module non publié `:dawn4k-acid-tests` vérifie le binding public Dawn avec la
[suite acid WebGPU publiée par Graphiks](https://github.com/Graphiks-org/WebGPU).
Il exécute l'intégralité du registre `foundationCases()`, sans copier les cas,
exclure les échecs ni affaiblir les assertions. Le binding ne dépend pas de la suite.

## Périmètre et exécution

Le module ne contient que le test du registre publié, sans tests d'infrastructure
ni scénarios GPU synthétiques. Il s'exécute sur JVM et en Kotlin/Native pour
macOS ARM64 et Linux x64. Windows exécute le test JVM avec D3D12. La suite acid
ne cible pas Android, iOS, tvOS ni les navigateurs.

Chaque cas reçoit un contexte, un adaptateur et un appareil neufs. Un thread
dédié maintient les portées d'erreur Dawn sur le même thread malgré les
suspensions des coroutines. Les fonctionnalités optionnelles déclarées sont
demandées explicitement. L'exécution d'un cas est limitée à 30 secondes ; le
traitement des événements pendant le nettoyage est limité à 10 secondes.

Exécuter les commandes depuis la racine du dépôt :

```bash
# macOS ARM64
DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k-acid-tests:jvmTest :dawn4k-acid-tests:macosArm64Test --continue

# Linux x64, avec Vulkan configuré
DAWN_TEST_BACKEND=Vulkan DAWN_REQUIRE_ADAPTER=1 ./gradlew :dawn4k-acid-tests:jvmTest :dawn4k-acid-tests:linuxX64Test --continue
```

```powershell
# JVM sous Windows
$env:DAWN_TEST_BACKEND = "D3D12"
$env:DAWN_REQUIRE_ADAPTER = "1"
.\gradlew.bat :dawn4k-acid-tests:jvmTest --continue
```

Les tâches racines `jvmTest`, `allTests` et `check` incluent aussi le module.
L'exécution GPU n'est pas mise en cache. Compiler ne démontre pas une exécution
GPU ; les tests natifs doivent s'exécuter sur un hôte compatible.

## Configuration de l'adaptateur

`DAWN_TEST_BACKEND` sélectionne `Metal`, `Vulkan` ou `D3D12`. Sans cette variable,
Dawn choisit le backend. `DAWN_REQUIRE_ADAPTER=1` impose la disponibilité d'un
adaptateur.

Sous Linux, installer `libvulkan1`, `mesa-vulkan-drivers` et `vulkan-tools`.
Sélectionner Mesa lavapipe en faisant pointer `VK_DRIVER_FILES` vers le fichier
`lvp_icd*.json` installé, puis vérifier le pilote avec `vulkaninfo --summary`.
Un adaptateur Vulkan CPU peut tester les buffers, le calcul et le rendu hors
écran ; il ne remplace pas la couverture Metal ou du système de fenêtres.
Les tests racines compilent aussi le pont Wayland de la démo, qui nécessite
`build-essential`, `pkg-config`, `wayland-protocols`, `libwayland-dev` et
`libxkbcommon-dev`.

Sous Windows, la convention Gradle télécharge l'archive Dawn vérifiée et configure
le chemin de recherche des DLL externes pour les tâches JVM.

Le runner acid utilise uniquement les API publiques, qui ne distinguent pas de
manière sûre l'indisponibilité d'un adaptateur des autres erreurs de demande.
Par conséquent, **toute erreur de demande d'adaptateur fait échouer l'exécution
acid**, même sans mode strict. Le texte d'une erreur ne sert jamais à déduire
une indisponibilité.

Les tests GPU propres au binding ont une politique différente : seul le statut
typé `Unavailable` de Dawn autorise une non-exécution hors mode strict, avec
`WARNING: GPU_TEST_NOT_EXECUTED`. La JVM utilise une hypothèse JUnit.
Kotlin/Native retourne avant le corps du test et son rapport standard peut
compter l'appel comme réussi. Cet avertissement signifie **aucune couverture
GPU**, et non une assertion réussie. Les erreurs d'adaptateur, les défaillances
d'appareil, les assertions échouées et les plantages natifs restent des échecs.

## Lecture des résultats

Les rapports JSON par cas sont écrits dans
`dawn4k-acid-tests/build/reports/acid/<task-name>.json`. Les rapports Gradle
standards se trouvent dans `dawn4k-acid-tests/build/reports/tests/`. Les rapports
JSON contiennent l'identité de la source de la suite, la cible, le backend
sélectionné, les descriptions des adaptateurs disponibles, les diagnostics par
cas et les compteurs de statuts.

| Statut | Signification |
| --- | --- |
| `passed` | Le cas publié s'est terminé avec succès. |
| `failed` | Une assertion, l'exécution ou le nettoyage du cas a échoué. |
| `unsupported` | Une fonctionnalité optionnelle déclarée est absente, ou le cas Compatibility constate le repli Core autorisé. |
| `not-run` | Réservé à une indisponibilité d'adaptateur identifiée de manière sûre ; les erreurs publiques de demande sont classées comme échecs. |

Les cas unsupported et not-run ne comptent jamais comme réussis. Le mode strict
refuse les résultats not-run. `complete=true` signifie que chaque entrée du
registre possède un résultat, **pas** que tous les cas ont réussi. Un rapport
intermédiaire `complete=false` n'est pas une preuve d'exécution réussie. Les
anciens rapports sont supprimés avant chaque tâche de test.

Un cas échoué fait échouer la tâche Gradle après collecte des autres cas
indépendants. Si la fermeture contrôlée du contexte échoue, l'exécution s'arrête
et le rapport reste incomplet, sans réutiliser un environnement non stabilisé.

## Contrats couverts par les cas de mapping et de validation

Les diagnostics par cas identifient le contrat public non respecté. Notamment :

- `buffers.map-cancel-remap` vérifie que l'annulation du mapping permet un remapping.
- `buffers.map-destroyed` vérifie la validation lors du mapping d'un buffer détruit.
- `buffers.partial-map-remap` vérifie les offsets absolus des plages mappées.
- `errors.overlapping-mapped-ranges` vérifie le rejet des plages mappées qui se chevauchent.
- `errors.unenabled-timestamp-feature` vérifie le refus d'une opération timestamp
  sans la fonctionnalité requise, avec le mécanisme de signalement attendu.

Consulter les rapports générés pour déterminer quels cas réussissent ou échouent
réellement sur l'adaptateur testé. Les échecs ne sont ni exclus ni transformés en
succès attendus.

## CI et limites

La matrice CI ordinaire exécute les tests JVM sur macOS/Metal, Linux/Vulkan CPU et
Windows/D3D12. La validation complète utilise `check` à la racine, avec les tests
natifs pris en charge par l'hôte et les vérifications Android/lint ; le Fast-Track
utilise `jvmTest` à la racine. Les simulateurs Apple ne sont démarrés que sur
macOS. Tous les runners imposent un adaptateur et publient les rapports même en
cas d'échec. Le check obligatoire `build-and-test` ne réussit que si tous les
runners réussissent.

Le remplacement des rapports intermédiaires n'est pas atomique : une erreur
d'écriture ou un arrêt du processus peut faire perdre le précédent point de
reprise. Les délais des coroutines sont coopératifs et ne peuvent pas interrompre
un appel natif définitivement bloqué. Un rapport auquel manquent des résultats
terminés ne doit pas être interprété comme une exécution réussie.
