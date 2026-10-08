# Dawn particle demo: adaptive desktop, Android and iOS

## Intent and approved scope

Extend the existing Dawn particle demo to Android and iOS while completing the
desktop graphical-parity work. Reuse the actual Compose controls and particle
simulation, adapting layout to available space rather than demanding identical
screen geometry on phones and desktop.

The user selected:

- One merged desktop/mobile specification and implementation plan.
- Convert the existing `dawn4k-demo` module to Kotlin Multiplatform, rather than
  create a separate `dawn4k-demo-shared` module.
- Adaptive layout, common controls and common simulation behavior.
- First mobile milestone: actual execution and Dawn particle presentation on
  Android Emulator and iOS Simulator, not compilation alone or physical devices.
- Preserve Native execution in this session after written-spec/plan review.
- Permit a minimal separate Android APK launcher because AGP 9's supported
  Android KMP plugin produces a library. The launcher contains packaging and
  startup glue, not a second demo implementation. iOS similarly needs an Xcode
  application shell around the KMP framework.

This is a single cross-platform demo project with coupled migration, rendering,
UI and lifecycle workstreams; it is not a collection of independently designed
apps. Qualification gates and independently testable stages belong in the
merged plan.

## Superseded artifacts and retained evidence

This specification supersedes the fixed-layout-only contract in
`2026-10-08-demo-ui-parity-design.md` and the implementation scope of
`2026-10-08-demo-ui-parity.md` and
`2026-10-08-demo-ui-parity-native-scene.md` under `docs/superpowers/`.
Those files remain historical evidence; do not delete their qualification work.
The earlier keyboard-only Wayland decision remains superseded.

Retain the desktop requirements: native Wayland without X11/XWayland, working
X11/macOS/Windows backends, automatic Linux selection, real graphical controls,
no Wayland-specific global keyboard commands, resource ownership and first-error
retention. Mobile layout flexibility does not waive any of those requirements.

The primary ComposeWindow/JBR hosting probe already failed on Linux ARM64:
JBR 25.0.4.1+1-b635.70 selected WLToolkit, and Skiko 0.144.6 selected
SOFTWARE_COMPAT, but SkiaLayer initialization failed with
`Can't lock DrawingSurface` through HardwareLayer/JAWT before panel painting.
No pointer, scale or borrowed-parent lifetime qualification passed.

The selected Wayland direction is therefore the approved native Compose-scene
adapter, not a new default JBR runtime. Pinned Compose 1.11.1 exposes
CanvasLayersComposeScene under InternalComposeUiApi opt-in; its native raster,
input and Dawn coexistence still require qualification. Do not confuse inspected
API availability with a working host.

## User-visible contract

### Shared controls and state

- One common Compose definition supplies Pause/Resume, Reset, device-permitted
  particle counts, initialization text, selected/disabled states and error text.
- Counts remain the existing 256/1024/4096/16384/65536 choices filtered by device
  limits, with the existing low-limit behavior retained.
- GPU controls remain disabled before initialization and after terminal failure.
  The lifetime action remains available.
- No native host decides which simulation controls exist, imitates buttons or
  changes count/Reset semantics.
- Desktop lifetime action is Close. Mobile lifetime action is Arrêter while a
  session is active, then Relancer when stopped. Mobile never calls process exit
  to simulate an application Close button.
- Stop releases GPU/session resources and leaves the common UI mounted. Restart
  creates a fresh session with the normal initial particle/count/pause defaults;
  it is not a resume of the stopped simulation. The distinction is explicit in
  tests and documentation.
- An error remains visible for the failed session. An explicit Restart may create
  a new session and clear that session's error; late callbacks from the old
  session must not alter the new session.
- Global Space/R/+/-/Escape simulation commands are removed. Standard focused
  button activation and Tab/Shift-Tab navigation remain normal Compose behavior.

### Adaptive content

Layout decisions use available content space in logical dp after window/mobile
safe-area insets, not platform name or physical buffer pixel count.

- Wide content: controls left, particle viewport right.
- Compact content: particle viewport above, controls below, with count options
  wrapping and controls scrolling when needed.
- Proposed deterministic wide breakpoint for plan/testing: usable width at least
  720.dp and usable height at least 360.dp. Otherwise use compact layout. Wide
  panel is 300.dp; particle viewport consumes remaining space. These values are
  design parameters, not a requirement to classify every landscape phone wide.
