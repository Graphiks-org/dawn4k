# Native Wayland Demo Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically run dawn4k-demo on native Wayland without X11, while preserving and testing the existing X11 path.

**Architecture:** A small C xdg-shell client owns the native window and a private Wayland event queue; Java FFM connects it to a dedicated Kotlin host. Dawn creates and presents the Vulkan surface, and a shared AWT-independent runner renders particles for both entry points. A separate Wayland-only Docker configuration proves that no XWayland fallback is involved.

**Tech Stack:** Kotlin/JVM, JDK 25 FFM, C, wayland-scanner, libwayland-client, libxkbcommon, Dawn/Vulkan, Sway, Mesa lavapipe, wayvnc/noVNC, Gradle, Python unittest.

**Spec:** `docs/superpowers/specs/2026-10-07-wayland-native-design.md`

## Global Constraints

- Backend selection is automatic by default, not a required command-line flag.
- The Wayland execution path must run without X11 or XWayland.
- No silent X11 fallback after an advertised Wayland connection fails.
- Retain Linux ARM64, JDK 25, lavapipe software Vulkan, Sway headless, wayvnc, websockify/noVNC, non-root desktop services, and localhost-only publication.
- Build and package the bridge for the existing Linux ARM64 and x64 JVM targets.
- Run the real desktop integration on ARM64 without emulation.
- Stop rendering and complete resource cleanup before destroying the Wayland objects and closing their connection.
- Initially support integer output scale; fractional-scale extensions and client-side decorations are outside this change.
- Do not stop or replace the current desktop implicitly.
- No Compose Wayland port, graphical control panel for this path, SDL dependency, X11 removal, or hardware GPU benchmarking.
- Compilation or OS-gated JVM tests are not equivalent to native presentation validation.

## Review Focus

1. Both display variables exist, but Wayland is stale: report the Wayland error, never launch X11 (Tasks 1 and 6).
2. Configure suggests zero size, or output scale changes mid-render: preserve logical size and configure matching positive physical dimensions (Tasks 2, 4 and 6).
3. Keyboard layout differs, focus is lost, or count is at its limits: respect the keymap and focus, never exceed the permitted counts (Tasks 2 and 4).
4. Close or disconnect arrives while startup or presentation is pending: bounded termination, no use-after-free or surviving owner (Tasks 4–6).
5. Host and Vulkan WSI read the same connection: separate event queues and pair prepare-read/cancel-read correctly; do not steal events or deadlock (Tasks 2 and 6).

## File Map and Execution Rules

All Kotlin source/test paths below are relative to `dawn4k-demo/src/main/kotlin/org/graphiks/dawn4k/demo/` and `dawn4k-demo/src/test/kotlin/org/graphiks/dawn4k/demo/`, respectively. Explicit non-Kotlin paths are repository-relative.

| File | Responsibility |
| --- | --- |
| `LinuxDisplayBackend.kt` | Pure selection and argument validation |
| `dawn4k-demo/src/main/c/wayland-host.h` | Fixed-width C ABI and event types |
| `dawn4k-demo/src/main/c/wayland-host.c` | Protocol objects, private queue, keyboard and lifetime |
| `dawn4k-demo/src/main/c/wayland-host-state.h` | Pure configure/scale/control transitions shared with C tests |
| `dawn4k-demo/src/test/c/wayland-host-test.c` | Native state and failure-path tests |
| `dawn4k-demo/src/test/c/wayland-host-abi.c` | ABI oracle |
| `dawn4k-demo/scripts/build-wayland-bridge.sh` | Generate protocol code and compile bridge/tests |
| `dawn4k-demo/scripts/cross-build-wayland-bridge.sh` | x64 bridge compilation using an x64 sysroot |
| `WaylandNative.kt` | Lazy FFM loading, extraction, ABI decoding |
| `WaylandSurfaceHost.kt` | Event owner, controls, handle lifetime, pixel size |
| `ParticleDemoRunner.kt` | GPU setup and render loop without AWT |
| `WaylandDemo.kt` | Native application lifecycle |
| `Main.kt`, `DemoApp.kt`, `DawnSurface.kt`, `ParticleControls.kt` | Integrate selection, shared runner, native surface and keyboard controls |
| `dawn4k-demo/docker/compose.wayland.yaml`, `sway-wayland.conf` | Separate Wayland-only desktop |
| Existing Docker Python scripts and `Dockerfile` | Backend-aware health, launch and native dependencies |
| `scripts/test-wayland-demo-entrypoint.py` | Distributed launcher and compositor-disconnect checks |
| `dawn4k-demo/README.md` | Automatic selection, two desktops and keyboard usage |

