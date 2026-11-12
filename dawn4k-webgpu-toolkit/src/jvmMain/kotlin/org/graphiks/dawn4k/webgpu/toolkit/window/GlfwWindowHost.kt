package org.graphiks.dawn4k.webgpu.toolkit.window

import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.toolkit.glfw.DawnMainThreadLoop
import org.graphiks.dawn4k.toolkit.glfw.DawnWindow
import org.graphiks.dawn4k.toolkit.glfw.DawnWindowEvent
import org.graphiks.dawn4k.toolkit.glfw.DawnWindowOptions
import org.graphiks.dawn4k.toolkit.glfw.openDawnWindow
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.surface.Window
import org.graphiks.webgpu.toolkit.surface.WindowConfig
import org.graphiks.webgpu.toolkit.surface.WindowHost
import org.graphiks.webgpu.toolkit.surface.WindowRuntime

/**
 * GLFW-based implementation of [WindowHost] that creates native windows using GLFW.
 * 
 * This host adapts the dawn4k-toolkit-glfw [DawnWindow] to the webgpu-toolkit-surface
 * [Window] contract, translating between the two event systems and lifecycle models.
 * 
 * This implementation is JVM-only as it depends on GLFW through LWJGL.
 * 
 * @param runtime Whether this host owns the windows it creates ([WindowRuntime.Owned]) or
 *                borrows them from an embedder ([WindowRuntime.Borrowed]).
 */
class GlfwWindowHost(
    override val runtime: WindowRuntime = WindowRuntime.Owned,
) : WindowHost {
    
    private val dawnContext: DawnContext = DawnContext.create()

    override suspend fun open(config: WindowConfig): Window {
        return DawnMainThreadLoop.run { main ->
            createGlfwWindow(config, runtime, dawnContext, main)
        }
    }
    
    /**
     * Detects the platform type based on the current OS.
     */
    private fun detectPlatformType(): org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        return when {
            osName.contains("mac") -> org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType.MetalLayer
            osName.contains("win") -> org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType.Win32
            osName.contains("linux") -> org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType.X11
            else -> org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType.MetalLayer
        }
    }
}

/**
 * Suspend function to create a GLFW window and run a block with it.
 * This is a convenience function that handles the lifecycle correctly.
 */
suspend fun <R> withGlfwWindow(
    host: GlfwWindowHost,
    config: WindowConfig = WindowConfig(),
    block: suspend (Window) -> R,
): R {
    val window = host.open(config)
    try {
        return block(window)
    } finally {
        if (window.runtime == WindowRuntime.Owned) {
            window.close()
        }
    }
}
