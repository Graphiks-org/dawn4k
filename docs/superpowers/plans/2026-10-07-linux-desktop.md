# Linux ARM64 Demo Desktop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Run the Dawn particle demo on native ARM64 Linux inside Docker on this Mac, with a shared browser-visible desktop and working controls.

**Architecture:** Sway runs headless with XWayland and software composition. wayvnc/websockify/noVNC expose the same desktop on localhost. A dedicated X11 child window presents Dawn/Vulkan frames via Mesa lavapipe beside the existing Compose controls.

**Tech Stack:** Kotlin/JVM, JDK 25, Java FFM, Compose Desktop, Dawn chromium/8077, Xlib, Ubuntu 24.04 ARM64, Sway, XWayland, wayvnc, noVNC, Docker Compose, Python standard library for service supervision/tests.

**Spec:** `docs/superpowers/specs/2026-10-07-linux-desktop-design.md`

## Global Constraints

- Native `linux/arm64` container on Docker Desktop; no x64 emulation.
- JDK 25 and `--enable-native-access=ALL-UNNAMED`.
- Preserve macOS/Metal and Windows/D3D12 behavior and existing Linux x64 packaging.
- Keep the pinned Dawn release `v8077.0.0` and generated ABI bindings.
- Ubuntu 24.04 is the initial base; validate the pinned Dawn library's actual glibc/libstdc++ requirements before proceeding.
- Bind only `127.0.0.1:6080`; no privileged mode, GPU devices, host display mounts, or publicly exposed VNC.
- Native resources and child windows outlive their Dawn surfaces; borrowed handles are not released by their borrowers.
- Source is bind-mounted read/write; Linux caches/build outputs are isolated in named Docker volumes.
- No native Wayland surfaces, new Kotlin/Native ARM64 target, GPU performance claims, or unrelated refactoring.
- Do not modify `.github/scripts/__pycache__/` or `.superpowers/`: they predate this task.

## Review Focus

1. Parent disposal during initialization/presentation must cancel cleanly, not crash the JVM through an X11 error or leak an executor (Task 4).
2. An empty/temporarily hidden viewport must stop acquisition, unmap the child, and recover after becoming visible (Tasks 4 and 6).
3. A missing display, unsupported OS, or failing desktop service must produce a bounded, actionable failure rather than an infinite wait (Tasks 2, 4, and 5).
4. Different surface format/alpha capabilities must be negotiated, copied before native members are freed, and passed to ParticleScene (Task 3).
5. A second launch or a new checkout path must not reuse stale architecture-specific build outputs or change permissions on host sources (Tasks 1, 2, and 6).

## File structure and ownership

- Packaging: `bindings/dawn.lock.json`, `dawn4k-native/build.gradle.kts`, `gradle/libs.versions.toml`, `dawn4k-demo/build.gradle.kts`, and the narrowly reviewed generated JVM bootstrap.
- Build verification: `scripts/test-linux-arm64-packaging.py` (new standard-library structural regression test) and `tests/abi/dawn_abi.c` (extend oracle coverage).
- Runtime loader smoke test: `dawn4k-native/src/jvmTest/kotlin/org/graphiks/dawn4k/native/LinuxArm64LibraryTest.kt` (new).
- Desktop: `dawn4k-demo/docker/Dockerfile`, `compose.yaml`, `sway.conf`, `wayvnc.conf`, `entrypoint.sh`, `start-desktop.py`, `export-environment.py`, `demo.sh`, `check-desktop.py`, `test_desktop.py` (all new).
- Presentation: `SurfaceConfiguration.kt` (pure capability selection), `Xlib.kt` (native calls/error boundary), `LinuxSurfaceHost.kt` (window ownership and serialized operations), `DemoPlatform.kt` (OS selection); all new in `dawn4k-demo/src/main/kotlin/org/graphiks/dawn4k/demo/`.
- Integration: modify `DawnSurface.kt`, `SurfaceHost.kt`, `DemoApp.kt`, `Main.kt`, `MetalLayerHost.kt`, and `WindowsSurfaceHost.kt` only where the platform-independent interfaces require it.
- Tests: new `DemoPlatformTest.kt`, `SurfaceConfigurationTest.kt`, `LinuxSurfaceHostTest.kt`, `LinuxSurfaceTest.kt`, and `LinuxDemoWindowTest.kt` in the existing demo test package.
- User guide: `dawn4k-demo/README.md`; verification report/screenshots under ignored build output, not versioned binary assets.

## Task 1: Package Dawn and Skiko for Linux ARM64

**Files:** Packaging and build-verification files listed above; existing `scripts/generate-dawn-bindings.sh` is the generator entry point, not a source to rewrite.

**Interfaces:** Produces classpath resource `linux-aarch64/libwebgpu_dawn.so`; loader platform normalization remains `linux-aarch64`. `-Pdawn.targets=linuxArm64` must prepare only the requested platform while ordinary generation can stage all existing platforms.

- [ ] **Step 1: Add a failing packaging regression test.** Run with Python from any working directory; derive the root from the script path. Include exact release/checksum and loader assertions:

