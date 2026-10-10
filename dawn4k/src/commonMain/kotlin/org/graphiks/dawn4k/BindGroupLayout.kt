package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.mapper.allocateBindGroupLayoutDescriptor
import org.graphiks.dawn4k.native.WGPUBindGroupLayout
import org.graphiks.dawn4k.native.wgpuBindGroupLayoutRelease
import org.graphiks.dawn4k.native.wgpuDeviceCreateBindGroupLayout
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUBindGroupLayout
import org.graphiks.webgpu.GPUBindGroupLayoutDescriptor

/**
 * A raw [GPUBindGroupLayout] backed by a Dawn `WGPUBindGroupLayout`. Refcount-only:
 * Dawn has no separate destroy for bind group layouts, so [close] releases the
 * reference immediately.
 */
class DawnBindGroupLayout internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUBindGroupLayout,
    label: String,
) : GPUBindGroupLayout {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuBindGroupLayoutRelease(handle) })
    }

    override fun close() {
        session.resources.release(this)
    }
}

/** Refuses a foreign or foreign-session bind group layout before its handle is read. */
internal fun GPUBindGroupLayout.requireDawnBindGroupLayout(owner: DeviceSession): DawnBindGroupLayout {
    val dawn = this as? DawnBindGroupLayout
        ?: throw IllegalArgumentException("the bind group layout does not belong to this Dawn backend: $this")
    require(dawn.session === owner) { "the bind group layout belongs to a different device session" }
    return dawn
}

/** Creates a [DawnBindGroupLayout] on [this] session and registers its reference. */
internal fun DeviceSession.createBindGroupLayout(descriptor: GPUBindGroupLayoutDescriptor): DawnBindGroupLayout =
    run {
        memoryScope { allocator ->
            val native = allocator.allocateBindGroupLayoutDescriptor(descriptor)
            val handle = wgpuDeviceCreateBindGroupLayout(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateBindGroupLayout returned no layout")
            DawnBindGroupLayout(this, handle, descriptor.label)
        }
    }
