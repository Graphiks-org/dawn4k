package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
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
    internal val handle: WGPUPipelineLayout,
    label: String,
) : GPUPipelineLayout {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuPipelineLayoutRelease(handle) })
    }

    override fun close() {
        session.resources.release(this)
    }
}

/** Refuses a foreign or foreign-session pipeline layout before its handle is read. */
internal fun GPUPipelineLayout.requireDawnPipelineLayout(owner: DeviceSession): DawnPipelineLayout {
    val dawn = this as? DawnPipelineLayout
        ?: throw IllegalArgumentException("the pipeline layout does not belong to this Dawn backend: $this")
    require(dawn.session === owner) { "the pipeline layout belongs to a different device session" }
    return dawn
}

/** Creates a [DawnPipelineLayout] on [this] session and registers its reference. */
internal fun DeviceSession.createPipelineLayout(descriptor: GPUPipelineLayoutDescriptor): DawnPipelineLayout =
    run {
        memoryScope { allocator ->
            val native = allocator.allocatePipelineLayoutDescriptor(descriptor, this)
            val handle = wgpuDeviceCreatePipelineLayout(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreatePipelineLayout returned no layout")
            DawnPipelineLayout(this, handle, descriptor.label)
        }
    }
