package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.uploadAddress
import org.graphiks.dawn4k.mapper.allocateExtent3D
import org.graphiks.dawn4k.mapper.allocateTexelCopyBufferLayout
import org.graphiks.dawn4k.mapper.allocateTexelCopyTextureInfo
import org.graphiks.dawn4k.mapper.dataSlice
import org.graphiks.dawn4k.native.WGPUQueue
import org.graphiks.dawn4k.native.wgpuQueueSubmit
import org.graphiks.dawn4k.native.wgpuQueueWriteBuffer
import org.graphiks.dawn4k.native.wgpuQueueWriteTexture
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUCommandBuffer
import org.graphiks.webgpu.GPUExtent3D
import org.graphiks.webgpu.GPUQueue
import org.graphiks.webgpu.GPUSize64
import org.graphiks.webgpu.GPUTexelCopyBufferLayout
import org.graphiks.webgpu.GPUTexelCopyTextureInfo

/**
 * A raw [GPUQueue] backed by the session's Dawn `WGPUQueue`. The queue is an
 * owned reference of the session (there is no independent [GPUQueue] close), so
 * this wrapper only borrows the already-owned handle and never releases it.
 */
class DawnQueue internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUQueue,
) : GPUQueue {

    override var label: String = ""

    override fun submit(commandBuffers: List<GPUCommandBuffer>) {
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                val buffers = commandBuffers.map { it.requireDawnCommandBuffer(session).handle }
                val commands = if (buffers.isEmpty()) {
                    null
                } else {
                    allocator.bufferOfAddresses(buffers.map { it.handler }).handler
                }
                wgpuQueueSubmit(handle, buffers.size.toULong(), commands)
            }
        }
    }

    override suspend fun onSubmittedWorkDone(): Result<Unit> = session.onSubmittedWorkDone()

    override fun writeBuffer(
        buffer: GPUBuffer,
        bufferOffset: GPUSize64,
        data: ArrayBuffer,
        dataOffset: GPUSize64,
        size: GPUSize64?,
    ) {
        val slice = dataSlice(data.size, dataOffset, size)
        val dawn = buffer.requireDawnBuffer(session)
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                val address = uploadAddress(allocator, data, slice.offset, slice.size)
                wgpuQueueWriteBuffer(handle, dawn.handle, bufferOffset, address, slice.size)
            }
        }
    }

    override fun writeTexture(
        destination: GPUTexelCopyTextureInfo,
        data: ArrayBuffer,
        dataLayout: GPUTexelCopyBufferLayout,
        size: GPUExtent3D,
    ) {
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                val nativeDestination = allocator.allocateTexelCopyTextureInfo(destination, session)
                val nativeLayout = allocator.allocateTexelCopyBufferLayout(dataLayout)
                val nativeSize = allocator.allocateExtent3D(size)
                val address = uploadAddress(allocator, data, 0uL, data.size)
                wgpuQueueWriteTexture(handle, nativeDestination, address, data.size, nativeLayout, nativeSize)
            }
        }
    }
}

/** The session's owned queue, wrapped once and cached for the session's lifetime. */
internal val DeviceSession.queue: DawnQueue
    get() = DawnQueue(this, queueHandle)
