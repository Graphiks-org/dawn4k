package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ManualProgressTest {
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
