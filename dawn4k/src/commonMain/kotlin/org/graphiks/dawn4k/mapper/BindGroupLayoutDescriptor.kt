package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUBindGroupLayoutDescriptor
import org.graphiks.dawn4k.native.WGPUBindGroupLayoutEntry
import org.graphiks.dawn4k.native.WGPUBufferBindingType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUSamplerBindingType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUTextureFormat_Undefined
import org.graphiks.dawn4k.native.WGPUTextureSampleType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_Undefined
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUBindGroupLayoutDescriptor
import org.graphiks.webgpu.GPUBindGroupLayoutEntry

/**
 * Allocates a [WGPUBindGroupLayoutDescriptor] with every field set explicitly
 * (kffi does not zero memory on Kotlin/Native): the `WGPU_BIND_GROUP_LAYOUT_DESCRIPTOR_INIT`
 * equivalent, plus one `WGPUBindGroupLayoutEntry` array.
 */
internal fun MemoryAllocator.allocateBindGroupLayoutDescriptor(
    descriptor: GPUBindGroupLayoutDescriptor,
): WGPUBindGroupLayoutDescriptor {
    val native = WGPUBindGroupLayoutDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.entryCount = descriptor.entries.size.toULong()
    native.entries = if (descriptor.entries.isEmpty()) {
        null
    } else {
        WGPUBindGroupLayoutEntry.allocateArray(this, descriptor.entries.size.toUInt()) { index, entry ->
            initBindGroupLayoutEntry(entry, descriptor.entries[index.toInt()])
        }.let { WGPUBindGroupLayoutEntry(it.handler) }
    }
    return native
}

/**
 * `WGPU_BIND_GROUP_LAYOUT_ENTRY_INIT` equivalent. Every one of the four
 * sub-layouts (buffer, sampler, texture, storage texture) is set from its
 * corresponding nullable Kotlin field, and an absent sub-layout is left at
 * BindingNotUsed/Undefined so Dawn sees exactly one resource type.
 */
private fun initBindGroupLayoutEntry(entry: WGPUBindGroupLayoutEntry, layout: GPUBindGroupLayoutEntry) {
    entry.nextInChain = null
    entry.binding = layout.binding
    entry.visibility = layout.visibility.value
    entry.bindingArraySize = 0u

    entry.buffer.nextInChain = null
    entry.buffer.type = layout.buffer?.type?.value ?: WGPUBufferBindingType_BindingNotUsed
    entry.buffer.hasDynamicOffset = if (layout.buffer?.hasDynamicOffset == true) 1u else 0u
    entry.buffer.minBindingSize = layout.buffer?.minBindingSize ?: 0uL

    entry.sampler.nextInChain = null
    entry.sampler.type = layout.sampler?.type?.value ?: WGPUSamplerBindingType_BindingNotUsed

    entry.texture.nextInChain = null
    entry.texture.sampleType = layout.texture?.sampleType?.value
        ?: WGPUTextureSampleType_BindingNotUsed
    entry.texture.viewDimension = layout.texture?.viewDimension?.value
        ?: WGPUTextureViewDimension_Undefined
    entry.texture.multisampled = if (layout.texture?.multisampled == true) 1u else 0u

    entry.storageTexture.nextInChain = null
    entry.storageTexture.access = layout.storageTexture?.access?.value
        ?: WGPUStorageTextureAccess_BindingNotUsed
    entry.storageTexture.format = layout.storageTexture?.format?.value
        ?: WGPUTextureFormat_Undefined
    entry.storageTexture.viewDimension = layout.storageTexture?.viewDimension?.value
        ?: WGPUTextureViewDimension_Undefined
}
