# Demo UI Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the same clickable Compose controls and content layout on every desktop backend, removing the Wayland-only global shortcuts without reintroducing XWayland.

**Architecture:** Qualify the pinned Compose/Skiko stack on a verified ARM64 JetBrains Runtime first. If successful, use one common Compose content layout and a separately owned native Wayland child surface for Dawn; UI and Vulkan never present to the same surface. If runtime/child-surface qualification fails, stop this primary-path plan and write the approved Compose-scene/native-host alternative plan from the actual failure evidence, without changing the working demo or weakening parity.

**Tech Stack:** Kotlin 2.4.20, Compose 1.11.1, Skiko 0.144.6, JDK 25, JBR 25.0.4.1-b635.70 qualification candidate, C/libwayland, Java FFM, Dawn Vulkan/Metal/D3D12, Sway/noVNC ARM64.

**Spec:** `docs/superpowers/specs/2026-10-08-demo-ui-parity-design.md`

## Global Constraints

- One application window containing the same Compose controls and particle view.
- One shared layout: control column on the left and particle viewport on the right.
- Linux Wayland must still operate without X11 or XWayland; the existing X11, macOS and Windows backends remain.
- Space, R, +, minus and Escape no longer act as global application commands.
- Standard focused-button activation/focus traversal remains available through Compose.
- All controls except Close are disabled until GPU initialization succeeds; the first terminal error is preserved.
- No silent X11 fallback, separately implemented imitation of the panel, or unapproved upstream fork.
- Do not replace the default runtime or working host before native qualification passes.
- Preserve viewers 6080 and 6081; qualification uses project `dawn4k-ui-qualification`, port 6084. Destructive compositor tests use their isolated 6082 project.
- Run full forced builds serially across source-mounted desktops: `.dawn/extract` is shared.
- Native Windows and x64 runtime evidence must not be claimed unless executed there.

## Review Focus

1. WLToolkit exists but Skiko selects an X11 renderer: qualify actual button painting/input with DISPLAY absent before shipping (Task 1).
2. A click after resize or scale change reaches the wrong target: compare logical control bounds and route child input only to the particle viewport (Tasks 2, 3 and 5).
3. Compose and Dawn attach buffers to the same surface or steal connection events: use an owned child surface and private queue; preserve correct read/cancel pairing (Task 3).
4. Close/disconnect during setup loses the terminal error or frees parent before child/GPU: deterministic checkpoints and explicit parent lifetime (Tasks 3–5).
5. Removing shortcuts disables normal focused-button activation or leaves a hidden global Escape handler: negative global-action tests plus standard focus activation tests (Tasks 4 and 5).

## File Map and Dependency Order

- `dawn4k-demo/scripts/qualification_runtime.py`: checksum verification and isolated runtime staging; never changes JAVA_HOME globally.
- `dawn4k-demo/scripts/test_qualification_runtime.py`: deterministic checksum/archive validation tests.
- `dawn4k-demo/qualification/jbr-arm64.json`: candidate URL, version and SHA-512 lock.
- `dawn4k-demo/src/test/kotlin/org/graphiks/dawn4k/demo/ComposeWaylandProbe.kt`: runs the actual panel under the candidate JVM and records state/bounds/native toolkit.
- `scripts/test-compose-wayland-probe.py`: launches probe on disposable compositor, pointer checks and evidence; no production entry-point changes.
- `dawn4k-demo/src/main/kotlin/org/graphiks/dawn4k/demo/ParticleDemoContent.kt`: common composable layout and viewport callback.
- `ParticleControlPanel.kt`: stable semantics/test tags and state diagnostics, no backend branching.
- `DemoApp.kt`, `Main.kt`, `MetalLayerHost.kt`: common content and bounded native viewport placement.
- `WaylandAwtParent.kt`: pinned-runtime handle/lifetime adapter, invoked on EDT with AWT lock.
- `src/main/c/wayland-child.h`, `wayland-child.c`: borrowed parent/display, owned subsurface and private host queue.
- `WaylandChildNative.kt`, `WaylandComposeSurfaceHost.kt`: FFM ABI and owner/lifecycle for child viewport.
- `WaylandComposeDemo.kt`: same Compose application content, native toolkit and Vulkan host.
- Existing C host/native wrappers: remove only obsolete shortcut dispatch after replacement integration succeeds; retain or delete unused code only after references/tests are audited.
- `build.gradle.kts`, Docker runtime/launcher files, README and live test scripts: package the qualified runtime selection and graphical acceptance evidence.

