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
| Runtime & mailbox | `dawn4k/.../internal/DawnRuntime.kt`, `CallbackMailbox.kt`, `PendingOperation.kt` | appels directs, règlements de callbacks explicitement progressés et suivi de possession |
| Sessions device & possession des ressources | `dawn4k/.../internal/DeviceSession.kt`, `OwnedReference.kt` | références adapter/device/queue possédées par la session ; ressources possédées individuellement par l'appelant |
| Wrappers & mappers | `dawn4k/.../*.kt`, `dawn4k/.../mapper/*` | une classe `Dawn*` par interface WebGPU ; tables explicites de descripteurs/énumérations par nom |
| Point d'entrée public | `dawn4k/.../DawnContext.kt`, `Adapter.kt`, `Device.kt` | `DawnContext.create(config)` → `requestAdapter` → `requestDevice` |

### Couche de possession du toolkit

`dawn4k-toolkit-core` → `dawn4k` ajoute la durée de vie GPU, un propriétaire fixé
à un thread du système et la progression. `dawn4k-toolkit-surface` → core ajoute
les cibles natives retenues, le choix d’un adapter compatible et les frames à
portée définie. `dawn4k-toolkit-glfw` → surface ajoute les fenêtres desktop et
leur boucle sur le thread principal du processus. Core et surface reprennent les
neuf cibles Kotlin du backend ; GLFW déclare ses trois cibles desktop. Ces modules
ne dépendent pas de Compose. La démo utilise désormais les factories publiques
du toolkit et conserve sa scène, ses contrôles et son redémarrage côté application.
Voir [Dawn4k toolkit](toolkit.fr.md) pour la possession, la libération des vues
mobiles, les modes de progression et la couverture réellement exécutée.

### Bindings générés

Le package `org.graphiks.dawn4k.native` est généré par kextract depuis la
release Dawn figée dans `bindings/dawn.lock.json` (voir
[Binding natif Dawn](native-binding.fr.md)). Le backend le consomme tel quel :
aucune constante numérique n'est jamais supposée, et chaque conversion
d'énumération/drapeau passe par une table de constantes nommées dans
`mapper/Enums.kt` et ses voisins.

### Threads et progression choisis par le consommateur

Le backend `:dawn4k` ne crée **aucun worker, dispatcher ou job de pompe**. Les downcalls,
y compris l'émission des requêtes asynchrones, s'exécutent sur l'appelant.
L'application choisit ses threads, l'ordre, la fréquence et l'annulation.

Appeler explicitement `DawnContext.processEvents()` : un lot fini de mailbox,
`wgpuInstanceProcessEvents`, puis un autre lot fini. La progression concurrente
ou réentrante est refusée. Les callbacks d'erreur utilisateur s'exécutent sur cet
appelant ; aucune méthode suspendue/await ne pompe implicitement.
Les erreurs non capturées sont transmises au callback du descripteur sans
historique interne. Sans callback, ou s'il lève une exception, stderr reçoit le
type et le message de l'erreur GPU ; les exceptions du callback incluent aussi
leur stack trace sans interrompre la progression. La journalisation et la
conservation des erreurs appartiennent à l'application.
Annuler une attente n'annule pas
la livraison native : ses résultats tardifs sont libérés pendant la progression.
La fin native et le transfert de possession Kotlin sont suivis séparément.

Pour un device partagé concurrent, choisir
`DawnConfig(implicitDeviceSynchronization = true)` **avant les requêtes device**.
La feature native Dawn `ImplicitDeviceSynchronization` est vérifiée puis fusionnée
avec les features requises du descripteur. Par défaut `false` : le consommateur
synchronise aussi les appels hors encodage, dont la progression qui ticke les
devices. Ce mécanisme natif n'est ni un scheduler Kotlin ni une promesse de gain.

Chaque tâche possède exclusivement son encoder et ses passes. Les ressources
immutables peuvent être partagées entre encoders indépendants. Joindre les tâches
avant publication des command buffers et ordonner la queue côté application.
Ne jamais courir map/unmap, accès mappés, destroy ou close contre l'utilisation.

