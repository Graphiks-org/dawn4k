# Adaptive Desktop and Mobile Dawn Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Preserve Native execution; one fresh-context whole-branch review at the end, not agents per task.

**Goal:** Convert dawn4k-demo to one KMP demo with shared adaptive Compose controls and real Dawn particle presentation on desktop, Android Emulator and iOS Simulator, removing global application shortcuts.

**Architecture:** The existing module owns common model/UI/particle-session orchestration and platform adapters in commonMain/jvmMain/androidMain/iosMain. Packaging-only Android and Xcode launchers consume it. Native Wayland uses a Compose raster scene on the parent and Dawn Vulkan on an owned subsurface; mobile uses Android SurfaceView/ANativeWindow and iOS UIView/CAMetalLayer without sharing their buffers with Compose.

**Tech Stack:** Kotlin 2.4.20, Compose 1.11.1, Skiko 0.144.6, AGP 9.0.0, JDK 25 desktop, supported Android bytecode target, locked Dawn v8077.0.0, existing Graphiks WebGPU/suite-demos KMP snapshots, Java FFM/libwayland/libxkbcommon, Android JNI/NDK and Xcode/Metal.

**Spec:** `docs/superpowers/specs/2026-10-08-demo-desktop-mobile-design.md`

## Global Constraints

- Convert dawn4k-demo itself to KMP; no dawn4k-demo-shared module.
- One common Compose controls/content implementation and one ParticleScene simulation.
- Packaging-only Android APK module and Xcode app shell are authorized; no demo logic duplicated there.
- First mobile milestone requires actual installed/executed Android Emulator and iOS Simulator apps, Dawn particle frames and functioning touch controls; compilation alone is not acceptance.
- Wide: usable width >=720.dp and height >=360.dp, left panel 300.dp. Otherwise compact: viewport above controls; controls <=half usable height with scrolling/wrapped counts. Very short content scrolls rather than overlaps.
- Mobile touch targets >=48.dp, safe areas/system insets respected. Layout uses usable logical dp, not physical buffer pixels or platform name.
- Same counts/ready/disabled/selected/error semantics; Close on desktop, Arrêter/Relancer on mobile. Restart is a fresh session, not preserved simulation resume.
- Rotation/background preserve count/reset generation/user pause and live particle data where the device survives. Background never adds a wall-time simulation jump. Process death is a fresh launch.
- Stop releases GPU/session resources without killing the mobile app. Repeated Stop/Restart is serialized; old-session callbacks cannot modify a new run.
- First terminal error wins within a session; only explicit new-session Restart clears it.
- No global Space/R/+/-/Escape actions; focused-button activation and Tab/Shift-Tab remain.
- Native Wayland has no DISPLAY/X11/XWayland, no silent fallback; existing auto-selection and optional --platform overrides stay.
- Retain existing desktop backends/distributions and restrict Metal to the actual common viewport.
- No Java reflection/AWT/FFM in common or Android code. Native pointer access is documented and typed at integration boundaries.
- GPU owners release acquired frame/view/scene and Dawn surface/device resources before underlying native target destruction; UI thread never waits indefinitely for GPU work.
- Preserve viewer 6080; qualification 6084; update 6081 only after success; destructive compositor loss only in 6082 project.
- Full forced source-mounted Dawn builds run serially because .dawn/extract is shared.
- No device certification, store publication, benchmark, tvOS, fractional Wayland scaling, upstream fork or broad dependency/backend rewrite.
- Stop at a concrete failed qualification; do not ship a different particle renderer, reduced UI or build-only evidence as success.

## Review Focus

1. Android replaces the surface during rotation while a frame is pending: prevent old-target presentation and release native refs exactly once, retain particle buffers (Tasks 3, 6 and 8).
2. Stop/Restart is tapped repeatedly while old initialization returns: one current session, first-error retention and rejected stale callbacks (Tasks 3 and 8).
3. UI shrinks across the adaptive breakpoint or into a system inset: all controls remain reachable, hit targets and physical viewport agree (Tasks 4, 6–9).
4. Wayland compositor delays buffer release during rapid resize: no busy-buffer overwrite/free, bounded retained generations, no Vulkan queue stealing (Task 7).
5. Pause/background/focus changes interact with input and clocks: no time jump, no accidental reset or stuck press, focused activation but no global shortcuts (Tasks 3, 5 and 8–9).

## Plan Gate and Read-Only Findings

The old preferred JBR/ComposeWindow gate failed at JAWT DrawingSurface even with
WLToolkit and effective SOFTWARE_COMPAT. Do not repeat that architecture or
change the desktop default JVM. Its committed diagnostic probe stays optional.

Planning checked only availability, not runtime support:

- suite-demos root metadata `0.1.0-20261006.180513-18` publishes Android,
  iosArm64 and iosSimulatorArm64 variants (platform metadata builds -8).
  Use the root KMP coordinate, not suite-demos-jvm in commonMain. Compatibility
  must be proven by resolution/compilation/live presentation.
- Xcode 27.0 (27A266a) and available iOS runtimes/devices exist; none was booted.
- SDK at `$HOME/Library/Android/sdk` has emulator/adb, images API29–36 and
  NDKs including 28.2.13676358. Emulator is not on PATH; use its SDK path.
  Existing AVDs belong to the user; do not wipe, reconfigure or kill them.
- Cached ParticleScene source exposes `particleBuffer: GPUBuffer` with CopySrc
  usage; real fingerprint readback requires no upstream ParticleScene change.
- Backend conventions already use com.android.kotlin.multiplatform.library;
  Android test configurations require opt-in. Existing backend iOS test binaries
  are disabled; their task success cannot prove simulator execution.
- DawnDevice exposes nativeHandle but its resource-owning texture-view constructor
  is internal. NativeBridge dispatches native calls. The demo's reflection must
  be replaced with one narrow, tested platform-integrator operation (Task 2).

Gate A is complete only after Tasks 5, 6 and 7 demonstrate actual scene raster,
Android Vulkan and iOS Metal/native Wayland presentation. Task 4 migration does
not itself prove these hosts work. Production Wayland dispatch changes in Task 9
only after its qualification. If a prerequisite fails, preserve the working
desktop and report the exact partial result before broadening scope.

## File Map

Paths below are exact task-owned paths. Kotlin package stays
`org.graphiks.dawn4k.demo` after source-set migration.

- `scripts/demo-mobile-environment.py`, `scripts/test_demo_mobile_environment.py`:
  read-only tool/profile resolution and reproducible qualification manifest.
- `dawn4k/src/commonMain/kotlin/org/graphiks/dawn4k/Device.kt` and
  `internal/DeviceSession.kt`; new `SurfaceTextureViewTest.kt` and
  `SurfaceTextureViewGpuTest.kt`: typed native-view
  ownership accessor replacing demo reflection, scoped backend change only.
