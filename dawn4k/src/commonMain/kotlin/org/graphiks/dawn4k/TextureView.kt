package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.WGPUTextureView
import org.graphiks.dawn4k.native.wgpuTextureViewRelease
import org.graphiks.webgpu.GPUTextureView

/**
 * A raw [GPUTextureView] backed by a Dawn `WGPUTextureView`. Refcount-only:
 * [close] releases the reference immediately.
 */
class DawnTextureView internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUTextureView,
    label: String,
) : GPUTextureView {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuTextureViewRelease(handle) })
    }

    override fun close() {
        session.resources.release(this)
    }
}
