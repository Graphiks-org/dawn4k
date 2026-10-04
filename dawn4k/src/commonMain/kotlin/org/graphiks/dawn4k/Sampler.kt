package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.mapper.allocateSamplerDescriptor
import org.graphiks.dawn4k.native.WGPUSampler
import org.graphiks.dawn4k.native.wgpuDeviceCreateSampler
import org.graphiks.dawn4k.native.wgpuSamplerRelease
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUSampler
import org.graphiks.webgpu.GPUSamplerDescriptor

/**
 * A raw [GPUSampler] backed by a Dawn `WGPUSampler`. Refcount-only: [close]
 * releases the reference immediately.
 */
class DawnSampler internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUSampler,
    label: String,
) : GPUSampler {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuSamplerRelease(handle) })
    }

    override fun close() {
        session.resources.release(this)
    }
}

/** Refuses a foreign or foreign-session sampler before its handle is read. */
internal fun GPUSampler.requireDawnSampler(owner: DeviceSession): DawnSampler {
    val dawn = this as? DawnSampler
        ?: throw IllegalArgumentException("the sampler does not belong to this Dawn backend: $this")
    require(dawn.session === owner) { "the sampler belongs to a different device session" }
    return dawn
}

/** Creates a [DawnSampler] on [this] session and registers its reference. */
internal fun DeviceSession.createSampler(descriptor: GPUSamplerDescriptor): DawnSampler =
    runtime.dispatcher.call {
        memoryScope { allocator ->
            val native = allocator.allocateSamplerDescriptor(descriptor)
            val handle = wgpuDeviceCreateSampler(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateSampler returned no sampler")
            DawnSampler(this, handle, descriptor.label)
        }
    }
