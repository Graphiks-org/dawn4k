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

suspend fun useGpu() = coroutineScope {
    DawnContext.create(DawnConfig(implicitDeviceSynchronization = true)).use { context ->
        val events = launch {
            while (isActive) { context.processEvents(); delay(1) }
        }
        try {
            context.requestAdapter().getOrThrow().use { adapter ->
                adapter.requestDevice().getOrThrow().use { device ->
                    // Threads d'encodage et ordre de queue choisis par l'application.
                }
            }
        } finally {
            withContext(NonCancellable) {
                events.cancelAndJoin()
                withTimeout(10_000) {
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
pendant le cleanup `NonCancellable` qui attend le GPU, puis drainer résultats
tardifs et teardown avec un timeout application. Un timeout **n'autorise pas** la
libération de handles actifs : close refuse les opérations en vol et enfants
ouverts. Cet exemple n'est pas une stratégie complète face à un driver bloqué.

Migration : remplacer la pompe automatique par `processEvents()` explicite ;
`drainEvents()` est un alias déprécié. `NativeBridge.call` est déprécié et exécute
inline, sans lock ni dispatcher. Push de scope, appels validés et émission du pop
doivent garder un **thread OS**, pas seulement une exécution de coroutines
sérialisée. Voir [Architecture](architecture.fr.md).

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
