package org.graphiks.dawn4k.toolkit.glfw.internal

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

internal class JvmGlfwTarget(
    private val handle: Long,
    private val main: DawnMainThreadLoop,
    extent: PixelExtent,
) {
    private val states = MutableStateFlow(DawnTargetState.fresh(extent, true))
    private val references = MutableStateFlow(0)
    var sink: (DawnWindowEvent) -> Unit = {}
    fun event(event: DawnWindowEvent) = sink(event)
    fun resize(extent: PixelExtent) { states.value = states.value.copy(extent = extent) }
    fun invalidate() { states.value = states.value.copy(available = false) }
    suspend fun awaitRetirement() { invalidate(); references.first { it == 0 } }

    val target: DawnSurfaceTarget by lazy {
        when (glfwPlatform(glfwGetPlatform())) {
            GlfwPlatform.Cocoa -> {
                val layer = installMetalLayer(glfwGetCocoaWindow(handle))
                DawnSurfaceTargets.metalLayer(layer.address(), main.owner, states, retention(layer))
            }
            GlfwPlatform.Win32 -> {
                init()
                val instance = GetModuleHandleW(MemorySegment.NULL).address()
                DawnSurfaceTargets.win32(glfwGetWin32Window(handle), instance, main.owner, states, retention())
            }
            GlfwPlatform.X11 -> DawnSurfaceTargets.x11(glfwGetX11Display(), glfwGetX11Window(handle).toULong(), main.owner, states, retention())
            GlfwPlatform.Wayland -> DawnSurfaceTargets.wayland(glfwGetWaylandDisplay(), glfwGetWaylandWindow(handle), main.owner, states, retention())
        }
    }

    private fun retention(layer: MemorySegment? = null) = object : DawnTargetRetention {
        override suspend fun retain() {
            layer?.let { ObjCRuntime.msgSend(null, it, ObjCRuntime.sel("retain")) }
            references.value++
        }
        override suspend fun retire() {
            check(references.value > 0)
            layer?.let { ObjCRuntime.msgSend(null, it, ObjCRuntime.sel("release")) }
            references.value--
        }
    }
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
