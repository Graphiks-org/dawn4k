# Dawn4k toolkit

The toolkit adds owned GPU sessions and presentation to the existing Dawn backend.
It lives in this repository; application buffers, textures and pipelines still use
normal WebGPU calls.

| Dependency | Responsibility | Kotlin targets |
| --- | --- | --- |
| `org.graphiks:dawn4k-toolkit-core` | Headless sessions, owner thread, progress, shutdown | JVM, Android, macOS ARM64, Linux x64, iOS ARM64, iOS ARM64 simulator, iOS x64 simulator, tvOS ARM64, tvOS ARM64 simulator |
| `org.graphiks:dawn4k-toolkit-surface` | External targets, supplied mobile views, frames | Same nine targets |
| `org.graphiks:dawn4k-toolkit-glfw` | Desktop windows and main loop | JVM, macOS ARM64, Linux x64 |

Add the smallest dependency that provides the host you need. Surface depends on
core; GLFW depends on surface. Use the same release version as `dawn4k`. The
unpublished `dawn4k-toolkit-examples` module contains the complete runnable code.
Core has no GLFW, AWT, Compose or UI dependency.

## Compute without a window

```kotlin
withDawnSession { gpu ->
    val result = runComputeExample(gpu) // examples module: [2, 4, 6, 8]
    println(result.contentToString())
}
```

The scoped block already runs on the session's GPU owner. Persistent applications
use `openDawnSession()`, call `gpu.run { ... }` for their resource lifetime, and
invoke `gpu.shutdown()` after that block ends. `context`, `adapter`, `device` and
`queue` are borrowed handles: do not close them independently.

## Offscreen textures and readback

`gpu.createRenderTexture(width, height, format)` creates an application-owned
texture with render-attachment and copy-source usage. Call it inside `gpu.run`.
Use `texture.createView().use { ... }`, render with ordinary WebGPU commands, then
copy into a `CopyDst | MapRead` staging buffer. Texture-to-buffer copies require
aligned rows (256 bytes in the example). Await `mapAsync`, copy mapped bytes into
a Kotlin array, and unmap before closing the staging buffer. No mapped memory
view may escape `unmap`. The examples' pixel test exercises this full path.

Resizing an offscreen image creates a replacement after its current users finish.
Close application resources before leaving the session block. A session does not
track arbitrary resources created through `device`.

## Desktop window

`withDawnWindow { gpu, window, presentation -> ... }` starts a process-main-thread
GLFW loop, creates a compatible GPU and owns its window. Call it from
`fun main() = runBlocking { ... }`, with `-XstartOnFirstThread` on macOS JVM. Do not wrap it in a dispatcher
that moves process-main-thread work to a worker. JVM requires Java 25 and
`--enable-native-access=ALL-UNNAMED`.

```kotlin
withDawnWindow { gpu, window, presentation ->
    TriangleRenderer(gpu, presentation.configuration.value!!.format).use { triangle ->
        presentation.withFrame { frame ->
            triangle.render(frame.view, frame.extent)
            frame.present()
        }
        gpu.queue.onSubmittedWorkDone().getOrThrow()
    }
}
```

The full launcher repeats frames and observes `DawnWindowEvent.CloseRequested`.
A frame owns its view and acquired texture reference. Present at most once; do
not cache or close that view yourself. Failure or cancellation abandons the frame
and releases it. A zero-sized or unavailable target suspends presentation; a
surface timeout skips the frame. `window.resize(w, h)` requests logical content
coordinates, whereas `window.extent` and frame extents are physical pixels.

The persistent form is `DawnMainThreadLoop.run { main ->
openDawnWindow(mainThread = main) }`. Keep that loop alive until window shutdown
finishes. All concurrent toolkit windows must use the same `Owned` or `Borrowed`
GLFW runtime mode. Borrowed mode never terminates the caller's runtime.

## Existing native window or view

Use `DawnSurfaceTargets.metalLayer`, `.win32`, `.x11` or `.wayland`, supplying its
UI/platform owner, `StateFlow<DawnTargetState>` and native retention hooks. Typed
platform overloads are available where the platform types exist. The retention
hooks must retain real handles and acknowledge release; returning from `retire`
means Dawn no longer references that target. Make custom retirement hooks safe
to retry after an interrupted attempt.

`withDawnSurface(target) { gpu, presentation -> ... }` selects an adapter compatible
with that exact retained surface. The toolkit owns its Dawn surface and retained
lease, while the application owns the external host. `replaceTarget` keeps the
GPU when compatible. Its optional new presentation policy can require the
existing pipeline format. A failed replacement preserves the previous policy.
`detachTarget(generation)` ignores obsolete generation requests.

