package org.graphiks.dawn4k.toolkit

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import org.graphiks.dawn4k.toolkit.internal.currentOsThreadId

/** A dispatcher pinned to one OS thread. Borrowed dispatchers must provide that guarantee. */
class DawnExecutionOwner private constructor(
    internal val dispatcher: CoroutineDispatcher,
    private val resource: AutoCloseable?,
) : AutoCloseable {
    private val thread = atomic(0L)
    private val closed = atomic(false)

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        fun dedicated(name: String = "dawn4k-toolkit"): DawnExecutionOwner {
            val dispatcher = newSingleThreadContext(name)
            return DawnExecutionOwner(dispatcher, dispatcher)
        }
        /** limitedParallelism(1) alone does not pin work to one OS thread. */
        fun borrow(dispatcher: CoroutineDispatcher): DawnExecutionOwner = DawnExecutionOwner(dispatcher, null)
    }

    internal fun checkThread() {
        check(!closed.value) { "execution owner is closed" }
        val current = currentOsThreadId().toLong()
        thread.compareAndSet(0L, current)
        check(thread.value == current) { "execution owner changed OS thread; use a pinned dispatcher" }
    }

    suspend fun <T> run(block: suspend CoroutineScope.() -> T): T = withContext(dispatcher) {
        checkThread()
        try { coroutineScope(block) } finally { checkThread() }
    }

    /** Stop all users first. A borrowed dispatcher is never closed. */
    override fun close() {
        if (closed.compareAndSet(false, true)) resource?.close()
    }
}
