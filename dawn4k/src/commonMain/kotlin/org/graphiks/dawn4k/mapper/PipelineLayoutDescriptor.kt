package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.WGPUPipelineLayoutDescriptor
import org.graphiks.dawn4k.requireDawnBindGroupLayout
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.GPUPipelineLayoutDescriptor

/**
 * Allocates a [WGPUPipelineLayoutDescriptor] with the bind-group-layout handles
 * of [GPUPipelineLayoutDescriptor.bindGroupLayouts]. Each layout must belong to
 * [session]; a foreign object is refused before its handle is extracted.
 */
internal fun MemoryAllocator.allocatePipelineLayoutDescriptor(
    descriptor: GPUPipelineLayoutDescriptor,
    session: DeviceSession,
): WGPUPipelineLayoutDescriptor {
    val layouts = descriptor.bindGroupLayouts.map { it?.requireDawnBindGroupLayout(session) }
    val native = WGPUPipelineLayoutDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.bindGroupLayoutCount = layouts.size.toULong()
    native.bindGroupLayouts = if (layouts.isEmpty()) null
        else bufferOfAddresses(layouts.map { it?.handle?.handler ?: NativeAddress(0L) }).handler
    native.immediateSize = descriptor.immediateSize
    return native
}
