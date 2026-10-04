package org.graphiks.dawn4k.testing

import org.graphiks.dawn4k.DawnBindGroup
import org.graphiks.dawn4k.DawnBuffer
import org.graphiks.dawn4k.DawnComputePipeline
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnShaderModule
import org.graphiks.dawn4k.createBindGroup
import org.graphiks.dawn4k.createBuffer
import org.graphiks.dawn4k.createComputePipeline
import org.graphiks.dawn4k.createShaderModule
import org.graphiks.dawn4k.internal.DawnRuntime
import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.wgpuCommandBufferRelease
import org.graphiks.dawn4k.native.wgpuCommandEncoderBeginComputePass
import org.graphiks.dawn4k.native.wgpuCommandEncoderCopyBufferToBuffer
import org.graphiks.dawn4k.native.wgpuCommandEncoderFinish
import org.graphiks.dawn4k.native.wgpuCommandEncoderRelease
import org.graphiks.dawn4k.native.wgpuComputePassEncoderDispatchWorkgroups
import org.graphiks.dawn4k.native.wgpuComputePassEncoderEnd
import org.graphiks.dawn4k.native.wgpuComputePassEncoderRelease
import org.graphiks.dawn4k.native.wgpuComputePassEncoderSetBindGroup
import org.graphiks.dawn4k.native.wgpuComputePassEncoderSetPipeline
import org.graphiks.dawn4k.native.wgpuDeviceCreateCommandEncoder
import org.graphiks.dawn4k.native.wgpuQueueSubmit
import org.graphiks.dawn4k.onSubmittedWorkDone
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUBufferDescriptor
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUComputePipelineDescriptor
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferDescriptor
import kotlin.test.assertContentEquals

/**
 * One raw GPU session for the tests of this module: a [DawnRuntime] and one
 * already-opened [DeviceSession], closed together in reverse order. Later tasks
 * add their raw creation helpers here as they need them for their tests.
 */
