# Démo : ParticleScene dans une fenêtre desktop

Le module `:dawn4k-demo` est une application desktop macOS qui ouvre une vraie
fenêtre et affiche la scène `ParticleScene` de
[`suite-demos`](https://github.com/Graphiks-org/WebGPU/tree/master/suite-demos)
via le backend `:dawn4k` (Dawn/Metal).

## Lancer

```bash
./gradlew :dawn4k-demo:run
```

Une fenêtre s'ouvre avec des particules animées. macOS uniquement (la surface
Metal et le pont Objective-C sont spécifiques à macOS).

## Fonctionnement

1. **Fenêtre** : Compose Desktop crée la fenêtre. La démo atteint le
   `SkiaLayer` sous-jacent (via le `ComposeContainer` de Compose) et lit le
   handle natif de la `NSView`.
2. **Couche Metal** : le `CAMetalLayer` de la `NSView` est récupéré (ou créé et
   installé) via `kffi-objc` — un pont Objective-C pur JVM sur FFM, sans JNA,
   sans compilation native.
3. **Surface** : une `WGPUSurface` Dawn est créée sur le `CAMetalLayer`
   (`WGPUSurfaceSourceMetalLayer`), configurée en `BGRA8Unorm` / `Fifo`.
4. **Boucle de rendu** : chaque frame acquiert la texture de surface, encode
   la `ParticleScene` (passe compute + passe render), soumet et présente.

La surface vit entièrement dans le module démo : `:dawn4k` n'expose que trois
accesseurs minimaux « platform integrator » (`DawnContext.nativeBridge()`,
`DawnDevice.nativeHandle()`, `DawnAdapter.nativeHandle()`) — pas d'API
`GPUSurface` publique.

## Tests

```bash
./gradlew :dawn4k-demo:test
```

Les tests sont headless (sans fenêtre) : ils vérifient le pont ObjC (NSView →
CAMetalLayer) et les wrappers de texture empruntée. La boucle de rendu complète
est validée manuellement sur macOS.
