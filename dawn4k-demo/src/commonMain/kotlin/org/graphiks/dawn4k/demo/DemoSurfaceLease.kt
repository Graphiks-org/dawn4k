package org.graphiks.dawn4k.demo

import org.graphiks.dawn4k.DawnBackend
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.toolkit.surface.CountingTargetRetention
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.toolkit.surface.DawnTargetState
import org.graphiks.dawn4k.toolkit.surface.PixelExtent as ToolkitPixelExtent

/** Retains the native target until the controller acknowledges surface cleanup. */
internal interface DemoSurfaceLease : AutoCloseable {
    val generation: Long
    val backend: DawnBackend
    val valid: Boolean
    val target: DawnSurfaceTarget
    suspend fun awaitRetirement()
    fun pixelExtent(): PixelExtent
}

/** Compatibility adapter; native host lifetime transfers to the lease. */
internal class DesktopSurfaceLease(
    override val generation: Long,
    private val host: SurfaceHost,
) : DemoSurfaceLease {
    // Seeded at construction: every desktop host's pixelSize is a plain query marshalled
    // to its owner thread, so no post-construction call is needed to obtain the extent.
    private val states = MutableStateFlow(
        host.pixelSize().let { (width, height) ->
            DawnTargetState(generation, ToolkitPixelExtent(width.coerceAtLeast(0), height.coerceAtLeast(0)), true)
        })
    private val retention = CountingTargetRetention()
    override val target: DawnSurfaceTarget = host.createTarget(states, retention)
    override val backend get() = host.backend
    override val valid get() = states.value.available
    override fun pixelExtent(): PixelExtent {
        check(valid) { "the viewport lease is invalid" }
        val (width, height) = host.pixelSize()
        states.value = states.value.copy(extent = ToolkitPixelExtent(width.coerceAtLeast(0), height.coerceAtLeast(0)))
        return PixelExtent(width, height)
    }
    override fun close() {
        retention.requireRetired()
        if (states.value.available) {
            states.value = states.value.copy(available = false)
            host.close()
        }
    }
    override suspend fun awaitRetirement() = retention.awaitRetirement()
}
