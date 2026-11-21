package org.graphiks.dawn4k.webgpu.toolkit.surface

import kotlinx.coroutines.*
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.webgpu.toolkit.*
import org.graphiks.dawn4k.webgpu.toolkit.surface.internal.*
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.toolkit.PreparedInitialization
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.SessionInitialization
import org.graphiks.webgpu.toolkit.SurfaceReference

/**
 * A GPU session presenting to one external target: [session] owns the device the presentation
 * runs on, and [presentation] owns the surface lease, while the application keeps owning the
 * native window or view behind the target.
 *
 * The session stays live until [shutdown]; the caller owns that call.
 */
class DawnPresentedSession internal constructor(
    /** The canonical session whose device presents; owned by the caller through [shutdown]. */
    val session: Session,
    val presentation: DawnPresentation,
    /** The target this session retains and presents to. */
    val surfaceTarget: DawnSurfaceTarget,
    private val extension: Dawn4kWebGpuToolkitExtension,
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
        // Retire on the context owner: the caller's thread may be mid-processEvents, where a
        // native close is rejected.
        withContext(checkNotNull(session.executionOwner) { "the session pins no execution owner" }) {
            presentation.retire()
        }
        session.manualProgress?.stop()
        session.shutdown()
        // The handles are released: drop the binding so the extension holds only live sessions.
        extension.detach(session)
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
    extension: Dawn4kWebGpuToolkitExtension = Dawn4kWebGpuToolkitExtension(),
): DawnPresentedSession = openDawnSurface(target, sessionOptions, presentationOptions, SurfaceFactory.Native, extension)

internal suspend fun openDawnSurface(
    target: DawnSurfaceTarget,
    sessionOptions: DawnSessionOptions,
    presentationOptions: DawnPresentationOptions,
    factory: SurfaceFactory,
    extension: Dawn4kWebGpuToolkitExtension,
): DawnPresentedSession {
    var presentation: DawnPresentation? = null
    var binding: SurfaceAttachment? = null

    // The surface is retained inside prepare(), against the context the extension just opened,
    // so the adapter request that follows is checked for compatibility with it.
    val initialization = SessionInitialization {
        val context = checkNotNull(extension.currentInstance()) {
            "the extension did not open an instance"
        }.dawnContext
        val retained = target.retain()
        val created = try {
            factory.create(retained, context)
        } catch (failure: Throwable) {
            // The session's rollback only retires a prepared initialization; a prepare that
            // fails after retaining must release the lease itself.
            withContext(NonCancellable) { runCatching { retained.retire() } }
            throw failure
        }
        binding = created
        object : PreparedInitialization {
            override val compatibleSurface: SurfaceReference?
                get() = created.compatibleSurface?.let(::DawnSurfaceReferenceAdapter)

            override suspend fun initialize(device: GPUDevice) {}

            override suspend fun retire() {
                // A startup failure after the presentation was built retires it; before that,
                // the prepared binding is what startup acquired.
                val configured = presentation
                if (configured != null) configured.retire() else created.retire()
            }
        }
    }

    val config = SessionConfig(
        startupTimeout = sessionOptions.startupTimeout,
        shutdownTimeout = sessionOptions.shutdownTimeout,
        deviceDescriptor = sessionOptions.deviceDescriptor,
    )
    // `initialize` runs inside `openDawnSession`, before the returned session exists; the
    // binding needs the session, so it completes right after startup returns.
    val session = openDawnSession(extension, config, initialization = initialization)
    try {
        val attached = checkNotNull(binding) { "startup prepared no surface binding" }
        attached.bind(extension, session)
        lateinit var controller: PresentationController
        controller = PresentationController(attached, presentationOptions) {
            val active = checkNotNull(controller.currentAttachment)
            val reference = active.lease.createSurface(extension.dawnContext(session))
            DawnNativeSurface(reference, extension.dawnAdapter(session), extension.dawnDevice(session))
        }
        val created = DawnPresentation(extension, session, controller, factory)
        presentation = created
        controller.initialize()
        created.observe(target)
        return DawnPresentedSession(
            session = session,
            presentation = created,
            surfaceTarget = target,
            extension = extension,
        )
    } catch (failure: Throwable) {
        withContext(NonCancellable) {
            presentation?.let { created -> runCatching { created.retire() } }
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
    extension: Dawn4kWebGpuToolkitExtension = Dawn4kWebGpuToolkitExtension(),
    block: suspend CoroutineScope.(Session, DawnPresentation) -> T,
): T {
    val presented = openDawnSurface(target, sessionOptions, presentationOptions, extension)
    return withToolkitShutdown(
        stopProgress = { presented.session.manualProgress?.stop() },
        shutdown = { presented.shutdown() },
    ) {
        presented.session.run {
            withContext(checkNotNull(presented.session.executionOwner) { "the session pins no execution owner" }) {
                coroutineScope { block(presented.session, presented.presentation) }
            }
        }
    }
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
