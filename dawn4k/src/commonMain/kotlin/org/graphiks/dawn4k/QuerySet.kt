package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.mapper.allocateQuerySetDescriptor
import org.graphiks.dawn4k.native.WGPUQuerySet
import org.graphiks.dawn4k.native.wgpuDeviceCreateQuerySet
import org.graphiks.dawn4k.native.wgpuQuerySetDestroy
import org.graphiks.dawn4k.native.wgpuQuerySetRelease
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUQuerySet
import org.graphiks.webgpu.GPUQuerySetDescriptor
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.GPUSize32Out

/**
 * A raw [GPUQuerySet] backed by a Dawn `WGPUQuerySet`. Its descriptor metadata
 * (type and count) is reported back verbatim; [close] destroys the query set and
 * keeps the tombstone handle until teardown.
 */
class DawnQuerySet internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUQuerySet,
    descriptor: GPUQuerySetDescriptor,
) : GPUQuerySet {

    override val type: GPUQueryType = descriptor.type
    override val count: GPUSize32Out = descriptor.count

    override var label: String = descriptor.label

    init {
        session.resources.own(
            key = this,
            destroy = { wgpuQuerySetDestroy(handle) },
            release = { wgpuQuerySetRelease(handle) },
        )
    }

    override fun close() {
        session.resources.destroy(this)
    }
}

/** Creates a [DawnQuerySet] on [this] session and registers it with the resource registry. */
internal fun DeviceSession.createQuerySet(descriptor: GPUQuerySetDescriptor): DawnQuerySet =
    runtime.dispatcher.call {
        memoryScope { allocator ->
            val native = allocator.allocateQuerySetDescriptor(descriptor)
            val handle = wgpuDeviceCreateQuerySet(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateQuerySet returned no query set")
            DawnQuerySet(this, handle, descriptor)
        }
    }
