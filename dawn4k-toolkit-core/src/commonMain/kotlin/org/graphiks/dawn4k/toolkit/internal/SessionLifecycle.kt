package org.graphiks.dawn4k.toolkit.internal

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.graphiks.dawn4k.toolkit.DawnSessionState
import org.graphiks.webgpu.*

internal class SessionLifecycle {
    private val lock = SynchronizedObject()
    private val mutableState = MutableStateFlow<DawnSessionState>(DawnSessionState.Ready)
    val state: StateFlow<DawnSessionState> = mutableState.asStateFlow()
    private val users = mutableSetOf<Job>()

    fun requireReady() {
        check(state.value == DawnSessionState.Ready) { "session does not admit work: ${state.value}" }
    }
    suspend fun <T> track(block: suspend CoroutineScope.() -> T): T = coroutineScope {
        val job = coroutineContext[Job]!!
        synchronized(lock) { requireReady(); users += job }
        try { block() } finally { synchronized(lock) { users -= job } }
    }
    fun beginShutdown() = synchronized(lock) {
        if (state.value != DawnSessionState.Closed) mutableState.value = DawnSessionState.ShuttingDown
    }
    fun timedOut() { mutableState.value = DawnSessionState.ShutdownTimedOut }
    fun closed() { mutableState.value = DawnSessionState.Closed }
    fun shutdownFailed(cause: Throwable) { mutableState.value = DawnSessionState.Failed(cause) }
    fun lost(info: GPUDeviceLostInfo) = interrupt(DawnSessionState.Lost(info), null)
    fun fail(cause: Throwable) = interrupt(DawnSessionState.Failed(cause), cause)
    fun uncapturedError(error: GPUError) {
        if (error is GPUOutOfMemoryError) fail(IllegalStateException("Dawn out of memory: ${error.message}"))
    }
    private fun interrupt(terminal: DawnSessionState, cause: Throwable?) {
        val jobs = synchronized(lock) {
            if (state.value != DawnSessionState.Ready) return
            mutableState.value = terminal
            users.toList()
        }
        val cancelled = CancellationException("Dawn session stopped", cause)
        jobs.forEach { it.cancel(cancelled) }
    }
}
