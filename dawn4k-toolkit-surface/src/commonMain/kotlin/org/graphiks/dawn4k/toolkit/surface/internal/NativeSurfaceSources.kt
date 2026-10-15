package org.graphiks.dawn4k.toolkit.surface.internal

import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnSurfaceReference
import org.graphiks.dawn4k.native.*
import org.graphiks.kffi.*

internal sealed class NativeSurfaceSource {
    abstract fun chain(allocator: MemoryAllocator): WGPUChainedStruct

    fun createSurface(context: DawnContext): DawnSurfaceReference = memoryScope { allocator ->
        val instance = context.nativeBridge().instanceHandle()
        check(instance != 0L) { "Dawn context is closed" }
        val descriptor = WGPUSurfaceDescriptor.allocate(allocator).apply {
            nextInChain = chain(allocator)
            label.data = null; label.length = 0uL
        }
        val surface = wgpuInstanceCreateSurface(WGPUInstance(NativeAddress(instance)), descriptor)
            ?: error("Dawn could not create a surface")
        try { context.adoptSurface(surface.handler.rawValue) }
        catch (failure: Throwable) { wgpuSurfaceRelease(surface); throw failure }
    }

    class Metal(private val layer: Long) : NativeSurfaceSource() {
        override fun chain(allocator: MemoryAllocator) = WGPUSurfaceSourceMetalLayer.allocate(allocator).apply {
            chain.next = null; chain.sType = WGPUSType_SurfaceSourceMetalLayer; this.layer = NativeAddress(this@Metal.layer)
        }.chain
    }
    class Win32(private val hwnd: Long, private val hinstance: Long) : NativeSurfaceSource() {
        override fun chain(allocator: MemoryAllocator) = WGPUSurfaceSourceWindowsHWND.allocate(allocator).apply {
            chain.next = null; chain.sType = WGPUSType_SurfaceSourceWindowsHWND
            hwnd = NativeAddress(this@Win32.hwnd); hinstance = NativeAddress(this@Win32.hinstance)
        }.chain
    }
    class X11(private val display: Long, private val window: ULong) : NativeSurfaceSource() {
        override fun chain(allocator: MemoryAllocator) = WGPUSurfaceSourceXlibWindow.allocate(allocator).apply {
            chain.next = null; chain.sType = WGPUSType_SurfaceSourceXlibWindow
            display = NativeAddress(this@X11.display); window = this@X11.window
        }.chain
    }
    class Wayland(private val display: Long, private val surface: Long) : NativeSurfaceSource() {
        override fun chain(allocator: MemoryAllocator) = WGPUSurfaceSourceWaylandSurface.allocate(allocator).apply {
            chain.next = null; chain.sType = WGPUSType_SurfaceSourceWaylandSurface
            display = NativeAddress(this@Wayland.display); surface = NativeAddress(this@Wayland.surface)
        }.chain
    }
    class AndroidWindow(private val window: () -> Long) : NativeSurfaceSource() {
        override fun chain(allocator: MemoryAllocator) = WGPUSurfaceSourceAndroidNativeWindow.allocate(allocator).apply {
            chain.next = null; chain.sType = WGPUSType_SurfaceSourceAndroidNativeWindow
            val address = this@AndroidWindow.window()
            check(address != 0L) { "Android native window is retired" }
            window = NativeAddress(address)
        }.chain
    }
}
