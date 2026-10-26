package org.graphiks.dawn4k.toolkit.surface

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

/** Factory default: a [DawnTargetRetention] without host lifetime of its own. */
internal object EmptyTargetRetention : DawnTargetRetention {
    override suspend fun retain() = Unit
    override suspend fun retire() = Unit
}

/**
 * Reference-counts the leases of one target for integrators that own the target's UI host.
 *
 * Every successful [retain] must be acknowledged by exactly one [retire]. [awaitRetirement]
 * and [requireRetired] start retirement — retain attempts that race or follow it fail
 * cleanly — and complete at the first moment no lease is outstanding. Both are idempotent
 * and safe to call before any retain.
 */
class CountingTargetRetention : DawnTargetRetention {
    private val lock = SynchronizedObject()
    private val outstanding = MutableStateFlow(0)
    private var retiring = false

    /** Fails once retirement has started: the host underneath a new lease could already be gone. */
    override suspend fun retain(): Unit = synchronized(lock) {
        check(!retiring) { "target retention is retiring" }
        outstanding.value += 1
    }

    override suspend fun retire(): Unit = synchronized(lock) {
        check(outstanding.value > 0) { "target lease retirements must match their retains" }
        outstanding.value -= 1
    }

    /** Starts retirement, then completes at the first moment no lease is outstanding. */
    suspend fun awaitRetirement() {
        synchronized(lock) { retiring = true }
        outstanding.first { it == 0 }
    }

    /** Starts retirement and requires every lease to have acknowledged retirement. Seals the retention even when this throws — every later retain fails. */
    fun requireRetired() {
        synchronized(lock) {
            retiring = true
            check(outstanding.value == 0) { "await target lease retirement before destroying its UI host" }
        }
    }
}
