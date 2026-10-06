package org.graphiks.dawn4k.demo

import java.lang.foreign.MemorySegment
import org.graphiks.kffi.objc.CALayer
import org.graphiks.kffi.objc.NSView
import org.graphiks.kffi.objc.NSWindow
import org.graphiks.kffi.objc.ObjCRuntime
import org.graphiks.kffi.objc.PlatformAvailability
import kotlin.math.roundToInt
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.NativeBridge

/** Owns the particle layer below Compose, not its backing layer. Outlives the Dawn surface. */
@OptIn(PlatformAvailability::class)
internal class MetalLayerHost private constructor(
    private val view: NSView,
    val layerPtr: Long,
) : SurfaceHost {
    private var closed = false

    companion object {
        fun attach(nsViewPtr: Long): MetalLayerHost = onAppKitThread {
            val view = NSView(MemorySegment.ofAddress(nsViewPtr))
            val layer = MetalSurface.metalLayerOf(nsViewPtr)
            ObjCRuntime.msgSend(null, view.ptr, ObjCRuntime.sel("retain"))
            MetalLayerHost(view, layer)
        }
    }

    /** Pixel dimensions, refreshed on AppKit so resize and monitor DPI changes are observed. */
    override val backend: DawnBackend get() = DawnBackend.Metal

    override fun createSurface(bridge: NativeBridge, deviceHandle: Long): DawnSurface =
        MetalSurface.create(bridge, deviceHandle, layerPtr)

    override fun pixelSize(): Pair<Int, Int> = onAppKitThread {
        check(!closed) { "the Metal layer host is closed" }
        val window = view.window()
        val scale = if (window == MemorySegment.NULL) 1.0 else NSWindow(window).backingScaleFactor()
        val bounds = view.bounds()
        val layer = CALayer(MemorySegment.ofAddress(layerPtr))
        layer.setFrame(bounds)
        layer.setContentsScale(scale)
        (bounds.size.width * scale).roundToInt() to (bounds.size.height * scale).roundToInt()
    }

    override fun close() = onAppKitThread {
        if (!closed) {
            closed = true
            val layer = MemorySegment.ofAddress(layerPtr)
            CALayer(layer).removeFromSuperlayer()
            ObjCRuntime.msgSend(null, layer, ObjCRuntime.sel("release"))
            ObjCRuntime.msgSend(null, view.ptr, ObjCRuntime.sel("release"))
        }
    }
}
