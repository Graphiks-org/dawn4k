package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUBufferDescriptor
import org.graphiks.dawn4k.native.WGPUBufferUsage
import org.graphiks.dawn4k.native.WGPUBufferUsage_CopyDst
import org.graphiks.dawn4k.native.WGPUBufferUsage_CopySrc
import org.graphiks.dawn4k.native.WGPUBufferUsage_Index
import org.graphiks.dawn4k.native.WGPUBufferUsage_Indirect
import org.graphiks.dawn4k.native.WGPUBufferUsage_MapRead
import org.graphiks.dawn4k.native.WGPUBufferUsage_MapWrite
import org.graphiks.dawn4k.native.WGPUBufferUsage_None
import org.graphiks.dawn4k.native.WGPUBufferUsage_QueryResolve
import org.graphiks.dawn4k.native.WGPUBufferUsage_Storage
import org.graphiks.dawn4k.native.WGPUBufferUsage_Uniform
import org.graphiks.dawn4k.native.WGPUBufferUsage_Vertex
import org.graphiks.dawn4k.native.WGPUMapMode
import org.graphiks.dawn4k.native.WGPUMapMode_None
import org.graphiks.dawn4k.native.WGPUMapMode_Read
import org.graphiks.dawn4k.native.WGPUMapMode_Write
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUBufferDescriptor
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUMapMode

/** webgpu.h `WGPU_STRLEN` (SIZE_MAX): the NUL-terminated length sentinel. */
internal const val WGPU_STRLEN: ULong = ULong.MAX_VALUE

/**
 * Explicit conversion table Kotlin `GPUBufferUsage` -> native `WGPUBufferUsage`.
 * The numeric values happen to coincide with the WebGPU header, but this module
 * never relies on that: each bit is mapped through its named constant.
 */
internal fun GPUBufferUsage.toNativeUsage(): WGPUBufferUsage {
    val value = this.value
    var usage = WGPUBufferUsage_None
    if (value and GPUBufferUsage.MapRead.value != 0uL) usage = usage or WGPUBufferUsage_MapRead
    if (value and GPUBufferUsage.MapWrite.value != 0uL) usage = usage or WGPUBufferUsage_MapWrite
    if (value and GPUBufferUsage.CopySrc.value != 0uL) usage = usage or WGPUBufferUsage_CopySrc
    if (value and GPUBufferUsage.CopyDst.value != 0uL) usage = usage or WGPUBufferUsage_CopyDst
    if (value and GPUBufferUsage.Index.value != 0uL) usage = usage or WGPUBufferUsage_Index
    if (value and GPUBufferUsage.Vertex.value != 0uL) usage = usage or WGPUBufferUsage_Vertex
    if (value and GPUBufferUsage.Uniform.value != 0uL) usage = usage or WGPUBufferUsage_Uniform
    if (value and GPUBufferUsage.Storage.value != 0uL) usage = usage or WGPUBufferUsage_Storage
    if (value and GPUBufferUsage.Indirect.value != 0uL) usage = usage or WGPUBufferUsage_Indirect
    if (value and GPUBufferUsage.QueryResolve.value != 0uL) usage = usage or WGPUBufferUsage_QueryResolve
    return usage
}

/** Explicit conversion table Kotlin `GPUMapMode` -> native `WGPUMapMode`. */
internal fun GPUMapMode.toNativeMode(): WGPUMapMode {
    val value = this.value
    var mode = WGPUMapMode_None
    if (value and GPUMapMode.Read.value != 0uL) mode = mode or WGPUMapMode_Read
    if (value and GPUMapMode.Write.value != 0uL) mode = mode or WGPUMapMode_Write
    return mode
}

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
    native.usage = descriptor.usage.toNativeUsage()
    native.size = descriptor.size
    native.mappedAtCreation = if (descriptor.mappedAtCreation) 1u else 0u
    return native
}
