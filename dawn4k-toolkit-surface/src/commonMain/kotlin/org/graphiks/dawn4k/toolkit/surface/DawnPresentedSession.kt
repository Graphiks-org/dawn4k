package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.*
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.internal.*

/**
 * A GPU session presenting to one external target: [gpu] owns the Dawn handles and
 * [presentation] owns its surface lease, while the application keeps owning the
 * native window or view behind the target.
 */
class DawnPresentedSession internal constructor(val gpu: DawnSession, val presentation: DawnPresentation) {
    /** Shuts the session down; retiring the presentation's surface and lease is the shutdown's first stage. */
    suspend fun shutdown() = gpu.shutdown()
}

/**
 * Opens a session presenting to [target]: retains the target, requests an adapter
 * compatible with the retained surface, binds device and queue, then negotiates
 * the surface configuration. Failure — including caller cancellation — rolls the
 * acquisition back and retires what was retained, bounded by
 * [DawnSessionOptions.shutdownTimeout].
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
    var lease: DawnTargetLease? = null
    var attachment: SurfaceAttachment? = null
    var presentation: DawnPresentation? = null
    val session = openDawnSession(sessionOptions, object : DawnSessionInitialization {
        override suspend fun prepare(context: DawnContext): DawnPreparedInitialization {
            return object : DawnPreparedInitialization {
                override val compatibleSurface get() = attachment?.compatibleSurface
                override suspend fun acquire() {
                    val retained = target.retain()
                    lease = retained
                    attachment = factory.create(retained, context)
                }
                override suspend fun initialize(session: DawnSession) {
                    val binding = checkNotNull(attachment)
                    binding.bind(session)
                    lateinit var controller: PresentationController
                    controller = PresentationController(binding, presentationOptions) {
                        val active = checkNotNull(controller.currentAttachment)
                        val reference = active.lease.createSurface(session.context)
                        DawnNativeSurface(reference, session.adapter, session.device)
                    }
                    val created = DawnPresentation(session, controller, factory)
                    presentation = created
                    controller.initialize()
                    created.observe(target)
                }
                override suspend fun retire() {
                    val configured = presentation
                    val acquired = attachment
                    if (configured != null) configured.retire()
                    else if (acquired != null) acquired.retire()
                    else lease?.retire()
                }
            }
        }
    })
    return DawnPresentedSession(session, checkNotNull(presentation))
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
    block: suspend CoroutineScope.(DawnSession, DawnPresentation) -> T,
): T {
    val presented = openDawnSurface(target, sessionOptions, presentationOptions)
    return withToolkitShutdown(
        stopProgress = { presented.gpu.manualProgress?.stop() },
        shutdown = { presented.shutdown() },
    ) { presented.gpu.run { block(it, presented.presentation) } }
}
