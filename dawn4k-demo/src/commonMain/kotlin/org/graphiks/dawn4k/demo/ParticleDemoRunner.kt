package org.graphiks.dawn4k.demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlin.coroutines.coroutineContext
import org.graphiks.dawn4k.DawnAdapter
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.NativeBridge
import org.graphiks.webgpu.suite.demos.particles.ParticleScene
import org.graphiks.webgpu.suite.demos.particles.initialParticles
import org.graphiks.webgpu.suite.demos.particles.maxParticleCount

/** Borrows the host; all GPU resources are released before the caller destroys its window. */
internal suspend fun runParticleDemo(host: SurfaceHost, controls: ParticleControls, negotiateCapabilities: Boolean) {
    println("[demo] creating Dawn context (${host.backend} backend)")
    DawnContext.create(DawnConfig(backend = host.backend, implicitDeviceSynchronization = true)).useWithDemoEventProgress { context, adapter, device ->
            val info = adapter.info
            println("[demo] adapter: ${info.vendor} / ${info.device} (${info.description}, architecture=${info.architecture})")
                val bridge = context.nativeBridge()
                host.createSurface(bridge, device.nativeHandle()).use { surface ->
                    if (negotiateCapabilities) surface.configureForAdapter((adapter as DawnAdapter).nativeHandle())
                    controls.initialize(maxParticleCount(device.limits))
                    renderLoop(device, surface, host, bridge, controls)
                }
    }
}

