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
    val lease = target.retain()
    var attachment: SurfaceAttachment? = null
    var presentation: DawnPresentation? = null
    var gpu: DawnSession? = null
    var leaseRetired = false
    try {
        val session = openDawnSession(sessionOptions, object : DawnSessionInitialization {
            override suspend fun prepare(context: DawnContext): DawnPreparedInitialization {
                val created = try { factory.create(lease, context) }
                catch (failure: Throwable) {
                    withContext(NonCancellable) {
                        try { lease.retire(); leaseRetired = true } catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
                    }
                    throw failure
                }
                attachment = created
                return object : DawnPreparedInitialization {
                    override val compatibleSurface = created.compatibleSurface
                    override suspend fun retire() {
                        if (presentation != null) presentation!!.retire() else created.retire()
                    }
                }
            }
        })
        gpu = session
        val binding = checkNotNull(attachment)
        session.run {
            binding.bind(session)
            lateinit var controller: PresentationController
            controller = PresentationController(binding, presentationOptions) {
                val active = checkNotNull(controller.currentAttachment)
                val reference = active.lease.createSurface(session.context)
                DawnNativeSurface(reference, session.adapter, session.device)
            }
            // Store the owner before capability negotiation, so failure has a cleanup route.
            val createdPresentation = DawnPresentation(session, controller, factory)
            presentation = createdPresentation
            controller.initialize()
            createdPresentation.observe(target)
        }
        return DawnPresentedSession(session, checkNotNull(presentation))
    } catch (failure: Throwable) {
        withContext(NonCancellable) {
            try {
                if (gpu != null) { gpu.manualProgress?.stop(); gpu.shutdown() }
                else if (attachment == null && !leaseRetired) { lease.retire(); leaseRetired = true }
            } catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
        }
        throw failure
    }
}

suspend fun <T> withDawnSurface(
    target: DawnSurfaceTarget,
    sessionOptions: DawnSessionOptions = DawnSessionOptions(),
    presentationOptions: DawnPresentationOptions = DawnPresentationOptions(),
    block: suspend CoroutineScope.(DawnSession, DawnPresentation) -> T,
): T {
    val presented = openDawnSurface(target, sessionOptions, presentationOptions)
    var failure: Throwable? = null
    try { return presented.gpu.run { block(it, presented.presentation) } }
    catch (caught: Throwable) { failure = caught; throw caught }
    finally {
        presented.gpu.manualProgress?.stop()
        try { presented.shutdown() }
        catch (cleanup: Throwable) { if (failure == null) throw cleanup else failure.addSuppressed(cleanup) }
    }
}
