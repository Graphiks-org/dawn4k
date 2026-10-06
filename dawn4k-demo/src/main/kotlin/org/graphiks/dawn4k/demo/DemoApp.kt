package org.graphiks.dawn4k.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.ComposeWindow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.NativeBridge
import org.graphiks.dawn4k.native.wgpuTextureCreateView
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.suite.demos.particles.ParticleScene
import org.graphiks.webgpu.suite.demos.particles.initialParticles
import org.graphiks.webgpu.suite.demos.particles.maxParticleCount
import org.jetbrains.skiko.MainUIDispatcher

@Composable
internal fun DemoApp(
    window: ComposeWindow,
    controls: ParticleControls = remember { ParticleControls() },
    onClose: () -> Unit = { window.dispose() },
) {
    ParticleControlPanel(controls, onClose)

    // LaunchedEffect drives the GPU setup + render loop off the UI thread's
    // composition, but the actual native calls go through the bridge (worker).
    LaunchedEffect(window, controls) {
        try {
            withContext(Dispatchers.Default) {
                runDemo(window, controls)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            failure.printStackTrace()
            controls.fail(failure.message ?: failure.toString())
        }
    }
}

/**
 * Runs the demo: opens a Dawn context, creates a device, extracts the
 * CAMetalLayer from the Compose window, creates the surface, builds the
 * particle scene, and renders frames until the coroutine is cancelled.
 */
private suspend fun runDemo(window: ComposeWindow, controls: ParticleControls) {
    val host = awaitMetalLayerHost(window)
    host.use {
        println("[demo] creating Dawn context (Metal backend)")
        DawnContext.create(DawnConfig(backend = DawnBackend.Metal)).use { context ->
            println("[demo] requesting adapter")
            context.requestAdapter().getOrThrow().use { adapter ->
                println("[demo] requesting device")
                (adapter.requestDevice().getOrThrow() as DawnDevice).use { device ->
                    val bridge = context.nativeBridge()
                    println("[demo] CAMetalLayer ptr=0x${host.layerPtr.toString(16)}")
                    MetalSurface.create(bridge, device.nativeHandle(), host.layerPtr).use { surface ->
                        controls.initialize(maxParticleCount(device.limits))
                        renderLoop(device, surface, host, bridge, controls)
                    }
                }
            }
        }
    }
}

/**
 * The render loop: acquires a frame, encodes the scene, submits, presents.
 * The delta is clamped to 0.05s (the scene's MaximumDeltaSeconds contract).
 */
private suspend fun renderLoop(
    device: DawnDevice,
    surface: MetalSurface,
    host: MetalLayerHost,
    bridge: NativeBridge,
    controls: ParticleControls,
) {
    var scene = ParticleScene.create(
        device, GPUTextureFormat.BGRA8Unorm, initialParticles(controls.state.value.count)
    )
    println("[demo] scene ready — starting render loop")
    val clock = ParticleFrameClock()
    var resetGeneration = controls.state.value.resetGeneration
    var frameCount = 0L
    try {
        while (coroutineContext.isActive) {
            val requested = controls.state.value
            if (requested.count != scene.count) {
                // No old scene resource is released while a submitted frame still uses it.
                device.queue.onSubmittedWorkDone().getOrThrow()
                val replacement = ParticleScene.create(
                    device, GPUTextureFormat.BGRA8Unorm, initialParticles(requested.count)
                )
                val previous = scene
                scene = replacement
                previous.close()
                println("[demo] scene changed (${scene.count} particles)")
            }
            if (requested.resetGeneration != resetGeneration) {
                scene.reset(initialParticles(scene.count))
                resetGeneration = requested.resetGeneration
                println("[demo] scene reset (${scene.count} particles)")
            }
            val delta = clock.advance(requested, System.nanoTime())

            val (width, height) = host.pixelSize()
            if (width <= 0 || height <= 0) {
                delay(16)
                continue
            }
            if (width != surface.width || height != surface.height) {
                println("[demo] configuring surface ${width}x${height}")
                surface.configure(width, height)
            }

            try {
                // The Dawn backend's render pass requires a DawnTextureView (internal
                // constructor). We create one via reflection over the borrowed texture's
                // WGPUTextureView handle. The DawnTextureView owns the view reference
                // and releases it on close().
                surface.acquireFrame().use { texture ->
                    bridge.call { createDawnTextureView(device, texture) }.use { view ->
                        device.createCommandEncoder().use { encoder ->
                            scene.encodeFrame(encoder, view, width, height, delta)
                            encoder.finish().use { commandBuffer ->
                                device.queue.submit(listOf(commandBuffer))
                            }
                        }
                    }
                    surface.present(texture)
                }
                frameCount++
                if (frameCount == 1L || frameCount % 120L == 0L) {
                    println("[demo] frame $frameCount rendered (${width}x${height}, particles=${scene.count}, paused=${requested.paused}, delta=${"%.3f".format(delta)}s)")
                }
            } catch (outdated: SurfaceOutdatedException) {
                // Reconfigure on the next iteration (the window was resized).
                println("[demo] surface outdated — reconfiguring ${width}x${height}")
                surface.configure(width, height)
            }
            delay(1) // let the Fifo present mode regulate the frame rate
        }
    } finally {
        scene.close()
    }
    println("[demo] render loop ended after $frameCount frames")
}

/**
 * Creates a `DawnTextureView` over the borrowed surface texture's handle.
 *
 * The Dawn backend's render pass encoder requires `DawnTextureView` instances
 * (its `requireDawnTextureView` does a type check). `DawnTextureView`'s
 * constructor is `internal` to `:dawn4k`, so we use reflection to:
 * 1. Read `DawnDevice.session` (internal property) to get the `DeviceSession`.
 * 2. Call `wgpuTextureCreateView` on the borrowed texture's `WGPUTexture` handle.
 * 3. Invoke the `DawnTextureView` constructor with the session and view handle.
 *
 * The returned `DawnTextureView` owns the view reference: `close()` releases it
 * via the session's resource registry.
 */
private fun createDawnTextureView(
    device: DawnDevice,
    texture: BorrowedSurfaceTexture,
): GPUTextureView {
    // 1. Get the DeviceSession from DawnDevice via reflection (internal property).
    val session = device.javaClass.getDeclaredField("session").apply {
        isAccessible = true
    }.get(device) ?: throw IllegalStateException("could not read DawnDevice.session")

    // 2. Create the WGPUTextureView from the borrowed texture's handle.
    val viewHandle = wgpuTextureCreateView(texture.handle, null)
        ?: throw IllegalStateException("wgpuTextureCreateView returned no view")

    // 3. Create a DawnTextureView via reflection (internal constructor).
    // WGPUTextureView is a @JvmInline value class wrapping NativeAddress (Long),
    // so at the JVM level the constructor takes (DeviceSession, long, String).
    val dawnTextureViewClass = Class.forName("org.graphiks.dawn4k.DawnTextureView")
    val deviceSessionClass = Class.forName("org.graphiks.dawn4k.internal.DeviceSession")
    val constructor = dawnTextureViewClass.getDeclaredConstructor(
        deviceSessionClass,
        Long::class.javaPrimitiveType,
        String::class.java,
    ).apply { isAccessible = true }

    // viewHandle.handler.rawValue gives the raw Long (value class erasure).
    val rawHandle = viewHandle.handler.rawValue
    return constructor.newInstance(session, rawHandle, "surface-view") as GPUTextureView
}

/** Keep handle lookup and native retention in one EDT operation, excluding disposal. */
@OptIn(org.graphiks.kffi.objc.PlatformAvailability::class)
internal suspend fun awaitMetalLayerHost(window: ComposeWindow): MetalLayerHost {
    val deadline = System.nanoTime() + 5_000_000_000L
    while (System.nanoTime() < deadline) {
        var host: MetalLayerHost? = null
        try {
            withContext(MainUIDispatcher) {
                if (!window.isDisplayable) throw CancellationException("the Compose window was closed")
                if (window.isShowing) {
                    val handle = window.windowHandle
                    if (handle != 0L) host = onAppKitThread {
                        val view = org.graphiks.kffi.objc.NSWindow(
                            java.lang.foreign.MemorySegment.ofAddress(handle)
                        ).contentView()
                        MetalLayerHost.attach(view.address())
                    }
                }
            }
        } catch (failure: Throwable) {
            // withContext may discard its result on cancellation after native attachment.
            host?.close()
            throw failure
        }
        host?.let { return it }
        delay(50)
    }
    error("timed out waiting for the Compose window's native handle")
}
