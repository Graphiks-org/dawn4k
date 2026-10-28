package org.graphiks.dawn4k.toolkit.glfw.internal

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import org.graphiks.dawn4k.toolkit.glfw.*
import org.graphiks.dawn4k.toolkit.surface.*
import org.graphiks.kffi.objc.CALayer
import org.graphiks.kffi.objc.CAAutoresizingMask
import org.graphiks.kffi.objc.NSView
import org.graphiks.kffi.objc.ObjCRuntime
import org.graphiks.kffi.objc.PlatformAvailability
import org.graphiks.kffi.win32.generated.GetModuleHandleW
import org.graphiks.kffi.win32.generated.init
import org.lwjgl.glfw.GLFW.glfwGetPlatform
import org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window
import org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandDisplay
import org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandWindow

/** Adds the JVM platform surface-target construction to the shared [GlfwWindowTarget] machinery. */
internal class JvmGlfwTarget private constructor(
    private val handle: Long,
    private val main: DawnMainThreadLoop,
    extent: PixelExtent,
    private val cocoa: CocoaLayerReference,
) : GlfwWindowTarget(
    extent,
    // The retention hooks mirror the platform targets: only Cocoa carries an object reference.
    onRetain = cocoa::retain,
    onRetire = cocoa::release,
) {
    internal constructor(handle: Long, main: DawnMainThreadLoop, extent: PixelExtent) :
        this(handle, main, extent, CocoaLayerReference())

    val target: DawnSurfaceTarget by lazy {
        when (glfwPlatform(glfwGetPlatform())) {
            GlfwPlatform.Cocoa -> {
                val layer = installMetalLayer(glfwGetCocoaWindow(handle))
                cocoa.layer = layer
                DawnSurfaceTargets.metalLayer(layer.address(), main.owner, states, retention)
            }
            GlfwPlatform.Win32 -> {
                init()
                val instance = GetModuleHandleW(MemorySegment.NULL).address()
                DawnSurfaceTargets.win32(glfwGetWin32Window(handle), instance, main.owner, states, retention)
            }
            GlfwPlatform.X11 -> DawnSurfaceTargets.x11(glfwGetX11Display(), glfwGetX11Window(handle).toULong(), main.owner, states, retention)
            GlfwPlatform.Wayland -> DawnSurfaceTargets.wayland(glfwGetWaylandDisplay(), glfwGetWaylandWindow(handle), main.owner, states, retention)
        }
    }
}

/** Holds the JVM Cocoa backing layer so the shared retention hooks can keep it alive. */
private class CocoaLayerReference {
    /** Installed when the Cocoa target builds its backing CAMetalLayer. */
    var layer: MemorySegment? = null
    fun retain() { layer?.let { ObjCRuntime.msgSend(null, it, ObjCRuntime.sel("retain")) } }
    fun release() { layer?.let { ObjCRuntime.msgSend(null, it, ObjCRuntime.sel("release")) } }
}

/** This is a toolkit-owned NSView; its backing layer lives until its GLFW window is destroyed. */
private val quartzCore by lazy {
    SymbolLookup.libraryLookup("/System/Library/Frameworks/QuartzCore.framework/QuartzCore", Arena.global())
}

@OptIn(PlatformAvailability::class)
private fun installMetalLayer(window: Long): MemorySegment {
    check(quartzCore.find("CACurrentMediaTime").isPresent) { "QuartzCore is unavailable" }
    val viewAddress = ObjCRuntime.msgSend(ValueLayout.ADDRESS, MemorySegment.ofAddress(window), ObjCRuntime.sel("contentView")) as MemorySegment
    val view = NSView(viewAddress)
    val allocated = ObjCRuntime.msgSend(ValueLayout.ADDRESS, ObjCRuntime.getClass("CAMetalLayer"), ObjCRuntime.sel("alloc")) as MemorySegment
    val layer = ObjCRuntime.msgSend(ValueLayout.ADDRESS, allocated, ObjCRuntime.sel("init")) as MemorySegment
    check(layer != MemorySegment.NULL) { "CAMetalLayer initialization failed" }
    try {
        view.setWantsLayer(true)
        ObjCRuntime.msgSend(null, viewAddress, ObjCRuntime.sel("setLayer:"), layer)
        CALayer(layer).apply {
            setFrame(view.bounds())
            setAutoresizingMask(CAAutoresizingMask.kCALayerWidthSizable + CAAutoresizingMask.kCALayerHeightSizable)
        }
        return layer
    } finally { ObjCRuntime.msgSend(null, layer, ObjCRuntime.sel("release")) }
}
