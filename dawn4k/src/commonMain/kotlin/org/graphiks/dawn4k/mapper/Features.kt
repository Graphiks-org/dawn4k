package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUSupportedFeatures
import org.graphiks.dawn4k.native.wgpuSupportedFeaturesFreeMembers
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.MemoryBuffer
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUSupportedFeatures

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
    // Dawn-internal and experimental names outside the public vocabulary are omitted.
    return values.mapNotNull { GPUFeatureName.of(it) }.toSet()
}

/**
 * Allocates the `requiredFeatures` array of a device request from the
 * contract's names, in order; null when no feature is required (the C default
 * `NULL` + count 0). The array lives in [this] allocator's arena, consumed by
 * the native request call.
 */
internal fun MemoryAllocator.allocateRequiredFeatures(features: List<GPUFeatureName>): NativeAddress? {
    if (features.isEmpty()) return null
    val values = UIntArray(features.size) { features[it].value }
    val buffer = allocateBuffer(features.size.toULong() * 4uL)
    buffer.writeUInts(values)
    return buffer.handler
}
