package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPURenderBundleDescriptor
import org.graphiks.dawn4k.native.WGPURenderBundleEncoderDescriptor
import org.graphiks.dawn4k.native.WGPUTextureFormat_Undefined
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPURenderBundleDescriptor
import org.graphiks.webgpu.GPURenderBundleEncoderDescriptor

/**
 * Allocates a [WGPURenderBundleEncoderDescriptor] with the color formats carried
 * as an explicit UInt array in the allocator arena, and a null depth/stencil
 * format mapped to the `WGPU_TEXTURE_FORMAT_INIT` (Undefined) value.
 * `WGPU_RENDER_BUNDLE_ENCODER_DESCRIPTOR_INIT` equivalent.
 */
internal fun MemoryAllocator.allocateRenderBundleEncoderDescriptor(
    descriptor: GPURenderBundleEncoderDescriptor,
): WGPURenderBundleEncoderDescriptor {
    val native = WGPURenderBundleEncoderDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.colorFormatCount = descriptor.colorFormats.size.toULong()
    native.colorFormats = if (descriptor.colorFormats.isEmpty()) {
        null
    } else {
        allocateBuffer((descriptor.colorFormats.size * 4).toULong()).apply {
            writeUInts(descriptor.colorFormats.map { it.toNativeTextureFormat() }.toUIntArray())
        }.handler
    }
    native.depthStencilFormat = descriptor.depthStencilFormat?.toNativeTextureFormat() ?: WGPUTextureFormat_Undefined
    native.sampleCount = descriptor.sampleCount
    native.depthReadOnly = if (descriptor.depthReadOnly) 1u else 0u
    native.stencilReadOnly = if (descriptor.stencilReadOnly) 1u else 0u
    return native
}

/** Allocates a [WGPURenderBundleDescriptor] (`WGPU_RENDER_BUNDLE_DESCRIPTOR_INIT`). */
internal fun MemoryAllocator.allocateRenderBundleDescriptor(
    descriptor: GPURenderBundleDescriptor,
): WGPURenderBundleDescriptor {
    val native = WGPURenderBundleDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    return native
}
