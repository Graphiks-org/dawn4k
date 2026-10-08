package org.graphiks.dawn4k.demo

import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.NativeBridge
import kotlinx.coroutines.flow.MutableStateFlow

/** Retains the native target until the controller acknowledges surface cleanup. */
internal interface DemoSurfaceLease : AutoCloseable {
    val generation: Long
    val backend: DawnBackend
    val valid: Boolean
    fun pixelExtent(): PixelExtent
    fun createSurface(bridge: NativeBridge, device: DawnDevice): DawnSurface
}

/** Compatibility adapter; native host lifetime transfers to the lease. */
internal class DesktopSurfaceLease(
    override val generation: Long,
    private val host: SurfaceHost,
) : DemoSurfaceLease {
    private val alive = MutableStateFlow(true)
    override val backend get() = host.backend
    override val valid get() = alive.value
    override fun pixelExtent(): PixelExtent {
        check(valid) { "the viewport lease is invalid" }
        val (width, height) = host.pixelSize()
        return PixelExtent(width, height)
    }
    override fun createSurface(bridge: NativeBridge, device: DawnDevice): DawnSurface {
        check(valid) { "the viewport lease is invalid" }
        return host.createSurface(bridge, device.nativeHandle())
    }
    override fun close() {
        if (alive.compareAndSet(true, false)) host.close()
    }
}
