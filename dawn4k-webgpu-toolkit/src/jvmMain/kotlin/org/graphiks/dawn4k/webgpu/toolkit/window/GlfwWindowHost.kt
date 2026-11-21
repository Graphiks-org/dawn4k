package org.graphiks.dawn4k.webgpu.toolkit.window

import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.dawn4k.webgpu.toolkit.glfw.DawnMainThreadLoop
import org.graphiks.dawn4k.webgpu.toolkit.glfw.internal.GlfwBackend
import org.graphiks.dawn4k.webgpu.toolkit.surface.internal.SurfaceFactory
import org.graphiks.webgpu.toolkit.surface.Window
import org.graphiks.webgpu.toolkit.surface.WindowConfig
import org.graphiks.webgpu.toolkit.surface.WindowHost
import org.graphiks.webgpu.toolkit.surface.WindowRuntime

/**
 * GLFW-based implementation of [WindowHost] that creates native windows using GLFW.
 *
 * This host adapts the [DawnWindow] to the toolkit [Window] contract, translating
 * between the two event systems and lifecycle models.
 *
 * This implementation is JVM-only as it depends on GLFW through LWJGL.
 *
 * The [DawnContext] backing the windows this host opens is created lazily on the first
 * [open] and released when [serve] returns; a host that never opened a window never creates one.
 *
 * The host serves: [serve] runs the GLFW loop on the process main thread — macOS JVM launchers
 * need `-XstartOnFirstThread` — and [open] returns live windows only while a serve is running.
 * Serving and [DawnMainThreadLoop.run] share the one-loop-per-process gate. [close] asks a
 * running serve to end; the served thread shuts every remaining window down, releases the
 * context, and then serving returns.
 *
 * @param runtime Whether this host owns the windows it creates ([WindowRuntime.Owned]) or
 *                borrows them from an embedder ([WindowRuntime.Borrowed]).
 */
class GlfwWindowHost(
    override val runtime: WindowRuntime = WindowRuntime.Owned,
) : WindowHost, AutoCloseable {

    @Volatile
    private var serving: DawnMainThreadLoop? = null

    /** Whether the host is currently serving; [open] requires it. */
    val isServing: Boolean get() = serving != null

    /** The extension every window this host opens binds its session to; one per host. */
    internal val extension = Dawn4kWebGpuToolkitExtension()

    /** The surface factory binding retained targets to native surfaces; tests swap it. */
    internal var surfaceFactory: SurfaceFactory = SurfaceFactory.Native

    /**
     * Serves the process main thread until [close]. Blocking: it returns only once the stop
     * drained every remaining window.
     */
    fun serve() = serve(null)

    internal fun serve(backend: GlfwBackend?) {
        val loop = DawnMainThreadLoop.createForServing(backend)
        serving = loop
        try {
            loop.serve()
        } finally {
            serving = null
        }
    }

    /**
     * Opens a live window on the served loop, or fails when the host is not serving — a loop
     * without a serve would shut the window down before the caller could use it.
     */
    override suspend fun open(config: WindowConfig): Window {
        val loop = serving ?: throw IllegalStateException(
            "GlfwWindowHost is not serving; call serve() on the process main thread before opening windows",
        )
        return loop.owner.onOwner {
            createGlfwWindow(config, runtime, loop, surfaceFactory, extension)
        }
    }

    /** Asks a running serve to end; harmless when the host never served. Idempotent. */
    override fun close() {
        serving?.stopServing()
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
