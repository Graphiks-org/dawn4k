package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.DawnLimits
import org.graphiks.dawn4k.native.WGPUCompatibilityModeLimits
import org.graphiks.dawn4k.native.WGPULimits
import org.graphiks.dawn4k.native.WGPUSType_CompatibilityModeLimits
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUSupportedLimits

/** webgpu.h `WGPU_LIMIT_U32_UNDEFINED` (UINT32_MAX): the "C exposes no value" sentinel. */
internal const val WGPU_LIMIT_U32_UNDEFINED: UInt = UInt.MAX_VALUE

/** webgpu.h `WGPU_LIMIT_U64_UNDEFINED` (UINT64_MAX): the "C exposes no value" sentinel. */
internal const val WGPU_LIMIT_U64_UNDEFINED: ULong = ULong.MAX_VALUE

/**
 * A native limits snapshot left a field at its `WGPU_LIMIT_*_UNDEFINED`
 * sentinel: C exposes no value for it, so the Kotlin capability is never
 * invented from it. A field that cannot be implemented at all requires a
 * coordinated contract evolution, not a fabricated value.
 */
internal class DawnUndefinedLimitException(val field: String) :
    IllegalStateException(
        "the native limits snapshot left $field undefined (WGPU_LIMIT_*_UNDEFINED); " +
            "the capability is not implementable without a coordinated contract evolution",
    )

/**
 * Allocates a `WGPULimits` with the `WGPUCompatibilityModeLimits` chain linked
 * in (the storage-per-stage limits live there), every chain field pre-filled
 * with its `WGPU_COMPATIBILITY_MODE_LIMITS_INIT` equivalent. The returned pair
 * is (limits, compatibility).
 */
private fun MemoryAllocator.linkedLimits(): Pair<WGPULimits, WGPUCompatibilityModeLimits> {
    val compatibility = WGPUCompatibilityModeLimits.allocate(this)
    compatibility.chain.next = null
    compatibility.chain.sType = WGPUSType_CompatibilityModeLimits
    compatibility.maxStorageBuffersInVertexStage = WGPU_LIMIT_U32_UNDEFINED
    compatibility.maxStorageTexturesInVertexStage = WGPU_LIMIT_U32_UNDEFINED
    compatibility.maxStorageBuffersInFragmentStage = WGPU_LIMIT_U32_UNDEFINED
    compatibility.maxStorageTexturesInFragmentStage = WGPU_LIMIT_U32_UNDEFINED
    val limits = WGPULimits.allocate(this)
    limits.nextInChain = compatibility.chain
    return limits to compatibility
}

/**
 * Allocates a read-ready limits snapshot: the `WGPU_LIMITS_INIT` equivalent —
 * every `WGPULimits` field pre-filled with its `WGPU_LIMIT_*_UNDEFINED`
 * sentinel — chained to the compatibility-mode limits. A field C does not
 * fill stays undefined, and [readLimits] refuses it instead of reading
 * allocator garbage as a capability.
 */
