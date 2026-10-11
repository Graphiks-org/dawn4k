package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.WGPUCommandBuffer
import org.graphiks.dawn4k.native.wgpuCommandBufferRelease
import org.graphiks.webgpu.GPUCommandBuffer

/**
 * A raw [GPUCommandBuffer] produced by [DawnCommandEncoder.finish]. Refcount-only:
 * [close] releases the reference immediately.
 */
class DawnCommandBuffer internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUCommandBuffer,
    label: String,
) : GPUCommandBuffer {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuCommandBufferRelease(handle) })
    }

    override fun close() {
        session.resources.release(this)
    }
}
