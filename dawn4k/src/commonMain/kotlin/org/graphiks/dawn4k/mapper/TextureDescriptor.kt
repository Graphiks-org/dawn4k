package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUTextureDescriptor
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUTextureDescriptor

/**
 * Allocates a [WGPUTextureDescriptor] with every field set explicitly (kffi does
 * not zero memory on Kotlin/Native): the `WGPU_TEXTURE_DESCRIPTOR_INIT` equivalent,
 * plus the view-format array when present. The label is left at its C default;
 * the texture keeps it as Kotlin-side metadata.
 *
 * The optional `textureBindingViewDimension` is deliberately not chained: the
 * `WGPUTextureBindingViewDimension` chained struct only constrains pre-core
 * devices, and it is never requested implicitly. On the core-features devices
 * this runtime requests, the field is ignored — matching the C default.
 */
internal fun MemoryAllocator.allocateTextureDescriptor(
    descriptor: GPUTextureDescriptor,
): WGPUTextureDescriptor {
    val native = WGPUTextureDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.usage = descriptor.usage.toNativeTextureUsage()
    native.dimension = descriptor.dimension.toNativeTextureDimension()

    val size = native.size
    size.width = descriptor.size.width
    size.height = descriptor.size.height
    size.depthOrArrayLayers = descriptor.size.depthOrArrayLayers

    native.format = descriptor.format.toNativeTextureFormat()
    native.mipLevelCount = descriptor.mipLevelCount
    native.sampleCount = descriptor.sampleCount

    native.viewFormatCount = descriptor.viewFormats.size.toULong()
    native.viewFormats = if (descriptor.viewFormats.isEmpty()) {
        null
    } else {
        allocateBuffer((descriptor.viewFormats.size * 4).toULong()).apply {
            writeUInts(descriptor.viewFormats.map { it.toNativeTextureFormat() }.toUIntArray())
        }.handler
    }
    return native
}
