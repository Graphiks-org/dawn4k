package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.graphiks.dawn4k.toolkit.internal.*
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SessionShutdownGpuTest {
    @Test
    fun cleanupCanAwaitGpuAfterCancellation() = runTest {
        val entered = CompletableDeferred<Unit>()
        var cleanupFinished = false
        val job = launch {
            withDawnSession { gpu ->
                try { entered.complete(Unit); awaitCancellation() }
                finally {
                    withContext(NonCancellable) {
                        assertTrue(gpu.queue.onSubmittedWorkDone().isSuccess)
                        cleanupFinished = true
                    }
                }
            }
        }
        entered.await()
        job.cancelAndJoin()
        assertTrue(cleanupFinished)
    }

    @Test
    fun shutdownInsideRunFailsBeforeMutation() = runTest {
        val session = openDawnSession()
        try {
            session.run {
                assertFailsWith<IllegalStateException> { it.shutdown() }
                assertEquals(DawnSessionState.Ready, it.state.value)
            }
        } finally { session.shutdown() }
        session.shutdown()
        assertEquals(DawnSessionState.Closed, session.state.value)
    }

}
