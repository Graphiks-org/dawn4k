# Décommissionnement de l'ancien toolkit dawn4k

État au 16/11/2026, après le merge des PR #8 (dawn4k) et #157/#158 (WebGPU).

## Point de départ

`dawn4k-webgpu-toolkit` est publié et à parité fonctionnelle avec l'ancien toolkit.
Mais il **emprunte son chemin de présentation** à `dawn4k-toolkit-surface` : il n'a
aucun code natif propre. C'est le verrou du décommissionnement.

### Qui dépend de qui

```
dawn4k-toolkit-core        (1607 l.)
      ↑
dawn4k-toolkit-surface     (2814 l.)  ← dawn4k-webgpu-toolkit
      ↑                                 dawn4k-toolkit-examples
dawn4k-toolkit-glfw        (1523 l.)  ← dawn4k-demo
      ↑                                 dawn4k-toolkit-examples
dawn4k-toolkit-surface              ← dawn4k-webgpu-toolkit
```

Consommateurs de l'ancien toolkit :

| Consommateur | Fichiers | Ce qu'il utilise |
| --- | --- | --- |
| `dawn4k-webgpu-toolkit` | 9 | présentation, cibles natives, GLFW, options |
| `dawn4k-demo` | 18 | présentation, cibles natives, session |
| `dawn4k-toolkit-examples` | 12 | session, fenêtre, vues mobiles |

### Ce que le nouveau module emprunte

- `openDawnSurface`, `DawnPresentedSession`, `DawnPresentation` — le chemin de présentation
- `DawnSurfaceTarget`, `DawnTargetLease`, `PixelExtent` — le socle des cibles
- `DawnSurfaceTargets`, `NativeSurface`, `PresentationController` (internes) — le natif
- `DawnMainThreadLoop`, `DawnWindow`, `DawnWindowOptions`, `DawnWindowEvent` — GLFW
- `DawnSurfaceView`, `DawnMetalView` — les vues mobiles

## Ordre des opérations

Chaque étape laisse le dépôt vert. On ne passe à la suivante qu'après CI verte.

### Étape 1 — Déplacer le socle des cibles (sans risque)

Déplacer de `dawn4k-toolkit-surface` vers `dawn4k-webgpu-toolkit` :

- `DawnSurfaceTarget`, `DawnTargetLease`, `PixelExtent`, `DawnTargetState`
- `DawnSurfaceTargets`, `DawnPlatformOwner`, `DawnTargetRetention`, `CountingTargetRetention`
- `internal/`: `NativeSurface`, `NativeSurfaceSources`, `SurfaceAttachment`,
  `SurfaceCapabilities`, `TargetLifecycle`, `HandleTargetSupport`

Ces fichiers ne dépendent que de `:dawn4k` et `kffi`. L'ancien toolkit les
réexporte le temps de la transition (`typealias`) pour ne rien casser.

**Vérification** : `:dawn4k-webgpu-toolkit:check` + `:dawn4k-toolkit-surface:check`.

### Étape 2 — Déplacer le chemin de présentation

Déplacer `DawnPresentation`, `DawnPresentedSession`, `PresentationController`,
`DawnFrame`, `DawnSurfaceConfiguration`, `DawnPresentationOptions`,
`DawnAlphaMode`, `DawnPresentMode`, `openDawnSurface`.

Le handle du nouveau module ouvre alors sa présentation avec du code local.
`dawn4k-toolkit-surface` ne garde que des `typealias` de compatibilité.

**Vérification** : le TCK et les tests desktop (Metal, Vulkan, D3D12) passent.

### Étape 3 — Déplacer la couche GLFW

Déplacer `DawnMainThreadLoop`, `DawnWindow`, `DawnWindowOptions`,
`DawnWindowEvent`, `GlfwRuntime` vers un module `dawn4k-webgpu-toolkit-glfw`,
ou dans `jvmMain` du module principal.

**Vérification** : les exemples desktop démarrent une fenêtre.

### Étape 4 — Migrer les consommateurs

- `dawn4k-toolkit-examples` : passer de `withDawnSession`/`withDawnWindow` aux vues
- `dawn4k-demo` : idem

**Vérification** : les exemples s'exécutent, les tests GPU de la demo passent.

### Étape 5 — Supprimer

Supprimer `dawn4k-toolkit-core`, `dawn4k-toolkit-surface`, `dawn4k-toolkit-glfw`,
retirer les `typealias` de compatibilité, mettre à jour `settings.gradle.kts`,
`build.gradle.kts`, `kmp-publish.gradle.kts` et `docs/build.gradle.kts`.

## Ce qui n'est pas déplaçable tel quel

- **Les vues mobiles** (`DawnSurfaceView`, `DawnMetalView`) dépendent d'Android
  et d'UIKit : elles restent dans un source set mobile du module principal.
- **`DawnSession`** : à supprimer purement, remplacé par `ToolkitSessionView`.
- **`DawnExecutionOwner`** : à supprimer, remplacé par `Session.executionOwner`
  (PR WebGPU #158).

## Décisions à prendre

1. **Un module GLFW séparé ou `jvmMain` ?** Un module séparé garde les natives
   LWJGL hors du chemin commun ; `jvmMain` simplifie les dépendances.
2. **Compatibilité `typealias` ou suppression sèche ?** **Tranché : suppression
   sèche.** Seul utilisateur, lib non publiée, donc pas de `typealias` de
   compatibilité : on déplace puis on supprime, sans couche intermédiaire.
