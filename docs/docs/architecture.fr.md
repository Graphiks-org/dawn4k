# Architecture

`:dawn4k` est le backend WebGPU de ce dépôt : il implémente le contrat WebGPU
public de Graphiks (`org.graphiks.webgpu`) au-dessus de l'API C de Dawn. Cette
page décrit la conception en couches, le modèle de threads et les deux
divergences délibérées du contrat que tout relecteur doit connaître : la
`ArrayBuffer` empruntée d'une plage mappée, et l'absence d'un `GPUDevice.lost`
commun.

## Couches

| Couche | Emplacement | Rôle |
| --- | --- | --- |
| Bindings générés | `:dawn4k-native` → `org.graphiks.dawn4k.native` | sortie kextract sur la release Dawn figée ; jamais éditée à la main |
| Dispatcher & runtime | `dawn4k/.../internal/NativeDispatcher.kt`, `DawnRuntime.kt`, `PendingOperation.kt` | un worker dédié par runtime ; opérations natives sérialisées, chorégraphie des callbacks et pompe d'événements |
| Sessions device & registre | `dawn4k/.../internal/DeviceSession.kt`, `ResourceRegistry.kt` | possession par device des références adapter/device/queue et de toute référence acquise ensuite |
| Wrappers & mappers | `dawn4k/.../*.kt`, `dawn4k/.../mapper/*` | une classe `Dawn*` par interface WebGPU ; tables explicites de descripteurs/énumérations par nom |
| Point d'entrée public | `dawn4k/.../DawnContext.kt`, `Adapter.kt`, `Device.kt` | `DawnContext.create(config)` → `requestAdapter` → `requestDevice` |

### Bindings générés

Le package `org.graphiks.dawn4k.native` est généré par kextract depuis la
release Dawn figée dans `bindings/dawn.lock.json` (voir
[Binding natif Dawn](native-binding.fr.md)). Le backend le consomme tel quel :
aucune constante numérique n'est jamais supposée, et chaque conversion
d'énumération/drapeau passe par une table de constantes nommées dans
`mapper/Enums.kt` et ses voisins.

### Le dispatcher sérialisé et le runtime

Chaque downcall natif et chaque règlement de callback passe par **un thread
worker dédié par runtime** (`NativeDispatcher`) : un exécuteur mono-thread sur
la JVM, un `kotlin.native.Worker` dédié sur Kotlin/Native. L'affinité est un
contrat Dawn, pas une optimisation — Dawn indexe les piles de scopes d'erreur
`WGPUDevice` par thread appelant, donc le push, les appels validés et le pop
doisent partager un thread. Les appels venus d'un autre thread sont *routés*
vers le worker, jamais confinés au thread appelant ni rejetés par lui.

`DawnRuntime` possède la `WGPUInstance`, découvre les adapters et les devices
par des requêtes à callbacks, et fait tourner une pompe d'événements
périodique (`wgpuInstanceProcessEvents`) **seulement pendant que des
enregistrements de callbacks sont ouverts**. Chaque opération en vol est une
`PendingOperation` : un résultat natif qui court contre la coroutine qui
l'attend sans jamais fuiter la référence native qu'il porte — une attente
annulée ou abandonnée règle quand même sa livraison tardive sur le worker,
avec une libération exactement unique.

### Sessions device et registre de ressources

Une `DeviceSession` possède la référence d'adapter qu'elle a acquise
(`wgpuAdapterAddRef`), le device et sa file. Toute ressource créée ensuite est
enregistrée dans le `ResourceRegistry` de la session avec un callback de
destruction optionnel et un callback de libération obligatoire :

- les objets **à simple refcount** (samplers, layouts, pipelines, encodeurs…)
  sont libérés par `close()` immédiatement ;
- les objets **à destruction** (buffers, textures, query sets) sont détruits
  par `close()` mais gardent une entrée *tombstone* jusqu'au démontage, car
  Dawn rend des objets d'erreur dont les handles doivent rester valides pour
  les erreurs natives tardives.

Fermer une session libère chaque référence enregistrée — en ordre inverse
d'acquisition, avant les références device et adapter — le tout sur le
dispatcher. Les compteurs de debug (`debugRemainingRefs`,
`debugOpenCallbacks`) sont ce que les tests utilisent pour assert les états
de possession sans jamais déréférencer la mémoire libérée.

### Wrappers, gardes et mappers

Chaque wrapper `Dawn*` emprunte son handle natif et refuse les objets étrangers
(`requireDawn*`) avant qu'un downcall ne lise un handle d'une autre session. Les
passe-encoders libèrent leur handle sur `end()` et l'encodeur de render bundle
sur `close()`, donc chacun porte une garde Kotlin qui refuse les commandes
tardives avec une `IllegalStateException` **avant** le downcall — `finish()` du
command encoder ou du bundle encoder ne libère **pas** délibérément, donc
finir deux fois est laissé à la validation de Dawn. Les labels sont des
métadonnées côté Kotlin : les vues C de label gardent leurs défauts sur chaque
downcall.

