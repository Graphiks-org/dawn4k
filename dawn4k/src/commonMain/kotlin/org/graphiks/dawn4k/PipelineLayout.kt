package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
import org.graphiks.dawn4k.internal.transferOwnership
import org.graphiks.dawn4k.mapper.allocatePipelineLayoutDescriptor
import org.graphiks.dawn4k.native.WGPUPipelineLayout
import org.graphiks.dawn4k.native.wgpuDeviceCreatePipelineLayout
import org.graphiks.dawn4k.native.wgpuPipelineLayoutRelease
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUPipelineLayout
import org.graphiks.webgpu.GPUPipelineLayoutDescriptor

/**
 * A raw [GPUPipelineLayout] backed by a Dawn `WGPUPipelineLayout`. Refcount-only:
 * [close] releases the reference immediately.
 */
class DawnPipelineLayout internal constructor(
    internal val session: DeviceSession,
    handle: WGPUPipelineLayout,
    label: String,
) : GPUPipelineLayout {

    override var label: String = label

    private val reference = OwnedReference(handle, release = { wgpuPipelineLayoutRelease(it) })
    internal val handle: WGPUPipelineLayout
        get() { session.requireOpen(); return reference.handle }

    override fun close() {
        reference.close()
    }
}

/** Creates a caller-owned [DawnPipelineLayout] on [this] session. */
internal fun DeviceSession.createPipelineLayout(descriptor: GPUPipelineLayoutDescriptor): DawnPipelineLayout =
    run {
        memoryScope { allocator ->
            val native = allocator.allocatePipelineLayoutDescriptor(descriptor)
            val handle = wgpuDeviceCreatePipelineLayout(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreatePipelineLayout returned no layout")
            transferOwnership(handle, release = { wgpuPipelineLayoutRelease(it) }) { DawnPipelineLayout(this, it, descriptor.label) }
        }
    }