Each task carries a RED/GREEN test gate and a scoped commit. Tasks 2–6 cannot
start unless Task 1 qualifies native Compose and the pinned parent's surface API.
Task 3 additionally qualifies real same-window Dawn presentation before Task 4
switches the production entry point.

---

### Task 1: Qualify Native Compose and the Runtime Handle Contract

**Files:** Create the runtime lock/staging tests and probe files from the file map; modify `dawn4k-demo/build.gradle.kts` only to add opt-in probe tasks.

**Interfaces:** Python `verify_archive(path: Path, expected_sha512: str) -> None`; `stage_runtime(archive: Path, destination: Path) -> Path` returns a verified runtime home. Kotlin `fun main()` in `ComposeWaylandProbe.kt`, launched only through `runComposeWaylandProbe`. Probe writes JSON snapshots to `DAWN_UI_PROBE_STATE`: toolkit, count, paused, resetGeneration, ready, error and named control bounds. It is test code, not an alternative shipped UI.

Pointer harness helpers are defined in `scripts/test-compose-wayland-probe.py`:
`read_state(path: Path) -> dict` reads an atomic JSON snapshot;
`click_control(name: str) -> None` looks up its latest bounds, adds the Sway
toplevel's content origin, applies the output scale and sends a pointer press/
release through noVNC; `send_keys_to_particle_viewport(keys: list[str]) -> None`
focuses a point strictly inside the current viewport and uses wtype only for
negative shortcut tests. All transitions wait for the expected state predicate
with a 5s deadline; elapsed time alone is never evidence of a successful click.

- [ ] **1. Add failing checksum and archive tests.** Use tiny local fixtures, not a network mock. Reject a digest mismatch before extraction and reject absolute/parent-traversal archive members. Do not overwrite an existing runtime directory on failure.

```python
def test_corrupt_archive_never_replaces_runtime(tmp_path):
    archive = tmp_path / 'runtime.tar.gz'
    archive.write_bytes(b'corrupt')
    destination = tmp_path / 'jbr'
    destination.mkdir()
    marker = destination / 'keep'
    marker.write_text('existing')
    with pytest.raises(ValueError, match='checksum'):
        verify_archive(archive, '0' * 128)
    assert marker.read_text() == 'existing'
```

Use unittest equivalents if pytest is unavailable; the deliverable command uses
`python3 -m unittest discover -s dawn4k-demo/scripts -p 'test_qualification_runtime.py'`.
Run RED against the missing functions, then implement streaming SHA-512, safe
tar extraction into a private staging directory and an atomic promotion only
after verifying `bin/java` and `release`. Record GREEN.

- [ ] **2. Pin and stage the candidate after the test gate.** Lock the following upstream data; never download a moving `latest` alias:

```json
{
  "version": "25.0.4.1-b635.70",
  "architecture": "aarch64",
  "url": "https://cache-redirector.jetbrains.com/intellij-jbr/jbr-25.0.4.1-linux-aarch64-b635.70.tar.gz",
  "sha512": "7741f93b5b51cf1a55c9f96df6fa7d77e73d9b5158e5799afd35261f03f844043cb740a5efeb3fb4221ed6d64b70da8e4e5020e81435c15258d9ce62cbefaf07"
}
```

Stage only under the qualification build volume, not `/opt/java/openjdk` or the
running desktop runtime. Keep Temurin for Gradle compilation. Archive checksum
was read from the upstream release during planning; runtime has not been run.

