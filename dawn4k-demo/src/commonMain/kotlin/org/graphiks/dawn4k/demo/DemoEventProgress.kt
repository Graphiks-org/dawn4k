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

/** Demo-owned scheduling, on the consumer's dispatcher. The backend creates no job. */
internal suspend fun <T> withEventProgress(progress: () -> Unit, block: suspend () -> T): T {
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
            } finally { withContext(NonCancellable) { events.cancelAndJoin() } }
        }
    }
}

internal suspend fun <T> DawnContext.withDemoEventProgress(block: suspend () -> T): T {
    try { return withEventProgress(::processEvents, block) } finally {
        withContext(NonCancellable) {
            withTimeout(10_000) {
                while (hasPendingOperations()) { processEvents(); delay(1) }
            }
        }
    }
}

internal suspend fun <T> DawnContext.useWithDemoEventProgress(block: suspend (DawnContext) -> T): T =
    use { context -> context.withDemoEventProgress { block(context) } }
