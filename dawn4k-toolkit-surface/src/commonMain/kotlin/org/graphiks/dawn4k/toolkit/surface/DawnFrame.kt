package org.graphiks.dawn4k.toolkit.surface

import org.graphiks.dawn4k.toolkit.surface.internal.NativeSurface
import org.graphiks.dawn4k.toolkit.surface.internal.SurfaceAcquisition
import org.graphiks.webgpu.GPUTextureView

/** One acquired surface texture. Closing releases its view and reference, without destroying the texture. */
class DawnFrame internal constructor(
    private val texture: SurfaceAcquisition.Texture,
    val extent: PixelExtent,
    private val owner: NativeSurface,
    private val canPresent: () -> Boolean,
    private val onClosed: () -> Unit,
) : AutoCloseable {
    private var closed = false
    private var presented = false
    val view: GPUTextureView get() { check(!closed) { "frame is closed" }; return texture.view }

    internal fun requireOwner(expected: NativeSurface) { require(owner === expected) { "frame belongs to another surface" } }

    fun present() {
        check(!closed && !presented) { "frame is closed or already presented" }
        check(canPresent()) { "presentation target is no longer available" }
        presented = true
        owner.present()
    }
    override fun close() {
        if (closed) return
        closed = true
        try { texture.view.close() }
        finally { try { texture.release() } finally { onClosed() } }
    }
}