```python
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class LinuxArm64PackagingTest(unittest.TestCase):
    def test_pinned_arm64_archives_and_loader(self):
        lock = json.loads((ROOT / 'bindings/dawn.lock.json').read_text())
        self.assertEqual('v8077.0.0', lock['release'])
        archives = {a['linkage']: a for a in lock['archives']
                    if a['target'] == 'linuxArm64'}
        self.assertEqual({'shared', 'static'}, set(archives))
        self.assertEqual('e0a3656a9786fdb2855bab20bb54b9d71838726417877d531962376db35b6f05',
                         archives['shared']['sha256'])
        self.assertEqual('31815aeaa95ec0b08037686c7008a7353d4f06588336092142da1f37c479f246',
                         archives['static']['sha256'])
        loader = ROOT / 'dawn4k-native/generated/src/jvmMain/kotlin/org/graphiks/dawn4k/native/webgpu_hJvm.kt'
        self.assertIn('"linux-aarch64" to Bundle(', loader.read_text())
        self.assertIn('"linux-x86-64" to Bundle(', loader.read_text())

if __name__ == '__main__':
    unittest.main()
```

- [ ] **Step 2: Run the red test.** `python3 scripts/test-linux-arm64-packaging.py`; expect missing ARM64 entries. This is a structural test, not proof of runtime loading.
- [ ] **Step 3: Add the two ARM64 lock records and staging.** Use names `dawn-chromium-8077-linuxArm64-shared.tar.gz` and `dawn-chromium-8077-linuxArm64-static.tar.gz` with the above SHA-256 values, verified from the release's `SHA256SUMS`. Add `linuxArm64` to default Dawn downloads without adding a K/N target. Add the following staging source:

```kotlin
from(layout.buildDirectory.dir("native/linuxArm64/shared/lib")) {
    include("libwebgpu_dawn.so")
    into("linux-aarch64")
}
```

Add catalog alias `skiko-awt-runtime-linux-arm64` for module `org.jetbrains.skiko:skiko-awt-runtime-linux-arm64` using existing `skiko` version `0.144.6`, and `runtimeOnly(libs.skiko.awt.runtime.linux.arm64)` to the demo. Verify the artifact resolves; never silently upgrade Compose/Skiko.

- [ ] **Step 4: Fix host-specific ABI header selection.** Select `macosArm64` on Mac, `linuxArm64` for Linux aarch64/arm64, and `linuxX64` for Linux amd64/x86_64. Use that selection in `verifyDawnAbi` and `buildDawnAbiHelper`; keep generation's pinned macOS header path unchanged. A Linux-only download must not require an absent macOS header for these tests.

```kotlin
val abiHeaderTarget = when {
    abiHost == "linux-aarch64" -> "linuxArm64"
    abiHost == "linux-x86-64" -> "linuxX64"
    else -> "macosArm64"
}
// In verification/helper tasks:
includeDir.set(layout.buildDirectory.dir("native/$abiHeaderTarget/shared/include"))
// The helper command's -I argument uses the same directory.
```

- [ ] **Step 5: Regenerate and promote only the reviewed bootstrap changes.** Run `bash scripts/generate-dawn-bindings.sh` on Mac (existing script validates LLVM and pinned kextract). Compare `build/regenerated/src` against `generated/src`. Promote the new ARM64 bundle/table and any required identical bootstrap bookkeeping; keep existing macOS/x64 keys and checksums intact. If generation changes ABI layouts or unrelated bindings, stop and investigate instead of copying entire directories. Do not invent a checksum of the shared library from the archive checksum.
- [ ] **Step 6: Verify structural tests and current host compilation.** Run `python3 scripts/test-linux-arm64-packaging.py`, `./gradlew :dawn4k-demo:compileKotlin :dawn4k-native:verifyDawnAbi`, and `git diff --check`. Inspect generated diff. Add a structural assertion that Skiko ARM64 and the staging path exist, then run again. Real ARM64 loading/ABI verification is a mandatory gate in Task 2.
- [ ] **Step 7: Commit only packaging changes.** `git commit -m "feat(native): package Dawn and Skiko for Linux ARM64"` after explicitly staging the files from this task.

## Task 2: Build a browser-visible Linux desktop independent of the demo

**Files:** All new files under `dawn4k-demo/docker/`; initial desktop instructions in `dawn4k-demo/README.md`.

**Interfaces:** Compose service name `desktop`; internal VNC `127.0.0.1:5900`; HTTP/WebSocket `0.0.0.0:6080` inside the container, published only on host loopback. `demo.sh [gradle arguments...]` uses `/workspace`, desktop environment `/run/user/1000/desktop-env.json`, and existing Gradle wrapper. `check-desktop.py` exits 0 only when compositor, display, and web endpoint are ready. `start-desktop.py` owns all child service processes.

- [ ] **Step 1: Add failing supervisor tests.** `test_desktop.py` imports pure functions from `start-desktop.py` with `importlib.util`, without starting services on import. Define `extract_display(environment: list[str]) -> str`, `require_services_alive(processes: dict[str, subprocess.Popen]) -> None`, and `stop_services(processes, timeout: float = 5.0) -> None`.

