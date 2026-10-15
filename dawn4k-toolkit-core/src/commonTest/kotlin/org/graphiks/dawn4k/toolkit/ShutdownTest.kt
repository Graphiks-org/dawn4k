package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.graphiks.dawn4k.toolkit.internal.*
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ShutdownTest {
    @Test fun automaticCleanupRetriesAfterTheDeadline() = runTest {
        val driver = Recorder(backgroundScope).apply { pending = true }
        val lifecycle = SessionLifecycle()
        val shutdown = shutdown(driver, lifecycle)
        shutdown.requestCleanup()
        advanceTimeBy(21); runCurrent()
        assertEquals(DawnSessionState.ShutdownTimedOut, lifecycle.state.value)
        driver.pending = false
        advanceTimeBy(200); runCurrent()
        assertEquals(1, driver.contexts)
        assertEquals(DawnSessionState.Closed, lifecycle.state.value)
    }

    @Test fun timedOutShutdownEventuallyRetiresWithoutAnotherCaller() = runTest {
        val driver = Recorder(backgroundScope).apply { pending = true }
        val shutdown = shutdown(driver)
        assertFailsWith<DawnShutdownTimeoutException> { shutdown.shutdown() }
        driver.pending = false
        advanceTimeBy(200); runCurrent()
        assertEquals(1, driver.contexts)
    }

    @Test
    fun concurrentShutdownSharesAttempt() = runTest {
        val gate = CompletableDeferred<Unit>()
        val driver = Recorder(backgroundScope).apply { retirementGate = gate }
        val shutdown = shutdown(driver)
        val first = async { shutdown.shutdown() }
        val second = async { shutdown.shutdown() }
        runCurrent()
        assertEquals(1, driver.retirements)
        gate.complete(Unit)
        awaitAll(first, second)
        assertEquals(1, driver.devices)
        assertEquals(1, driver.contexts)
    }

    @Test
    fun timeoutPreservesHandlesAndPolling() = runTest {
        val driver = Recorder(backgroundScope).apply { pending = true }
        val lifecycle = SessionLifecycle()
        val shutdown = shutdown(driver, lifecycle)
        assertFailsWith<DawnShutdownTimeoutException> { shutdown.shutdown() }
        assertEquals(0, driver.contexts)
        assertEquals(0, driver.devices)
        assertEquals(DawnSessionState.ShutdownTimedOut, lifecycle.state.value)
        val eventsAtTimeout = driver.events
        advanceTimeBy(3)
        runCurrent()
        assertTrue(driver.events > eventsAtTimeout)
        driver.pending = false
        shutdown.shutdown()
        assertEquals(1, driver.contexts)
    }

    @Test
    fun retryDoesNotRepeatSuccessfulCleanup() = runTest {
        val driver = Recorder(backgroundScope).apply { deviceCreatesPendingTeardown = true }
        val shutdown = shutdown(driver)
        assertFailsWith<DawnShutdownTimeoutException> { shutdown.shutdown() }
        assertEquals(1, driver.devices)
        assertEquals(0, driver.contexts)
        driver.pending = false
        shutdown.shutdown()
        shutdown.shutdown()
        assertEquals(1, driver.devices)
        assertEquals(1, driver.adapters)
        assertEquals(1, driver.contexts)
        assertEquals(1, driver.retirements)
    }

    @Test
    fun failedStageReportsFailureAndCanBeRetried() = runTest {
        val failure = IllegalStateException("platform retirement failed")
        val driver = Recorder(backgroundScope).apply { retirementFailure = failure }
        val lifecycle = SessionLifecycle()
        val shutdown = shutdown(driver, lifecycle)
        assertEquals(failure.message, assertFailsWith<IllegalStateException> { shutdown.shutdown() }.message)
        assertEquals(failure.message, assertIs<DawnSessionState.Failed>(lifecycle.state.value).cause.message)
        assertEquals(0, driver.devices)
        driver.retirementFailure = null
        shutdown.shutdown()
        assertEquals(1, driver.devices)
    }

    private fun TestScope.shutdown(driver: Recorder, lifecycle: SessionLifecycle = SessionLifecycle()) =
        SessionShutdown(
            DawnExecutionOwner.borrow(StandardTestDispatcher(testScheduler)), false,
            20.milliseconds, 1.milliseconds, lifecycle, driver,
            coroutineContext,
        )

    private class Recorder(private val scope: CoroutineScope) : SessionShutdownDriver {
        var pending = false
        var deviceCreatesPendingTeardown = false
        var events = 0
        var devices = 0
        var adapters = 0
        var contexts = 0
        var retirements = 0
        var retirementGate: CompletableDeferred<Unit>? = null
        var retirementFailure: Throwable? = null
        private var poller: Job? = null
        override fun processEvents() { events++ }
        override fun hasPendingOperations() = pending
        override suspend fun retirePresentation() {
            retirements++
            retirementFailure?.let { throw it }
            retirementGate?.await()
        }
        override fun closeDevice() { devices++; if (deviceCreatesPendingTeardown) pending = true }
        override fun closeAdapter() { adapters++ }
        override fun closeContext() { check(!pending); contexts++ }
        override suspend fun stopPolling() { poller?.cancelAndJoin(); poller = null }
        override fun resumePolling() {
            if (poller == null) poller = scope.launch { while (isActive) { processEvents(); delay(1) } }
        }
    }
}
