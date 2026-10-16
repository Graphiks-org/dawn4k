package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("DEPRECATION")
class ExecutionOwnerTest {
    @Test
    fun gpuCallsAndProgressKeepThreadIdentity() = runBlocking {
        val caller = Thread.currentThread().id
        val observed = mutableSetOf<Long>()
        val errorDelivered = CompletableDeferred<Unit>()
        val options = DawnSessionOptions(deviceDescriptor = DeviceDescriptor(
            onUncapturedError = object : GPUUncapturedErrorCallback {
                override fun onUncapturedError(error: GPUError) {
                    observed += Thread.currentThread().id
                    errorDelivered.complete(Unit)
                }
            },
        ))
        withDawnSession(options) { gpu ->
            val owner = Thread.currentThread().id
            assertNotEquals(caller, owner)
            observed += owner
            // Invalid usage triggers a real GPU validation callback during processEvents.
            gpu.device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.Uniform)).close()
            errorDelivered.await()
            delay(1)
            observed += Thread.currentThread().id
            assertEquals(setOf(owner), observed)
        }
    }
}