- `dawn4k-demo/src/commonMain/kotlin/org/graphiks/dawn4k/demo/`: ParticleControls,
  ParticleFrameClock, DemoGeometry, DemoSessionState, DemoSessionController,
  DemoSurfaceLease, ParticleDemoRunner, DawnSurface, SurfaceConfiguration,
  ParticleControlPanel and ParticleDemoContent.
- `dawn4k-demo/src/commonTest/kotlin/org/graphiks/dawn4k/demo/`: pure geometry,
  session reducer, clock and controls tests. Native GPU behavior is not mocked
  into these tests.
- `dawn4k-demo/src/jvmMain/kotlin/org/graphiks/dawn4k/demo/`: migrated desktop
  Main/DemoApp/hosts/WaylandNative/qualification probes as appropriate;
  DesktopSessionOwner adapts existing lifetime; ParticleComposeScene and
  WaylandRasterFrames host common content without AWT windows on Wayland.
- `dawn4k-demo/src/jvmTest/kotlin/org/graphiks/dawn4k/demo/`: all existing JVM
  tests, plus actual shared-content/scene tests and updated live checkpoints.
- `dawn4k-demo/src/androidMain/kotlin/org/graphiks/dawn4k/demo/`:
  AndroidDemo, AndroidSurfaceHost, AndroidNativeWindow, AndroidDemoControllerStore.
- `dawn4k-demo/src/androidDeviceTest/kotlin/org/graphiks/dawn4k/demo/`:
  AndroidDemoGpuTest and AndroidDemoLifecycleTest on a real emulator.
- `dawn4k-demo/src/androidMain/cpp/native-window.c` and `CMakeLists.txt`:
  tiny JNI retain/release adapter, compiled through explicit NDK tasks rather
  than unsupported KMP externalNativeBuild.
- `dawn4k-demo/src/iosMain/kotlin/org/graphiks/dawn4k/demo/`:
  DemoViewController, IosSurfaceHost, ParticleMetalView and IosDemoLifecycle.
- `dawn4k-demo/androidApp/`: separate Gradle launcher :dawn4k-demo-androidApp,
  build.gradle.kts, src/main/AndroidManifest.xml and src/main/kotlin/
  org/graphiks/dawn4k/demo/app/MainActivity.kt. Activity delegates to AndroidDemo.
- `dawn4k-demo/iosApp/`: checked-in minimal Dawn4kDemo.xcodeproj, Dawn4kDemo/App.swift,
  Info.plist and Dawn4kDemoUITests/DemoAcceptanceTests.swift. KMP framework owns UI.
- `dawn4k-demo/src/main/c/wayland-host.h/.c` remains native C location; add
  wayland-ui-buffers.h/.c and tests. Native script paths remain stable.
- `scripts/test-demo-mobile.py`, `scripts/test-demo-ui-parity.py` and existing
  `test-compose-wayland-probe.py`: bounded launch/real-input acceptance and evidence.
- `dawn4k-demo/build.gradle.kts`, settings.gradle.kts, gradle/libs.versions.toml,
  native build/Docker scripts and README: KMP packaging/task migration and launches.

## Shared Interfaces and Ownership

Task 3 defines the following complete logical model. Task 4 uses it for common
content; platform hosts implement leases in Tasks 6/7/9. New functions in this
section are plan-produced APIs, not assertions they exist already.

```kotlin
data class LogicalViewport(val x: Float, val y: Float, val width: Float, val height: Float)
data class PixelExtent(val width: Int, val height: Int)
enum class DemoPhase { Initializing, Running, Stopping, Stopped, Failed }
data class DemoSessionState(
    val id: Long = 1L,
    val phase: DemoPhase = DemoPhase.Initializing,
    val lifecycleActive: Boolean = true,
    val error: String? = null,
)
interface DemoSurfaceLease : AutoCloseable {
    val generation: Long
    val backend: DawnBackend
    val valid: Boolean
    fun pixelExtent(): PixelExtent
    fun createSurface(bridge: NativeBridge, device: DawnDevice): DawnSurface
}
class DemoSessionController {
    val controls: ParticleControls
    val state: StateFlow<DemoSessionState>
    val surface: StateFlow<DemoSurfaceLease?>
    fun attach(lease: DemoSurfaceLease)
    suspend fun detach(generation: Long)
    fun lifecycle(active: Boolean)
    suspend fun stop()
    suspend fun restart()
    suspend fun close()
}
```

Controller construction takes the caller-owned CoroutineScope and an injectable
monotonic clock: `DemoSessionController(scope: CoroutineScope,
nowNanos: () -> Long = ::demoMonotonicNanos)`. `demoMonotonicNanos(): Long` uses
Kotlin TimeSource.Monotonic with a process-local origin; no platform wall clock.
Constructor creates controls/state/surface and a serialized command owner.
Its controller-owned CoroutineScope child runs `runParticleDemo(controller)`;
restart serially joins the old child and replaces controls data for the new id,
without replacing the public controls object observed by Compose.

`attach` publishes only to that owner, retires an older lease and rejects
attachments after final close. `detach(generation)` invalidates that generation,
waits for its Dawn surface/frame release, then closes the retained lease; stale
detach cannot remove a newer surface. Pending frames finish/release on their
owner before detach acknowledgment. Platform callbacks schedule detach on a
non-UI scope and retain native target references through acknowledgment; they
must synchronously mark a destroyed lease invalid before scheduling detach.
An invalid lease is never newly acquired/presented. Native view destruction
during an already-started native call is tested explicitly, not assumed solved
by a Kotlin flag.

Runner retains context/device/ParticleScene for the active session while swapping
presentation surfaces. First usable surface determines texture format; if a new
surface cannot support the existing format, fail visibly rather than recreate
ParticleScene/reset data silently. Capability negotiation runs on every new
surface and preserves that format. A zero extent or lifecycle inactive state
suspends frame acquisition and resets clock baseline, without closing scene.

`DawnSurface.acquireFrame(): BorrowedSurfaceTexture`, `configure`, `present` and
idempotent close remain. New common `BorrowedSurfaceTexture.createView(device:
DawnDevice): GPUTextureView` delegates to Task 2 accessor; frame guards reject
use after release. No Java reflection or native pointer conversion outside the
owning adapter. Failed/stopping/stopped phases cannot accept count/Reset changes.

Keep existing test/caller compatibility with `initialize(maximum: Int, sessionId:
Long = currentSessionId)` and `fail(message: String, sessionId: Long = currentSessionId)`;
asynchronous renderer callbacks always pass their captured id explicitly. Calling
`beginSession(id)` intentionally resets availableCounts/count/pause/reset/error
once. Late callbacks do not use the default current id.

