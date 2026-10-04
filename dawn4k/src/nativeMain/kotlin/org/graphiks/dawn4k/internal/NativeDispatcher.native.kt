package org.graphiks.dawn4k.internal

import kotlin.native.concurrent.ThreadLocal
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Kotlin/Native actual: the worker is a single-threaded view over the shared
 * default pool — a dispatcher supported by the resolved coroutines version,
 * never `Dispatchers.Main`. The deferred reentrant close is published by
 * [createNativeDispatcher].
 */
internal actual fun createNativeDispatcher(): NativeDispatcher = workerBackedDispatcher(
    worker = Dispatchers.Default.limitedParallelism(1),
    newSlot = { NativeWorkerSlot },
    disposeWorker = {
        // A limited view of the shared default pool holds no resource to free.
    },
)

/** Worker identity of the thread currently running a dispatcher task. */
@ThreadLocal
private var workerIdentity: Any? = null

/**
 * Thread-local worker identity. The slot is shared yet safe: each dispatcher is
 * recognized by identity, so a foreign worker never takes another dispatcher's
 * fast path, and the worker loop clears the identity after every task.
 */
private object NativeWorkerSlot : WorkerSlot {
    override fun set(owner: Any?) {
        workerIdentity = owner
    }

    override fun get(): Any? = workerIdentity
}
