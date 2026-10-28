package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DawnRuntime
import org.graphiks.dawn4k.internal.SurfaceReferenceState

/**
 * One owned native surface reference bound to its creating context. Platform
 * integrators adopt an acquired surface through [DawnContext.adoptSurface].
 * The native presentation target must outlive this reference and its requests.
 */
class DawnSurfaceReference internal constructor(
    private val runtime: DawnRuntime,
    private val state: SurfaceReferenceState,
) : AutoCloseable {
    /** Advanced integration handle, valid while this owner is open. */
    fun nativeHandle(): Long = state.nativeHandle()

    internal fun retainForRequest(owner: DawnRuntime): Pair<Long, AutoCloseable> {
        val handle = state.nativeHandle()
        return handle to state.retainForRequest(owner)
    }

    /** Idempotent; in-flight adapter requests keep independent native references. */
    override fun close() {
        runtime.requireCanClose(state)
        state.close()
    }
}
