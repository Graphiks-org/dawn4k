package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUFeatureName
import org.graphiks.dawn4k.native.WGPUFeatureName_BGRA8UnormStorage
import org.graphiks.dawn4k.native.WGPUFeatureName_ClipDistances
import org.graphiks.dawn4k.native.WGPUFeatureName_CoreFeaturesAndLimits
import org.graphiks.dawn4k.native.WGPUFeatureName_Depth32FloatStencil8
import org.graphiks.dawn4k.native.WGPUFeatureName_DepthClipControl
import org.graphiks.dawn4k.native.WGPUFeatureName_DualSourceBlending
import org.graphiks.dawn4k.native.WGPUFeatureName_Float32Blendable
import org.graphiks.dawn4k.native.WGPUFeatureName_Float32Filterable
import org.graphiks.dawn4k.native.WGPUFeatureName_IndirectFirstInstance
import org.graphiks.dawn4k.native.WGPUFeatureName_PrimitiveIndex
import org.graphiks.dawn4k.native.WGPUFeatureName_RG11B10UfloatRenderable
import org.graphiks.dawn4k.native.WGPUFeatureName_ShaderF16
import org.graphiks.dawn4k.native.WGPUFeatureName_Subgroups
import org.graphiks.dawn4k.native.WGPUFeatureName_TextureCompressionASTC
import org.graphiks.dawn4k.native.WGPUFeatureName_TextureCompressionASTCSliced3D
import org.graphiks.dawn4k.native.WGPUFeatureName_TextureCompressionBC
import org.graphiks.dawn4k.native.WGPUFeatureName_TextureCompressionBCSliced3D
import org.graphiks.dawn4k.native.WGPUFeatureName_TextureCompressionETC2
import org.graphiks.dawn4k.native.WGPUFeatureName_TextureComponentSwizzle
import org.graphiks.dawn4k.native.WGPUFeatureName_TextureFormatsTier1
import org.graphiks.dawn4k.native.WGPUFeatureName_TextureFormatsTier2
import org.graphiks.dawn4k.native.WGPUFeatureName_TimestampQuery
import org.graphiks.dawn4k.native.WGPUSupportedFeatures
import org.graphiks.dawn4k.native.wgpuSupportedFeaturesFreeMembers
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.MemoryBuffer
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUSupportedFeatures

/**
 * Explicit conversion table Kotlin `GPUFeatureName` -> native
 * `WGPUFeatureName`. Every entry maps through its named Dawn constant; the
 * numeric values are never assumed to coincide, and the sealed Kotlin enum
 * makes the table exhaustive at compile time.
 */
internal fun GPUFeatureName.toNativeFeatureName(): WGPUFeatureName = when (this) {
    GPUFeatureName.CoreFeaturesAndLimits -> WGPUFeatureName_CoreFeaturesAndLimits
    GPUFeatureName.DepthClipControl -> WGPUFeatureName_DepthClipControl
    GPUFeatureName.Depth32FloatStencil8 -> WGPUFeatureName_Depth32FloatStencil8
    GPUFeatureName.TextureCompressionBC -> WGPUFeatureName_TextureCompressionBC
    GPUFeatureName.TextureCompressionBCSliced3D -> WGPUFeatureName_TextureCompressionBCSliced3D
    GPUFeatureName.TextureCompressionETC2 -> WGPUFeatureName_TextureCompressionETC2
    GPUFeatureName.TextureCompressionASTC -> WGPUFeatureName_TextureCompressionASTC
    GPUFeatureName.TextureCompressionASTCSliced3D -> WGPUFeatureName_TextureCompressionASTCSliced3D
    GPUFeatureName.TimestampQuery -> WGPUFeatureName_TimestampQuery
    GPUFeatureName.IndirectFirstInstance -> WGPUFeatureName_IndirectFirstInstance
    GPUFeatureName.ShaderF16 -> WGPUFeatureName_ShaderF16
    GPUFeatureName.RG11B10UfloatRenderable -> WGPUFeatureName_RG11B10UfloatRenderable
    GPUFeatureName.BGRA8UnormStorage -> WGPUFeatureName_BGRA8UnormStorage
    GPUFeatureName.Float32Filterable -> WGPUFeatureName_Float32Filterable
    GPUFeatureName.Float32Blendable -> WGPUFeatureName_Float32Blendable
    GPUFeatureName.ClipDistances -> WGPUFeatureName_ClipDistances
    GPUFeatureName.DualSourceBlending -> WGPUFeatureName_DualSourceBlending
    GPUFeatureName.Subgroups -> WGPUFeatureName_Subgroups
    GPUFeatureName.TextureFormatsTier1 -> WGPUFeatureName_TextureFormatsTier1
    GPUFeatureName.TextureFormatsTier2 -> WGPUFeatureName_TextureFormatsTier2
    GPUFeatureName.PrimitiveIndex -> WGPUFeatureName_PrimitiveIndex
    GPUFeatureName.TextureComponentSwizzle -> WGPUFeatureName_TextureComponentSwizzle
}

