package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.*
import org.graphiks.dawn4k.toolkit.DawnSession
import org.graphiks.dawn4k.toolkit.surface.internal.*

/** Uses the GPU session owner. The application continues to own an external window or view. */
class DawnPresentation internal constructor(
    private val gpu: DawnSession,
    private val controller: PresentationController,
    private val factory: SurfaceFactory,
) {
    val configuration = controller.configuration
    private val monitoring = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watch: Job? = null

    internal fun observe(target: DawnSurfaceTarget) {
        watch?.cancel()
        watch = monitoring.launch {
            target.state.collect { state ->
                if (!state.available) runCatching { detachTarget(state.generation) }
            }
        }
    }
    suspend fun withFrame(block: suspend (DawnFrame) -> Unit): Boolean = gpu.run {
        val frame = controller.acquireFrame() ?: return@run false
        frame.use { block(it) }
        true
    }
    suspend fun replaceTarget(target: DawnSurfaceTarget) = gpu.run {
        val lease = target.retain()
        var replacement: SurfaceAttachment? = null
        try {
            replacement = factory.create(lease, gpu.context)
            replacement.bind(gpu)
            controller.replaceTarget(target, replacement)
            observe(target)
        } catch (failure: Throwable) {
            withContext(NonCancellable) {
                try { if (replacement == null) lease.retire() else replacement.retire() }
                catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
            }
            throw failure
        }
    }
    suspend fun detachTarget(generation: Long) = gpu.run { controller.detachTarget(generation) }

    internal suspend fun retire() {
        monitoring.cancel()
        controller.retire()
    }
}
