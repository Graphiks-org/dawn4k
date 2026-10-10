# Demo: ParticleScene in a desktop window

The `:dawn4k-demo` module is a macOS and Windows x64 desktop application that opens a real
window and renders the `ParticleScene` from
[`suite-demos`](https://github.com/Graphiks-org/WebGPU/tree/master/suite-demos)
through the `:dawn4k` backend (Dawn/Metal on macOS, Dawn/D3D12 on Windows).

## Run

The demo owns its event-progression coroutine, on its consumer dispatcher.
It starts before adapter/device discovery and stays alive through idle/pause,
surface replacement, readback and `NonCancellable` GPU-awaiting cleanup.
It is joined before synchronous device/adapter closure; pending results are
drained first, then the resulting teardown is explicitly progressed before
context close.
Shared-device synchronization is explicitly opted into with
`DawnConfig(implicitDeviceSynchronization = true)`; dawn4k itself creates no
worker or scheduling job. See [the migration guide](getting-started.md).

```bash
./gradlew :dawn4k-demo:run
```

On Windows, use PowerShell with JDK 25 configured in `JAVA_HOME`:

```powershell
.\gradlew.bat :dawn4k-demo:run
```

Gradle downloads and verifies the pinned Dawn `mingwX64` archive and configures
the external DLL search path, including its bundled MSVC runtime dependencies.
Windows' `System32/d3dcompiler_47.dll` is preloaded before adapter discovery.
A graphical session and a D3D12-compatible GPU are required.

A window opens with animated particles.

The floating Compose panel provides **Pause/Resume**, **Reset** and particle
count choices (256, 1024, 4096, 16384, 65536, limited by the GPU). Reset restores
the initial positions and velocities, even while paused. Changing the count
recreates only the scene, not the device or surface. Pause freezes the simulation
but keeps rendering so window resizing still works.

On Windows, Compose controls sit to the left of an opaque Win32 presentation
area. The window uses its standard system title bar and borders.

On macOS, Compose draws transparently above Metal. Its native transparency API requires an
undecorated window: drag the title strip to move it, resize from the edges and use
**Close** to close it. Controls are disabled until initialization completes or
after a fatal error; the diagnostic remains visible in the panel.

## How it works

1. **Window**: Compose Desktop creates the window. Its public `windowHandle`
   provides the `NSWindow` pointer; `kffi-objc` retrieves its content `NSView`.
2. **Metal layer**: a dedicated `CAMetalLayer` is installed below transparent
   Compose children without replacing their backing layer. AppKit work uses a common-mode
   run-loop source so dragging and live resize do not stall rendering. The
   Objective-C bridge uses JVM FFM, with no JNA or native compilation.
3. **Surface**: a Dawn `WGPUSurface` is created over the `CAMetalLayer`
   (`WGPUSurfaceSourceMetalLayer`), configured for `BGRA8Unorm` / `Fifo` and
   resized to the window's physical pixel dimensions, including Retina scaling.
4. **Render loop**: each frame acquires the surface texture, encodes the
   `ParticleScene` (compute pass + render pass), submits, and presents.

On Windows, `windowHandle` provides the parent HWND. The demo owns a separate
Win32 child HWND created on the AWT EDT, and creates a Dawn surface using
`WGPUSurfaceSourceWindowsHWND` and the process HINSTANCE. Compose layout and
`GetClientRect` provide physical pixel dimensions. A Swing timer drains the
child's Win32 message queue on the EDT, including during GPU initialization and
waits. Win32 calls use JVM FFM;
the Dawn render loop and acquired-texture lifecycle are shared with macOS.

To build a standalone distribution with its Windows DLLs and launcher:

```powershell
.\gradlew.bat :dawn4k-demo:installDist
.\dawn4k-demo\build\install\dawn4k-demo\bin\dawn4k-demo.bat
```

The surface lives entirely inside the demo module: `:dawn4k` only exposes
three minimal "platform integrator" accessors (`DawnContext.nativeBridge()`,
`DawnDevice.nativeHandle()`, `DawnAdapter.nativeHandle()`) — no public
`GPUSurface` API.

## Tests

```bash
./gradlew :dawn4k-demo:test
```

On Windows: `.\gradlew.bat :dawn4k-demo:test`. The integration test opens a real
window and checks presentation, pause/resume, reset, count changes and resizing.
A separate host test checks posted-message processing without a GPU render loop.

On macOS, the tests verify layer preservation, AppKit dispatch during mouse
tracking, resizing and resource cleanup. An integration test opens a real
Compose window and checks successful frame presentation, physical pixel sizes
and early-close cancellation, plus live pause/reset/count changes; these tests require a graphical session. Visible
particles and uninterrupted animation during dragging are also checked manually.