Read the approved spec before execution. Run each listed test before and after its implementation, recording the actual failure/pass rather than assuming it. Do not stage pre-existing untracked files. Commit only each task's named source/test/build/doc files. Local logs and screenshots remain untracked evidence. Stop if native requirements contradict the spec; do not quietly substitute SDL or XWayland.

---

### Task 1: Pure Linux Backend Selection and Control Commands

**Files:** Create `LinuxDisplayBackend.kt`, `LinuxDisplayBackendTest.kt`; modify `ParticleControls.kt`, `ParticleControlsTest.kt`.

**Interfaces:** Produce `enum class LinuxDisplayBackend { Wayland, X11 }`; `selectLinuxDisplayBackend(environment: Map<String, String>, forced: String? = null): LinuxDisplayBackend`; `parseLinuxBackendOverride(args: Array<String>, platform: DemoPlatform): String?`; `ParticleControls.stepCount(direction: Int): Unit`.

- [ ] **1. Write failing selection and control tests.** Include each advertised endpoint alone, both, blank strings, no endpoints, invalid/duplicate overrides and non-Linux overrides. Selection does not probe sockets; startup will prove connection failure separately.

```kotlin
@Test fun waylandWinsWhenBothAreAdvertised() {
    assertEquals(LinuxDisplayBackend.Wayland,
        selectLinuxDisplayBackend(mapOf("WAYLAND_DISPLAY" to "wayland-1", "DISPLAY" to ":0")))
}
@Test fun inheritedSocketAlsoAdvertisesWayland() {
    assertEquals(LinuxDisplayBackend.Wayland,
        selectLinuxDisplayBackend(mapOf("WAYLAND_SOCKET" to "7")))
}
@Test fun x11CanBeForcedInAWaylandSession() {
    assertEquals(LinuxDisplayBackend.X11,
        selectLinuxDisplayBackend(mapOf("WAYLAND_DISPLAY" to "wayland-1", "DISPLAY" to ":0"), "x11"))
}
@Test fun countStepsClampAtDeviceChoices() {
    val controls = ParticleControls()
    controls.stepCount(1) // ignored until ready
    controls.initialize(1024)
    controls.stepCount(1)
    assertEquals(1024, controls.state.value.count)
    controls.stepCount(-1)
    assertEquals(256, controls.state.value.count)
    controls.stepCount(-1)
    assertEquals(256, controls.state.value.count)
}
```

- [ ] **2. Run RED.** `./gradlew :dawn4k-demo:test --tests '*LinuxDisplayBackendTest' --tests '*ParticleControlsTest' --console=plain`; expect unresolved selection/control methods, not a download or native-loading failure.
- [ ] **3. Implement the pure functions.** Require the forced endpoint's variables, give missing-display diagnostics mentioning both protocols and `/opt/demo/demo.sh`, and reject malformed arguments. Use immutable state updates for count stepping:

```kotlin
fun stepCount(direction: Int) = mutableState.update { state ->
    if (!state.ready || direction == 0) state else {
        val index = state.availableCounts.indexOf(state.count)
        val next = (index + direction.coerceIn(-1, 1))
            .coerceIn(0, state.availableCounts.lastIndex)
        state.copy(count = state.availableCounts[next])
    }
}
```

- [ ] **4. Run GREEN** using Step 2's exact command. Also run `--tests '*DemoPlatformTest'`. Do not wire Main yet: the current runnable demo remains unchanged.
- [ ] **5. Commit:** `feat(demo): select Linux display backend automatically`.

### Task 2: Native Wayland Protocol Bridge and Packaged ABI

**Files:** Create the six C/build-script files in the file map, plus `dawn4k-demo/scripts/cross-build-wayland-bridge.sh`; modify `dawn4k-demo/build.gradle.kts` and `dawn4k-demo/docker/Dockerfile`.

**Interfaces:** The C header exports the following ABI, using fixed-width fields and no JVM callbacks:

