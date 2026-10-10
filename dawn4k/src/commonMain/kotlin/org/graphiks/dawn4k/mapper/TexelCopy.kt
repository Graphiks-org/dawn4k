package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.WGPUExtent3D
import org.graphiks.dawn4k.native.WGPUTexelCopyBufferInfo
import org.graphiks.dawn4k.native.WGPUTexelCopyBufferLayout
import org.graphiks.dawn4k.native.WGPUTexelCopyTextureInfo
import org.graphiks.dawn4k.requireDawnBuffer
import org.graphiks.dawn4k.requireDawnTexture
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUExtent3D
import org.graphiks.webgpu.GPUTexelCopyBufferInfo
import org.graphiks.webgpu.GPUTexelCopyBufferLayout
import org.graphiks.webgpu.GPUTexelCopyTextureInfo

/**
 * Allocates a [WGPUTexelCopyBufferLayout] with every field set explicitly (kffi
 * does not zero memory on Kotlin/Native): the `WGPU_TEXEL_COPY_BUFFER_LAYOUT_INIT`
 * equivalent. A null bytesPerRow / rowsPerImage maps to the
 * `WGPU_COPY_STRIDE_UNDEFINED` sentinel.
 */
internal fun MemoryAllocator.allocateTexelCopyBufferLayout(
    layout: GPUTexelCopyBufferLayout,
): WGPUTexelCopyBufferLayout {
    val native = WGPUTexelCopyBufferLayout.allocate(this)
    native.offset = layout.offset
    native.bytesPerRow = layout.bytesPerRow ?: WGPU_COPY_STRIDE_UNDEFINED
    native.rowsPerImage = layout.rowsPerImage ?: WGPU_COPY_STRIDE_UNDEFINED
    return native
}

/**
 * Allocates a [WGPUTexelCopyBufferInfo] carrying the layout and the buffer, which
 * must belong to [session]; a foreign buffer is refused before any handle is read.
 */
internal fun MemoryAllocator.allocateTexelCopyBufferInfo(
    info: GPUTexelCopyBufferInfo,
    session: DeviceSession,
): WGPUTexelCopyBufferInfo {
    val buffer = info.buffer.requireDawnBuffer(session)
    val native = WGPUTexelCopyBufferInfo.allocate(this)
    native.layout.offset = info.offset
    native.layout.bytesPerRow = info.bytesPerRow ?: WGPU_COPY_STRIDE_UNDEFINED
    native.layout.rowsPerImage = info.rowsPerImage ?: WGPU_COPY_STRIDE_UNDEFINED
    native.buffer = buffer.handle
    return native
}

/**
 * Allocates a [WGPUTexelCopyTextureInfo] whose texture belongs to [session]; a
 * foreign texture is refused before any handle is read.
 */
internal fun MemoryAllocator.allocateTexelCopyTextureInfo(
    info: GPUTexelCopyTextureInfo,
    session: DeviceSession,
): WGPUTexelCopyTextureInfo {
    val texture = info.texture.requireDawnTexture(session)
    val native = WGPUTexelCopyTextureInfo.allocate(this)
    native.texture = texture.handle
    native.mipLevel = info.mipLevel
    native.origin.x = info.origin.x
    native.origin.y = info.origin.y
    native.origin.z = info.origin.z
    native.aspect = info.aspect.toNativeTextureAspect()
    return native
}

/** Allocates a [WGPUExtent3D] with its three coordinates set explicitly. */
internal fun MemoryAllocator.allocateExtent3D(extent: GPUExtent3D): WGPUExtent3D {
    val native = WGPUExtent3D.allocate(this)
    native.width = extent.width
    native.height = extent.height
    native.depthOrArrayLayers = extent.depthOrArrayLayers
    return native
}
