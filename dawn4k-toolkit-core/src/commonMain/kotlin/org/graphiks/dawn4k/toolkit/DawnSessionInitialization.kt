package org.graphiks.dawn4k.toolkit

import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnSurfaceReference

/** Prepare context-bound platform resources before adapter selection. */
fun interface DawnSessionInitialization {
    /** Clean partially acquired resources in a finally block if this call fails. */
    suspend fun prepare(context: DawnContext): DawnPreparedInitialization
}

interface DawnPreparedInitialization {
    val compatibleSurface: DawnSurfaceReference?
    /** Release platform resources before the context closes; support retry after failure. */
    suspend fun retire()
}
