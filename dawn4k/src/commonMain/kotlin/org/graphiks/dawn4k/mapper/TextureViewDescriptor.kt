package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUSType_TextureComponentSwizzleDescriptor
import org.graphiks.dawn4k.native.WGPUTextureComponentSwizzleDescriptor
import org.graphiks.dawn4k.native.WGPUTextureViewDescriptor
import org.graphiks.dawn4k.native.WGPUTextureFormat_Undefined
import org.graphiks.dawn4k.native.WGPUTextureAspect_Undefined
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_Undefined
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUTextureSwizzle
import org.graphiks.webgpu.GPUTextureViewDescriptor

/**
 * Allocates a [WGPUTextureViewDescriptor] with every field set explicitly (kffi
 * does not zero memory on Kotlin/Native): the `WGPU_TEXTURE_VIEW_DESCRIPTOR_INIT`
 * equivalent. A null format/dimension maps to the native `Undefined` values, a
 * null mip/layer count maps to the `WGPU_MIP_LEVEL_COUNT_UNDEFINED` /
 * `WGPU_ARRAY_LAYER_COUNT_UNDEFINED` sentinels, and a null usage is the C default
 * zero (the full texture usage set).
 *
 * A non-identity [GPUTextureViewDescriptor.swizzle] chains a
 * [WGPUTextureComponentSwizzleDescriptor] (the `texture-component-swizzle`
 * feature). The identity mapping chains nothing, so a plain view never requests
 * an optional feature implicitly.
 */
internal fun MemoryAllocator.allocateTextureViewDescriptor(
    descriptor: GPUTextureViewDescriptor,
): WGPUTextureViewDescriptor {
    val native = WGPUTextureViewDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.format = descriptor.format?.toNativeTextureFormat() ?: WGPUTextureFormat_Undefined
    native.dimension = descriptor.dimension?.toNativeTextureViewDimension() ?: WGPUTextureViewDimension_Undefined
    native.baseMipLevel = descriptor.baseMipLevel
    native.mipLevelCount = descriptor.mipLevelCount ?: WGPU_MIP_LEVEL_COUNT_UNDEFINED
    native.baseArrayLayer = descriptor.baseArrayLayer
    native.arrayLayerCount = descriptor.arrayLayerCount ?: WGPU_ARRAY_LAYER_COUNT_UNDEFINED
    native.aspect = descriptor.aspect.toNativeTextureAspect()
    native.usage = descriptor.usage.toNativeTextureUsage()

    if (descriptor.swizzle != GPUTextureSwizzle()) {
        val swizzle = WGPUTextureComponentSwizzleDescriptor.allocate(this)
        val chain = swizzle.chain
        chain.next = null
        chain.sType = WGPUSType_TextureComponentSwizzleDescriptor
        swizzle.swizzle.r = descriptor.swizzle.red.toNativeComponentSwizzle()
        swizzle.swizzle.g = descriptor.swizzle.green.toNativeComponentSwizzle()
        swizzle.swizzle.b = descriptor.swizzle.blue.toNativeComponentSwizzle()
        swizzle.swizzle.a = descriptor.swizzle.alpha.toNativeComponentSwizzle()
        native.nextInChain = chain
    }
    return native
}
