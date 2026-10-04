package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.mapper.allocateBindGroupDescriptor
import org.graphiks.dawn4k.native.WGPUBindGroup
import org.graphiks.dawn4k.native.wgpuBindGroupRelease
import org.graphiks.dawn4k.native.wgpuDeviceCreateBindGroup
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBindGroupDescriptor

/**
 * A raw [GPUBindGroup] backed by a Dawn `WGPUBindGroup`. Refcount-only: [close]
 * releases the reference immediately.
 */
class DawnBindGroup internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUBindGroup,
    label: String,
) : GPUBindGroup {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuBindGroupRelease(handle) })
    }

    override fun close() {
        session.resources.release(this)
    }
}

/**
 * Creates a [DawnBindGroup] on [this] session. Every binding resource must belong
 * to [this] session: a foreign object is refused (in the descriptor mapper) before
 * any handle is extracted or any downcall is made.
 */
internal fun DeviceSession.createBindGroup(descriptor: GPUBindGroupDescriptor): DawnBindGroup =
    runtime.dispatcher.call {
        memoryScope { allocator ->
            val native = allocator.allocateBindGroupDescriptor(descriptor, this)
            val handle = wgpuDeviceCreateBindGroup(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateBindGroup returned no bind group")
            DawnBindGroup(this, handle, descriptor.label)
        }
    }
