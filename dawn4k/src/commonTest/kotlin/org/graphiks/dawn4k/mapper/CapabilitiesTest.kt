package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUAdapterInfo
import org.graphiks.dawn4k.native.WGPUBackendType_Metal
import org.graphiks.dawn4k.native.WGPUBackendType_Undefined
import org.graphiks.dawn4k.native.WGPUErrorFilter_Internal
import org.graphiks.dawn4k.native.WGPUErrorFilter_OutOfMemory
import org.graphiks.dawn4k.native.WGPUErrorFilter_Validation
import org.graphiks.dawn4k.native.WGPUFeatureName_BGRA8UnormStorage
import org.graphiks.dawn4k.native.WGPUFeatureName_CoreFeaturesAndLimits
import org.graphiks.dawn4k.native.WGPUFeatureName_DepthClipControl
import org.graphiks.dawn4k.native.WGPUFeatureName_DawnInternalUsages
import org.graphiks.dawn4k.native.WGPUFeatureName_ShaderF16
import org.graphiks.dawn4k.native.WGPUFeatureName_Subgroups
import org.graphiks.dawn4k.native.WGPUFeatureName_TimestampQuery
import org.graphiks.dawn4k.native.WGPUSupportedFeatures
import org.graphiks.dawn4k.native.WGPUSType_CompatibilityModeLimits
import org.graphiks.kffi.MemoryBuffer
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPURequestAdapterOptions
import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.GPURequiredLimits
import org.graphiks.webgpu.GPUSupportedFeatures
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.RequestAdapterOptions
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure capability-mapping tests: the feature table maps every contract name
 * through its named Dawn constant (never assuming the numeric values
 * coincide), native names outside the contract project to no capability, the
 * limits chain links `WGPUCompatibilityModeLimits` for both the required-limits
 * write and the snapshot read, and a field C leaves `WGPU_LIMIT_*_UNDEFINED`
 * is refused with a diagnostic instead of being invented into a capability.
 */
class CapabilitiesTest {

    // --- Features -----------------------------------------------------------

    @Test
    fun featureTableIsExhaustiveInBothDirections() {
        assertEquals(22, GPUFeatureName.entries.size, "The published contract feature list changed")
        for ((index, feature) in GPUFeatureName.entries.withIndex()) {
            assertEquals((index + 1).toUInt(), feature.toNativeFeatureName())
            assertEquals(feature, feature.toNativeFeatureName().toPublicFeatureName())
        }
        assertEquals(WGPUFeatureName_CoreFeaturesAndLimits, GPUFeatureName.CoreFeaturesAndLimits.toNativeFeatureName())
        assertEquals(WGPUFeatureName_DepthClipControl, GPUFeatureName.DepthClipControl.toNativeFeatureName())
        assertEquals(WGPUFeatureName_TimestampQuery, GPUFeatureName.TimestampQuery.toNativeFeatureName())
        assertEquals(WGPUFeatureName_ShaderF16, GPUFeatureName.ShaderF16.toNativeFeatureName())
        assertEquals(WGPUFeatureName_Subgroups, GPUFeatureName.Subgroups.toNativeFeatureName())
        assertEquals(WGPUFeatureName_BGRA8UnormStorage, GPUFeatureName.BGRA8UnormStorage.toNativeFeatureName())
    }

    @Test
    fun outOfContractNativeFeaturesProjectToNoCapability() {
        assertNull(WGPUFeatureName_DawnInternalUsages.toPublicFeatureName())
        assertEquals(GPUFeatureName.TimestampQuery, WGPUFeatureName_TimestampQuery.toPublicFeatureName())
    }