After Stop, the mounted mobile view may still have a valid underlying Surface/
CAMetalLayer but its old retained lease has been closed. On new Initializing id,
the host reacquires a fresh retained lease from that currently valid view, with
a new generation, or waits for its real creation callback. Never reuse a closed
lease or wait forever for a callback that will not reoccur merely because the
controller restarted. Stopping/Restart button taps are serialized and show a
disabled transitional lifetime action rather than opening parallel sessions.

## Evidence Protocol and Harness Contracts

Common test diagnostics are opt-in, not a user-facing remote control channel.
`DemoEvidence.snapshot(): DemoEvidenceSnapshot` returns session id/phase,
actual backend/adapter, count/paused/resetGeneration, lifecycleActive, firstError,
logical button/viewport bounds, physical extent, surface generation, frameCount
and `simulationFingerprint`. The fingerprint is a deterministic checksum of
actual particle buffer data sampled through GPU readback at a synchronized
checkpoint, not a hash of control state. Readback is opt-in and excluded from
normal frame loop performance. Adapter/backend identity comes from actual Dawn.

`DemoEvidence` and snapshot types live in commonMain with a no-op default sink;
tests install a sink. Android/iOS debug hosts expose a read-only snapshot file
inside app storage for adb run-as/simctl container retrieval. No writes to this
file change application state. Release hosts do not expose a test command API.

Python harness functions used by Tasks 6–10:

```python
def await_snapshot(read, predicate, timeout=5):
    # `read` is a callable returning the latest app-written JSON dictionary.
    deadline = time.monotonic() + timeout
    last = None
    while time.monotonic() < deadline:
        last = read()
        if predicate(last):
            return last
        time.sleep(0.05)
    raise AssertionError(f'state deadline exceeded: {last}')

def simulation_state(s):
    return (s['sessionId'], s['count'], s['paused'], s['resetGeneration'])
```

`click_control(name)` locates the latest actual common button bounds and sends
native pointer/touch input (WayVNC RFB, Android instrumentation/adb, iOS XCTest).
Do not hardcode label-dependent coordinates or call controls methods as a
substitute for clicks. iOS XCTest finds actual accessible buttons; debug snapshot
bounds provide a coordinate fallback if native accessibility interop requires it.
Frame acceptance waits for incremented frameCount from successful native present
and captures visible output; no configure-only success or fixed-sleep proof.

Deadlines: build/link 600s, native app startup 30s after build, control transition
5s, clean user Close/Stop 10s. Simulator boot may use its documented blocking
bootstatus/adb boot-completed deadline 120s. Scripts own/clean only processes and
new profiles they started; existing user AVDs/devices/desktops are preserved.

---

### Task 1: Pin Mobile Prerequisites and Qualifiable Dependency Variants

**Files:** Create scripts/demo-mobile-environment.py and test_demo_mobile_environment.py;
modify libs.versions.toml/settings repository filters only if required for the
root suite-demos coordinate. Record evidence under build/demo-mobile-environment/.

**Interfaces:** `select_ios_device(devices: dict, runtime: str, udid: str | None = None) -> str` returns
one available owned-or-user-selected simulator UDID; `select_android_image(sdk:
Path, api: int, abi: str) -> Path` rejects absent/wrong-ABI images. CLI
`python3 scripts/demo-mobile-environment.py --report PATH` writes tool versions,
SDK/NDK/profile paths, status and dependency variant metadata, without boot/install.

- [ ] Write failing unittest fixtures: unavailable iOS runtime cannot be selected,
  two matching devices require explicit UDID rather than arbitrary selection,
  arm64 image cannot satisfy x86_64 profile, missing emulator fails clearly,
  user AVD files remain byte-identical. Use literal JSON/directory fixtures.
  Tests load the hyphenated CLI module with importlib.util.spec_from_file_location;
  CLI entrypoint is guarded so import cannot inventory tools or boot devices.

```python
with self.assertRaisesRegex(ValueError, 'unavailable'):
    select_ios_device({'devices': {'ios-test': [
        {'udid': 'A', 'isAvailable': False, 'name': 'Phone'}]}}, 'ios-test')
```

- [ ] Run RED: `python3 -m unittest discover -s scripts -p 'test_demo_mobile_environment.py' -v`.
  Implement metadata/profile validation, SDK binary lookup and bounded subprocess
  reads. Do not create/boot profiles or install SDK components in this task.
- [ ] Run GREEN and read-only actual inventory. Candidate iOS baseline: an
  available iOS 26.5 ARM64 iPhone runtime under current Xcode. Candidate Android:
  API35 or36 installed arm64-v8a image, standard 4KiB first milestone. Create
  a dedicated profile at Task 6; do not use bench/Kadre AVDs. Check selected NDK
  28.2.13676358 contains required host/target toolchains; pin the choice in report.
- [ ] Read/record root suite-demos Gradle metadata and Android/iosSimulatorArm64
  artifacts from the same snapshot publication. Add alias
  `suite-demos = { module = "org.graphiks:suite-demos", version.ref = "webgpu" }`.
  Preserve existing snapshot repository group filtering; enable Gradle dependency
  verification/locking for selected artifacts at Task 4 to record bytes actually
  resolved, not merely moving metadata. Native Dawn archive hashes stay unchanged.
- [ ] Commit scoped prerequisite tooling/catalog change. Gate: missing tools or
  dependency variant fails explicitly; no simulator/GPU success claimed.

### Task 2: Replace Reflection With a Typed Texture-View Ownership Accessor

**Files:** Modify backend Device.kt and internal/DeviceSession.kt; create
commonTest/SurfaceTextureViewTest.kt and SurfaceTextureViewGpuTest.kt. Modify current demo ParticleDemoRunner.kt
before migration and BorrowedSurfaceTexture in DawnSurface.kt.

**Interfaces:** New platform-integrator operation
`DawnDevice.adoptSurfaceTextureView(handle: Long, label: String = "surface-view"):
GPUTextureView`. The caller supplies an owned nonzero WGPUTextureView reference
created from a live surface texture for this exact device. Successful return
transfers one reference to the session registry; failure does not transfer it.
Do not pretend a raw pointer proves its device provenance. This accessor is
outside the WebGPU interface, matching existing documented nativeHandle/bridge.
`DeviceSession.requireOpen()` is worker-confined and prevents adoption after close.

- [ ] Write RED common/backend tests: null handle rejected, closed session cannot
  adopt, view close followed by device close releases its reference once, device
  close releases an unclosed adopted view once. Use existing ResourceRegistry/
  dispatcher native helper seams for reference counts plus one actual GPU view;
  no fake view constructor substituted for the operation under test.
