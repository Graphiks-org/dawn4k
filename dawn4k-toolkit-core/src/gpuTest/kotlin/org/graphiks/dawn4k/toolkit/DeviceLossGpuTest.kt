package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.toolkit.internal.*
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.dawn4k.native.*
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class DeviceLossGpuTest {
    @Test
    fun deviceLossStopsAdmission() = runTest {
        val session = openDawnSession()
        try {
            assertFailsWith<CancellationException> {
                session.run {
                    memoryScope { allocator ->
                        val message = WGPUStringView.allocate(allocator).apply { data = null; length = 0uL }
                        wgpuDeviceForceLoss(WGPUDevice(NativeAddress(it.device.nativeHandle())), WGPUDeviceLostReason_Unknown, message)
                    }
                    awaitCancellation()
                }
            }
            assertEquals(GPUDeviceLostReason.Unknown, session.awaitLost().getOrThrow().reason)
            assertFailsWith<IllegalStateException> { session.run { } }
        } finally { session.shutdown() }
    }

    @Test
    fun outOfMemoryStopsAdmissionAndPreservesUserCallback() = runTest {
        var userCalls = 0
        val session = openDawnSession(DawnSessionOptions(deviceDescriptor = DeviceDescriptor(
            onUncapturedError = object : GPUUncapturedErrorCallback {
                override fun onUncapturedError(error: GPUError) { userCalls++ }
            },
        )))
        try {
            assertFailsWith<CancellationException> {
                session.run {
                    memoryScope { allocator ->
                        val message = WGPUStringView.allocate(allocator).apply { data = null; length = 0uL }
                        wgpuDeviceInjectError(WGPUDevice(NativeAddress(it.device.nativeHandle())), WGPUErrorType_OutOfMemory, message)
                    }
                    awaitCancellation()
                }
            }
            assertEquals(1, userCalls)
            assertFailsWith<IllegalStateException> { session.run { } }
        } finally { session.shutdown() }
    }

}
