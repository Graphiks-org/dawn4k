# Dawn4k toolkit

Le toolkit ajoute des sessions GPU et la présentation au backend Dawn existant.
Il reste dans ce dépôt. Les buffers, textures et pipelines de l’application
utilisent toujours les APIs WebGPU ordinaires.

| Dépendance | Responsabilité | Cibles Kotlin |
| --- | --- | --- |
| `org.graphiks:dawn4k-toolkit-core` | Sessions sans fenêtre, thread GPU, progression, arrêt | JVM, Android, macOS ARM64, Linux x64, iOS ARM64, simulateur iOS ARM64, simulateur iOS x64, tvOS ARM64, simulateur tvOS ARM64 |
| `org.graphiks:dawn4k-toolkit-surface` | Cibles externes, vues mobiles fournies, frames | Les mêmes neuf cibles |
| `org.graphiks:dawn4k-toolkit-glfw` | Fenêtres desktop et boucle principale | JVM, macOS ARM64, Linux x64 |

Les runtimes natifs JVM couvrent macOS ARM64, Linux x64/ARM64 et Windows x64.

Choisir la dépendance correspondant à l’hôte. Surface dépend de core ; GLFW
dépend de surface. Utiliser la même version publiée que `dawn4k`. Le module
`dawn4k-toolkit-examples`, non publié, contient les exemples complets. Core ne
dépend ni de GLFW, ni d’AWT, de Compose ou d’une interface graphique.

## Compute sans fenêtre

```kotlin
withDawnSession { gpu ->
    val result = runComputeExample(gpu) // module examples : [2, 4, 6, 8]
    println(result.contentToString())
}
```

Le bloc s’exécute déjà sur le thread GPU de la session. Pour une durée de vie
explicite, appeler `openDawnSession()`, conserver les ressources et leurs
utilisateurs dans `gpu.run { ... }`, puis appeler `gpu.shutdown()` après la fin
de ce bloc. `context`, `adapter`, `device` et `queue` sont des références
empruntées : ne pas les fermer séparément.

## Rendu offscreen et readback

`gpu.createRenderTexture(width, height, format)` crée une texture appartenant à
l’application, utilisable comme render attachment (cible de rendu) et source de
copie. L’appeler dans `gpu.run`. Créer sa vue dans un bloc `use`, encoder le
rendu, puis copier dans un staging buffer (buffer intermédiaire) ayant les usages
`CopyDst | MapRead`. Respecter l’alignement des lignes, soit 256 octets dans
l’exemple. Attendre `mapAsync`, copier les octets dans un tableau Kotlin, puis
appeler `unmap` avant de fermer le buffer. Aucune vue de mémoire mappée ne doit
survivre à `unmap`. Le test de pixels des exemples exécute ce parcours complet.

Pour redimensionner une texture, attendre ses utilisateurs avant de la remplacer.
Fermer les ressources de l’application avant de quitter le bloc GPU : la session
ne recense pas les ressources créées arbitrairement avec `device`.

## Fenêtre desktop

`withDawnWindow { gpu, window, presentation -> ... }` démarre la boucle GLFW sur
le thread principal du processus, crée un GPU compatible et possède sa fenêtre.
L’appeler depuis `fun main() = runBlocking { ... }`, avec `-XstartOnFirstThread` sur JVM macOS.
Ne pas déplacer cette boucle vers un worker. La JVM demande Java 25 HotSpot et
`--enable-native-access=ALL-UNNAMED`.

```kotlin
withDawnWindow { gpu, window, presentation ->
    TriangleRenderer(gpu, presentation.configuration.value!!.format).use { triangle ->
        presentation.withFrame { frame ->
            triangle.render(frame.view, frame.extent)
            frame.present()
        }
        gpu.queue.onSubmittedWorkDone().getOrThrow()
    }
}
```

Le lanceur complet répète les frames et observe `DawnWindowEvent.CloseRequested`.
La frame possède sa vue et la référence de texture acquise. Présenter au plus une
fois ; ne pas conserver ni fermer cette vue soi-même. Une erreur ou annulation
abandonne et libère la frame. Une cible indisponible ou de taille nulle suspend
la présentation ; un timeout de surface saute la frame. `window.resize(w, h)`
utilise les coordonnées logiques du contenu ; les dimensions de la frame et
`window.extent` utilisent les pixels physiques.