```c
typedef struct dawn_wl_host dawn_wl_host;
enum dawn_wl_event_type { DAWN_WL_CONFIGURE=1, DAWN_WL_PAUSE=2,
    DAWN_WL_RESET=3, DAWN_WL_COUNT_UP=4, DAWN_WL_COUNT_DOWN=5,
    DAWN_WL_CLOSE=6, DAWN_WL_ERROR=7 };
typedef struct dawn_wl_event {
    int32_t type, width, height, scale;
    uint32_t serial;
    int32_t code;
} dawn_wl_event;
dawn_wl_host *dawn_wl_open(const char *title, int32_t width, int32_t height,
                          char *error, uint32_t error_capacity);
void *dawn_wl_display(dawn_wl_host *host);
void *dawn_wl_surface(dawn_wl_host *host);
int32_t dawn_wl_next_event(dawn_wl_host *host, dawn_wl_event *event,
                           int32_t timeout_ms); /* 1 event, 0 timeout, -1 error */
int32_t dawn_wl_set_scale(dawn_wl_host *host, int32_t scale);
int32_t dawn_wl_set_title(dawn_wl_host *host, const char *title);
void dawn_wl_wake(dawn_wl_host *host); /* safe from another thread */
void dawn_wl_close(dawn_wl_host *host); /* owner only, after Dawn cleanup */
```

`dawn_wl_open` connects and submits the initial empty commit; it does not wait indefinitely for configure. All calls except wake are on the Kotlin event owner, with stable raw display/surface handles borrowed by Dawn until close. `next_event` retains actions in FIFO order and stores terminal errors; overflow becomes a reported error, never silently dropped control requests.

- [ ] **1. Write the native failing harness.** Define `dawn_wl_state { int32_t width,height,scale; }` and `dawn_wl_apply_configure(state,w,h,scale)` in `wayland-host-state.h`, used by production callbacks and tests. Tests pin zero suggestions, invalid scale, overflow, focus loss and shutdown after partial object creation. The ABI oracle prints `sizeof`, alignment and all offsets. A core case:

```c
static void zero_suggestion_preserves_chosen_size(void) {
    dawn_wl_state state = {640, 480, 1};
    assert(dawn_wl_apply_configure(&state, 0, 0, 2) == 0);
    assert(state.width == 640 && state.height == 480 && state.scale == 2);
}
```

- [ ] **2. Run RED in Linux ARM64.** Add build prerequisites before invoking the harness: `libwayland-dev wayland-protocols libxkbcommon-dev pkg-config`. Rebuild the image without restarting the existing container. Run `bash dawn4k-demo/scripts/build-wayland-bridge.sh --test` in a one-shot ARM64 container mounted on the repo; expect missing bridge/state implementation. Record the architecture with `uname -m`.
- [ ] **3. Implement the bridge and build script.** Generate `xdg-shell-client-protocol.h` and `xdg-shell-protocol.c` from `/usr/share/wayland-protocols/stable/xdg-shell/xdg-shell.xml`, into `build/wayland/<target>/generated`. Compile with warnings as errors:

```bash
wayland-scanner client-header "$protocol" "$generated/xdg-shell-client-protocol.h"
wayland-scanner private-code "$protocol" "$generated/xdg-shell-protocol.c"
"$CC" -std=c11 -fPIC -shared -Wall -Wextra -Werror \
  -I"$generated" -Isrc/main/c src/main/c/wayland-host.c \
  "$generated/xdg-shell-protocol.c" $(pkg-config --cflags --libs wayland-client xkbcommon) \
  -o "$output/libdawn4k_wayland.so"
```

The script resolves paths from its location, not the caller's working directory. The snippet's C paths are module-relative. Link all runtime dependencies explicitly. Build tests separately with sanitizers where supported; do not ship a sanitizer runtime dependency.

Bind compositor, xdg_wm_base, output and seat at supported protocol versions; retain registry names for removal. Put every host proxy on a **private wl_event_queue** before dispatch; use a registry proxy wrapper to avoid moving the display's default queue. Vulkan WSI may use its own queues on the same connection. A socket wait follows this protocol:

```c
/* Repeat dispatch_pending until prepare_read_queue succeeds. */
while (wl_display_prepare_read_queue(display, queue) != 0) {
    if (wl_display_dispatch_queue_pending(display, queue) < 0) return -1;
}
/* Flush; poll display fd + wake fd, watching POLLOUT when flush gives EAGAIN.
   Always pair prepared reads with read_events or cancel_read, including
   timeout, EINTR, wake, POLLERR and flush failure paths. */
```

Bound `next_event` to at most the requested timeout, including spurious wakes. Use eventfd or a pipe to wake it; drain it before further waits. Answer pings, acknowledge configure before publishing its size, and publish close/errors without destroying the objects beneath Dawn. Parse/mmap keyboard keymaps, add the evdev offset for xkb translation, update modifiers, handle only focused key presses, close fds on every path. Scale is output-derived; `set_scale` marshals `wl_surface_set_buffer_scale` without a competing commit. Beyond the initial empty commit, Dawn owns buffer attachment/commits.