- [ ] Run failing backend suite and a Mac GPU test in its existing gpuTestJvm
  task; observe missing accessor before implementation.
- [ ] Implement dispatcher-confined adoption and exception-safe caller:

```kotlin
fun DawnDevice.adoptSurfaceTextureView(handle: Long, label: String): GPUTextureView =
    session.runtime.dispatcher.call {
        require(handle != 0L) { "the surface texture view is null" }
        session.requireOpen()
        DawnTextureView(session, WGPUTextureView(NativeAddress(handle)), label)
    }
// In BorrowedSurfaceTexture.createView, on bridge.call:
val view = wgpuTextureCreateView(handle, null) ?: error("surface texture view unavailable")
try { device.adoptSurfaceTextureView(view.handler.rawValue, "surface-view") }
catch (failure: Throwable) { wgpuTextureViewRelease(view); throw failure }
```

Place the public method inside DawnDevice so it can access its internal session;
the snippet shows operation shape, not an extension bypassing visibility.
Registration must be atomic with ownership transfer; if constructor registration
can throw after owning, make that invariant explicit before adding caller release.
- [ ] Replace the reflection helper entirely and run GREEN: backend common/JVM
  tests, actual GPU texture view frame on Mac and demo suite. Assert released
  borrowed textures are refused and acquired textures are released, never
  destroyed. Add KDoc ownership/provenance/dispatcher requirements.
- [ ] Commit narrow backend accessor and reflection removal. No public surface
  subsystem redesign or generated binding regeneration.

### Task 3: Shared Session, Clock and Surface-Replacement Policy

**Files:** Create DemoGeometry, DemoSessionState, DemoSessionController,
DemoSurfaceLease, DemoEvidence and tests under the temporary existing JVM tree;
Task 4 moves portable files to commonMain/commonTest. Modify runner/clock/controls
and current native hosts through a DesktopSurfaceLease adapter, preserving the
old SurfaceHost API until desktop call sites switch atomically in Task 4.

**Interfaces:** Implements Shared Interfaces/Evidence contracts above. Controls
add internal `beginSession(id: Long)` and session-id-aware readiness/failure
updates; UI-facing togglePause/reset/selectCount remain. `ParticleFrameClock.suspend()`
clears its previous baseline. `runParticleDemo(controller)` keeps device/scene
alive across presentation-lease changes and produces evidence after present.

- [ ] Write failing pure reducer/clock tests for stale ready/fail ids, first-error
  wins, repeated Stop/Restart serialization and lifecycle/user pause separation.
  Reducer inputs are real user/native events, not mock renderer successes.

```kotlin
val clock = ParticleFrameClock()
val running = ParticleControlState(ready = true)
assertEquals(0f, clock.advance(running, 1_000_000_000L))
assertEquals(0.016f, clock.advance(running, 1_016_000_000L), 0.00001f)
clock.suspend()
assertEquals(0f, clock.advance(running, 101_000_000_000L))
```

- [ ] Run RED, implement model and serialized command owner. Monotonic source:

```kotlin
private val demoTimeOrigin = TimeSource.Monotonic.markNow()
internal fun demoMonotonicNanos(): Long = demoTimeOrigin.elapsedNow().inWholeNanoseconds
```

Scope/dispatcher ownership is explicit: controller's commands serialize state;
GPU job runs off UI; all native Dawn operations use NativeBridge.call. Close is
final, Stop is restartable, Restart waits for prior cleanup before incrementing id.
- [ ] Add actual GPU surface-replacement tests on Mac using two retained native
  viewport leases and existing checkpoints. While paused, snapshot actual particle
  buffer fingerprint, detach first surface and attach second, then assert equal
  fingerprint/count/reset generation and presented new surface generation. A
  stale first-generation detach cannot release the new lease. Use the actual
  published `ParticleScene.particleBuffer` for readback, never hash control values.
  Implement `suspend fun particleFingerprint(device: GPUDevice, scene: ParticleScene):
  String` in demo diagnostics using a CopyDst|MapRead staging buffer and FNV-1a
  over actual mapped bytes after queued copy completion:

```kotlin
val source = scene.particleBuffer
device.createBuffer(BufferDescriptor(size = source.size,
    usage = GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead)).use { staging ->
    device.createCommandEncoder().use { encoder ->
        encoder.copyBufferToBuffer(source, 0uL, staging, 0uL, source.size)
        encoder.finish().use { device.queue.submit(listOf(it)) }
    }
    device.queue.onSubmittedWorkDone().getOrThrow()
    staging.mapAsync(GPUMapMode.Read).getOrThrow()
    try {
        var hash = 14695981039346656037uL
        for (byte in staging.getMappedRange().toByteArray()) {
            hash = (hash xor byte.toUByte().toULong()) * 1099511628211uL
        }
        return hash.toString(16)
    } finally { staging.unmap() }
}
```
- [ ] Adapt runner so context/device/scene outlive disposable surface. Poll latest
  lifecycle/lease under its serialized contract, acknowledge detach only after
  outstanding frame/view/surface cleanup, skip zero dimensions. Preserve selected
  texture format or fail visibly. Surface Lost after intentional invalidation
  retires that surface, not the session; unrelated device failure is terminal.
- [ ] Run GREEN on controls/clock/reducer and real replacement/cleanup tests.
  Record actual counts of owner/device/surface creation; no model-only evidence
  for retained particle buffers. Commit session policy and renderer split.

### Task 4: KMP Migration and One Adaptive Compose Content

**Files:** Modify dawn4k-demo/build.gradle.kts, root plugin aliases/catalog and
settings as required. Move current desktop sources/tests into jvmMain/jvmTest;
portable model/runner/surface configuration and UI into commonMain/commonTest.
Create ParticleDemoContent and its shared composition tests; adapt DemoApp/Main
and MetalLayerHost. Add task aliases for existing run/test/distribution usage.

**Interfaces:**

```kotlin
@Composable
fun ParticleDemoContent(
    controller: DemoSessionController,
    mobile: Boolean,
    onLifetimeAction: () -> Unit,
    onViewportBounds: (LogicalViewport) -> Unit,
    viewport: @Composable (Modifier) -> Unit,
    onControlBounds: ((String, LogicalViewport) -> Unit)? = null,
)
```

`viewport` draws only the native host slot; common composition owns all layout.
`mobile` changes lifetime action/target sizes only, not orientation layout rules.
Native host callbacks supply physical bounds/density; common viewport values
are normalized logical dp. Desktop native positions convert dp→AWT logical
window coordinates; backing scale is independently applied by native hosts.

- [ ] Add a failing build-functional test invoking Gradle task discovery and
  platform compilation with expected KMP targets before converting plugins.
  Verify common metadata has no AWT/Java FFM/reflection references. Use actual
  compilation as the gate, not grep as sole proof. Snapshot existing task/resource
  and live opt-in behavior first to preserve fixtures across migration.