    @Test
    fun supportedFeaturesAreCopiedOut() = memoryScope { allocator ->
        val buffer = allocator.allocateBuffer(3uL * 4uL)
        buffer.writeUInts(uintArrayOf(WGPUFeatureName_TimestampQuery, WGPUFeatureName_Subgroups, 0xF0000000u))
        val supported = WGPUSupportedFeatures.allocate(allocator)
        supported.featureCount = 3uL
        supported.features = buffer.handler
        // The copy borrows only; the FreeMembers cleanup stays with the native
        // snapshot owner (the real path frees Dawn-owned members there).
        val features = copySupportedFeatures(supported)
        // The out-of-contract native name is projected away, not invented.
        assertEquals(setOf(GPUFeatureName.TimestampQuery, GPUFeatureName.Subgroups), features)
        assertEquals(3uL, supported.featureCount, "the copy leaves the borrowed struct untouched")
    }

    @Test
    fun requiredFeaturesArrayCarriesEveryNameInOrder() = memoryScope { allocator ->
        val required = listOf(GPUFeatureName.TimestampQuery, GPUFeatureName.ShaderF16)
        val address = allocator.allocateRequiredFeatures(required)
        val values = UIntArray(2)
        MemoryBuffer(address ?: error("the required features array was not allocated"), 8uL).readUInts(values)
        assertContentEquals(uintArrayOf(WGPUFeatureName_TimestampQuery, WGPUFeatureName_ShaderF16), values)
    }

    @Test
    fun emptyRequiredFeaturesStayEmpty() = memoryScope { allocator ->
        assertNull(allocator.allocateRequiredFeatures(emptyList()))
    }

    // --- Limits -------------------------------------------------------------

    @Test
    fun requiredLimitsChainTheCompatibilityModeStructAndCopyEveryField() = memoryScope { allocator ->
        val limits = TestLimits()
        val native = allocator.allocateRequiredLimits(limits)
        val chain = native.nextInChain ?: error("the compatibility-mode limits were not chained")
        assertEquals(WGPUSType_CompatibilityModeLimits, chain.sType)
        assertEquals(limits.maxTextureDimension1D, native.maxTextureDimension1D)
        assertEquals(limits.maxTextureDimension2D, native.maxTextureDimension2D)
        assertEquals(limits.maxTextureDimension3D, native.maxTextureDimension3D)
        assertEquals(limits.maxTextureArrayLayers, native.maxTextureArrayLayers)
        assertEquals(limits.maxBindGroups, native.maxBindGroups)
        assertEquals(limits.maxBindGroupsPlusVertexBuffers, native.maxBindGroupsPlusVertexBuffers)
        assertEquals(limits.maxImmediateSize, native.maxImmediateSize)
        assertEquals(limits.maxBindingsPerBindGroup, native.maxBindingsPerBindGroup)
        assertEquals(limits.maxDynamicUniformBuffersPerPipelineLayout, native.maxDynamicUniformBuffersPerPipelineLayout)
        assertEquals(limits.maxDynamicStorageBuffersPerPipelineLayout, native.maxDynamicStorageBuffersPerPipelineLayout)
        assertEquals(limits.maxSampledTexturesPerShaderStage, native.maxSampledTexturesPerShaderStage)
        assertEquals(limits.maxSamplersPerShaderStage, native.maxSamplersPerShaderStage)
        assertEquals(limits.maxStorageBuffersPerShaderStage, native.maxStorageBuffersPerShaderStage)
        assertEquals(limits.maxStorageTexturesPerShaderStage, native.maxStorageTexturesPerShaderStage)
        assertEquals(limits.maxUniformBuffersPerShaderStage, native.maxUniformBuffersPerShaderStage)
        assertEquals(limits.maxUniformBufferBindingSize, native.maxUniformBufferBindingSize)
        assertEquals(limits.maxStorageBufferBindingSize, native.maxStorageBufferBindingSize)
        assertEquals(limits.minUniformBufferOffsetAlignment, native.minUniformBufferOffsetAlignment)
        assertEquals(limits.minStorageBufferOffsetAlignment, native.minStorageBufferOffsetAlignment)
        assertEquals(limits.maxVertexBuffers, native.maxVertexBuffers)
        assertEquals(limits.maxBufferSize, native.maxBufferSize)
        assertEquals(limits.maxVertexAttributes, native.maxVertexAttributes)
        assertEquals(limits.maxVertexBufferArrayStride, native.maxVertexBufferArrayStride)
        assertEquals(limits.maxInterStageShaderVariables, native.maxInterStageShaderVariables)
        assertEquals(limits.maxColorAttachments, native.maxColorAttachments)
        assertEquals(limits.maxColorAttachmentBytesPerSample, native.maxColorAttachmentBytesPerSample)
        assertEquals(limits.maxComputeWorkgroupStorageSize, native.maxComputeWorkgroupStorageSize)
        assertEquals(limits.maxComputeInvocationsPerWorkgroup, native.maxComputeInvocationsPerWorkgroup)
        assertEquals(limits.maxComputeWorkgroupSizeX, native.maxComputeWorkgroupSizeX)
        assertEquals(limits.maxComputeWorkgroupSizeY, native.maxComputeWorkgroupSizeY)
        assertEquals(limits.maxComputeWorkgroupSizeZ, native.maxComputeWorkgroupSizeZ)
        assertEquals(limits.maxComputeWorkgroupsPerDimension, native.maxComputeWorkgroupsPerDimension)
        val compatibility = compatibilityLimitsOf(native)
        assertEquals(limits.maxStorageBuffersInVertexStage, compatibility.maxStorageBuffersInVertexStage)
        assertEquals(limits.maxStorageTexturesInVertexStage, compatibility.maxStorageTexturesInVertexStage)
        assertEquals(limits.maxStorageBuffersInFragmentStage, compatibility.maxStorageBuffersInFragmentStage)
        assertEquals(limits.maxStorageTexturesInFragmentStage, compatibility.maxStorageTexturesInFragmentStage)
    }

