package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
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
 * releases its native reference immediately.
 */
class DawnQuerySet internal constructor(
    internal val session: DeviceSession,
    handle: WGPUQuerySet,
    descriptor: GPUQuerySetDescriptor,
) : GPUQuerySet {

    override val type: GPUQueryType = descriptor.type
    override val count: GPUSize32Out = descriptor.count

    override var label: String = descriptor.label

    private val reference = OwnedReference(handle,
        destroy = { wgpuQuerySetDestroy(it) }, release = { wgpuQuerySetRelease(it) })
    internal val handle: WGPUQuerySet
        get() { session.requireOpen(); return reference.handle }

    override fun close() {
        reference.close()
    }
}

/** Creates a caller-owned [DawnQuerySet] on [this] session. */
internal fun DeviceSession.createQuerySet(descriptor: GPUQuerySetDescriptor): DawnQuerySet =
    run {
        memoryScope { allocator ->
            val native = allocator.allocateQuerySetDescriptor(descriptor)
            val handle = wgpuDeviceCreateQuerySet(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateQuerySet returned no query set")
            DawnQuerySet(this, handle, descriptor)
        }
    }
