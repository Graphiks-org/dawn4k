# Getting Started — dawn4k

How to work on **dawn4k**, the Kotlin Multiplatform binding to Google's Dawn
(WebGPU) C API for desktop (JVM, macOS ARM64, Linux x64).

## Prerequisites

- JDK 25 and the repository Gradle wrapper (`9.8.0`).
- A macOS ARM64 or Linux x64 host.
- A C toolchain (`cc`) for the ABI oracle and the callback helper.
- Optional: LLVM (`brew --prefix llvm`) if you need to rebuild the pinned
  kextract generator.

## Modules

- **`:dawn4k-native`** — generated low-level Dawn bindings (`org.graphiks.dawn4k.native`),
  the C ABI oracle and the cinterop/linkage configuration. Depends only on kffi.
See [Native Dawn binding](native-binding.md) for generation and linkage.

## Common commands

```bash
./gradlew :dawn4k-native:jvmTest        # fast JVM tests
./gradlew allTests                      # host targets
./gradlew :dawn4k-native:compileAndroidMain  # Android/JVM target compiles
./gradlew :dawn4k-native:compileKotlinIosArm64  # iOS target compiles
```

## Managed sessions and presentation

For application code that wants the toolkit to own bootstrap, one GPU thread,
progression and shutdown, add `org.graphiks:dawn4k-toolkit-core` at the same
release version as the backend. Add `dawn4k-toolkit-surface` for an existing native
view or `dawn4k-toolkit-glfw` for toolkit-owned desktop windows. Use
`withDawnSession { ... }` inside a coroutine instead of writing a backend pump.
See [Dawn4k toolkit](toolkit.md) for complete runnable compute/triangle examples,
Android/Apple hosts, manual progression and timeout/retirement rules. The lower
level backend example below remains useful for caller-owned integration.

## Using the WebGPU backend

`:dawn4k` implements `org.graphiks.webgpu`. It does not schedule native work or
pump callbacks for you. A complete minimal bootstrap (success path) is:

```kotlin
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnContext
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUDevice

suspend fun useGpu() = coroutineScope {
    DawnContext.create(DawnConfig(implicitDeviceSynchronization = true)).use { context ->
        val events = launch {
            while (isActive) { context.processEvents(); delay(1) }
        }
        var adapter: GPUAdapter? = null
        var device: GPUDevice? = null
        try {
            val acquiredAdapter = context.requestAdapter().getOrThrow()
            adapter = acquiredAdapter
            device = acquiredAdapter.requestDevice().getOrThrow()
            // Application-owned encoding threads and queue ordering.
        } finally {
            withContext(NonCancellable) {
                events.cancelAndJoin()
                withTimeout(10_000) {
                    // Settle cancelled requests before closing their owners.
                    while (context.hasPendingOperations()) {
                        context.processEvents()
                        delay(1)
                    }
                    try { device?.close() } finally { adapter?.close() }
                    while (context.hasPendingOperations()) {
                        context.processEvents()
                        delay(1)
                    }
                }
            }
        }
    }
}
```

The feature is opt-in (default `false`); enable it when a shared device is called
concurrently. Otherwise externally synchronize non-encoding calls and device
event progression. Use independent encoders, join encoding tasks before submitting
their command buffers, and never race resource closure or mapping against users.

After cancellation, stop/join producers and waiters, keep progression alive during
any `NonCancellable` GPU-awaiting cleanup, then stop/join the event job **before
closing device/adapter children**. Drain late results first and device teardown
after child closure, under an application timeout. A timeout is **not** permission to free
active handles: context closure refuses pending operations or open children.
This sample is not a complete recovery strategy for a stuck driver.

Use `processEvents()` to progress callbacks explicitly; no await method pumps
events. Platform integrators use `NativeBridge.instanceHandle()` and issue native
calls directly, with application-owned synchronization and lifetime. Error-scope push, validated calls
and pop issuance must retain one **OS thread**, not merely serial coroutine
execution. See [Architecture](architecture.md).

## Resource ownership

Resources belong to their caller, not to the device session. Use `use` or
`try/finally` and close them before their device/context. For example, with the
`webgpu-descriptors` dependency:

```kotlin
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor

device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopyDst)).use { buffer ->
    // Use buffer while it is open; join users before leaving this scope.
}
```

Close is idempotent and releases the acquired native reference immediately.
Buffers, textures and query sets are destroyed first. Copied metadata remains
readable; operations on closed handles are rejected. Device close does not clean
up forgotten resource references, and no GC fallback does it. The queue remains
borrowed from its device and has no independent close.

Stop/join users and settle pending callbacks before resource closure. Resource
guards do not make racing use/close safe. For an external-interface render bundle,
use `(bundle as org.graphiks.dawn4k.DawnRenderBundle).close()`. Pass `end()` ends
and releases the pass; the concrete Dawn pass wrappers additionally implement
`AutoCloseable` for exception cleanup without implicitly issuing end.

## Regenerating the bindings

```bash
bash scripts/generate-dawn-bindings.sh
```

The script builds the pinned kextract in a dedicated, git-ignored checkout and
regenerates into `dawn4k-native/build/regenerated/src`. Review that diff, then
promote it into the versioned `dawn4k-native/generated/src`. Ordinary compilation
never runs the generator.

## Customizing the build

- `settings.gradle.kts` — `rootProject.name`.
- `build.gradle.kts` — `group` and version.
- `buildSrc/.../kmp-library.gradle.kts` — desktop targets and toolchain.
- `buildSrc/.../kmp-publish.gradle.kts` — publication coordinates and POM fields.
- `buildSrc/.../kmp-dokka.gradle.kts` — Dokka module name and source link.
- `docs/mkdocs.yml` — site metadata.
- `LICENSE` — copyright holder.

## Final verification

- [ ] `./gradlew allTests` succeeds.
- [ ] `mkdocs build -f docs/mkdocs.yml` works.
