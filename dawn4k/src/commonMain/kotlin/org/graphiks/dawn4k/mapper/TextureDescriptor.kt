package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUTextureDescriptor
import org.graphiks.dawn4k.native.WGPUTextureBindingViewDimension
import org.graphiks.dawn4k.native.WGPUSType_TextureBindingViewDimension
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUTextureDescriptor

/**
 * Allocates a [WGPUTextureDescriptor] with every field set explicitly (kffi does
 * not zero memory on Kotlin/Native): the `WGPU_TEXTURE_DESCRIPTOR_INIT` equivalent,
 * plus the view-format array when present. The label is left at its C default;
 * the texture keeps it as Kotlin-side metadata.
 *
 * An explicit `textureBindingViewDimension` chains [WGPUTextureBindingViewDimension]
 * in the same arena. Dawn applies the compatibility-mode restriction and ignores
 * it on core devices; an absent dimension keeps the C default without a chain.
 */
internal fun MemoryAllocator.allocateTextureDescriptor(
    descriptor: GPUTextureDescriptor,
): WGPUTextureDescriptor {
    val native = WGPUTextureDescriptor.allocate(this)
    native.nextInChain = null
    descriptor.textureBindingViewDimension?.let { dimension ->
        val bindingDimension = WGPUTextureBindingViewDimension.allocate(this)
        val chain = bindingDimension.chain
        chain.next = null
        chain.sType = WGPUSType_TextureBindingViewDimension
        bindingDimension.textureBindingViewDimension = dimension.value
        native.nextInChain = chain
    }
    native.label.initNull()
    native.usage = descriptor.usage.value
    native.dimension = descriptor.dimension.value

    val size = native.size
    size.width = descriptor.size.width
    size.height = descriptor.size.height
    size.depthOrArrayLayers = descriptor.size.depthOrArrayLayers

    native.format = descriptor.format.value
    native.mipLevelCount = descriptor.mipLevelCount
    native.sampleCount = descriptor.sampleCount

    native.viewFormatCount = descriptor.viewFormats.size.toULong()
    native.viewFormats = if (descriptor.viewFormats.isEmpty()) {
        null
    } else {
        allocateBuffer((descriptor.viewFormats.size * 4).toULong()).apply {
            writeUInts(descriptor.viewFormats.map { it.value }.toUIntArray())
        }.handler
    }
    return native
}
