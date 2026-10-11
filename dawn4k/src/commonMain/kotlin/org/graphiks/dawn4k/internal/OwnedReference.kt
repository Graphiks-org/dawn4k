package org.graphiks.dawn4k.internal

/**
 * One caller-owned native reference. No registry or GC fallback retains it.
 * Close claims ownership once; native actions run outside the monitor.
 * Handle access is a state guard, not a use/close barrier: callers join users.
 */
internal class OwnedReference<H>(
    private val rawHandle: H,
    private val destroy: ((H) -> Unit)? = null,
    private val release: (H) -> Unit,
) : AutoCloseable {
    private val lock = SynchronizedObject()
    private var closed = false

    val handle: H
        get() = lock.withLock {
            check(!closed) { "the native resource is closed" }
            rawHandle
        }

    override fun close() {
        val first = lock.withLock {
            if (closed) false else { closed = true; true }
        }
        if (!first) return
        try { destroy?.invoke(rawHandle) } finally { release(rawHandle) }
    }
}
