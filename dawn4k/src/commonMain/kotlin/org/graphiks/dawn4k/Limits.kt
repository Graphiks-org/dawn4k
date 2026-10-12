package org.graphiks.dawn4k

import org.graphiks.webgpu.GPUSupportedLimits

/**
 * A copied snapshot of an adapter's or device's supported limits, every field
 * a Kotlin-owned value read from a filled native `WGPULimits` (with its
 * compatibility-mode chain). No field is ever a `WGPU_LIMIT_*_UNDEFINED`
 * value: the mapper refuses such a snapshot instead of inventing a capability.
 */
class DawnLimits internal constructor(
    override val maxTextureDimension1D: UInt,
    override val maxTextureDimension2D: UInt,
    override val maxTextureDimension3D: UInt,
    override val maxTextureArrayLayers: UInt,
    override val maxBindGroups: UInt,
    override val maxBindGroupsPlusVertexBuffers: UInt,
    override val maxImmediateSize: UInt,
    override val maxBindingsPerBindGroup: UInt,
    override val maxDynamicUniformBuffersPerPipelineLayout: UInt,
    override val maxDynamicStorageBuffersPerPipelineLayout: UInt,
    override val maxSampledTexturesPerShaderStage: UInt,
    override val maxSamplersPerShaderStage: UInt,
    override val maxStorageBuffersPerShaderStage: UInt,
    override val maxStorageBuffersInVertexStage: UInt,
    override val maxStorageBuffersInFragmentStage: UInt,
    override val maxStorageTexturesPerShaderStage: UInt,
    override val maxStorageTexturesInVertexStage: UInt,
    override val maxStorageTexturesInFragmentStage: UInt,
    override val maxUniformBuffersPerShaderStage: UInt,
    override val maxUniformBufferBindingSize: ULong,
    override val maxStorageBufferBindingSize: ULong,
    override val minUniformBufferOffsetAlignment: UInt,
    override val minStorageBufferOffsetAlignment: UInt,
    override val maxVertexBuffers: UInt,
    override val maxBufferSize: ULong,
    override val maxVertexAttributes: UInt,
    override val maxVertexBufferArrayStride: UInt,
    override val maxInterStageShaderVariables: UInt,
    override val maxColorAttachments: UInt,
    override val maxColorAttachmentBytesPerSample: UInt,
    override val maxComputeWorkgroupStorageSize: UInt,
    override val maxComputeInvocationsPerWorkgroup: UInt,
    override val maxComputeWorkgroupSizeX: UInt,
    override val maxComputeWorkgroupSizeY: UInt,
    override val maxComputeWorkgroupSizeZ: UInt,
    override val maxComputeWorkgroupsPerDimension: UInt,
) : GPUSupportedLimits
