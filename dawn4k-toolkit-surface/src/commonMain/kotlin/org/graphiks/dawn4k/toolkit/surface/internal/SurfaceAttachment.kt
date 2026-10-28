package org.graphiks.dawn4k.toolkit.surface.internal

import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.DawnSession
import org.graphiks.dawn4k.toolkit.surface.*

internal fun interface SurfaceFactory {
    fun create(lease: DawnTargetLease, context: DawnContext): SurfaceAttachment

    companion object {
        val Native = SurfaceFactory { lease, context ->
            val reference = lease.createSurface(context)
            SurfaceAttachment(lease, reference, { DawnNativeSurface(reference, it.adapter, it.device) }, reference::close)
        }
    }
}

/** The binding owns the surface before a device exists; the driver assumes it after bind. */
internal class SurfaceAttachment(
    val lease: DawnTargetLease,
    val compatibleSurface: DawnSurfaceReference?,
    private val bindNative: (DawnSession) -> NativeSurface,
    private val releaseBinding: () -> Unit,
    native: NativeSurface? = null,
) {
    val generation: Long = lease.state.value.generation
    private var bound: NativeSurface? = native
    private var surfaceRetired = false
    private var leaseRetired = false

    val surface: NativeSurface get() = checkNotNull(bound) { "surface is not bound to a device" }
    fun bind(gpu: DawnSession) { if (bound == null) bound = bindNative(gpu) }
    fun replaceNative(replacement: NativeSurface) {
        check(!surfaceRetired)
        surface.close()
        bound = replacement
    }
    suspend fun retire() {
        if (!surfaceRetired) {
            if (bound == null) releaseBinding() else surface.close()
            surfaceRetired = true
        }
        if (!leaseRetired) { lease.retire(); leaseRetired = true }
    }
}
