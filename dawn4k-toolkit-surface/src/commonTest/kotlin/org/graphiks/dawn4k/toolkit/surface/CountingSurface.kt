package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.surface.internal.*
import org.graphiks.webgpu.*

internal class CountingSurface : NativeSurface {
    var configurations = 0
    var presentations = 0
    var closes = 0
    var unconfigurations = 0
    var capabilityFailure: Throwable? = null
    var onCapabilities: () -> Unit = {}
    var onClose: () -> Unit = {}
    val releases = mutableListOf<String>()
    val statuses = mutableListOf<SurfaceAcquisition>()
    override fun capabilities(): SurfaceCapabilities {
        onCapabilities()
        capabilityFailure?.let { throw it }
        return SurfaceCapabilities(listOf(GPUTextureFormat.BGRA8Unorm), listOf(DawnAlphaMode.Opaque), listOf(DawnPresentMode.Fifo))
    }
    override fun configure(configuration: DawnSurfaceConfiguration) { configurations++ }
    override fun unconfigure() { unconfigurations++ }
    override fun acquire(): SurfaceAcquisition = if (statuses.isEmpty()) texture() else statuses.removeAt(0)
    fun texture() = SurfaceAcquisition.Texture(
        object : GPUTextureView {
            override var label = ""
            override fun close() { releases += "view" }
        },
        { releases += "texture" },
    )
    override fun present() { presentations++ }
    override fun close() { onClose(); closes++ }
}

internal class CountingTarget(generation: Long, extent: PixelExtent = PixelExtent(16, 16)) : DawnSurfaceTarget, DawnTargetLease {
    override val state = MutableStateFlow(DawnTargetState(generation, extent, true))
    var retirements = 0
    var onRetire: () -> Unit = {}
    override suspend fun retain(): DawnTargetLease = this
    override fun createSurface(context: DawnContext): DawnSurfaceReference = error("fixture uses a fake surface binding")
    override suspend fun retire() { onRetire(); retirements++ }
}

internal suspend fun attachment(target: CountingTarget, native: NativeSurface) =
    SurfaceAttachment(target.retain(), null, { native }, native::close, native = native)
