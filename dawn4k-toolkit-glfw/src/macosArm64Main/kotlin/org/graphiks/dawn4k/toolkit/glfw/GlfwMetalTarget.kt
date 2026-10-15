@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.glfw.internal

import cnames.structs.GLFWwindow
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.toolkit.glfw.DawnMainThreadLoop
import org.graphiks.dawn4k.toolkit.glfw.c.*
import org.graphiks.dawn4k.toolkit.surface.*
import platform.AppKit.NSWindow
import platform.CoreFoundation.*
import platform.QuartzCore.*
import platform.posix.pthread_main_np

internal actual fun requireGlfwMainThread() {
    check(pthread_main_np() != 0) { "GLFW requires the process main thread" }
}

internal actual fun nativeSurfaceTarget(
    window: CPointer<GLFWwindow>, platform: GlfwPlatform, main: DawnMainThreadLoop,
    states: MutableStateFlow<DawnTargetState>, retention: DawnTargetRetention,
): DawnSurfaceTarget {
    check(platform == GlfwPlatform.Cocoa) { "macOS requires the active GLFW Cocoa platform" }
    val address = checkNotNull(dawn_glfw_cocoa_window(window)) { "GLFW Cocoa window is null" }
    val nativeWindow = interpretObjCPointer<NSWindow>(address.rawValue)
    val view = checkNotNull(nativeWindow.contentView)
    val layer = CAMetalLayer()
    view.wantsLayer = true
    view.layer = layer
    layer.frame = view.bounds
    layer.contentsScale = nativeWindow.backingScaleFactor
    layer.autoresizingMask = kCALayerWidthSizable or kCALayerHeightSizable
    val references = mutableListOf<CFTypeRef>()
    return DawnSurfaceTargets.metalLayer(layer.objcPtr().toLong(), main.owner, states, object : DawnTargetRetention {
        override suspend fun retain() {
            val pointer = checkNotNull(interpretCPointer<ByteVar>(layer.objcPtr()))
            val reference = checkNotNull(CFRetain(pointer))
            try { retention.retain(); references += reference }
            catch (failure: Throwable) { CFRelease(reference); throw failure }
        }
        override suspend fun retire() {
            CFRelease(references.removeAt(references.lastIndex))
            retention.retire()
        }
    })
}
