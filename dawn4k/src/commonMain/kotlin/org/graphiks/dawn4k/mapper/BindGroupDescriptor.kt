package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.WGPUBindGroupDescriptor
import org.graphiks.dawn4k.native.WGPUBindGroupEntry
import org.graphiks.dawn4k.requireDawnBindGroupLayout
import org.graphiks.dawn4k.requireDawnBuffer
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUBindGroupDescriptor
import org.graphiks.webgpu.GPUBindGroupEntry
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferBinding

/** webgpu.h `WGPU_WHOLE_SIZE` (UINT64_MAX): the "to the end of the buffer" size sentinel. */
private const val WGPU_WHOLE_SIZE: ULong = ULong.MAX_VALUE

/**
 * Allocates a [WGPUBindGroupDescriptor] and its entry array. The layout and every
 * binding resource must belong to [session]; a foreign object is refused with a
 * Kotlin error before any handle is extracted or any downcall is made.
 */
internal fun MemoryAllocator.allocateBindGroupDescriptor(
    descriptor: GPUBindGroupDescriptor,
    session: DeviceSession,
): WGPUBindGroupDescriptor {
    val layout = descriptor.layout.requireDawnBindGroupLayout(session)
    val native = WGPUBindGroupDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.layout = layout.handle
    native.entryCount = descriptor.entries.size.toULong()
    native.entries = if (descriptor.entries.isEmpty()) {
        null
    } else {
        WGPUBindGroupEntry.allocateArray(this, descriptor.entries.size.toUInt()) { index, entry ->
            initBindGroupEntry(entry, descriptor.entries[index.toInt()], session)
        }.let { WGPUBindGroupEntry(it.handler) }
    }
    return native
}

/** `WGPU_BIND_GROUP_ENTRY_INIT` equivalent for one buffer (or buffer-binding) resource. */
private fun initBindGroupEntry(entry: WGPUBindGroupEntry, bind: GPUBindGroupEntry, session: DeviceSession) {
    entry.nextInChain = null
    entry.binding = bind.binding
    when (val resource = bind.resource) {
        is GPUBuffer -> {
            val buffer = resource.requireDawnBuffer(session)
            entry.buffer = buffer.handle
            entry.offset = 0uL
            entry.size = WGPU_WHOLE_SIZE
        }
        is GPUBufferBinding -> {
            val buffer = resource.buffer.requireDawnBuffer(session)
            entry.buffer = buffer.handle
            entry.offset = resource.offset
            entry.size = resource.size ?: WGPU_WHOLE_SIZE
        }
        else -> throw IllegalArgumentException(
            "unsupported binding resource ${resource::class.simpleName}; only buffers are supported so far",
        )
    }
    entry.sampler = null
    entry.textureView = null
}
