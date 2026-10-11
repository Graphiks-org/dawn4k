package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
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
    handle: WGPUSampler,
    label: String,
) : GPUSampler {

    override var label: String = label

    private val reference = OwnedReference(handle, release = { wgpuSamplerRelease(it) })
    internal val handle: WGPUSampler
        get() { session.requireOpen(); return reference.handle }

    override fun close() {
        reference.close()
    }
}

/** Creates a caller-owned [DawnSampler] on [this] session. */
internal fun DeviceSession.createSampler(descriptor: GPUSamplerDescriptor): DawnSampler =
    run {
        memoryScope { allocator ->
            val native = allocator.allocateSamplerDescriptor(descriptor)
            val handle = wgpuDeviceCreateSampler(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateSampler returned no sampler")
            DawnSampler(this, handle, descriptor.label)
        }
    }