- Compact controls occupy no more than half the usable height; their content
  scrolls rather than hiding actions. Viewport gets the remaining height. At
  unusually short dimensions the full content may scroll rather than overlap or
  create a negative-sized native viewport.
- Preserve current panel colors, labels, control model and simulation visuals.
  Mobile buttons have at least 48.dp touch targets; desktop retains compact
  pointer-friendly targets. Do not introduce a separate mobile component tree.
- Native borders and platform font rasterization may differ. At equal logical
  constraints, the selected layout mode, spacing rules and control states agree.
- Respect iOS safe areas and Android system bars/cutouts. Touch targets cannot
  extend beneath system UI. Rotation selects layout from new constraints.

The common viewport is a layout slot, not a Compose imitation of particles.
Each host places its native surface strictly inside its reported bounds.

## Module and build architecture

### Single KMP demo module

`dawn4k-demo` owns shared demo content, model/session state, portable simulation
orchestration and platform adapters. Source sets separate concerns:

- `commonMain`: ParticleControls/state, adaptive Compose content, platform-neutral
  viewport dimensions, session/lifecycle policy and shareable particle logic.
- `jvmMain`: desktop entry point, AWT/Compose Desktop hosts, FFM/C Wayland scene
  adapter, Win32/X11/AppKit specifics and desktop distribution support.
- `androidMain`: Compose/native-view hosting, Android lifecycle and Android-native
  Dawn surface integration; never desktop Java FFM or AWT.
- `iosMain`: Compose UI controller, native UIView/CAMetalLayer host and Apple
  lifecycle integration, shared by device and simulator source sets as supported.
- Common and platform tests mirror these boundaries. Existing desktop tests and
  opt-in native fixtures migrate without being silently dropped or gated out.

Initial application targets: JVM desktop, Android, iosArm64 and
iosSimulatorArm64. Retain existing backend target declarations; extra demo
architectures are added only if needed by the selected available simulator.
No tvOS demo expansion is included.

### Packaging-only launchers

The Android launcher module applies com.android.application, declares the
Activity/manifest and depends on the Android artifact of `dawn4k-demo`. Put
reusable Android host/render logic in the KMP module, not in this launcher.

The iOS Xcode app links the KMP framework, embeds the demo controller and forwards
application lifecycle if necessary. It has no duplicate controls or simulation.
Simulator debug execution does not require App Store provisioning.

Keep the desktop launcher and installDist use case. KMP migration may change
underlying Gradle task wiring; the plan must retain documented convenient run,
test and distribution commands and verify resource extraction from a packaged
distribution, not only from source/build directories.

### Build constraints

- Current pins: Kotlin 2.4.20, Compose 1.11.1, Skiko 0.144.6, AGP 9.0.0,
  Dawn archives selected by `bindings/dawn.lock.json` (currently v8077.0.0).
- Use the supported Android KMP library plugin already used by backend modules.
  Do not silently revert to legacy AGP APIs or downgrade plugins to manufacture
  a single-module APK build.
- Keep desktop JDK 25/FFM requirements separate from Android bytecode/runtime
  requirements. Sharing a source set cannot make Java FFM available on Android.
- Resolve the actual mobile variants of particle-scene/WebGPU dependencies.
  The current `suite-demos-jvm` dependency cannot move to commonMain unchanged.
  If a compatible artifact is absent, identify the minimal portable particle
  source/dependency migration and its license/API requirements in the plan;
  do not create a visibly different mobile simulation or assume publication.
- Keep native build/resource tasks platform-scoped. Android builds must not
  require Linux Wayland headers; desktop builds must not require a booted mobile
  simulator. Platform-specific execution prerequisites are explicit.
- No unrelated Gradle/backend rewrite, broad dependency refresh or public
  binding redesign solely for this demo.

## Renderer and surface boundaries

The current ParticleDemoRunner is not fully portable: it uses System.nanoTime,
Java reflection into DawnDevice/texture-view construction and a JVM particle
artifact. Remove these assumptions before claiming common native execution.

Separate simulation/frame orchestration from platform surface creation and
native texture-view adoption. Common code uses a monotonic clock and a typed
surface/frame contract, not java.awt.Rectangle, Java Class reflection or raw
platform pointers interpreted without an adapter.

Surface integration must provide: backend selection, physical extent,
capability negotiation, configure/acquire/present, texture-view access and
deterministic borrowed-frame release. Preserve serialized Dawn calls on its
backend owner and distinguish borrowed acquired textures from owned resources.

