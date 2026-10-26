package org.graphiks.dawn4k.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget

/**
 * Adapts a toolkit-owned view target (DawnSurfaceView on Android, DawnMetalView on Apple)
 * to the controller's lease contract. Availability and extent derive from the view's own
 * target state; the view drives retention and acknowledges retirement itself, so closing
 * the adapter releases nothing.
 */
internal class ToolkitViewLease(
    override val target: DawnSurfaceTarget,
    override val backend: DawnBackend,
    private val awaitViewRetirement: suspend () -> Unit,
) : DemoSurfaceLease {
    override val generation: Long = target.state.value.generation
    override val valid: Boolean get() = target.state.value.available
    override fun pixelExtent() =
        PixelExtent(target.state.value.extent.width, target.state.value.extent.height)
    override suspend fun awaitRetirement() = awaitViewRetirement()
    override fun close() = Unit
}

/**
 * Bridges one toolkit view into the controller for the collector's lifetime: attach a lease
 * once the view's target is available with a drawable extent, detach when the view invalidates
 * it, and detach whatever remains when the collector itself is cancelled. The view owns its
 * native target for its whole lifetime, so its generation identifies the lease.
 */
internal suspend fun bridgeToolkitView(
    controller: DemoSessionController,
    backend: DawnBackend,
    target: DawnSurfaceTarget,
    awaitViewRetirement: suspend () -> Unit,
) = bridgeTargetInto(controller::attach, controller::detach, backend, target, awaitViewRetirement)

/**
 * The bridge core over the controller's two operations, so the collector's decisions are
 * drivable without a GPU session. A fresh target generation is allocated per availability
 * cycle, and StateFlow conflation can fold a remove+re-add into a single available state
 * that hides the invalidation — so a lease from a superseded generation retires as well,
 * in the same emission that attaches its replacement.
 */
internal suspend fun bridgeTargetInto(
    attach: (ToolkitViewLease) -> Unit,
    detach: suspend (Long) -> Unit,
    backend: DawnBackend,
    target: DawnSurfaceTarget,
    awaitViewRetirement: suspend () -> Unit,
) {
    var lease: ToolkitViewLease? = null
    fun retireOutstanding() {
        val outstanding = lease ?: return
        lease = null
        // Never block the UI thread; the controller retires under the frame lock.
        CoroutineScope(Dispatchers.Default).launch {
            try { detach(outstanding.generation) }
            finally { withContext(NonCancellable) { outstanding.awaitRetirement(); outstanding.close() } }
        }
    }
    try {
        target.state.collect { state ->
            if (!state.available || lease?.let { it.generation != state.generation } == true) retireOutstanding()
            if (state.available && state.extent.isPositive && lease == null) {
                val created = ToolkitViewLease(target, backend, awaitViewRetirement)
                lease = created
                attach(created)
            }
        }
    } finally { retireOutstanding() }
}
