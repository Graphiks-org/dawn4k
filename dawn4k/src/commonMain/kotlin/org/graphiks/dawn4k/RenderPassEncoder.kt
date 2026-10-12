package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
import org.graphiks.dawn4k.internal.uploadAddress
import org.graphiks.dawn4k.mapper.dataSlice
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
 * A caller-owned [GPURenderPassEncoder]. [end] releases its native reference
 * and temporary attachment views. [close] releases an abandoned pass and its
 * temporary views without implicitly ending it.
 *
 * [end] releases the native handle, so any command after it would dereference a
 * freed handle. A local ownership guard refuses both a second [end] and any later
 * command with an [IllegalStateException] before a downcall — unlike the command
 * encoder's [DawnCommandEncoder.finish], whose "finish twice" validation error is
 * deliberately left to Dawn (the encoder handle is only released by [close]).
 */
class DawnRenderPassEncoder internal constructor(
    internal val session: DeviceSession,
    handle: WGPURenderPassEncoder,
    private val temporaryViews: List<DawnTextureView>,
    label: String,
) : GPURenderPassEncoder, AutoCloseable {

    override var label: String = label

    private val reference = OwnedReference(handle, release = { wgpuRenderPassEncoderRelease(it) })
    internal val handle: WGPURenderPassEncoder
        get() { session.requireOpen(); return reference.handle }

    private fun requireOpen() {
        handle
    }

    override fun setPipeline(pipeline: GPURenderPipeline) {
        requireOpen()
        val dawn = pipeline as DawnRenderPipeline
        wgpuRenderPassEncoderSetPipeline(handle, dawn.handle)
    }

    override fun setIndexBuffer(buffer: GPUBuffer, indexFormat: GPUIndexFormat, offset: GPUSize64, size: GPUSize64?) {
        requireOpen()
        val dawn = buffer as DawnBuffer
        run {
            wgpuRenderPassEncoderSetIndexBuffer(handle, dawn.handle, indexFormat.value, offset, size ?: WGPU_WHOLE_SIZE)
        }
    }

    override fun setVertexBuffer(slot: GPUIndex32, buffer: GPUBuffer?, offset: GPUSize64, size: GPUSize64?) {
        requireOpen()
        val dawn = buffer as DawnBuffer?
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
        val dawn = indirectBuffer as DawnBuffer
        wgpuRenderPassEncoderDrawIndirect(handle, dawn.handle, indirectOffset)
    }

    override fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64) {
        requireOpen()
        val dawn = indirectBuffer as DawnBuffer
        wgpuRenderPassEncoderDrawIndexedIndirect(handle, dawn.handle, indirectOffset)
    }

    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: List<UInt>) {
        requireOpen()
        val dawn = bindGroup as DawnBindGroup?
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
                val handles = bundles.map { (it as DawnRenderBundle).handle }
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
        try { wgpuRenderPassEncoderEnd(handle) } finally { close() }
    }

    /** Releases an abandoned pass and its attachment views, without ending it. */
    override fun close() {
        var failure: Throwable? = null
        fun attempt(action: () -> Unit) {
            try { action() } catch (caught: Throwable) {
                val first = failure
                if (first == null) failure = caught else first.addSuppressed(caught)
            }
        }
        attempt { reference.close() }
        temporaryViews.forEach { view -> attempt { view.close() } }
        failure?.let { throw it }
    }
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
