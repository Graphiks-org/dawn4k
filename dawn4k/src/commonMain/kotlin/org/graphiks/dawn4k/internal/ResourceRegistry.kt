package org.graphiks.dawn4k.internal

/**
 * Explicit ownership bookkeeping of one owner (a device session): every native
 * reference the owner acquired is registered with its optional destroy
 * callback and its mandatory release callback.
 *
 * - [destroy] runs the destroy callback exactly once, immediately, but keeps
 *   the entry until teardown: the handle reference stays valid for the
 *   tombstones the validation tests need.
 * - [release] releases a refcount-only object (no destroy callback) right
 *   away; the entry is gone afterwards.
 * - [close] runs the outstanding releases exactly once, in reverse acquisition
 *   order, so they all settle before the owner releases its own
 *   device/adapter/instance references. It is idempotent, and a closed owner
 *   refuses new acquisitions.
 *
 * Bookkeeping is synchronized; native actions run on the caller, outside the
 * monitor. Consumers must join resource users before destroying or closing.
 */
internal class ResourceRegistry {

    private val lock = SynchronizedObject()

    private class Entry(val destroy: (() -> Unit)?, val release: () -> Unit) {
        var destroyed = false
        var released = false
    }

    /** Insertion-ordered; protected by [lock]. */
    private val owned = LinkedHashMap<Any, Entry>()

    private var closed = false

    /** Registers a newly acquired reference: [destroy] is optional (refcount-only objects), [release] is not. */
    fun own(key: Any, destroy: (() -> Unit)?, release: () -> Unit) {
        lock.withLock {
            check(!closed) { "the resource registry is closed; a closed owner refuses new acquisitions" }
            check(key !in owned) { "the resource registry already owns $key" }
            owned[key] = Entry(destroy, release)
        }
    }

    /** Destroys the object behind [key] once; the entry stays as a tombstone until teardown. */
    fun destroy(key: Any) {
        val action = lock.withLock {
            val entry = owned[key]
            if (entry == null || entry.destroyed) null else {
                entry.destroyed = true
                entry.destroy
            }
        }
        action?.invoke()
    }

    /** Releases the refcount-only object behind [key]; no-op afterwards. */
    fun release(key: Any) {
        val entry = lock.withLock { owned.remove(key)?.also { it.released = true } }
        entry?.release?.invoke()
    }

    /** Runs every outstanding release exactly once, then closes the owner. */
    fun close() {
        val entries = lock.withLock {
            if (closed) emptyList() else {
                closed = true
                owned.values.toList().asReversed().also { owned.clear() }
            }
        }
        var firstFailure: Throwable? = null
        for (entry in entries) {
            try { entry.release() } catch (failure: Throwable) {
                if (firstFailure == null) firstFailure = failure
            }
        }
        firstFailure?.let { throw it }
    }

    internal fun debugRemainingRefs(): Int = lock.withLock { owned.size }
}
