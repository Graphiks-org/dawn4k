package org.graphiks.dawn4k.demo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlin.coroutines.coroutineContext
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.*
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.suite.demos.particles.ParticleScene
import org.graphiks.webgpu.suite.demos.particles.initialParticles
import org.graphiks.webgpu.suite.demos.particles.maxParticleCount

/** Borrows its caller-owned native host until all presentation leases retire. */
internal suspend fun runParticleDemo(host: SurfaceHost, controls: ParticleControls) {
    val lease = DesktopSurfaceLease(1, host)
    var failure: Throwable? = null
    try {
        withDawnSurface(lease.target, DawnSessionOptions(config = DawnConfig(host.backend)), demoPresentationOptions()) { gpu, presentation ->
            val info = gpu.adapter.info
            println("[demo] adapter: ${info.vendor} / ${info.device} (${info.description}, architecture=${info.architecture})")
            controls.initialize(maxParticleCount(gpu.device.limits))
            renderLoop(gpu, presentation, lease, controls)
        }
    } catch (caught: Throwable) { failure = caught; throw caught }
    finally { demoCleanup(failure, { lease.awaitRetirement() }) }
}

/** One admitted application lifetime keeps particle resources alive through device-loss cleanup. */
internal suspend fun runParticleDemo(controller: DemoSessionController, sessionId: Long) {
    val clock = controller.frameClock
    var presented: DawnPresentedSession? = null
    var backend: DawnBackend? = null
    var generation = -1L
    var surfaceCreations = 0L
    var failure: Throwable? = null

    fun registerRetirement(active: DawnPresentedSession) {
        controller.releasePresentation = {
            // The frame lock already excludes rendering. Bypass application admission so
            // target retirement cannot wait behind the long-lived simulation block.
            active.gpu.executionOwner.run {
                try { active.gpu.queue.onSubmittedWorkDone().getOrThrow() }
                finally { active.presentation.detachTarget(generation); clock.suspend() }
            }
        }
    }

    try {
        while (presented == null) {
            controller.surface.first { it?.valid == true }
            controller.withSurface { lease ->
                if (lease == null || !lease.valid) return@withSurface
                lease.pixelExtent()
                try {
                    val opened = openDawnSurface(lease.target, DawnSessionOptions(config = DawnConfig(lease.backend)), demoPresentationOptions())
                    presented = opened
                    backend = lease.backend
                    generation = lease.generation
                    surfaceCreations++
                    controller.sessionOpened(opened.gpu)
                    registerRetirement(opened)
                } catch (caught: Throwable) {
                    // A disappearing view is normal; its rollback still owns any retained target.
                    if (lease.valid || caught is CancellationException) throw caught
                }
            }
        }
        val active = checkNotNull(presented)
        val gpu = active.gpu
        val presentation = active.presentation
        gpu.run {
            val info = gpu.adapter.info
            println("[demo] adapter: ${info.vendor} / ${info.device} (${info.description}, architecture=${info.architecture})")
            var scene: ParticleScene? = null
            var format: GPUTextureFormat? = null
            var resetGeneration = 0L
            var frameCount = 0L
            var lastExtent: PixelExtent? = null
            var applicationFailure: Throwable? = null
            try {
                while (coroutineContext.isActive) {
                    requireDemoReady(gpu)
                    controller.withSurface { lease ->
                        if (lease == null || !lease.valid || !controller.state.value.lifecycleActive) {
                            clock.suspend(); return@withSurface
                        }
                        val extent = lease.pixelExtent()
                        if (extent.width <= 0 || extent.height <= 0) { clock.suspend(); return@withSurface }
                        if (generation != lease.generation) {
                            check(lease.backend == backend) { "replacement viewport requires a different backend" }
                            presentation.replaceTarget(lease.target, demoPresentationOptions(format))
                            generation = lease.generation
                            surfaceCreations++
                            registerRetirement(active)
                            lastExtent = null
                        }
                        val selected = presentation.configuration.value ?: return@withSurface
                        check(scene == null || format == selected.format) {
                            "replacement surface cannot preserve the simulation texture format"
                        }
                        if (scene == null) {
                            format = selected.format
                            controller.controls.initialize(maxParticleCount(gpu.device.limits), sessionId)
                            scene = ParticleScene.create(gpu.device, selected.format,
                                initialParticles(controller.controls.state.value.count))
                            controller.ready(sessionId)
                        }
                        var current = checkNotNull(scene)
                        val requested = controller.controls.state.value
                        if (requested.count != current.count) {
                            gpu.queue.onSubmittedWorkDone().getOrThrow()
                            val replacement = ParticleScene.create(gpu.device, selected.format, initialParticles(requested.count))
                            scene = replacement
                            current.close()
                            current = replacement
                            println("[demo] scene changed (${current.count} particles)")
                        }
                        if (requested.resetGeneration != resetGeneration) {
                            current.reset(initialParticles(current.count))
                            resetGeneration = requested.resetGeneration
                            println("[demo] scene reset (${current.count} particles)")
                        }
                        val delta = clock.advance(requested, controller.nowNanos())
                        if (lastExtent != extent) {
                            println("[demo] configuring surface ${extent.width}x${extent.height}")
                            lastExtent = extent
                        }
                        if (!lease.valid) { clock.suspend(); return@withSurface }
                        var renderedExtent = extent
                        val rendered = presentation.withFrame { frame ->
                            gpu.device.createCommandEncoder().use { encoder ->
                                current.encodeFrame(encoder, frame.view, frame.extent.width, frame.extent.height, delta)
                                encoder.finish().use { gpu.queue.submit(listOf(it)) }
                            }
                            renderedExtent = PixelExtent(frame.extent.width, frame.extent.height)
                            if (lease.valid) frame.present()
                        }
                        if (!rendered || !lease.valid) { clock.suspend(); return@withSurface }
                        frameCount++
                        if (frameCount == 1L || frameCount % 120L == 0L)
                            println("[demo] frame $frameCount rendered (${renderedExtent.width}x${renderedExtent.height}, particles=${current.count}, paused=${requested.paused}, delta=${delta}s)")
                        val fingerprint = if (controller.evidence.wantsFingerprint) particleFingerprint(gpu.device, current) else null
                        controller.evidence.publish(DemoEvidenceSnapshot(
                            sessionId = sessionId, phase = controller.state.value.phase,
                            backend = checkNotNull(backend).name, adapter = info.description,
                            count = current.count, paused = requested.paused, resetGeneration = resetGeneration,
                            physicalExtent = renderedExtent, surfaceGeneration = generation,
                            frameCount = frameCount, simulationFingerprint = fingerprint,
                            deviceCreations = 1, surfaceCreations = surfaceCreations,
                        ))
                    }
                    delay(1)
                }
            } catch (caught: Throwable) { applicationFailure = caught; throw caught }
            finally {
                demoCleanup(applicationFailure, {
                    controller.withSurface {
                        demoCleanup(null,
                            { controller.releasePresentation?.invoke() },
                            { controller.releasePresentation = null; scene?.close() },
                        )
                    }
                })
            }
        }
    } catch (caught: Throwable) { failure = caught; throw caught }
    finally {
        // Shutdown begins after application admission and scene cleanup have ended.
        demoCleanup(failure,
            { presented?.shutdown() },
            { presented?.gpu?.let(controller::sessionClosed) },
        )
    }
}

