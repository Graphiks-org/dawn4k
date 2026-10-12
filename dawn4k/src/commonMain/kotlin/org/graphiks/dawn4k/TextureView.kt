package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
import org.graphiks.dawn4k.native.WGPUTextureView
import org.graphiks.dawn4k.native.wgpuTextureViewRelease
import org.graphiks.webgpu.GPUTextureView

/**
 * A raw [GPUTextureView] backed by a Dawn `WGPUTextureView`. Refcount-only:
 * [close] releases the reference immediately.
 */
class DawnTextureView internal constructor(
    internal val session: DeviceSession,
    handle: WGPUTextureView,
    label: String,
) : GPUTextureView {

    override var label: String = label

    private val reference = OwnedReference(handle, release = { wgpuTextureViewRelease(it) })
    internal val handle: WGPUTextureView
        get() { session.requireOpen(); return reference.handle }

    override fun close() {
        reference.close()
    }
}
