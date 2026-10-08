# Native Wayland Compose Scene Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Supply identical Compose controls and layout on native Wayland, X11, Windows and macOS, without application-global shortcuts.

**Architecture:** Keep the native C/FFM xdg window and use the existing panel in CanvasLayersComposeScene. Skia rasterizes the common window content into separately released wl_shm buffers; Dawn presents exclusively to a child wl_subsurface at the viewport bounds. One serialized owner dispatches Compose and native UI events; GPU work stays on the existing runner dispatcher.

**Tech Stack:** Kotlin 2.4.20, Compose 1.11.1, Skiko 0.144.6, existing JDK 25, Java FFM, libwayland-client, libxkbcommon, Dawn/Vulkan.

**Spec:** `docs/superpowers/specs/2026-10-08-demo-ui-parity-design.md`

## Qualification Evidence and Change of Path

Primary plan Task 1 actually executed on Linux ARM64 in the isolated 6084 desktop.
The checksum-verified JBR 25.0.4.1+1-b635.70 selected `sun.awt.wl.WLToolkit` and
the pinned Skiko reported `SOFTWARE_COMPAT`. Compose Window then failed before
rendering with `IllegalStateException: Can't lock DrawingSurface`, through
`DrawingSurface.lock → HardwareLayer.init → SkiaLayer.addNotify`.
The pinned Skiko v0.144.6 HardwareLayer source unconditionally initializes the
JAWT drawing surface. This is not a button/input failure or a successful native
Compose window. The preferred adapter is therefore not qualified.

Pinned ui-desktop 1.11.1 bytecode and matching upstream v1.11.1 source confirm
`CanvasLayersComposeScene(density, layoutDirection, size, coroutineContext,
platformContext, invalidate)`, `ComposeScene.setContent`, `render(Canvas,Long)`,
`sendPointerEvent`, `sendKeyEvent`, `cancelPointerInput` and `close`. The factory
requires `InternalComposeUiApi` opt-in, not reflection. Its pinned internal API
stability is a maintenance limitation; runtime/native raster support is still
unqualified. No SkiaLayer, ComposeWindow or AWT Toolkit is needed by this path.

This plan replaces Tasks 1, 3 and 4 of the primary-path plan. The common-layout
and final acceptance/documentation tasks remain requirements, explicitly mapped
below. Approval is required before executing this replacement plan.

## Global Constraints

- One application window containing the same Compose controls and particle view.
- Shared left control column, right viewport; same colors, labels, spacing,
  count options and ready/disabled/selected/error states. Only native borders and
  font rasterization may differ.
- Actual ParticleControlPanel and ParticleControls, no imitation or shortcut UI.
- No X11, XWayland, JBR default switch, upstream fork or silent backend fallback.
- Close enabled during startup/error; other buttons disabled until initialization.
- First terminal cause wins; GPU cleanup precedes destruction of native surfaces.
- Space/R/+/-/Escape are not global commands. Preserve normal focused-button
  activation, Tab/Shift-Tab navigation and focus-loss cancellation.
- Automatic Linux selection and optional --platform overrides unchanged.
- Integer scale 1→2→1 only; one logical-to-physical conversion per boundary.
- Preserve viewers 6080/6081; qualification on 6084, destructive loss on 6082.
- Full forced builds serial; Windows native evidence only when actually run.
- If scene qualification fails, stop without shipping reduced controls.

## Review Focus

1. Compositor delays wl_buffer.release during resize: never overwrite/free busy
   buffers; bounded generation retirement and backpressure (Task 2).
2. Pointer press then leave/focus loss: no stuck pressed state or accidental
   Reset, correct cancel event and coordinate scale (Tasks 1–3).
3. Tab/Enter after input-mode/focus changes: real Compose button activation,
   no evdev global-action substitution (Tasks 1, 2 and 4).
4. GPU setup fails while UI is active: display the first error, keep Close usable,
   release only acquired resources (Tasks 3 and 4).
