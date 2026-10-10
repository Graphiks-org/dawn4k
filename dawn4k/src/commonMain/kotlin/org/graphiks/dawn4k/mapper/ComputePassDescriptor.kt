package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.WGPUComputePassDescriptor
import org.graphiks.dawn4k.native.WGPUPassTimestampWrites
import org.graphiks.dawn4k.requireDawnQuerySet
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUComputePassDescriptor
import org.graphiks.webgpu.GPUComputePassTimestampWrites

/**
 * Allocates a [WGPUComputePassDescriptor] whose optional timestamp writes query
 * set belongs to [session]. `WGPU_COMPUTE_PASS_DESCRIPTOR_INIT` +
 * `WGPU_PASS_TIMESTAMP_WRITES_INIT` equivalent; an absent timestamp write index
 * maps to the `WGPU_QUERY_SET_INDEX_UNDEFINED` sentinel.
 */
internal fun MemoryAllocator.allocateComputePassDescriptor(
    descriptor: GPUComputePassDescriptor,
    session: DeviceSession,
): WGPUComputePassDescriptor {
    val native = WGPUComputePassDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.timestampWrites = descriptor.timestampWrites?.let { allocateTimestampWrites(it, session) }
    return native
}

private fun MemoryAllocator.allocateTimestampWrites(
    writes: GPUComputePassTimestampWrites,
    session: DeviceSession,
): WGPUPassTimestampWrites {
    val native = WGPUPassTimestampWrites.allocate(this)
    native.nextInChain = null
    native.querySet = writes.querySet.requireDawnQuerySet(session).handle
    native.beginningOfPassWriteIndex = writes.beginningOfPassWriteIndex ?: WGPU_QUERY_SET_INDEX_UNDEFINED
    native.endOfPassWriteIndex = writes.endOfPassWriteIndex ?: WGPU_QUERY_SET_INDEX_UNDEFINED
    return native
}
