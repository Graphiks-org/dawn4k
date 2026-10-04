package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUShaderModuleDescriptor
import org.graphiks.dawn4k.native.WGPUShaderSourceWGSL
import org.graphiks.dawn4k.native.WGPUStringView
import org.graphiks.dawn4k.native.WGPUSType_ShaderSourceWGSL
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUShaderModuleDescriptor

/** `WGPU_STRING_VIEW_INIT` equivalent for an absent string. */
internal fun WGPUStringView.initNull() {
    data = null
    length = WGPU_STRLEN
}

/** Points the view at a NUL-terminated copy of [value] allocated in [allocator]. */
internal fun WGPUStringView.initFrom(value: String, allocator: MemoryAllocator) {
    data = allocator.allocateFrom(value)
    length = WGPU_STRLEN
}

/** [initFrom] for a nullable string: a null maps to the empty StringView. */
internal fun WGPUStringView.initNullable(value: String?, allocator: MemoryAllocator) {
    data = value?.let { allocator.allocateFrom(it) }
    length = WGPU_STRLEN
}

/**
 * Allocates a [WGPUShaderModuleDescriptor] chained to a [WGPUShaderSourceWGSL]
 * carrying [GPUShaderModuleDescriptor.code]; the `WGPU_SHADER_MODULE_DESCRIPTOR_INIT`
 * + `WGPU_SHADER_SOURCE_WGSL_INIT` equivalent. The WGSL chain and its code string
 * live in [this] allocator's arena, which is consumed by the synchronous create call.
 */
internal fun MemoryAllocator.allocateShaderModuleDescriptor(descriptor: GPUShaderModuleDescriptor): WGPUShaderModuleDescriptor {
    val wgsl = WGPUShaderSourceWGSL.allocate(this)
    val chain = wgsl.chain
    chain.next = null
    chain.sType = WGPUSType_ShaderSourceWGSL
    wgsl.code.initFrom(descriptor.code, this)

    val native = WGPUShaderModuleDescriptor.allocate(this)
    native.nextInChain = chain
    native.label.initNull()
    return native
}