- [ ] **3. Add the real Compose probe and launch task.** Test-source compilation
can access the internal panel as the project's existing friend test compilation.
Use a real Window and the current composable rather than a recreated control set:

```kotlin
fun main() {
    check(System.getenv("DISPLAY").isNullOrBlank())
    check(java.awt.Toolkit.getDefaultToolkit().javaClass.name == "sun.awt.wl.WLToolkit")
    val controls = ParticleControls()
    androidx.compose.ui.window.application {
        androidx.compose.ui.window.Window(onCloseRequest = ::exitApplication,
            title = "dawn4k Compose Wayland qualification") {
            ParticleControlPanel(controls, ::exitApplication)
            androidx.compose.runtime.LaunchedEffect(Unit) { controls.initialize(65536) }
        }
    }
}
```

Add state collection and JSON snapshots around this content. Add a test-only
bounds collector to ControlButton if needed, defaulting to no observation in
production; do not approximate pixel hit coordinates from labels. Store JSON
atomically and include the count button's selected/disabled state.
Use test tags `control:Pause`, `control:Resume`, `control:Reset`, `control:Close`
and `control:<count>` on the actual common buttons. For standalone probe bounds,
extend the existing panel compatibly with
`onControlBounds: ((String, Rectangle) -> Unit)? = null`, pass that observer to
each actual ControlButton and emit `boundsInWindow()` from onGloballyPositioned
only when the observer is present. The callback runs on the UI dispatcher;
existing two-argument call sites retain the default. Tests identify buttons
through semantics; the standalone probe records these actual layout callbacks,
not a copy of the panel or an assumed UI-test inspection API.

```kotlin
tasks.register<JavaExec>("runComposeWaylandProbe") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("org.graphiks.dawn4k.demo.ComposeWaylandProbeKt")
    executable = providers.gradleProperty("qualification.java").get()
    jvmArgs("--enable-native-access=ALL-UNNAMED", "-Dawt.toolkit.name=WLToolkit",
        "-Dskiko.renderApi=SOFTWARE_COMPAT",
        "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.awt.wl=ALL-UNNAMED")
}
```

Resolve the property lazily inside task configuration so normal builds do not
require it. Confirm the actual pinned artifacts recognize SOFTWARE_COMPAT via
their own classes/source; a property silently ignored is not qualification.
The UI may raster through Java2D/BufferedImage while Dawn remains Vulkan.

- [ ] **4. Write and execute native probe assertions.** Launch on port 6084 with
the existing Wayland Compose override, `-p dawn4k-ui-qualification`, and an
environment loaded from that desktop's JSON with DISPLAY/XAUTHORITY removed.
RED must establish no qualified result before the probe runs; then assert
WLToolkit, Sway `shell=xdg_shell`, null X11 window id and no XWayland process.
Click actual control-bound centers through noVNC/pointer automation and assert:

```python
assert state['toolkit'] == 'sun.awt.wl.WLToolkit'
assert state['ready'] is True
# after a pointer click on Pause:
assert state['paused'] is True
# after a pointer click on 256 and Reset:
assert state['count'] == 256
assert state['resetGeneration'] == 1
```

Repeat at integer output scale 2, then restore 1 in finally. Validate Close
through the button with an exit deadline of 10 seconds. Give builds 600 seconds,
application startup 30 seconds and per-input state transitions 5 seconds.
Use an isolated process with owned cleanup; never kill a visible compositor.

- [ ] **5. Inspect and verify the pinned native-parent API on the actual runtime.**
Probe reflective access with the necessary module opens, not a guessed window
handle. Inspect `sun.awt.AWTAccessor`, the component peer's `getSurface()`,
`getWlSurfacePtr()`, and `sun.awt.wl.WLDisplay.getInstance().getDisplayPtr()`.
Verify access on EDT under `sun.awt.SunToolkit.awtLock()/awtUnlock()` and record
class/method signatures, nonzero addresses and lifetime on hide/dispose.
These names are candidates from source inspection; absent/changed signatures
are a qualification failure, not a reason to reinterpret another pointer.
Output `build/ui-qualification/parent-contract.json` with exact runtime hash and
verified method names. Never hold the AWT lock during blocking GPU work.

