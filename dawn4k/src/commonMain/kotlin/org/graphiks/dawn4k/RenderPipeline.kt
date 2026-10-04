package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.mapper.allocateRenderPipelineDescriptor
import org.graphiks.dawn4k.native.WGPURenderPipeline
import org.graphiks.dawn4k.native.wgpuDeviceCreateRenderPipeline
import org.graphiks.dawn4k.native.wgpuRenderPipelineGetBindGroupLayout
import org.graphiks.dawn4k.native.wgpuRenderPipelineRelease
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUBindGroupLayout
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPURenderPipelineDescriptor

/**
 * A raw [GPURenderPipeline] backed by a Dawn `WGPURenderPipeline`. Refcount-only:
 * [close] releases the reference immediately.
 */
class DawnRenderPipeline internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPURenderPipeline,
    label: String,
) : GPURenderPipeline {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuRenderPipelineRelease(handle) })
    }

    /**
     * The owned bind group layout at [index]. The native call returns a NEW
     * reference; the returned [DawnBindGroupLayout] owns it, and its [close]
     * releases it.
     */
    override fun getBindGroupLayout(index: UInt): GPUBindGroupLayout {
        val layoutHandle = session.runtime.dispatcher.call { wgpuRenderPipelineGetBindGroupLayout(handle, index) }
            ?: throw IllegalStateException("wgpuRenderPipelineGetBindGroupLayout returned no layout")
        return DawnBindGroupLayout(session, layoutHandle, "")
    }

    override fun close() {
        session.resources.release(this)
    }
}

/** Refuses a foreign or foreign-session render pipeline before its handle is read. */
internal fun GPURenderPipeline.requireDawnRenderPipeline(owner: DeviceSession): DawnRenderPipeline {
    val dawn = this as? DawnRenderPipeline
        ?: throw IllegalArgumentException("the render pipeline does not belong to this Dawn backend: $this")
    require(dawn.session === owner) { "the render pipeline belongs to a different device session" }
    return dawn
}

/** Creates a [DawnRenderPipeline] on [this] session and registers its reference. */
internal fun DeviceSession.createRenderPipeline(descriptor: GPURenderPipelineDescriptor): DawnRenderPipeline =
    runtime.dispatcher.call {
        memoryScope { allocator ->
            val native = allocator.allocateRenderPipelineDescriptor(descriptor, this)
            val handle = wgpuDeviceCreateRenderPipeline(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateRenderPipeline returned no pipeline")
            DawnRenderPipeline(this, handle, descriptor.label)
        }
    }
