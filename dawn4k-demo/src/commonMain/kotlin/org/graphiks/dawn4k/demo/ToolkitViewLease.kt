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
) {
    var lease: ToolkitViewLease? = null
    fun retireOutstanding() {
        val outstanding = lease ?: return
        lease = null
        // Never block the UI thread; the controller retires under the frame lock.
        CoroutineScope(Dispatchers.Default).launch {
            try { controller.detach(outstanding.generation) }
            finally { withContext(NonCancellable) { outstanding.awaitRetirement(); outstanding.close() } }
        }
    }
    try {
        target.state.collect { state ->
            if (!state.available) retireOutstanding()
            else if (state.extent.isPositive && lease == null) {
                val created = ToolkitViewLease(target, backend, awaitViewRetirement)
                lease = created
                controller.attach(created)
            }
        }
    } finally { retireOutstanding() }
}
