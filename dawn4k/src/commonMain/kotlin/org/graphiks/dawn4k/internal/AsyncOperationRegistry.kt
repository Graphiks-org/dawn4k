package org.graphiks.dawn4k.internal

/** Tracks native delivery and Kotlin ownership independently, under a short lock. */
internal class AsyncOperationRegistry {
    private class Entry(val owner: Any, var native: Boolean = false, var ownership: Boolean = false)
    private val lock = SynchronizedObject()
    private val entries = mutableMapOf<PendingOperation<*>, Entry>()

    fun register(owner: Any, operation: PendingOperation<*>) {
        lock.withLock { check(entries.put(operation, Entry(owner)) == null) }
    }
    fun nativeSettled(operation: PendingOperation<*>) = settle(operation, native = true)
    fun ownershipSettled(operation: PendingOperation<*>) = settle(operation, native = false)
    private fun settle(operation: PendingOperation<*>, native: Boolean) {
        lock.withLock {
            val entry = entries[operation] ?: return@withLock
            if (native) entry.native = true else entry.ownership = true
            if (entry.native && entry.ownership) entries.remove(operation)
        }
    }
    fun hasPending(owner: Any? = null): Boolean = lock.withLock {
        if (owner == null) entries.isNotEmpty() else entries.values.any { it.owner == owner }
    }
    fun nativeCount(): Int = lock.withLock { entries.values.count { !it.native } }
}
