package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.uploadAddress
import org.graphiks.dawn4k.mapper.allocateRenderBundleDescriptor
import org.graphiks.dawn4k.mapper.allocateRenderBundleEncoderDescriptor
import org.graphiks.dawn4k.mapper.dataSlice
import org.graphiks.dawn4k.mapper.toNativeIndexFormat
import org.graphiks.dawn4k.mapper.WGPU_WHOLE_SIZE
import org.graphiks.dawn4k.native.WGPURenderBundleEncoder
import org.graphiks.dawn4k.native.wgpuDeviceCreateRenderBundleEncoder
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderDraw
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderDrawIndexed
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderDrawIndexedIndirect
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderDrawIndirect
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderFinish
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderInsertDebugMarker
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderPopDebugGroup
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderPushDebugGroup
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderRelease
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderSetBindGroup
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderSetImmediates
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderSetIndexBuffer
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderSetPipeline
import org.graphiks.dawn4k.native.wgpuRenderBundleEncoderSetVertexBuffer
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUIndex32
import org.graphiks.webgpu.GPUIndexFormat
import org.graphiks.webgpu.GPURenderBundle
import org.graphiks.webgpu.GPURenderBundleDescriptor
import org.graphiks.webgpu.GPURenderBundleEncoder
import org.graphiks.webgpu.GPURenderBundleEncoderDescriptor
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPUSize32
import org.graphiks.webgpu.GPUSize64
import org.graphiks.webgpu.GPUSignedOffset32

/**
 * A raw [GPURenderBundleEncoder] backed by a Dawn `WGPURenderBundleEncoder`.
 * Refcount-only: [close] releases the reference immediately, so any command or
 * [finish] after it would dereference a freed handle. A Kotlin [closed] flag
 * refuses them with an [IllegalStateException] before a downcall — mirroring
 * the pass encoders. [finish] itself does NOT release the handle (only [close]
 * does), so finishing twice WITHOUT closing stays valid and a second [finish]
 * surfaces Dawn's validation error, never a Kotlin guard.
 */
class DawnRenderBundleEncoder internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPURenderBundleEncoder,
    label: String,
) : GPURenderBundleEncoder {

    override var label: String = label

    /** Set once by [close]; a command after it would use a released native handle. */
    private var closed = false

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuRenderBundleEncoderRelease(handle) })
    }

    private fun requireOpen() {
        check(!closed) { "the render bundle encoder has already been closed" }
    }

    override fun setPipeline(pipeline: GPURenderPipeline) {
        requireOpen()
        val dawn = pipeline.requireDawnRenderPipeline(session)
        session.runtime.dispatcher.call { wgpuRenderBundleEncoderSetPipeline(handle, dawn.handle) }
    }

    override fun setIndexBuffer(buffer: GPUBuffer, indexFormat: GPUIndexFormat, offset: GPUSize64, size: GPUSize64?) {
        requireOpen()
        val dawn = buffer.requireDawnBuffer(session)
        session.runtime.dispatcher.call {
            wgpuRenderBundleEncoderSetIndexBuffer(handle, dawn.handle, indexFormat.toNativeIndexFormat(), offset, size ?: WGPU_WHOLE_SIZE)
        }
    }

    override fun setVertexBuffer(slot: GPUIndex32, buffer: GPUBuffer?, offset: GPUSize64, size: GPUSize64?) {
        requireOpen()
        val dawn = buffer?.requireDawnBuffer(session)
        session.runtime.dispatcher.call {
            wgpuRenderBundleEncoderSetVertexBuffer(handle, slot, dawn?.handle, offset, size ?: WGPU_WHOLE_SIZE)
        }
    }

    override fun draw(vertexCount: GPUSize32, instanceCount: GPUSize32, firstVertex: GPUSize32, firstInstance: GPUSize32) {
        requireOpen()
        session.runtime.dispatcher.call {
            wgpuRenderBundleEncoderDraw(handle, vertexCount, instanceCount, firstVertex, firstInstance)
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
        session.runtime.dispatcher.call {
            wgpuRenderBundleEncoderDrawIndexed(handle, indexCount, instanceCount, firstIndex, baseVertex, firstInstance)
        }
    }

    override fun drawIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64) {
        requireOpen()
        val dawn = indirectBuffer.requireDawnBuffer(session)
        session.runtime.dispatcher.call { wgpuRenderBundleEncoderDrawIndirect(handle, dawn.handle, indirectOffset) }
    }

    override fun drawIndexedIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64) {
        requireOpen()
        val dawn = indirectBuffer.requireDawnBuffer(session)
        session.runtime.dispatcher.call { wgpuRenderBundleEncoderDrawIndexedIndirect(handle, dawn.handle, indirectOffset) }
    }

    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: List<UInt>) {
        requireOpen()
        val dawn = bindGroup?.requireDawnBindGroup(session)
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                wgpuRenderBundleEncoderSetBindGroup(
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
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                val address = uploadAddress(allocator, data, slice.offset, slice.size)
                wgpuRenderBundleEncoderSetImmediates(handle, rangeOffset, address, slice.size)
            }
        }
    }

    override fun pushDebugGroup(groupLabel: String) {
        requireOpen()
        session.runtime.dispatcher.call {
            memoryScope { allocator -> wgpuRenderBundleEncoderPushDebugGroup(handle, allocator.allocateLabel(groupLabel)) }
        }
    }

    override fun popDebugGroup() {
        requireOpen()
        session.runtime.dispatcher.call { wgpuRenderBundleEncoderPopDebugGroup(handle) }
    }

    override fun insertDebugMarker(markerLabel: String) {
        requireOpen()
        session.runtime.dispatcher.call {
            memoryScope { allocator -> wgpuRenderBundleEncoderInsertDebugMarker(handle, allocator.allocateLabel(markerLabel)) }
        }
    }

    override fun finish(descriptor: GPURenderBundleDescriptor?): GPURenderBundle {
        requireOpen()
        val bundle = session.runtime.dispatcher.call {
            memoryScope { allocator ->
                wgpuRenderBundleEncoderFinish(handle, descriptor?.let { allocator.allocateRenderBundleDescriptor(it) })
            }
        } ?: throw IllegalStateException("wgpuRenderBundleEncoderFinish returned no bundle")
        return DawnRenderBundle(session, bundle, descriptor?.label ?: "")
    }

    override fun close() {
        if (closed) return
        closed = true
        session.resources.release(this)
    }
}

/** Creates a [DawnRenderBundleEncoder] on [this] session and registers its reference. */
internal fun DeviceSession.createRenderBundleEncoder(descriptor: GPURenderBundleEncoderDescriptor): DawnRenderBundleEncoder =
    runtime.dispatcher.call {
        memoryScope { allocator ->
            val native = allocator.allocateRenderBundleEncoderDescriptor(descriptor)
            val handle = wgpuDeviceCreateRenderBundleEncoder(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateRenderBundleEncoder returned no encoder")
            DawnRenderBundleEncoder(this, handle, descriptor.label)
        }
    }