private suspend fun renderLoop(gpu: DawnSession, presentation: DawnPresentation,
    lease: DemoSurfaceLease, controls: ParticleControls) {
    val format = checkNotNull(presentation.configuration.value).format
    var scene = ParticleScene.create(gpu.device, format, initialParticles(controls.state.value.count))
    println("[demo] scene ready — starting render loop")
    val clock = ParticleFrameClock()
    var resetGeneration = controls.state.value.resetGeneration
    var frameCount = 0L
    var previousExtent: PixelExtent? = null
    var failure: Throwable? = null
    try {
        while (coroutineContext.isActive) {
            requireDemoReady(gpu)
            val requested = controls.state.value
            if (requested.count != scene.count) {
                gpu.queue.onSubmittedWorkDone().getOrThrow()
                val replacement = ParticleScene.create(gpu.device, format, initialParticles(requested.count))
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
            val extent = lease.pixelExtent()
            if (extent.width <= 0 || extent.height <= 0) { clock.suspend(); delay(16); continue }
            val delta = clock.advance(requested, demoMonotonicNanos())
            if (previousExtent != extent) {
                println("[demo] configuring surface ${extent.width}x${extent.height}")
                previousExtent = extent
            }
            val rendered = presentation.withFrame { frame ->
                gpu.device.createCommandEncoder().use { encoder ->
                    scene.encodeFrame(encoder, frame.view, frame.extent.width, frame.extent.height, delta)
                    encoder.finish().use { gpu.queue.submit(listOf(it)) }
                }
                coroutineContext.ensureActive()
                frame.present()
            }
            if (rendered) {
                frameCount++
                if (frameCount == 1L || frameCount % 120L == 0L)
                    println("[demo] frame $frameCount rendered (${extent.width}x${extent.height}, particles=${scene.count}, paused=${requested.paused}, delta=${delta}s)")
            } else clock.suspend()
            delay(1)
        }
    } catch (caught: Throwable) { failure = caught; throw caught }
    finally { demoCleanup(failure, { gpu.queue.onSubmittedWorkDone().getOrThrow() }, { scene.close() }) }
}

private fun demoPresentationOptions(format: GPUTextureFormat? = null) =
    DawnPresentationOptions(requiredFormat = format, requiredPresentMode = DawnPresentMode.Fifo)

private fun requireDemoReady(gpu: DawnSession) {
    when (val state = gpu.state.value) {
        DawnSessionState.Ready -> Unit
        is DawnSessionState.Lost -> error("device lost: ${state.info.message}")
        is DawnSessionState.Failed -> throw state.cause
        else -> error("GPU session is no longer ready: $state")
    }
}

private suspend fun demoCleanup(primary: Throwable?, vararg actions: suspend () -> Unit) = withContext(NonCancellable) {
    var first = primary
    for (action in actions) {
        try { action() }
        catch (failure: Throwable) {
            if (first == null) first = failure else if (failure !== first) first.addSuppressed(failure)
        }
    }
    if (primary == null) first?.let { throw it }
}
