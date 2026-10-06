package org.graphiks.dawn4k.internal

import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * JVM and Android actual: the worker is a dedicated single thread exposed as
 * a coroutine dispatcher. The deferred reentrant close is published by
 * [createNativeDispatcher].
 */
internal actual fun createNativeDispatcher(): NativeDispatcher {
    val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "dawn4k-native-dispatcher")
    }
    return workerBackedDispatcher(
        worker = executor.asCoroutineDispatcher(),
        newSlot = ::JvmWorkerSlot,
        disposeWorker = { executor.shutdown() },
    )
}

/** Thread-local worker identity: only the worker thread sees its dispatcher. */
private class JvmWorkerSlot : WorkerSlot {
    private val owner = ThreadLocal<Any?>()

    override fun set(owner: Any?) {
        this.owner.set(owner)
    }

    override fun get(): Any? = owner.get()
}