- [ ] Convert kotlin("jvm") to org.jetbrains.kotlin.multiplatform and supported
  Android KMP library plugin; configure only needed targets:

```kotlin
kotlin {
    jvmToolchain(25)
    jvm()
    android {
        namespace = "org.graphiks.dawn4k.demo"
        compileSdk = 37
        minSdk = 24
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder { sourceSetTreeName = "test" }
    }
    iosArm64()
    iosSimulatorArm64()
    applyDefaultHierarchyTemplate()
    sourceSets.commonMain.dependencies {
        implementation(project(":dawn4k"))
        implementation(libs.suite.demos)
        implementation(libs.webgpu.descriptors)
        implementation(libs.kotlinx.coroutines.core)
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.ui)
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotlinx.coroutines.test)
        implementation("org.jetbrains.compose.ui:ui-test:1.11.1")
    }
}
```

Configure Android dependencies/backend bytecode compatibility consistently;
if backend Android artifact currently targets unsupported desktop bytecode,
correct Android target options only and prove D8 success. Keep existing backend
native cinterop/link requirements for iOS when demo common raw bindings need
platform libraries; mirror headers/static archive/framework linkage, never guess
that transitive klibs automatically supply missing cinterop/link options.
- [ ] Preserve application/distribution tasks explicitly for the JVM compilation:
  runtime classpath from jvm main output+runtimeDependencyFiles, mainClass MainKt,
  JavaExec run and CreateStartScripts/distribution native resources. Windows DLL
  packaging/default-argument logic and private extraction remain. Old test alias
  depends on jvmTest; native build/test opt-ins attach to jvm resources/tests, not
  Android/iOS tasks. Move probes to JVM tests and rewrite their classpaths without
  removing diagnostic failures. Verify installDist uses packaged resources.
  Prefer the Gradle distribution plugin plus explicit JVM tasks rather than
  forcing the Java/application plugin's incompatible source-set assumptions
  into KMP. Add an exact legacy `test` alias only if not already occupied; it
  must delegate to jvmTest and preserve native opt-in inputs/caching policy.
- [ ] Write failing common layout tests and real Compose semantics tests: 719x600
  compact, 720x359 compact, 720x360 wide; same controls at both modes, initializing
  Close enabled, all GPU buttons disabled, failed error visible, 48.dp mobile
  targets, wrapped counts/scroll access on 320x480 usable content. Test system
  insets by constraints, not by assuming screen pixel size. Example:

```kotlin
assertEquals(LayoutMode.Compact, demoLayoutMode(719f, 600f))
assertEquals(LayoutMode.Compact, demoLayoutMode(720f, 359f))
assertEquals(LayoutMode.Wide, demoLayoutMode(720f, 360f))
rule.onNodeWithText("Pause").performClick()
assertTrue(controller.controls.state.value.paused)
```

`enum LayoutMode { Wide, Compact }` and `demoLayoutMode(widthDp: Float,heightDp:
Float): LayoutMode` live in DemoGeometry; real composition asserts actual bounds,
so helper tests alone cannot prove adaptation. Run RED then implement one shared
panel/content using BoxWithConstraints, Row/Column, scrolling and wrapping.
Give actual ControlButton clickable semantics Role.Button and preserve visible
label semantics/test tags, so XCTest/instrumentation/focus tests exercise real
accessible controls. Use supported pinned Compose UI-test API; configure JVM
JUnit runner and Android instrumented runner in their respective test source
sets, not a platform-specific JUnit dependency in commonTest.
- [ ] Constrain Mac owned CAMetalLayer to reported viewport. Pure fixture: window
  logical height800, viewport(300,20,700,600)→AppKit nonflipped(300,180,700,600),
  scale2 drawable1400x1200. Test flipped origins separately. No GPU overlay over
  controls. X11/Windows bounds adapters consume the same geometry.
- [ ] Run GREEN metadata/JVM/Android/iOS compilations, shared semantics, Mac live
  pointer/resize, X11 live suite and installDist. Record resolved mobile artifacts
  in dependency verification/lock evidence; do not stage broad unrelated lock
  changes. Commit migration/adaptive content. No Wayland production switch yet.

### Task 5: Qualify Compose Scene Raster and Real Input Without AWT Window Hosting

**Files:** Create jvmMain ParticleComposeScene.kt/WaylandRasterFrames.kt and
jvmTest ParticleComposeSceneTest.kt/ComposeSceneWaylandProbe.kt; add opt-in Exec
runComposeSceneWaylandProbe using the normal desktop JVM, not JBR.

**Interfaces:**

```kotlin
data class UiPixelFrame(val width: Int, val height: Int, val stride: Int, val bgra: ByteArray)
class ParticleComposeScene(
    controller: DemoSessionController,
    onClose: () -> Unit,
    onViewportBounds: (LogicalViewport) -> Unit,
    onControlBounds: ((String, LogicalViewport) -> Unit)? = null,
) : AutoCloseable {
    fun resize(logicalWidth: Int, logicalHeight: Int, scale: Int)
    fun render(timeNanos: Long): UiPixelFrame
    fun pointer(type: PointerEventType, x: Float, y: Float, pressed: Boolean)
    fun key(key: Key, type: KeyEventType, shift: Boolean = false)
    fun focus(focused: Boolean)
    override fun close()
}
```

Scene/input/close confined to captured owner dispatcher; pointer receives logical
surface coordinates and scales once to scene pixels. Size is physical, Density
is integer scale. Frame is packed premultiplied BGRA, wl_shm ARGB8888 on supported
little-endian targets. Only common content supplies controls.

- [ ] Write RED tests of actual common composition on raster Skia Surface: read
  actual button bounds, press/release Pause, count256, Reset, Close. Add focus-loss
  cancel after press, focused Enter/Space/Tab and negative unfocused commands.

```kotlin
scene.resize(800, 600, 1)
val first = scene.render(0L)
assertEquals(3200, first.stride)
val button = bounds.getValue("Pause")
scene.pointer(PointerEventType.Press, button.x + button.width / 2, button.y + button.height / 2, true)
scene.pointer(PointerEventType.Release, button.x + button.width / 2, button.y + button.height / 2, false)
scene.render(16_000_000L)
assertTrue(controller.controls.state.value.paused)
```

- [ ] Run RED, then use pinned CanvasLayersComposeScene and Skia raster Surface,
  wrapping its native canvas with Compose graphics Canvas:

```kotlin
val scene = CanvasLayersComposeScene(
    density = Density(scale.toFloat()), size = IntSize(width * scale, height * scale),
    coroutineContext = ownerDispatcher, invalidate = { dirty = true })
scene.setContent { ParticleDemoContent(controller, false, onClose, onViewportBounds,
    viewport = {}, onControlBounds = onControlBounds) }
```

