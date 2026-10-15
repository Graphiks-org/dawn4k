package org.graphiks.dawn4k.toolkit.glfw.internal

import org.graphiks.dawn4k.toolkit.glfw.*
import org.graphiks.dawn4k.toolkit.surface.*

internal enum class GlfwPlatform { Win32, Cocoa, Wayland, X11 }
internal fun glfwPlatform(value: Int): GlfwPlatform = when (value) {
    0x00060001 -> GlfwPlatform.Win32
    0x00060002 -> GlfwPlatform.Cocoa
    0x00060003 -> GlfwPlatform.Wayland
    0x00060004 -> GlfwPlatform.X11
    else -> throw UnsupportedOperationException("unsupported GLFW platform: $value")
}

internal interface GlfwBackend {
    fun initialize()
    fun requireInitialized()
    fun terminate()
    fun createWindow(options: DawnWindowOptions): Long
    fun destroyWindow(handle: Long)
    fun pollEvents()
    fun pixelExtent(handle: Long): PixelExtent
    fun surfaceTarget(handle: Long): DawnSurfaceTarget
    fun setEventSink(handle: Long, sink: (DawnWindowEvent) -> Unit) {}
    fun requestClose(handle: Long) {}
    suspend fun awaitTargetRetirement(handle: Long) {}
}

internal expect fun createGlfwBackend(main: DawnMainThreadLoop): GlfwBackend
internal expect fun requireGlfwMainThread()
internal expect fun pauseGlfwMainThread()
