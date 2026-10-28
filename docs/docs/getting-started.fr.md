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

## Sessions gérées et présentation

Pour confier au toolkit le bootstrap, un thread GPU, la progression et l’arrêt,
ajouter `org.graphiks:dawn4k-toolkit-core` à la même version que le backend.
Ajouter `dawn4k-toolkit-surface` pour une vue native existante ou
`dawn4k-toolkit-glfw` pour une fenêtre desktop possédée par le toolkit. Utiliser
`withDawnSession { ... }` depuis une coroutine. Le [guide toolkit](toolkit.fr.md)
fournit les exemples compute/triangle, les hôtes Android/Apple, la progression
manuelle et les règles de délai et d’accusé de libération. L’exemple de backend
ci-dessous reste destiné aux intégrations possédées par l’appelant.

## Utiliser le backend WebGPU

`:dawn4k` implémente `org.graphiks.webgpu` sans scheduler ni pompe implicite.
Bootstrap minimal autonome (chemin de succès) :

```kotlin
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnContext
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUDevice

suspend fun useGpu() = coroutineScope {
    DawnContext.create(DawnConfig(implicitDeviceSynchronization = true)).use { context ->
        val events = launch {
            while (isActive) { context.processEvents(); delay(1) }
        }
        var adapter: GPUAdapter? = null
        var device: GPUDevice? = null
        try {
            val acquiredAdapter = context.requestAdapter().getOrThrow()
            adapter = acquiredAdapter
            device = acquiredAdapter.requestDevice().getOrThrow()
            // Threads d'encodage et ordre de queue choisis par l'application.
        } finally {
            withContext(NonCancellable) {
                events.cancelAndJoin()
                withTimeout(10_000) {
                    // Régler les requêtes annulées avant de fermer leurs owners.
                    while (context.hasPendingOperations()) {
                        context.processEvents()
                        delay(1)
                    }
                    try { device?.close() } finally { adapter?.close() }
                    while (context.hasPendingOperations()) {
                        context.processEvents()
                        delay(1)
                    }
                }
            }
        }
    }
}
```

La feature est opt-in (`false` par défaut) ; l'activer pour un device appelé en
concurrence. Sinon synchroniser aussi les appels hors encodage et la progression
des événements device. Utiliser des encoders indépendants, joindre les tâches
avant soumission et ne jamais courir fermeture/mapping contre leurs utilisateurs.

Après annulation, arrêter/joindre producteurs et attentes, conserver la progression
pendant le cleanup `NonCancellable` qui attend le GPU, puis arrêter/joindre le job
d'événements **avant de fermer les enfants device/adapter**. Drainer les résultats
tardifs avant cette fermeture et le teardown ensuite, avec un timeout application.
Un timeout **n'autorise pas** la
libération de handles actifs : close refuse les opérations en vol et enfants
ouverts. Cet exemple n'est pas une stratégie complète face à un driver bloqué.

Utiliser `processEvents()` pour progresser explicitement les callbacks ; aucune
attente ne pompe les événements. Les intégrateurs utilisent
`NativeBridge.instanceHandle()` et émettent les appels natifs directement, en
assurant eux-mêmes la synchronisation et la durée de vie.
Push de scope, appels validés et émission du pop
doivent garder un **thread OS**, pas seulement une exécution de coroutines
sérialisée. Voir [Architecture](architecture.fr.md).

## Possession des ressources

Les ressources appartiennent à l'appelant, pas à la session du device. Utiliser
`use` ou `try/finally` et les fermer avant le device/contexte. Par exemple, avec
la dépendance `webgpu-descriptors` :

```kotlin
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor

device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopyDst)).use { buffer ->
    // Utiliser le buffer ouvert ; joindre les utilisateurs avant de sortir.
}
```

Close est idempotent et libère immédiatement la référence native acquise.
Buffers, textures et query sets sont détruits avant cette libération. Les
métadonnées copiées restent lisibles ; les opérations sur un handle fermé sont
refusées. Fermer le device ne nettoie pas les références oubliées et aucun GC
ne le fait implicitement. La file reste empruntée au device, sans close séparé.

Arrêter et joindre les utilisateurs, puis régler les callbacks en attente avant
de fermer les ressources. Les gardes ne rendent pas une course utilisation/close
sûre. Pour un render bundle typé par l'interface externe, utiliser
`(bundle as org.graphiks.dawn4k.DawnRenderBundle).close()`. `end()` termine et
libère une passe ; les wrappers Dawn concrets implémentent aussi `AutoCloseable`
pour le nettoyage après exception, sans appel implicite à end.

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
