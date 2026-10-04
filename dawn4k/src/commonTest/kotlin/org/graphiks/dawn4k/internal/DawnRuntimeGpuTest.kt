package org.graphiks.dawn4k.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.testing.NativeFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Real-GPU lifecycle of [DawnRuntime]: twenty device sessions opened and closed
 * leave no open callback registration and no owned reference behind, and a wait
 * in flight when the runtime closes fails with the close diagnostic instead of
 * hanging. Runs only through the gpuTest* tasks; a host without an adapter
 * fails these tests (no silent skip).
 */
class DawnRuntimeGpuTest {

    @Test
    fun twentySessionsOpenAndCloseLeavingNoRegistrationsOrRemainingRefs() = runBlocking {
        // The fixture raw-opens once first: it is the entry point of the later
        // tasks and delegates to exactly the machinery stressed below.
        NativeFixture.open().use { fixture ->
            assertEquals(0, fixture.runtime.debugOpenCallbacks())
        }

        val runtime = DawnRuntime(DawnConfig())
        try {
            repeat(20) {
                val session = runtime.openSession()
                // Only the queue ref is owned so far; later tasks add more.
                assertEquals(1, session.resources.debugRemainingRefs())
                session.close()
                assertEquals(0, session.resources.debugRemainingRefs())
                session.close()
                runtime.drainEvents()
            }
            assertEquals(0, runtime.debugOpenCallbacks())
        } finally {
            runtime.close()
        }
        assertEquals(0, runtime.debugOpenCallbacks())
    }

    @Test
    fun waitInterruptedByRuntimeCloseFailsWithTheCloseDiagnostic() = runBlocking {
        val runtime = DawnRuntime(DawnConfig())
        try {
            // Prove the GPU first: a host without an adapter fails here.
            runtime.openSession().close()

            val interrupted = async(Dispatchers.Default) {
                runCatching { runtime.openSession() }
            }
            // Deterministic hand-off: wait until the second openSession has an
            // adapter request in flight — its callback registration is open.
            withTimeout(ADAPTER_HANDOFF_TIMEOUT_MS) {
                while (runtime.debugOpenCallbacks() == 0) yield()
            }
            runtime.close()

            val outcome = interrupted.await()
            assertTrue(outcome.isFailure)
            assertIs<DawnRuntimeClosedException>(outcome.exceptionOrNull())
        } finally {
            runtime.close()
        }
        assertEquals(0, runtime.debugOpenCallbacks())
    }

    private companion object {
        const val ADAPTER_HANDOFF_TIMEOUT_MS = 20_000L
    }
}
