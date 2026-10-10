package org.graphiks.dawn4k.demo

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnAdapter
import org.graphiks.dawn4k.DawnDevice

/** Demo-owned scheduling, on the consumer's dispatcher. The backend creates no job. */
internal suspend fun <T> withEventProgress(
    progress: () -> Unit,
    afterProgress: suspend () -> Unit = {},
    block: suspend () -> T,
): T {
    val consumer = currentCoroutineContext()
    // Only the progression lifetime is shielded. The consumer still observes
    // its original cancellation, and can await GPU completion in its finally.
    return withContext(NonCancellable) {
        coroutineScope {
            val blockJob = CompletableDeferred<Job>()
            val events = launch {
                val work = blockJob.await()
                try { while (isActive) { progress(); delay(1) } } catch (failure: Throwable) {
                    if (failure !is CancellationException) {
                        work.cancel(CancellationException("event progression failed", failure))
                    }
                    throw failure
                }
            }
            try {
                withContext(consumer) {
                    blockJob.complete(checkNotNull(currentCoroutineContext()[Job]))
                    block()
                }
            } finally {
                withContext(NonCancellable) {
                    events.cancelAndJoin()
                    afterProgress()
                }
            }
        }
    }
}

internal suspend fun <T> DawnContext.withDemoEventProgress(
    closeChildren: () -> Unit = {},
    block: suspend () -> T,
): T = withEventProgress(::processEvents, afterProgress = {
    settleDemoEvents()
    closeChildren()
    settleDemoEvents()
}, block = block)

private suspend fun DawnContext.settleDemoEvents() {
    withTimeout(10_000) {
        while (hasPendingOperations()) { processEvents(); delay(1) }
    }
}

/** Owns child closure outside the running event job, including failed bootstrap. */
internal suspend fun <T> DawnContext.useWithDemoEventProgress(
    block: suspend (DawnContext, DawnAdapter, DawnDevice) -> T,
): T = use { context ->
    var adapter: DawnAdapter? = null
    var device: DawnDevice? = null
    context.withDemoEventProgress(closeChildren = {
        try { device?.close() } finally { adapter?.close() }
    }) {
        val acquiredAdapter = context.requestAdapter().getOrThrow() as DawnAdapter
        adapter = acquiredAdapter
        val acquiredDevice = acquiredAdapter.requestDevice().getOrThrow() as DawnDevice
        device = acquiredDevice
        block(context, acquiredAdapter, acquiredDevice)
    }
}
