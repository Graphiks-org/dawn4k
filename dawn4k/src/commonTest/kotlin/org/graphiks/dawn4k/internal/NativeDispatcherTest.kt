@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.graphiks.dawn4k.internal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Scheduling tests for the serialized worker owned by [createNativeDispatcher],
 * simulating native calls and callbacks without any GPU: FIFO serialization,
 * reentrancy, cancellation before/after dispatch, close semantics and foreign
 * threads posting owned copies. Cross-thread synchronization uses latches and
 * channels only, never timed sleeps.
 */
class NativeDispatcherTest {

    @Test
    fun callRunsItsBlockOnTheWorkerAndReturnsTheValue() = runBlocking {
        createNativeDispatcher().use { dispatcher ->
            assertEquals(7, dispatcher.call { 7 })
        }
    }

    @Test
    fun postedBlocksRunSerializedInFifoOrder() = runBlocking {
        createNativeDispatcher().use { dispatcher ->
            val firstDone = CompletableDeferred<Unit>()
            val observed = CompletableDeferred<Boolean>()
            dispatcher.post { firstDone.complete(Unit) }
            dispatcher.post { observed.complete(firstDone.isCompleted) }
            // The queue drains in order: the second block only runs once the
            // first one has completed on the same worker.
            assertTrue(observed.await())
        }
    }

    @Test
    fun drainWaitsForTheBlocksPostedBeforeIt() = runTest {
        createNativeDispatcher().use { dispatcher ->
            val done = mutableListOf<CompletableDeferred<Unit>>()
            repeat(3) {
                val marker = CompletableDeferred<Unit>()
                done += marker
                dispatcher.post { marker.complete(Unit) }
            }
            dispatcher.drain()
            done.forEach { assertTrue(it.isCompleted) }
        }
    }

    @Test
    fun awaitCallDeliversTheBlockResult() = runTest {
        createNativeDispatcher().use { dispatcher ->
            assertEquals(21, dispatcher.awaitCall { 21 })
        }
    }

    @Test
    fun awaitCallPropagatesTheBlockFailure() = runTest {
        createNativeDispatcher().use { dispatcher ->
            val failure = assertFailsWith<IllegalStateException> {
                dispatcher.awaitCall<Int> { throw IllegalStateException("native rejected") }
            }
            assertEquals("native rejected", failure.message)
        }
    }

    @Test
    fun reentrantCallRunsTheNestedBlockInline() = runBlocking {
        createNativeDispatcher().use { dispatcher ->
            // A nested call from the worker must not enqueue (a single worker
            // would deadlock on itself): it runs inline on the fast path.
            assertEquals(42, dispatcher.call { dispatcher.call { 42 } })
        }
    }

    @Test
    fun reentrantAwaitCallRunsInlineWithoutSuspending() = runBlocking {
        createNativeDispatcher().use { dispatcher ->
            // From a suspend context already on the worker, awaiting a call
            // must run inline: enqueueing would deadlock the single worker on
            // its own running task.
            val result = CompletableDeferred<Int>()
            dispatcher.post { runBlocking { result.complete(dispatcher.awaitCall { 42 }) } }
            assertEquals(42, result.await())
        }
    }

    @Test
    fun reentrantPostIsQueuedNotExecutedInline() = runBlocking {
        createNativeDispatcher().use { dispatcher ->
            val inner = CompletableDeferred<Unit>()
            val postedBeforeRun = dispatcher.call {
                dispatcher.post { inner.complete(Unit) }
                // The reentrant post must be queued behind the current task,
                // not executed while the worker is busy with the outer call.
                inner.isCompleted
            }
            assertFalse(postedBeforeRun)
            dispatcher.drain()
            assertTrue(inner.isCompleted)
        }
    }

    @Test
    fun cancellationBeforeTheWorkerDispatchesSkipsTheNativeCall() = runTest {
        createNativeDispatcher().use { dispatcher ->
            // Hold the worker behind a latch so the awaited call is provably
            // queued but not dispatched yet.
            val blocker = CompletableDeferred<Unit>()
            dispatcher.post { runBlocking { blocker.await() } }
            var nativeCalls = 0
            val waiter = async { dispatcher.awaitCall { nativeCalls++; 42 } }
            runCurrent()
            // awaitCall posts its task before suspending, so the task is queued
            // behind the blocker when the waiter cancels.
            waiter.cancelAndJoin()

            blocker.complete(Unit)
            dispatcher.drain()
            // Cancel before dispatch: the native side goes not-started ->
            // terminal and the C call is never made.
            assertEquals(0, nativeCalls)
        }
    }

    @Test
    fun cancellationAfterDispatchLetsTheNativeCallFinish() = runTest {
        createNativeDispatcher().use { dispatcher ->
            val inFlight = CompletableDeferred<Unit>()
            val releaseCall = CompletableDeferred<Unit>()
            var nativeCalls = 0
            val waiter = async {
                dispatcher.awaitCall {
                    nativeCalls++
                    inFlight.complete(Unit)
                    runBlocking { releaseCall.await() }
                    42
                }
            }
            // The call provably started (it is parked inside its block) before
            // the waiter cancels.
            inFlight.await()
            waiter.cancelAndJoin()
            releaseCall.complete(Unit)
            dispatcher.drain()

            // Cancel after dispatch: the waiter is gone but the native call
            // was already in flight and completed undisturbed.
            assertEquals(1, nativeCalls)
            assertTrue(waiter.isCancelled)
        }
    }

