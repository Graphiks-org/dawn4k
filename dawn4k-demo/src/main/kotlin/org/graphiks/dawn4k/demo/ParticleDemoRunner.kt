package org.graphiks.dawn4k.demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import org.graphiks.dawn4k.DawnAdapter
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.NativeBridge
import org.graphiks.dawn4k.native.wgpuTextureCreateView
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.suite.demos.particles.ParticleScene
import org.graphiks.webgpu.suite.demos.particles.initialParticles
import org.graphiks.webgpu.suite.demos.particles.maxParticleCount

/** Borrows the host; all GPU resources are released before the caller destroys its window. */
internal suspend fun runParticleDemo(host: SurfaceHost, controls: ParticleControls, negotiateCapabilities: Boolean) {
    println("[demo] creating Dawn context (${host.backend} backend)")
    DawnContext.create(DawnConfig(backend = host.backend)).use { context ->
        println("[demo] requesting adapter")
        context.requestAdapter().getOrThrow().use { adapter ->
            val info = adapter.info
            println("[demo] adapter: ${info.vendor} / ${info.device} (${info.description}, architecture=${info.architecture})")
            println("[demo] requesting device")
            (adapter.requestDevice().getOrThrow() as DawnDevice).use { device ->
                val bridge = context.nativeBridge()
                host.createSurface(bridge, device.nativeHandle()).use { surface ->
                    if (negotiateCapabilities) surface.configureForAdapter((adapter as DawnAdapter).nativeHandle())
                    controls.initialize(maxParticleCount(device.limits))
                    renderLoop(device, surface, host, bridge, controls)
                }
            }
        }
    }
}

private suspend fun renderLoop(device: DawnDevice, surface: DawnSurface, host: SurfaceHost,
    bridge: NativeBridge, controls: ParticleControls) {
    var scene = ParticleScene.create(device, surface.textureFormat, initialParticles(controls.state.value.count))
    println("[demo] scene ready — starting render loop")
    val clock = ParticleFrameClock()
    var resetGeneration = controls.state.value.resetGeneration
    var frameCount = 0L
    try {
        while (coroutineContext.isActive) {
            val requested = controls.state.value
            if (requested.count != scene.count) {
                device.queue.onSubmittedWorkDone().getOrThrow()
                val replacement = ParticleScene.create(device, surface.textureFormat, initialParticles(requested.count))
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
            if (width <= 0 || height <= 0) { delay(16); continue }
            if (width != surface.width || height != surface.height) {
                println("[demo] configuring surface ${width}x${height}")
                surface.configure(width, height)
            }
            try {
                surface.acquireFrame().use { texture ->
                    bridge.call { createDawnTextureView(device, texture) }.use { view ->
                        device.createCommandEncoder().use { encoder ->
                            scene.encodeFrame(encoder, view, width, height, delta)
                            encoder.finish().use { device.queue.submit(listOf(it)) }
                        }
                    }
                    surface.present(texture)
                }
                frameCount++
                if (frameCount == 1L || frameCount % 120L == 0L)
                    println("[demo] frame $frameCount rendered (${width}x${height}, particles=${scene.count}, paused=${requested.paused}, delta=${"%.3f".format(delta)}s)")
            } catch (outdated: SurfaceOutdatedException) {
                println("[demo] surface outdated — reconfiguring ${width}x${height}")
                surface.configure(width, height)
            }
            delay(1)
        }
    } finally { scene.close() }
    println("[demo] render loop ended after $frameCount frames")
}

/** Wraps the borrowed surface texture view in Dawn's internal resource-owner type. */
private fun createDawnTextureView(device: DawnDevice, texture: BorrowedSurfaceTexture): GPUTextureView {
    val session = device.javaClass.getDeclaredField("session").apply { isAccessible = true }.get(device)
        ?: error("could not read DawnDevice.session")
    val viewHandle = wgpuTextureCreateView(texture.handle, null) ?: error("wgpuTextureCreateView returned no view")
    val dawnTextureViewClass = Class.forName("org.graphiks.dawn4k.DawnTextureView")
    val deviceSessionClass = Class.forName("org.graphiks.dawn4k.internal.DeviceSession")
    val constructor = dawnTextureViewClass.getDeclaredConstructor(deviceSessionClass,
        Long::class.javaPrimitiveType, String::class.java).apply { isAccessible = true }
    return constructor.newInstance(session, viewHandle.handler.rawValue, "surface-view") as GPUTextureView
}
