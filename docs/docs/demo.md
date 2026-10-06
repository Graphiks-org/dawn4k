# Demo: ParticleScene in a desktop window

The `:dawn4k-demo` module is a macOS desktop application that opens a real
window and renders the `ParticleScene` from
[`suite-demos`](https://github.com/Graphiks-org/WebGPU/tree/master/suite-demos)
through the `:dawn4k` backend (Dawn/Metal).

## Run

```bash
./gradlew :dawn4k-demo:run
```

A window opens with animated particles. macOS only (the Metal surface and the
Objective-C bridge are macOS-specific).

The floating Compose panel provides **Pause/Resume**, **Reset** and particle
count choices (256, 1024, 4096, 16384, 65536, limited by the GPU). Reset restores
the initial positions and velocities, even while paused. Changing the count
recreates only the scene, not the device or surface. Pause freezes the simulation
but keeps rendering so window resizing still works.

Compose draws transparently above Metal. Its native transparency API requires an
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

The surface lives entirely inside the demo module: `:dawn4k` only exposes
three minimal "platform integrator" accessors (`DawnContext.nativeBridge()`,
`DawnDevice.nativeHandle()`, `DawnAdapter.nativeHandle()`) — no public
`GPUSurface` API.

## Tests

```bash
./gradlew :dawn4k-demo:test
```

On macOS, the tests verify layer preservation, AppKit dispatch during mouse
tracking, resizing and resource cleanup. An integration test opens a real
Compose window and checks successful frame presentation, physical pixel sizes
and early-close cancellation, plus live pause/reset/count changes; these tests require a graphical session. Visible
particles and uninterrupted animation during dragging are also checked manually.
