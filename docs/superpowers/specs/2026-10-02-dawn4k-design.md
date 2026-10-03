# dawn4k — spécification de travail

## Statut et intention

Spécification jointe aux plans demandés par l'utilisateur le 2026-10-02.
Les décisions explicites sont : un monorepo, deux bibliothèques `dawn4k-native`
et `dawn4k`, kextract pour le bas niveau, consommation de dawn-packer,
réutilisation des tests acides WebGPU, desktop d'abord et dépendances WebGPU
évolutives `0.1.0-SNAPSHOT`. Les choix détaillés ci-dessous sont proposés pour
revue ; ni ce document ni la demande de plan n'autorisent l'implémentation.

**Succès final :** un projet KMP consommateur utilise les artefacts publiés,
obtient un adapter/device Dawn et passe le catalogue obligatoire de la suite
WebGPU sur les configurations desktop effectivement validées.

## Contraintes globales

- Monorepo ; artefacts proposés `org.graphiks:dawn4k-native` et `org.graphiks:dawn4k`.
- Packages proposés `org.graphiks.dawn4k.raw` et `org.graphiks.dawn4k`.
- Version propre au monorepo : `0.1.0-SNAPSHOT`, surcharge `releaseVersion`.
- WebGPU : `org.graphiks:webgpu-api:0.1.0-SNAPSHOT`, `org.graphiks:webgpu-descriptors:0.1.0-SNAPSHOT`, `org.graphiks:suite-core:0.1.0-SNAPSHOT`, `org.graphiks:suite-acid-tests:0.1.0-SNAPSHOT`.
- Pas de pin horodaté ni de verrou de contenu empêchant les évolutions WebGPU.
- Dépôt snapshots : `https://central.sonatype.com/repository/maven-snapshots/`.
- Kotlin `2.4.20`, JDK `25` ; conserver le wrapper Gradle `9.6.1` sous réserve de la vérification de compilation initiale.
- kffi : consommation runtime proposée `org.graphiks:kffi:1.0.0-SNAPSHOT` ; vérifier sa disponibilité et sa compatibilité au premier incrément.
- kextract : outil de génération, pas dépendance runtime ; référence proposée `e1d922eefb6b65887c6cc4bd98370ad2d7ba8b45` du dépôt `klang-toolkit/kextract`.
- Dawn : release dawn-packer `v8077.0.0`, tag `chromium/8077`, révision `531028367c60ce07251ec0231c1b95bedb4495bc`.
- Header réel : `include/dawn/webgpu.h` ; empreinte de référence `f5cb9da17efab635daa12c5a9474e00d525809cf1330306a0eb88dee616f487f`.
- Cibles initiales : JVM macOS ARM64/Linux x64 et Kotlin/Native `macosArm64`/`linuxX64`.
- JVM : Dawn shared ; Kotlin/Native : Dawn static au premier livrable.
- Aucun backend `null` ne constitue une preuve de validation fonctionnelle GPU.
- Ne pas copier les interfaces/descriptors/tests de WebGPU ; ne pas modifier manuellement les sources générées.
- Pas d'implémentation navigateur, Windows, mobile, surface fenêtrée ou benchmark dans le premier livrable.

## Découpage et dépendances

1. **dawn4k-native** : surface C de Dawn générée par kextract, bibliothèque
   native, cinterop et bootstrap JVM. Dépend de kffi ; ne dépend pas de WebGPU.
2. **dawn4k** : classes implémentant les interfaces Graphiks, conversions
   descriptors/enums, ownership, événementiel et adaptation suspendue.
   Dépend de dawn4k-native et webgpu-api ; utilise les descriptors existants.
3. **dawn4k-suite** : runner non publié, dépend de dawn4k, suite-core et
   suite-acid-tests ; produit rapports JSON et résultat de processus explicite.
4. **native-smoke** : consommateur bas niveau non publié, exécutable JVM/K/N
   indépendant du haut niveau ; valide chargement, appels et callbacks.
5. **buildSrc / bindings** : conventions et entrées déclaratives. La compilation
   ordinaire consomme les sources générées versionnées sans reconstruire kextract.

Les plans sont séquentiels mais chacun termine avec un logiciel testable :
binding + smoke, backend + readbacks, puis catalogue + publications locales documentées.

## Entrées publiques du backend

```kotlin
enum class DawnBackend { Metal, Vulkan }
data class DawnConfig(val backend: DawnBackend? = null)

class DawnContext private constructor() : AutoCloseable {
    suspend fun requestAdapter(
        options: org.graphiks.webgpu.GPURequestAdapterOptions? = null,
    ): Result<org.graphiks.webgpu.GPUAdapter>
    suspend fun drainEvents()
    override fun close()
    companion object {
        fun create(config: DawnConfig = DawnConfig()): DawnContext
    }
}
```

