package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.graphiks.dawn4k.toolkit.internal.BootstrapTransaction
import org.graphiks.dawn4k.toolkit.internal.retireWithDeadline
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SessionBootstrapTest {
    @Test
    fun rollbackDeadlineBoundsEachAttemptAndTheLoopRetries() = runTest {
        val owner = DawnExecutionOwner.borrow(StandardTestDispatcher(testScheduler))
        val transaction = BootstrapTransaction()
        var hanging = true
        var attempts = 0
        transaction.onRollback { attempts++; if (hanging) awaitCancellation() }
        val primary = IllegalStateException("bootstrap failed")
        val options = DawnSessionOptions(
            execution = DawnExecutionMode.Manual(owner),
            shutdownTimeout = 20.milliseconds,
        )
        val retirement = backgroundScope.launch {
            retireWithDeadline(owner, transaction, primary, ownsOwner = false, options)
        }
        // Two deadlines plus their retry delays elapse; a stuck rollback must not own the loop.
        advanceTimeBy(200); runCurrent()
        assertTrue(attempts >= 2, "expected the deadline to bound each rollback attempt, saw $attempts")
        hanging = false
        retirement.join()
        assertTrue(retirement.isCompleted)
        // A timed-out attempt is unclean, not a suppressed failure riding the original one.
        assertEquals(0, primary.suppressedExceptions.size)
    }
}
