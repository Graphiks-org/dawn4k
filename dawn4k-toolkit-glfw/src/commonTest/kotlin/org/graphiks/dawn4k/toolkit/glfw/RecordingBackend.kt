package org.graphiks.dawn4k.toolkit.glfw

import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnSurfaceReference
import org.graphiks.dawn4k.toolkit.glfw.internal.*
import org.graphiks.dawn4k.toolkit.surface.*

internal class RecordingBackend : GlfwBackend {
    var initialized = false
    var initializations = 0
    var terminations = 0
    var creates = 0
    var destroys = 0
    var extentFailure: Throwable? = null
    var beforeRetirement: suspend () -> Unit = {}
    var beforeTermination: () -> Unit = {}
    override fun initialize() { initialized = true; initializations++ }
    override fun requireInitialized() { check(initialized) }
    override fun terminate() { beforeTermination(); initialized = false; terminations++ }
    override fun createWindow(options: DawnWindowOptions): Long { creates++; return creates.toLong() }
    override fun destroyWindow(handle: Long) { destroys++ }
    override fun pollEvents() {}
    override suspend fun awaitTargetRetirement(handle: Long) { beforeRetirement() }
    override fun pixelExtent(handle: Long): PixelExtent {
        extentFailure?.let { throw it }
        return PixelExtent(300, 150)
    }
    override fun surfaceTarget(handle: Long): DawnSurfaceTarget = object : DawnSurfaceTarget {
        override val state = MutableStateFlow(DawnTargetState(handle, pixelExtent(handle), true))
        override suspend fun retain(): DawnTargetLease = error("fake window fixture has no native surface")
    }
}
