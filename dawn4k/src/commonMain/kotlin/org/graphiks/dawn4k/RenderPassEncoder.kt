package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.uploadAddress
import org.graphiks.dawn4k.mapper.dataSlice
import org.graphiks.dawn4k.mapper.toNativeIndexFormat
import org.graphiks.dawn4k.mapper.WGPU_WHOLE_SIZE
import org.graphiks.dawn4k.native.WGPUColor
import org.graphiks.dawn4k.native.WGPURenderPassEncoder
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderBeginOcclusionQuery
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderDraw
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderDrawIndexed
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderDrawIndexedIndirect
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderDrawIndirect
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderEnd
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderEndOcclusionQuery
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderExecuteBundles
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderInsertDebugMarker
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderPopDebugGroup
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderPushDebugGroup
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderRelease
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderSetBindGroup
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderSetBlendConstant
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderSetImmediates
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderSetIndexBuffer
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderSetPipeline
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderSetScissorRect
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderSetStencilReference
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderSetVertexBuffer
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderSetViewport
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUColor
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUIndex32
import org.graphiks.webgpu.GPUIndexFormat
import org.graphiks.webgpu.GPUIntegerCoordinate
import org.graphiks.webgpu.GPURenderBundle
import org.graphiks.webgpu.GPURenderPassEncoder
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPUSize32
import org.graphiks.webgpu.GPUSize64
import org.graphiks.webgpu.GPUSignedOffset32
import org.graphiks.webgpu.GPUStencilValue

/**
 * A raw [GPURenderPassEncoder] borrowed from its owning command encoder. It has
 * no [close]: the pass handle is released when [end] is called (together with the
 * temporary attachment views created for [GPUTexture] attachments), or by the
 * session teardown if the pass is never ended.
 */
