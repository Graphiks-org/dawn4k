package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.flow.StateFlow
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnSurfaceReference

data class PixelExtent(val width: Int, val height: Int) {
    init { require(width >= 0 && height >= 0) { "pixel dimensions must be nonnegative" } }
    val isPositive: Boolean get() = width > 0 && height > 0
}

/** Use a fresh generation whenever the native target identity changes. */
data class DawnTargetState(val generation: Long, val extent: PixelExtent, val available: Boolean)

interface DawnSurfaceTarget {
    val state: StateFlow<DawnTargetState>
    /** Retention must survive cancellation at a platform dispatcher return boundary. */
    suspend fun retain(): DawnTargetLease
}

interface DawnTargetLease {
    val state: StateFlow<DawnTargetState>
    fun createSurface(context: DawnContext): DawnSurfaceReference
    /** Acknowledged on the platform owner, after every associated surface reference has retired. */
    suspend fun retire()
}
