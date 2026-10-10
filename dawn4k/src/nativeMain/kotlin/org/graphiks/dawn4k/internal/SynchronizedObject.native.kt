@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.concurrent.atomics.ExperimentalAtomicApi::class,
)

package org.graphiks.dawn4k.internal

import platform.posix.sched_yield
import kotlin.concurrent.atomics.AtomicInt

/**
 * Kotlin/Native actual: a compare-and-set spin lock. Every operation owns a
 * private lock and its critical section is a handful of plain field writes
 * that never block, so a preempted holder always finishes its section; a
 * yielding spinner cannot livelock or deadlock, it only waits. The atomics
 * are sequentially consistent, which publishes the guarded state to the next
 * entering thread (the release of `store(0)` pairs with the acquire of the
 * winning `compareAndSet`), exactly like the mutex this replaces.
 */
internal actual class SynchronizedObject {

    /** 0 = free, 1 = held; the lock itself. */
    private val held = AtomicInt(0)

    actual fun <R> withLock(block: () -> R): R {
        while (!held.compareAndSet(0, 1)) {
            sched_yield()
        }
        try {
            return block()
        } finally {
            held.store(0)
        }
    }
}
