package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.DawnTextureView
import org.graphiks.dawn4k.native.WGPUColor
import org.graphiks.dawn4k.native.WGPULoadOp_Undefined
import org.graphiks.dawn4k.native.WGPUStoreOp_Undefined
import org.graphiks.dawn4k.native.WGPUPassTimestampWrites
import org.graphiks.dawn4k.native.WGPURenderPassColorAttachment
import org.graphiks.dawn4k.native.WGPURenderPassDepthStencilAttachment
import org.graphiks.dawn4k.native.WGPURenderPassDescriptor
import org.graphiks.dawn4k.native.WGPURenderPassMaxDrawCount
import org.graphiks.dawn4k.native.WGPUTextureView
import org.graphiks.dawn4k.native.WGPUSType_RenderPassMaxDrawCount
import org.graphiks.dawn4k.DawnQuerySet
import org.graphiks.dawn4k.DawnTexture
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUColor
import org.graphiks.webgpu.GPURenderPassColorAttachment
import org.graphiks.webgpu.GPURenderPassDepthStencilAttachment
import org.graphiks.webgpu.GPURenderPassDescriptor
import org.graphiks.webgpu.GPURenderPassTimestampWrites
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureOrGPUTextureView
import org.graphiks.webgpu.GPUTextureView

/** The WebGPU default maxDrawCount (the value for which no chain is requested). */
private const val DEFAULT_MAX_DRAW_COUNT: ULong = 50000000uL

/**
 * Allocates a [WGPURenderPassDescriptor]; Dawn validates device ownership.
 * A [GPUTexture] attachment (not a [GPUTextureView]) gets an owned
 * temporary view created here and appended to [temporaryViews]; the pass encoder
 * releases those views when the pass ends, so they never outlive the encoding.
 *
 * `WGPU_RENDER_PASS_DESCRIPTOR_INIT` + `WGPU_RENDER_PASS_COLOR_ATTACHMENT_INIT` +
 * `WGPU_RENDER_PASS_DEPTH_STENCIL_ATTACHMENT_INIT` equivalents. The
 * [GPURenderPassDescriptor.maxDrawCount] is chained as a
 * [WGPURenderPassMaxDrawCount] only when it differs from the default, and an
 * absent timestamp write index maps to the `WGPU_QUERY_SET_INDEX_UNDEFINED`
 * sentinel.
 */
internal fun MemoryAllocator.allocateRenderPassDescriptor(
    descriptor: GPURenderPassDescriptor,
    temporaryViews: MutableList<DawnTextureView>,
): WGPURenderPassDescriptor {
    val native = WGPURenderPassDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()

    native.colorAttachmentCount = descriptor.colorAttachments.size.toULong()
    native.colorAttachments = if (descriptor.colorAttachments.isEmpty()) {
        null
    } else {
        WGPURenderPassColorAttachment.allocateArray(this, descriptor.colorAttachments.size.toUInt()) { index, target ->
            initColorAttachment(target, descriptor.colorAttachments[index.toInt()], temporaryViews)
        }.let { WGPURenderPassColorAttachment(it.handler) }
    }

    native.depthStencilAttachment = descriptor.depthStencilAttachment?.let {
        allocateDepthStencilAttachment(it, temporaryViews)
    }

    native.occlusionQuerySet = (descriptor.occlusionQuerySet as DawnQuerySet?)?.handle

    native.timestampWrites = descriptor.timestampWrites?.let { allocateTimestampWrites(it) }

    if (descriptor.maxDrawCount != DEFAULT_MAX_DRAW_COUNT) {
        val maxDrawCount = WGPURenderPassMaxDrawCount.allocate(this)
        maxDrawCount.chain.next = null
        maxDrawCount.chain.sType = WGPUSType_RenderPassMaxDrawCount
        maxDrawCount.maxDrawCount = descriptor.maxDrawCount
        native.nextInChain = maxDrawCount.chain
    }
    return native
}