Use actual pinned Surface.makeRasterN32Premul/readPixels signatures, successful
checked allocation/readback and resource close. No SkiaLayer/HardwareLayer,
ComposeWindow, direct Toolkit calls or reflection. Use InternalComposeUiApi opt-in explicitly.
Approved 2026-10-09 qualification exception: the pinned global snapshot manager's
headless Swing/HeadlessToolkit notification queue is permitted. Keep DISPLAY absent
and java.awt.headless=true; verify class-init traces show no AWT Window or
SkiaLayer/HardwareLayer/JAWT host, and report awtToolkitFree=false explicitly.
Implement required PlatformContext focus/input-mode defaults; test them rather
than assuming Empty context supplies working keyboard focus.
- [ ] At scale2, assert physical1600x1200/stride6400, same logical button bounds,
  correct physical pointer conversion and compact/wide switching. Zero size
  suspends raster; reject dimensions/stride overflow and unsupported scale.
- [ ] Run GREEN on Mac JVM and isolated ARM64 6084 child JVM with DISPLAY absent
  and java.awt.headless=true. Save actual PNGs for inspection and result booleans
  sceneRaster/pointerControls/focusedKeys/scale2; no native toplevel/Dawn claim yet.
- [ ] Commit qualified scene adapter; failure blocks Wayland integration, not
  permission to revert to keyboard controls/JBR fallback.

### Task 6: Real Android/iOS Native Surface Qualification and Packaging

**Files:** androidMain and iosMain host files, tiny Android JNI source/build,
androidApp Gradle/manifest/MainActivity, iosApp Xcode shell/UI test target,
settings mapping, scripts/test-demo-mobile.py and platform GPU tests.

**Interfaces:** `@Composable fun AndroidDemo(controller: DemoSessionController)`;
`fun DemoViewController(): UIViewController`; AndroidSurfaceHost and IosSurfaceHost
implement DemoSurfaceLease. `AndroidNativeWindow.acquire(surface: android.view.Surface):
Long` returns retained ANativeWindow; `release(address:Long)` releases exactly once.
C JNI adapter invokes ANativeWindow_fromSurface/release; no raw Java object is
cast to a native window. DawnSurface adds createAndroid using actual generated
WGPUSurfaceSourceAndroidNativeWindow source; Metal factory stays common.

- [ ] Write failing Android retained-window tests (null/invalid Surface, balanced
  acquire/release, two surface generations) and iOS live layer tests (drawable
  dimensions/content scale, retained layer survives view removal until detached).
  Add frame-present acceptance before implementation. Compile missing API RED;
  do not treat a platform-skipped task as RED/GREEN.
- [ ] Implement Android JNI with these operations only:

```c
JNIEXPORT jlong JNICALL Java_org_graphiks_dawn4k_demo_AndroidNativeWindow_acquire(
    JNIEnv *env, jobject self, jobject surface) {
    if (!surface) return 0;
    ANativeWindow *window = ANativeWindow_fromSurface(env, surface);
    return (jlong)(uintptr_t)window;
}
JNIEXPORT void JNICALL Java_org_graphiks_dawn4k_demo_AndroidNativeWindow_release(
    JNIEnv *env, jobject self, jlong address) {
    if (address) ANativeWindow_release((ANativeWindow *)(uintptr_t)address);
}
```

Choose Kotlin object external method layout and generated JNI symbol signatures
consistently, verify actual compiled symbols. Kotlin lease guards idempotence.
Compile libdawn4k_demo_window.so for arm64-v8a/x86_64 with pinned NDK; package
generated jniLibs with platform-scoped tasks. Check packaged Dawn dependency
library/ABI with APK inspection and native load on emulator, not only C compile.
AndroidNativeWindow initializes with System.loadLibrary("dawn4k_demo_window");
generated jniLibs are wired to the KMP Android artifact or packaging-only APK
task explicitly, never dropped because the old JNI merge task name changed.
- [ ] Host SurfaceView through AndroidView in the common viewport slot; its
  layout is owned by common content. Surface callback retains a generation,
  emits physical dimensions, marks invalid on destruction, asynchronously
  detaches on a non-UI scope. Do not wait indefinitely on the main thread.
  Native window reference stays retained until frame/surface releases acknowledge
  detach; invalidated native-call failures cannot be mistaken for GPU readiness.
- [ ] On iOS, use UIKitView inside common viewport and a dedicated UIView whose
  layer is CAMetalLayer. Compose's own layer remains separate. Main-thread
  UIView layout updates drawableSize from bounds*contentScaleFactor; backend
  Metal surface owns only a retained layer lease. Controller provided by
  ComposeUIViewController; Swift shell embeds it and contains no simulation.
- [ ] Add launchers and checked-in test targets. Android Activity delegates to
  AndroidDemo; no UI controls in launcher. Framework name Dawn4kDemo, static iOS
  framework plus required Dawn archive/framework linkage. Xcode build invokes
  actual KMP embedAndSignAppleFrameworkForXcode task with simulator environment;
  validate static symbol/link ordering and duplicate-symbol absence.
  Settings maps the approved packaging module exactly:

```kotlin
include(":dawn4k-demo-androidApp")
project(":dawn4k-demo-androidApp").projectDir = file("dawn4k-demo/androidApp")
```
- [ ] Boot dedicated profiles only during execution. Android CLI inventory picks
  installed arm64 image; create named dawn4k-demo-qualification AVD without
  changing user profiles, run emulator with supported Vulkan configuration and
  check adb reported ABI. iOS choose explicit inventory UDID or create dedicated
  device from an available iOS26.5 runtime; preserve user devices. Install/run:

```bash
./gradlew :dawn4k-demo-androidApp:assembleDebug
adb -s "$ANDROID_SERIAL" install -r dawn4k-demo/androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s "$ANDROID_SERIAL" shell am start -n org.graphiks.dawn4k.demo.app/.MainActivity
xcodebuild -project dawn4k-demo/iosApp/Dawn4kDemo.xcodeproj -scheme Dawn4kDemo \
  -configuration Debug -destination "platform=iOS Simulator,id=$IOS_UDID" \
  -derivedDataPath dawn4k-demo/build/ios-derived build
xcrun simctl install "$IOS_UDID" dawn4k-demo/build/ios-derived/Build/Products/Debug-iphonesimulator/Dawn4kDemo.app
xcrun simctl launch "$IOS_UDID" org.graphiks.dawn4k.demo.ios
```

