package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import kotlin.coroutines.*
import kotlin.test.*

class CallbackReentryTest {
    private suspend fun rejectsInlineReentry(operation: suspend (DawnSession) -> Unit) {
        val delivered = CompletableDeferred<Pair<Result<Unit>?, DawnSessionState>>()
        val completed = CompletableDeferred<Result<Unit>>()
        lateinit var session: DawnSession
        session = openDawnSession(DawnSessionOptions(deviceDescriptor = DeviceDescriptor(
            onUncapturedError = object : GPUUncapturedErrorCallback {
                override fun onUncapturedError(error: GPUError) {
                    var inlineResult: Result<Unit>? = null
                    suspend { operation(session) }.startCoroutine(object : Continuation<Unit> {
                        override val context = EmptyCoroutineContext
                        override fun resumeWith(result: Result<Unit>) {
                            inlineResult = result
                            completed.complete(result)
                        }
                    })
                    // A rejected call must complete before the native callback returns. This
                    // probes the same pre-dispatch boundary as runBlocking without hanging RED.
                    delivered.complete(inlineResult to session.state.value)
                }
            },
        )))
        try {
            session.run {
                it.device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.Uniform)).close()
            }
            val (inlineResult, state) = delivered.await()
            completed.await()
            val failure = assertNotNull(inlineResult, "callback reentry suspended instead of being rejected")
                .exceptionOrNull()
            assertIs<IllegalStateException>(failure)
            assertTrue(failure.message.orEmpty().contains("progress", ignoreCase = true))
            assertEquals(DawnSessionState.Ready, state)
        } finally { session.shutdown() }
    }

    @Test fun runFromRealCallbackIsRejectedBeforeDispatch() = runTest {
        var entered = false
        rejectsInlineReentry { session -> session.run { entered = true } }
        assertFalse(entered)
    }

    @Test fun shutdownFromRealCallbackIsRejectedBeforeMutation() = runTest {
        rejectsInlineReentry { it.shutdown() }
    }

    @Test fun realCallbackCanEnqueueDeferredApplicationWork() = runTest {
        val deferredWork = CompletableDeferred<Int>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        lateinit var session: DawnSession
        session = openDawnSession(DawnSessionOptions(deviceDescriptor = DeviceDescriptor(
            onUncapturedError = object : GPUUncapturedErrorCallback {
                override fun onUncapturedError(error: GPUError) {
                    scope.launch { deferredWork.complete(session.run { 42 }) }
                }
            },
        )))
        try {
            session.run {
                it.device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.Uniform)).close()
            }
            assertEquals(42, deferredWork.await())
        } finally { scope.cancel(); session.shutdown() }
    }
}