/** The simulation survives presentation-target replacement within one session. */
internal suspend fun runParticleDemo(controller: DemoSessionController, sessionId: Long) {
    val firstLease = controller.surface.first { it?.valid == true }!!
    DawnContext.create(DawnConfig(backend = firstLease.backend, implicitDeviceSynchronization = true)).useWithDemoEventProgress { context, adapter, device ->
            val info = adapter.info
            println("[demo] adapter: ${info.vendor} / ${info.device} (${info.description}, architecture=${info.architecture})")
                val bridge = context.nativeBridge()
                val clock = controller.frameClock
                var scene: ParticleScene? = null
                var presentation: DawnSurface? = null
                var presentationFormat: org.graphiks.webgpu.GPUTextureFormat? = null
                var generation = -1L
                var resetGeneration = 0L
                var frameCount = 0L
                var surfaceCreations = 0L
                try {
                    while (coroutineContext.isActive) {
                        controller.withSurface { lease ->
                            if (lease == null || !lease.valid || !controller.state.value.lifecycleActive) {
                                clock.suspend()
                                return@withSurface
                            }
                            val extent = lease.pixelExtent()
                            if (extent.width <= 0 || extent.height <= 0) {
                                clock.suspend()
                                return@withSurface
                            }
                            if (presentation == null || generation != lease.generation) {
                                check(lease.backend == firstLease.backend) { "replacement viewport requires a different backend" }
                                val replacement = lease.createSurface(bridge, device)
                                try {
                                    replacement.configureForAdapter((adapter as DawnAdapter).nativeHandle(), presentationFormat)
                                    check(scene == null || replacement.textureFormat == presentationFormat) {
                                        "replacement surface cannot preserve the simulation texture format"
                                    }
                                } catch (failure: Throwable) { replacement.close(); throw failure }
                                presentation = replacement
                                generation = lease.generation
                                surfaceCreations++
                                controller.releasePresentation = {
                                    try { device.queue.onSubmittedWorkDone().getOrThrow() }
                                    finally {
                                        presentation?.close()
                                        presentation = null
                                        clock.suspend()
                                    }
                                }
                                if (scene == null) {
                                    presentationFormat = replacement.textureFormat
                                    controller.controls.initialize(maxParticleCount(device.limits), sessionId)
                                    scene = ParticleScene.create(device, replacement.textureFormat,
                                        initialParticles(controller.controls.state.value.count))
                                    controller.ready(sessionId)
                                }
                            }
                            val surface = checkNotNull(presentation)
                            var activeScene = checkNotNull(scene)
                            val requested = controller.controls.state.value
                            if (requested.count != activeScene.count) {
                                device.queue.onSubmittedWorkDone().getOrThrow()
                                val replacement = ParticleScene.create(device, surface.textureFormat, initialParticles(requested.count))
                                scene = replacement
                                activeScene.close()
                                activeScene = replacement
                                println("[demo] scene changed (${activeScene.count} particles)")
                            }
                            if (requested.resetGeneration != resetGeneration) {
                                activeScene.reset(initialParticles(activeScene.count))
                                resetGeneration = requested.resetGeneration
                                println("[demo] scene reset (${activeScene.count} particles)")
                            }
                            val delta = clock.advance(requested, controller.nowNanos())
                            if (surface.width != extent.width || surface.height != extent.height) {
                                println("[demo] configuring surface ${extent.width}x${extent.height}")
                                surface.configure(extent.width, extent.height)
                            }
                            if (!lease.valid) { clock.suspend(); return@withSurface }
                            try {
                                surface.acquireFrame().use { texture ->
                                    texture.createView(device).use { view ->
                                        device.createCommandEncoder().use { encoder ->
                                            activeScene.encodeFrame(encoder, view, extent.width, extent.height, delta)
                                            encoder.finish().use { device.queue.submit(listOf(it)) }
                                        }
                                    }
                                    if (lease.valid) surface.present(texture)
                                }
                                if (!lease.valid) { clock.suspend(); return@withSurface }
                                frameCount++
                                if (frameCount == 1L || frameCount % 120L == 0L)
                                    println("[demo] frame $frameCount rendered (${extent.width}x${extent.height}, particles=${activeScene.count}, paused=${requested.paused}, delta=${delta}s)")
                                val fingerprint = if (controller.evidence.wantsFingerprint)
                                    particleFingerprint(device, activeScene) else null
                                controller.evidence.publish(DemoEvidenceSnapshot(
                                    sessionId = sessionId, phase = controller.state.value.phase,
                                    backend = firstLease.backend.name, adapter = adapter.info.description,
                                    count = activeScene.count, paused = requested.paused,
                                    resetGeneration = resetGeneration,
                                    physicalExtent = extent, surfaceGeneration = generation,
                                    frameCount = frameCount, simulationFingerprint = fingerprint,
                                    deviceCreations = 1, surfaceCreations = surfaceCreations,
                                ))
                            } catch (outdated: SurfaceOutdatedException) {
                                if (lease.valid) surface.configure(extent.width, extent.height)
                                else { controller.releasePresentation?.invoke(); controller.releasePresentation = null }
                                clock.suspend()
                            }
                        }
                        delay(1)
                    }
                } finally {
                    withContext(NonCancellable) {
                        controller.withSurface {
                            try { controller.releasePresentation?.invoke() }
                            finally { controller.releasePresentation = null; scene?.close() }
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
            val (width, height) = host.pixelSize()
            if (width <= 0 || height <= 0) { clock.suspend(); delay(16); continue }
            val delta = clock.advance(requested, demoMonotonicNanos())
            if (width != surface.width || height != surface.height) {
                println("[demo] configuring surface ${width}x${height}")
                surface.configure(width, height)
            }
            try {
                surface.acquireFrame().use { texture ->
                    texture.createView(device).use { view ->
                        device.createCommandEncoder().use { encoder ->
                            scene.encodeFrame(encoder, view, width, height, delta)
                            encoder.finish().use { device.queue.submit(listOf(it)) }
                        }
                    }
                    surface.present(texture)
                }
                frameCount++
                if (frameCount == 1L || frameCount % 120L == 0L)
                    println("[demo] frame $frameCount rendered (${width}x${height}, particles=${scene.count}, paused=${requested.paused}, delta=${delta}s)")
            } catch (outdated: SurfaceOutdatedException) {
                println("[demo] surface outdated — reconfiguring ${width}x${height}")
                surface.configure(width, height)
            }
            delay(1)
        }
    } finally { scene.close() }
    println("[demo] render loop ended after $frameCount frames")
}
