package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.WGPUBlendState
import org.graphiks.dawn4k.native.WGPUColorTargetState
import org.graphiks.dawn4k.native.WGPUCompareFunction_Undefined
import org.graphiks.dawn4k.native.WGPUConstantEntry
import org.graphiks.dawn4k.native.WGPUDepthStencilState
import org.graphiks.dawn4k.native.WGPUFragmentState
import org.graphiks.dawn4k.native.WGPURenderPipelineDescriptor
import org.graphiks.dawn4k.native.WGPUVertexAttribute
import org.graphiks.dawn4k.native.WGPUVertexBufferLayout
import org.graphiks.dawn4k.native.WGPUVertexState
import org.graphiks.dawn4k.native.WGPUStringView
import org.graphiks.dawn4k.requireDawnPipelineLayout
import org.graphiks.dawn4k.requireDawnShaderModule
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUBlendComponent
import org.graphiks.webgpu.GPUColorTargetState
import org.graphiks.webgpu.GPUDepthStencilState
import org.graphiks.webgpu.GPUFragmentState
import org.graphiks.webgpu.GPUProgrammableStage
import org.graphiks.webgpu.GPURenderPipelineDescriptor
import org.graphiks.webgpu.GPUVertexBufferLayout
import org.graphiks.webgpu.GPUVertexState

/**
 * Allocates a [WGPURenderPipelineDescriptor] whose vertex and fragment stages
 * reference their shader modules (same [session]) and the optional pipeline
 * layout (same [session] or null = auto layout). `WGPU_RENDER_PIPELINE_DESCRIPTOR_INIT`
 * + `WGPU_VERTEX_STATE_INIT` + `WGPU_FRAGMENT_STATE_INIT` + `WGPU_PRIMITIVE_STATE_INIT`
 * + `WGPU_MULTISAMPLE_STATE_INIT` equivalent.
 */
internal fun MemoryAllocator.allocateRenderPipelineDescriptor(
    descriptor: GPURenderPipelineDescriptor,
    session: DeviceSession,
): WGPURenderPipelineDescriptor {
    val layout = descriptor.layout?.requireDawnPipelineLayout(session)

    val native = WGPURenderPipelineDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    native.layout = layout?.handle

    initVertexState(native.vertex, descriptor.vertex, session)

    val primitive = native.primitive
    primitive.nextInChain = null
    primitive.topology = descriptor.primitive.topology.toNativePrimitiveTopology()
    primitive.stripIndexFormat = descriptor.primitive.stripIndexFormat.toNativeOrUndefined
    primitive.frontFace = descriptor.primitive.frontFace.toNativeFrontFace()
    primitive.cullMode = descriptor.primitive.cullMode.toNativeCullMode()
    primitive.unclippedDepth = if (descriptor.primitive.unclippedDepth) 1u else 0u

    native.depthStencil = descriptor.depthStencil?.let { allocateDepthStencilState(it) }

    val multisample = native.multisample
    multisample.nextInChain = null
    multisample.count = descriptor.multisample.count
    multisample.mask = descriptor.multisample.mask
    multisample.alphaToCoverageEnabled = if (descriptor.multisample.alphaToCoverageEnabled) 1u else 0u

    native.fragment = descriptor.fragment?.let { allocateFragmentState(it, session) }
    return native
}

/** Fills a vertex stage: module, entry point, constants, and the vertex-buffer layouts. */
private fun MemoryAllocator.initVertexState(
    state: WGPUVertexState,
    stage: GPUVertexState,
    session: DeviceSession,
) {
    val module = stage.module.requireDawnShaderModule(session)
    state.nextInChain = null
    state.module = module.handle
    initProgrammableStageFields(
        entryPoint = state.entryPoint,
        constantCount = { state.constantCount = it },
        constants = { state.constants = it },
        stage = stage,
    )
    state.bufferCount = stage.buffers.size.toULong()
    state.buffers = if (stage.buffers.isEmpty()) {
        null
    } else {
        WGPUVertexBufferLayout.allocateArray(this, stage.buffers.size.toUInt()) { index, buffer ->
            initVertexBufferLayout(buffer, stage.buffers[index.toInt()])
        }.let { WGPUVertexBufferLayout(it.handler) }
    }
}