- [ ] **4. Package the native artifact without breaking Mac builds.** Add Linux-only `buildWaylandBridge` and `testWaylandBridge` tasks, declared source/protocol/compiler inputs and outputs. Stage host output under `build/generated/waylandResources/linux-aarch64` or `linux-x86-64`, connect it to resources and distributions, and skip compiler invocation on non-Linux hosts. Add an explicit `buildWaylandBridgeX64` task for the cross script; normal ARM64 builds do not require its sysroot. A `-Pwayland.targets=linuxArm64,linuxX64` property requests both artifacts and declares both producing tasks as resource dependencies; default to the current Linux architecture. Staging uses only requested target outputs, never whichever stale binaries happen to exist. Never check in generated protocol code or `.so` binaries.
- [ ] **5. Build ARM64 and x64 and run GREEN.** ARM64: `./gradlew :dawn4k-demo:testWaylandBridge :dawn4k-demo:buildWaylandBridge :dawn4k-demo:jar`. Cross build: install the ARM64-hosted `gcc-x86-64-linux-gnu` cross compiler; provision Ubuntu noble amd64 packages with authenticated APT sources restricted to amd64, leaving existing arm64 sources restricted to arm64. Download and extract `libwayland-dev:amd64 libwayland-client0:amd64 libxkbcommon-dev:amd64 libxkbcommon0:amd64` plus their development dependencies into an x64 sysroot, never install them over ARM64 libraries. The cross script uses `x86_64-linux-gnu-gcc`, target `PKG_CONFIG_LIBDIR`, and `PKG_CONFIG_SYSROOT_DIR`. Run `:dawn4k-demo:buildWaylandBridgeX64`; inspect ELF machine and NEEDED entries with `readelf -h -d`. Run only ARM64 native tests; label x64 evidence compile-only. Build `:dawn4k-demo:jar -Pwayland.targets=linuxArm64,linuxX64` and verify both resource names in `jar tf`.
- [ ] **6. Commit:** `feat(demo): add native Wayland client bridge and packaging`.

### Task 3: Backend-Aware Desktop and Separate Wayland-Only Environment

**Files:** Create `dawn4k-demo/docker/compose.wayland.yaml`, `sway-wayland.conf`; modify `start-desktop.py`, `export-environment.py`, `check-desktop.py`, `launch-demo.py`, `test_desktop.py`, `compose.yaml`, `Dockerfile`.

**Interfaces:** Python `validate_desktop_environment(environment, backend)` returns a sanitized environment or raises RuntimeError; backend is `x11` or `wayland` from `DAWN_DESKTOP_BACKEND` (default x11). `run_gradle(root,tasks,environment)` retains its current signature. The X11 desktop's default launch passes `--platform=x11` explicitly because Sway also advertises Wayland.

- [ ] **1. Write failing supervisor/launcher tests.** In `test_desktop.py` add:

```python
def test_wayland_desktop_does_not_require_display(self):
    desktop = load_script("start-desktop.py")
    result = desktop.validate_desktop_environment(
        {"WAYLAND_DISPLAY": "wayland-1", "XDG_RUNTIME_DIR": "/run/user/1000",
         "DISPLAY": ":9", "XAUTHORITY": "/stale"}, "wayland")
    self.assertNotIn("DISPLAY", result)
    self.assertNotIn("XAUTHORITY", result)
    self.assertEqual("wayland-1", result["WAYLAND_DISPLAY"])
```

Also pin missing WAYLAND_DISPLAY, unknown backend, X11 still requiring DISPLAY, stale exported JSON, noVNC-only false readiness and preservation of Gradle failure codes.
- [ ] **2. Run RED:** `python3 -m unittest discover -s dawn4k-demo/docker -p 'test_*.py' -v`.
- [ ] **3. Implement the desktop split.** Keep `sway.conf` unchanged for X11. The new config uses `xwayland disable`, output resolution 1280x800, floating rule matching native `app_id` `org.graphiks.dawn4k.demo`, and the existing foot terminal. Choose the config in the supervisor based on the validated backend. Export only actual compositor values, sanitized before use. Wayland readiness requires Sway IPC **and** a real connection probe using `wayland-info` with a timeout; never require `xdpyinfo` there. Both still probe VNC greeting and HTTP. Add `wayland-utils` and `wtype` for diagnostics/testing.