5. UI and Vulkan operations share a display: private queues, paired prepare/read/
   cancel and separate surfaces, no parent buffer stealing (Tasks 2–4).

## File Map and Contracts

- Create `dawn4k-demo/src/main/kotlin/org/graphiks/dawn4k/demo/ParticleComposeScene.kt`:
  owning-dispatcher scene adapter, render/input/focus/close only.
- Create adjacent `WaylandRasterFrames.kt`: Skia raster pixel copy and frame sizing.
- Create test-source `ComposeSceneWaylandProbe.kt` and `ParticleComposeSceneTest.kt`:
  real headless native Skia/Compose controls before production switch.
- Modify `dawn4k-demo/src/main/c/wayland-host.h/.c`: native parent UI buffer lifecycle,
  owned particle subsurface and raw input events; no Compose decisions in C.
- Create `src/main/c/wayland-ui-buffers.h/.c` and native tests: mmap/shm pools,
  overflow checks and release-aware slots separated from xdg/input policy.
- Modify `WaylandNative.kt`, `WaylandSurfaceHost.kt`, `WaylandHostState.kt`:
  expanded ABI, owner dispatcher and viewport size instead of whole-window size.
- Create `WaylandComposeDemo.kt`: application owner and readiness/lifetime wiring.
- Modify `Main.kt`, native build task/scripts and `build.gradle.kts`: isolated
  qualification launcher, xkb dependency and packaged ABI.
- Reuse primary plan Task 2 file map: `ParticleDemoContent.kt`, `DemoApp.kt`,
  `MetalLayerHost.kt` and tests for bounded shared layout on existing platforms.
- Modify `scripts/test-compose-wayland-probe.py`, lifecycle scripts, README and
  opt-in tests for native scene evidence rather than WLToolkit evidence.

Kotlin contracts used throughout:

```kotlin
data class UiPixelFrame(val width: Int, val height: Int, val stride: Int, val bgra: ByteArray)
class ParticleComposeScene(
    controls: ParticleControls,
    onClose: () -> Unit,
    onViewportBounds: (Rectangle) -> Unit,
    onControlBounds: ((String, Rectangle) -> Unit)? = null,
) : AutoCloseable {
    fun resize(logicalWidth: Int, logicalHeight: Int, scale: Int)
    fun render(timeNanos: Long): UiPixelFrame
    fun pointer(type: PointerEventType, logicalX: Float, logicalY: Float, pressed: Boolean)
    fun key(key: Key, type: KeyEventType, shift: Boolean = false)
    fun focus(focused: Boolean)
    override fun close()
}
```

All calls occur on the same owner dispatcher. Constructor captures that dispatcher
for effects; scene size is physical and density is integer scale. Layout callbacks
convert physical Compose bounds to logical Rectangle once. `pointer` multiplies
logical coordinates by scale once before Compose hit testing. `UiPixelFrame` is
packed premultiplied BGRA, wl_shm ARGB8888 on supported little-endian targets.
Scene owns/cleans raster resources; C copies pixels before returning from present.

## Task 1: Qualify the Actual Scene and Raster API on ARM64

**Files:** Create scene/raster adapters and tests/probe above. Modify the common
panel only for bounds diagnostics/focus semantics already required for parity.
Add `runComposeSceneWaylandProbe` to build.gradle.kts, using normal build JVM.

**Interfaces:** Produces ParticleComposeScene and UiPixelFrame as specified.
Consumes the existing ParticleControls and actual ParticleControlPanel.

- [ ] Write failing tests rendering the real panel on a Skia raster surface:
  Close bounds exist before initialize, count/Pause/Reset disabled; ready enables
  count selection and click state transitions; late initialize cannot clear fail.

