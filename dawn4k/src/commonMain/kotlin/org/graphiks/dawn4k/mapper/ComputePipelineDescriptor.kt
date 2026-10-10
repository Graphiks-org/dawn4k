package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.WGPUComputePipelineDescriptor
import org.graphiks.dawn4k.native.WGPUComputeState
import org.graphiks.dawn4k.native.WGPUConstantEntry
import org.graphiks.dawn4k.requireDawnPipelineLayout
import org.graphiks.dawn4k.requireDawnShaderModule
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUComputePipelineDescriptor

/**
 * Allocates a [WGPUComputePipelineDescriptor] whose compute stage references the
 * shader module (same [session]) and the optional pipeline layout (same [session]
 * or null = auto layout). `WGPU_COMPUTE_PIPELINE_DESCRIPTOR_INIT` +
 * `WGPU_COMPUTE_STATE_INIT` equivalent.
 */
internal fun MemoryAllocator.allocateComputePipelineDescriptor(
    descriptor: GPUComputePipelineDescriptor,
    session: DeviceSession,
): WGPUComputePipelineDescriptor {
    val module = descriptor.compute.module.requireDawnShaderModule(session)
    val layout = descriptor.layout?.requireDawnPipelineLayout(session)

    val native = WGPUComputePipelineDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.layout = layout?.handle

    val compute = native.compute
    compute.nextInChain = null
    compute.module = module.handle
    initComputeState(compute, descriptor.compute.entryPoint, descriptor.compute.constants)
    return native
}

/**
 * Fills a compute stage's entry point (nullable) and pipeline-overridable
 * constants. The UTF-8 keys and the entry point are copied into [this] allocator's
 * arena, so they outlive the (synchronous) create call that consumes the
 * descriptor; a null entry point is the empty StringView.
 */
internal fun MemoryAllocator.initComputeState(
    compute: WGPUComputeState,
    entryPoint: String?,
    constants: Map<String, Double>,
) {
    compute.entryPoint.initNullable(entryPoint, this)
    val pairs = constants.entries.toList()
    compute.constantCount = pairs.size.toULong()
    compute.constants = if (pairs.isEmpty()) {
        null
    } else {
        WGPUConstantEntry.allocateArray(this, pairs.size.toUInt()) { index, entry ->
            entry.nextInChain = null
            entry.key.initFrom(pairs[index.toInt()].key, this)
            entry.value = pairs[index.toInt()].value
        }.let { WGPUConstantEntry(it.handler) }
    }
}