internal class NativeFixture(
    internal val runtime: DawnRuntime,
    internal val session: DeviceSession,
) : AutoCloseable {

    companion object {
        suspend fun open(): NativeFixture {
            val runtime = DawnRuntime(DawnConfig())
            val session = runtime.openSession()
            return NativeFixture(runtime, session)
        }
    }

    /** Creates a raw [DawnBuffer] on the fixture's session. */
    fun createBuffer(descriptor: GPUBufferDescriptor): DawnBuffer =
        session.createBuffer(descriptor)

    /** Creates a raw [DawnShaderModule] on the fixture's session. */
    fun createShaderModule(descriptor: GPUShaderModuleDescriptor): DawnShaderModule =
        session.createShaderModule(descriptor)

    /** Creates a raw [DawnComputePipeline] on the fixture's session. */
    fun createComputePipeline(descriptor: GPUComputePipelineDescriptor): DawnComputePipeline =
        session.createComputePipeline(descriptor)

    /**
     * Raw buffer-to-buffer copy: encode on the device, finish, submit on the
     * session's queue, and wait for the submitted work to complete. This is the
     * pre-CommandEncoder-wrapper path (Task 6 replaces it with the public API).
     */
    suspend fun copyAndSubmit(source: DawnBuffer, destination: DawnBuffer, size: ULong) {
        session.runtime.dispatcher.awaitCall {
            memoryScope { allocator ->
                val encoder = wgpuDeviceCreateCommandEncoder(session.handle, null)
                    ?: throw IllegalStateException("wgpuDeviceCreateCommandEncoder returned no encoder")
                try {
                    wgpuCommandEncoderCopyBufferToBuffer(
                        encoder,
                        source.handle,
                        0uL,
                        destination.handle,
                        0uL,
                        size,
                    )
                    val commandBuffer = wgpuCommandEncoderFinish(encoder, null)
                        ?: throw IllegalStateException("wgpuCommandEncoderFinish returned no command buffer")
                    try {
                        val commands = allocator.bufferOfAddresses(listOf(commandBuffer.handler))
                        wgpuQueueSubmit(session.queueHandle, 1uL, commands.handler)
                    } finally {
                        wgpuCommandBufferRelease(commandBuffer)
                    }
                } finally {
                    wgpuCommandEncoderRelease(encoder)
                }
            }
        }
        session.onSubmittedWorkDone().getOrThrow()
    }

    /**
     * Raw compute dispatch: encode a compute pass that sets [pipeline] and
     * [bindGroup], dispatches [workgroupCountX] workgroups, copies [storage] into
     * [staging], submits, and waits for the work to complete. The caller maps and
     * reads [staging] afterwards.
     */
    suspend fun dispatchWorkgroups(
        pipeline: DawnComputePipeline,
        bindGroup: DawnBindGroup,
        storage: DawnBuffer,
        staging: DawnBuffer,
        workgroupCountX: UInt,
        dynamicOffsets: UIntArray = uintArrayOf(),
    ) {
        session.runtime.dispatcher.awaitCall {
            memoryScope { allocator ->
                val encoder = wgpuDeviceCreateCommandEncoder(session.handle, null)
                    ?: throw IllegalStateException("wgpuDeviceCreateCommandEncoder returned no encoder")
                try {
                    val pass = wgpuCommandEncoderBeginComputePass(encoder, null)
                        ?: throw IllegalStateException("wgpuCommandEncoderBeginComputePass returned no pass")
                    try {
                        wgpuComputePassEncoderSetPipeline(pass, pipeline.handle)
                        val dynamicOffsetsAddress: NativeAddress? = if (dynamicOffsets.isEmpty()) {
                            null
                        } else {
                            allocator.allocateBuffer((dynamicOffsets.size * 4).toULong()).apply {
                                writeUInts(dynamicOffsets)
                            }.handler
                        }
                        wgpuComputePassEncoderSetBindGroup(
                            pass, 0u, bindGroup.handle, dynamicOffsets.size.toULong(), dynamicOffsetsAddress,
                        )
                        wgpuComputePassEncoderDispatchWorkgroups(pass, workgroupCountX, 1u, 1u)
                        wgpuComputePassEncoderEnd(pass)
                    } finally {
                        wgpuComputePassEncoderRelease(pass)
                    }
                    wgpuCommandEncoderCopyBufferToBuffer(encoder, storage.handle, 0uL, staging.handle, 0uL, storage.size)
                    val commandBuffer = wgpuCommandEncoderFinish(encoder, null)
                        ?: throw IllegalStateException("wgpuCommandEncoderFinish returned no command buffer")
                    try {
                        val commands = allocator.bufferOfAddresses(listOf(commandBuffer.handler))
                        wgpuQueueSubmit(session.queueHandle, 1uL, commands.handler)
                    } finally {
                        wgpuCommandBufferRelease(commandBuffer)
                    }
                } finally {
                    wgpuCommandEncoderRelease(encoder)
                }
            }
        }
        session.onSubmittedWorkDone().getOrThrow()
    }

    /**
     * Standalone compute oracle: a 16-byte storage buffer bound at the pipeline's
     * own group 0 layout, 4 workgroups, copy to a 16-byte staging buffer, map and
     * compare against [expected]. The temporary bind-group layout and bind group
     * are released before returning.
     */
    suspend fun dispatchAndReadback(pipeline: DawnComputePipeline, expected: UIntArray) {
        val storage = createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc))
        val staging = createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
        try {
            pipeline.getBindGroupLayout(0u).use { layout ->
                session.createBindGroup(BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, storage)))).use { bindGroup ->
                    dispatchWorkgroups(pipeline, bindGroup, storage, staging, workgroupCountX = 4u)
                    staging.mapAsync(GPUMapMode.Read).getOrThrow()
                    assertContentEquals(expected, staging.getMappedRange().toUIntArray())
                    staging.unmap()
                }
            }
        } finally {
            storage.close()
            staging.close()
        }
    }

    override fun close() {
        session.close()
        runtime.close()
    }
}
