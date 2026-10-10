package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.checkedRange
import org.graphiks.dawn4k.mapper.allocateComputePassDescriptor
import org.graphiks.dawn4k.mapper.allocateExtent3D
import org.graphiks.dawn4k.mapper.allocateRenderPassDescriptor
import org.graphiks.dawn4k.mapper.allocateTexelCopyBufferInfo
import org.graphiks.dawn4k.mapper.allocateTexelCopyTextureInfo
import org.graphiks.dawn4k.mapper.initNull
import org.graphiks.dawn4k.native.WGPUCommandBufferDescriptor
import org.graphiks.dawn4k.native.WGPUCommandEncoder
import org.graphiks.dawn4k.native.WGPUCommandEncoderDescriptor
import org.graphiks.dawn4k.native.wgpuCommandEncoderBeginComputePass
import org.graphiks.dawn4k.native.wgpuCommandEncoderBeginRenderPass
import org.graphiks.dawn4k.native.wgpuCommandEncoderClearBuffer
import org.graphiks.dawn4k.native.wgpuCommandEncoderCopyBufferToBuffer
import org.graphiks.dawn4k.native.wgpuCommandEncoderCopyBufferToTexture
import org.graphiks.dawn4k.native.wgpuCommandEncoderCopyTextureToBuffer
import org.graphiks.dawn4k.native.wgpuCommandEncoderCopyTextureToTexture
import org.graphiks.dawn4k.native.wgpuCommandEncoderFinish
import org.graphiks.dawn4k.native.wgpuCommandEncoderInsertDebugMarker
import org.graphiks.dawn4k.native.wgpuCommandEncoderPopDebugGroup
import org.graphiks.dawn4k.native.wgpuCommandEncoderPushDebugGroup
import org.graphiks.dawn4k.native.wgpuCommandEncoderRelease
import org.graphiks.dawn4k.native.wgpuCommandEncoderResolveQuerySet
import org.graphiks.dawn4k.native.wgpuDeviceCreateCommandEncoder
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUCommandBuffer
import org.graphiks.webgpu.GPUCommandBufferDescriptor
import org.graphiks.webgpu.GPUCommandEncoder
import org.graphiks.webgpu.GPUCommandEncoderDescriptor
import org.graphiks.webgpu.GPUComputePassDescriptor
import org.graphiks.webgpu.GPUComputePassEncoder
import org.graphiks.webgpu.GPUExtent3D
import org.graphiks.webgpu.GPUQuerySet
import org.graphiks.webgpu.GPURenderPassDescriptor
import org.graphiks.webgpu.GPURenderPassEncoder
import org.graphiks.webgpu.GPUSize32
import org.graphiks.webgpu.GPUSize64
import org.graphiks.webgpu.GPUTexelCopyBufferInfo
import org.graphiks.webgpu.GPUTexelCopyTextureInfo

/**
 * One caller exclusively owns an encoder and its passes. Independent encoders
 * may run on different consumer threads; join users before close or submit.
 * A raw [GPUCommandEncoder] backed by a Dawn `WGPUCommandEncoder`. Refcount-only:
 * [close] releases the reference immediately. [finish] does NOT release the
 * handle — it returns the command buffer and lets Dawn own the "already finished"
 * state, so a second [finish] surfaces Dawn's validation error rather than a
 * Kotlin-side guard.
 */
