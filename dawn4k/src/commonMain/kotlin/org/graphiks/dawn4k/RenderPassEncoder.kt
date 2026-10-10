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
 * The pass and its parent encoder require exclusive caller use, without scheduling.
 * A raw [GPURenderPassEncoder] borrowed from its owning command encoder. It has
 * no [close]: the pass handle is released when [end] is called (together with the
 * temporary attachment views created for [GPUTexture] attachments), or by the
 * session teardown if the pass is never ended.
 *
 * [end] releases the native handle, so any command after it would dereference a
 * freed handle. A Kotlin [ended] flag refuses both a second [end] and any later
 * command with an [IllegalStateException] before a downcall — unlike the command
 * encoder's [DawnCommandEncoder.finish], whose "finish twice" validation error is
 * deliberately left to Dawn (the encoder handle is only released by [close]).
 */
class DawnRenderPassEncoder internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPURenderPassEncoder,
    private val temporaryViews: List<DawnTextureView>,
) : GPURenderPassEncoder {

    override var label: String = ""

    /** Set once by [end]; a command after it would use a released native handle. */
    private var ended = false

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuRenderPassEncoderRelease(handle) })
    }

    private fun requireOpen() {
        check(!ended) { "the render pass encoder has already been ended" }
    }

    override fun setPipeline(pipeline: GPURenderPipeline) {
        requireOpen()
        val dawn = pipeline.requireDawnRenderPipeline(session)
        wgpuRenderPassEncoderSetPipeline(handle, dawn.handle)
    }

    override fun setIndexBuffer(buffer: GPUBuffer, indexFormat: GPUIndexFormat, offset: GPUSize64, size: GPUSize64?) {
        requireOpen()
        val dawn = buffer.requireDawnBuffer(session)
        run {
            wgpuRenderPassEncoderSetIndexBuffer(handle, dawn.handle, indexFormat.toNativeIndexFormat(), offset, size ?: WGPU_WHOLE_SIZE)
        }
    }

    override fun setVertexBuffer(slot: GPUIndex32, buffer: GPUBuffer?, offset: GPUSize64, size: GPUSize64?) {
        requireOpen()
        val dawn = buffer?.requireDawnBuffer(session)
        run {
            wgpuRenderPassEncoderSetVertexBuffer(handle, slot, dawn?.handle, offset, size ?: WGPU_WHOLE_SIZE)
        }
    }

    override fun draw(vertexCount: GPUSize32, instanceCount: GPUSize32, firstVertex: GPUSize32, firstInstance: GPUSize32) {
        requireOpen()
        run {
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
        requireOpen()
        run {
            wgpuRenderPassEncoderDrawIndexed(handle, indexCount, instanceCount, firstIndex, baseVertex, firstInstance)
        }
    }

    override fun drawIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64) {
        requireOpen()
        val dawn = indirectBuffer.requireDawnBuffer(session)
        wgpuRenderPassEncoderDrawIndirect(handle, dawn.handle, indirectOffset)
    }

    override fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64) {
        requireOpen()
        val dawn = indirectBuffer.requireDawnBuffer(session)
        wgpuRenderPassEncoderDrawIndexedIndirect(handle, dawn.handle, indirectOffset)
    }

    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: List<UInt>) {
        requireOpen()
        val dawn = bindGroup?.requireDawnBindGroup(session)
        run {
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
        requireOpen()
        val slice = dataSlice(data.size, dataOffset, dataSize)
        run {
            memoryScope { allocator ->
                val address = uploadAddress(allocator, data, slice.offset, slice.size)
                wgpuRenderPassEncoderSetImmediates(handle, rangeOffset, address, slice.size)
            }
        }
    }

    override fun setViewport(x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float) {
        requireOpen()
        run {
            wgpuRenderPassEncoderSetViewport(handle, x, y, width, height, minDepth, maxDepth)
        }
    }

    override fun setScissorRect(x: GPUIntegerCoordinate, y: GPUIntegerCoordinate, width: GPUIntegerCoordinate, height: GPUIntegerCoordinate) {
        requireOpen()
        run {
            wgpuRenderPassEncoderSetScissorRect(handle, x, y, width, height)
        }
    }

    override fun setBlendConstant(color: GPUColor) {
        requireOpen()
        run {
            memoryScope { allocator ->
                wgpuRenderPassEncoderSetBlendConstant(handle, allocator.allocateColor(color))
            }
        }
    }

    override fun setStencilReference(reference: GPUStencilValue) {
        requireOpen()
        wgpuRenderPassEncoderSetStencilReference(handle, reference)
    }

    override fun beginOcclusionQuery(queryIndex: GPUSize32) {
        requireOpen()
        wgpuRenderPassEncoderBeginOcclusionQuery(handle, queryIndex)
    }

    override fun endOcclusionQuery() {
        requireOpen()
        wgpuRenderPassEncoderEndOcclusionQuery(handle)
    }

    override fun executeBundles(bundles: List<GPURenderBundle>) {
        requireOpen()
        run {
            memoryScope { allocator ->
                val handles = bundles.map { it.requireDawnRenderBundle(session).handle }
                val addresses = if (handles.isEmpty()) null else allocator.bufferOfAddresses(handles.map { it.handler }).handler
                wgpuRenderPassEncoderExecuteBundles(handle, handles.size.toULong(), addresses)
            }
        }
    }

    override fun pushDebugGroup(groupLabel: String) {
        requireOpen()
        run {
            memoryScope { allocator -> wgpuRenderPassEncoderPushDebugGroup(handle, allocator.allocateLabel(groupLabel)) }
        }
    }

    override fun popDebugGroup() {
        requireOpen()
        wgpuRenderPassEncoderPopDebugGroup(handle)
    }

    override fun insertDebugMarker(markerLabel: String) {
        requireOpen()
        run {
            memoryScope { allocator -> wgpuRenderPassEncoderInsertDebugMarker(handle, allocator.allocateLabel(markerLabel)) }
        }
    }

    override fun end() {
        requireOpen()
        ended = true
        wgpuRenderPassEncoderEnd(handle)
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
