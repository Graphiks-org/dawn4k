@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.demo

import kotlinx.cinterop.objcPtr
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.DawnBackend
import platform.QuartzCore.CAMetalLayer
import org.graphiks.dawn4k.toolkit.surface.*

/** Strongly retains only the layer until the controller acknowledges native retirement. */
internal class IosSurfaceHost(
    override val generation: Long,
    layer: CAMetalLayer,
    extent: PixelExtent,
) : DemoSurfaceLease {
    private val retainedLayer = MutableStateFlow<CAMetalLayer?>(layer)
    private val alive = MutableStateFlow(true)
    private val dimensions = MutableStateFlow(extent)
    private val states = MutableStateFlow(DawnTargetState(generation,
        org.graphiks.dawn4k.toolkit.surface.PixelExtent(extent.width, extent.height), true))
    private val retention = CountingTargetRetention()
    private val retainedTarget = metalLayerTarget(layer, states, retention = retention)
    override val target: DawnSurfaceTarget get() = retainedTarget
    override suspend fun awaitRetirement() = retention.awaitRetirement()
    val layerPointer: Long get() = retainedLayer.value?.objcPtr()?.toLong() ?: 0L
    override val backend = DawnBackend.Metal
    override val valid get() = alive.value && retainedLayer.value != null
    fun resize(extent: PixelExtent) {
        dimensions.value = extent
        states.value = states.value.copy(extent = org.graphiks.dawn4k.toolkit.surface.PixelExtent(extent.width, extent.height))
    }
    fun invalidate() { alive.value = false; states.value = states.value.copy(available = false) }
    override fun pixelExtent() = dimensions.value
    override fun close() {
        retention.requireRetired()
        invalidate()
        retainedLayer.value = null
    }
}