    @Test
    fun closeWhileAnAwaitedCallIsPendingDrainsIt() = runTest {
        val dispatcher = createNativeDispatcher()
        val waiter = async { dispatcher.awaitCall { 21 } }
        runCurrent()
        // The call task is provably queued (awaitCall posts before suspending):
        // closing from a foreign thread must drain it and end the wait.
        val closer = launch(Dispatchers.Default) { dispatcher.close() }

        assertEquals(21, waiter.await())
        closer.join()
    }

    @Test
    fun callbackFromAnotherThreadPostsItsOwnedCopy() = runTest {
        createNativeDispatcher().use { dispatcher ->
            val operation = PendingOperation<List<Int>> { fail("a consumed payload is never released") }
            val waiter = async { operation.await() }
            runCurrent()

            val borrowedPayload = listOf(1, 2, 3)
            launch(Dispatchers.Default) {
                // A callback running outside the worker copies its borrowed data
                // and posts the owned copy; transitions stay on the worker.
                val ownedCopy = borrowedPayload.toList()
                dispatcher.post {
                    operation.complete(Result.success(ownedCopy))
                    operation.nativeTerminal()
                }
            }

            assertEquals(listOf(1, 2, 3), waiter.await().getOrThrow())
            dispatcher.drain()
            assertTrue(operation.nativeFinished)
        }
    }

    @Test
    fun outcomeCompletedInlineDuringTheNativeCallIsDelivered() = runBlocking {
        createNativeDispatcher().use { dispatcher ->
            val operation = PendingOperation<Int> { fail("a consumed outcome is never released") }
            // Dawn can fire a callback inline during the very native call that
            // registers it: the outcome exists on the worker before the waiter
            // reaches await().
            dispatcher.call { operation.complete(Result.success(9)) }

            assertEquals(9, operation.await().getOrThrow())
            operation.nativeTerminal()
            assertTrue(operation.nativeFinished)
        }
    }

    @Test
    fun reentrantCloseFromTheWorkerDoesNotWaitForItsOwnWorker() = runBlocking {
        val dispatcher = createNativeDispatcher()
        val closedInline = CompletableDeferred<Unit>()
        dispatcher.post {
            // Close from a task running on the worker: it must defer the join
            // instead of waiting for its own worker, or this would deadlock.
            dispatcher.close()
            closedInline.complete(Unit)
        }
        closedInline.await()
        // The outer close joins the drained exit and is idempotent.
        dispatcher.close()
    }

    @Test
    fun reentrantCloseInsideACallBlockReturnsItsValue() = runBlocking {
        val dispatcher = createNativeDispatcher()
        // Close from a synchronous call on the worker: the block finishes and
        // delivers its value, then the worker drains and exits.
        assertEquals(42, dispatcher.call { dispatcher.close(); 42 })
        dispatcher.close()
    }

    @Test
    fun workSubmittedAfterCloseIsRejected() = runTest {
        val dispatcher = createNativeDispatcher()
        dispatcher.close()

        assertFailsWith<IllegalStateException> { dispatcher.post { } }
        assertFailsWith<IllegalStateException> { dispatcher.call { 1 } }
        assertFailsWith<IllegalStateException> { dispatcher.awaitCall { 1 } }
        // Closing an already-closed dispatcher is a no-op.
        dispatcher.close()
    }

    @Test
    fun stressOfAThousandOperationsEndsWithNoOutstandingReferences() = runBlocking {
        createNativeDispatcher().use { dispatcher ->
            // One owned "reference" per operation: it is settled exactly once,
            // either consumed by the waiter or released late, never both.
            val settled = Channel<Int>(Channel.UNLIMITED)
            val operations = List(1000) { index -> PendingOperation<Int> { settled.trySend(index) } }

            coroutineScope {
                operations.forEachIndexed { index, operation ->
                    launch(Dispatchers.Default) {
                        when (index % 4) {
                            // The waiter registers then cancels; the native side
                            // completes late from this foreign thread.
                            0 -> {
                                val registered = CompletableDeferred<Unit>()
                                val waiter = launch {
                                    registered.complete(Unit)
                                    operation.await()
                                }
                                registered.await()
                                waiter.cancelAndJoin()
                                operation.complete(Result.success(index))
                                operation.nativeTerminal()
                            }
                            // The completion posted on the worker races the
                            // waiter registration: buffered or delivered.
                            1 -> {
                                dispatcher.post {
                                    operation.complete(Result.success(index))
                                    operation.nativeTerminal()
                                }
                                operation.await().onSuccess { settled.trySend(index) }
                            }
                            // The outcome exists before the waiter registers.
                            2 -> {
                                dispatcher.post {
                                    operation.complete(Result.success(index))
                                    operation.nativeTerminal()
                                }
                                dispatcher.drain()
                                operation.await().onSuccess { settled.trySend(index) }
                            }
                            // Cancellation races the delivery from both sides.
                            else -> {
                                val registered = CompletableDeferred<Unit>()
                                val waiter = launch {
                                    registered.complete(Unit)
                                    operation.await().onSuccess { settled.trySend(index) }
                                }
                                registered.await()
                                dispatcher.post {
                                    operation.complete(Result.success(index))
                                    operation.nativeTerminal()
                                }
                                waiter.cancelAndJoin()
                            }
                        }
                    }
                }
            }

            val settledIndices = HashSet<Int>()
            repeat(1000) { settledIndices += settled.receive() }
            val outstandingReferences = 1000 - settledIndices.size
            assertEquals(0, outstandingReferences)
        }
    }
}
