package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.mapper.allocateTextureDescriptor
import org.graphiks.dawn4k.mapper.allocateTextureViewDescriptor
import org.graphiks.dawn4k.native.WGPUTexture
import org.graphiks.dawn4k.native.wgpuDeviceCreateTexture
import org.graphiks.dawn4k.native.wgpuTextureCreateView
import org.graphiks.dawn4k.native.wgpuTextureDestroy
import org.graphiks.dawn4k.native.wgpuTextureRelease
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureDescriptor
import org.graphiks.webgpu.GPUTextureDimension
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.GPUTextureViewDescriptor
import org.graphiks.webgpu.GPUIntegerCoordinateOut
import org.graphiks.webgpu.GPUSize32Out

/**
 * A raw [GPUTexture] backed by a Dawn `WGPUTexture`. Its descriptor metadata
 * (size, mip/sample counts, dimension, format, usage) is reported back verbatim;
 * [close] destroys the texture and keeps the tombstone handle until teardown.
 */
class DawnTexture internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUTexture,
    descriptor: GPUTextureDescriptor,
) : GPUTexture {

    override val width: GPUIntegerCoordinateOut = descriptor.size.width
    override val height: GPUIntegerCoordinateOut = descriptor.size.height
    override val depthOrArrayLayers: GPUIntegerCoordinateOut = descriptor.size.depthOrArrayLayers
    override val mipLevelCount: GPUIntegerCoordinateOut = descriptor.mipLevelCount
    override val sampleCount: GPUSize32Out = descriptor.sampleCount
    override val dimension: GPUTextureDimension = descriptor.dimension
    override val format: GPUTextureFormat = descriptor.format
    override val usage: GPUTextureUsage = descriptor.usage

    override var label: String = descriptor.label

    init {
        session.resources.own(
            key = this,
            destroy = { wgpuTextureDestroy(handle) },
            release = { wgpuTextureRelease(handle) },
        )
    }

    /**
     * Creates a [DawnTextureView] over this texture. A null [descriptor] is passed
     * through to Dawn unchanged, so it adopts the C `WGPU_TEXTURE_VIEW_DESCRIPTOR_INIT`
     * defaults (whole texture, all aspects). The returned view owns a NEW native
     * reference; its [GPUTextureView.close] releases it.
     */
    override fun createView(descriptor: GPUTextureViewDescriptor?): GPUTextureView =
        run {
            memoryScope { allocator ->
                val native = descriptor?.let { allocator.allocateTextureViewDescriptor(it) }
                val viewHandle = wgpuTextureCreateView(handle, native)
                    ?: throw IllegalStateException("wgpuTextureCreateView returned no view")
                DawnTextureView(session, viewHandle, descriptor?.label ?: "")
            }
        }

    override fun close() {
        session.resources.destroy(this)
    }
}

/** Refuses a foreign or foreign-session texture before its handle is read. */
internal fun GPUTexture.requireDawnTexture(owner: DeviceSession): DawnTexture {
    val dawn = this as? DawnTexture
        ?: throw IllegalArgumentException("the texture does not belong to this Dawn backend: $this")
    require(dawn.session === owner) { "the texture belongs to a different device session" }
    return dawn
}

/** Creates a [DawnTexture] on [this] session and registers it with the resource registry. */
internal fun DeviceSession.createTexture(descriptor: GPUTextureDescriptor): DawnTexture =
    run {
        memoryScope { allocator ->
            val native = allocator.allocateTextureDescriptor(descriptor)
            val handle = wgpuDeviceCreateTexture(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateTexture returned no texture")
            DawnTexture(this, handle, descriptor)
        }
    }