```kotlin
val controls = ParticleControls()
val bounds = mutableMapOf<String, Rectangle>()
val scene = ParticleComposeScene(controls, {}, {}, { name, r -> bounds[name] = r })
scene.resize(800, 600, 1)
scene.render(0L)
controls.initialize(65536)
scene.render(16_000_000L)
val pause = bounds.getValue("Pause")
scene.pointer(PointerEventType.Press, pause.centerX.toFloat(), pause.centerY.toFloat(), true)
scene.pointer(PointerEventType.Release, pause.centerX.toFloat(), pause.centerY.toFloat(), false)
scene.render(32_000_000L)
assertTrue(controls.state.value.paused)
scene.close()
```

- [ ] Run RED: `./gradlew :dawn4k-demo:test --tests '*ParticleComposeSceneTest'`;
  missing adapter must fail before implementation. Test actual Compose effects on
  the test-owned serialized dispatcher; do not stub composition or manually
  update state to stand in for input.
- [ ] Implement scene using the pinned factory and raster Skia Surface:

```kotlin
val scene = CanvasLayersComposeScene(
    density = Density(scale.toFloat()), size = IntSize(width * scale, height * scale),
    coroutineContext = ownerDispatcher, invalidate = { dirty = true })
scene.setContent { ParticleControlPanel(controls, onClose, onControlBounds) }
// Wrap raster.canvas with androidx.compose.ui.graphics.Canvas(nativeCanvas),
// then scene.render(composeCanvas, timeNanos); readPixels into packed BGRA.
```

Use `Surface.makeRasterN32Premul` and its actual pinned readPixels signature,
checking successful allocation/copy, not SkiaLayer or HardwareLayer. Require
positive dimensions, scale 1 or 2 and checked multiplication/stride. Record any
unsupported native raster/input path as a qualification failure.
- [ ] Extend tests with real pointer count 256, Reset, Close; press→focus loss→
  release must not activate. Test Tab/Shift-Tab and focused Enter/Space through
  Compose `KeyEvent` factory and `sendKeyEvent`; unfocused R/+/-/Escape must not
  change state. Verify scene creation never initializes AWT Toolkit, in a child
  JVM with DISPLAY absent and `java.awt.headless=true`.
- [ ] Run GREEN on Mac and isolated ARM64 6084 using current JVM and actual
  skiko-linux-arm64 native library. Scale 1/2 logical button bounds remain equal;
  frames have dimensions 800x600 and 1600x1200, stride 3200 and 6400. Save a raster
  PNG for inspection. Commit qualification code only after both runs pass.

## Task 2: Native UI Buffers, Particle Subsurface and Raw Input

**Files:** C buffer unit, host/header, native build dependencies, WaylandNative
event layout and compiled ABI oracle/tests.

**Interfaces:** Native ABI adds functions below; Kotlin wrappers maintain the
same exact types. Existing `dawn_wl_surface` now returns the owned particle
child after migration; parent is used only for UI buffers/xdg role.

```c
int32_t dawn_wl_ui_present(dawn_wl_host *host, const void *bgra,
    int32_t width, int32_t height, int32_t stride);
int32_t dawn_wl_viewport(dawn_wl_host *host, int32_t x, int32_t y,
    int32_t width, int32_t height, int32_t scale);
/* present: 1 copied/queued, 0 backpressure, -1 error; viewport: 1 or -1. */
```

Extend dawn_wl_event with raw pointer event kind, logical fixed-point x/y,
button state, key symbol, modifiers and focus state using explicitly sized
integers. Preserve CONFIGURE/CLOSE/ERROR; remove action translation only at
Task 4. New ABI is rebuilt and packaged with matching Kotlin; compiled oracle
must assert every offsetof/sizeof, not only struct total size.

- [ ] Write failing C tests for size overflow/negative dimensions/stride mismatch,
  three busy slots then backpressure, release permits reuse, resize retires old
  slots without reuse, disconnect frees pools once and close is idempotent.
  Example: present three 8x4/stride32 frames without release → 1/1/1; fourth → 0;
  release slot 1 → next present 1 and other slot contents unchanged.
