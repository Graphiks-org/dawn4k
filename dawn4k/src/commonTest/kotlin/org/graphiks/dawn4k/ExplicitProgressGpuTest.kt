package org.graphiks.dawn4k

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.graphiks.dawn4k.native.WGPUFeatureName_ImplicitDeviceSynchronization
import org.graphiks.dawn4k.native.wgpuDeviceHasFeature
import org.graphiks.dawn4k.testing.gpuTestConfig
import org.graphiks.dawn4k.testing.gpuTestEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExplicitProgressGpuTest {
    @Test fun cancelledRequestsReleaseTheirLateAdapterAndDeviceBeforeContextClose() = runBlocking {
        DawnContext.create(gpuTestConfig()).use { context ->
            val cancelledAdapter = async(start = CoroutineStart.UNDISPATCHED) { context.requestAdapter() }
            cancelledAdapter.cancelAndJoin()
            assertTrue(context.hasPendingOperations())
            context.settleTestEvents()
            val adapterRequest = async(start = CoroutineStart.UNDISPATCHED) { context.requestAdapter() }
            withTimeout(10_000) { while (!adapterRequest.isCompleted) { context.processEvents(); yield() } }
            adapterRequest.await().getOrThrow().use { adapter ->
                val cancelledDevice = async(start = CoroutineStart.UNDISPATCHED) { adapter.requestDevice() }
                cancelledDevice.cancelAndJoin()
                assertTrue(context.hasPendingOperations())
                assertFailsWith<IllegalStateException> { adapter.close() }
                context.settleTestEvents()
                assertFalse(context.hasPendingOperations())
            }
        }
    }
    @Test fun teardownRemainsPendingUntilLossRoutesAndReferencesSettle() = runBlocking {
        DawnContext.create(gpuTestConfig()).useWithProgress { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                val device = adapter.requestDevice().getOrThrow()
                device.close()
                assertTrue(context.hasPendingOperations())
                assertFailsWith<IllegalStateException> { context.close() }
                context.settleTestEvents()
                assertEquals(org.graphiks.webgpu.GPUDeviceLostReason.Destroyed, device.awaitLost().getOrThrow().reason)
                assertFalse(context.hasPendingOperations())
            }
        }
    }

    @Test fun processEventsRejectsReentryAndCloseWithoutPoisoningInstance() {
        val runtime = org.graphiks.dawn4k.internal.DawnRuntime(gpuTestConfig())
        try {
            runtime.postCallback {
                assertFailsWith<IllegalStateException> { runtime.processEvents() }
                assertEquals("cannot close during processEvents()", assertFailsWith<IllegalStateException> { runtime.close() }.message)
            }
            runtime.processEvents()
            assertTrue(runtime.currentInstance() != null)
            runtime.processEvents()
        } finally { runtime.close() }
    }
    @Test fun requestRequiresExplicitSettlementAndCloseCanBeRetried() = runBlocking {
        DawnContext.create(gpuTestConfig()).use { context ->
            val request = async(start = CoroutineStart.UNDISPATCHED) { context.requestAdapter() }
            assertTrue(context.hasPendingOperations())
            assertFailsWith<IllegalStateException> { context.close() }
            withTimeout(10_000) {
                while (!request.isCompleted) { context.processEvents(); yield() }
            }
            val adapter = request.await().getOrThrow()
            assertFailsWith<IllegalStateException> { context.close() }
            assertTrue(context.nativeBridge().instanceHandle() != 0L)
            adapter.close()
            assertFalse(context.hasPendingOperations())
        }
    }

    @Test fun implicitSynchronizationIsOptInOnTheNativeDevice() = runBlocking {
        if (!gpuTestEnvironment("ExplicitProgressGpuTest.implicitSynchronizationIsOptInOnTheNativeDevice")) return@runBlocking
        for (enabled in listOf(false, true)) {
            DawnContext.create(gpuTestConfig().copy(implicitDeviceSynchronization = enabled)).use { context ->
                val adapterRequest = async(start = CoroutineStart.UNDISPATCHED) { context.requestAdapter() }
                withTimeout(10_000) { while (!adapterRequest.isCompleted) { context.processEvents(); yield() } }
                adapterRequest.await().getOrThrow().use { adapter ->
                    val deviceRequest = async(start = CoroutineStart.UNDISPATCHED) { adapter.requestDevice() }
                    withTimeout(10_000) { while (!deviceRequest.isCompleted) { context.processEvents(); yield() } }
                    (deviceRequest.await().getOrThrow() as DawnDevice).use { device ->
                        assertEquals(enabled, wgpuDeviceHasFeature(device.session.handle, WGPUFeatureName_ImplicitDeviceSynchronization) != 0u)
                    }
                }
                withTimeout(10_000) { while (context.hasPendingOperations()) { context.processEvents(); yield() } }
            }
        }
    }
}
