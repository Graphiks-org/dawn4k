package org.graphiks.dawn4k.toolkit.surface.internal

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.AtomicLong
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.dawn4k.toolkit.surface.DawnPlatformOwner
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTargets
import org.graphiks.dawn4k.toolkit.surface.DawnTargetLease
import org.graphiks.dawn4k.toolkit.surface.DawnTargetRetention
import org.graphiks.dawn4k.toolkit.surface.DawnTargetState
import org.graphiks.dawn4k.toolkit.surface.EmptyTargetRetention

/**
 * The retained native-handle target shared by the platform factories: each lease acquires
 * the handle on the platform owner, keeps it in a per-lease [AtomicLong] that the native
 * surface source reads lazily, and releases it on acknowledged retirement. A failure after
 * [TargetRetirement] registration rolls the acquisition back.
 */
internal fun retainedHandleTarget(
    source: (handle: () -> Long) -> NativeSurfaceSource,
    acquire: () -> Long,
    release: (handle: Long) -> Unit,
    owner: DawnPlatformOwner,
    states: StateFlow<DawnTargetState>,
    retention: DawnTargetRetention = EmptyTargetRetention,
    retirement: TargetRetirement? = null,
): DawnSurfaceTarget = object : DawnSurfaceTarget {
    override val state = states
    override suspend fun retain(): DawnTargetLease {
        val address = atomic(0L)
        var generation = 0L
        val handles = object : DawnTargetRetention {
            override suspend fun retain() {
                generation = states.value.generation
                val handle = acquire()
                try {
                    retirement?.acquired(generation)
                    address.value = handle
                } catch (failure: Throwable) {
                    release(handle)
                    throw failure
                }
            }
            override suspend fun retire() {
                val handle = address.value
                if (handle != 0L) {
                    release(handle)
                    address.value = 0L
                    retirement?.retired(generation)
                }
            }
        }
        val target = DawnSurfaceTargets.retained(source { address.value }, owner, states, ComposedTargetRetention(retention, handles))
        return HandleLease(target.retain(), address)
    }
}

/** The integrator's retention starts before the native handle; both roll back together. */
private class ComposedTargetRetention(
    private val external: DawnTargetRetention,
    private val handles: DawnTargetRetention,
) : DawnTargetRetention {
    override suspend fun retain() {
        external.retain()
        try {
            handles.retain()
        } catch (failure: Throwable) {
            try { external.retire() } catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
            throw failure
        }
    }
    override suspend fun retire() {
        handles.retire()
        external.retire()
    }
}

/** A [DawnTargetLease] plus test and integration access to the retained native handle. */
internal class HandleLease(
    private val delegate: DawnTargetLease,
    private val address: AtomicLong,
) : DawnTargetLease by delegate {
    /** The retained handle; zero once its lease has retired. */
    val retainedHandle: Long get() = address.value
}