These package/bundle IDs and product names are produced by this task. Set
Android archive basename to androidApp-debug.apk explicitly if module naming
would differ; harness consumes actual Gradle output rather than guessing.
- [ ] Run GREEN native frames and actual touch Pause/256/Reset/Stop/Restart on
  both apps; inspect screenshots and backend/adapter identity. GPU frames must
  increment from native present with visible particles, not just UI labels.
  Record androidVulkan/iosMetal/commonTouch as true only after observed success.
  Missing Vulkan/Metal is a failed gate; no silent GPU renderer substitution.
- [ ] Commit hosts/packaging/test fixture after both qualifications, or retain
  scoped diagnostics and report blocker without claiming mobile support done.

### Task 7: Native Wayland UI Buffer Ownership and Particle Subsurface

**Files:** Existing C host plus wayland-ui-buffers.h/.c/tests, WaylandNative FFM
ABI, native build/pkg-config inputs and child-host tests. Integrate qualified
ParticleComposeScene only in the isolated probe until Task 9.

**Interfaces:**

```c
int32_t dawn_wl_enable_ui(dawn_wl_host *host);
int32_t dawn_wl_ui_present(dawn_wl_host *host, const void *bgra,
    int32_t width, int32_t height, int32_t stride);
int32_t dawn_wl_viewport(dawn_wl_host *host, int32_t x, int32_t y,
    int32_t width, int32_t height, int32_t scale);
void *dawn_wl_particle_surface(dawn_wl_host *host);
```

UI present returns1 queued,0 backpressure,-1 error. Viewport returns1/-1;
logical coordinates, physical buffer dimensions. Extend event ABI with explicitly
sized raw pointer kind/fixed-point x/y/button state/key symbol/modifiers/focus
fields. FFM layout and C sizeof/offsetof oracle must agree field-by-field.
Keep old dawn_wl_surface path only until production switch; particle accessor
returns exclusively the new owned subsurface.
`dawn_wl_enable_ui` is an explicit owner-thread opt-in used by the qualified
scene probe: initialize UI resources/child/raw-input mode only there and fail
atomically with cleanup. The old shipped native-only host still presents to its
parent and keeps its old event route until Task9 changes Main. This avoids
claiming preservation while accidentally attaching UI buffers to the live old
Dawn surface. FFM adds `WaylandNative.enableUi(host)` with checked return value.

- [ ] Write RED native tests: 3 busy8x4/stride32 slots present1/1/1 then0;
  released slot permits reuse, other busy pixels unchanged; resize retires old
  generation without freeing busy memory, at most2 generations retained; overflow/
  bad stride fails before allocation; all fd/mmap/proxies cleaned on partial bind.
- [ ] Run RED, implement mmap-backed wl_shm parent UI buffers and release listeners.
  Copy Kotlin pixels before FFM arena closes. Defer new generation allocation on
  saturation, keep latest UI dirty until release, never enqueue unlimited frames.
  On disconnect reclaim only after connection/server ownership is gone. Close
  cannot invoke callbacks on freed host; protocol destruction and local cleanup
  order is documented and exercised under sanitizers.
- [ ] Bind wl_shm/wl_subcompositor explicitly and reject missing ARGB8888 or
  subcompositor. Create desynchronized particle child, logical position and
  buffer scale. Empty child input region routes full-window coordinates to
  parent. Parent commit presents UI; Dawn never attaches buffers to parent.
- [ ] Add raw pointer/keyboard/focus events. Use libxkbcommon keymap/modifier
  decoding, fd cleanup/replacement and correct ordering; never translate keys
  into simulation actions in UI mode. Preserve Compose focused activation, cancel pointer
  on loss, and no stale releases on destroyed scene. Preserve old application
  route until Task9 switch so visible existing demo remains usable meanwhile.
- [ ] Run actual one-window probe on6084 with common adaptive scene+Dawn child:
  no DISPLAY/XWayland, one xdg toplevel, visible common UI and particles; actual
  clicks Pause/256/Reset/Resume at scale1→2→1 and compact/wide resize. Inspect
  queued buffers/protocol in WAYLAND_DEBUG, private queue read/cancel pairing;
  no dispatch of Vulkan queue. Test delayed buffer releases and pointer press→leave.
- [ ] Run GREEN ASan/UBSan, compiled ABI oracle, ARM64 native cycles and real
  close/setup/presentation/disconnect in6082. Compile x64 separately, label
  compile-only. Commit qualified host/FFM/scene integration; do not switch Main yet.

### Task 8: Mobile Rotation, Background and Restart Lifecycle Proof

**Files:** AndroidDemoControllerStore, AndroidDemo lifecycle callbacks, iosMain
IosDemoLifecycle, common controller tests and Android/iOS actual lifecycle tests,
test-demo-mobile harness/checkpoints.

**Interfaces:** Android ViewModel/controller store survives configuration change;
only final destruction closes controller. iOS app/scene active notifications call
controller.lifecycle without toggling controls.paused. Activity/view recreation
replaces surface generation, not controller session. `detach` acknowledgment
coordinates native retained-reference cleanup. Both hosts use the Task3 clock.

- [ ] Write RED actual rotation test while user-paused: record common state and
  GPU buffer fingerprint, rotate, await new surface presented, assert unchanged
  count/reset/user pause/fingerprint and new extent/layout. Test compact↔wide
  boundaries through actual app view constraints and safe-area screenshots.

```python
before = await_snapshot(read, lambda s: s['paused'] and s['frameCount'] > 0)
rotate_device()
after = await_snapshot(read, lambda s: s['surfaceGeneration'] != before['surfaceGeneration']
    and s['frameCount'] > before['frameCount'], timeout=30)
assert simulation_state(after) == simulation_state(before)
assert after['simulationFingerprint'] == before['simulationFingerprint']
```

`rotate_device()` uses Android instrumentation orientation/XCTest device
orientation, not direct state mutations. Fingerprints are taken at paused GPU
readback checkpoints and remain valid across the actual surface swap.
- [ ] Implement retained Android session store and iOS notification lifecycle;
  unsubscribe/release on final host disposal, no duplicate listeners after
  recomposition. Route old callback ids through controller's stale-id guard.
  Native view removal releases presentation only, not active simulation/device.
  On restarted id, reacquire native window/layer lease from the still-mounted
  valid native view, increment generation and attach; test native view has not
  been recreated, so no SurfaceCreated/layout callback is required for restart.
- [ ] Write RED background/return tests for running and user-paused modes:
  background stops acquisition/time advancement; after return first resumed
  delta0 and subsequent deltas<=0.05; paused setting untouched. Validate actual
  frame/simulation data, not only LifecycleOwner currentState. Process-kill test
  expects fresh defaults, not claimed restoration.
