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

/** One opened surface attempt: the session plus what the caller must record about its lease. */
private class OpenedPresentation(val session: DawnPresentedSession, val backend: DawnBackend, val generation: Long)

/** Tracks the live particle scene against the controls state: replaces it when the requested
 *  particle count changes and resets it when a new reset generation arrives. */
private class ParticleSceneBook(
    private val gpu: DawnSession,
    val format: GPUTextureFormat,
    count: Int,
    initialResetGeneration: Long = 0L,
) {
    var scene: ParticleScene = ParticleScene.create(gpu.device, format, initialParticles(count)); private set
    var resetGeneration: Long = initialResetGeneration; private set

    /** Reconciles the scene with [requested] and returns the scene to render. */
    suspend fun reconcile(requested: ParticleControlState): ParticleScene {
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
        return scene
    }

    fun close() = scene.close()
}

/** One submitted frame's outcome: whether it was presented, and at which extent. */
private class RenderedFrame(val presented: Boolean, val extent: PixelExtent)

/** Encodes, submits and presents one frame of [scene]; null when no frame slot was acquired.
 *  [shouldPresent] runs after submission and decides whether the frame may still present. */
private suspend fun renderFrame(
    gpu: DawnSession,
    presentation: DawnPresentation,
    scene: ParticleScene,
    delta: Float,
    shouldPresent: suspend () -> Boolean,
): RenderedFrame? {
    var outcome: RenderedFrame? = null
    val rendered = presentation.withFrame { frame ->
        gpu.device.createCommandEncoder().use { encoder ->
            scene.encodeFrame(encoder, frame.view, frame.extent.width, frame.extent.height, delta)
            encoder.finish().use { gpu.queue.submit(listOf(it)) }
        }
        val presented = shouldPresent()
        if (presented) frame.present()
        outcome = RenderedFrame(presented, PixelExtent(frame.extent.width, frame.extent.height))
    }
    return if (rendered) outcome else null
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
            openPresentedSession(controller) { opened ->
                presented = opened.session
                backend = opened.backend
                generation = opened.generation
                surfaceCreations++
                registerRetirement(opened.session)
            }
        }
        val active = checkNotNull(presented)
        val gpu = active.gpu
        val presentation = active.presentation
        gpu.run {
            val info = gpu.adapter.info
            println("[demo] adapter: ${info.vendor} / ${info.device} (${info.description}, architecture=${info.architecture})")
            var book: ParticleSceneBook? = null
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
                            presentation.replaceTarget(lease.target, demoPresentationOptions(book?.format))
                            generation = lease.generation
                            surfaceCreations++
                            registerRetirement(active)
                            lastExtent = null
                        }
                        val selected = presentation.configuration.value ?: return@withSurface
                        var sceneBook = book
                        check(sceneBook == null || sceneBook.format == selected.format) {
                            "replacement surface cannot preserve the simulation texture format"
                        }
                        if (sceneBook == null) {
                            controller.controls.initialize(maxParticleCount(gpu.device.limits), sessionId)
                            sceneBook = ParticleSceneBook(gpu, selected.format, controller.controls.state.value.count)
                            book = sceneBook
                            controller.ready(sessionId)
                        }
                        val requested = controller.controls.state.value
                        val current = checkNotNull(sceneBook).reconcile(requested)
                        val delta = clock.advance(requested, controller.nowNanos())
                        if (lastExtent != extent) {
                            println("[demo] configuring surface ${extent.width}x${extent.height}")
                            lastExtent = extent
                        }
                        if (!lease.valid) { clock.suspend(); return@withSurface }
                        val frame = renderFrame(gpu, presentation, current, delta) { lease.valid }
                        if (frame == null || !lease.valid) { clock.suspend(); return@withSurface }
                        frameCount++
                        if (frameCount == 1L || frameCount % 120L == 0L)
                            println("[demo] frame $frameCount rendered (${frame.extent.width}x${frame.extent.height}, particles=${current.count}, paused=${requested.paused}, delta=${delta}s)")
                        val fingerprint = if (controller.evidence.wantsFingerprint) particleFingerprint(gpu.device, current) else null
                        controller.evidence.publish(DemoEvidenceSnapshot(
                            sessionId = sessionId, phase = controller.state.value.phase,
                            backend = checkNotNull(backend).name, adapter = info.description,
                            count = current.count, paused = requested.paused, resetGeneration = sceneBook.resetGeneration,
                            physicalExtent = frame.extent, surfaceGeneration = generation,
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
                            { controller.releasePresentation = null; book?.close() },
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

/** Tries once, under the frame lock, to open the presented surface for the current lease;
 *  [onOpened] records the session where retirement must be registered before any queued
 *  command can observe it. A vanished view leaves the result unset so the caller retries. */
private suspend fun openPresentedSession(
    controller: DemoSessionController,
    onOpened: (OpenedPresentation) -> Unit,
) {
    controller.surface.first { it?.valid == true }
    controller.withSurface { lease ->
        if (lease == null || !lease.valid) return@withSurface
        lease.pixelExtent()
        try {
            val opened = openDawnSurface(lease.target, DawnSessionOptions(config = DawnConfig(lease.backend)), demoPresentationOptions())
            controller.sessionOpened(opened.gpu)
            onOpened(OpenedPresentation(opened, lease.backend, lease.generation))
        } catch (caught: Throwable) {
            // A disappearing view is normal; its rollback still owns any retained target.
            if (lease.valid || caught is CancellationException) throw caught
        }
    }
}

private suspend fun renderLoop(gpu: DawnSession, presentation: DawnPresentation,
    lease: DemoSurfaceLease, controls: ParticleControls) {
    val format = checkNotNull(presentation.configuration.value).format
    val book = ParticleSceneBook(gpu, format, controls.state.value.count, controls.state.value.resetGeneration)
    println("[demo] scene ready — starting render loop")
    val clock = ParticleFrameClock()
    var frameCount = 0L
    var previousExtent: PixelExtent? = null
    var failure: Throwable? = null
    try {
        while (coroutineContext.isActive) {
            requireDemoReady(gpu)
            val requested = controls.state.value
            val scene = book.reconcile(requested)
            val extent = lease.pixelExtent()
            if (extent.width <= 0 || extent.height <= 0) { clock.suspend(); delay(16); continue }
            val delta = clock.advance(requested, demoMonotonicNanos())
            if (previousExtent != extent) {
                println("[demo] configuring surface ${extent.width}x${extent.height}")
                previousExtent = extent
            }
            val frame = renderFrame(gpu, presentation, scene, delta) { coroutineContext.ensureActive(); true }
            if (frame == null) { clock.suspend(); delay(16); continue }
            frameCount++
            if (frameCount == 1L || frameCount % 120L == 0L)
                println("[demo] frame $frameCount rendered (${extent.width}x${extent.height}, particles=${scene.count}, paused=${requested.paused}, delta=${delta}s)")
            delay(1)
        }
    } catch (caught: Throwable) { failure = caught; throw caught }
    finally { demoCleanup(failure, { gpu.queue.onSubmittedWorkDone().getOrThrow() }, { book.close() }) }
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
