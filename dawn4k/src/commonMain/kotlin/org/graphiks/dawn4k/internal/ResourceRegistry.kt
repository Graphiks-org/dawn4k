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
 * All calls happen on the owner's dispatcher, never on arbitrary threads: when
 * a dispatcher is provided every mutation routes through its worker (the
 * destroy and release callbacks make native calls), and without one — the pure
 * single-threaded tests — the calls run inline.
 */
internal class ResourceRegistry(private val dispatcher: NativeDispatcher? = null) {

    private class Entry(val destroy: (() -> Unit)?, val release: () -> Unit) {
        var destroyed = false
        var released = false
    }

    /** Insertion-ordered; mutated only on the dispatcher worker. */
    private val owned = LinkedHashMap<Any, Entry>()

    /** Worker-confined; published to foreign threads by the routing join. */
    private var closed = false

    /** Registers a newly acquired reference: [destroy] is optional (refcount-only objects), [release] is not. */
    fun own(key: Any, destroy: (() -> Unit)?, release: () -> Unit) {
        routed { ownOnOwner(key, destroy, release) }
    }

    /** Destroys the object behind [key] once; the entry stays as a tombstone until teardown. */
    fun destroy(key: Any) {
        routed { destroyOnOwner(key) }
    }

    /** Releases the refcount-only object behind [key]; no-op afterwards. */
    fun release(key: Any) {
        routed { releaseOnOwner(key) }
    }

    /** Runs every outstanding release exactly once, then closes the owner. */
    fun close() {
        routed { closeOnOwner() }
    }

    /**
     * Test visibility: entries not released yet (alive or tombstoned). Zero
     * after teardown. Routed through the dispatcher while it is alive; after
     * [close] the state is final and read directly.
     */
    internal fun debugRemainingRefs(): Int =
        if (closed) owned.size else routed { owned.size }

    private fun <R> routed(block: () -> R): R =
        dispatcher?.call { block() } ?: block()

    private fun ownOnOwner(key: Any, destroy: (() -> Unit)?, release: () -> Unit) {
        check(!closed) { "the resource registry is closed; a closed owner refuses new acquisitions" }
        check(key !in owned) { "the resource registry already owns $key" }
        owned[key] = Entry(destroy, release)
    }

    private fun destroyOnOwner(key: Any) {
        val entry = owned[key] ?: return
        if (!entry.destroyed) {
            entry.destroyed = true
            entry.destroy?.invoke()
        }
    }

    private fun releaseOnOwner(key: Any) {
        val entry = owned[key] ?: return
        if (entry.released) return
        entry.released = true
        owned.remove(key)
        entry.release()
    }

    private fun closeOnOwner() {
        if (closed) return
        closed = true
        var firstFailure: Throwable? = null
        // Reverse acquisition order: dependants release before their owners.
        for (entry in owned.values.reversed()) {
            if (entry.released) continue
            entry.released = true
            try {
                entry.release()
            } catch (failure: Throwable) {
                if (firstFailure == null) firstFailure = failure
            }
        }
        owned.clear()
        firstFailure?.let { throw it }
    }
}
