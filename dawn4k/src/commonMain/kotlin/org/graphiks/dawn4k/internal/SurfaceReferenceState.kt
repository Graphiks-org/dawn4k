package org.graphiks.dawn4k.internal

/** Native leaf refcount calls are serialized with acquisition; callbacks run outside the monitor. */
internal class SurfaceReferenceState(
    private val owner: Any,
    private val handle: Long,
    private val addRef: (Long) -> Unit,
    private val release: (Long) -> Unit,
    private val onReleased: () -> Unit = {},
) : AutoCloseable {
    private val lock = SynchronizedObject()
    private var closed = false
    private var requests = 0
    private var releasing = 0
    private var retired = false

    init { require(handle != 0L) { "the surface handle is null" } }

    fun nativeHandle(): Long = lock.withLock {
        check(!closed) { "the surface is closed" }
        handle
    }

    fun retainForRequest(owner: Any): AutoCloseable {
        var leaseClosed = false
        val lease = object : AutoCloseable {
            override fun close() {
                val releaseNow = lock.withLock {
                    if (leaseClosed) false else {
                        leaseClosed = true
                        requests--
                        releasing++
                        true
                    }
                }
                if (releaseNow) releaseReference()
            }
        }
        lock.withLock {
            require(this.owner === owner) { "the surface belongs to a different context" }
            check(!closed) { "the surface is closed" }
            addRef(handle)
            requests++
        }
        return lease
    }

    override fun close() {
        val releaseNow = lock.withLock {
            if (closed) false else { closed = true; releasing++; true }
        }
        if (releaseNow) releaseReference()
    }

    private fun releaseReference() {
        release(handle)
        val notify = lock.withLock {
            releasing--
            if (closed && requests == 0 && releasing == 0 && !retired) {
                retired = true
                true
            } else false
        }
        if (notify) onReleased()
    }
}
