@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.demo

import kotlinx.cinterop.objcPtr
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.NativeBridge
import platform.QuartzCore.CAMetalLayer

/** Strongly retains only the layer until the controller acknowledges native retirement. */
internal class IosSurfaceHost(
    override val generation: Long,
    layer: CAMetalLayer,
    extent: PixelExtent,
) : DemoSurfaceLease {
    private val retainedLayer = MutableStateFlow<CAMetalLayer?>(layer)
    private val alive = MutableStateFlow(true)
    private val dimensions = MutableStateFlow(extent)
    val layerPointer: Long get() = retainedLayer.value?.objcPtr()?.toLong() ?: 0L
    override val backend = DawnBackend.Metal
    override val valid get() = alive.value && retainedLayer.value != null
    fun resize(extent: PixelExtent) { dimensions.value = extent }
    fun invalidate() { alive.value = false }
    override fun pixelExtent() = dimensions.value
    override fun createSurface(bridge: NativeBridge, device: DawnDevice): DawnSurface {
        check(valid) { "iOS viewport lease is invalid" }
        return DawnSurface.createMetal(bridge, device.nativeHandle(), layerPointer)
    }
    override fun close() {
        invalidate()
        retainedLayer.value = null
    }
}