/** `WGPU_RENDER_PASS_COLOR_ATTACHMENT_INIT` equivalent, resolving attachment views. */
private fun initColorAttachment(
    target: WGPURenderPassColorAttachment,
    attachment: GPURenderPassColorAttachment?,
    temporaryViews: MutableList<DawnTextureView>,
) {
    target.nextInChain = null
    target.view = attachment?.let { resolveAttachmentView(it.view, temporaryViews) }
    target.depthSlice = attachment?.depthSlice ?: WGPU_DEPTH_SLICE_UNDEFINED
    target.resolveTarget = attachment?.resolveTarget?.let { resolveAttachmentView(it, temporaryViews) }
    target.loadOp = attachment?.loadOp?.toNativeLoadOp() ?: WGPULoadOp_Undefined
    target.storeOp = attachment?.storeOp?.toNativeStoreOp() ?: WGPUStoreOp_Undefined
    target.clearValue.initFrom(attachment?.clearValue)
}

/** `WGPU_RENDER_PASS_DEPTH_STENCIL_ATTACHMENT_INIT` equivalent. */
private fun MemoryAllocator.allocateDepthStencilAttachment(
    attachment: GPURenderPassDepthStencilAttachment,
    temporaryViews: MutableList<DawnTextureView>,
): WGPURenderPassDepthStencilAttachment {
    val native = WGPURenderPassDepthStencilAttachment.allocate(this)
    native.nextInChain = null
    native.view = resolveAttachmentView(attachment.view, temporaryViews)
    native.depthLoadOp = attachment.depthLoadOp.toNativeOrUndefined
    native.depthStoreOp = attachment.depthStoreOp.toNativeOrUndefined
    native.depthClearValue = attachment.depthClearValue ?: Float.NaN
    native.depthReadOnly = if (attachment.depthReadOnly) 1u else 0u
    native.stencilLoadOp = attachment.stencilLoadOp.toNativeOrUndefined
    native.stencilStoreOp = attachment.stencilStoreOp.toNativeOrUndefined
    native.stencilClearValue = attachment.stencilClearValue
    native.stencilReadOnly = if (attachment.stencilReadOnly) 1u else 0u
    return native
}

/** `WGPU_PASS_TIMESTAMP_WRITES_INIT` equivalent. */
private fun MemoryAllocator.allocateTimestampWrites(
    writes: GPURenderPassTimestampWrites,
): WGPUPassTimestampWrites {
    val native = WGPUPassTimestampWrites.allocate(this)
    native.nextInChain = null
    native.querySet = (writes.querySet as DawnQuerySet).handle
    native.beginningOfPassWriteIndex = writes.beginningOfPassWriteIndex ?: WGPU_QUERY_SET_INDEX_UNDEFINED
    native.endOfPassWriteIndex = writes.endOfPassWriteIndex ?: WGPU_QUERY_SET_INDEX_UNDEFINED
    return native
}

/**
 * Resolves an attachment view to its native handle: a [GPUTextureView] is used
 * directly, while a [GPUTexture] gets an owned temporary view. Construction
 * releases acquired views on failure; a successful pass owns them until end/close.
 */
private fun resolveAttachmentView(
    view: GPUTextureOrGPUTextureView,
    temporaryViews: MutableList<DawnTextureView>,
): WGPUTextureView = when (view) {
    is GPUTexture -> {
        val temp = (view as DawnTexture).createView(null) as DawnTextureView
        temporaryViews += temp
        temp.handle
    }
    is GPUTextureView -> (view as DawnTextureView).handle
}

/** The `WGPU_COLOR_INIT` clear value (transparent black) when [color] is absent. */
private fun WGPUColor.initFrom(color: GPUColor?) {
    r = color?.r ?: 0.0
    g = color?.g ?: 0.0
    b = color?.b ?: 0.0
    a = color?.a ?: 0.0
}