First inspect existing NativeBridge/nativeHandle/platform-integrator accessors
and resource wrappers. If a missing texture-view adoption operation requires a
backend change, make that narrow operation explicit and test its ownership;
do not access private backend internals through platform-specific reflection.

### Desktop

- X11/Windows/macOS retain their native GPU backends. Metal viewport changes
  remove the current macOS overlay-only layout and constrain the particle layer
  to the common viewport.
- Linux selection remains: WAYLAND_DISPLAY or WAYLAND_SOCKET selects Wayland;
  otherwise DISPLAY selects X11; no endpoint produces an early diagnostic.
  Optional --platform=wayland/x11 overrides remain. Wayland failure does not
  silently fall back to X11.
- Native Wayland: the existing C/FFM host owns one xdg toplevel. Compose scene
  rasterizes the common content into parent UI wl_shm buffers; Dawn Vulkan owns
  a separate particle subsurface. No surface is presented by two renderers.
- Buffer release governs UI memory reuse. Busy buffers are never overwritten,
  freed or accumulated without a bound across resize generations. Pointer and
  keyboard events reach Compose in coherent ordering and coordinate units.
- Preserve private event queues and correct prepare-read/read/cancel pairing;
  do not steal Vulkan queue events. Never depend on XWayland to render controls.

### Android

Compose renders the common UI around a native viewport hosted through the
supported Android view interop. Dawn uses Vulkan with an ANativeWindow obtained
and retained from the real Android surface by the platform adapter.

Surface creation/change/destruction callbacks define usable extent and lifetime.
Hold the native window only while valid and release it exactly once after its
Dawn surface no longer needs it. Android JNI/native packaging is qualified for
the emulator ABI; desktop FFM/library-loader assumptions must not leak here.

### iOS

Compose Multiplatform hosts the common UI and a native UIView viewport whose
CAMetalLayer is used by Dawn Metal. UIKit/layer geometry operations run on the
main thread; GPU work stays off that thread. The layer is retained until Dawn's
surface is released and configured in physical drawable pixels using actual
content scale and viewport size.

Compose and Dawn never share the same backing/rendering layer. Native interop
must keep controls above/outside the particle bounds and pass input correctly.

## Lifecycle, ownership and errors

### Common session policy

Represent initializing, running, stopped/stopping and failed session states
explicitly. UI-requested pause is distinct from lifecycle suspension. Reset
generation and count requests remain immutable snapshots consumed by the GPU
owner. Stop/Restart are serialized; repeated taps cannot create multiple owners.

Each run has a session identity. Late readiness/error/frame callbacks from a
stopped run are ignored. Within a run, first terminal failure wins even if GPU
initialization subsequently returns. A session is not ready merely because its
window/view exists.

### Rotation and mobile background

- Rotation changes layout/native surface extent without issuing Reset or
  changing selected count, reset generation or user pause.
- If Android recreates its Activity/view, retain the active session/controller
  across configuration change, release the old surface safely, and attach the new
  surface without resetting particle data. GPU device/session retention is scoped
  independently from that disposable presentation surface.
- Background/loss of usable presentation suspends frame acquisition and particle
  time advancement. Preserve the active GPU simulation when the OS allows it;
  on return resume presentation without a large elapsed-time simulation jump.
- Lifecycle suspension must not toggle the user's paused setting. A user-paused
  scene remains paused after return. Mobile process death is a fresh launch, not
  a promised persistent simulation restore.
- Device loss/resource invalidation that cannot preserve the simulation is a
  visible session error, not a silent reset presented as successful rotation.
- Zero-sized/not-yet-configured viewports suspend rendering; never allocate or
  configure negative/overflowing extents. Convert logical bounds to physical
  sizes once, at each native adapter boundary.

### Termination and cleanup

Cancel and await the render owner; release acquired frames/views and particle
scene, then Dawn presentation surface and device/session resources before
destroying their underlying native window/layer.
Never await blocking GPU work on the UI dispatcher while it must process input.
Desktop Close/native close and mobile Stop keep bounded cleanup tests; these
controlled checkpoints do not prove recovery from an indefinitely hung driver.

On desktop compositor loss, preserve the first cause, release owned resources
and exit with failure. User Close exits successfully. Mobile render failure
leaves the common error UI mounted with lifetime/restart action available.

## Qualification gates and acceptance evidence

### Gate A: native rendering prerequisites