class DawnCommandEncoder internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUCommandEncoder,
    label: String,
) : GPUCommandEncoder {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuCommandEncoderRelease(handle) })
    }

    override fun beginRenderPass(descriptor: GPURenderPassDescriptor): GPURenderPassEncoder {
        val temporaryViews = mutableListOf<DawnTextureView>()
        return run {
            memoryScope { allocator ->
                val native = allocator.allocateRenderPassDescriptor(descriptor, session, temporaryViews)
                val pass = wgpuCommandEncoderBeginRenderPass(handle, native)
                    ?: throw IllegalStateException("wgpuCommandEncoderBeginRenderPass returned no pass")
                DawnRenderPassEncoder(session, pass, temporaryViews)
            }
        }
    }

    override fun beginComputePass(descriptor: GPUComputePassDescriptor?): GPUComputePassEncoder =
        run {
            memoryScope { allocator ->
                val native = descriptor?.let { allocator.allocateComputePassDescriptor(it, session) }
                val pass = wgpuCommandEncoderBeginComputePass(handle, native)
                    ?: throw IllegalStateException("wgpuCommandEncoderBeginComputePass returned no pass")
                DawnComputePassEncoder(session, pass)
            }
        }

    override fun copyBufferToBuffer(
        source: GPUBuffer,
        sourceOffset: GPUSize64,
        destination: GPUBuffer,
        destinationOffset: GPUSize64,
        size: GPUSize64?,
    ) {
        val copySize = size ?: checkedRange(source.size, sourceOffset, null).size
        val dawnSource = source.requireDawnBuffer(session)
        val dawnDestination = destination.requireDawnBuffer(session)
        run {
            wgpuCommandEncoderCopyBufferToBuffer(
                handle,
                dawnSource.handle,
                sourceOffset,
                dawnDestination.handle,
                destinationOffset,
                copySize,
            )
        }
    }

    override fun copyBufferToTexture(
        source: GPUTexelCopyBufferInfo,
        destination: GPUTexelCopyTextureInfo,
        copySize: GPUExtent3D,
    ) {
        run {
            memoryScope { allocator ->
                wgpuCommandEncoderCopyBufferToTexture(
                    handle,
                    allocator.allocateTexelCopyBufferInfo(source, session),
                    allocator.allocateTexelCopyTextureInfo(destination, session),
                    allocator.allocateExtent3D(copySize),
                )
            }
        }
    }

    override fun copyTextureToBuffer(
        source: GPUTexelCopyTextureInfo,
        destination: GPUTexelCopyBufferInfo,
        copySize: GPUExtent3D,
    ) {
        run {
            memoryScope { allocator ->
                wgpuCommandEncoderCopyTextureToBuffer(
                    handle,
                    allocator.allocateTexelCopyTextureInfo(source, session),
                    allocator.allocateTexelCopyBufferInfo(destination, session),
                    allocator.allocateExtent3D(copySize),
                )
            }
        }
    }

    override fun copyTextureToTexture(
        source: GPUTexelCopyTextureInfo,
        destination: GPUTexelCopyTextureInfo,
        copySize: GPUExtent3D,
    ) {
        run {
            memoryScope { allocator ->
                wgpuCommandEncoderCopyTextureToTexture(
                    handle,
                    allocator.allocateTexelCopyTextureInfo(source, session),
                    allocator.allocateTexelCopyTextureInfo(destination, session),
                    allocator.allocateExtent3D(copySize),
                )
            }
        }
    }

    override fun clearBuffer(buffer: GPUBuffer, offset: GPUSize64, size: GPUSize64?) {
        val clearSize = size ?: checkedRange(buffer.size, offset, null).size
        val dawn = buffer.requireDawnBuffer(session)
        run {
            wgpuCommandEncoderClearBuffer(handle, dawn.handle, offset, clearSize)
        }
    }

    override fun resolveQuerySet(
        querySet: GPUQuerySet,
        firstQuery: GPUSize32,
        queryCount: GPUSize32,
        destination: GPUBuffer,
        destinationOffset: GPUSize64,
    ) {
        val dawnQuerySet = querySet.requireDawnQuerySet(session)
        val dawnDestination = destination.requireDawnBuffer(session)
        run {
            wgpuCommandEncoderResolveQuerySet(
                handle,
                dawnQuerySet.handle,
                firstQuery,
                queryCount,
                dawnDestination.handle,
                destinationOffset,
            )
        }
    }

    override fun finish(descriptor: GPUCommandBufferDescriptor?): GPUCommandBuffer {
        val buffer = run {
            memoryScope { allocator ->
                wgpuCommandEncoderFinish(handle, descriptor?.let { allocator.allocateCommandBufferDescriptor(it) })
            }
        } ?: throw IllegalStateException("wgpuCommandEncoderFinish returned no command buffer")
        return DawnCommandBuffer(session, buffer, descriptor?.label ?: "")
    }

    override fun pushDebugGroup(groupLabel: String) {
        run {
            memoryScope { allocator ->
                wgpuCommandEncoderPushDebugGroup(handle, allocator.allocateLabel(groupLabel))
            }
        }
    }

    override fun popDebugGroup() {
        wgpuCommandEncoderPopDebugGroup(handle)
    }

    override fun insertDebugMarker(markerLabel: String) {
        run {
            memoryScope { allocator ->
                wgpuCommandEncoderInsertDebugMarker(handle, allocator.allocateLabel(markerLabel))
            }
        }
    }

    override fun close() {
        session.resources.release(this)
    }
}

/** `WGPU_COMMAND_BUFFER_DESCRIPTOR_INIT` equivalent. */
private fun MemoryAllocator.allocateCommandBufferDescriptor(descriptor: GPUCommandBufferDescriptor): WGPUCommandBufferDescriptor {
    val native = WGPUCommandBufferDescriptor.allocate(this)
    native.nextInChain = null
    native.label.initNull()
    return native
}

/** Creates a [DawnCommandEncoder] on [this] session and registers its reference. */
internal fun DeviceSession.createCommandEncoder(descriptor: GPUCommandEncoderDescriptor? = null): DawnCommandEncoder =
    run {
        memoryScope { allocator ->
            val native = descriptor?.let {
                WGPUCommandEncoderDescriptor.allocate(allocator).also { descriptorNative ->
                    descriptorNative.nextInChain = null
                    descriptorNative.label.initNull()
                }
            }
            val handle = wgpuDeviceCreateCommandEncoder(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateCommandEncoder returned no encoder")
            DawnCommandEncoder(this, handle, descriptor?.label ?: "")
        }
    }