/** Allocates a fragment stage: module, entry point, constants, and the color targets. */
private fun MemoryAllocator.allocateFragmentState(
    stage: GPUFragmentState,
    session: DeviceSession,
): WGPUFragmentState {
    val module = stage.module.requireDawnShaderModule(session)
    val native = WGPUFragmentState.allocate(this)
    native.nextInChain = null
    native.module = module.handle
    initProgrammableStageFields(
        entryPoint = native.entryPoint,
        constantCount = { native.constantCount = it },
        constants = { native.constants = it },
        stage = stage,
    )
    native.targetCount = stage.targets.size.toULong()
    native.targets = if (stage.targets.isEmpty()) {
        null
    } else {
        WGPUColorTargetState.allocateArray(this, stage.targets.size.toUInt()) { index, target ->
            initColorTargetState(target, stage.targets[index.toInt()])
        }.let { WGPUColorTargetState(it.handler) }
    }
    return native
}

/**
 * Fills the entry point and pipeline-overridable constants shared by the vertex
 * and fragment stages (both have the same three fields).
 */
private fun MemoryAllocator.initProgrammableStageFields(
    entryPoint: WGPUStringView,
    constantCount: (ULong) -> Unit,
    constants: (WGPUConstantEntry?) -> Unit,
    stage: GPUProgrammableStage,
) {
    entryPoint.initNullable(stage.entryPoint, this)
    val pairs = stage.constants.entries.toList()
    constantCount(pairs.size.toULong())
    constants(
        if (pairs.isEmpty()) {
            null
        } else {
            WGPUConstantEntry.allocateArray(this, pairs.size.toUInt()) { index, entry ->
                entry.nextInChain = null
                entry.key.initFrom(pairs[index.toInt()].key, this)
                entry.value = pairs[index.toInt()].value
            }.let { WGPUConstantEntry(it.handler) }
        },
    )
}

private fun MemoryAllocator.allocateDepthStencilState(
    state: GPUDepthStencilState,
): WGPUDepthStencilState {
    val native = WGPUDepthStencilState.allocate(this)
    native.nextInChain = null
    native.format = state.format.toNativeTextureFormat()
    native.depthWriteEnabled = state.depthWriteEnabled.toNativeOptionalBool()
    native.depthCompare = state.depthCompare?.toNativeCompareFunction() ?: WGPUCompareFunction_Undefined
    initStencilFaceState(native.stencilFront, state.stencilFront)
    initStencilFaceState(native.stencilBack, state.stencilBack)
    native.stencilReadMask = state.stencilReadMask
    native.stencilWriteMask = state.stencilWriteMask
    native.depthBias = state.depthBias
    native.depthBiasSlopeScale = state.depthBiasSlopeScale
    native.depthBiasClamp = state.depthBiasClamp
    return native
}

private fun initStencilFaceState(
    face: org.graphiks.dawn4k.native.WGPUStencilFaceState,
    state: org.graphiks.webgpu.GPUStencilFaceState,
) {
    face.compare = state.compare.toNativeCompareFunction()
    face.failOp = state.failOp.toNativeStencilOperation()
    face.depthFailOp = state.depthFailOp.toNativeStencilOperation()
    face.passOp = state.passOp.toNativeStencilOperation()
}

private fun MemoryAllocator.initVertexBufferLayout(buffer: WGPUVertexBufferLayout, layout: GPUVertexBufferLayout) {
    buffer.nextInChain = null
    buffer.stepMode = layout.stepMode.toNativeVertexStepMode()
    buffer.arrayStride = layout.arrayStride
    buffer.attributeCount = layout.attributes.size.toULong()
    buffer.attributes = if (layout.attributes.isEmpty()) {
        null
    } else {
        WGPUVertexAttribute.allocateArray(this, layout.attributes.size.toUInt()) { index, attribute ->
            val source = layout.attributes[index.toInt()]
            attribute.nextInChain = null
            attribute.format = source.format.toNativeVertexFormat()
            attribute.offset = source.offset
            attribute.shaderLocation = source.shaderLocation
        }.let { WGPUVertexAttribute(it.handler) }
    }
}

private fun MemoryAllocator.initColorTargetState(target: WGPUColorTargetState, state: GPUColorTargetState) {
    target.nextInChain = null
    target.format = state.format.toNativeTextureFormat()
    target.blend = state.blend?.let { allocateBlendState(it) }
    target.writeMask = state.writeMask.toNativeColorWriteMask()
}

private fun MemoryAllocator.allocateBlendState(
    state: org.graphiks.webgpu.GPUBlendState,
): WGPUBlendState {
    val native = WGPUBlendState.allocate(this)
    initBlendComponent(native.color, state.color)
    initBlendComponent(native.alpha, state.alpha)
    return native
}

private fun initBlendComponent(
    component: org.graphiks.dawn4k.native.WGPUBlendComponent,
    state: GPUBlendComponent,
) {
    component.operation = state.operation.toNativeBlendOperation()
    component.srcFactor = state.srcFactor.toNativeBlendFactor()
    component.dstFactor = state.dstFactor.toNativeBlendFactor()
}
