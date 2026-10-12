package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUBufferDescriptor
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUBufferDescriptor

/** webgpu.h `WGPU_STRLEN` (SIZE_MAX): the NUL-terminated length sentinel. */
internal const val WGPU_STRLEN: ULong = ULong.MAX_VALUE

/**
 * Allocates a native [WGPUBufferDescriptor] with every field set explicitly
 * (kffi does not zero memory on Kotlin/Native) — the `WGPU_BUFFER_DESCRIPTOR_INIT`
 * equivalent. The label is left at its C default; the buffer keeps the label as
 * Kotlin-side metadata.
 */
internal fun MemoryAllocator.allocateBufferDescriptor(descriptor: GPUBufferDescriptor): WGPUBufferDescriptor {
    val native = WGPUBufferDescriptor.allocate(this)
    native.nextInChain = null
    native.label.data = null
    native.label.length = WGPU_STRLEN
    native.usage = descriptor.usage.value
    native.size = descriptor.size
    native.mappedAtCreation = if (descriptor.mappedAtCreation) 1u else 0u
    return native
}