class DawnRenderPassEncoder internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPURenderPassEncoder,
    private val temporaryViews: List<DawnTextureView>,
) : GPURenderPassEncoder {

    override var label: String = ""

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuRenderPassEncoderRelease(handle) })
    }

    override fun setPipeline(pipeline: GPURenderPipeline) {
        val dawn = pipeline.requireDawnRenderPipeline(session)
        session.runtime.dispatcher.call { wgpuRenderPassEncoderSetPipeline(handle, dawn.handle) }
    }

    override fun setIndexBuffer(buffer: GPUBuffer, indexFormat: GPUIndexFormat, offset: GPUSize64, size: GPUSize64?) {
        val dawn = buffer.requireDawnBuffer(session)
        session.runtime.dispatcher.call {
            wgpuRenderPassEncoderSetIndexBuffer(handle, dawn.handle, indexFormat.toNativeIndexFormat(), offset, size ?: WGPU_WHOLE_SIZE)
        }
    }

    override fun setVertexBuffer(slot: GPUIndex32, buffer: GPUBuffer?, offset: GPUSize64, size: GPUSize64?) {
        val dawn = buffer?.requireDawnBuffer(session)
        session.runtime.dispatcher.call {
            wgpuRenderPassEncoderSetVertexBuffer(handle, slot, dawn?.handle, offset, size ?: WGPU_WHOLE_SIZE)
        }
    }

    override fun draw(vertexCount: GPUSize32, instanceCount: GPUSize32, firstVertex: GPUSize32, firstInstance: GPUSize32) {
        session.runtime.dispatcher.call {
            wgpuRenderPassEncoderDraw(handle, vertexCount, instanceCount, firstVertex, firstInstance)
        }
    }

    override fun drawIndexed(
        indexCount: GPUSize32,
        instanceCount: GPUSize32,
        firstIndex: GPUSize32,
        baseVertex: GPUSignedOffset32,
        firstInstance: GPUSize32,
    ) {
        session.runtime.dispatcher.call {
            wgpuRenderPassEncoderDrawIndexed(handle, indexCount, instanceCount, firstIndex, baseVertex, firstInstance)
        }
    }

    override fun drawIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64) {
        val dawn = indirectBuffer.requireDawnBuffer(session)
        session.runtime.dispatcher.call { wgpuRenderPassEncoderDrawIndirect(handle, dawn.handle, indirectOffset) }
    }

    override fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64) {
        val dawn = indirectBuffer.requireDawnBuffer(session)
        session.runtime.dispatcher.call { wgpuRenderPassEncoderDrawIndexedIndirect(handle, dawn.handle, indirectOffset) }
    }

    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: List<UInt>) {
        val dawn = bindGroup?.requireDawnBindGroup(session)
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                wgpuRenderPassEncoderSetBindGroup(
                    handle,
                    index,
                    dawn?.handle,
                    dynamicOffsetsData.size.toULong(),
                    allocator.dynamicOffsetsBuffer(dynamicOffsetsData),
                )
            }
        }
    }

    override fun setImmediates(rangeOffset: GPUSize32, data: ArrayBuffer, dataOffset: GPUSize64, dataSize: GPUSize64?) {
        val slice = dataSlice(data.size, dataOffset, dataSize)
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                val address = uploadAddress(allocator, data, slice.offset, slice.size)
                wgpuRenderPassEncoderSetImmediates(handle, rangeOffset, address, slice.size)
            }
        }
    }

    override fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float) {
        session.runtime.dispatcher.call {
            wgpuRenderPassEncoderSetViewport(handle, x, y, width, height, minDepth, maxDepth)
        }
    }

    override fun setScissorRect(x: GPUIntegerCoordinate, y: GPUIntegerCoordinate, width: GPUIntegerCoordinate, height: GPUIntegerCoordinate) {
        session.runtime.dispatcher.call {
            wgpuRenderPassEncoderSetScissorRect(handle, x, y, width, height)
        }
    }

    override fun setBlendConstant(color: GPUColor) {
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                wgpuRenderPassEncoderSetBlendConstant(handle, allocator.allocateColor(color))
            }
        }
    }

    override fun setStencilReference(reference: GPUStencilValue) {
        session.runtime.dispatcher.call { wgpuRenderPassEncoderSetStencilReference(handle, reference) }
    }

    override fun beginOcclusionQuery(queryIndex: GPUSize32) {
        session.runtime.dispatcher.call { wgpuRenderPassEncoderBeginOcclusionQuery(handle, queryIndex) }
    }

    override fun endOcclusionQuery() {
        session.runtime.dispatcher.call { wgpuRenderPassEncoderEndOcclusionQuery(handle) }
    }

    override fun executeBundles(bundles: List<GPURenderBundle>) {
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                val handles = bundles.map { it.requireDawnRenderBundle(session).handle }
                val addresses = if (handles.isEmpty()) null else allocator.bufferOfAddresses(handles.map { it.handler }).handler
                wgpuRenderPassEncoderExecuteBundles(handle, handles.size.toULong(), addresses)
            }
        }
    }

    override fun pushDebugGroup(groupLabel: String) {
        session.runtime.dispatcher.call {
            memoryScope { allocator -> wgpuRenderPassEncoderPushDebugGroup(handle, allocator.allocateLabel(groupLabel)) }
        }
    }

    override fun popDebugGroup() {
        session.runtime.dispatcher.call { wgpuRenderPassEncoderPopDebugGroup(handle) }
    }

    override fun insertDebugMarker(markerLabel: String) {
        session.runtime.dispatcher.call {
            memoryScope { allocator -> wgpuRenderPassEncoderInsertDebugMarker(handle, allocator.allocateLabel(markerLabel)) }
        }
    }

    override fun end() {
        session.runtime.dispatcher.call { wgpuRenderPassEncoderEnd(handle) }
        session.resources.release(this)
        temporaryViews.forEach { it.close() }
    }
}

/** Refuses a foreign or foreign-session render bundle before its handle is read. */
internal fun GPURenderBundle.requireDawnRenderBundle(owner: DeviceSession): DawnRenderBundle {
    val dawn = this as? DawnRenderBundle
        ?: throw IllegalArgumentException("the render bundle does not belong to this Dawn backend: $this")
    require(dawn.session === owner) { "the render bundle belongs to a different device session" }
    return dawn
}

/** Allocates a [WGPUColor] from a [GPUColor] for the blend-constant downcall. */
internal fun MemoryAllocator.allocateColor(color: GPUColor): WGPUColor {
    val native = WGPUColor.allocate(this)
    native.r = color.r
    native.g = color.g
    native.b = color.b
    native.a = color.a
    return native
}
