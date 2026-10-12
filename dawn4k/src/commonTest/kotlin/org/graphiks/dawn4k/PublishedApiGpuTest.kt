package org.graphiks.dawn4k

import org.graphiks.dawn4k.testing.gpuTestEnvironment
import org.graphiks.dawn4k.testing.gpuTestConfig
import org.graphiks.dawn4k.testing.captureStandardError

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.dawn4k.native.wgpuDeviceDestroy
import org.graphiks.webgpu.GPUDeviceLostReason
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RequiredLimits
import org.graphiks.webgpu.descriptors.TextureDescriptor
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class PublishedApiGpuTest {
    @Test
    fun prematureContextCloseIsRefusedAndExplicitProgressPreservesLoss() = runTest {
        if (!gpuTestEnvironment("PublishedApiGpuTest.prematureContextCloseIsRefusedAndExplicitProgressPreservesLoss")) return@runTest
        DawnContext.create(gpuTestConfig()).useWithProgress { context ->
            val adapter = context.requestAdapter().getOrThrow()
            val device = adapter.requestDevice().getOrThrow()
            var resourcesClosed = false
            try {
                val observer = async(start = CoroutineStart.UNDISPATCHED) { device.awaitLost() }
                run {
                    device.close()
                    adapter.close()
                    resourcesClosed = true
                    assertFailsWith<IllegalStateException> { context.close() }
                    context.settleTestEvents()
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
        if (!gpuTestEnvironment("PublishedApiGpuTest.nativeLossWhileIdleIsProgressedWithoutAnotherGpuOperation")) return@runTest
        DawnContext.create(gpuTestConfig()).useWithProgress { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                (adapter.requestDevice().getOrThrow() as DawnDevice).use { device ->
                    val observer = async(start = CoroutineStart.UNDISPATCHED) { device.awaitLost().getOrThrow() }
                    wgpuDeviceDestroy(device.session.handle)
                    val loss = withContext(Dispatchers.Default) { withTimeout(5.seconds) { observer.await() } }
                    assertEquals(GPUDeviceLostReason.Destroyed, loss.reason)
                }
            }
        }
    }

    @Test
    fun cancellingOneLossObserverLeavesTheDeviceAndOtherObserversAlive() = runTest {
        if (!gpuTestEnvironment("PublishedApiGpuTest.cancellingOneLossObserverLeavesTheDeviceAndOtherObserversAlive")) return@runTest
        DawnContext.create(gpuTestConfig()).useWithProgress { context ->
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
        if (!gpuTestEnvironment("PublishedApiGpuTest.resourceUsageReturnsTheFullMaskIncludingUnknownMetadataBits")) return@runTest
        DawnContext.create(gpuTestConfig()).useWithProgress { context ->
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
        if (!gpuTestEnvironment("PublishedApiGpuTest.aSingleRequestedLimitDoesNotRequestUndefinedCapabilities")) return@runTest
        DawnContext.create(gpuTestConfig()).useWithProgress { context ->
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

    @Test
    fun publicCallbackFailureIsReportedAndLaterErrorsStillArrive() {
        if (!runGpuTest { gpuTestEnvironment("PublishedApiGpuTest.publicCallbackFailureIsReportedAndLaterErrorsStillArrive") }) return
        var deliveries = 0
        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()
        val output = captureStandardError {
            runGpuTest {
                DawnContext.create(gpuTestConfig()).useWithProgress { context ->
                    context.requestAdapter().getOrThrow().use { adapter ->
                        adapter.requestDevice(DeviceDescriptor(onUncapturedError = GPUUncapturedErrorCallback {
                            assertIs<GPUValidationError>(it)
                            deliveries++
                            if (deliveries == 1) {
                                first.complete(Unit)
                                throw IllegalStateException("public consumer failed")
                            }
                            second.complete(Unit)
                        })).getOrThrow().use { device ->
                            device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
                            device.queue.onSubmittedWorkDone().getOrThrow()
                            withTimeout(5.seconds) { first.await() }
                            device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
                            device.queue.onSubmittedWorkDone().getOrThrow()
                            withTimeout(5.seconds) { second.await() }
                            device.queue.onSubmittedWorkDone().getOrThrow()
                        }
                    }
                }
            }
        }
        assertEquals(2, deliveries)
        assertContains(output, "Validation")
        assertContains(output, "IllegalStateException")
        assertContains(output, "public consumer failed")
    }

    @Test
    fun callbackCannotReenterProgressOrCloseItsContext() {
        if (!runGpuTest { gpuTestEnvironment("PublishedApiGpuTest.callbackCannotReenterProgressOrCloseItsContext") }) return
        var deliveries = 0
        val observed = List(3) { CompletableDeferred<Unit>() }
        val output = captureStandardError {
            runGpuTest {
                DawnContext.create(gpuTestConfig()).useWithProgress { context ->
                    context.requestAdapter().getOrThrow().use { adapter ->
                        adapter.requestDevice(DeviceDescriptor(onUncapturedError = GPUUncapturedErrorCallback {
                            observed[deliveries++].complete(Unit)
                            when (deliveries) {
                                1 -> context.processEvents()
                                2 -> context.close()
                            }
                        })).getOrThrow().use { device ->
                            for (delivery in observed) {
                                device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
                                device.queue.onSubmittedWorkDone().getOrThrow()
                                withTimeout(5.seconds) { delivery.await() }
                            }
                            device.queue.onSubmittedWorkDone().getOrThrow()
                        }
                    }
                }
            }
        }
        assertEquals(3, deliveries)
        assertContains(output, "processEvents() is already active")
        assertContains(output, "cannot close during processEvents()")
    }

    @Test
    fun scopedErrorsDoNotReachTheCallbackOrFallbackDiagnostic() {
        if (!runGpuTest { gpuTestEnvironment("PublishedApiGpuTest.scopedErrorsDoNotReachTheCallbackOrFallbackDiagnostic") }) return
        var deliveries = 0
        runGpuTest {
            DawnContext.create(gpuTestConfig()).useWithProgress { context ->
                // Each WebGPU adapter is consumed by its first device request.
                for (descriptor in listOf(
                    DeviceDescriptor(onUncapturedError = GPUUncapturedErrorCallback { deliveries++ }),
                    DeviceDescriptor(),
                )) {
                    context.requestAdapter().getOrThrow().use { adapter ->
                        adapter.requestDevice(descriptor).getOrThrow().use { device ->
                            // Adapter/device initialization can emit native driver warnings.
                            // Capture only the scoped operations, without filtering diagnostics.
                            val output = captureStandardError {
                                runGpuTest {
                                    device.pushErrorScope(GPUErrorFilter.Validation)
                                    device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
                                    assertIs<GPUValidationError>(device.popErrorScope().getOrThrow())
                                    device.queue.onSubmittedWorkDone().getOrThrow()
                                }
                            }
                            assertEquals("", output)
                        }
                    }
                }
            }
        }
        assertEquals(0, deliveries)
    }

    /** Real-time bound includes adapter/device requests and queue barriers, not only callback waits. */
    private fun <T> runGpuTest(block: suspend () -> T): T = runBlocking {
        withTimeout(15.seconds) { block() }
    }
}