La forme persistante utilise `DawnMainThreadLoop.run { main ->
openDawnWindow(mainThread = main) }`. Garder la boucle active jusqu’à la fin de
l’arrêt. Toutes les fenêtres simultanées choisissent le même mode de possession
du runtime GLFW, `Owned` ou `Borrowed`. Le mode emprunté ne termine jamais le
runtime de l’appelant.

## Fenêtre ou vue native existante

Utiliser `DawnSurfaceTargets.metalLayer`, `.win32`, `.x11` ou `.wayland`, avec
le propriétaire UI/platform, un `StateFlow<DawnTargetState>` et les hooks de
rétention natifs. Des surcharges typées existent sur les plateformes concernées.
Les hooks doivent retenir réellement les handles ; le retour de `retire`
confirme que Dawn ne référence plus la cible. Les hooks personnalisés doivent
supporter une nouvelle tentative après interruption de leur libération.

`withDawnSurface(target) { gpu, presentation -> ... }` choisit un adapter
compatible avec cette surface précise. Le toolkit possède sa surface Dawn et la
référence retenue ; l’application possède la fenêtre externe. `replaceTarget`
préserve le GPU compatible. Sa politique optionnelle peut imposer le format du
pipeline existant. Un remplacement échoué conserve la politique précédente.
`detachTarget(generation)` ignore les demandes d’une ancienne génération.

Le protocole complet d’une cible — générations, rétention, suspension
d’acquisition et libération — figure dans la section Cycle de vie. Les appels UI
restent sur leur propriétaire UI ; l’encodage GPU reste dans `gpu.run`.
`gpu.executionOwner` est emprunté et réservé aux intégrations avancées qui
disposent de leur propre protocole d’admission.

## Android, iOS et tvOS

Android fournit `DawnSurfaceView` ; iOS et tvOS fournissent `DawnMetalView`.
Leur `target` publie générations, disponibilité et dimensions physiques.
L’attacher avec la même factory de surface. Garder le job de rendu et son
nettoyage dans le bloc GPU. Lors du retrait de la vue, annuler et rejoindre le
rendu de façon asynchrone, puis attendre `awaitRetirement()` avant de terminer
la destruction de l’hôte. Ne jamais bloquer le thread UI en attendant un thread
GPU qui a besoin de l’UI pour libérer les références natives. Apple conserve de
vraies références Core Foundation ; Android conserve une référence native-window
indépendante du wrapper Java. Les hôtes d’exemple montrent ce nettoyage asynchrone.

## Cycle de vie

`gpu.state` publie un `StateFlow<DawnSessionState>` ; les états sont des valeurs
distinctes, un observateur sait donc toujours dans quelle phase se trouve une
session.

| État | Signification | Utilisable dans cet état |
| --- | --- | --- |
| `Ready` | Démarrage réussi | `gpu.run`, présentation, arrêt |
| `ShuttingDown` | Une tentative d’arrêt est en cours | Rien de nouveau ; le travail admis se termine d’abord |
| `ShutdownTimedOut` | La tentative a dépassé `shutdownTimeout` ; les handles restent vivants | Nouvel appel à `shutdown()` ; le nettoyage continue |
| `Lost` | Le device a signalé une perte | Nettoyage seul ; créer une nouvelle session pour redémarrer |
| `Failed` | Une erreur fatale ou une tentative d’arrêt échouée a arrêté la session | Nettoyage seul ; créer une nouvelle session pour redémarrer |
| `Closed` | Tous les handles natifs sont fermés | Rien ; final et idempotent |

Les transitions : une tentative d’arrêt fait passer `Ready` → `ShuttingDown`, et
une tentative réussie se termine en `Closed`. Une tentative dépassée ou échouée
publie `ShutdownTimedOut` ou `Failed` en gardant vivants les handles non
fermés ;
l’application peut rappeler `shutdown()`, et le nettoyage en arrière-plan
réessaie aussi. La perte du device et les erreurs fatales n’interrompent qu’une
session `Ready`, annulent son travail admis et n’y retournent jamais.

