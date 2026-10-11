package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUPipelineLayoutDescriptor
import org.graphiks.dawn4k.DawnBindGroupLayout
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.GPUPipelineLayoutDescriptor

/**
 * Allocates a [WGPUPipelineLayoutDescriptor] with the bind-group-layout handles
 * of [GPUPipelineLayoutDescriptor.bindGroupLayouts]. Dawn validates device ownership.
 */
internal fun MemoryAllocator.allocatePipelineLayoutDescriptor(
    descriptor: GPUPipelineLayoutDescriptor,
): WGPUPipelineLayoutDescriptor {
    val layouts = descriptor.bindGroupLayouts.map { it as DawnBindGroupLayout? }
    val native = WGPUPipelineLayoutDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.bindGroupLayoutCount = layouts.size.toULong()
    native.bindGroupLayouts = if (layouts.isEmpty()) null
        else bufferOfAddresses(layouts.map { it?.handle?.handler ?: NativeAddress(0L) }).handler
    native.immediateSize = descriptor.immediateSize
    return native
}
