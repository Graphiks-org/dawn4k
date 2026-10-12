package org.graphiks.dawn4k.internal

import org.graphiks.dawn4k.native.WGPUDeviceLostReason_CallbackCancelled
import org.graphiks.dawn4k.native.WGPUDeviceLostReason_Destroyed
import org.graphiks.dawn4k.native.WGPUDeviceLostReason_FailedCreation
import org.graphiks.dawn4k.native.WGPUDeviceLostReason_Unknown
import org.graphiks.webgpu.GPUDeviceLostReason
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceLostReasonTest {
    @Test
    fun nativeReasonsPreserveThePublicReason() {
        assertEquals(GPUDeviceLostReason.Unknown, deviceLostReason(WGPUDeviceLostReason_Unknown))
        assertEquals(GPUDeviceLostReason.Destroyed, deviceLostReason(WGPUDeviceLostReason_Destroyed))
        assertEquals(GPUDeviceLostReason.CallbackCancelled, deviceLostReason(WGPUDeviceLostReason_CallbackCancelled))
        assertEquals(GPUDeviceLostReason.FailedCreation, deviceLostReason(WGPUDeviceLostReason_FailedCreation))
    }

    @Test
    fun unrecognizedReasonFallsBackToUnknown() {
        assertEquals(GPUDeviceLostReason.Unknown, deviceLostReason(0u))
        assertEquals(GPUDeviceLostReason.Unknown, deviceLostReason(UInt.MAX_VALUE))
    }
}