The override file sets `name: dawn4k-wayland-desktop`, `DAWN_DESKTOP_BACKEND: wayland`, and default port 6081 using `DAWN_WAYLAND_PORT`. It must replace, not append, the base published ports (`ports: !override [...]` with a documented Compose minimum supporting that tag). All ten existing named volumes must resolve to the new project; do not share writable build volumes across desktops. The image contains both backend dependencies but Wayland config never starts XWayland.

```yaml
name: dawn4k-wayland-desktop
services:
  desktop:
    environment:
      DAWN_DESKTOP_BACKEND: wayland
    ports: !override
      - "127.0.0.1:${DAWN_WAYLAND_PORT:-6081}:6080"
```

Launcher default tasks are `:dawn4k-demo:run` and `--args=--platform=x11` only for the X11 desktop; the Wayland desktop uses `:dawn4k-demo:run` without a flag. User-supplied task lists remain untouched. Preserve signal forwarding, logs and ICD selection. Tests need backend-specific opt-ins, not all native tests at once.

- [ ] **4. Run GREEN and inspect both resolved configurations.** Run Step 2 again, then `docker compose -f dawn4k-demo/docker/compose.yaml config` and the same command with `-f dawn4k-demo/docker/compose.wayland.yaml`. Check loopback ports, distinct volumes and default backend. Start **only** the new project with `up -d --build`; inspect health, `uname -m`, exported JSON, `pgrep -a Xwayland` (no match), and `/tmp/.X11-unix` (no listeners). Open noVNC on localhost:6081 and verify the terminal is usable. Existing localhost:6080 must remain available.
- [ ] **5. Commit:** `feat(demo): add isolated Wayland-only desktop validation`.

### Task 4: FFM Host, Configure State and Dawn Wayland Surface

**Files:** Create `WaylandNative.kt`, `WaylandSurfaceHost.kt`, `WaylandHostState.kt`, `WaylandNativeTest.kt`, `WaylandHostStateTest.kt`, `WaylandSurfaceHostTest.kt`, `WaylandSurfaceTest.kt`; modify `DawnSurface.kt`, `dawn4k-demo/build.gradle.kts`.

**Interfaces:** `WaylandSurfaceHost.open(controls: ParticleControls, onClose: () -> Unit): WaylandSurfaceHost`; implements existing SurfaceHost. `WaylandHostState(initialWidth: Int, initialHeight: Int)` supplies `configure(width: Int,height: Int,scale: Int)`, `pixelSize(): Pair<Int,Int>`, and `close()`. `DawnSurface.createWayland(bridge: NativeBridge, deviceHandle: Long, display: Long, surface: Long): DawnSurface`. FFM methods mirror Task 2 ABI without JVM upcall stubs.

- [ ] **1. Write failing pure/ABI tests.** Verify zero suggestions, positive integer scales, multiplication overflow, no pixels before first acknowledged configure, and close before configure. Representative checks:

```kotlin
@Test fun initialConfigureAndScaleDeterminePixels() {
    val state = WaylandHostState(640, 480)
    assertEquals(0 to 0, state.pixelSize())
    state.configure(0, 0, 2)
    assertEquals(1280 to 960, state.pixelSize())
    state.close()
    assertFailsWith<IllegalStateException> { state.pixelSize() }
}
```

Add null-handle rejection with a NativeBridge whose methods throw if reached, using `createWayland(noNativeCalls, 1L, 0L, 1L)` and the inverse. ARM64 opt-in ABI tests compare every C event offset with the FFM layout, validate every exported symbol, and load from the packaged resource, not a developer build path.
- [ ] **2. Run RED:** `./gradlew :dawn4k-demo:test --tests '*WaylandHostStateTest' --tests '*WaylandSurfaceTest'`; then in the new desktop `/opt/demo/demo.sh :dawn4k-demo:test --tests '*WaylandNativeTest'` with `DAWN_WAYLAND_TESTS=1`. Expected missing host/state/factory, not DISPLAY errors.
- [ ] **3. Implement the surface and lazy loader.** Use the existing create helper:

```kotlin
fun createWayland(bridge: NativeBridge, deviceHandle: Long,
                  display: Long, surface: Long): DawnSurface {
    require(display != 0L && surface != 0L) { "the Wayland display or surface handle is null" }
    return create(bridge, deviceHandle) { allocator ->
        val source = WGPUSurfaceSourceWaylandSurface.allocate(allocator)
        source.chain.next = null
        source.chain.sType = WGPUSType_SurfaceSourceWaylandSurface
        source.display = NativeAddress(display)
        source.surface = NativeAddress(surface)
        source.chain
    }
}
```