    @Test
    fun limitsSnapshotRoundTripsEveryField() = memoryScope { allocator ->
        val native = allocator.allocateRequiredLimits(TestLimits())
        val copied = readLimits(native)
        assertLimitsEqual(TestLimits(), copied)
    }

    @Test
    fun undefinedSnapshotFieldsAreRefusedNotInvented() = memoryScope { allocator ->
        val native = allocator.allocateLimitsSnapshot()
        // The snapshot starts entirely undefined: C must fill every field, and
        // a field it leaves undefined can never become a Kotlin capability.
        val failure = assertFailsWith<DawnUndefinedLimitException> { readLimits(native) }
        assertEquals("maxTextureDimension1D", failure.field)
    }

    @Test
    fun oneUndefinedFieldStopsTheWholeSnapshot() = memoryScope { allocator ->
        val native = allocator.allocateLimitsSnapshot()
        native.maxTextureDimension1D = 8192u
        native.maxTextureDimension2D = 8192u
        val failure = assertFailsWith<DawnUndefinedLimitException> { readLimits(native) }
        assertEquals("maxTextureDimension3D", failure.field)
    }

    // --- Adapter options ----------------------------------------------------

    @Test
    fun windowsBackendIsSentToTheAdapterRequest() = memoryScope { allocator ->
        val backend = org.graphiks.dawn4k.DawnBackend.valueOf("D3D12")
        val options = allocator.allocateRequestAdapterOptions(null, backend)
        assertEquals(org.graphiks.dawn4k.native.WGPUBackendType_D3D12, options.backendType)
        assertNull(options.compatibleSurface)
    }

    @Test
    fun nullAdapterOptionsRequestTheConfiguredBackend() = memoryScope { allocator ->
        val options = allocator.allocateRequestAdapterOptions(null, org.graphiks.dawn4k.DawnBackend.Metal)
        assertNull(options.nextInChain)
        assertEquals(WGPUBackendType_Metal, options.backendType)
        assertEquals(0u, options.featureLevel)
        assertEquals(0u, options.powerPreference)
        assertEquals(0u, options.forceFallbackAdapter)
        assertNull(options.compatibleSurface)
    }

