package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
import org.graphiks.dawn4k.internal.transferOwnership
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
 * [close] destroys the texture and releases its native reference immediately.
 */
class DawnTexture internal constructor(
    internal val session: DeviceSession,
    handle: WGPUTexture,
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

    private val reference = OwnedReference(handle,
        destroy = { wgpuTextureDestroy(it) }, release = { wgpuTextureRelease(it) })
    internal val handle: WGPUTexture
        get() { session.requireOpen(); return reference.handle }

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
                transferOwnership(viewHandle, release = { org.graphiks.dawn4k.native.wgpuTextureViewRelease(it) }) {
                    DawnTextureView(session, it, descriptor?.label ?: "")
                }
            }
        }

    override fun close() {
        reference.close()
    }
}

/** Creates a caller-owned [DawnTexture] on [this] session. */
internal fun DeviceSession.createTexture(descriptor: GPUTextureDescriptor): DawnTexture =
    run {
        memoryScope { allocator ->
            val native = allocator.allocateTextureDescriptor(descriptor)
            val handle = wgpuDeviceCreateTexture(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateTexture returned no texture")
            transferOwnership(handle, release = { wgpuTextureRelease(it) }) { DawnTexture(this, it, descriptor) }
        }
    }
