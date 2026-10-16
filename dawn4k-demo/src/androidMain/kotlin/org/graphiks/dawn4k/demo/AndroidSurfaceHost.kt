package org.graphiks.dawn4k.demo

import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.toolkit.surface.*

internal class AndroidSurfaceHost(
    override val generation: Long,
    surface: Surface,
    extent: PixelExtent,
) : DemoSurfaceLease {
    private val alive = MutableStateFlow(true)
    private val dimensions = MutableStateFlow(extent)
    private val states = MutableStateFlow(DawnTargetState(generation,
        org.graphiks.dawn4k.toolkit.surface.PixelExtent(extent.width, extent.height), true))
    private val retainedTarget = RetiringDemoTarget(androidSurfaceTarget(surface, DawnPlatformOwners.androidMain(), states))
    override val target: DawnSurfaceTarget get() = retainedTarget
    override suspend fun awaitRetirement() = retainedTarget.awaitRetirement()
    init { require(surface.isValid) { "Android surface is invalid" } }
    override val backend = DawnBackend.Vulkan
    override val valid get() = alive.value
    fun invalidate() { alive.value = false; states.value = states.value.copy(available = false) }
    fun resize(width: Int, height: Int) {
        dimensions.value = PixelExtent(width, height)
        states.value = states.value.copy(extent = org.graphiks.dawn4k.toolkit.surface.PixelExtent(width, height))
    }
    override fun pixelExtent() = dimensions.value
    override fun close() {
        retainedTarget.requireRetired()
        invalidate()
    }
}
