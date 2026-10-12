package org.graphiks.dawn4k

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.graphiks.dawn4k.internal.DawnRuntime
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Consumer-owned test job on the test's dispatcher, never in the backend. */
internal fun DawnContext.startTestProgress(scope: CoroutineScope): Job = scope.launch {
    while (isActive) { processEvents(); delay(1) }
}

internal fun DawnRuntime.startTestProgress(scope: CoroutineScope): Job = scope.launch {
    while (isActive) { processEvents(); delay(1) }
}

internal fun DawnRuntime.settleTestEvents() {
    val deadline = TimeSource.Monotonic.markNow()
    while (hasPendingOperations()) {
        check(deadline.elapsedNow() < 10.seconds) { "native operations did not settle within 10 seconds" }
        processEvents()
    }
}

internal fun DawnContext.settleTestEvents() {
    val deadline = TimeSource.Monotonic.markNow()
    while (hasPendingOperations()) {
        check(deadline.elapsedNow() < 10.seconds) { "native operations did not settle within 10 seconds" }
        processEvents()
    }
}

internal suspend fun <T> DawnContext.useWithProgress(block: suspend (DawnContext) -> T): T = coroutineScope {
    val events = startTestProgress(this)
    try { block(this@useWithProgress) } finally {
        withContext(NonCancellable) { events.cancelAndJoin() }
        settleTestEvents()
        close()
    }
}