- [ ] Add real NativeBridge checkpoints for Stop during capabilities/frame
  present, rapid Stop/Restart twice and old ready/error callbacks. After cleanup
  exactly1 new active owner, old refs released once, new session ready/error not
  overwritten by old result. Failed session has lifetime/restart action enabled;
  first error persists until explicit Restart. No application process exit on Stop.
- [ ] Run GREEN on Android Emulator and iOS Simulator using real packaged apps
  and actual pointer/touch controls. Do not substitute backend disabled iOS test
  tasks. Record frame/surface/session identities, screenshot safe insets and
  cleanup deadlines. Commit mobile lifecycle and evidence harness tests.

### Task 9: Desktop Production Switch, Global Shortcut Removal and Full Parity

**Files:** JVM Main/DesktopSessionOwner/WaylandComposeDemo/WaylandSurfaceHost,
native action dispatch/title hints, shared panel/UI tests, Mac host and all
existing desktop lifecycle/checkpoint/entrypoint fixtures, parity scripts.

**Interfaces:** `runWaylandComposeDemo()` creates controller, native host and
scene owner; Main's existing Linux selector invokes it before AWT. Desktop
lifetime action awaits GPU/controller cleanup then native/scene close, user
exit0 versus disconnect failure. X11/Windows/Mac use the same session/content.

- [ ] Write RED shipped installDist acceptance for common pointer buttons and
  negative global actions. Focus particle viewport and individually send Space,
  R,+,-,Escape; count/pause/reset/session unchanged, app alive. Focus Pause using
  normal navigation and verify Enter/Space activation. Native close still works.
- [ ] Switch Main only after Task7 gate passed; remove old Wayland shortcut
  actions/translation/title hints and unreferenced stepCount. Adapt protocol
  lifetime tests rather than deleting them with old action tests. Common panel
  remains single source; no two application windows or hidden X11 fallback.
- [ ] Add RED terminal error then initialize, Close at real capabilities/present
  checkpoints, scene close once, Dawn surface before native parent, and compositor
  disconnection. Exercise preserved pending-lifecycle script in isolated6082;
  controlled schedule checkpoint is not claimed to recover a hung driver.
- [ ] Run GREEN auto-selection, optional explicit X11/Wayland, absent/stale socket,
  no AWT on Wayland and runtime/resource extraction from installDist. Verify old
  run/test aliases and live opt-out→opt-in invalidation still operate after KMP.
- [ ] Execute cross-host actual parity: same common labels/count states and equal
  logical bounds for equal usable constraints, Mac layer bounds and X11 UI click
  coverage, Wayland scale/resize, mobile touch adaptation/lifetime difference.
  Capture actual content/screenshots; do not pixel-compare native borders/fonts.
- [ ] Run full forced suites sequentially: Mac
  `./gradlew :dawn4k-demo:test :dawn4k:jvmTest :dawn4k-native:jvmTest :dawn4k-native:verifyDawnAbi --rerun-tasks`;
  then6084 Wayland and separate X11 desktop via their /opt/demo/demo.sh with same
  tasks/live opt-ins. Read XML failures/skips/executed counts. Run Python native
  supervisor/launcher tests, mobile installed app UI suites and installDist checks.
  Never run forced Mac/Linux builds concurrently against shared extraction.
- [ ] After native acceptance succeeds, update visible6081, inspect browser and
  retain6080. Commit production switch/tests; Windows/x64 runtime and physical
  mobile devices remain explicitly unexecuted unless actually supplied.

### Task 10: Reproducible Documentation, Review and Integration Handoff

**Files:** dawn4k-demo/README.md, root docs links if moved, .superpowers ledger;
reports/screenshots under dawn4k-demo/build/demo-parity/ (not committed).

**Interfaces:** No new runtime API. Document verified app/task names, preserved
desktop commands, packaging launchers and effective dependency/runtime versions.

- [ ] Update README with adaptive layout/common controls/mobile Stop/Restart,
  standard focused activation, no global shortcuts; remove keyboard-only Wayland
  limitation and false all-AWT-is-X11 statement. Document native-scene internal
  API maintenance risk and integer-scale scope. JBR diagnostic is not required
  production runtime. List exact verified SDK/NDK/AVD/UDID setup and app launch
  commands from Task6 rather than an unqualified generic latest recipe.
- [ ] Run every documented desktop/mobile build/install/launch on the qualified
  hosts. Packaging-only modules contain no duplicate UI/particle logic; mobile
  APK includes only needed ABIs and both verified Dawn/native-window libraries.
  Framework links simulator static archive, not device archive; inspect hashes.
  Report tests, actual native presentation and exclusions separately.
- [ ] Request one fresh-context whole-branch review under preserved Native mode.
  Provide spec/plan/baseline, failed JBR evidence, narrow backend ownership accessor,
  KMP task/resource migration, native buffers/queues, actual simulator and lifecycle
  evidence. Critical/important findings require reproduced RED/GREEN corrections.
- [ ] Run fresh post-review acceptance, preserve visible services/worktree and
  scoped commits. Offer local integration/PR/keep choices only then; no push,
  merge or PR without authorization. Link any actually worked/opened/resolved
  issue or change to this session immediately, not incidental source references.

## Scope Coverage and Execution Order

Task1 prerequisites → Task2 typed adoption → Task3 session/retained simulation →
Task4 KMP/common UI → Task5 raster qualification → Task6 mobile native gate →
Task7 Wayland native gate → Task8 mobile lifetime → Task9 production desktop
switch/parity → Task10 docs/review/fresh handoff. Execute inline and serially;
the common session/native surface contracts deliberately couple this work.

User request and spec coverage:

- Single KMP demo + packaging exception: Tasks1/4/6.
- Portable real particle simulation, typed views and no reflection: Tasks2–4/6.
- Adaptive controls, touch targets/safe areas, Mac common viewport: Tasks4/6/8/9.
- Actual Android Emulator/iOS Simulator Dawn frames: Tasks6/8/10.
- No XWayland/native scene buffers and input: Tasks5/7/9.
- Rotation/background retained data, Stop/Restart/errors: Tasks3/6/8.
- Global shortcuts removed versus normal focused activation: Tasks5/7/9.
- Native ownership/frame/queue/clock/checkpoint evidence: Tasks2/3/7–9.
- Preserved selection/desktop launch/distribution/live fixtures: Tasks4/9/10.
- Fresh independent review/evidence limitations/authorization: Task10.

The five Review Focus classes each have real/pure test gates in their listed
tasks. Source availability is never used to skip a native runtime gate. Missing
variants, unavailable GPU backend, unsafe native lifetime or required broad API
change stops the affected integration with concrete evidence; do not silently
change scope or declare all platforms complete.

## Execution Handoff

Preserve the chosen Native method in the current isolated worktree. This written
plan requires user review before implementation; approval of the merged spec
authorized planning, not execution of artifacts the user has not yet seen.