Extract the bridge resource to a per-process private temporary directory, close streams, and load with a retained Arena; report missing arch/resource/dependency errors precisely. FFM event layout is six consecutive 32-bit fields (24 bytes), verified by the oracle, not assumed silently. Guard Linux-only initialization so ordinary Mac unit tests never load the library.

- [ ] **4. Implement the dedicated event owner.** Create/open/poll/close native objects on one named owner executor. Poll with a maximum 16ms native timeout, handle FIFO events through ParticleControls, and publish immutable snapshots. Calls from render workers may submit short owner commands; no owner call waits for a GPU operation. In pixelSize, marshal the latest buffer scale on the owner before returning the corresponding physical dimensions; don't independently commit. A native close/error invokes onClose/cancellation but **does not free** the native connection. `close()` wakes, stops polling and releases native objects only after the runner has released its surface. It is idempotent, with a 5-second owner termination bound. Set title and `app_id`, updating title after control-state changes.
- [ ] **5. Add and run host/surface integration GREEN.** In ARM64 with `DAWN_WAYLAND_TESTS=1`, open a real host, await configure with a deadline, create Dawn/Vulkan, negotiate capabilities, configure and present. Use `swaymsg` to resize and verify host dimensions, then close while waiting for configure and repeat open/close to check surviving owner threads/fds. Run ABI and pure tests again. Add `DAWN_WAYLAND_TESTS` as a test input and disable caching/up-to-date reuse when set, without removing existing opt-ins. Prove an opt-in run follows a preceding opt-out run and executes native tests.
- [ ] **6. Commit:** `feat(demo): present Dawn Vulkan on a native Wayland surface`.

### Task 5: Shared Particle Runner and Automatic Native Launch

**Files:** Create `ParticleDemoRunner.kt`, `WaylandDemo.kt`, `WaylandDemoWindowTest.kt`; modify `Main.kt`, `DemoApp.kt`, `scripts/test-linux-demo-entrypoint.py`; add `scripts/test-wayland-demo-entrypoint.py`.

**Interfaces:** `suspend fun runParticleDemo(host: SurfaceHost, controls: ParticleControls, negotiateCapabilities: Boolean): Unit` borrows the host, owns context/adapter/device/surface/scene, and releases all before returning. `fun runWaylandDemo(): Unit` owns host and application cancellation; both entry points call the shared runner.

- [ ] **1. Write failing entry-point and runner integration tests.** A spawned distributed application in the Wayland-only desktop must reach `frame 1 rendered` without DISPLAY, AWT initialization or Xlib loading. A stale WAYLAND_DISPLAY with DISPLAY supplied must fail and never report X11 selection. Update the old missing-display test to unset `DISPLAY`, `WAYLAND_DISPLAY` **and** `WAYLAND_SOCKET`; separately force X11 with DISPLAY removed and Wayland present. Add actual process exit-code and timeout assertions, not only log assertions.

```python
# Distributed-launch negative cases: no compositor needed.
command = ["env", "-u", "DISPLAY", "-u", "WAYLAND_DISPLAY", "-u", "WAYLAND_SOCKET",
           "/workspace/dawn4k-demo/build/install/dawn4k-demo/bin/dawn4k-demo"]
result = subprocess.run(command, text=True, capture_output=True, timeout=10)
assert result.returncode != 0
assert "DISPLAY" in result.stderr and "WAYLAND" in result.stderr
assert "HeadlessException" not in result.stderr
assert "AWTError" not in result.stderr
```

The script executes that command inside the desktop through Compose, not on the Mac. Add runner checks for first frame, controls, count/reset, configure after resize, close before ready and release order; gate only real native cases with DAWN_WAYLAND_TESTS.
- [ ] **2. Run RED** with the distributed entry-point script and `--tests '*WaylandDemoWindowTest'` in the Wayland desktop. Before Main is wired, expect the existing missing DISPLAY diagnostic.
- [ ] **3. Extract the existing renderer without unrelated changes.** Move GPU setup, renderLoop and texture-view helper into ParticleDemoRunner; keep borrowed surface texture semantics and frame-clock behavior intact. Capability negotiation remains Linux-only for existing paths and enabled for Wayland. Compose continues to create/own its host around the shared runner:

```kotlin
host.use { runParticleDemo(it, controls, negotiateCapabilities = platform == DemoPlatform.Linux) }
```

