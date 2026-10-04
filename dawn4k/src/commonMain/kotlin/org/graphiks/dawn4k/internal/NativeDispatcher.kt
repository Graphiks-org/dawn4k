package org.graphiks.dawn4k.internal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Serializes native operations and their callbacks onto one owned worker: every
 * task posted here runs alone, in FIFO order, on a single thread.
 */
internal interface NativeDispatcher : AutoCloseable {

    /**
     * Runs [block] on the worker and blocks the calling thread until it returns.
     * Reentrant when called from the worker itself: the block then runs inline
     * on the fast path instead of enqueuing behind itself.
     */
    fun <T> call(block: () -> T): T

    /**
     * Dispatches [block] to the worker and suspends until it has run. A waiter
     * cancelled before the worker dispatches the block skips the native call
     * entirely; cancelling after dispatch lets the in-flight call finish and
     * discards its result.
     */
    suspend fun <T> awaitCall(block: () -> T): T

    /** Enqueues [block] on the worker without waiting for it. */
    fun post(block: () -> Unit)

    /**
     * Suspends until every block posted before this call has run. On a closed
     * dispatcher, waits for the ongoing drain instead.
     */
    suspend fun drain()
}

/** Creates a dispatcher owning a single dedicated worker. */
internal expect fun createNativeDispatcher(): NativeDispatcher

/**
 * Per-thread identity of the worker currently executing a task; null outside of
 * the worker. Backed by a `ThreadLocal` on the JVM and a `@ThreadLocal` global
 * on Kotlin/Native. Both platform actuals pin ONE dedicated worker thread (a
 * single-thread executor on the JVM, a dedicated `Worker` on Kotlin/Native), so
 * the identity only ever matches on that thread and the reentrant fast path
 * never triggers on a foreign thread.
 */
internal interface WorkerSlot {
    fun set(owner: Any?)
    fun get(): Any?
}

/**
 * Builds the worker-backed dispatcher: the platform actuals of
 * [createNativeDispatcher] supply the serial worker, its thread slot and the
 * disposal hook. The worker loop owns the queue; closing the queue makes the
 * loop drain its remaining tasks and exit, and the disposal runs when it does.
 */
internal fun workerBackedDispatcher(
    worker: CoroutineDispatcher,
    newSlot: () -> WorkerSlot,
    disposeWorker: () -> Unit,
): NativeDispatcher = WorkerNativeDispatcher(worker, newSlot(), disposeWorker)

private class WorkerNativeDispatcher(
    private val worker: CoroutineDispatcher,
    private val slot: WorkerSlot,
    private val disposeWorker: () -> Unit,
) : NativeDispatcher {

    private val queue = Channel<() -> Unit>(Channel.UNLIMITED)

    /**
     * First failure of a fire-and-forget block, rethrown by the close that
     * initiated the shutdown. Worker-thread-confined; published to callers by
     * the `join` in [close].
     */
    private var firstUncaught: Throwable? = null

    private val workerJob: Job = CoroutineScope(worker).launch {
        for (task in queue) {
            slot.set(this@WorkerNativeDispatcher)
            try {
                task()
            } catch (failure: Throwable) {
                if (firstUncaught == null) firstUncaught = failure
            } finally {
                slot.set(null)
            }
        }
    }

    init {
        workerJob.invokeOnCompletion { disposeWorker() }
    }

    override fun <T> call(block: () -> T): T {
        if (slot.get() === this) return block()
        val outcome = CompletableDeferred<Result<T>>()
        if (!queue.trySend { outcome.complete(runCatching(block)) }.isSuccess) throwClosed()
        return runBlocking { outcome.await() }.getOrThrow()
    }

    override suspend fun <T> awaitCall(block: () -> T): T {
        if (slot.get() === this) return block()
        // The raw result of an awaited call owns no native reference at this
        // layer; callback outcomes own theirs and use PendingOperation directly.
        val operation = PendingOperation<T> { }
        if (!queue.trySend {
                if (!operation.waiterFinished) {
                    // The waiter still exists: dispatch the native call. A
                    // waiter cancelled before dispatch makes no C call at all.
                    operation.complete(runCatching(block))
                }
                operation.nativeTerminal()
            }.isSuccess) throwClosed()
        return operation.await().getOrThrow()
    }

    override fun post(block: () -> Unit) {
        if (!queue.trySend {
                try {
                    block()
                } catch (failure: Throwable) {
                    if (firstUncaught == null) firstUncaught = failure
                }
            }.isSuccess) throwClosed()
    }

    override suspend fun drain() {
        val reached = CompletableDeferred<Unit>()
        if (queue.trySend { reached.complete(Unit) }.isSuccess) {
            reached.await()
        } else {
            // Closing is already draining the queue: wait for it to finish.
            workerJob.join()
        }
    }

    override fun close() {
        val initiated = queue.close()
        if (slot.get() === this) {
            // Reentrant close from the worker: initiate only. The loop drains
            // the remaining queue and exits on its own; joining here would make
            // the worker wait for itself, forever.
            return
        }
        runBlocking { workerJob.join() }
        if (initiated) firstUncaught?.let { throw it }
    }

    private fun throwClosed(): Nothing =
        throw IllegalStateException("native dispatcher is closed")
}