- [ ] **6. Gate and commit.** Write `build/ui-qualification/result.json` with
`composeNative`, `pointerControls`, `scale2` and `parentApi` booleans, plus logs
and screenshots. All four must be true to proceed. On failure, preserve evidence,
stop production integration and prepare the alternative scene-hosting plan for
review. Commit only probe/test/lock/task files; do not stage build evidence.

### Task 2: One Common Content Layout, Including a Bounded Metal Viewport

**Files:** Create `ParticleDemoContent.kt` and `ParticleDemoContentTest.kt`; modify
`ParticleControlPanel.kt`, `DemoApp.kt`, `Main.kt`, `MetalLayerHost.kt`, and its tests.

**Interfaces:** `@Composable fun ParticleDemoContent(controls: ParticleControls,
onClose: () -> Unit, onViewportBounds: (Rectangle) -> Unit)`; existing
`ParticleControlPanel(controls,onClose,onControlBounds = null)` remains the single controls source.
`MetalLayerHost.attach(nsViewPtr: Long, viewport: AtomicReference<Rectangle>)`
mirrors the existing X11/Windows viewport ownership and converts Compose top-left
coordinates to AppKit bounds with an explicitly tested origin rule.

- [ ] **1. Write failing common-content tests and pure viewport-coordinate tests.**
Add Compose UI-test dependencies at the pinned Compose version. Tag the shared
panel, viewport and buttons; assert real semantics on initializing/ready/failed
states. Literal coordinate fixture: content height 800 and viewport `(300,20,700,600)`
maps to AppKit `(300,180,700,600)` for a non-flipped view, unchanged top-left for a
flipped view. Zero viewport means no usable GPU size; scale 2 gives 1400x1200.

```kotlin
controls.initialize(65536)
rule.setContent { ParticleDemoContent(controls, {}, {}) }
rule.onNodeWithText("Pause").performClick()
assertTrue(controls.state.value.paused)
rule.onNodeWithText("256").performClick()
assertEquals(256, controls.state.value.count)
rule.onNodeWithText("Reset").performClick()
assertEquals(1L, controls.state.value.resetGeneration)
```

Create the rule using the supported UI-test API in the resolved pinned artifact;
run actual composition, not a string/source assertion. Close must be clickable
before initialize and after fail. Count controls must be disabled when not ready.

- [ ] **2. Run RED**, then extract the existing Row into the shared composable:

```kotlin
Row(Modifier.fillMaxSize().background(Color(0xFF101820))) {
    Box(Modifier.width(300.dp).fillMaxHeight()) { ParticleControlPanel(controls, onClose) }
    Box(Modifier.weight(1f).fillMaxHeight().onGloballyPositioned {
        val b = it.boundsInWindow()
        onViewportBounds(Rectangle(b.left.roundToInt(), b.top.roundToInt(),
            b.width.roundToInt(), b.height.roundToInt()))
    })
}
```

Remove only the macOS overlay-specific content branch, keeping native window
decoration differences where necessary. Position the owned CAMetalLayer within
the shared viewport and prevent it from covering the controls. Update attach
call sites atomically. Keep renderer and native release order unchanged.

- [ ] **3. Run GREEN** on common semantics/pure coordinate tests, native macOS
presentation and X11 live suite. Capture actual macOS pointer interaction and
viewport resizing, not only JVM success. Record Windows native validation as
unexecuted if no Windows runner is available. Commit the common-layout change.

### Task 3: Qualify a Dawn Child Surface in the Compose Wayland Window