Les scopes d'erreur Dawn sont **locaux au thread OS** : push, appels validés et
émission native du pop doivent garder le même thread. Un single-thread executor
possédé par le consommateur convient sur JVM ;
`Dispatchers.Default.limitedParallelism(1)` ne garantit pas cette identité.

### Sessions device et ressources possédées par l'appelant

Une `DeviceSession` possède la référence d'adapter qu'elle a acquise
(`wgpuAdapterAddRef`), le device et sa file. Chaque wrapper de ressource possède
individuellement sa référence native via un `OwnedReference` local :

- les objets **à simple refcount** (samplers, layouts, pipelines, encodeurs…)
  sont libérés par `close()` immédiatement ;
- les objets **à destruction** (buffers, textures, query sets) sont détruits
  par `close()`, qui libère immédiatement leur référence native ensuite.

L'appelant ferme les ressources avant leur device. Fermer une session libère sa
file, pas les wrappers de ressources : oublier close peut provoquer une fuite.
Les références device/adapter restent possédées jusqu'au règlement explicite de
la perte et de la quiescence des routes. Aucun registre, finalizer ou nettoyage
implicite par le GC n'est ajouté. Les actions natives s'exécutent sur l'appelant.

`close()` est idempotent. Les buffers, textures et query sets ne conservent plus
de handle détruit pour une validation tardive. Les métadonnées copiées restent
lisibles ; toute utilisation d'un handle fermé lève `IllegalStateException`
avant un appel natif. Les gardes ne protègent pas une course utilisation/close :
arrêter et joindre les utilisateurs, puis régler les opérations en attente avant
la fermeture. Un résultat asynchrone non réclamé reste possédé par son opération
jusqu'au transfert à l'appelant.

### Wrappers, gardes et mappers

Chaque wrapper de ressource `Dawn*` possède sa référence native ; `DawnQueue`
emprunte seulement celle de la session. Les mappers castent les objets
WebGPU vers leurs wrappers Dawn ; la validation WebGPU de Dawn vérifie les usages
inter-device. Les
passe-encoders libèrent leur handle sur `end()` et l'encodeur de render bundle
sur `close()`, donc chacun porte une garde Kotlin qui refuse les commandes
tardives avec une `IllegalStateException` **avant** le downcall — `finish()` du
command encoder ou du bundle encoder ne libère **pas** délibérément, donc
finir deux fois est laissé à la validation de Dawn. Les labels sont des
métadonnées côté Kotlin : les vues C de label gardent leurs défauts sur chaque
downcall.

Les interfaces externes de render bundle et de passe n'ont pas de close.
`DawnRenderBundle` implémente `AutoCloseable` : caster un bundle typé par
l'interface vers son wrapper Dawn pour le fermer. `DawnComputePassEncoder` et
`DawnRenderPassEncoder` proposent aussi close pour une passe abandonnée après
une exception, sans appeler end implicitement. Les vues temporaires de la passe
de rendu sont libérées sur end comme sur close.

## Mécanique de session du toolkit

Chaque composant du cycle de vie du toolkit garantit un invariant qu'un simple
ordonnancement ne peut pas assurer. Un contributeur modifiant `dawn4k-toolkit-core`
ou `dawn4k-toolkit-surface` doit savoir à quoi sert chaque pièce ; les règles
publiques vivent dans [Dawn4k toolkit](toolkit.fr.md).

**Propriétaire d'exécution fixé à un thread.** Le dispatcher d'une session est
fixé à un thread du système et retient le premier thread vu : un changement de
thread échoue immédiatement au lieu de corrompre les scopes d'erreur Dawn,
locaux au thread OS. `limitedParallelism(1)` est sériel sans être fixé, donc un
dispatcher emprunté doit fournir lui-même cette garantie. Le propriétaire refuse
aussi la progression réentrante : les opérations de session ne s'exécutent pas
dans une pompe d'événements, et une pompe ne s'exécute pas dans elle-même.