Une cible surface externe suit son propre protocole, et le toolkit ne touche ses
handles natifs qu’à l’intérieur :

1. la cible publie `DawnTargetState(generation, extent, available)` en pixels
   physiques, avec une génération neuve chaque fois que le handle natif change
   d’identité ;
2. `retain()` retient le handle dans un bail ; le toolkit revérifie la
   génération après le `retain` suspendu et annule l’acquisition en cas
   d’écart ;
3. les capacités sont négociées et la surface configurée ; une taille nulle ou
   `available = false` suspend l’acquisition des frames au lieu de l’échouer ;
4. chaque `withFrame` acquiert une texture, présente au plus une fois et la
   libère ;
5. `replaceTarget` échange les cibles entre les frames en préservant le GPU
   compatible ; `detachTarget(generation)` ignore les générations obsolètes ;
6. `retire()` accuse la libération : Dawn ne référence plus le handle ; détruire
   l’hôte possédé par l’application seulement après cet acquittement.

## Progression, perte du device et arrêt

Le mode géré possède un dispatcher fixé à un thread du système et sa progression
d’événements. Un dispatcher externe fixé à un thread peut être emprunté avec
`DawnExecutionOwner.borrow`. `limitedParallelism(1)` ne fixe pas un thread du
système. Le propriétaire emprunté doit rester actif jusqu’à la fin du nettoyage.

Pour une progression manuelle, fournir `DawnExecutionMode.Manual(owner)`.
Le bootstrap progresse jusqu’au transfert vers `gpu.manualProgress`. Appeler
`processEvents()` sur le même propriétaire, depuis un seul driver (pilote de
progression), en cédant régulièrement l’exécution. Le driver s’exécute pendant
les suspensions du travail GPU. L’annuler et le rejoindre, appeler
`manualProgress.stop()`, puis arrêter la session hors de `gpu.run`. L’arrêt
persistant refuse un driver actif ; les helpers à portée définie le révoquent
lorsqu’ils terminent.

Observer `gpu.state` (la section Cycle de vie liste tous les états) ou
`awaitLost()`. La perte du device et le manque de mémoire
arrêtent les nouvelles admissions et déclenchent le nettoyage. Les ressources ne
sont pas rejouées automatiquement. Annuler et rejoindre les producteurs de
l’application et fermer leurs ressources dans le bloc GPU admis, puis créer
explicitement une nouvelle session si l’application souhaite redémarrer.

Les délais de tentative de démarrage et d’arrêt valent dix secondes par défaut.
Le démarrage couvre la rétention et la négociation des capacités. Le rollback
(annulation transactionnelle) reste propriétaire des acquisitions même si
l’appelant est annulé ; un démarrage échoué peut attendre en plus jusqu’à
`shutdownTimeout` pour ce rollback. Un appel natif ne peut pas être interrompu
de force. Un timeout d’arrêt n’autorise jamais à libérer des handles encore
référencés : le nettoyage réessaie en arrière-plan sur le propriétaire retenu,
et l’application peut rappeler `shutdown()`. Les erreurs de nettoyage restent
des exceptions supprimées de l’erreur initiale. Un arrêt réussi est idempotent ;
l’arrêt depuis son propre `gpu.run` est refusé.

Chaque tentative persistante d’arrêt de fenêtre reste bornée tant que sa boucle
principale tourne. La boucle GLFW à portée définie reste active jusqu’à la
libération de toutes les fenêtres, y compris les acquisitions partielles. Son
appel englobant peut donc dépasser le délai d’une tentative individuelle.

## Exécuter les exemples

Les lanceurs desktop accordent 60 secondes au démarrage GPU, y compris pour les adaptateurs logiciels.

