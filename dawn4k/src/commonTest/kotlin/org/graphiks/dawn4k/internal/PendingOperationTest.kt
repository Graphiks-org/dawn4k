@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.graphiks.dawn4k.internal

import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * State-machine tests for [PendingOperation], simulating the scheduling of a
 * native callback without any GPU: buffered outcomes, late deliveries, owner
 * abandonment and the exactly-once release of values nobody consumes.
 */
class PendingOperationTest {

    @Test fun duplicateFailureCannotAcknowledgeAnUnconsumedOwnedOutcome() = runTest {
        val operation = PendingOperation<Int> { }
        operation.complete(Result.success(7))
        operation.complete(Result.failure(IllegalStateException("duplicate")))
        assertFalse(operation.ownershipCompletion.isCompleted)
        assertEquals(7, operation.await().getOrThrow())
        operation.acceptOwnership()
        assertTrue(operation.ownershipCompletion.isCompleted)
    }

    @Test fun cancellationAfterReservationDefersReleaseAndAckUntilMailboxDrain() = runTest {
        val mailbox = CallbackMailbox()
        val releases = mutableListOf<Int>()
        lateinit var operation: PendingOperation<Int>
        operation = PendingOperation { value ->
            mailbox.post { releases += value; operation.acceptOwnership() }
        }
        var acks = 0
        operation.ownershipCompletion.invokeOnCompletion { acks++ }
        val waiter = async { operation.await() }
        runCurrent()
        operation.complete(Result.success(11))
        waiter.cancelAndJoin()
        assertTrue(releases.isEmpty())
        assertEquals(0, acks)
        mailbox.drain()
        assertEquals(listOf(11), releases)
        assertEquals(1, acks)
        operation.acceptOwnership()
        assertEquals(1, acks)
    }

    @Test fun cancelledWaiterKeepsOwnershipPendingUntilLateResultIsReleased() = runTest {
        val mailbox = CallbackMailbox()
        lateinit var operation: PendingOperation<Int>
        operation = PendingOperation { mailbox.post { operation.acceptOwnership() } }
        val waiter = async { operation.await() }
        runCurrent()
        waiter.cancelAndJoin()
        assertFalse(operation.ownershipCompletion.isCompleted)
        operation.complete(Result.success(17))
        assertFalse(operation.ownershipCompletion.isCompleted)
        mailbox.drain()
        assertTrue(operation.ownershipCompletion.isCompleted)
    }

    @Test
    fun resultAfterCancellationIsReleasedExactlyOnce() = runTest {
        val released = mutableListOf<Int>()
        val operation = PendingOperation<Int> { released += it }
        val waiter = async { operation.await() }
        runCurrent()
        waiter.cancelAndJoin()
        operation.complete(Result.success(7))
        operation.nativeTerminal()
        assertEquals(listOf(7), released)
        assertTrue(operation.nativeFinished)
    }

    @Test
    fun resultCompletedBeforeAwaitDeliversTheBufferedOutcome() = runTest {
        val released = mutableListOf<Int>()
        val operation = PendingOperation<Int> { released += it }

        // The native callback fired inline before the waiter registered: the
        // outcome is final during await's registration and is delivered inline.
        operation.complete(Result.success(3))

        assertFalse(operation.waiterFinished)
        assertEquals(3, operation.await().getOrThrow())
        assertTrue(operation.waiterFinished)
        assertTrue(released.isEmpty())

        operation.nativeTerminal()
        assertTrue(operation.nativeFinished)
    }

    @Test
    fun nativeFailureReachesTheWaiter() = runTest {
        val released = mutableListOf<Int>()
        val operation = PendingOperation<Int> { released += it }
        val waiter = async { operation.await() }
        runCurrent()

        operation.complete(Result.failure(IllegalStateException("device lost")))

        val outcome = waiter.await()
        assertTrue(outcome.isFailure)
        assertEquals("device lost", outcome.exceptionOrNull()!!.message)
        operation.nativeTerminal()
        assertTrue(operation.nativeFinished)
        // A failed delivery carries no owned reference, so nothing is released.
        assertTrue(released.isEmpty())
    }