**Admission.** `gpu.run` associe un mutex non réentrant à un marqueur de contexte
coroutine. Un `run` imbriqué voit le marqueur et rejoint l'admission externe au
lieu de s'interbloquer ; un arrêt attend cette même admission comme barrière de
drain du travail applicatif. `run` enregistre aussi son job auprès du cycle de vie :
c'est ainsi que la perte du device et les erreurs fatales annulent exactement le
travail admis.

**Bootstrap transactionnel.** Le démarrage acquiert les handles natifs dans
l'ordre — contexte, rétention, adapter, device, présentation — et chaque étape
est couverte par une action de rollback dans une transaction ; la rétention
enregistre la sienne avant l'acquisition, couvrant ainsi une annulation pendant
l'acquire. L'annulation de l'appelant n'exécute jamais ce rollback en place : il
appartient à un scope `NonCancellable`, borné par `shutdownTimeout` par
tentative, et rejoué jusqu'à transaction vide. L'échéance de démarrage
s'exécute sur l'horloge GPU réelle, pas sur le scheduler — potentiellement
virtuel — de l'appelant.

**Arrêt par étapes.** Un arrêt libère la présentation, puis ferme device, adapter
et contexte avec un drain entre chaque étape, en n'avançant son compteur d'étape
qu'après succès : une tentative rejouée reprend là où la précédente s'était
arrêtée. Les appelants concurrents partagent une seule tentative en vol ; un
timeout ou un échec préserve les handles non fermés, publie l'état et planifie
une
nouvelle tentative en arrière-plan sur le propriétaire, tandis que
l'application reste libre de rappeler `shutdown()`.

**Générations de cible.** L'identité native d'une cible est un compteur de
génération. Le watcher asynchrone de la présentation et un `replaceTarget` en
vol peuvent courir ensemble ; un `detachTarget` dont la génération ne correspond
plus à l'attachement actif ne fait rien, donc un detach tardif ne peut jamais
tuer la nouvelle cible. La rétention revérifie la génération après son `retain`
suspendu, car le handle peut changer d'identité pendant la constitution du bail.

**Acquittement de libération.** Dawn garde des références natives vers une
surface après que l'application a cessé de l'utiliser : un hôte ne doit donc pas
détruire sa fenêtre ou sa vue de son propre chef. Le `retire()` d'un bail
s'exécute sur le propriétaire de plateforme et ne revient qu'une fois toutes les
références de surface disparues ; une libération échouée ou interrompue reste
enregistrée et rejouable au lieu d'être présumée acquittée.

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
Annuler un observateur ne détruit ni le device ni les autres attentes. Le
consommateur progresse les notifications, même sans autre opération en vol.
Les deux routes sont fermées et leur arrêt prouvé avant le résultat terminal ;
les observateurs tardifs reçoivent le même résultat. `close()` détruit le device ;
les appels suivants à `processEvents()` règlent sa perte et libèrent les références
device/adapter. Fermer pendant la progression, avec des opérations pertinentes
en vol ou des enfants contexte ouverts lève avant mutation. Arrêter/joindre les
producteurs, régler les résultats, fermer devices/adapters, drainer le teardown,
puis fermer le contexte. `hasPendingOperations()` est un snapshot, pas une barrière
de shutdown ; les routes permanentes inactives et observateurs de perte ne le
maintiennent pas indéfiniment à true.

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
actuals de verrou via un source set `jvmSharedMain`.

## Règles de possession pour l'appelant

- fermez les **sessions avant leur runtime**, et les adapters/devices en ordre
  inverse de création (`DawnContext.close()` est idempotent, comme chaque
  `close()`) ;
- une ressource appartient à exactement une session de device — la passer à un
  autre device est refusé avant qu'un handle ne soit lu ;
- un command buffer ne peut pas être re-soumis, et un encodeur fini ne doit pas
  être fini à nouveau (Dawn observe les deux comme des erreurs de validation
  non capturées).