**Files:** Create `WaylandAwtParent.kt`, `wayland-child.h/.c`, `WaylandChildNative.kt`,
`WaylandComposeSurfaceHost.kt`, C child ABI/lifecycle tests and
`WaylandComposeSurfaceHostTest.kt`; update native build scripts/packaging.

**Interfaces:** `data class WaylandParentHandles(val display: Long,val surface: Long)`;
`fun acquireWaylandParent(window: ComposeWindow): WaylandParentHandles` runs EDT
inspection with the exact Task 1 contract. C opaque `dawn_wl_child` owns only its
own registry/compositor/subcompositor/queue/surface/subsurface, never its parent.

```c
typedef struct dawn_wl_child dawn_wl_child;
dawn_wl_child *dawn_wl_child_open(void *display, void *parent,
    char *error, uint32_t capacity);
void *dawn_wl_child_surface(dawn_wl_child *child);
int32_t dawn_wl_child_bounds(dawn_wl_child *child,
    int32_t x, int32_t y, int32_t width, int32_t height, int32_t scale);
void dawn_wl_child_close(dawn_wl_child *child);
```

Kotlin `WaylandComposeSurfaceHost.attach(window: ComposeWindow,
viewport: AtomicReference<Rectangle>): WaylandComposeSurfaceHost` implements
SurfaceHost; createSurface delegates to DawnSurface.createWayland with borrowed
display and owned child surface. Zero viewport is suspended presentation.

- [ ] **1. Write failing native tests:** null parent/display rejected, incomplete
registry cleanup, missing wl_subcompositor, invalid/overflowing bounds, size/scale
oracle, repeated create/close, borrowed parent survives close. Verify C ABI with
a compiled oracle rather than JVM-only assumed offsets.

```kotlin
val host = WaylandComposeSurfaceHost.attach(window, viewport)
try {
    viewport.set(Rectangle(300, 0, 700, 600))
    assertEquals(700 to 600, host.pixelSize())
    viewport.set(Rectangle())
    assertEquals(0 to 0, host.pixelSize())
} finally { host.close(); host.close() }
```

Run RED under the candidate runtime in the qualification desktop.

- [ ] **2. Implement the owned child.** Create a display proxy wrapper assigned
to a private queue before obtaining registry; bind compositor/subcompositor on
that queue and create `wl_surface` + `wl_subsurface` against the borrowed parent.
Use `wl_subsurface_set_position` in logical units and buffer scale in physical
dimensions. Select desynchronized child updates and arrange parent commits with
the runtime's existing paint lifecycle, rather than independently attaching any
buffer to its parent. Check protocol ordering by WAYLAND_DEBUG in qualification.
Do not dispatch runtime or Vulkan queues, disconnect the display, acknowledge
the parent's xdg configure, or free runtime-owned objects. On errors destroy
only objects actually acquired in reverse ownership order.

- [ ] **3. Implement the runtime lifetime lease.** Intercept closing/disposal
at the application boundary: cancel and await runner cleanup before disposing
Compose parent. Observe loss/hide and reject new child operations when parent
is invalid. Use a single serialized owner for bridge events/bounds, with no EDT
lock held while awaiting a roundtrip or GPU presentation. If this cannot be
made safe with the pinned upstream runtime, fail the hosting qualification.

- [ ] **4. Run GREEN with a real shared-window renderer.** Use
`runParticleDemo(host,controls,true)` inside the probe's lifecycle. Click Pause,
256, Reset and Resume while Vulkan frames continue. Inspect Sway: exactly one
application toplevel, UI and particles visible together, no X11 window id.
At scale 2 and after resize, click all button-bound centers and verify that the
child neither obscures controls nor takes their pointer events. Compare pure
bounds fixture and actual configured physical dimensions.

- [ ] **5. Exercise actual close at GPU startup/presentation checkpoints.**
Reuse the existing real NativeBridge checkpoint fixture, adapted to the shared
window: Close through the common button, native compositor close, parent hide/
restore and disposable compositor disconnection. Assert surfaces released
before parent destruction, no surviving owner and first terminal cause intact.
Run ASan/UBSan C tests and repeated owner cycles. Commit only when all native
same-window qualification checks pass.

