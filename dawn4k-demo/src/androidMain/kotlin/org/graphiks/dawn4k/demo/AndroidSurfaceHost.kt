package org.graphiks.dawn4k.demo

import android.view.Surface
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.NativeBridge

internal class AndroidSurfaceHost(
    override val generation: Long,
    surface: Surface,
    extent: PixelExtent,
) : DemoSurfaceLease {
    private val alive = MutableStateFlow(true)
    private val dimensions = MutableStateFlow(extent)
    private val window = MutableStateFlow(0L)
    init {
        require(surface.isValid) { "Android surface is invalid" }
        window.value = AndroidNativeWindow.acquire(surface)
        check(window.value != 0L) { "ANativeWindow_fromSurface failed" }
    }
    val nativeWindow get() = window.value
    override val backend = DawnBackend.Vulkan
    override val valid get() = alive.value && nativeWindow != 0L
    fun invalidate() { alive.value = false }
    fun resize(width: Int, height: Int) { dimensions.value = PixelExtent(width, height) }
    override fun pixelExtent() = dimensions.value
    override fun createSurface(bridge: NativeBridge, device: DawnDevice): DawnSurface {
        check(valid) { "Android viewport lease is invalid" }
        return DawnSurface.createAndroid(bridge, device.nativeHandle(), nativeWindow)
    }
    override fun close() {
        invalidate()
        val address = window.getAndUpdate { 0L }
        if (address != 0L) AndroidNativeWindow.release(address)
    }
}
