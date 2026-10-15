package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graphiks.dawn4k.toolkit.DawnSession
import org.graphiks.dawn4k.toolkit.DawnSessionState
import org.graphiks.dawn4k.toolkit.surface.internal.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** Uses the GPU session owner. The application continues to own an external window or view. */
class DawnPresentation internal constructor(
    private val gpu: DawnSession,
    private val controller: PresentationController,
    private val factory: SurfaceFactory,
) {
    val configuration = controller.configuration
    private val monitoring = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val frames = Mutex()
    private var watch: Job? = null

    private class FrameUse(val presentation: DawnPresentation) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<FrameUse>
    }

    private suspend fun checkOutsideFrame() {
        check(currentCoroutineContext()[FrameUse]?.presentation !== this) {
            "finish the current frame before replacing or detaching its target"
        }
    }

    internal fun observe(target: DawnSurfaceTarget) {
        watch?.cancel()
        val generation = controller.activeGeneration ?: return
        watch = monitoring.launch {
            target.state.collect { state ->
                if (!state.available || state.generation != generation) runCatching {
                    gpu.executionOwner.run { frames.withLock { controller.detachTarget(generation) } }
                }
            }
        }
    }

    suspend fun withFrame(block: suspend (DawnFrame) -> Unit): Boolean = gpu.run {
        checkOutsideFrame()
        frames.withLock {
            val frame = controller.acquireFrame() ?: return@withLock false
            withContext(FrameUse(this@DawnPresentation)) { frame.use { block(it) } }
            true
        }
    }

    suspend fun replaceTarget(target: DawnSurfaceTarget) = replaceTargetWithOptions(target, null)

    /** Applies the new policy only after successful attachment, for example to retain an existing pipeline's format. */
    suspend fun replaceTarget(target: DawnSurfaceTarget, options: DawnPresentationOptions) =
        replaceTargetWithOptions(target, options)

    private suspend fun replaceTargetWithOptions(target: DawnSurfaceTarget, options: DawnPresentationOptions?) {
        checkOutsideFrame()
        check(gpu.state.value == DawnSessionState.Ready) { "GPU session is not ready" }
        gpu.executionOwner.run {
            frames.withLock {
                check(gpu.state.value == DawnSessionState.Ready) { "GPU session is not ready" }
                val lease = target.retain()
                var replacement: SurfaceAttachment? = null
                try {
                    replacement = factory.create(lease, gpu.context)
                    replacement.bind(gpu)
                    controller.replaceTarget(target, replacement, options)
                    observe(target)
                } catch (failure: Throwable) {
                    withContext(NonCancellable) {
                        try { if (replacement == null) lease.retire() else replacement.retire() }
                        catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
                    }
                    throw failure
                }
            }
        }
    }

    suspend fun detachTarget(generation: Long) {
        checkOutsideFrame()
        gpu.executionOwner.run { frames.withLock { controller.detachTarget(generation) } }
    }

    internal suspend fun retire() {
        monitoring.cancel()
        frames.withLock { controller.retire() }
    }
}
