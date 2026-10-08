package org.graphiks.dawn4k.demo

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import org.graphiks.dawn4k.NativeBridge
import org.graphiks.kffi.objc.CALayer
import org.graphiks.kffi.objc.CAAutoresizingMask
import org.graphiks.kffi.objc.NSView
import org.graphiks.kffi.objc.ObjCRuntime
import org.graphiks.kffi.objc.PlatformAvailability

/** AppKit attachment; the common Dawn lifecycle lives in [DawnSurface]. */
@OptIn(PlatformAvailability::class)
object MetalSurface {
    fun create(bridge: NativeBridge, deviceHandle: Long, metalLayerPtr: Long): DawnSurface =
        DawnSurface.createMetal(bridge, deviceHandle, metalLayerPtr)

    fun createCaMetalLayer(): MemorySegment {
        val layerClass = ObjCRuntime.getClass("CAMetalLayer")
        val allocated = ObjCRuntime.msgSend(
            ValueLayout.ADDRESS, layerClass, ObjCRuntime.sel("alloc")
        ) as MemorySegment
        require(allocated != MemorySegment.NULL) { "CAMetalLayer alloc failed" }
        val initialized = ObjCRuntime.msgSend(
            ValueLayout.ADDRESS, allocated, ObjCRuntime.sel("init")
        ) as MemorySegment
        require(initialized != MemorySegment.NULL) { "CAMetalLayer init failed" }
        return initialized
    }

    /** Installs a layer below Compose without replacing its backing layer. */
    fun metalLayerOf(nsViewPtr: Long): Long = ObjCRuntime.autoreleasePool {
        require(nsViewPtr != 0L) { "the NSView pointer is null" }
        val view = NSView(MemorySegment.ofAddress(nsViewPtr))
        view.setWantsLayer(true)
        val layer = view.layer()
        require(layer != MemorySegment.NULL) { "setWantsLayer(true) did not create a layer" }
        val metalLayer = createCaMetalLayer()
        val overlay = CALayer(metalLayer)
        overlay.setFrame(view.bounds())
        overlay.setAutoresizingMask(
            CAAutoresizingMask.kCALayerWidthSizable + CAAutoresizingMask.kCALayerHeightSizable
        )
        overlay.setZPosition(-1.0)
        CALayer(layer).addSublayer(metalLayer)
        metalLayer.address()
    }
}
