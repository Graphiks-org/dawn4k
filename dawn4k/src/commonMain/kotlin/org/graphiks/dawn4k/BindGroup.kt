package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
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
    handle: WGPUBindGroup,
    label: String,
) : GPUBindGroup {

    override var label: String = label

    private val reference = OwnedReference(handle, release = { wgpuBindGroupRelease(it) })
    internal val handle: WGPUBindGroup
        get() { session.requireOpen(); return reference.handle }

    override fun close() {
        reference.close()
    }
}

/**
 * Creates a [DawnBindGroup] on [this] session. Dawn validates that binding
 * resources belong to the same device.
 */
internal fun DeviceSession.createBindGroup(descriptor: GPUBindGroupDescriptor): DawnBindGroup =
    run {
        memoryScope { allocator ->
            val native = allocator.allocateBindGroupDescriptor(descriptor)
            val handle = wgpuDeviceCreateBindGroup(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateBindGroup returned no bind group")
            DawnBindGroup(this, handle, descriptor.label)
        }
    }