Ce bloc définit un contrat proposé, pas un corps d'implémentation. Le contexte
possède l'instance et le service de progression des événements. Il n'est pas un
singleton. Chaque demande retourne un nouvel objet adapter possédé par l'appelant.
Le runner le ferme seulement après fermeture de ses adapters/devices.
`drainEvents` est une opération spécifique au contexte Dawn pour la validation ;
elle ne modifie aucune interface GPU partagée et n'expose pas de pointeur brut.

## Génération et distribution native

Télécharger uniquement les archives requises par les tâches demandées. Vérifier
le checksum fixé du tar.gz, puis `manifest.json` (cible, linkage, tag/révision et
empreintes des fichiers). Refuser les entrées tar absolues, traversées de chemin,
liens et manifestes incohérents ; extraction dans un répertoire temporaire avant
promotion atomique vers `build/native/`.

Les headers de toutes les archives utilisées doivent correspondre à la même
révision. La génération utilise une copie préparée et ses fichiers d'entrée
identifiés ; conserver les headers d'origine pour cinterop et le test C ABI.
Ne pas reprendre un prelude supposant `size_t` 64 bits pour une cible 32 bits.
Cette restriction est compatible avec les quatre configurations initiales.

Versionner les sorties sous `dawn4k-native/generated/src/` et le manifeste de
génération (Dawn, kextract, paramètres ABI, contrats callbacks, hashes des bundles
JVM). Une génération explicite dans `build/` permet de relire les sorties avant
promotion, sans gate automatisé de comparaison ni réécriture des fichiers
manuels. Si kextract ne comprend pas un type de
Dawn, corriger son générateur en amont ; ne pas ajouter un fichier Kotlin fake.

Le bootstrap JVM généré embarque les bundles `darwin-aarch64` et
`linux-x86-64`, vérifie leur intégrité et extrait les fichiers. Utiliser
`--enable-native-access=ALL-UNNAMED` pour les exécutions JVM. La liaison static
K/N est testée dans un exécutable réel, avec les dépendances système de
`dawn-packer/docs/consumption.md`, pas seulement via création d'un klib.

## ABI, defaults et conversions

Tester `sizeof`, `alignof` et `offsetof` avec un programme C compilé contre le
header d'origine ; inclure `WGPUStringView`, `WGPUFuture`, les CallbackInfo
passés par valeur et les chaînes imbriquées. Une comparaison de texte Kotlin
seule ne prouve pas l'ABI.

Les structures C doivent recevoir les defaults définis par leurs macros
`WGPU_*_INIT`, pas simplement une mémoire mise à zéro. Mapper explicitement :
booléens, `ULong`/`size_t`, listes/pointeurs/counts, chaînes UTF-8 + longueur,
champs nullables, constantes de taille indéfinie et `nextInChain`/`sType`.
Ne jamais utiliser les ordinals d'enums Kotlin comme valeurs C.

Points déjà vérifiés dans le header livré :
- `maxImmediateSize` et les fonctions `SetImmediates` existent.
- Les limites storage par stage sont dans `WGPUCompatibilityModeLimits` chaîné
  à `WGPULimits`, et non dans les champs directs de `WGPULimits`.
- Le swizzle passe par `WGPUTextureComponentSwizzleDescriptor`.
- Le maximum de draws passe par `WGPURenderPassMaxDrawCount`.
- Les callbacks uncaptured-error et device-lost ont un premier paramètre
  `WGPUDevice const *` ; ne pas le traiter comme un `WGPUDevice` par valeur.

## Ownership et asynchronisme

Les allocations de descriptors restent valides pendant la consommation définie
par le contrat C. Les résultats pointés et messages callbacks sont copiés
pendant la callback avant toute reprise sur un autre thread. Ne pas suspendre
dans un `memoryScope` JVM confiné ni faire transiter son allocator entre threads.

Le backend utilise `AllowProcessEvents` pour les opérations qui proposent un
mode, et progresse `wgpuInstanceProcessEvents` sur un worker possédé par le
contexte. Les upcalls spontanées possibles sont copiées/routées vers ce worker.
Les appels Dawn et transitions d'état sont sérialisés ; les APIs synchrones
réentrantes depuis le worker s'exécutent directement, sans deadlock.

L'opération asynchrone peut être lancée, complétée inline, différée, annulée,
ou interrompue par fermeture. Une continuation ne termine qu'une fois.
L'annulation Kotlin ne prouve pas l'annulation native : garder la registration
et ses données nécessaires jusqu'à terminal native ou teardown prouvé ; une
callback tardive libère tout résultat nouvellement possédé sans le livrer.
La fermeture arrête les nouveaux appels, draine/termine les opérations, ferme
les producteurs natifs puis libère les registrations. Aucun pointeur libéré
n'est transmis à Dawn ou à une callback ; aucune exception ne traverse la FFI.