internal fun MemoryAllocator.allocateLimitsSnapshot(): WGPULimits {
    val (limits, _) = linkedLimits()
    limits.maxTextureDimension1D = WGPU_LIMIT_U32_UNDEFINED
    limits.maxTextureDimension2D = WGPU_LIMIT_U32_UNDEFINED
    limits.maxTextureDimension3D = WGPU_LIMIT_U32_UNDEFINED
    limits.maxTextureArrayLayers = WGPU_LIMIT_U32_UNDEFINED
    limits.maxBindGroups = WGPU_LIMIT_U32_UNDEFINED
    limits.maxBindGroupsPlusVertexBuffers = WGPU_LIMIT_U32_UNDEFINED
    limits.maxBindingsPerBindGroup = WGPU_LIMIT_U32_UNDEFINED
    limits.maxDynamicUniformBuffersPerPipelineLayout = WGPU_LIMIT_U32_UNDEFINED
    limits.maxDynamicStorageBuffersPerPipelineLayout = WGPU_LIMIT_U32_UNDEFINED
    limits.maxSampledTexturesPerShaderStage = WGPU_LIMIT_U32_UNDEFINED
    limits.maxSamplersPerShaderStage = WGPU_LIMIT_U32_UNDEFINED
    limits.maxStorageBuffersPerShaderStage = WGPU_LIMIT_U32_UNDEFINED
    limits.maxStorageTexturesPerShaderStage = WGPU_LIMIT_U32_UNDEFINED
    limits.maxUniformBuffersPerShaderStage = WGPU_LIMIT_U32_UNDEFINED
    limits.maxUniformBufferBindingSize = WGPU_LIMIT_U64_UNDEFINED
    limits.maxStorageBufferBindingSize = WGPU_LIMIT_U64_UNDEFINED
    limits.minUniformBufferOffsetAlignment = WGPU_LIMIT_U32_UNDEFINED
    limits.minStorageBufferOffsetAlignment = WGPU_LIMIT_U32_UNDEFINED
    limits.maxVertexBuffers = WGPU_LIMIT_U32_UNDEFINED
    limits.maxBufferSize = WGPU_LIMIT_U64_UNDEFINED
    limits.maxVertexAttributes = WGPU_LIMIT_U32_UNDEFINED
    limits.maxVertexBufferArrayStride = WGPU_LIMIT_U32_UNDEFINED
    limits.maxInterStageShaderVariables = WGPU_LIMIT_U32_UNDEFINED
    limits.maxColorAttachments = WGPU_LIMIT_U32_UNDEFINED
    limits.maxColorAttachmentBytesPerSample = WGPU_LIMIT_U32_UNDEFINED
    limits.maxComputeWorkgroupStorageSize = WGPU_LIMIT_U32_UNDEFINED
    limits.maxComputeInvocationsPerWorkgroup = WGPU_LIMIT_U32_UNDEFINED
    limits.maxComputeWorkgroupSizeX = WGPU_LIMIT_U32_UNDEFINED
    limits.maxComputeWorkgroupSizeY = WGPU_LIMIT_U32_UNDEFINED
    limits.maxComputeWorkgroupSizeZ = WGPU_LIMIT_U32_UNDEFINED
    limits.maxComputeWorkgroupsPerDimension = WGPU_LIMIT_U32_UNDEFINED
    limits.maxImmediateSize = WGPU_LIMIT_U32_UNDEFINED
    return limits
}

/**
 * Allocates the `requiredLimits` of a device request from a
 * [GPUSupportedLimits]: the `WGPU_LIMITS_INIT` equivalent with the
 * compatibility-mode chain linked in, every field copied verbatim — the
 * contract's limits object has no "undefined" concept, so every field is a
 * concrete requirement exactly as given. The structs live in [this]
 * allocator's arena, consumed by the native request call.
 */
internal fun MemoryAllocator.allocateRequiredLimits(limits: GPUSupportedLimits): WGPULimits {
    val (native, compatibility) = linkedLimits()
    native.maxTextureDimension1D = limits.maxTextureDimension1D
    native.maxTextureDimension2D = limits.maxTextureDimension2D
    native.maxTextureDimension3D = limits.maxTextureDimension3D
    native.maxTextureArrayLayers = limits.maxTextureArrayLayers
    native.maxBindGroups = limits.maxBindGroups
    native.maxBindGroupsPlusVertexBuffers = limits.maxBindGroupsPlusVertexBuffers
    native.maxBindingsPerBindGroup = limits.maxBindingsPerBindGroup
    native.maxDynamicUniformBuffersPerPipelineLayout = limits.maxDynamicUniformBuffersPerPipelineLayout
    native.maxDynamicStorageBuffersPerPipelineLayout = limits.maxDynamicStorageBuffersPerPipelineLayout
    native.maxSampledTexturesPerShaderStage = limits.maxSampledTexturesPerShaderStage
    native.maxSamplersPerShaderStage = limits.maxSamplersPerShaderStage
    native.maxStorageBuffersPerShaderStage = limits.maxStorageBuffersPerShaderStage
    native.maxStorageTexturesPerShaderStage = limits.maxStorageTexturesPerShaderStage
    native.maxUniformBuffersPerShaderStage = limits.maxUniformBuffersPerShaderStage
    native.maxUniformBufferBindingSize = limits.maxUniformBufferBindingSize
    native.maxStorageBufferBindingSize = limits.maxStorageBufferBindingSize
    native.minUniformBufferOffsetAlignment = limits.minUniformBufferOffsetAlignment
    native.minStorageBufferOffsetAlignment = limits.minStorageBufferOffsetAlignment
    native.maxVertexBuffers = limits.maxVertexBuffers
    native.maxBufferSize = limits.maxBufferSize
    native.maxVertexAttributes = limits.maxVertexAttributes
    native.maxVertexBufferArrayStride = limits.maxVertexBufferArrayStride
    native.maxInterStageShaderVariables = limits.maxInterStageShaderVariables
    native.maxColorAttachments = limits.maxColorAttachments
    native.maxColorAttachmentBytesPerSample = limits.maxColorAttachmentBytesPerSample
    native.maxComputeWorkgroupStorageSize = limits.maxComputeWorkgroupStorageSize
    native.maxComputeInvocationsPerWorkgroup = limits.maxComputeInvocationsPerWorkgroup
    native.maxComputeWorkgroupSizeX = limits.maxComputeWorkgroupSizeX
    native.maxComputeWorkgroupSizeY = limits.maxComputeWorkgroupSizeY
    native.maxComputeWorkgroupSizeZ = limits.maxComputeWorkgroupSizeZ
    native.maxComputeWorkgroupsPerDimension = limits.maxComputeWorkgroupsPerDimension
    native.maxImmediateSize = limits.maxImmediateSize
    compatibility.maxStorageBuffersInVertexStage = limits.maxStorageBuffersInVertexStage
    compatibility.maxStorageTexturesInVertexStage = limits.maxStorageTexturesInVertexStage
    compatibility.maxStorageBuffersInFragmentStage = limits.maxStorageBuffersInFragmentStage
    compatibility.maxStorageTexturesInFragmentStage = limits.maxStorageTexturesInFragmentStage
    return native
}

