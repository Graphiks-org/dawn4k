package org.graphiks.dawn4k.webgpu.toolkit.window

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.toolkit.glfw.DawnMainThreadLoop
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
 * The [DawnContext] backing the windows this host opens is created lazily on the first
 * [open] and released by [close]; a host that never opened a window never creates one.
 *
 * @param runtime Whether this host owns the windows it creates ([WindowRuntime.Owned]) or
 *                borrows them from an embedder ([WindowRuntime.Borrowed]).
 */
class GlfwWindowHost(
    override val runtime: WindowRuntime = WindowRuntime.Owned,
) : WindowHost, AutoCloseable {

    private val contextMutex = Mutex()

    @Volatile
    private var dawnContext: DawnContext? = null

    override suspend fun open(config: WindowConfig): Window {
        val context = contextMutex.withLock {
            dawnContext ?: DawnContext.create().also { dawnContext = it }
        }
        return DawnMainThreadLoop.run { main ->
            createGlfwWindow(config, runtime, context, main)
        }
    }

    /**
     * Releases the [DawnContext] this host created for its windows.
     *
     * Must not be called while a window opened by this host is still in use — the
     * window's surface target borrows that context. Idempotent: closing a host that
     * never opened a window is a no-op.
     */
    override fun close() {
        val context = dawnContext ?: return
        dawnContext = null
        runCatching { context.close() }
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