Before committing to a production adapter, qualify actual Compose-scene raster
and input on Linux ARM64, actual Dawn Vulkan presentation on an Android Emulator
and actual Dawn Metal presentation on an iOS Simulator. Record emulator ABI,
OS/API/runtime versions, native library hashes and selected adapter/backend.

Use available supported simulator profiles and pinned native archive variants.
If the emulator lacks Vulkan or the simulator cannot supply required Metal/Dawn
functionality, preserve the concrete failure and report the missing prerequisite.
Neither compilation alone nor a Compose-only particle animation satisfies this
gate. Software-backed Vulkan is acceptable when it actually runs Dawn; no
hardware-GPU performance claim is implied.

### Gate B: shared behavior and layout

Exercise the actual common composable and rendered viewport:

- Startup/error: GPU controls disabled, lifetime action available.
- Pointer/touch Pause/Resume, each available count, Reset and Close/Stop/Restart.
- Wide/compact transitions, portrait/landscape, safe areas and visible hit targets.
- Real presented particle frames, dimensions and state changes; logs saying
  configure succeeded are not proof of presentation.
- Focused keyboard activation versus negative unfocused global shortcut tests.
- Same logical layout at equal constraints, same labels/options/states except the
  explicitly chosen desktop/mobile lifetime action and touch-target adaptation.

### Gate C: native lifecycle and regressions

- Desktop resize and Wayland output integer scale 1→2→1, correct hit testing,
  native close, startup/presentation Close and isolated compositor disconnect.
- Mobile rotation, background/return both running and user-paused, surface
  destroy/recreate, Stop during setup/presentation, rapid repeated Stop/Restart,
  error retention and absence of stale callbacks from old sessions.
- Common unit/Compose tests plus actual platform-host acceptance. Existing
  disabled iOS backend test binaries do not constitute simulator execution;
  add an actual app-driven simulator test/harness rather than count gated tests.
- Mac and Linux live acceptance; native Windows validation only when executed
  on a Windows runner. Mark unexecuted Windows/x64/device cases explicitly.
- First mobile milestone requires both an installed Android app executed in an
  emulator and an installed iOS app executed in a simulator, with native Dawn
  frames and functional common controls. Physical phones are outside this gate.

### Evidence and operational preservation

Preserve viewer 6080, qualify Wayland in the disposable 6084 desktop and update
visible 6081 only after successful integration. Destructive compositor-loss
tests use isolated 6082, never a visible desktop. Full forced source-mounted
Dawn builds run sequentially because `.dawn/extract` is shared.

Reports distinguish source inspection, compilation, live native execution and
unexecuted platforms. Store logs/screenshots and qualification reports locally;
document reproducible launches, emulator/simulator prerequisites, packaged
resources and maintenance risk of the pinned internal Compose scene API.

## Out of scope and stop rule

App Store/Play publication, physical-device certification, hardware benchmarks,
tvOS, new simulation features, persisted process-death recovery, fractional
Wayland scaling, upstream forks and a separate shared demo module are excluded.

No production adapter replaces a working host before qualification. If either
mobile target or the native Wayland scene cannot meet its gate, report the exact
blocker and partial results; do not label reduced controls, non-Dawn rendering or
build-only evidence as completed cross-platform support. Broad dependency/API
changes needed beyond the stated boundaries return to design review.

## Source and status references

- Local: dawn4k-demo/build.gradle.kts, ParticleControls.kt,
  ParticleDemoRunner.kt, DawnSurface.kt, ParticleControlPanel.kt,
  WaylandSurfaceHost.kt, gradle/libs.versions.toml and backend KMP conventions.
- Local probe: dawn4k-demo/build/ui-qualification/result.json and
  preferred-host-failure.log (build artifacts, not committed platform evidence).
- Android KMP packaging:
  https://developer.android.com/kotlin/multiplatform/plugin
- Pinned scene API source:
  https://github.com/JetBrains/compose-multiplatform-core/blob/v1.11.1/compose/ui/ui/src/skikoMain/kotlin/androidx/compose/ui/scene/CanvasLayersComposeScene.skiko.kt
- Pinned JAWT initialization:
  https://github.com/JetBrains/skiko/blob/v0.144.6/skiko/src/awtMain/kotlin/org/jetbrains/skiko/HardwareLayer.kt

At this specification stage no KMP demo conversion, mobile app scaffolding,
simulator installation or native-scene integration has been performed. Earlier
desktop qualification code remains committed; the merged implementation plan
follows explicit user review of this written specification.