/**
 * Views the `WGPUCompatibilityModeLimits` chained into [limits]; refuses a
 * missing chain (the storage-per-stage capabilities are not optional) and a
 * chain of an unexpected type.
 */
internal fun compatibilityLimitsOf(limits: WGPULimits): WGPUCompatibilityModeLimits {
    val chain = limits.nextInChain
        ?: throw DawnUndefinedLimitException("the compatibility-mode limits chain")
    if (chain.sType != WGPUSType_CompatibilityModeLimits) {
        throw IllegalStateException("unexpected chained limits type ${chain.sType}")
    }
    return WGPUCompatibilityModeLimits(chain.handler)
}

/** Refuses a `WGPU_LIMIT_U32_UNDEFINED` value instead of inventing a capability. */
private fun definedU32(field: String, value: UInt): UInt =
    if (value == WGPU_LIMIT_U32_UNDEFINED) throw DawnUndefinedLimitException(field) else value

/** Refuses a `WGPU_LIMIT_U64_UNDEFINED` value instead of inventing a capability. */
private fun definedU64(field: String, value: ULong): ULong =
    if (value == WGPU_LIMIT_U64_UNDEFINED) throw DawnUndefinedLimitException(field) else value

/**
 * Copies a filled limits snapshot into a Kotlin-owned [DawnLimits]: every
 * `WGPULimits` field plus the chained compatibility-mode storage-per-stage
 * fields. A field still at its `WGPU_LIMIT_*_UNDEFINED` sentinel — one C
 * never exposed a value for — stops the whole snapshot with a
 * [DawnUndefinedLimitException] naming it: the capability is never invented.
 */
