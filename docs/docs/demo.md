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

1. **Window**: Compose Desktop creates the window. The demo reaches the
   underlying `SkiaLayer` (via Compose's public `ComposeContainer`) and reads
   the native `NSView` handle.
2. **Metal layer**: the `NSView`'s `CAMetalLayer` is retrieved (or created and
   installed) through `kffi-objc` — a pure-JVM Objective-C bridge over FFM, no
   JNA, no native compilation.
3. **Surface**: a Dawn `WGPUSurface` is created over the `CAMetalLayer`
   (`WGPUSurfaceSourceMetalLayer`), configured for `BGRA8Unorm` / `Fifo`.
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

The tests are headless (no window): they verify the ObjC bridge (NSView →
CAMetalLayer) and the borrowed-texture wrappers. The full window render loop
is validated manually on macOS.
