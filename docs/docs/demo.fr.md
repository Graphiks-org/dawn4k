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

1. **Fenêtre** : Compose Desktop crée la fenêtre. Son API publique `windowHandle`
   fournit le pointeur `NSWindow` ; `kffi-objc` récupère sa `NSView` de contenu.
2. **Couche Metal** : une `CAMetalLayer` dédiée est installée au-dessus des enfants
   Compose, sans remplacer sa couche de fond. Les appels AppKit utilisent une
   source de boucle d'événements en modes communs pour éviter les gels pendant un
   glisser ou un redimensionnement. Le pont Objective-C utilise FFM sur JVM, sans
   JNA ni compilation native.
3. **Surface** : une `WGPUSurface` Dawn est créée sur le `CAMetalLayer`
   (`WGPUSurfaceSourceMetalLayer`), configurée en `BGRA8Unorm` / `Fifo` et
   redimensionnée selon la taille physique en pixels, y compris sur écran Retina.
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

Sur macOS, les tests vérifient la conservation de la couche Compose, les appels
AppKit pendant un glisser, le redimensionnement et la libération des ressources.
Un test d'intégration ouvre une vraie fenêtre Compose et vérifie la présentation
d'une frame, les tailles physiques en pixels et l'annulation lors d'une fermeture
précoce ; ces tests nécessitent une session graphique. La visibilité des particules
et la continuité de l'animation pendant un glisser sont aussi vérifiées manuellement.
