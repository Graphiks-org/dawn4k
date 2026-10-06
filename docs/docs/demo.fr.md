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

Le panneau Compose flottant propose **Pause/Resume**, **Reset** et le
choix du nombre de particules (256, 1024, 4096, 16384, 65536, selon les limites
du GPU). Réinitialiser restaure les positions et vitesses initiales, même en pause.
Changer le nombre recrée uniquement la scène, pas le device ni la surface.
La pause fige la simulation mais conserve le rendu et le redimensionnement.

Compose est transparent au-dessus de Metal. Cette transparence native impose une
fenêtre sans bordure système : déplacer via la barre de titre, redimensionner
par les bords et utiliser **Close** pour quitter. L'interface est en anglais. Les contrôles sont désactivés
pendant l'initialisation ou après une erreur fatale, affichée dans le panneau.

## Fonctionnement

1. **Fenêtre** : Compose Desktop crée la fenêtre. Son API publique `windowHandle`
   fournit le pointeur `NSWindow` ; `kffi-objc` récupère sa `NSView` de contenu.
2. **Couche Metal** : une `CAMetalLayer` dédiée est installée sous les enfants
   Compose transparents, sans remplacer leur couche de fond. Les appels AppKit utilisent une
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
précoce, ainsi que pause/réinitialisation/changement du nombre en direct ; ces tests
nécessitent une session graphique. La visibilité des particules
et la continuité de l'animation pendant un glisser sont aussi vérifiées manuellement.
