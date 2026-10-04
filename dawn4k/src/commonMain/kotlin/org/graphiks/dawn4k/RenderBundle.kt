package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.native.WGPURenderBundle
import org.graphiks.dawn4k.native.wgpuRenderBundleRelease
import org.graphiks.webgpu.GPURenderBundle

/**
 * A raw [GPURenderBundle] produced by [DawnRenderBundleEncoder.finish]. There is
 * no [close] in the interface: the bundle is an owned reference of the session,
 * released by the session teardown (never leaked after finish), and stays valid
 * for execution in render passes until then.
 */
class DawnRenderBundle internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPURenderBundle,
    label: String,
) : GPURenderBundle {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuRenderBundleRelease(handle) })
    }
}
