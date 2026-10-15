package org.graphiks.dawn4k.toolkit.surface.internal

import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.native.*
import org.graphiks.dawn4k.toolkit.surface.*
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope

internal sealed interface SurfaceAcquisition {
    class Texture(val view: GPUTextureView, val release: () -> Unit, val suboptimal: Boolean = false) : SurfaceAcquisition
    data object Timeout : SurfaceAcquisition
    data object Outdated : SurfaceAcquisition
    data object Lost : SurfaceAcquisition
    class Error(val cause: Throwable) : SurfaceAcquisition
}
internal interface NativeSurface : AutoCloseable {
    fun capabilities(): SurfaceCapabilities
    fun configure(configuration: DawnSurfaceConfiguration)
    fun unconfigure()
    fun acquire(): SurfaceAcquisition
    fun present()
}

internal class DawnNativeSurface(
    private val reference: DawnSurfaceReference,
    private val adapter: DawnAdapter,
    private val device: DawnDevice,
) : NativeSurface {
    private val handle: WGPUSurface get() = WGPUSurface(NativeAddress(reference.nativeHandle()))
    private var configured = false
    private var closed = false

    override fun capabilities(): SurfaceCapabilities = memoryScope { allocator ->
        val caps = WGPUSurfaceCapabilities.allocate(allocator).apply {
            nextInChain = null; usages = 0uL
            formatCount = 0uL; formats = null
            alphaModeCount = 0uL; alphaModes = null
            presentModeCount = 0uL; presentModes = null
        }
        try {
            check(wgpuSurfaceGetCapabilities(handle, WGPUAdapter(NativeAddress(adapter.nativeHandle())), caps) == WGPUStatus_Success) {
                "surface is incompatible with this adapter"
            }
            check(caps.usages and WGPUTextureUsage_RenderAttachment != 0uL) { "surface cannot be a render attachment" }
            SurfaceCapabilities(
                readSurfaceEnums(caps.formats, caps.formatCount).mapNotNull { value -> GPUTextureFormat.entries.firstOrNull { it.value == value } },
                readSurfaceEnums(caps.alphaModes, caps.alphaModeCount).mapNotNull { value -> DawnAlphaMode.entries.firstOrNull { it.nativeValue == value } },
                readSurfaceEnums(caps.presentModes, caps.presentModeCount).mapNotNull { value -> DawnPresentMode.entries.firstOrNull { it.nativeValue == value } },
            )
        } finally { wgpuSurfaceCapabilitiesFreeMembers(caps) }
    }

    override fun configure(configuration: DawnSurfaceConfiguration) {
        check(!closed)
        require(configuration.extent.isPositive)
        memoryScope { allocator ->
            val native = WGPUSurfaceConfiguration.allocate(allocator).apply {
                nextInChain = null
                device = WGPUDevice(NativeAddress(this@DawnNativeSurface.device.nativeHandle()))
                format = configuration.format.value
                usage = WGPUTextureUsage_RenderAttachment
                width = configuration.extent.width.toUInt(); height = configuration.extent.height.toUInt()
                viewFormatCount = 0uL; viewFormats = null
                alphaMode = configuration.alphaMode.nativeValue; presentMode = configuration.presentMode.nativeValue
            }
            wgpuSurfaceConfigure(handle, native)
        }
        configured = true
    }
    override fun unconfigure() { if (configured) { wgpuSurfaceUnconfigure(handle); configured = false } }

    override fun acquire(): SurfaceAcquisition = memoryScope { allocator ->
        check(!closed && configured) { "surface is not configured" }
        val output = WGPUSurfaceTexture.allocate(allocator).apply { nextInChain = null; texture = null; status = 0u }
        wgpuSurfaceGetCurrentTexture(handle, output)
        val texture = output.texture
        if (output.status == WGPUSurfaceGetCurrentTextureStatus_SuccessOptimal ||
            output.status == WGPUSurfaceGetCurrentTextureStatus_SuccessSuboptimal) {
            check(texture != null) { "Dawn reported success without a surface texture" }
            try {
                val view = wgpuTextureCreateView(texture, null) ?: error("surface texture view creation failed")
                val ownedView = try { device.adoptSurfaceTextureView(view.handler.rawValue) }
                catch (failure: Throwable) { wgpuTextureViewRelease(view); throw failure }
                SurfaceAcquisition.Texture(ownedView, { wgpuTextureRelease(texture) },
                    output.status == WGPUSurfaceGetCurrentTextureStatus_SuccessSuboptimal)
            } catch (failure: Throwable) { wgpuTextureRelease(texture); throw failure }
        } else {
            texture?.let { wgpuTextureRelease(it) }
            when (output.status) {
                WGPUSurfaceGetCurrentTextureStatus_Timeout -> SurfaceAcquisition.Timeout
                WGPUSurfaceGetCurrentTextureStatus_Outdated -> SurfaceAcquisition.Outdated
                WGPUSurfaceGetCurrentTextureStatus_Lost -> SurfaceAcquisition.Lost
                WGPUSurfaceGetCurrentTextureStatus_Error -> SurfaceAcquisition.Error(IllegalStateException("surface acquisition failed"))
                else -> SurfaceAcquisition.Error(IllegalStateException("unknown surface acquisition status: ${output.status}"))
            }
        }
    }
    override fun present() {
        check(!closed && configured)
        check(wgpuSurfacePresent(handle) == WGPUStatus_Success) { "surface presentation failed" }
    }
    override fun close() {
        if (closed) return
        unconfigure()
        reference.close()
        closed = true
    }
}