### Task 4: Switch the Wayland Entry Point and Remove Global Shortcuts

**Files:** Create `WaylandComposeDemo.kt`; modify `Main.kt`, Wayland demo/host C
and Kotlin dispatch, `ParticleControls.kt` only if stepCount becomes unreferenced,
launcher/Docker/runtime files and existing entry-point tests.

**Interfaces:** `fun runWaylandComposeDemo(): Unit` owns Compose window, controls,
child host and runner; optional runtime selection applies only to the app process.
Native toolkit is selected before Toolkit/Compose initialization. Existing Linux
selection semantics and `runParticleDemo` signature remain unchanged.

- [ ] **1. Write failing distributed-application tests** for visible common
buttons and absence of global shortcuts. With the pointer focused in the
particle viewport, send Space, R, +, minus and Escape separately; after each,
assert unchanged count/paused/reset state and a still-open application. Then
focus Pause using normal UI focus navigation and verify standard activation.
Missing/stale Wayland endpoints still fail without an X11 fallback.

```python
before = read_state()
send_keys_to_particle_viewport(['space', 'r', 'plus', 'minus', 'Escape'])
assert read_state() == before
assert application.poll() is None
click_control('Pause')
assert read_state()['paused'] is True
click_control('Close')
assert application.wait(timeout=10) == 0
```

Helpers consume recorded semantic bounds/state, not hardcoded screen positions.
In these tests bind `read_state()` to the Task 1 path-specific helper through
`functools.partial(read_state, state_path)` and reuse the same pointer harness.

- [ ] **2. Run RED, then integrate** Main's selected Wayland backend into the
common Compose application lifecycle, configure WLToolkit before any UI access,
and select the qualified software-compatible UI renderer without changing Dawn
Vulkan. Require the pinned candidate/runtime contract at startup and produce an
explicit diagnostic on incompatible JVMs; no automatic XToolkit fallback.
Package the runtime/artifact lock through the Docker application launch path;
keep Gradle's build JVM and X11 launch runtime independent.

- [ ] **3. Remove shortcut actions and hints.** Delete translations for
DAWN_WL_PAUSE/RESET/COUNT_UP/COUNT_DOWN and Escape-as-close from the now-replaced
application path. Audit references before deleting obsolete C xdg host/FFM
wrappers and key-action tests. Keep tests for protocol lifetime/scale that still
apply, adapting them to the child host. Do not remove Compose keyboard focus or
normal button activation. Replace title hint text with the common title.

- [ ] **4. Run GREEN** on real distributed launch, pointer controls, negative
shortcut cases, Close during initialization, invalid runtime, absent/stale socket
and error retention. Verify installDist uses only the declared packaged bridge
resource, not a build-tree library. Commit the application switch.

### Task 5: Cross-Environment Graphical and Lifecycle Acceptance

**Files:** Update Wayland window/entrypoint/lifecycle tests and scripts; create
`scripts/test-demo-ui-parity.py`; update existing opt-in test coverage.

**Interfaces:** A normalized evidence snapshot contains backend, toolkit,
logical control/viewport bounds, labels, selected/enabled states and actual
simulation state. Snapshots are test diagnostics, not a new user-facing control API.

- [ ] **1. Add real parity tests.** Render common content at the same logical
size and compare labels, buttons, layout proportions and state transitions for
Wayland and X11. Run the same composed-control assertions on macOS and available
Windows runners. Native borders/font pixels are explicitly excluded from exact
comparison; content bounds and control state are not.

```python
assert wayland['labels'] == x11['labels']
assert wayland['controlLogicalBounds'] == x11['controlLogicalBounds']
assert wayland['selectedCount'] == x11['selectedCount'] == 256
assert wayland['paused'] is x11['paused'] is True
```