- [ ] Run RED with native bridge test task; implement mmap-backed wl_shm slots,
  release listeners and checked sizes. Keep at most two size generations alive;
  further resize defers allocation until releases, never grows unbounded. Native
  memory remains valid while compositor holds buffer; orderly close destroys
  owned protocol resources, disconnect ends server lifetime before local reclaim.
- [ ] Add wl_subcompositor registry binding and a desynchronized child particle
  surface. Parent presents the full common raster content; child positions at
  logical viewport and uses wl_surface buffer scale. Empty child input region
  lets the parent receive coherent full-window pointer coordinates. Require
  subcompositor and shm ARGB8888 explicitly, or fail with a useful diagnostic.
- [ ] Add failing live tests: parent and child own different nonzero surfaces,
  one xdg toplevel, actual pointer enter/motion/button/release/focus events and
  scale 1→2→1. Test native close during a pending UI frame; no release callback
  after host destruction. Correctly drain only the private native queue; preserve
  existing prepare-read/read/cancel pairing while Vulkan dispatches its queue.
- [ ] Implement wl_pointer and xkb keymap/modifier decoding. Forward raw key
  events for Tab/Enter/Space and all other keys without assigning control actions;
  modifier updates precede key events, focus loss cancels pointer input. Close
  keymap fds on every failure, unref old keymap/state when replaced. Link/pkg-config
  libxkbcommon and include its inputs in native task invalidation.
- [ ] Run GREEN, ASan/UBSan and ABI oracle on ARM64; compile x64 separately and
  label it compile-only. Commit the native adapter independently of Main switch.

## Task 3: Common Content and Real Shared-Window Qualification

**Files:** Primary Task 2 common-layout files plus scene adapter, Wayland host
viewport and qualification probe, pointer acceptance script.

**Interfaces:** `ParticleDemoContent(controls,onClose,onViewportBounds,
onControlBounds = null)` becomes the single composable. Scene uses it instead
of panel-only content. WaylandSurfaceHost.pixelSize uses the logical viewport
times scale; createSurface supplies only the particle child. C present copies
the UiPixelFrame before the Kotlin ByteArray/FFM arena is released.

- [ ] Execute primary plan Task 2 RED/GREEN tests and implementation for shared
  left-300.dp/right-fill layout, Mac CAMetalLayer bounds and ready/error semantics.
  Add its optional bounds observer to the common content interface; preserve
  two-argument panel calls. All native hosts consume the same logical viewport.
- [ ] Write failing isolated same-window acceptance: pointer-click Pause/Resume,
  256 and Reset while Dawn presents particles on the child; one xdg toplevel,
  no DISPLAY/XWayland and UI remains visible and clickable. Expected state:

```python
click_control('Pause')
assert await_state(lambda s: s['paused'])['paused']
click_control('256')
assert await_state(lambda s: s['count'] == 256)['count'] == 256
previous = read_state()['resetGeneration']
click_control('Reset')
assert await_state(lambda s: s['resetGeneration'] == previous + 1)
```

- [ ] Run RED, then wire the host owner as a coroutine dispatcher for scene
  invalidations, effects, input and raster presentation. Pump must not block
  scene indefinitely; use bounded socket waits already present, render on dirty
  state and after actual layout/scale changes. Never hold native UI operations
  while awaiting GPU setup/frame. Notify runner of viewport only after layout.
- [ ] On backpressure keep dirty=true and retry after release; copy latest state
  rather than queue unlimited stale frames. On resize/scale, scene resizes,
  logical viewport updates child position, Dawn reconfigures physical dimensions.
  Cancel input across focus loss and child destruction; test released/presented
  frames rather than sleeps as proof of completion.
- [ ] Run GREEN at scale 1→2→1, resize and after several button transitions; compare
  actual logical button bounds with X11. Inspect browser screenshot on 6084.
  Save `native-scene-result.json` with sceneRaster, pointerControls, scale2,
  sameWindowDawn and nativeProtocol; every value must be true before Task 4.
  Commit qualified scene/host integration; do not update visible 6081 yet.