internal fun readLimits(limits: WGPULimits): DawnLimits {
    val compatibility = compatibilityLimitsOf(limits)
    return DawnLimits(
        maxTextureDimension1D = definedU32("maxTextureDimension1D", limits.maxTextureDimension1D),
        maxTextureDimension2D = definedU32("maxTextureDimension2D", limits.maxTextureDimension2D),
        maxTextureDimension3D = definedU32("maxTextureDimension3D", limits.maxTextureDimension3D),
        maxTextureArrayLayers = definedU32("maxTextureArrayLayers", limits.maxTextureArrayLayers),
        maxBindGroups = definedU32("maxBindGroups", limits.maxBindGroups),
        maxBindGroupsPlusVertexBuffers = definedU32("maxBindGroupsPlusVertexBuffers", limits.maxBindGroupsPlusVertexBuffers),
        maxImmediateSize = definedU32("maxImmediateSize", limits.maxImmediateSize),
        maxBindingsPerBindGroup = definedU32("maxBindingsPerBindGroup", limits.maxBindingsPerBindGroup),
        maxDynamicUniformBuffersPerPipelineLayout = definedU32(
            "maxDynamicUniformBuffersPerPipelineLayout",
            limits.maxDynamicUniformBuffersPerPipelineLayout,
        ),
        maxDynamicStorageBuffersPerPipelineLayout = definedU32(
            "maxDynamicStorageBuffersPerPipelineLayout",
            limits.maxDynamicStorageBuffersPerPipelineLayout,
        ),
        maxSampledTexturesPerShaderStage = definedU32("maxSampledTexturesPerShaderStage", limits.maxSampledTexturesPerShaderStage),
        maxSamplersPerShaderStage = definedU32("maxSamplersPerShaderStage", limits.maxSamplersPerShaderStage),
        maxStorageBuffersPerShaderStage = definedU32("maxStorageBuffersPerShaderStage", limits.maxStorageBuffersPerShaderStage),
        maxStorageBuffersInVertexStage = definedU32("maxStorageBuffersInVertexStage", compatibility.maxStorageBuffersInVertexStage),
        maxStorageBuffersInFragmentStage = definedU32("maxStorageBuffersInFragmentStage", compatibility.maxStorageBuffersInFragmentStage),
        maxStorageTexturesPerShaderStage = definedU32("maxStorageTexturesPerShaderStage", limits.maxStorageTexturesPerShaderStage),
        maxStorageTexturesInVertexStage = definedU32("maxStorageTexturesInVertexStage", compatibility.maxStorageTexturesInVertexStage),
        maxStorageTexturesInFragmentStage = definedU32("maxStorageTexturesInFragmentStage", compatibility.maxStorageTexturesInFragmentStage),
        maxUniformBuffersPerShaderStage = definedU32("maxUniformBuffersPerShaderStage", limits.maxUniformBuffersPerShaderStage),
        maxUniformBufferBindingSize = definedU64("maxUniformBufferBindingSize", limits.maxUniformBufferBindingSize),
        maxStorageBufferBindingSize = definedU64("maxStorageBufferBindingSize", limits.maxStorageBufferBindingSize),
        minUniformBufferOffsetAlignment = definedU32("minUniformBufferOffsetAlignment", limits.minUniformBufferOffsetAlignment),
        minStorageBufferOffsetAlignment = definedU32("minStorageBufferOffsetAlignment", limits.minStorageBufferOffsetAlignment),
        maxVertexBuffers = definedU32("maxVertexBuffers", limits.maxVertexBuffers),
        maxBufferSize = definedU64("maxBufferSize", limits.maxBufferSize),
        maxVertexAttributes = definedU32("maxVertexAttributes", limits.maxVertexAttributes),
        maxVertexBufferArrayStride = definedU32("maxVertexBufferArrayStride", limits.maxVertexBufferArrayStride),
        maxInterStageShaderVariables = definedU32("maxInterStageShaderVariables", limits.maxInterStageShaderVariables),
        maxColorAttachments = definedU32("maxColorAttachments", limits.maxColorAttachments),
        maxColorAttachmentBytesPerSample = definedU32("maxColorAttachmentBytesPerSample", limits.maxColorAttachmentBytesPerSample),
        maxComputeWorkgroupStorageSize = definedU32("maxComputeWorkgroupStorageSize", limits.maxComputeWorkgroupStorageSize),
        maxComputeInvocationsPerWorkgroup = definedU32("maxComputeInvocationsPerWorkgroup", limits.maxComputeInvocationsPerWorkgroup),
        maxComputeWorkgroupSizeX = definedU32("maxComputeWorkgroupSizeX", limits.maxComputeWorkgroupSizeX),
        maxComputeWorkgroupSizeY = definedU32("maxComputeWorkgroupSizeY", limits.maxComputeWorkgroupSizeY),
        maxComputeWorkgroupSizeZ = definedU32("maxComputeWorkgroupSizeZ", limits.maxComputeWorkgroupSizeZ),
        maxComputeWorkgroupsPerDimension = definedU32("maxComputeWorkgroupsPerDimension", limits.maxComputeWorkgroupsPerDimension),
    )
}
