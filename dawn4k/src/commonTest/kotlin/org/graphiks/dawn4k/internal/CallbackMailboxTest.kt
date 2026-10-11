package org.graphiks.dawn4k.internal

import org.graphiks.dawn4k.testing.captureStandardError
import org.graphiks.webgpu.GPUError
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CallbackMailboxTest {
    @Test fun postNeverExecutesInlineAndDrainIsFinite() {
        val mailbox = CallbackMailbox()
        val seen = mutableListOf<Int>()
        mailbox.post { seen += 1; mailbox.post { seen += 3 } }
        mailbox.post { seen += 2 }
        assertTrue(seen.isEmpty())
        mailbox.drain()
        assertEquals(listOf(1, 2), seen)
        assertFalse(mailbox.isEmpty())
        mailbox.drain()
        assertEquals(listOf(1, 2, 3), seen)
    }

    @Test fun oneFailureDoesNotDropOtherActions() {
        val mailbox = CallbackMailbox()
        var called = false
        mailbox.post { error("boom") }
        mailbox.post { called = true }
        assertFailsWith<IllegalStateException> { mailbox.drain() }
        assertTrue(called)
        assertTrue(mailbox.isEmpty())
    }

    @Test
    fun unhandledErrorsReportTheirTypeAndMessage() {
        val mailbox = CallbackMailbox()
        val callbacks = DeviceCallbacks()
        mailbox.post { callbacks.handleUncapturedError(DawnValidationError("invalid buffer λ")) }
        mailbox.post { callbacks.handleUncapturedError(DawnOutOfMemoryError("allocation failed")) }
        mailbox.post { callbacks.handleUncapturedError(DawnInternalError("driver failed")) }
        val output = captureStandardError { mailbox.drain() }
        assertContains(output, "Validation")
        assertContains(output, "invalid buffer λ")
        assertContains(output, "OutOfMemory")
        assertContains(output, "allocation failed")
        assertContains(output, "Internal")
        assertContains(output, "driver failed")
        assertTrue(mailbox.isEmpty())
    }

    @Test
    fun handledErrorsAreDeliveredOnceWithoutLoggingOrReplay() {
        val mailbox = CallbackMailbox()
        val callbacks = DeviceCallbacks()
        val received = mutableListOf<GPUError>()
        callbacks.uncapturedErrorSink = { received += it }
        val first = DawnValidationError("first")
        val second = DawnInternalError("second")
        mailbox.post { callbacks.handleUncapturedError(first) }
        mailbox.post { callbacks.handleUncapturedError(second) }
        val output = captureStandardError {
            mailbox.drain()
            callbacks.uncapturedErrorSink = { error("an old error was replayed") }
            mailbox.drain()
        }
        assertEquals("", output)
        assertEquals(2, received.size)
        assertSame(first, received[0])
        assertSame(second, received[1])
        assertTrue(mailbox.isEmpty())
    }

    @Test
    fun throwingCallbackIsReportedWithoutPreventingTheNextDelivery() {
        val mailbox = CallbackMailbox()
        val callbacks = DeviceCallbacks()
        val received = mutableListOf<GPUError>()
        callbacks.uncapturedErrorSink = {
            received += it
            if (received.size == 1) throw IllegalStateException("consumer failed")
        }
        mailbox.post { callbacks.handleUncapturedError(DawnValidationError("first GPU error")) }
        mailbox.post { callbacks.handleUncapturedError(DawnValidationError("second GPU error")) }
        val output = captureStandardError { mailbox.drain() }
        assertEquals(2, received.size)
        assertContains(output, "first GPU error")
        assertContains(output, "IllegalStateException")
        assertContains(output, "consumer failed")
        assertContains(output, "throwingCallbackIsReportedWithoutPreventingTheNextDelivery")
        assertTrue(mailbox.isEmpty())
    }
}