Publish dimensions in physical pixels and a fresh generation when the native
handle changes. Zero dimensions and `available = false` prevent acquisition.
Stop users and await target retirement before destroying caller-owned native
handles. Native UI calls stay on the supplied UI owner; GPU encoding stays in
`gpu.run`. `gpu.executionOwner` is borrowed and intended for advanced platform
integrations with their own admission protocol.

## Android, iOS and tvOS

Android provides `DawnSurfaceView`; iOS and tvOS provide `DawnMetalView`. Their
`target` publishes attachment generations, availability and physical dimensions.
Attach it with the same surface factory. Keep the rendering job and its resource
cleanup inside the GPU scope. When the view disappears, cancel/join rendering
asynchronously and `awaitRetirement()` before completing host destruction.
Never block the UI thread waiting for a GPU owner that needs that UI thread for
native release. Apple layer retention uses actual Core Foundation references;
Android retention uses a native-window reference independent of the Java wrapper.
The sample hosts demonstrate asynchronous view teardown.

## Progress, loss and shutdown

Managed execution owns one OS-thread-pinned dispatcher and event progression.
A caller-owned pinned dispatcher can be borrowed with `DawnExecutionOwner.borrow`.
`limitedParallelism(1)` alone does not pin an OS thread. Borrowed owners remain
caller-owned and must outlive native cleanup.

For manual progression, supply `DawnExecutionMode.Manual(owner)`. Bootstrap pumps
until ownership transfers to `gpu.manualProgress`. Call `processEvents()` only
on that same owner, from exactly one driver, yielding between bounded iterations.
Run that driver alongside suspended GPU work. Cancel/join the driver, call
`manualProgress.stop()`, then invoke shutdown outside `gpu.run`. A persistent
shutdown rejects an active manual driver; scoped helpers revoke it on scope exit.

Observe `gpu.state` or `awaitLost()`. Device loss and out-of-memory stop new
admissions and trigger cleanup; buffers and pipelines are not replayed. Cancel
and join application producers and close their resources inside the admitted GPU
scope, then create a new session deliberately if the application wants to restart.

Startup and shutdown attempt timeouts default to ten seconds. Startup covers
retention and capability negotiation, with rollback ownership retained even if
the caller cancels. A failed startup may additionally wait up to `shutdownTimeout`
for rollback. Native calls cannot be forcibly preempted. A shutdown timeout never
authorizes freeing handles that remain referenced: cleanup retries in the
background on the retained owner, and the application may retry `shutdown()`.
Primary errors keep cleanup errors as suppressed exceptions. Successful shutdown
is idempotent; shutdown inside its own `gpu.run` is rejected.

Each persistent window shutdown attempt is finite while its main loop runs. A
scoped GLFW main loop stays runnable until every full or partial window lease
retires, so its enclosing call can outlast an individual attempt's deadline.

## Run the examples

```bash
./gradlew :dawn4k-toolkit-examples:runCompute
./gradlew :dawn4k-toolkit-examples:runTriangle
# Finite smoke run for an actual graphical host:
./gradlew :dawn4k-toolkit-examples:runTriangle -Ptoolkit.example.frames=60
./gradlew :dawn4k-toolkit-examples:runComputeDebugExecutableMacosArm64
./gradlew :dawn4k-toolkit-examples:runTriangleDebugExecutableMacosArm64
./gradlew :dawn4k-toolkit-examples-androidApp:installDebug
```

Native Linux uses the corresponding `LinuxX64` executable tasks. Android's sample
runs compute and displays the triangle. Open the examples' Apple Xcode project
and select the `ToolkitIOS` or `ToolkitTVOS` scheme; the shared Kotlin framework
is built by its build phase. The examples use public toolkit/backend APIs.

The Android sample requires API 28 or newer, matching the descriptor dependency.

## Validation coverage

Supported variants and executed GPU validation are different claims. Local proof
for this change (2026-10-15) includes macOS ARM64/JVM Metal, Linux x64 under
emulation with Vulkan llvmpipe and real X11/Wayland presentation, Android API 35
ARM64 instrumentation, and booted ARM64 iOS/tvOS simulators with Metal. The
particle demo also passed actual Android/iOS app control and restart scenarios.
Apple ARM64 device variants and the Intel iOS simulator were compiled locally;
that is not physical-device or Intel-simulator GPU execution. Windows JVM,
Linux ARM64 JVM and hardware-only device execution need their matching host
reports. Required GPU jobs must fail on missing adapters or skipped acceptance
suites; compilation never substitutes for execution.
