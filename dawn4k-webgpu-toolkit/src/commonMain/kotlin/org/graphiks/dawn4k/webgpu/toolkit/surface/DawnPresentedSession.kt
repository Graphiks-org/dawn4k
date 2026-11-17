package org.graphiks.dawn4k.webgpu.toolkit.surface

import kotlinx.coroutines.*
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.webgpu.toolkit.*
import org.graphiks.dawn4k.webgpu.toolkit.interop.ToolkitSessionView
import org.graphiks.dawn4k.webgpu.toolkit.surface.internal.*
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.toolkit.PreparedInitialization
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.SessionInitialization
import org.graphiks.webgpu.toolkit.SurfaceReference
import org.graphiks.webgpu.toolkit.openSession

/**
 * A GPU session presenting to one external target: [gpu] owns the Dawn handles and
 * [presentation] owns its surface lease, while the application keeps owning the
 * native window or view behind the target.
 *
 * The session stays live until [shutdown]; the caller owns that call.
 */
class DawnPresentedSession internal constructor(
    val gpu: ToolkitSessionView,
    val presentation: DawnPresentation,
    /** The target this session retains and presents to. */
    val surfaceTarget: DawnSurfaceTarget,
    private val session: Session,
) {
    /**
     * Acquires one frame and hands it to the caller, who presents and closes it. Returns null
     * when the target cannot be drawn to right now.
     */
    suspend fun acquireFrame(): DawnFrame? = presentation.acquireFrame()

    /**
     * Retires the presentation's surface and lease, then shuts the session down. Idempotent:
     * a second call is a no-op.
     */
    suspend fun shutdown() {
        presentation.retire()
        gpu.manualProgress?.stop()
        session.shutdown()
    }
}

/**
 * Opens a session presenting to [target]: retains the target, requests an adapter
 * compatible with the retained surface, binds device and queue, then negotiates
 * the surface configuration. Failure — including caller cancellation — rolls the
 * acquisition back and retires what was retained, bounded by
 * [DawnSessionOptions.shutdownTimeout].
 *
 * The returned session stays live; the caller must reach [DawnPresentedSession.shutdown].
 */
suspend fun openDawnSurface(
    target: DawnSurfaceTarget,
    sessionOptions: DawnSessionOptions = DawnSessionOptions(),
    presentationOptions: DawnPresentationOptions = DawnPresentationOptions(),
): DawnPresentedSession = openDawnSurface(target, sessionOptions, presentationOptions, SurfaceFactory.Native)

internal suspend fun openDawnSurface(
    target: DawnSurfaceTarget,
    sessionOptions: DawnSessionOptions,
    presentationOptions: DawnPresentationOptions,
    factory: SurfaceFactory,
): DawnPresentedSession {
    val extension = Dawn4kWebGpuToolkitExtension()
    var presentation: DawnPresentation? = null
    var view: ToolkitSessionView? = null

    // The surface is retained inside prepare(), against the context the extension just opened,
    // so the adapter request that follows is checked for compatibility with it.
    val initialization = SessionInitialization {
        val context = checkNotNull(extension.currentInstance()) {
            "the extension did not open an instance"
        }.dawnContext
        val retained = target.retain()
        val binding = try {
            factory.create(retained, context)
        } catch (failure: Throwable) {
            // The session's rollback only retires a prepared initialization; a prepare that
            // fails after retaining must release the lease itself.
            withContext(NonCancellable) { runCatching { retained.retire() } }
            throw failure
        }
        object : PreparedInitialization {
            override val compatibleSurface: SurfaceReference?
                get() = binding.compatibleSurface?.let(::DawnSurfaceReferenceAdapter)

            override suspend fun initialize(device: GPUDevice) {
                // The view is built here from the device the session just started: `initialize`
                // runs inside `openSession`, before the returned session exists.
                val sessionView = checkNotNull(view) { "the toolkit session is not available yet" }
                sessionView.bindDevice(device)
                binding.bind(sessionView)
                lateinit var controller: PresentationController
                controller = PresentationController(binding, presentationOptions) {
                    val active = checkNotNull(controller.currentAttachment)
                    val reference = active.lease.createSurface(sessionView.context)
                    DawnNativeSurface(reference, sessionView.adapter, sessionView.device)
                }
                val created = DawnPresentation(sessionView, controller, factory)
                presentation = created
                controller.initialize()
                created.observe(target)
            }

            override suspend fun retire() {
                val configured = presentation
                if (configured != null) configured.retire() else binding.retire()
            }
        }
    }

    val config = SessionConfig(
        startupTimeout = sessionOptions.startupTimeout,
        shutdownTimeout = sessionOptions.shutdownTimeout,
        deviceDescriptor = sessionOptions.deviceDescriptor,
    )
    // `initialize` runs inside `openSession`, before the returned session exists, so the view is
    // created first and bound to the session once startup returns.
    val sessionView = ToolkitSessionView(extension)
    view = sessionView
    val session = openSession(extension, config, initialization = initialization)
    try {
        sessionView.bind(session)
        return DawnPresentedSession(
            gpu = sessionView,
            presentation = checkNotNull(presentation) { "the presentation was not created" },
            surfaceTarget = target,
            session = session,
        )
    } catch (failure: Throwable) {
        withContext(NonCancellable) {
            session.manualProgress?.stop()
            runCatching { session.shutdown() }
        }
        throw failure
    }
}

/**
 * The scoped form of [openDawnSurface]: every exit stops manual progress first
 * and then shuts down, so neither the presentation nor the session outlives
 * [block].
 */
suspend fun <T> withDawnSurface(
    target: DawnSurfaceTarget,
    sessionOptions: DawnSessionOptions = DawnSessionOptions(),
    presentationOptions: DawnPresentationOptions = DawnPresentationOptions(),
    block: suspend CoroutineScope.(ToolkitSessionView, DawnPresentation) -> T,
): T {
    val presented = openDawnSurface(target, sessionOptions, presentationOptions)
    return withToolkitShutdown(
        stopProgress = { presented.gpu.manualProgress?.stop() },
        shutdown = { presented.shutdown() },
    ) { presented.gpu.run { block(it, presented.presentation) } }
}

/**
 * Presents a dawn4k surface reference through the toolkit's [SurfaceReference].
 *
 * Both describe a retained native surface the adapter must be compatible with; the toolkit
 * only needs the reference to be closable, which the dawn4k reference already is.
 */
private class DawnSurfaceReferenceAdapter(
    private val reference: DawnSurfaceReference,
) : SurfaceReference {
    override fun close() = reference.close()
}