- [ ] **2. Pin scale/resize and lifecycle regressions.** Repeated resize waits
for an actually presented particle frame at each size, not only configure.
Output scale 1→2→1 keeps logical click targets aligned. Pointer input over the
panel never goes to the particle surface. Error panel retains its first error
through late GPU initialization. During initialization/error, only Close is
enabled. Setup/presentation Close and disposable compositor loss use explicit
real-operation checkpoints, not launch-then-cancel races.

- [ ] **3. Run suites serially and read XML evidence.** Mac:
`./gradlew :dawn4k-demo:test :dawn4k:jvmTest :dawn4k-native:jvmTest :dawn4k-native:verifyDawnAbi --rerun-tasks`.
Wayland and rebuilt X11: use each desktop's `/opt/demo/demo.sh` with the same
task list and its independent live opt-in, 600s build deadline. Run the actual
distributed pointer acceptance, opt-out→opt-in checks and isolated pending-loss
test, not only model tests. Keep 6080 running throughout.

- [ ] **4. Inspect visually in browser and on Mac.** Capture Pause/Resume,
count selection, Reset, resize and both clients' shared desktop. Confirm new
Wayland UI works before updating the visible 6081 desktop. Record JBR/Skiko
effective versions, protocol identity, runtime/bridge hashes and all unexecuted
platform cases. Fix reproduced failures via focused RED/GREEN, then commit tests.

### Task 6: Documentation, Independent Review and Integration Handoff

**Files:** Update `dawn4k-demo/README.md`; local evidence under
`dawn4k-demo/build/ui-parity/`; progress ledger under `.superpowers/`.

**Interfaces:** No runtime interface change. Shipped launch/install commands and
the report describe the verified adapter, not speculative fallback support.

- [ ] **1. Replace obsolete documentation.** Remove the keyboard-only table,
no-panel limitation and claims that all AWT implies X11. Document common buttons,
standard focused-control access, native runtime requirements, auto-selection,
runtime mismatch errors, integer-scale limitations and two production viewers.
Document exact pinned-runtime staging and application launch commands verified
in Tasks 1/4, not an upstream "latest" link.

- [ ] **2. Run documented launches and package checks.** Verify UI+particles
under automatic Wayland selection and explicit X11 selection, inspect resources
and driver/runtime identity. Check placeholders/trailing whitespace and retain
local evidence before any workspace cleanup.

- [ ] **3. Request one whole-branch independent review** if Native execution is
selected. Include the spec, qualification result, actual parent API contract,
C ownership/queue protocol, negative shortcuts and lifecycle checkpoints.
Resolve critical/important findings with observed regressions; do not call
driver-stall recovery proven by controlled scheduling tests.

- [ ] **4. Re-run fresh acceptance and hand off.** Commit scoped documentation,
preserve worktree and visible services. Offer local integration/PR/keep choices
only after tests and review pass; do not push/merge without user authorization.
Link any issue or PR actually worked/opened/resolved to this session immediately.

## Coverage and Primary-Path Stop Rule

User-visible parity/common panel/macOS arrangement: Tasks 2, 4 and 5. Native
runtime qualification: Task 1. Single-window child ownership and coexistence:
Task 3. Removal of global shortcuts: Tasks 4–6. Error/lifecycle/scale evidence:
Tasks 3–5. Packaging and preserved X11: Tasks 1, 4–6.

The approved scene/native-host alternative is **not claimed implemented by this
primary-path plan**. If Task 1 or 3 fails, retain the working demo and deliver
the precise failing API/render/input/ownership result. Use that evidence to
write a separate concrete alternative adapter plan for review; do not continue
with assumed APIs, a custom button panel, silent XWayland or an upstream fork.

## Execution Method

Native execution is recommended: runtime handles, child lifetime and shared UI
layout are tightly coupled, and the qualification gate must be interpreted in
one context. Implementation begins only after user review of this written plan.
