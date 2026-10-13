package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
import org.graphiks.dawn4k.internal.transferOwnership
import org.graphiks.dawn4k.mapper.allocateQuerySetDescriptor
import org.graphiks.dawn4k.native.WGPUQuerySet
import org.graphiks.dawn4k.native.wgpuDeviceCreateQuerySet
import org.graphiks.dawn4k.native.wgpuDeviceHasFeature
import org.graphiks.dawn4k.native.wgpuQuerySetDestroy
import org.graphiks.dawn4k.native.wgpuQuerySetRelease
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUQuerySet
import org.graphiks.webgpu.GPUQuerySetDescriptor
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.GPUFeatureName
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

/**
 * Creates a caller-owned [DawnQuerySet] on [this] session. Timestamp queries
 * require the device feature; refusal is synchronous and not a scope error.
 */
internal fun DeviceSession.createQuerySet(descriptor: GPUQuerySetDescriptor): DawnQuerySet =
    run {
        // WebGPU rejects this on the content timeline, without a scope error.
        if (descriptor.type == GPUQueryType.Timestamp) {
            require(wgpuDeviceHasFeature(handle, GPUFeatureName.TimestampQuery.value) != 0u) {
                "TimestampQuery must be enabled on the device to create a timestamp query set"
            }
        }
        memoryScope { allocator ->
            val native = allocator.allocateQuerySetDescriptor(descriptor)
            val handle = wgpuDeviceCreateQuerySet(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateQuerySet returned no query set")
            transferOwnership(handle, release = { wgpuQuerySetRelease(it) }) { DawnQuerySet(this, it, descriptor) }
        }
    }
