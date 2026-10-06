package org.graphiks.dawn4k.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.native.wgpuTextureCreateView
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.suite.demos.particles.ParticleScene
import org.graphiks.webgpu.suite.demos.particles.initialParticles
import org.graphiks.webgpu.suite.demos.particles.maxParticleCount
import org.jetbrains.skiko.SkiaLayer

private const val ParticleCount = 4096

@Composable
fun DemoApp() {
    // The Compose content is empty: the whole window is the WebGPU surface.
    // We only need the SkiaLayer to extract the NSView → CAMetalLayer.
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (errorMessage != null) {
        // No material3/material on the classpath: log to stderr and show an empty canvas.
        System.err.println("dawn4k-demo error: $errorMessage")
    }

    // LaunchedEffect drives the GPU setup + render loop off the UI thread's
    // composition, but the actual native calls go through the bridge (worker).
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.Default) {
                runDemo()
            }
        } catch (failure: Throwable) {
            errorMessage = failure.message ?: failure.toString()
        }
    }

    // Empty composable: reserves the window. The SkiaLayer is reached via
    // reflection on the ComposeWindow (see extractMetalLayerPtr).
    Canvas(Modifier.fillMaxSize()) {}
}

/**
 * Runs the demo: opens a Dawn context, creates a device, extracts the
 * CAMetalLayer from the Compose window, creates the surface, builds the
 * particle scene, and renders frames until the coroutine is cancelled.
 */
private suspend fun runDemo() {
    println("[demo] creating Dawn context (Metal backend)")
    val context = DawnContext.create(DawnConfig(backend = DawnBackend.Metal))
    try {
        println("[demo] requesting adapter")
        val adapter = context.requestAdapter().getOrThrow()
        try {
            println("[demo] requesting device")
            val device = adapter.requestDevice().getOrThrow() as DawnDevice
            try {
                val bridge = context.nativeBridge()
                println("[demo] extracting CAMetalLayer from the Compose window")
                val metalLayerPtr = extractMetalLayerPtr()
                    ?: throw IllegalStateException("could not retrieve the CAMetalLayer from the Compose window")
                println("[demo] CAMetalLayer ptr=0x${metalLayerPtr.toString(16)}")
                val surface = MetalSurface.create(bridge, device.nativeHandle(), metalLayerPtr)
                try {
                    val limits = device.limits
                    val count = minOf(ParticleCount, maxParticleCount(limits))
                    println("[demo] creating ParticleScene ($count particles)")
                    val scene = ParticleScene.create(device, GPUTextureFormat.BGRA8Unorm, initialParticles(count))
                    try {
                        println("[demo] scene ready — starting render loop")
                        renderLoop(device, surface, scene)
                    } finally {
                        scene.close()
                    }
                } finally {
                    surface.close()
                }
            } finally {
                device.close()
            }
        } finally {
            adapter.close()
        }
    } finally {
        context.close()
    }
}

/**
 * The render loop: acquires a frame, encodes the scene, submits, presents.
 * The delta is clamped to 0.05s (the scene's MaximumDeltaSeconds contract).
 */
private suspend fun renderLoop(
    device: DawnDevice,
    surface: MetalSurface,
    scene: ParticleScene,
) {
    var lastFrame = System.nanoTime()
    var frameCount = 0L
    while (coroutineContext.isActive) {
        val now = System.nanoTime()
        val delta = ((now - lastFrame) / 1e9).toFloat().coerceIn(0f, 0.05f)
        lastFrame = now

        val width = surface.width
        val height = surface.height
        if (width <= 0 || height <= 0) {
            // Not configured yet (first frame): configure with a default size.
            println("[demo] configuring surface ${width}x${height} → 800x600")
            surface.configure(800, 600)
            continue
        }

        try {
            val texture = surface.acquireFrame()
            // The Dawn backend's render pass requires a DawnTextureView (internal
            // constructor). We create one via reflection over the borrowed texture's
            // WGPUTextureView handle. The DawnTextureView owns the view reference
            // and releases it on close().
            val view = createDawnTextureView(device, texture)
            try {
                val encoder = device.createCommandEncoder()
                try {
                    scene.encodeFrame(encoder, view, width, height, delta)
                    val commandBuffer = encoder.finish()
                    try {
                        device.queue.submit(listOf(commandBuffer))
                    } finally {
                        commandBuffer.close()
                    }
                } finally {
                    encoder.close()
                }
            } finally {
                view.close()
            }
            surface.present(texture)
            frameCount++
            if (frameCount == 1L || frameCount % 120L == 0L) {
                println("[demo] frame $frameCount rendered (${width}x${height}, delta=${"%.3f".format(delta)}s)")
            }
        } catch (outdated: SurfaceOutdatedException) {
            // Reconfigure on the next iteration (the window was resized).
            println("[demo] surface outdated — reconfiguring ${width}x${height}")
            surface.configure(width, height)
        }
        delay(1) // let the Fifo present mode regulate the frame rate
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

/**
 * Extracts the `CAMetalLayer*` pointer from the current Compose window.
 *
 * The chain is: ComposeWindow (JFrame) → composePanel (private) →
 * ComposeWindowPanel → _composeContainer (private) → ComposeContainer →
 * contentComponent (public) → SkiaLayer → contentHandle (public).
 *
 * On macOS, `SkiaLayer.contentHandle` returns the `NSWindow*` (not the NSView*).
 * We get the content view via `-[NSWindow contentView]` and pass that NSView*
 * to [MetalSurface.metalLayerOf].
 *
 * The private hops use reflection; if Compose changes its internals, this
 * throws with a clear message.
 */
private fun extractMetalLayerPtr(): Long? {
    waitForVisibleWindow()
    val window = java.awt.Window.getWindows().firstOrNull { it.isShowing }
        ?: throw IllegalStateException("no visible window found")
    // ComposeWindow extends JFrame; its composePanel field is private.
    val composePanel = window.javaClass.getDeclaredField("composePanel").apply {
        isAccessible = true
    }.get(window)
    // ComposeWindowPanel._composeContainer is private.
    val container = composePanel.javaClass.getDeclaredField("_composeContainer").apply {
        isAccessible = true
    }.get(composePanel)
    // ComposeContainer is internal — use reflection to call contentComponent.
    val skiaLayer = container.javaClass.getMethod("getContentComponent").invoke(container) as SkiaLayer
    // On macOS, contentHandle returns the NSWindow* (verified in skiko's
    // Drawlayer.mm: getContentHandle returns layer.window).
    val nsWindowPtr = skiaLayer.contentHandle
    // Get the content view (NSView*) from the NSWindow via ObjC.
    val nsViewPtr = org.graphiks.kffi.objc.ObjCRuntime.autoreleasePool {
        val contentView = org.graphiks.kffi.objc.ObjCRuntime.msgSend(
            java.lang.foreign.ValueLayout.ADDRESS,
            java.lang.foreign.MemorySegment.ofAddress(nsWindowPtr),
            org.graphiks.kffi.objc.ObjCRuntime.sel("contentView"),
        ) as java.lang.foreign.MemorySegment
        contentView.address()
    }
    return MetalSurface.metalLayerOf(nsViewPtr)
}

/**
 * Waits until at least one AWT window is visible (showing), with a timeout.
 * The LaunchedEffect runs after the first composition, but the window may not
 * be visible yet — poll until it is.
 */
private fun waitForVisibleWindow(timeoutMillis: Long = 5000) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < deadline) {
        if (java.awt.Window.getWindows().any { it.isShowing }) return
        Thread.sleep(50)
    }
    throw IllegalStateException("timed out waiting for a visible window after ${timeoutMillis}ms")
}
