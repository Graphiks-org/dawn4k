package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.toolkit.internal.BootstrapTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BootstrapTransactionTest {
    @Test
    fun cancellationAtDispatcherReturnRetainsAcquisition() = runTest {
        val owner = DawnExecutionOwner.dedicated()
        val transaction = BootstrapTransaction()
        var closed = false
        try {
            val caller = launch {
                try {
                    val job = coroutineContext[kotlinx.coroutines.Job]!!
                    owner.run {
                        transaction.onRollback { closed = true }
                        job.cancel()
                    }
                } finally {
                    withContext(NonCancellable) { owner.run { transaction.rollback() } }
                }
            }
            caller.join()
            assertTrue(closed)
        } finally { owner.close() }
    }

    @Test
    fun cancelledBootstrapUnwindsEveryCompletedAcquisition() = runTest {
        val expected = listOf(listOf("context"), listOf("adapter", "context"), listOf("device", "adapter", "context"))
        for (completed in 1..3) {
            val transaction = BootstrapTransaction()
            val closed = mutableListOf<String>()
            listOf("context", "adapter", "device").take(completed).forEach { name ->
                transaction.onRollback { closed += name }
            }
            assertTrue(transaction.rollback(CancellationException("bootstrap cancelled")))
            assertEquals(expected[completed - 1], closed)
        }
    }

    @Test
    fun successfulTransferDoesNotRunRollbackActions() = runTest {
        val transaction = BootstrapTransaction()
        var closes = 0
        transaction.onRollback { closes++ }
        transaction.commit()
        transaction.rollback()
        assertEquals(0, closes)
    }

    @Test
    fun cleanupFailureDoesNotMaskThePrimaryFailure() = runTest {
        val transaction = BootstrapTransaction()
        val primary = IllegalArgumentException("bootstrap")
        val cleanup = IllegalStateException("cleanup")
        transaction.onRollback { throw cleanup }
        transaction.rollback(primary)
        assertEquals(listOf(cleanup), primary.suppressedExceptions)
    }
}