    @Test
    fun abandonTerminatesTheWaitAndTheLateValueIsReleased() = runTest {
        val released = mutableListOf<Int>()
        val operation = PendingOperation<Int> { released += it }
        val waiter = async { operation.await() }
        runCurrent()

        // The owner (runtime shutdown, device loss) terminates the wait...
        operation.abandon(IllegalStateException("device lost"))
        assertTrue(operation.waiterFinished)
        val outcome = waiter.await()
        assertTrue(outcome.isFailure)
        assertEquals("device lost", outcome.exceptionOrNull()!!.message)

        // ...but the native side is still in flight: its late success owns a
        // reference nobody consumes anymore and must be released exactly once.
        assertFalse(operation.nativeFinished)
        operation.complete(Result.success(11))
        assertEquals(listOf(11), released)

        operation.nativeTerminal()
        assertTrue(operation.nativeFinished)
    }

    @Test
    fun abandonBeforeAnyWaiterBuffersTheFailureAndReleasesTheLateValue() = runTest {
        val released = mutableListOf<Int>()
        val operation = PendingOperation<Int> { released += it }

        // Abandonment with no waiter yet must not hang a later await().
        operation.abandon(IllegalStateException("device lost"))
        assertFalse(operation.waiterFinished)

        val outcome = operation.await()
        assertTrue(outcome.isFailure)
        assertEquals("device lost", outcome.exceptionOrNull()!!.message)

        operation.complete(Result.success(12))
        assertEquals(listOf(12), released)
        operation.nativeTerminal()
        assertTrue(operation.nativeFinished)
    }

    @Test
    fun doubleDeliveryWithAnOwnedValueReleasesItOnce() = runTest {
        val released = mutableListOf<Int>()
        val operation = PendingOperation<Int> { released += it }
        val waiter = async { operation.await() }
        runCurrent()

        operation.complete(Result.success(1))
        // A second delivery carrying a new owned reference has no consumer.
        operation.complete(Result.success(2))

        assertEquals(1, waiter.await().getOrThrow())
        // The new reference is released; the delivered one is never re-released.
        assertEquals(listOf(2), released)
        operation.nativeTerminal()
    }

    @Test
    fun lateFailureAfterDeliveryHasNoReferenceToRelease() = runTest {
        val released = mutableListOf<Int>()
        val operation = PendingOperation<Int> { released += it }
        val waiter = async { operation.await() }
        runCurrent()

        operation.complete(Result.success(1))
        assertEquals(1, waiter.await().getOrThrow())
        // A duplicate failure owns no C reference: releasing it twice would
        // drop a reference that does not exist.
        operation.complete(Result.failure(IllegalStateException("duplicate")))
        assertTrue(released.isEmpty())
        operation.nativeTerminal()
        assertTrue(operation.nativeFinished)
    }

    @Test
    fun nativeTerminalDoesNotEndTheWaiterFacet() = runTest {
        val released = mutableListOf<Int>()
        val operation = PendingOperation<Int> { released += it }

        // The native facet and the waiter facet are two distinct states:
        // terminalizing the registration does not deliver or end the wait.
        operation.nativeTerminal()
        operation.nativeTerminal()
        assertTrue(operation.nativeFinished)
        assertFalse(operation.waiterFinished)

        val waiter = async { operation.await() }
        runCurrent()
        assertFalse(operation.waiterFinished)

        operation.complete(Result.success(4))
        assertEquals(4, waiter.await().getOrThrow())
        assertTrue(operation.waiterFinished)
        assertTrue(released.isEmpty())
    }

    @Test
    fun anOperationServesASingleWaiter() = runTest {
        val released = mutableListOf<Int>()
        val operation = PendingOperation<Int> { released += it }
        val first = async { operation.await() }
        runCurrent()

        assertFailsWith<IllegalStateException> { operation.await() }

        first.cancelAndJoin()
        operation.complete(Result.success(5))
        assertEquals(listOf(5), released)
        operation.nativeTerminal()
    }
}
