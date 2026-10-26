package org.graphiks.dawn4k.toolkit

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Advanced building block for toolkit integrators: a native retirement that concurrent
 * callers share, as the toolkit's own platform leases use it.
 *
 * [retireOnce] runs [retire] (the constructor action) under a mutex-guarded lazy [Deferred]:
 * - concurrent callers await the same in-flight attempt instead of starting their own;
 * - cancelling a wait leaves the retirement running to completion and its owner alive —
 *   only a fresh [retireOnce] call observes the outcome;
 * - a failed or cancelled attempt is not shared: the next call starts a fresh one, so a
 *   failed retirement stays retryable; after a successful attempt later calls return
 *   without running [retire] again.
 *
 * [retire] starts on the thread that first triggered the retirement and resumes wherever
 * its own suspensions resume. Confine platform-thread work inside the action itself, as
 * the toolkit's leases do by hopping to their platform owner there.
 */
class SharedRetirement(private val retire: suspend () -> Unit) {
    private val mutex = Mutex()
    private val retired = atomic(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private var attempt: Deferred<Unit>? = null

    suspend fun retireOnce() {
        val shared = mutex.withLock {
            if (retired.value) return
            attempt?.takeUnless { it.isCompleted }
                ?: scope.async(start = CoroutineStart.LAZY) { retire(); retired.value = true }
                    .also { attempt = it; it.start() }
        }
        // Cancelling this wait leaves the retirement running to completion.
        shared.await()
    }
}
