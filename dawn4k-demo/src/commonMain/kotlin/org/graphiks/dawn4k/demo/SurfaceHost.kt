package org.graphiks.dawn4k.demo

import org.graphiks.dawn4k.DawnBackend
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.dawn4k.toolkit.surface.DawnTargetState
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget

/** Owns an external UI target; toolkit presentation leases retire before this host closes. */
internal interface SurfaceHost : AutoCloseable {
    val backend: DawnBackend
    fun pixelSize(): Pair<Int, Int>
    fun createTarget(states: StateFlow<DawnTargetState>): DawnSurfaceTarget
}
