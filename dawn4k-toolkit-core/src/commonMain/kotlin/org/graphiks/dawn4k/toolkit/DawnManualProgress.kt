package org.graphiks.dawn4k.toolkit

import kotlinx.atomicfu.atomic

/** Exclusive caller-side progress lease. Call on the session's pinned execution owner. */
class DawnManualProgress internal constructor(
    private val owner: DawnExecutionOwner,
    private val process: () -> Unit,
) {
    private val stopped = atomic(false)
    val isStopped: Boolean get() = stopped.value
    fun processEvents() {
        owner.checkThread()
        check(!isStopped) { "manual progression is stopped" }
        process()
    }
    /** Revokes future iterations; actual iterations and teardown run on the same owner. */
    fun stop() { stopped.value = true }
}