WaylandDemo opens a host whose close callback cancels the application Job, runs the renderer off the native event owner, and closes the host in finally **after** runner cleanup. Controls failure prints a useful diagnostic and yields a nonzero process status; normal compositor close/Escape yields successful termination. A compositor connection loss is a reported error. Avoid swallowing setup CancellationException, and bound native waits independently of GPU setup completion.
- [ ] **4. Wire Main before UI initialization.** Change `main()` to `main(args: Array<String>)`, parse the override, and split dispatch:

```kotlin
if (platform == DemoPlatform.Linux) {
    when (selectLinuxDisplayBackend(System.getenv(), forced)) {
        LinuxDisplayBackend.Wayland -> { runWaylandDemo(); return }
        LinuxDisplayBackend.X11 -> initializeLinuxXlibThreading()
    }
}
// Existing Compose launch remains here for X11/macOS/Windows.
```

Keep logging selection separate from GPU backend selection. Validate options on non-Linux before reaching Compose. Do not instantiate AWT types or reference an eager Xlib singleton in the Wayland branch.
- [ ] **5. Run GREEN** using Step 2 plus both entry-point scripts, ParticleControls/FrameClock tests and all existing Mac JVM suites. Run the existing Linux desktop native tests in its separate project with X11 forced; verify the new pure selection has not changed Compose host creation.
- [ ] **6. Commit:** `feat(demo): auto-launch Wayland with shared particle rendering`.

### Task 6: Real Wayland Interaction, Failure Paths and X11 Non-Regression

**Files:** Extend `WaylandSurfaceHostTest.kt`, `WaylandDemoWindowTest.kt`, `scripts/test-wayland-demo-entrypoint.py`; create `scripts/test-wayland-desktop-opt-in.py`; extend `scripts/test-linux-desktop-opt-in.py` if needed for independent opt-ins. Modify production code only for reproduced failures, with the regression test first.

**Interfaces:** Reuse Tasks 1–5. Native test helpers query Sway IPC by `app_id` and unique title, run commands with deadlines, and always clean up owned processes. No X11 automation in Wayland tests.

- [ ] **1. Write failing real-behavior checks.** Use `wtype` while the target is focused for Space/R/+/-/Escape; compare control snapshots/logs, including shifted plus and another XKB layout. Change the output keymap via Sway and restore it in finally. Inject no input after focus loss. Resize the floating native window with Sway IPC and await a new configure plus subsequent presentation. Change HEADLESS-1 scale to 2, assert physical size = logical size × 2, then restore scale 1 and verify continuing frames.

```python
tree = json.loads(subprocess.check_output(["swaymsg", "-t", "get_tree"], text=True))
def nodes(node):
    yield node
    for child in node.get("nodes", []) + node.get("floating_nodes", []):
        yield from nodes(child)
demo = next(n for n in nodes(tree) if n.get("app_id") == "org.graphiks.dawn4k.demo")
assert demo.get("window") is None, "an XWayland XID is not native Wayland"
assert demo.get("shell") == "xdg_shell"
subprocess.run(["swaymsg", f"[con_id={demo['id']}] resize set width 900 px height 600 px"],
               check=True, timeout=5)
```

Test unavailable globals/seat removal through bridge harness failure injection, actual close immediately after launch, and repeated resize while rendering to exercise host/WSI event queues. Include close after a presented frame, not just after setup.
- [ ] **2. Run RED for each uncovered failure**, reproducing it in the real ARM64 desktop. If a new case passes already, retain it as coverage without claiming RED evidence. Logs must distinguish controls state from actual rendered-frame state.
- [ ] **3. Test compositor disconnection in a disposable third project.** Use the Wayland Compose configuration with an explicit project name and unused port 6082, distinct cache volumes, no simultaneous rebuilds of generated source. Start only the tested application there, terminate its Sway process, and await application error exit within 10 seconds. Do not kill Sway in the user's visible desktop. Stop only this disposable project's services afterwards, preserving volumes unless the user authorizes deletion.
- [ ] **4. Implement minimal fixes with regression tests.** Pair each change with the failing case and rerun it; no unreviewed broad host/renderer refactor. Example test pattern for bounded owner release:

```kotlin
val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
while (Thread.getAllStackTraces().keys.any { it.isAlive && it.name.startsWith("dawn-wayland-owner-") }
       && System.nanoTime() < deadline) Thread.sleep(10)
assertTrue(Thread.getAllStackTraces().keys.none {
    it.isAlive && it.name.startsWith("dawn-wayland-owner-")
})
```

- [ ] **5. Run the full matrix and inspect the browser.** Commands:

