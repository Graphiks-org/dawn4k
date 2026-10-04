package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUBindGroupLayoutDescriptor
import org.graphiks.dawn4k.native.WGPUBindGroupLayoutEntry
import org.graphiks.dawn4k.native.WGPUBufferBindingType
import org.graphiks.dawn4k.native.WGPUBufferBindingType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUBufferBindingType_ReadOnlyStorage
import org.graphiks.dawn4k.native.WGPUBufferBindingType_Storage
import org.graphiks.dawn4k.native.WGPUBufferBindingType_Uniform
import org.graphiks.dawn4k.native.WGPUSamplerBindingType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUShaderStage
import org.graphiks.dawn4k.native.WGPUShaderStage_Compute
import org.graphiks.dawn4k.native.WGPUShaderStage_Fragment
import org.graphiks.dawn4k.native.WGPUShaderStage_None
import org.graphiks.dawn4k.native.WGPUShaderStage_Vertex
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUTextureFormat_Undefined
import org.graphiks.dawn4k.native.WGPUTextureSampleType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_Undefined
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUBindGroupLayoutDescriptor
import org.graphiks.webgpu.GPUBindGroupLayoutEntry
import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUShaderStage

/**
 * Explicit conversion table Kotlin `GPUShaderStage` -> native `WGPUShaderStage`.
 * Each set bit is mapped through its named constant; the numeric values are never
 * assumed to coincide.
 */
internal fun GPUShaderStage.toNativeShaderStage(): WGPUShaderStage {
    val value = this.value
    var stage = WGPUShaderStage_None
    if (value and GPUShaderStage.Vertex.value != 0uL) stage = stage or WGPUShaderStage_Vertex
    if (value and GPUShaderStage.Fragment.value != 0uL) stage = stage or WGPUShaderStage_Fragment
    if (value and GPUShaderStage.Compute.value != 0uL) stage = stage or WGPUShaderStage_Compute
    return stage
}

/**
 * Explicit conversion table Kotlin `GPUBufferBindingType` -> native
 * `WGPUBufferBindingType`.
 */
internal fun GPUBufferBindingType.toNativeBufferBindingType(): WGPUBufferBindingType = when (this) {
    GPUBufferBindingType.BindingNotUsed -> WGPUBufferBindingType_BindingNotUsed
    GPUBufferBindingType.Uniform -> WGPUBufferBindingType_Uniform
    GPUBufferBindingType.Storage -> WGPUBufferBindingType_Storage
    GPUBufferBindingType.ReadOnlyStorage -> WGPUBufferBindingType_ReadOnlyStorage
}

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
    entry.visibility = layout.visibility.toNativeShaderStage()
    entry.bindingArraySize = 0u

    entry.buffer.nextInChain = null
    entry.buffer.type = layout.buffer?.type?.toNativeBufferBindingType() ?: WGPUBufferBindingType_BindingNotUsed
    entry.buffer.hasDynamicOffset = if (layout.buffer?.hasDynamicOffset == true) 1u else 0u
    entry.buffer.minBindingSize = layout.buffer?.minBindingSize ?: 0uL

    entry.sampler.nextInChain = null
    entry.sampler.type = layout.sampler?.type?.toNativeSamplerBindingType() ?: WGPUSamplerBindingType_BindingNotUsed

    entry.texture.nextInChain = null
    entry.texture.sampleType = layout.texture?.sampleType?.toNativeTextureSampleType()
        ?: WGPUTextureSampleType_BindingNotUsed
    entry.texture.viewDimension = layout.texture?.viewDimension?.toNativeTextureViewDimension()
        ?: WGPUTextureViewDimension_Undefined
    entry.texture.multisampled = if (layout.texture?.multisampled == true) 1u else 0u

    entry.storageTexture.nextInChain = null
    entry.storageTexture.access = layout.storageTexture?.access?.toNativeStorageTextureAccess()
        ?: WGPUStorageTextureAccess_BindingNotUsed
    entry.storageTexture.format = layout.storageTexture?.format?.toNativeTextureFormat()
        ?: WGPUTextureFormat_Undefined
    entry.storageTexture.viewDimension = layout.storageTexture?.viewDimension?.toNativeTextureViewDimension()
        ?: WGPUTextureViewDimension_Undefined
}
