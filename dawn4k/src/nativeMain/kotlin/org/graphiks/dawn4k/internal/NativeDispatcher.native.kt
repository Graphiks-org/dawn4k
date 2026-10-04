@file:OptIn(kotlin.native.concurrent.ObsoleteWorkersApi::class)
@file:Suppress("ObsoleteWorkersApi")

package org.graphiks.dawn4k.internal

import kotlin.native.concurrent.ThreadLocal
import kotlin.native.concurrent.Worker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

/**
 * Kotlin/Native actual: the worker is ONE dedicated [Worker] thread exposed as
 * a coroutine dispatcher. Every dispatched task runs on that same OS thread —
 * the affinity is a Dawn contract, not an optimization: the backend keeps
 * per-thread error-scope stacks (`WGPUDevice` scopes are keyed by the calling
 * thread), so the calls that push a scope, the calls whose validation errors it
 * must capture, and the pop all have to share one thread. The previous
 * `Dispatchers.Default.limitedParallelism(1)` view serialized the tasks but
 * let them hop between pool threads, which pushed and popped different Dawn
 * scope stacks. The deferred reentrant close is published by
 * [createNativeDispatcher].
 */
internal actual fun createNativeDispatcher(): NativeDispatcher {
    val worker = Worker.start(name = "dawn4k-native-dispatcher")
    return workerBackedDispatcher(
        worker = WorkerThreadDispatcher(worker),
        newSlot = { NativeWorkerSlot },
        disposeWorker = {
            // The worker exits once its already-queued tasks have drained. The
            // close never joins it from here (the JVM executor shutdown does
            // not either); a reentrant close from the worker itself terminates
            // it once the running task returns.
            worker.requestTermination()
        },
    )
}

/** A [CoroutineDispatcher] that runs every task on one dedicated [Worker] thread. */
private class WorkerThreadDispatcher(private val worker: Worker) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        worker.executeAfter(0L) { block.run() }
    }
}

/** Worker identity of the thread currently running a dispatcher task. */
@ThreadLocal
private var workerIdentity: Any? = null

/**
 * Thread-local worker identity. Only the dedicated worker thread ever sees its
 * dispatcher here, so the reentrant fast path never triggers on a foreign
 * thread.
 */
private object NativeWorkerSlot : WorkerSlot {
    override fun set(owner: Any?) {
        workerIdentity = owner
    }

    override fun get(): Any? = workerIdentity
}