```python
def test_display_comes_from_sway_environment(self):
    self.assertEqual(':3', desktop.extract_display(['OTHER=x', 'DISPLAY=:3']))
    with self.assertRaisesRegex(RuntimeError, 'DISPLAY'):
        desktop.extract_display(['WAYLAND_DISPLAY=wayland-1'])

def test_failed_service_names_itself(self):
    process = subprocess.Popen([sys.executable, '-c', 'raise SystemExit(7)'])
    process.wait(timeout=5)
    with self.assertRaisesRegex(RuntimeError, 'sway.*7'):
        desktop.require_services_alive({'sway': process})

def test_shutdown_stops_children(self):
    process = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(60)'],
                               start_new_session=True)
    desktop.stop_services({'child': process}, timeout=1)
    self.assertIsNotNone(process.poll())
```

Add a retry test proving compositor readiness has a deadline, and a process test proving SIGTERM triggers cleanup. Run `python3 -m unittest discover -s dawn4k-demo/docker -p 'test_*.py' -v`; expect missing module/functions.

- [ ] **Step 2: Create the image and package dependencies.** Start `FROM ubuntu:24.04`. Install `sway xwayland wayvnc novnc websockify dbus-x11 foot mesa-vulkan-drivers vulkan-tools libvulkan1 libx11-6 libx11-dev libxext6 libxi6 libxrender1 libxtst6 libxrandr2 libgl1 libgl1-mesa-dri libegl1 libfontconfig1 fonts-dejavu-core ca-certificates curl python3 gcc libc6-dev binutils procps x11-utils xdotool grim gosu`. Use `--no-install-recommends`; check package availability during build rather than guessing replacements. Create UID/GID 1000 demo user (reuse/rename the base image's UID 1000 account if present rather than creating a conflicting UID), installing startup files outside `/workspace` so bind mounts cannot hide them.

Pin Temurin `25.0.4.1+1` ARM64 from the vendor API response checked during planning:

```dockerfile
ARG JDK_URL=https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.4.1%2B1/OpenJDK25U-jdk_aarch64_linux_hotspot_25.0.4.1_1.tar.gz
ARG JDK_SHA256=69df11a02cfa3ef7d7ca645e03edce6778ec090e100f6ae2b42097865730ac52
RUN curl -fL "$JDK_URL" -o /var/tmp/jdk.tar.gz \
    && echo "$JDK_SHA256  /var/tmp/jdk.tar.gz" | sha256sum -c - \
    && mkdir -p /opt/java \
    && tar -xzf /var/tmp/jdk.tar.gz --strip-components=1 -C /opt/java \
    && rm /var/tmp/jdk.tar.gz
ENV JAVA_HOME=/opt/java
ENV PATH=/opt/java/bin:$PATH
```

JDK and Dawn checksums must be validated on the actual downloaded bytes. Shell chaining here is Dockerfile syntax, not a request to chain diagnostic tool commands.

- [ ] **Step 3: Add explicit software/headless compositor configuration.** Environment `WLR_BACKENDS=headless`, `WLR_HEADLESS_OUTPUTS=1`, `WLR_RENDERER=pixman`, `XDG_RUNTIME_DIR=/run/user/1000`, `XDG_SESSION_TYPE=wayland`. Start Sway inside `dbus-run-session`; verify the packaged compositor actually supports this configuration.

```text
# sway.conf
xwayland force
output HEADLESS-1 resolution 1280x800
seat seat0 fallback true
font monospace 10
default_border pixel 2
exec foot
```

Use `wayvnc.conf` with `enable_auth=false`, bound to internal loopback. Launch `wayvnc -C /etc/demo/wayvnc.conf 127.0.0.1 5900` using the compositor's actual `WAYLAND_DISPLAY`. Launch `websockify --web=/usr/share/novnc 0.0.0.0:6080 127.0.0.1:5900`. Configure software capture only using options supported by the installed wayvnc version; a desktop screenshot is required to establish compatibility.

- [ ] **Step 4: Implement supervision and readiness.** Spawn each service with its own process group and logs forwarded to container stdout. Poll for the owned Sway IPC socket under `XDG_RUNTIME_DIR`, run `swaymsg -s <socket> -t get_outputs`, then retrieve `get_version`, `get_outputs`, and `get_inputs` through the discovered socket to diagnose readiness. Obtain environment with `swaymsg exec python3 /opt/demo/export-environment.py`; do not assume Sway exports `DISPLAY` back into its parent. Retry exporting until XWayland supplies DISPLAY. The helper atomically writes only the compositor/display environment into the private runtime file:

```python
import json
import os
from pathlib import Path

runtime = Path(os.environ['XDG_RUNTIME_DIR'])
destination = runtime / 'desktop-env.json'
temporary = runtime / f'desktop-env-{os.getpid()}.json'
keys = ('DISPLAY', 'WAYLAND_DISPLAY', 'SWAYSOCK', 'XDG_RUNTIME_DIR',
        'DBUS_SESSION_BUS_ADDRESS', 'XDG_SESSION_TYPE')
temporary.write_text(json.dumps({key: os.environ[key] for key in keys if key in os.environ}))
temporary.chmod(0o600)
temporary.replace(destination)
```

Convert the JSON mapping to `KEY=value` strings before passing to `extract_display`, then update child service environments with that mapping. Wait at most 30 seconds, checking process exit at every iteration.

```python
def require_services_alive(processes):
    for name, process in processes.items():
        code = process.poll()
        if code is not None:
            raise RuntimeError(f'{name} exited with status {code}')

def extract_display(environment):
    for item in environment:
        if item.startswith('DISPLAY=') and item[8:]:
            return item[8:]
    raise RuntimeError('Sway has not provided DISPLAY for XWayland')
```

Call `stop_services` in `finally`, TERM groups in reverse dependency order, wait to deadline, KILL only remaining groups owned by this supervisor, then reap them. Signal handlers set a shutdown flag; they do not raise out of cleanup. Run a health check that reads the environment JSON, validates an X connection with `xdpyinfo`, and requests `/vnc.html` on localhost. Browser screenshot verification remains separate from HTTP health.

- [ ] **Step 5: Add Compose and scoped volume initialization.** Use this service contract:

```yaml
name: dawn4k-linux-desktop
services:
  desktop:
    platform: linux/arm64
    build: .
    init: true
    ports:
      - "127.0.0.1:6080:6080"
    working_dir: /workspace
    shm_size: 512m
    volumes:
      - ../..:/workspace
      - gradle-home:/home/demo/.gradle
      - root-gradle:/workspace/.gradle
      - root-kotlin:/workspace/.kotlin
      - root-build:/workspace/build
      - buildsrc-gradle:/workspace/buildSrc/.gradle
      - buildsrc-build:/workspace/buildSrc/build
      - native-build:/workspace/dawn4k-native/build
      - dawn-build:/workspace/dawn4k/build
      - demo-build:/workspace/dawn4k-demo/build
      - docs-build:/workspace/docs/build
    healthcheck:
      test: ["CMD", "python3", "/opt/demo/check-desktop.py"]
      interval: 5s
      timeout: 5s
      retries: 12
      start_period: 10s
```

Declare each listed named volume. `entrypoint.sh` initializes only those explicitly mounted cache/output directories and `/run/user/1000`, sets runtime mode 0700, and uses `exec gosu demo python3 /opt/demo/start-desktop.py`. Never recursively chown `/workspace` or the source tree. Test fresh volumes and a second startup; inspect source file modes before/after. Use Compose's default scoped names; document `-p` for separate worktree environments and host-port conflicts.

- [ ] **Step 6: Implement the launcher.** `demo.sh` invokes a Python environment reader to exec Gradle rather than sourcing untrusted shell assignments. Select exactly one `lvp*.json` ICD from `/usr/share/vulkan/icd.d/`, fail if absent/ambiguous, set `VK_DRIVER_FILES` and `VK_ICD_FILENAMES` to that path, and set the compositor-provided `DISPLAY`. Default arguments are `:dawn4k-demo:run -Pdawn.targets=linuxArm64 --console=plain`; supplied arguments replace the task list while retaining `-Pdawn.targets=linuxArm64`. Save stdout/stderr to `/workspace/dawn4k-demo/build/linux-desktop/demo.log` without swallowing the child exit code. Python `Popen` plus streaming to stdout/file or Bash `pipefail`/`tee` are acceptable; ensure TERM reaches Gradle and JVM. Use these user-facing commands:

```bash
docker compose -f dawn4k-demo/docker/compose.yaml up --build -d --wait
docker compose -f dawn4k-demo/docker/compose.yaml exec desktop /opt/demo/demo.sh
docker compose -f dawn4k-demo/docker/compose.yaml down
```

- [ ] **Step 7: Verify desktop and ARM64 runtime gate.** Run supervisor tests, `bash -n` on shell entry points, Compose config validation, build/start commands above, and open `http://localhost:6080/vnc.html?autoconnect=1&shared=1&resize=remote` through OpenChamber. Capture the actual desktop. Inside the container run `uname -m`, `java -version`, `vulkaninfo --summary` with the selected ICD, and `/opt/demo/demo.sh :dawn4k-native:verifyDawnAbi :dawn4k-native:jvmTest`. Confirm ARM64, JDK 25, ABI match, and callback helper success. Run a generated-binding instance-create/release smoke test added to `dawn4k-native/src/jvmTest/kotlin/org/graphiks/dawn4k/native/LinuxArm64LibraryTest.kt`:

```kotlin
@Test fun bundledLibraryCreatesAnInstance() {
    if (!System.getProperty("os.name").contains("Linux") ||
        System.getProperty("os.arch") !in listOf("aarch64", "arm64")) return
    val instance = assertNotNull(wgpuCreateInstance(null))
    wgpuInstanceRelease(instance)
}
```

Run without an external `java.library.path`; verify the resource bundle is actually used. Inspect `ldd` for missing dependencies if loading fails. Stop on incompatible base/library requirements and revise the design with the user; do not fall back to x64 silently.
- [ ] **Step 8: Commit desktop plus its tests and usage instructions.** `git commit -m "feat(demo): add a browser-accessible ARM64 Linux desktop"` with task files explicitly staged.

## Task 3: Add Xlib surface creation and Linux capability negotiation

**Files:** `DawnSurface.kt`, new `SurfaceConfiguration.kt` and `SurfaceConfigurationTest.kt`, new `LinuxSurfaceTest.kt`, `tests/abi/dawn_abi.c`.

**Interfaces:** `DawnSurface.createXlib(bridge: NativeBridge, deviceHandle: Long, display: Long, window: Long): DawnSurface`; `DawnSurface.configureForAdapter(adapterHandle: Long): SurfaceConfiguration`; `DawnSurface.textureFormat: GPUTextureFormat`. Pure `selectSurfaceConfiguration(formats: List<UInt>, alphaModes: List<UInt>, presentModes: List<UInt>): SurfaceConfiguration`, where `SurfaceConfiguration(textureFormat: GPUTextureFormat, nativeFormat: UInt, alphaMode: UInt, presentMode: UInt)` is immutable. Defaults remain current BGRA8/Auto/Fifo for existing platforms unless explicitly negotiated.

- [ ] **Step 1: Write red selection tests.** Use generated enum constants, not magic numbers. Include preferred BGRA8, RGBA8-only, alpha fallback, and unsupported/empty cases:

```kotlin
@Test fun acceptsRgbaOnlySurface() {
    val config = selectSurfaceConfiguration(
        listOf(WGPUTextureFormat_RGBA8Unorm),
        listOf(WGPUCompositeAlphaMode_Opaque), listOf(WGPUPresentMode_Fifo))
    assertEquals(GPUTextureFormat.RGBA8Unorm, config.textureFormat)
    assertEquals(WGPUCompositeAlphaMode_Opaque, config.alphaMode)
}
@Test fun rejectsEmptyFormats() {
    assertFailsWith<IllegalStateException> {
        selectSurfaceConfiguration(emptyList(),
            listOf(WGPUCompositeAlphaMode_Opaque), listOf(WGPUPresentMode_Fifo))
    }
}
```

Run `./gradlew :dawn4k-demo:test --tests '*SurfaceConfigurationTest'`; expect unresolved selector.

- [ ] **Step 2: Implement deterministic selection.** Prefer BGRA8Unorm then RGBA8Unorm. Fail explicitly when neither is supported (do not guess a format without a corresponding ParticleScene mapping). Prefer Auto if advertised, otherwise Opaque, otherwise the first advertised valid alpha mode. Require advertised Fifo and render-attachment usage; produce actionable errors for empty mode lists or unsupported usage. Tests pin each branch and the error messages.

```kotlin
val format = listOf(WGPUTextureFormat_BGRA8Unorm, WGPUTextureFormat_RGBA8Unorm)
    .firstOrNull { it in formats }
    ?: error("surface supports neither BGRA8Unorm nor RGBA8Unorm: $formats")
check(WGPUPresentMode_Fifo in presentModes) { "surface does not advertise Fifo presentation" }
val alpha = listOf(WGPUCompositeAlphaMode_Auto, WGPUCompositeAlphaMode_Opaque)
    .firstOrNull { it in alphaModes } ?: alphaModes.firstOrNull()
    ?: error("surface has no supported alpha mode")
```

- [ ] **Step 3: Add pre-native handle validation and Xlib descriptor.** Test zero display/window through a fake `NativeBridge` whose `call` throws if invoked; invalid handles must fail before calling native code. Match the existing descriptor factory pattern:

```kotlin
fun createXlib(bridge: NativeBridge, deviceHandle: Long, display: Long, window: Long): DawnSurface {
    require(display != 0L && window != 0L) { "the X11 display or window handle is null" }
    return create(bridge, deviceHandle) { allocator ->
        val source = WGPUSurfaceSourceXlibWindow.allocate(allocator)
        source.chain.next = null
        source.chain.sType = WGPUSType_SurfaceSourceXlibWindow
        source.display = NativeAddress(display)
        source.window = window.toULong()
        source.chain
    }
}
```

- [ ] **Step 4: Query capabilities on the Dawn worker.** `configureForAdapter` validates a live adapter, initializes all capability fields, calls `wgpuSurfaceGetCapabilities`, copies UInt array values using bounded FFM segments, and always frees returned members in `finally`. Counts must fit a bounded JVM list before multiplying by four; reject null pointers with positive counts and non-RenderAttachment usages. Assign configuration only after a successful selection; `configure` uses it and `textureFormat` exposes its public format. Never pass an adapter after its lifetime. Add native integration assertions for negotiated modes/configuration under Linux desktop.
- [ ] **Step 5: Extend the C oracle and verify.** Add `TYPE` and all `FIELD` entries for `WGPUSurfaceSourceXlibWindow`, `WGPUSurfaceCapabilities`, and `WGPUSurfaceConfiguration` in `tests/abi/dawn_abi.c`. Run unit tests on Mac and ARM64 oracle verification in the desktop. If the oracle's generated parser needs support for these types, extend `buildSrc/src/main/kotlin/org/graphiks/dawn4k/build/DawnAbi.kt` narrowly and test the resulting JSON has the type entries. Native calls must not precede confirmed layouts.
- [ ] **Step 6: Commit.** `git commit -m "feat(demo): support Xlib Dawn surfaces and negotiate Linux formats"` after targeted tests pass.

## Task 4: Own an X11 child window safely

**Files:** new `Xlib.kt`, `LinuxSurfaceHost.kt`, and `LinuxSurfaceHostTest.kt`; modify `SurfaceHost.kt`, `MetalLayerHost.kt`, and `WindowsSurfaceHost.kt` for an optional no-op `pumpEvents` contract only if necessary.

**Interfaces:** `LinuxSurfaceHost.attach(parentWindow: Long, viewport: AtomicReference<Rectangle>): LinuxSurfaceHost`; `initializeLinuxXlibThreading(): Unit` initializes Xlib before AWT startup on Linux; `backend` is Vulkan; existing `pixelSize`, `createSurface`, and `close` contracts remain. Add `isClosed: Boolean` internally for lifetime tests. Xlib operations use one dedicated owner executor; no AWT-private APIs. Expose no native library initialization in cross-platform top-level code.

- [ ] **Step 1: Verify the parent-handle facility before embedding.** In a Linux-only test, create/show a `ComposeWindow` on the EDT, retrieve `window.windowHandle`, and verify `xwininfo -id <handle>` identifies the same window. Compare client size/position with AWT bounds. If the handle is not an XID, investigate Skiko's supported facility; stop for design review if no reliable public route exists.
- [ ] **Step 2: Add red Linux host tests.** Gate on Linux and an explicit `DAWN_DESKTOP_TESTS=1` opt-in; the gated suite must assert `DISPLAY` exists rather than skip when the opt-in is set. Cross-platform unit tests cover null parent handle validation before loading Xlib. In-container tests exercise:

```kotlin
val viewport = AtomicReference(Rectangle(20, 30, 200, 150))
val host = LinuxSurfaceHost.attach(parentXid, viewport)
try {
    assertEquals(200 to 150, host.pixelSize())
    viewport.set(Rectangle(20, 30, 320, 240))
    assertEquals(320 to 240, host.pixelSize())
    viewport.set(Rectangle())
    assertEquals(0 to 0, host.pixelSize())
    viewport.set(Rectangle(20, 30, 200, 150))
    assertEquals(200 to 150, host.pixelSize())
} finally {
    host.close()
    host.close()
}
assertTrue(host.isClosed)
```

Verify child parentage/geometry/map state through `xwininfo -tree`/`-id`, not only cached Kotlin dimensions. Include parent disposal before attachment and while attached, requiring `CancellationException` or an actionable attachment failure, never fatal X11 termination. Add a test that repeatedly attaches/closes and verifies no owner threads survive.

- [ ] **Step 3: Implement narrowly scoped Xlib bindings.** Load `libX11.so.6` lazily. Bind `XOpenDisplay`, `XCloseDisplay`, `XCreateSimpleWindow`, `XMapWindow`, `XUnmapWindow`, `XMoveResizeWindow`, `XDestroyWindow`, `XSelectInput`, `XPending`, `XNextEvent`, `XFlush`, `XSync`, and `XSetErrorHandler` with verified LP64 layouts (`Window`/C unsigned long is 64-bit on both supported Linux architectures). Use a small C layout probe compiled by the container for `XEvent`, `XErrorEvent`, and `XWindowAttributes` before reading offsets. Never guess struct offsets from Java's pointer size alone.

```kotlin
val openDisplay = linker.downcallHandle(
    lookup.find("XOpenDisplay").orElseThrow(),
    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS))
val createSimpleWindow = linker.downcallHandle(
    lookup.find("XCreateSimpleWindow").orElseThrow(),
    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS,
        ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
        ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG))
```

Install a retained, non-throwing X error callback before issuing child operations. X error handlers are process-global: preserve/chaining the previous handler for other display connections, record errors for owned connections, then detect them after `XSync` on the owner thread. Do not throw through an upcall or suppress unrelated AWT errors. Retain callback memory for its entire installed lifetime. `BadWindow` for the disposed parent/child becomes cancellation, other errors include request/error codes. No Xlib call is made from the callback.

- [ ] **Step 4: Implement lifetime and geometry.** Open a dedicated display on the owner executor, create child with positive initial dimensions, select StructureNotify events, and map only when viewport is nonempty. Serialize create/resize/event/close work through that executor; synchronous calls propagate native failures. Copy `Rectangle` snapshots, clamp invisible geometry to a hidden child rather than passing negative sizes to Xlib, keep child above the Compose canvas, and sync before surface creation. Poll events with a bounded count and handle DestroyNotify as cancellation. The periodic pump is independent of GPU initialization so parent destruction is noticed even while adapter requests wait. Stop polling and executor on close, destroy only if still alive, then close the owned display.

```kotlin
override val backend: DawnBackend get() = DawnBackend.Vulkan
override fun createSurface(bridge: NativeBridge, deviceHandle: Long): DawnSurface =
    DawnSurface.createXlib(bridge, deviceHandle, displayAddress, childXid)
```

Document the display access assumption: application Xlib calls are single-owner, but Dawn/Vulkan may access the display for WSI from its worker. Implement `initializeLinuxXlibThreading()` as an idempotent call to `XInitThreads` before any AWT/Xlib use; check its nonzero return and fail explicitly otherwise. Call it before constructing any Compose window in Linux tests and from Main before entering the Compose application. Bind `XInitThreads` with `FunctionDescriptor.of(ValueLayout.JAVA_INT)`. Do not mutate AWT's connection. If process-global initialization cannot be safely arranged, stop and review instead of accepting races.

- [ ] **Step 5: Run targeted tests.** `DAWN_DESKTOP_TESTS=1 /opt/demo/demo.sh :dawn4k-demo:test --tests '*LinuxSurfaceHostTest'` inside the desktop, with missing-display test as a separate subprocess. Run cross-platform unit tests on Mac; they must not load Xlib. Repeat close/dispose tests to catch timing races and inspect process/thread termination.
- [ ] **Step 6: Commit.** `git commit -m "feat(demo): own the Linux X11 presentation viewport"` after native geometry and lifetime tests pass.

## Task 5: Integrate Linux platform selection and render format

**Files:** new `DemoPlatform.kt`, `DemoPlatformTest.kt`; modify `Main.kt`, `DemoApp.kt`, `dawn4k-demo/README.md`.

**Interfaces:** `enum class DemoPlatform { MacOS, Windows, Linux }`; `detectDemoPlatform(osName: String): DemoPlatform?`; `awaitLinuxSurfaceHost(window: ComposeWindow, viewport: AtomicReference<Rectangle>): LinuxSurfaceHost`. Host selection consumes Task 4; Linux configuration consumes Task 3 with `(adapter as DawnAdapter).nativeHandle()`.

- [ ] **Step 1: Add red detection tests.**

```kotlin
@Test fun recognizesSupportedPlatformsWithoutLoadingLibraries() {
    assertEquals(DemoPlatform.Linux, detectDemoPlatform("Linux"))
    assertEquals(DemoPlatform.MacOS, detectDemoPlatform("Mac OS X"))
    assertEquals(DemoPlatform.Windows, detectDemoPlatform("Windows 11"))
    assertEquals(null, detectDemoPlatform("FreeBSD"))
}
```

Run `./gradlew :dawn4k-demo:test --tests '*DemoPlatformTest'`; expect unresolved declarations.

- [ ] **Step 2: Implement pure detection and UI dispatch.**

```kotlin
internal fun detectDemoPlatform(osName: String): DemoPlatform? {
    val os = osName.lowercase()
    return when {
        os.contains("mac") -> DemoPlatform.MacOS
        os.startsWith("windows") -> DemoPlatform.Windows
        os.contains("linux") -> DemoPlatform.Linux
        else -> null
    }
}
```

`Main` reports unsupported systems explicitly. For Linux call `initializeLinuxXlibThreading()` before entering the Compose application. Show Linux title `Dawn/Vulkan`, use ordinary decorated AWT window, and reuse Windows side-by-side layout for all non-Mac supported platforms. Keep Mac transparent/undecorated/draggable behavior unchanged. Pass `DemoPlatform` instead of the old `windows: Boolean` to `runDemo`.

- [ ] **Step 3: Attach the Linux host with bounded waiting and cancellation cleanup.** Use the existing Windows host-attachment pattern: read showing/displayable/handle on `MainUIDispatcher`, retain `host` outside `withContext`, close it if cancellation discards the result, and return after a nonzero handle. Deadline is five seconds with 50ms retries. Missing `DISPLAY` fails immediately with a message naming the Docker launcher. Never block the EDT on Dawn setup.

```kotlin
val host: SurfaceHost = when (platform) {
    DemoPlatform.MacOS -> awaitMetalLayerHost(window)
    DemoPlatform.Windows -> awaitWindowsSurfaceHost(window, viewport)
    DemoPlatform.Linux -> awaitLinuxSurfaceHost(window, viewport)
}
```

- [ ] **Step 4: Negotiate format before scene creation and preserve resource nesting.** While the adapter is alive, obtain its raw handle through the existing public accessor and invoke `surface.configureForAdapter` for Linux only. Replace both `ParticleScene.create(... BGRA8Unorm ...)` sites with `surface.textureFormat`. Log backend, adapter info, native surface modes, and first-frame completion. Do not request a different backend silently when lavapipe initialization fails; expose the existing failure UI and logs.

```kotlin
println("[demo] adapter: ${adapter.info}")
if (platform == DemoPlatform.Linux) {
    surface.configureForAdapter((adapter as DawnAdapter).nativeHandle())
}
// Both initial and count-change scene construction:
// Initial:
ParticleScene.create(device, surface.textureFormat, initialParticles(controls.state.value.count))
// Count-change replacement:
ParticleScene.create(device, surface.textureFormat, initialParticles(requested.count))
```

- [ ] **Step 5: Verify unit and native smoke tests.** Run all demo tests on Mac. Inside Linux, run opted-in host tests and launch the demo via `/opt/demo/demo.sh`; verify logs contain Vulkan adapter identity, supported configuration, and `frame 1 rendered`. Do not treat an adapter request alone as presentation success. If lavapipe requires request-option changes, add a failing targeted test before changing them and remain on Vulkan.
- [ ] **Step 6: Commit.** `git commit -m "feat(demo): run the Compose particle demo on Linux Vulkan"` after the first frame is presented.

## Task 6: Exercise the real desktop, controls, and shutdown; finish user guide

**Files:** new `LinuxDemoWindowTest.kt`; complete `dawn4k-demo/README.md`; evidence in `dawn4k-demo/build/linux-desktop/`.

**Interfaces:** Opt-in integration tests use `DAWN_DESKTOP_TESTS=1`, fail if Linux display prerequisites are missing, and reuse the service/launcher names from Task 2. All retries have explicit deadlines, no infinite waits.

- [ ] **Step 1: Add the failing end-to-end test before extending behavior.** Base it on `WindowsDemoWindowTest.kt`, using Linux opt-in gating and 60-second GPU startup deadline (software driver), then 20-second interaction deadlines. Capture stdout with `try/finally`, restore it and dispose the window on the EDT even if creation fails. Assertions must cover:

```kotlin
await("first presented frame", controls) { trace.toString().contains("frame 1 rendered") }
controls.togglePause()
controls.selectCount(256)
await("count while paused", controls) {
    trace.toString().contains("particles=256, paused=true, delta=0")
}
controls.reset()
await("reset", controls) { trace.toString().contains("scene reset (256 particles)") }
val previous = trace.toString().lineSequence().count { it.contains("configuring surface") }
SwingUtilities.invokeAndWait { window.setSize(1100, 700) }
await("resize", controls) {
    trace.toString().lineSequence().count { it.contains("configuring surface") } > previous
}
controls.togglePause()
await("resume", controls) { trace.toString().contains("particles=256, paused=false") }
assertEquals(null, controls.state.value.error)
```

Add minimize/restore (or a zero-size viewport test if Sway prevents minimization) and close-during-startup cases. Assert host threads/processes do not remain after close. Do not assume a skipped Linux test is a pass for Linux support.

- [ ] **Step 2: Run the integrated suite and fix only demonstrated failures.** Execute inside the running desktop:

```bash
DAWN_DESKTOP_TESTS=1 /opt/demo/demo.sh :dawn4k-demo:test :dawn4k-native:jvmTest :dawn4k-native:verifyDawnAbi
```

Use systematic-debugging for any failure and maintain red/green evidence. On Mac run `./gradlew :dawn4k-demo:test`. Windows can be compilation/unit-gated here; report that actual Windows native execution is unavailable instead of claiming it was exercised.

- [ ] **Step 3: Validate visible UI, not just logs.** Launch the demo, open noVNC in the OpenChamber browser, capture visible controls and particles, and inspect the screenshot. Attempt a control click via browser-supported interaction; if the canvas cannot be driven, use container `xdotool` scoped to the demo XID after identifying the window with `xwininfo`. Verify a visible state change with another capture. Resize the real window, reset particles, pause/resume, change count, and close. Record commands, logs, screenshots, and any agent-browser limitations in a short report under build output. Simultaneous browser clients must share the same desktop.
- [ ] **Step 4: Test restart and source safety.** Stop/start Compose without `-v`, confirm the desktop returns and demo runs with existing caches. Compare host source permissions/content and host build output state with the pre-start checks from Task 2. Use a separate Compose project name for another checkout; confirm output volumes do not collide, while reporting that both projects cannot bind the same 6080 port simultaneously. Stop the service and verify no demo child processes survive. Do not delete volumes unless the user requests cache deletion.
- [ ] **Step 5: Write exact user instructions and limitations.** README includes prerequisites (Docker Desktop with ARM64 images, network access for pinned artifacts/SNAPSHOT Maven dependencies), build/start command, shared noVNC URL, demo launcher, terminal access, tests, logs, stop/rebuild commands, named volume behavior, local-only security, CPU rendering/performance limitations, XWayland versus native Wayland explanation, and the port-conflict workaround. Include troubleshooting for missing ICD, native dependency failure, HTTP ready but blank desktop, unavailable snapshots, and missing `DISPLAY` (use the launcher instead of a raw Gradle invocation).
- [ ] **Step 6: Final verification and commit.** Run `git diff --check`, Python supervisor/packaging tests, shell syntax tests, Compose config validation, current-host demo tests, and in-container suite above. Inspect staged changes and commit only implementation/docs files: `git commit -m "test(demo): verify Linux desktop presentation and document usage"`. Summarize actual evidence and limitations. No merge, push, PR, or volume deletion is authorized by this plan.

## Plan self-review

- [x] Spec coverage: packaging/ABI (1), desktop/security/caches/supervision (2), capabilities (3), X11 lifetime (4), integration/logging (5), visual controls/shutdown/docs (6).
- [x] Placeholder scan: no unspecified replacement versions, unowned interfaces, or deferred product requirements.
- [x] Interface consistency: one platform enum; Xlib factory uses display pointer and XID; surface format negotiated before ParticleScene; existing adapter accessor supplies capabilities.
- [x] Review Focus: each listed condition has tests owned by its corresponding task.
- [ ] User reviews plan and selects Native or Subagent-driven execution.
