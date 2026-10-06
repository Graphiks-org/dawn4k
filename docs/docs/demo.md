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

## How it works

1. **Window**: Compose Desktop creates the window. Its public `windowHandle`
   provides the `NSWindow` pointer; `kffi-objc` retrieves its content `NSView`.
2. **Metal layer**: a dedicated `CAMetalLayer` is installed above Compose's
   children without replacing its backing layer. AppKit work uses a common-mode
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
and early-close cancellation; these tests require a graphical session. Visible
particles and uninterrupted animation during dragging are also checked manually.