    @Test
    fun adapterOptionsMergeContractHintsAndBackend() = memoryScope { allocator ->
        val options = allocator.allocateRequestAdapterOptions(
            RequestAdapterOptions(
                featureLevel = "core",
                powerPreference = org.graphiks.webgpu.GPUPowerPreference.LowPower,
                forceFallbackAdapter = true,
            ),
            null,
        )
        assertEquals(WGPUBackendType_Undefined, options.backendType)
        assertEquals(2u, options.featureLevel)
        assertEquals(1u, options.powerPreference)
        assertEquals(1u, options.forceFallbackAdapter)
    }

    @Test
    fun unknownFeatureLevelIsRefused() {
        memoryScope { allocator ->
            val unknown = object : GPURequestAdapterOptions {
                override val featureLevel = "experimental"
                override val powerPreference = null
                override val forceFallbackAdapter = false
                override val xrCompatible = false
            }
            assertFailsWith<IllegalArgumentException> {
                allocator.allocateRequestAdapterOptions(unknown, null)
            }
        }
    }

    // --- Device descriptor --------------------------------------------------

    @Test
    fun deviceDescriptorCarriesCapabilitiesAndKeepsLabelsKotlinSide() = memoryScope { allocator ->
        val native = org.graphiks.dawn4k.native.WGPUDeviceDescriptor.allocate(allocator)
        // The runtime's C-default equivalent runs first in the real path
        // (WGPU_DEVICE_DESCRIPTOR_INIT): empty string views, no requirements.
        native.nextInChain = null
        native.label.data = null
        native.label.length = ULong.MAX_VALUE
        native.requiredFeatureCount = 0uL
        native.requiredFeatures = null
        native.requiredLimits = null
        val defaultQueue = native.defaultQueue
        defaultQueue.nextInChain = null
        defaultQueue.label.data = null
        defaultQueue.label.length = ULong.MAX_VALUE

        native.applyDeviceDescriptor(
            DeviceDescriptor(
                label = "device-label-λ",
                requiredFeatures = listOf(GPUFeatureName.TimestampQuery),
                requiredLimits = TestLimits(),
            ),
            allocator,
        )

        assertEquals(1uL, native.requiredFeatureCount)
        val features = native.requiredFeatures ?: error("the required features array was not allocated")
        val names = UIntArray(1)
        MemoryBuffer(features, 4uL).readUInts(names)
        assertContentEquals(uintArrayOf(WGPUFeatureName_TimestampQuery), names)
        assertTrue(native.requiredLimits != null, "the required limits were not chained")
        assertNull(native.label.data, "labels stay Kotlin-side metadata; the C label keeps its default")
        assertNull(native.defaultQueue.label.data)
    }

    @Test
    fun deviceDescriptorWithoutCapabilitiesKeepsTheDefaults() = memoryScope { allocator ->
        val native = org.graphiks.dawn4k.native.WGPUDeviceDescriptor.allocate(allocator)
        native.nextInChain = null
        native.label.data = null
        native.label.length = ULong.MAX_VALUE
        native.requiredFeatureCount = 0uL
        native.requiredFeatures = null
        native.requiredLimits = null
        val defaultQueue = native.defaultQueue
        defaultQueue.nextInChain = null
        defaultQueue.label.data = null
        defaultQueue.label.length = ULong.MAX_VALUE

        native.applyDeviceDescriptor(null, allocator)

        assertEquals(0uL, native.requiredFeatureCount)
        assertNull(native.requiredFeatures)
        assertNull(native.requiredLimits)
    }

    // --- Adapter info -------------------------------------------------------

