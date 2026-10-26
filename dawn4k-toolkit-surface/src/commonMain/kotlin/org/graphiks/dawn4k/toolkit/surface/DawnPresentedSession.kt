package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.*
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.internal.*

class DawnPresentedSession internal constructor(val gpu: DawnSession, val presentation: DawnPresentation) {
    suspend fun shutdown() = gpu.shutdown()
}

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
