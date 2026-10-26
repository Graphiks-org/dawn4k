package org.graphiks.dawn4k.toolkit.glfw.internal

import org.graphiks.dawn4k.toolkit.SharedRetirement
import org.graphiks.dawn4k.toolkit.glfw.DawnGlfwRuntimeOwnership

/** Accessed on the process main thread; all windows share the initialization lease. */
internal class GlfwRuntime(private val backend: GlfwBackend) {
    private var references = 0
    private var ownership: DawnGlfwRuntimeOwnership? = null
    val isActive: Boolean get() = references > 0

    fun acquire(mode: DawnGlfwRuntimeOwnership): Lease {
        if (references == 0) {
            if (mode == DawnGlfwRuntimeOwnership.Owned) backend.initialize() else backend.requireInitialized()
            ownership = mode
        } else check(ownership == mode) { "active GLFW windows must use the same runtime ownership" }
        references++
        return Lease()
    }

    inner class Lease internal constructor() {
        // SharedRetirement starts the body undispatched on the triggering thread, so
        // backend.terminate() and the reference updates stay on the process main thread.
        private val retirement = SharedRetirement {
            if (references == 1 && ownership == DawnGlfwRuntimeOwnership.Owned) backend.terminate()
            references--
            if (references == 0) ownership = null
        }
        suspend fun retire() = retirement.retireOnce()
    }
}