    @Test
    fun adapterInfoStringsAreCopiedOut() = memoryScope { allocator ->
        val info = allocator.allocateAdapterInfoSnapshot()
        info.vendor.data = allocator.allocateFrom("vendor-λ")
        info.vendor.length = 9uL
        info.architecture.data = allocator.allocateFrom("arch")
        info.architecture.length = 4uL
        info.device.data = allocator.allocateFrom("device")
        info.device.length = 6uL
        info.description.data = allocator.allocateFrom("description")
        info.description.length = 11uL
        info.subgroupMinSize = 4u
        info.subgroupMaxSize = 128u
        // The copy borrows only; the FreeMembers cleanup stays with the
        // native snapshot owner (the real path frees Dawn-owned members there).
        val copied = copyAdapterInfo(info, isFallbackAdapter = true)
        assertEquals("vendor-λ", copied.vendor)
        assertEquals("arch", copied.architecture)
        assertEquals("device", copied.device)
        assertEquals("description", copied.description)
        assertEquals(4u, copied.subgroupMinSize)
        assertEquals(128u, copied.subgroupMaxSize)
        assertTrue(copied.isFallbackAdapter)
    }

    // --- Error filters ------------------------------------------------------

    @Test
    fun errorFilterTableIsExhaustive() {
        assertEquals(WGPUErrorFilter_Validation, GPUErrorFilter.Validation.toNativeErrorFilter())
        assertEquals(WGPUErrorFilter_OutOfMemory, GPUErrorFilter.OutOfMemory.toNativeErrorFilter())
        assertEquals(WGPUErrorFilter_Internal, GPUErrorFilter.Internal.toNativeErrorFilter())
    }
}

/** Distinct, ordered values for every limit: any cross-field mixup shows up. */
private class TestLimits : GPUSupportedLimits, GPURequiredLimits {
    override val maxTextureDimension1D = 11u
    override val maxTextureDimension2D = 12u
    override val maxTextureDimension3D = 13u
    override val maxTextureArrayLayers = 14u
    override val maxBindGroups = 15u
    override val maxBindGroupsPlusVertexBuffers = 16u
    override val maxImmediateSize = 17u
    override val maxBindingsPerBindGroup = 18u
    override val maxDynamicUniformBuffersPerPipelineLayout = 19u
    override val maxDynamicStorageBuffersPerPipelineLayout = 20u
    override val maxSampledTexturesPerShaderStage = 21u
    override val maxSamplersPerShaderStage = 22u
    override val maxStorageBuffersPerShaderStage = 23u
    override val maxStorageBuffersInVertexStage = 24u
    override val maxStorageBuffersInFragmentStage = 25u
    override val maxStorageTexturesPerShaderStage = 26u
    override val maxStorageTexturesInVertexStage = 27u
    override val maxStorageTexturesInFragmentStage = 28u
    override val maxUniformBuffersPerShaderStage = 29u
    override val maxUniformBufferBindingSize = 30uL
    override val maxStorageBufferBindingSize = 31uL
    override val minUniformBufferOffsetAlignment = 32u
    override val minStorageBufferOffsetAlignment = 33u
    override val maxVertexBuffers = 34u
    override val maxBufferSize = 35uL
    override val maxVertexAttributes = 36u
    override val maxVertexBufferArrayStride = 37u
    override val maxInterStageShaderVariables = 38u
    override val maxColorAttachments = 39u
    override val maxColorAttachmentBytesPerSample = 40u
    override val maxComputeWorkgroupStorageSize = 41u
    override val maxComputeInvocationsPerWorkgroup = 42u
    override val maxComputeWorkgroupSizeX = 43u
    override val maxComputeWorkgroupSizeY = 44u
    override val maxComputeWorkgroupSizeZ = 45u
    override val maxComputeWorkgroupsPerDimension = 46u
}

/**
 * The published [GPUSupportedLimits] has no data class, so the round-trip
 * assertion compares field by field: every capability the mapper reads must be
 * the value it wrote, and the comparison stays honest if the contract grows.
 */