## Task 4: Production Switch, Shortcut Removal and Lifetime Proof

**Files:** Main, WaylandComposeDemo, Wayland host/title/actions, old key tests,
live lifecycle tests and shipped distribution tests.

**Interfaces:** `runWaylandComposeDemo()` owns controls, scene, native host and
runner. Existing Linux selector dispatches to it before AWT. Owner closes scene
exactly once after cancel/join runner; releases Dawn before child/parent/connection.

- [ ] Write failing shipped-application pointer tests, negative global shortcuts
  and focused button activation using Task 3 semantic bounds/state snapshots.
  Each unfocused key Space/R/+/-/Escape leaves count, paused and reset generation
  unchanged and application alive; focused Pause still supports Enter/Space.
- [ ] Run RED, then change Main to the qualified adapter. Remove old pause/reset/
  count/Escape global dispatch and title hints, retaining native xdg close.
  Delete obsolete stepCount only after reference audit; do not delete protocol
  lifecycle/queue/scale tests. Use normal JDK 25, no JBR toolkit flags required.
- [ ] Write actual Close checkpoint regressions at GPU capabilities/presentation,
  button Close during startup and first-terminal-error before initialize.
  Inject the existing real NativeBridge checkpoints, not immediate launch cancel.
  GPU setup failure retains UI with only Close enabled; disconnection cannot show
  UI, exits with failure and preserves first cause. User Close exits successfully.
- [ ] Run GREEN, distributed installDist extracted bridge/resource checks,
  auto-selection plus explicit X11, stale/absent socket and no quiet fallback.
  Record cleanup deadlines and driver-stall limitation. Commit production switch.

## Task 5: Parity Acceptance, Documentation and Whole-Branch Review

**Files:** Primary Tasks 5/6 parity/lifecycle scripts, README and local evidence.

**Interfaces:** Evidence now identifies native ComposeScene, not WLToolkit.
Common labels/logical bounds/state are unchanged across all hosts.

- [ ] Execute primary Task 5 with actual Wayland scene application and X11/Mac:
  same labels/bounds, pointer actions, readiness/errors, scale/resize, no global
  commands and deterministic startup/presentation/disconnection close. Rebuild
  forced Gradle suites serially and read XML; OS-gated tests are not native proof.
- [ ] After qualification passes, update visible 6081 and inspect it and 6080;
  keep the existing 6080 session intact. Record Windows native and x64 runtime
  as unexecuted unless a real runner is used. Save logs/screenshots/hashes locally.
- [ ] Update README: common controls, automatic Linux selection, scene raster
  requirements, integer-scale scope, standard focus activation and no keyboard-
  only limitation. State pinned InternalComposeUiApi opt-in/maintenance risk;
  JBR probe remains diagnostic, not a shipped runtime requirement.
- [ ] Execute documented launches and install checks; request the single fresh-
  context whole-branch review allowed by Native mode, focused on shm lifetimes,
  event queues, scene dispatcher, key focus, error retention and shared layout.
  Resolve important findings with focused RED/GREEN and fresh acceptance.
- [ ] Commit scoped docs and offer integration options only after review/tests.
  No push/merge/PR without authorization; link any actually worked issue/change
  immediately. Preserve worktree and live services.

## Self-Review and Handoff

All spec requirements map to Tasks 1–5: actual common Compose and focus (1/3/4),
separate buffer/surface ownership (2/3), Mac shared layout (3), selection/global
shortcut removal (4), error/lifetime/scale and native parity evidence (2–5).
Five Review Focus classes each have explicit tests. Contracts use one logical
viewport and physical pixel frames with no JBR borrowed handles. Qualification
gates forbid switching Main until native scene+Dawn coexistence is demonstrated.

Preserve Native execution. This replacement plan awaits user review before any
implementation beyond the primary-path qualification work already performed.