```bash
docker compose -f dawn4k-demo/docker/compose.yaml -f dawn4k-demo/docker/compose.wayland.yaml exec -T -e DAWN_WAYLAND_TESTS=1 desktop /opt/demo/demo.sh :dawn4k-demo:test :dawn4k:jvmTest :dawn4k-native:jvmTest :dawn4k-native:verifyDawnAbi
docker compose -f dawn4k-demo/docker/compose.yaml exec -T -e DAWN_DESKTOP_TESTS=1 desktop /opt/demo/demo.sh :dawn4k-demo:test :dawn4k:jvmTest :dawn4k-native:jvmTest :dawn4k-native:verifyDawnAbi
./gradlew :dawn4k-demo:test :dawn4k:jvmTest :dawn4k-native:jvmTest :dawn4k-native:verifyDawnAbi --console=plain
python3 -m unittest discover -s dawn4k-demo/docker -p 'test_*.py' -v
python3 scripts/test-linux-demo-entrypoint.py
python3 scripts/test-wayland-demo-entrypoint.py
python3 scripts/test-linux-desktop-opt-in.py
python3 scripts/test-wayland-desktop-opt-in.py
```

Use the browser tools to inspect localhost:6081 noVNC, exercise controls, and capture the actual scene and resize. Open a second shared client and verify it sees the same desktop. Keep localhost:6080 available. Record logs, test report counts including OS-gated cases, native identity, ELF/package inspection, screenshots and unexecuted platform checks in `dawn4k-demo/build/wayland-desktop/verification.md`.
- [ ] **6. Commit:** `test(demo): verify native Wayland and X11 non-regression`.

### Task 7: Usage Documentation and Integration Review

**Files:** Modify `dawn4k-demo/README.md`; update the local progress/evidence report (do not stage local evidence directories).

**Interfaces:** No new runtime interface. Document the shipped commands and actual backend-selection/error semantics.

- [ ] **1. Write the documentation acceptance checklist.** Confirm it contains automatic precedence, both overrides, no silent fallback, every keyboard command, integer scaling limitations, C build/runtime dependencies, platform evidence limitations, two ports/projects, software-rendering warning and troubleshooting for missing/stale sockets/native bridge. Link it to the actual scripts, not invented executable names.
- [ ] **2. Run the documented commands and fix inaccurate instructions.** Wayland:

```bash
docker compose -f dawn4k-demo/docker/compose.yaml -f dawn4k-demo/docker/compose.wayland.yaml up -d --build
docker compose -f dawn4k-demo/docker/compose.yaml -f dawn4k-demo/docker/compose.wayland.yaml exec desktop /opt/demo/demo.sh
```

Verify `http://localhost:6081/vnc.html?autoconnect=1&shared=1&resize=scale`. Existing X11 commands still use only the base Compose file and port 6080. Show `--args=--platform=x11` and `--args=--platform=wayland` for explicit Gradle runs, and the same application arguments for installDist.
- [ ] **3. Commit:** `docs(demo): document native Wayland selection and validation`.
- [ ] **4. Review the whole implementation against the spec.** Use the execution method selected by the user; any requested independent reviewer receives the spec, this plan, implementation base `86496a8`, final head, evidence report and Review Focus list. Reproduce findings before fixing; rerun targeted tests and the relevant matrix after changes.
- [ ] **5. Handoff without implicit integration.** Present verified outcomes and remaining platform limitations. Leave both useful desktops available. Do not push, merge or create a PR without the user's choice; immediately link any worked/created issue or PR to the session with `openchamber session.link` when its URL is known.

## Self-Review Coverage

- Automatic selection and invalid/missing endpoints: Tasks 1 and 5–6.
- Native protocol, keymap, queue isolation, ping/configure and cleanup: Tasks 2 and 4–6.
- Native ABI, two architecture artifacts, no eager loading on Mac: Tasks 2 and 4.
- Dawn descriptor, capabilities, borrowed handles/textures: Tasks 4–5.
- Shared particle renderer and existing controls semantics: Tasks 1 and 5–6.
- Browser-visible scene, no XWayland, no DISPLAY: Tasks 3 and 5–6.
- Resize, integer scale, close during startup and disconnect: Tasks 4–6.
- X11, Mac suites, live-test cache regression and ABI verification: Tasks 5–6.
- Documentation, evidence accuracy and user-controlled integration: Task 7.

Execution recommendation: Native implementation in this session followed by one independent whole-branch review. The bridge, host and runner share tightly coupled lifecycle contracts; keeping their implementation context together reduces interface drift. Per-task RED/GREEN verification remains mandatory.
