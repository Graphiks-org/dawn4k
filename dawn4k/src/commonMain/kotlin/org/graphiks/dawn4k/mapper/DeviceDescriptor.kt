package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUDeviceDescriptor
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUDeviceDescriptor

/**
 * Applies the public device-request capabilities onto the runtime-initialized
 * `WGPUDeviceDescriptor` (its `WGPU_DEVICE_DESCRIPTOR_INIT` equivalent, with
 * the callback infos installed, has already run):
 *
 * - [GPUDeviceDescriptor.requiredFeatures] becomes the native feature array
 *   (empty stays the C default: count 0, `NULL`);
 * - [GPUDeviceDescriptor.requiredLimits] becomes the chained `WGPULimits` +
 *   `WGPUCompatibilityModeLimits` pair: explicit values are copied verbatim,
 *   absent values use the pinned header's width-specific undefined sentinels;
 * - the labels stay Kotlin-side metadata: the C label views keep their
 *   defaults, matching the wrappers of this backend.
 *
 * The allocations live in [allocator]'s arena, consumed by the native request
 * call.
 */
internal fun WGPUDeviceDescriptor.applyDeviceDescriptor(
    descriptor: GPUDeviceDescriptor?,
    allocator: MemoryAllocator,
) {
    val features = descriptor?.requiredFeatures.orEmpty()
    requiredFeatureCount = features.size.toULong()
    requiredFeatures = allocator.allocateRequiredFeatures(features)
    requiredLimits = descriptor?.requiredLimits?.let { allocator.allocateRequiredLimits(it) }
}
