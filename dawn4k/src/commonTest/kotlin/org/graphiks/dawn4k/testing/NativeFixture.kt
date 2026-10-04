package org.graphiks.dawn4k.testing

import org.graphiks.dawn4k.DawnBuffer
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.createBuffer
import org.graphiks.dawn4k.internal.DawnRuntime
import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.wgpuCommandBufferRelease
import org.graphiks.dawn4k.native.wgpuCommandEncoderCopyBufferToBuffer
import org.graphiks.dawn4k.native.wgpuCommandEncoderFinish
import org.graphiks.dawn4k.native.wgpuCommandEncoderRelease
import org.graphiks.dawn4k.native.wgpuDeviceCreateCommandEncoder
import org.graphiks.dawn4k.native.wgpuQueueSubmit
import org.graphiks.dawn4k.onSubmittedWorkDone
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUBufferDescriptor

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

    override fun close() {
        session.close()
        runtime.close()
    }
}
