package org.graphiks.dawn4k.internal

/** Callback settlements only. No thread, scheduler or rendering commands. */
internal class CallbackMailbox {
    private val lock = SynchronizedObject()
    private val queued = ArrayDeque<() -> Unit>()

    fun post(action: () -> Unit) { lock.withLock { queued.addLast(action) } }
    fun isEmpty(): Boolean = lock.withLock { queued.isEmpty() }

    /** Executes one finite batch on its caller, always outside the monitor. */
    fun drain() {
        val batch = lock.withLock { queued.toList().also { queued.clear() } }
        var firstFailure: Throwable? = null
        for (action in batch) {
            try { action() } catch (failure: Throwable) {
                if (firstFailure == null) firstFailure = failure
            }
        }
        firstFailure?.let { throw it }
    }
}
