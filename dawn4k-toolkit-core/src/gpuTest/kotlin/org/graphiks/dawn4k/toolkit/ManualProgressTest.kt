package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.DawnContext
import kotlin.test.*

class ManualProgressTest {
    @Test
    fun scopedManualFailureStopsItsDriverBeforeCleanup() = runTest {
        val owner = DawnExecutionOwner.dedicated()
        var retained: DawnSession? = null
        var context: DawnContext? = null
        try {
            assertFailsWith<IllegalArgumentException> {
                withDawnSession(DawnSessionOptions(execution = DawnExecutionMode.Manual(owner))) {
                    retained = it
                    context = it.context
                    throw IllegalArgumentException("application failed")
                }
            }
            assertEquals(0L, assertNotNull(context).nativeBridge().instanceHandle())
        } finally {
            retained?.manualProgress?.stop()
            retained?.shutdown()
            owner.close()
        }
    }

    @Test
    fun manualBootstrapHandsOffExactlyOnce() = runTest {
        val owner = DawnExecutionOwner.dedicated()
        try {
            val session = openDawnSession(DawnSessionOptions(execution = DawnExecutionMode.Manual(owner)))
            val manual = assertNotNull(session.manualProgress)
            try {
                owner.run { manual.processEvents() }
                manual.stop()
                manual.stop()
                assertTrue(manual.isStopped)
                owner.run { assertFailsWith<IllegalStateException> { manual.processEvents() } }
            } finally { manual.stop(); session.shutdown() }
        } finally { owner.close() }
    }

    @Test
    fun shutdownRejectsAnActiveManualDriver() = runTest {
        val owner = DawnExecutionOwner.dedicated()
        try {
            val session = openDawnSession(DawnSessionOptions(execution = DawnExecutionMode.Manual(owner)))
            try {
                assertFailsWith<IllegalStateException> { session.shutdown() }
                assertEquals(DawnSessionState.Ready, session.state.value)
            } finally { session.manualProgress!!.stop(); session.shutdown() }
        } finally { owner.close() }
    }
}