## La limitation de la `ArrayBuffer` empruntée

`GPUBuffer.getMappedRange` renvoie une `ArrayBuffer` qui **enveloppe la mémoire
réellement mappée** — une vue empruntée, jamais une copie CPU. Dès que le
buffer est unmappé (ou fermé), la mémoire rend au device et la vue est
**invalide**. Ce n'est délibérément **pas gardé** : aucun wrapper ne peut savoir
quelles vues sont encore détenues. Ne lisez ni n'écrivez jamais une vue de
plage mappée après `unmap()` — le faire est un comportement indéfini, pas une
exception Kotlin.

## Contrat WebGPU publié

`GPUBuffer.usage` et `GPUTexture.usage` renvoient des masques typés qui préservent
les bits du descripteur, y compris les bits inconnus. Les `requiredLimits` du
device acceptent des `GPURequiredLimits` partielles : une propriété nulle utilise
la sentinelle indéfinie 32 ou 64 bits du header Dawn verrouillé ; un zéro explicite
reste zéro. Les slots nuls des layouts de pipeline, buffers de vertex, cibles
couleur, attachments de passe et formats de bundle gardent leurs indices et
utilisent leur représentation native vide respective.

`GPUDevice.awaitLost()` observe un résultat natif `GPUDeviceLostInfo` partagé.
Annuler un observateur ne détruit ni le device ni les autres attentes. La pompe
d'événements progresse les notifications pendant ces attentes, même sans autre
opération GPU en vol. Les deux routes de callback sont fermées et leur arrêt est
prouvé avant de compléter le résultat terminal ; les observateurs tardifs
reçoivent ce même résultat. `close()` détruit explicitement le device natif,
traite sa notification et libère les références possédées. Une perte native déjà
reçue reste prioritaire sur la fermeture réentrante ultérieure du contexte, sans
omettre la preuve d'arrêt des callbacks. Le callback d'erreur non capturée du
descripteur reste ordonné par le dispatcher.

## Couverture des cibles face à `:dawn4k-native`

`:dawn4k-native` publie des bindings pour une large matrice (JVM, Android,
macOS, Linux x64, Android NDK, iOS et tvOS). Le backend `:dawn4k` déclare
désormais la même matrice Kotlin : JVM, Android, `macosArm64`, `linuxX64`,
les trois cibles iOS et les deux cibles tvOS. Android est arrivée en dernier,
après que `webgpu-api` a gagné la fabrique `ArrayBuffer.wrap(address, size)`
par adresse empruntée qu'exige une plage mappée GPU (Graphiks-org/WebGPU#135) :

| Cible | `:dawn4k-native` | `:dawn4k` | Statut de validation |
| --- | --- | --- | --- |
| JVM | oui | oui (validée) | tests JVM et suite GPU sur Metal |
| `macosArm64` | oui | oui (validée, Metal) | tests K/N et suite GPU sur Metal |
| `linuxX64` | oui | oui | un hôte Linux avec GPU est requis pour valider |
| iOS (`arm64`/`x64`/simulateur) | oui | oui | compilée, binaires de test désactivés comme `:dawn4k-native` — la validation exige un device ou un simulateur démarré |
| tvOS (`arm64`/simulateur) | oui | oui | compilée, binaires de test désactivés comme `:dawn4k-native` — la validation exige un device ou un simulateur démarré |
| Android | oui | oui | compilée comme `:dawn4k-native` (pas de compilation de test hôte) ; la plage mappée empruntée passe par le `ArrayBuffer.wrap(address, size)` amont |

Sur Android, la vue empruntée de `getMappedRange` et le chemin rapide
d'upload passent par les fabriques publiques de `webgpu-api` :
`wrap(address, size)` pour la vue empruntée, et l'adresse du `ByteBuffer`
direct pour l'upload, avec repli par copie dès que l'accesseur d'adresse est
indisponible — le même contrat de dégradation que l'actual JVM. La dépendance
`suite-acid-tests` vit dans un source set de test intermédiaire desktop
uniquement, car la suite acid publiée n'a pas de variantes Apple mobiles.
Le témoin acid du contrat public (`PublicContractGpuTest`) tourne via les tâches
desktop standards ; les tests GPU communs tournent aussi sur les simulateurs
Apple. Les utilitaires de test signalent l'absence d'adapter, tandis que la CI
CPU stricte exige un adapter. Les runtimes JVM et Android partagent leurs
actuals de dispatcher et de verrou via un source set `jvmSharedMain`.

## Règles de possession pour l'appelant

- fermez les **sessions avant leur runtime**, et les adapters/devices en ordre
  inverse de création (`DawnContext.close()` est idempotent, comme chaque
  `close()`) ;
- une ressource appartient à exactement une session de device — la passer à un
  autre device est refusé avant qu'un handle ne soit lu ;
- un command buffer ne peut pas être re-soumis, et un encodeur fini ne doit pas
  être fini à nouveau (Dawn observe les deux comme des erreurs de validation
  non capturées).