/**
 * Explicit conversion table native `WGPUFeatureName` -> Kotlin
 * `GPUFeatureName`: null for the native names the published contract has no
 * capability for (Dawn-internal and experimental extensions). They are
 * projected away, never invented: the contract's feature vocabulary is closed,
 * and so is what a device request can ask for through it.
 */
internal fun WGPUFeatureName.toPublicFeatureName(): GPUFeatureName? = when (this) {
    WGPUFeatureName_CoreFeaturesAndLimits -> GPUFeatureName.CoreFeaturesAndLimits
    WGPUFeatureName_DepthClipControl -> GPUFeatureName.DepthClipControl
    WGPUFeatureName_Depth32FloatStencil8 -> GPUFeatureName.Depth32FloatStencil8
    WGPUFeatureName_TextureCompressionBC -> GPUFeatureName.TextureCompressionBC
    WGPUFeatureName_TextureCompressionBCSliced3D -> GPUFeatureName.TextureCompressionBCSliced3D
    WGPUFeatureName_TextureCompressionETC2 -> GPUFeatureName.TextureCompressionETC2
    WGPUFeatureName_TextureCompressionASTC -> GPUFeatureName.TextureCompressionASTC
    WGPUFeatureName_TextureCompressionASTCSliced3D -> GPUFeatureName.TextureCompressionASTCSliced3D
    WGPUFeatureName_TimestampQuery -> GPUFeatureName.TimestampQuery
    WGPUFeatureName_IndirectFirstInstance -> GPUFeatureName.IndirectFirstInstance
    WGPUFeatureName_ShaderF16 -> GPUFeatureName.ShaderF16
    WGPUFeatureName_RG11B10UfloatRenderable -> GPUFeatureName.RG11B10UfloatRenderable
    WGPUFeatureName_BGRA8UnormStorage -> GPUFeatureName.BGRA8UnormStorage
    WGPUFeatureName_Float32Filterable -> GPUFeatureName.Float32Filterable
    WGPUFeatureName_Float32Blendable -> GPUFeatureName.Float32Blendable
    WGPUFeatureName_ClipDistances -> GPUFeatureName.ClipDistances
    WGPUFeatureName_DualSourceBlending -> GPUFeatureName.DualSourceBlending
    WGPUFeatureName_Subgroups -> GPUFeatureName.Subgroups
    WGPUFeatureName_TextureFormatsTier1 -> GPUFeatureName.TextureFormatsTier1
    WGPUFeatureName_TextureFormatsTier2 -> GPUFeatureName.TextureFormatsTier2
    WGPUFeatureName_PrimitiveIndex -> GPUFeatureName.PrimitiveIndex
    WGPUFeatureName_TextureComponentSwizzle -> GPUFeatureName.TextureComponentSwizzle
    else -> null
}

/**
 * Copies the borrowed native feature list into a Kotlin-owned set. Must run
 * before the caller frees the snapshot's members
 * ([wgpuSupportedFeaturesFreeMembers]) and leaves the borrowed struct
 * untouched.
 */
internal fun copySupportedFeatures(features: WGPUSupportedFeatures): GPUSupportedFeatures {
    val count = features.featureCount
    val base = features.features ?: return emptySet()
    if (count == 0uL) return emptySet()
    if (count > Int.MAX_VALUE.toULong()) {
        throw IllegalStateException("the native feature list count $count is not representable")
    }
    val values = UIntArray(count.toInt())
    MemoryBuffer(base, count * 4uL).readUInts(values)
    return values.mapNotNull { it.toPublicFeatureName() }.toSet()
}

/**
 * Allocates the `requiredFeatures` array of a device request from the
 * contract's names, in order; null when no feature is required (the C default
 * `NULL` + count 0). The array lives in [this] allocator's arena, consumed by
 * the native request call.
 */
internal fun MemoryAllocator.allocateRequiredFeatures(features: List<GPUFeatureName>): NativeAddress? {
    if (features.isEmpty()) return null
    val values = UIntArray(features.size) { features[it].toNativeFeatureName() }
    val buffer = allocateBuffer(features.size.toULong() * 4uL)
    buffer.writeUInts(values)
    return buffer.handler
}
