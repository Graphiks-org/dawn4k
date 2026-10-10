package org.graphiks.dawn4k.demo

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class DemoEventProgressTest {
    @Test fun progressionFailureInterruptsTheWaitingConsumer() = runTest {
        val startedAt = testScheduler.currentTime
        assertFailsWith<IllegalStateException> {
            withEventProgress({ error("progression failed") }) { delay(1_000) }
        }
        assertEquals(startedAt, testScheduler.currentTime, "progression failure must not wait for the consumer timeout")
    }
    @Test fun cancellationKeepsProgressAliveThroughConsumerCleanup() = runTest {
        val entered = CompletableDeferred<Unit>()
        val cleanupProgressed = CompletableDeferred<Unit>()
        var cleaning = false
        var cleanupTimedOut = false
        val consumer = async {
            withEventProgress({ if (cleaning) cleanupProgressed.complete(Unit) }) {
                try { entered.complete(Unit); awaitCancellation() } finally {
                    withContext(NonCancellable) {
                        cleaning = true
                        cleanupTimedOut = withTimeoutOrNull(20) { cleanupProgressed.await() } == null
                    }
                }
            }
        }
        entered.await()
        consumer.cancelAndJoin()
        assertFalse(cleanupTimedOut, "queue completion in cancellation cleanup still needs event progression")
    }
    @Test fun progressesWhileTheConsumerWaitsAndStopsAfterSuccess() = runTest {
        var iterations = 0
        val progressed = CompletableDeferred<Unit>()
        assertEquals(42, withEventProgress({ iterations++; progressed.complete(Unit) }) {
            progressed.await()
            42
        })
        val atExit = iterations
        delay(10)
        assertTrue(atExit > 0)
        assertEquals(atExit, iterations)
    }

    @Test fun stopsAfterFailureAndCancellation() = runTest {
        var iterations = 0
        assertFailsWith<IllegalStateException> {
            withEventProgress({ iterations++ }) { delay(2); error("consumer") }
        }
        val afterFailure = iterations
        delay(10)
        assertEquals(afterFailure, iterations)
        val started = CompletableDeferred<Unit>()
        val consumer = async {
            withEventProgress({ iterations++; started.complete(Unit) }) { delay(Long.MAX_VALUE) }
        }
        started.await()
        consumer.cancelAndJoin()
        val afterCancellation = iterations
        delay(10)
        assertEquals(afterCancellation, iterations)
    }
}
