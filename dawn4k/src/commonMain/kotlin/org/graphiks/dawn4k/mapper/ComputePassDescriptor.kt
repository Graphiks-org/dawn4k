package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUComputePassDescriptor
import org.graphiks.dawn4k.native.WGPUPassTimestampWrites
import org.graphiks.dawn4k.DawnQuerySet
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUComputePassDescriptor
import org.graphiks.webgpu.GPUComputePassTimestampWrites

/**
 * Allocates a [WGPUComputePassDescriptor]; Dawn validates query-set ownership.
 * `WGPU_COMPUTE_PASS_DESCRIPTOR_INIT` +
 * `WGPU_PASS_TIMESTAMP_WRITES_INIT` equivalent; an absent timestamp write index
 * maps to the `WGPU_QUERY_SET_INDEX_UNDEFINED` sentinel.
 */
internal fun MemoryAllocator.allocateComputePassDescriptor(
    descriptor: GPUComputePassDescriptor,
): WGPUComputePassDescriptor {
    val native = WGPUComputePassDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.timestampWrites = descriptor.timestampWrites?.let { allocateTimestampWrites(it) }
    return native
}

private fun MemoryAllocator.allocateTimestampWrites(
    writes: GPUComputePassTimestampWrites,
): WGPUPassTimestampWrites {
    val native = WGPUPassTimestampWrites.allocate(this)
    native.nextInChain = null
    native.querySet = (writes.querySet as DawnQuerySet).handle
    native.beginningOfPassWriteIndex = writes.beginningOfPassWriteIndex ?: WGPU_QUERY_SET_INDEX_UNDEFINED
    native.endOfPassWriteIndex = writes.endOfPassWriteIndex ?: WGPU_QUERY_SET_INDEX_UNDEFINED
    return native
}
