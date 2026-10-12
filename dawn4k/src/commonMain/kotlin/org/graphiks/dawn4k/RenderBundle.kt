package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
import org.graphiks.dawn4k.native.WGPURenderBundle
import org.graphiks.dawn4k.native.wgpuRenderBundleRelease
import org.graphiks.webgpu.GPURenderBundle

/**
 * A raw [GPURenderBundle] produced by [DawnRenderBundleEncoder.finish]. There is
 * no close in the WebGPU interface; callers close this concrete wrapper to
 * release its reference. Device teardown is not a resource cleanup mechanism.
 */
class DawnRenderBundle internal constructor(
    internal val session: DeviceSession,
    handle: WGPURenderBundle,
    label: String,
) : GPURenderBundle, AutoCloseable {

    override var label: String = label

    private val reference = OwnedReference(handle, release = { wgpuRenderBundleRelease(it) })
    internal val handle: WGPURenderBundle
        get() { session.requireOpen(); return reference.handle }

    override fun close() = reference.close()
}
