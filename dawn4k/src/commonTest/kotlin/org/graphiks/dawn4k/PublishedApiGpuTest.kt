package org.graphiks.dawn4k

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.dawn4k.native.wgpuDeviceDestroy
import org.graphiks.webgpu.GPUDeviceLostReason
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RequiredLimits
import org.graphiks.webgpu.descriptors.TextureDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class PublishedApiGpuTest {
    @Test
    fun reentrantDeviceThenContextClosePreservesTheSameLossForExistingAndLateObservers() = runTest {
        DawnContext.create().use { context ->
            val adapter = context.requestAdapter().getOrThrow()
            val device = adapter.requestDevice().getOrThrow()
            var resourcesClosed = false
            try {
                val observer = async(start = CoroutineStart.UNDISPATCHED) { device.awaitLost() }
                context.nativeBridge().call {
                    device.close()
                    adapter.close()
                    resourcesClosed = true
                    context.close()
                }
                val first = withContext(Dispatchers.Default) { withTimeout(5.seconds) { observer.await().getOrThrow() } }
                assertEquals(GPUDeviceLostReason.Destroyed, first.reason)
                val late = withContext(Dispatchers.Default) { withTimeout(5.seconds) { device.awaitLost().getOrThrow() } }
                assertEquals(first, late)
            } finally {
                if (!resourcesClosed) {
                    device.close()
                    adapter.close()
                }
            }
        }
    }

    @Test
    fun nativeLossWhileIdleIsProgressedWithoutAnotherGpuOperation() = runTest {
        DawnContext.create().use { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                (adapter.requestDevice().getOrThrow() as DawnDevice).use { device ->
                    val observer = async(start = CoroutineStart.UNDISPATCHED) { device.awaitLost().getOrThrow() }
                    device.session.runtime.dispatcher.call { wgpuDeviceDestroy(device.session.handle) }
                    val loss = withContext(Dispatchers.Default) { withTimeout(5.seconds) { observer.await() } }
                    assertEquals(GPUDeviceLostReason.Destroyed, loss.reason)
                }
            }
        }
    }

    @Test
    fun cancellingOneLossObserverLeavesTheDeviceAndOtherObserversAlive() = runTest {
        DawnContext.create().use { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                adapter.requestDevice().getOrThrow().use { device ->
                    val cancelled = async(start = CoroutineStart.UNDISPATCHED) { device.awaitLost().getOrThrow() }
                    val first = async(start = CoroutineStart.UNDISPATCHED) { device.awaitLost().getOrThrow() }
                    val second = async(start = CoroutineStart.UNDISPATCHED) { device.awaitLost().getOrThrow() }
                    cancelled.cancelAndJoin()
                    device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.CopyDst)).close()
                    device.queue.onSubmittedWorkDone().getOrThrow()
                    assertTrue(!first.isCompleted && !second.isCompleted, "observer cancellation must not destroy the device")
                    device.close()
                    val loss = withContext(Dispatchers.Default) { withTimeout(5.seconds) { first.await() } }
                    assertEquals(GPUDeviceLostReason.Destroyed, loss.reason)
                    assertEquals(loss, withContext(Dispatchers.Default) { withTimeout(5.seconds) { second.await() } })
                    assertEquals(loss, withContext(Dispatchers.Default) { withTimeout(5.seconds) { device.awaitLost().getOrThrow() } })
                }
            }
        }
    }

    @Test
    fun resourceUsageReturnsTheFullMaskIncludingUnknownMetadataBits() = runTest {
        DawnContext.create().use { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                adapter.requestDevice().getOrThrow().use { device ->
                    // Unknown bits must survive metadata projection, irrespective
                    // of native validation of the descriptor's usage.
                    device.pushErrorScope(GPUErrorFilter.Validation)
                    val bufferUsage = GPUBufferUsage.fromBits((1uL shl 63) or 12uL)
                    device.createBuffer(BufferDescriptor(4uL, bufferUsage)).use { buffer ->
                        assertEquals(bufferUsage, buffer.usage)
                        assertTrue(GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst in buffer.usage)
                        assertTrue(GPUBufferUsage.None in buffer.usage)
                    }
                    val textureUsage = GPUTextureUsage.fromBits((1uL shl 63) or 17uL)
                    device.createTexture(TextureDescriptor(Extent3D(1u), GPUTextureFormat.RGBA8Unorm, textureUsage)).use { texture ->
                        assertEquals(textureUsage, texture.usage)
                        assertTrue(GPUTextureUsage.CopySrc or GPUTextureUsage.RenderAttachment in texture.usage)
                    }
                    device.popErrorScope().getOrThrow()
                }
            }
        }
    }

    @Test
    fun aSingleRequestedLimitDoesNotRequestUndefinedCapabilities() = runTest {
        DawnContext.create().use { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                adapter.requestDevice(DeviceDescriptor(requiredLimits = RequiredLimits(maxBindGroups = 1u)))
                    .getOrThrow().use { device ->
                        assertTrue(device.limits.maxBindGroups >= 1u)
                        device.pushErrorScope(GPUErrorFilter.Validation)
                        device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.CopyDst)).close()
                        assertNull(device.popErrorScope().getOrThrow())
                    }
            }
        }
    }
}
