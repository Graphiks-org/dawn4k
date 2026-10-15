package org.graphiks.dawn4k.toolkit.surface.internal

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import org.graphiks.dawn4k.toolkit.surface.*

private val generations = atomic(0L)
internal fun nextTargetGeneration(): Long = generations.incrementAndGet()

internal class TargetLifecycle {
    private val mutableState = MutableStateFlow(DawnTargetState.fresh(PixelExtent(0, 0), false))
    val state = mutableState.asStateFlow()
    private val invalidated = atomic(0L)
    val lastInvalidatedGeneration: Long get() = invalidated.value

    fun available(extent: PixelExtent): Long {
        val generation = nextTargetGeneration()
        mutableState.value = DawnTargetState(generation, extent, true)
        return generation
    }
    fun resize(generation: Long, extent: PixelExtent) {
        val current = state.value
        if (current.generation == generation && current.available) mutableState.value = current.copy(extent = extent)
    }
    fun invalidate(generation: Long) {
        val current = state.value
        if (current.generation != generation) return
        invalidated.value = generation
        mutableState.value = current.copy(available = false)
    }
}

/** Invalidation stops acquisition; this registry acknowledges the last retained native lease. */
internal class TargetRetirement {
    private val lock = SynchronizedObject()
    private val references = MutableStateFlow<Map<Long, Int>>(emptyMap())
    fun acquired(generation: Long) = synchronized(lock) {
        val current = references.value
        references.value = current + (generation to ((current[generation] ?: 0) + 1))
    }
    fun retired(generation: Long) = synchronized(lock) {
        val current = references.value
        val count = checkNotNull(current[generation]) { "target lease was not registered" }
        references.value = if (count == 1) current - generation else current + (generation to count - 1)
    }
    suspend fun await(generation: Long) { references.first { (it[generation] ?: 0) == 0 } }
}
