package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
import org.graphiks.dawn4k.native.WGPUCommandBuffer
import org.graphiks.dawn4k.native.wgpuCommandBufferRelease
import org.graphiks.webgpu.GPUCommandBuffer

/**
 * A raw [GPUCommandBuffer] produced by [DawnCommandEncoder.finish]. Refcount-only:
 * [close] releases the reference immediately.
 */
class DawnCommandBuffer internal constructor(
    internal val session: DeviceSession,
    handle: WGPUCommandBuffer,
    label: String,
) : GPUCommandBuffer {

    override var label: String = label

    private val reference = OwnedReference(handle, release = { wgpuCommandBufferRelease(it) })
    internal val handle: WGPUCommandBuffer
        get() { session.requireOpen(); return reference.handle }

    override fun close() {
        reference.close()
    }
}