```bash
./gradlew :dawn4k-toolkit-examples:runCompute
./gradlew :dawn4k-toolkit-examples:runTriangle
# Exécution graphique bornée pour validation :
./gradlew :dawn4k-toolkit-examples:runTriangle -Ptoolkit.example.frames=60
./gradlew :dawn4k-toolkit-examples:runComputeDebugExecutableMacosArm64
./gradlew :dawn4k-toolkit-examples:runTriangleDebugExecutableMacosArm64
./gradlew :dawn4k-toolkit-examples-androidApp:installDebug
```

Sur Linux Native, utiliser les tâches correspondantes `LinuxX64`. L’application
Android exécute le compute et affiche le triangle. Dans le projet Xcode des
exemples, choisir `ToolkitIOS` ou `ToolkitTVOS` ; sa phase de compilation construit
le framework Kotlin commun. Les exemples utilisent les APIs publiques.

Les bibliothèques core/surface et l’exemple Android demandent API 28 ou plus,
conformément à la dépendance de descripteurs.

## Vérification de publication

`./gradlew verifyToolkitPublication` publie les cinq bibliothèques localement et
vérifie exactement neuf cibles Kotlin pour core/surface et trois pour GLFW.
`python3 scripts/verify-toolkit-consumers.py --maven-root ~/.m2/repository`
construit une copie indépendante des exemples publics avec des coordonnées
Maven, des exécutables/frameworks Native et un APK Android minifié. Ce projet
n’importe aucune convention de build ni chemin de bibliothèque native du dépôt.

Les variantes Apple Native embarquent Dawn statiquement. Linux Native embarque
l’ELF Dawn vérifié dans son archive cinterop, vérifie son chargement avant le
bootstrap (initialisation), puis le charge depuis un fichier mémoire anonyme ou
un fichier temporaire si nécessaire. Le runtime Linux épinglé demande glibc 2.38
et libstdc++ avec `GLIBCXX_3.4.32` ; son inclusion ne réduit pas ces exigences.
Aucun chemin du dépôt n’est requis à l’exécution. La publication macOS reçoit
des bundles Dawn/GLFW Linux identifiés par leur source et leurs checksums
(sommes de contrôle), construits préalablement sur Ubuntu par le workflow.
Ces archives Linux ne pouvant être produites que sur un hôte Linux x86_64 (ou
fournies depuis celui-ci), la cible `linuxX64` n’y est déclarée que là-bas ou
lorsque `dawn.nativeBundle.linuxX64` et `glfw.nativeBundle.linuxX64` désignent
des bundles vérifiés. Les imports Gradle et IDE sur tout autre hôte restent
verts sans eux ; renseignez ces deux propriétés (comme la CI) pour compiler
les variantes Linux localement.

`./gradlew verifyToolkitCoverage` contrôle toute la matrice d’exécution à partir
des rapports portables `build/toolkit-coverage/**/summary.json` et de leurs
preuves JUnit/logs. `-Ptoolkit.coverage.dir=...` désigne les rapports téléchargés.
Un sous-ensemble local peut être déclaré explicitement avec
`-Ptoolkit.coverage.require=module:target:presentation:gpu` ; le comportement par
défaut et l’agrégation CI exigent toute la matrice. Adaptateur, tests ou marqueurs
d’exécution absents, tests échoués ou ignorés font échouer le contrôle. Le job
protégé `build-and-test` exige aussi la réussite de ce workflow.

## Couverture validée

Les variantes supportées et l’exécution GPU sont deux informations distinctes.
La validation locale de cette modification (2026-10-15) couvre macOS ARM64/JVM
avec Metal, Linux x64 émulé avec Vulkan llvmpipe et de vraies présentations
X11/Wayland, Android API 35 ARM64 et les simulateurs iOS/tvOS ARM64 démarrés avec
Metal. La démo de particules a également passé ses scénarios d’application
Android/iOS, contrôles et redémarrage compris. Les variantes Apple ARM64 pour
appareils et le simulateur iOS Intel ont été compilés ; cela ne constitue pas une
exécution GPU sur appareil physique ou simulateur Intel. Windows JVM, Linux
ARM64 JVM et les appareils physiques nécessitent leurs rapports sur hôte adapté.
Un job GPU obligatoire doit échouer en l’absence d’adapter ou si une suite
requise est sautée. Une compilation ne remplace jamais une exécution.
