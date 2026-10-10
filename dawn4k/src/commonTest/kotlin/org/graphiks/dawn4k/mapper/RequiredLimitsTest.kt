package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUDeviceDescriptor
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.RequiredLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequiredLimitsTest {
    @Test
    fun absentRequirementsUseWidthSpecificSentinelsRatherThanZero() = memoryScope { allocator ->
        val native = allocator.allocateRequiredLimits(RequiredLimits())
        assertEquals(UInt.MAX_VALUE, native.maxTextureDimension1D)
        assertEquals(UInt.MAX_VALUE, native.minUniformBufferOffsetAlignment)
        assertEquals(UInt.MAX_VALUE, native.maxComputeWorkgroupsPerDimension)
        assertEquals(UInt.MAX_VALUE, native.maxImmediateSize)
        assertEquals(ULong.MAX_VALUE, native.maxUniformBufferBindingSize)
        assertEquals(ULong.MAX_VALUE, native.maxStorageBufferBindingSize)
        assertEquals(ULong.MAX_VALUE, native.maxBufferSize)
        val compatibility = compatibilityLimitsOf(native)
        assertEquals(UInt.MAX_VALUE, compatibility.maxStorageBuffersInVertexStage)
        assertEquals(UInt.MAX_VALUE, compatibility.maxStorageTexturesInFragmentStage)
    }

    @Test
    fun partialRequirementsPreserveExplicitZeroAndDoNotConstrainOtherFields() = memoryScope { allocator ->
        val native = allocator.allocateRequiredLimits(
            RequiredLimits(maxBindGroups = 0u, maxBufferSize = 0uL, maxStorageBuffersInFragmentStage = 2u),
        )
        assertEquals(0u, native.maxBindGroups)
        assertEquals(0uL, native.maxBufferSize)
        assertEquals(2u, compatibilityLimitsOf(native).maxStorageBuffersInFragmentStage)
        assertEquals(UInt.MAX_VALUE, native.maxVertexBuffers)
        assertEquals(ULong.MAX_VALUE, native.maxStorageBufferBindingSize)
    }

    @Test
    fun nullLimitsKeepANullNativeRequestPointer() = memoryScope { allocator ->
        val native = WGPUDeviceDescriptor.allocate(allocator)
        native.applyDeviceDescriptor(DeviceDescriptor(), allocator)
        assertNull(native.requiredLimits)
    }
}
