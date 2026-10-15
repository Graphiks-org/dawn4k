package org.graphiks.dawn4k.toolkit.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graphiks.dawn4k.toolkit.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

internal interface SessionShutdownDriver {
    fun processEvents()
    fun hasPendingOperations(): Boolean
    suspend fun awaitApplicationWork() {}
    suspend fun retirePresentation()
    fun closeDevice()
    fun closeAdapter()
    fun closeContext()
    suspend fun stopPolling() {}
    fun resumePolling() {}
}

/** Stages advance only after success. Concurrent callers share a complete attempt. */
internal class SessionShutdown(
    private val owner: DawnExecutionOwner,
    private val ownsOwner: Boolean,
    private val timeout: Duration,
    private val interval: Duration,
    private val lifecycle: SessionLifecycle,
    private val driver: SessionShutdownDriver,
    executionContext: CoroutineContext = Dispatchers.Default,
) {
    private val scope = CoroutineScope(executionContext + SupervisorJob())
    private val mutex = Mutex()
    private var attempt: Deferred<Unit>? = null
    private var stage = 0

    fun requestCleanup() { scope.launch { runCatching { shutdown() } } }

    suspend fun shutdown() = withContext(NonCancellable) {
        val shared = mutex.withLock {
            if (lifecycle.state.value == DawnSessionState.Closed) return@withContext
            attempt?.takeUnless { it.isCompleted } ?: scope.async(start = CoroutineStart.LAZY) {
                lifecycle.beginShutdown()
                try {
                    owner.run {
                        try {
                            withTimeout(timeout) {
                                driver.awaitApplicationWork()
                                if (stage == 0) { driver.retirePresentation(); stage = 1 }
                                driver.stopPolling()
                                if (stage == 1) { drain(); stage = 2 }
                                if (stage == 2) { driver.closeDevice(); stage = 3 }
                                if (stage == 3) { drain(); stage = 4 }
                                if (stage == 4) { driver.closeAdapter(); stage = 5 }
                                if (stage == 5) { drain(); driver.closeContext(); stage = 6 }
                            }
                        } catch (failure: Throwable) {
                            if (stage < 6) driver.resumePolling()
                            throw failure
                        }
                    }
                    // Return from the worker before closing its owned dispatcher.
                    if (ownsOwner) owner.close()
                    lifecycle.closed()
                } catch (timeout: TimeoutCancellationException) {
                    lifecycle.timedOut()
                    throw DawnShutdownTimeoutException(timeout)
                } catch (failure: Throwable) {
                    lifecycle.shutdownFailed(failure)
                    throw failure
                }
            }.also { attempt = it; it.start() }
        }
        shared.await()
    }

    private suspend fun drain() {
        while (true) {
            driver.processEvents()
            if (!driver.hasPendingOperations()) return
            delay(interval)
        }
    }
}
