# Intégration Compose

`:dawn4k-compose` pose une scène Dawn sous une interface Compose. Compose
produit l'interface ; dawn4k rend la scène ; le toolkit fournit les vues et
les cibles de surface. Aucun pixel ne traverse les couches : le compositeur
du système les empile.

## Rôle de chaque module

| Module | Responsabilité |
| --- | --- |
| `:dawn4k` | Le backend WebGPU : device, adapter, surfaces, présentation. Ne dépend jamais de Compose. |
| `:dawn4k-webgpu-toolkit` | Les vues natives (`DawnSurfaceView`, `DawnMetalView`), les cibles desktop (`DawnSurfaceTargets.metalLayer/win32/x11/wayland`), l'acquisition de frames, la progression. |
| `:dawn4k-compose` | Le collant Compose : `DawnViewport` place la couche native sous l'UI et expose `DawnViewportHost`. |

`:dawn4k-compose` ne contient aucun appel `wgpu*` : les surfaces s'acquièrent
par `DawnTargetLease.createSurface(context)` du toolkit.

## Le contrat

```kotlin
@Composable
expect fun DawnViewport(
    target: DawnSurfaceTarget,
    modifier: Modifier,
    onHost: (DawnViewportHost) -> Unit,
)

class DawnViewportHost(
    val backend: DawnBackend,
    val state: StateFlow<DawnTargetState>,   // génération, étendue, disponibilité
) : AutoCloseable
```

`onHost` arrive sur le thread UI de la plateforme (dispatcher Compose sur
JVM, thread principal sur Android et iOS). L'hôte est valable jusqu'à ce que
le composable quitte la composition ou que la plateforme détruise la couche ;
le consommateur retire toute lease toolkit qu'il a prise avant que l'hôte ne
disparaisse.

## Cibles du module

| Cible | Hôte |
| --- | --- |
| JVM (macOS, Windows, Linux) | l'actual desktop : tout `DawnSurfaceTarget` fourni par l'intégrateur |
| Android | la `DawnSurfaceView` du toolkit, via `AndroidView` |
| iOS (`arm64`, `simulateur arm64`) | la `DawnMetalView` du toolkit, via `UIKitView` |

Pas de tvOS (Compose ne le couvre pas), pas de Kotlin/Native Linux ni d'Apple
x64 (Compose 1.11 ne les publie pas). La cible native macOS est couverte par
l'actual JVM desktop.

## Exemple

```kotlin
// Android
DawnViewport(target = view.target, modifier = Modifier.fillMaxSize()) { host ->
    // host.state collect la génération et l'étendue de la vue
}

// Desktop : la cible vient de l'intégrateur, Wayland inclus
val target = DawnSurfaceTargets.wayland(display, surface, owner, states, retention)
DawnViewport(target = target, modifier = Modifier.fillMaxSize()) { host -> ... }
```

## Ce que ce module n'est pas

- Un moteur de rendu Compose : la scène reste rendue par Dawn.
- Un partage de texture : les couches sont empilées, aucune lecture croisée.
- Un producteur d'événements : l'input reste la responsabilité de Compose et
  de l'application.
