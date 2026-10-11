package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUBindGroupDescriptor
import org.graphiks.dawn4k.native.WGPUBindGroupEntry
import org.graphiks.dawn4k.DawnBindGroupLayout
import org.graphiks.dawn4k.DawnBuffer
import org.graphiks.dawn4k.DawnSampler
import org.graphiks.dawn4k.DawnTextureView
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUBindGroupDescriptor
import org.graphiks.webgpu.GPUBindGroupEntry
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferBinding
import org.graphiks.webgpu.GPUSampler
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureView

/**
 * Allocates a [WGPUBindGroupDescriptor] and its entry array.
 * Dawn validates device ownership of the layout and binding resources.
 */
internal fun MemoryAllocator.allocateBindGroupDescriptor(
    descriptor: GPUBindGroupDescriptor,
): WGPUBindGroupDescriptor {
    val layout = descriptor.layout as DawnBindGroupLayout
    val native = WGPUBindGroupDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.layout = layout.handle
    native.entryCount = descriptor.entries.size.toULong()
    native.entries = if (descriptor.entries.isEmpty()) {
        null
    } else {
        WGPUBindGroupEntry.allocateArray(this, descriptor.entries.size.toUInt()) { index, entry ->
            initBindGroupEntry(entry, descriptor.entries[index.toInt()])
        }.let { WGPUBindGroupEntry(it.handler) }
    }
    return native
}

/**
 * `WGPU_BIND_GROUP_ENTRY_INIT` equivalent. A buffer (or buffer-binding) resource
 * fills the buffer members, a sampler fills `sampler`, and a texture view fills
 * `textureView`; the members of the unset families are left null.
 */
private fun initBindGroupEntry(entry: WGPUBindGroupEntry, bind: GPUBindGroupEntry) {
    entry.nextInChain = null
    entry.binding = bind.binding
    entry.buffer = null
    entry.offset = 0uL
    entry.size = 0uL
    entry.sampler = null
    entry.textureView = null
    when (val resource = bind.resource) {
        is GPUBuffer -> {
            val buffer = resource as DawnBuffer
            entry.buffer = buffer.handle
            entry.offset = 0uL
            entry.size = WGPU_WHOLE_SIZE
        }
        is GPUBufferBinding -> {
            val buffer = resource.buffer as DawnBuffer
            entry.buffer = buffer.handle
            entry.offset = resource.offset
            entry.size = resource.size ?: WGPU_WHOLE_SIZE
        }
        is GPUSampler -> {
            entry.sampler = (resource as DawnSampler).handle
        }
        is GPUTextureView -> {
            entry.textureView = (resource as DawnTextureView).handle
        }
        is GPUTexture -> throw IllegalArgumentException(
            "bind a GPUTextureView instead; implicit texture views are not supported by this backend",
        )
    }
}