`close()` est idempotent. Les buffers/textures doivent subir la destruction
WebGPU (libérer la seule référence C ne suffit pas). Le plan définit un registry
de références possédées par device : destruction logique au `close()`, maintien
d'un handle tombstone valide lorsque les opérations de validation sur un objet
détruit l'exigent, puis release exactement une fois au teardown du propriétaire.
Cette première stratégie privilégie la correction ; elle n'annonce pas une
libération immédiate de tous les handles. Les interfaces sans `AutoCloseable`
(queue, passes, bundle dans la baseline examinée) ont un propriétaire explicite.

Une vue `getMappedRange` est empruntée : les writes modifient la vraie mémoire
GPU mappée. Les factories publiques `ArrayBuffer.wrap` JVM/K/N existent. Elles
ne protègent pas toutes contre accès après `unmap` ; cette limite du contrat doit
être documentée et ne doit pas être testée en déréférençant de la mémoire native
libérée. Si un garde commun devient nécessaire, proposer une évolution WebGPU
et republier la snapshot avant de la consommer, sans nouvelle implémentation
externe de son interface `ArrayBuffer` sealed.

## Contrat d'erreurs

Les callbacks d'erreurs non capturées sont installées à la création du device
et restent actives pendant sa vie. Mapper Validation, OutOfMemory et Internal
sur les interfaces publiques correspondantes ; mapper les résultats suspendus
sur `Result` sans transformer une annulation de coroutine en succès/échec métier.

La validation des commandes invalides doit rester observable dans les error
scopes Dawn. Ne pas prévalider tout WebGPU dans Kotlin avec des exceptions qui
masqueraient la validation native attendue par la suite. Les checks de mémoire
host préviennent cependant les overflows/out-of-bounds avant accès FFI.
La perte de device doit débloquer les attentes ; ne pas inventer un
`GPUDevice.lost` absent de l'API commune examinée.

## Validation et provenance

Trois étages : tests purs (conversions/lifecycle), tests ABI et smoke réels,
puis suite publique sur Dawn Metal/Vulkan réel ou Vulkan logiciel identifié.
À la demande de l'utilisateur, ne pas créer de tests d'infrastructure
(configuration/build Gradle, téléchargement/extraction, génération, packaging/
publication, provenance, scripts de rapport ou politiques CI). Conserver les
protections d'exécution, les commandes de build/publication locale et la revue
des sorties ; les tests portent sur le binding et le comportement WebGPU/runner.

Le runner utilise `foundationCases()` et `AcidContext`, demande un adapter et
device frais par cas et les features optionnelles déclarées, ainsi qu'une
fabrique réellement fraîche pour les cas de contexte. Vérifier les features
reçues sur le device, drainer les événements/erreurs avant de conclure, timeout
30 secondes par cas et fermeture en `finally`.

Statuts : `passed`, `failed`, `unsupported` (feature optionnelle absente),
`not-run` (exécution non disponible). Pas de faux succès pour adapter absent,
timeout, méthode obligatoire manquante, sélection vide ou id inconnu.
Les subsets servent au développement ; le mode d'acceptation exécute tout le
catalogue résolu. Le nombre de référence est 123/118 obligatoires/5 optionnels,
pas une constante éternelle puisque la snapshot est évolutive.

Rapports : cible, OS/arch, JDK/Kotlin, révision Dawn, linkage/backend sélectionné,
description adapter, identité/hash des artefacts WebGPU/kffi effectivement
consommés, ids/résultats/diagnostics/durées des cas. Le commit WebGPU n'est
mentionné que s'il est prouvé ; ne pas assimiler automatiquement master à Maven.

Publication non distante : `publishToMavenLocal`, revue des coordonnées,
variantes et transitives, puis exemple d'usage documenté. Pas de harness
automatisé de publication ni de consommateur de test à créer ; une vérification
manuelle externe reste possible.
Une publication distante ou un merge n'est pas autorisé par ce plan.

## Sorties et critères par incrément

| Plan | Livrable | Critère de sortie |
| --- | --- | --- |
| 01 native | dawn4k-native + native-smoke | génération relue, tests C ABI et appels/callbacks JVM/K/N réussis |
| 02 backend | dawn4k | roundtrip buffers, compute et rendu offscreen via API Graphiks ; lifecycle/erreurs vérifiés |
| 03 suite | runner + distribution | catalogue obligatoire complet sur configurations déclarées validées et publications locales documentées |

La CI rapide ne revendique pas de GPU ; les jobs GPU publient les rapports,
y compris sur échec. Un runner sans GPU utilisable doit être explicitement
`not-run` et ne valide pas la plateforme. La disponibilité du GPU d'un runner
hébergé doit être établie avant de rendre son job bloquant.
