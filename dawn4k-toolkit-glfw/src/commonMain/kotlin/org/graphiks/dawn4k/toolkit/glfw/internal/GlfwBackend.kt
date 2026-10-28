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

/**
 * A platform GLFW binding. All entry points run on the process main thread.
 *
 * Close contract: [requestClose] marks the window's target unavailable and sets the platform
 * should-close flag, but never emits [DawnWindowEvent.CloseRequested]. The window-close button
 * callback still emits it (via `GlfwWindowTarget.onClose`); `DawnWindow.requestClose` is the
 * sole emitter on the programmatic path. The `WindowSurfaceState` terminal guard then drops
 * every later event.
 */
internal interface GlfwBackend {
    fun initialize()
    fun requireInitialized()
    fun terminate()
    fun createWindow(options: DawnWindowOptions): Long
    fun destroyWindow(handle: Long)
    fun resizeWindow(handle: Long, width: Int, height: Int) {}
    fun pollEvents()
    fun pixelExtent(handle: Long): PixelExtent
    fun surfaceTarget(handle: Long): DawnSurfaceTarget
    fun setEventSink(handle: Long, sink: (DawnWindowEvent) -> Unit) {}
    /** Invalidates the target and sets the platform should-close flag; does not emit [DawnWindowEvent.CloseRequested]. */
    fun requestClose(handle: Long) {}
    suspend fun awaitTargetRetirement(handle: Long) {}
}

internal expect fun createGlfwBackend(main: DawnMainThreadLoop): GlfwBackend
internal expect fun requireGlfwMainThread()
internal expect fun pauseGlfwMainThread()
internal expect fun writeGlfwStandardError(message: String)