private fun assertLimitsEqual(expected: TestLimits, observed: GPUSupportedLimits) {
    assertEquals(expected.maxTextureDimension1D, observed.maxTextureDimension1D)
    assertEquals(expected.maxTextureDimension2D, observed.maxTextureDimension2D)
    assertEquals(expected.maxTextureDimension3D, observed.maxTextureDimension3D)
    assertEquals(expected.maxTextureArrayLayers, observed.maxTextureArrayLayers)
    assertEquals(expected.maxBindGroups, observed.maxBindGroups)
    assertEquals(expected.maxBindGroupsPlusVertexBuffers, observed.maxBindGroupsPlusVertexBuffers)
    assertEquals(expected.maxImmediateSize, observed.maxImmediateSize)
    assertEquals(expected.maxBindingsPerBindGroup, observed.maxBindingsPerBindGroup)
    assertEquals(expected.maxDynamicUniformBuffersPerPipelineLayout, observed.maxDynamicUniformBuffersPerPipelineLayout)
    assertEquals(expected.maxDynamicStorageBuffersPerPipelineLayout, observed.maxDynamicStorageBuffersPerPipelineLayout)
    assertEquals(expected.maxSampledTexturesPerShaderStage, observed.maxSampledTexturesPerShaderStage)
    assertEquals(expected.maxSamplersPerShaderStage, observed.maxSamplersPerShaderStage)
    assertEquals(expected.maxStorageBuffersPerShaderStage, observed.maxStorageBuffersPerShaderStage)
    assertEquals(expected.maxStorageBuffersInVertexStage, observed.maxStorageBuffersInVertexStage)
    assertEquals(expected.maxStorageBuffersInFragmentStage, observed.maxStorageBuffersInFragmentStage)
    assertEquals(expected.maxStorageTexturesPerShaderStage, observed.maxStorageTexturesPerShaderStage)
    assertEquals(expected.maxStorageTexturesInVertexStage, observed.maxStorageTexturesInVertexStage)
    assertEquals(expected.maxStorageTexturesInFragmentStage, observed.maxStorageTexturesInFragmentStage)
    assertEquals(expected.maxUniformBuffersPerShaderStage, observed.maxUniformBuffersPerShaderStage)
    assertEquals(expected.maxUniformBufferBindingSize, observed.maxUniformBufferBindingSize)
    assertEquals(expected.maxStorageBufferBindingSize, observed.maxStorageBufferBindingSize)
    assertEquals(expected.minUniformBufferOffsetAlignment, observed.minUniformBufferOffsetAlignment)
    assertEquals(expected.minStorageBufferOffsetAlignment, observed.minStorageBufferOffsetAlignment)
    assertEquals(expected.maxVertexBuffers, observed.maxVertexBuffers)
    assertEquals(expected.maxBufferSize, observed.maxBufferSize)
    assertEquals(expected.maxVertexAttributes, observed.maxVertexAttributes)
    assertEquals(expected.maxVertexBufferArrayStride, observed.maxVertexBufferArrayStride)
    assertEquals(expected.maxInterStageShaderVariables, observed.maxInterStageShaderVariables)
    assertEquals(expected.maxColorAttachments, observed.maxColorAttachments)
    assertEquals(expected.maxColorAttachmentBytesPerSample, observed.maxColorAttachmentBytesPerSample)
    assertEquals(expected.maxComputeWorkgroupStorageSize, observed.maxComputeWorkgroupStorageSize)
    assertEquals(expected.maxComputeInvocationsPerWorkgroup, observed.maxComputeInvocationsPerWorkgroup)
    assertEquals(expected.maxComputeWorkgroupSizeX, observed.maxComputeWorkgroupSizeX)
    assertEquals(expected.maxComputeWorkgroupSizeY, observed.maxComputeWorkgroupSizeY)
    assertEquals(expected.maxComputeWorkgroupSizeZ, observed.maxComputeWorkgroupSizeZ)
    assertEquals(expected.maxComputeWorkgroupsPerDimension, observed.maxComputeWorkgroupsPerDimension)
}
