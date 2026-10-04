package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUQuerySetDescriptor
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUQuerySetDescriptor

/**
 * Allocates a [WGPUQuerySetDescriptor] with every field set explicitly (kffi does
 * not zero memory on Kotlin/Native): the `WGPU_QUERY_SET_DESCRIPTOR_INIT` equivalent.
 */
internal fun MemoryAllocator.allocateQuerySetDescriptor(
    descriptor: GPUQuerySetDescriptor,
): WGPUQuerySetDescriptor {
    val native = WGPUQuerySetDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.type = descriptor.type.toNativeQueryType()
    native.count = descriptor.count
    return native
}
