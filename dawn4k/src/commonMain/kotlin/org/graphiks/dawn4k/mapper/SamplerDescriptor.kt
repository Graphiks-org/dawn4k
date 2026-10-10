package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUCompareFunction_Undefined
import org.graphiks.dawn4k.native.WGPUSamplerDescriptor
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUSamplerDescriptor

/**
 * Allocates a [WGPUSamplerDescriptor] with every field set explicitly (kffi does
 * not zero memory on Kotlin/Native): the `WGPU_SAMPLER_DESCRIPTOR_INIT` equivalent.
 * A null [GPUSamplerDescriptor.compare] maps to the native `Undefined` value (no
 * comparison sampling), and the anisotropy clamp is copied through.
 */
internal fun MemoryAllocator.allocateSamplerDescriptor(
    descriptor: GPUSamplerDescriptor,
): WGPUSamplerDescriptor {
    val native = WGPUSamplerDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.addressModeU = descriptor.addressModeU.toNativeAddressMode()
    native.addressModeV = descriptor.addressModeV.toNativeAddressMode()
    native.addressModeW = descriptor.addressModeW.toNativeAddressMode()
    native.magFilter = descriptor.magFilter.toNativeFilterMode()
    native.minFilter = descriptor.minFilter.toNativeFilterMode()
    native.mipmapFilter = descriptor.mipmapFilter.toNativeMipmapFilterMode()
    native.lodMinClamp = descriptor.lodMinClamp
    native.lodMaxClamp = descriptor.lodMaxClamp
    native.compare = descriptor.compare?.toNativeCompareFunction() ?: WGPUCompareFunction_Undefined
    native.maxAnisotropy = descriptor.maxAnisotropy
    return native
}
