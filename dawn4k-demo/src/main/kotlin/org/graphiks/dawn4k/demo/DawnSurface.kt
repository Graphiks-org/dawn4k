package org.graphiks.dawn4k.demo

import org.graphiks.dawn4k.NativeBridge
import org.graphiks.dawn4k.native.WGPUAdapter
import org.graphiks.dawn4k.native.WGPUSurfaceCapabilities
import org.graphiks.dawn4k.native.wgpuSurfaceGetCapabilities
import org.graphiks.dawn4k.native.wgpuSurfaceCapabilitiesFreeMembers
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.dawn4k.native.WGPUCompositeAlphaMode_Auto
import org.graphiks.dawn4k.native.WGPUDevice
import org.graphiks.dawn4k.native.WGPUInstance
import org.graphiks.dawn4k.native.WGPUPresentMode_Fifo
import org.graphiks.dawn4k.native.WGPUSurface
import org.graphiks.dawn4k.native.WGPUSurfaceConfiguration
import org.graphiks.dawn4k.native.WGPUSurfaceDescriptor
import org.graphiks.dawn4k.native.WGPUSurfaceGetCurrentTextureStatus_Lost
import org.graphiks.dawn4k.native.WGPUSurfaceGetCurrentTextureStatus_Outdated
import org.graphiks.dawn4k.native.WGPUSurfaceGetCurrentTextureStatus_SuccessOptimal
import org.graphiks.dawn4k.native.WGPUSurfaceGetCurrentTextureStatus_SuccessSuboptimal
import org.graphiks.dawn4k.native.WGPUSurfaceSourceMetalLayer
import org.graphiks.dawn4k.native.WGPUSurfaceSourceWindowsHWND
import org.graphiks.dawn4k.native.WGPUSurfaceSourceXlibWindow
import org.graphiks.dawn4k.native.WGPUSType_SurfaceSourceXlibWindow
import org.graphiks.dawn4k.native.WGPUSType_SurfaceSourceWindowsHWND
import org.graphiks.dawn4k.native.WGPUChainedStruct
import org.graphiks.dawn4k.native.WGPUSurfaceTexture
import org.graphiks.dawn4k.native.WGPUStatus_Success
import org.graphiks.dawn4k.native.WGPUTexture
import org.graphiks.dawn4k.native.WGPUTextureFormat_BGRA8Unorm
import org.graphiks.dawn4k.native.WGPUTextureUsage_RenderAttachment
import org.graphiks.dawn4k.native.WGPUSType_SurfaceSourceMetalLayer
import org.graphiks.dawn4k.native.wgpuInstanceCreateSurface
import org.graphiks.dawn4k.native.wgpuSurfaceConfigure
import org.graphiks.dawn4k.native.wgpuSurfaceGetCurrentTexture
import org.graphiks.dawn4k.native.wgpuSurfacePresent
import org.graphiks.dawn4k.native.wgpuSurfaceRelease
import org.graphiks.dawn4k.native.wgpuSurfaceUnconfigure
import org.graphiks.dawn4k.native.wgpuTextureRelease
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.memoryScope

/**
 * A Dawn presentation surface, living entirely inside the demo.
 *
 * The surface is created from the raw native handles exposed by `:dawn4k`'s
 * platform-integrator accessors ([NativeBridge], `DawnDevice.nativeHandle()`).
 * Every native call is routed through [bridge.call] so it runs on the backend's
 * worker thread — the only thread allowed to touch Dawn handles.
 *
 * The current texture is BORROWED from the surface: it is released after
 * [present], never destroyed. [BorrowedSurfaceTexture.close] releases only
 * the acquired reference, including when encoding or presentation fails.
 */
class DawnSurface private constructor(
    private val bridge: NativeBridge,
    private val device: WGPUDevice,
    private val handle: WGPUSurface,
) : AutoCloseable {

    private var configuredWidth: Int = 0
    private var configuredHeight: Int = 0
    private var closed = false
    private var configuration = SurfaceConfiguration(
        GPUTextureFormat.BGRA8Unorm, WGPUTextureFormat_BGRA8Unorm,
        WGPUCompositeAlphaMode_Auto, WGPUPresentMode_Fifo,
    )

    val textureFormat: GPUTextureFormat get() = configuration.textureFormat

    /** Adapter must remain alive; returned members are copied and then freed. */
    internal fun configureForAdapter(adapterHandle: Long): SurfaceConfiguration {
        check(!closed) { "the surface is closed" }
        require(adapterHandle != 0L) { "the adapter is closed: no adapter handle" }
        return bridge.call {
            memoryScope { allocator ->
                val capabilities = WGPUSurfaceCapabilities.allocate(allocator)
                capabilities.nextInChain = null
                capabilities.usages = 0uL
                capabilities.formatCount = 0uL
                capabilities.formats = null
                capabilities.presentModeCount = 0uL
                capabilities.presentModes = null
                capabilities.alphaModeCount = 0uL
                capabilities.alphaModes = null
                try {
                    val status = wgpuSurfaceGetCapabilities(handle,
                        WGPUAdapter(NativeAddress(adapterHandle)), capabilities)
                    check(status == WGPUStatus_Success) { "wgpuSurfaceGetCapabilities failed (status=$status)" }
                    check(capabilities.usages and WGPUTextureUsage_RenderAttachment != 0uL) {
                        "surface does not support RenderAttachment usage"
                    }
                    val selected = selectSurfaceConfiguration(
                        readSurfaceEnums(capabilities.formats, capabilities.formatCount),
                        readSurfaceEnums(capabilities.alphaModes, capabilities.alphaModeCount),
                        readSurfaceEnums(capabilities.presentModes, capabilities.presentModeCount),
                    )
                    configuration = selected
                    println("[demo] surface capabilities: $selected")
                    selected
                } finally {
                    wgpuSurfaceCapabilitiesFreeMembers(capabilities)
                }
            }
        }
    }

    /**
     * Creates a surface over [metalLayerPtr] (a `CAMetalLayer*` as a Long).
     * The layer must outlive the surface.
     */
    companion object {
        /** webgpu.h `WGPU_STRLEN` (SIZE_MAX): the NUL-terminated length sentinel. */
        private const val WGPU_STRLEN: ULong = ULong.MAX_VALUE

        fun createMetal(
            bridge: NativeBridge,
            deviceHandle: Long,
            metalLayerPtr: Long,
        ): DawnSurface {
            require(metalLayerPtr != 0L) { "the metal layer pointer is null" }
            return create(bridge, deviceHandle) { allocator ->
                val source = WGPUSurfaceSourceMetalLayer.allocate(allocator)
                source.chain.next = null
                source.chain.sType = WGPUSType_SurfaceSourceMetalLayer
                source.layer = NativeAddress(metalLayerPtr)
                source.chain
            }
        }

        fun createWindows(bridge: NativeBridge, deviceHandle: Long, hwnd: Long, hinstance: Long): DawnSurface {
            require(hwnd != 0L && hinstance != 0L) { "the Win32 window or instance handle is null" }
            return create(bridge, deviceHandle) { allocator ->
                val source = WGPUSurfaceSourceWindowsHWND.allocate(allocator)
                source.chain.next = null
                source.chain.sType = WGPUSType_SurfaceSourceWindowsHWND
                source.hwnd = NativeAddress(hwnd)
                source.hinstance = NativeAddress(hinstance)
                source.chain
            }
        }

        fun createXlib(bridge: NativeBridge, deviceHandle: Long, display: Long, window: Long): DawnSurface {
            require(display != 0L && window != 0L) { "the X11 display or window handle is null" }
            return create(bridge, deviceHandle) { allocator ->
                val source = WGPUSurfaceSourceXlibWindow.allocate(allocator)
                source.chain.next = null
                source.chain.sType = WGPUSType_SurfaceSourceXlibWindow
                source.display = NativeAddress(display)
                source.window = window.toULong()
                source.chain
            }
        }

        private fun create(bridge: NativeBridge, deviceHandle: Long, source: (MemoryAllocator) -> WGPUChainedStruct): DawnSurface {
            val instanceHandle = bridge.instanceHandle()
            require(instanceHandle != 0L) { "the context is closed: no instance handle" }
            require(deviceHandle != 0L) { "the device is closed: no device handle" }
            return bridge.call {
                memoryScope { allocator ->
                    val instance = WGPUInstance(NativeAddress(instanceHandle))
                    val device = WGPUDevice(NativeAddress(deviceHandle))

                    val descriptor = WGPUSurfaceDescriptor.allocate(allocator)
                    descriptor.nextInChain = source(allocator)
                    descriptor.label.data = null
                    descriptor.label.length = WGPU_STRLEN

                    val surface = wgpuInstanceCreateSurface(instance, descriptor)
                        ?: throw IllegalStateException("wgpuInstanceCreateSurface returned no surface")
                    DawnSurface(bridge, device, surface)
                }
            }
        }

    }

    /**
     * Configures (or reconfigures) the surface for [width]×[height] pixels.
     * Safe to call again on resize.
     */
    fun configure(width: Int, height: Int) {
        check(!closed) { "the surface is closed" }
        require(width > 0 && height > 0) { "dimensions must be positive, was ${width}x${height}" }
        bridge.call {
            memoryScope { allocator ->
                val config = WGPUSurfaceConfiguration.allocate(allocator)
                config.nextInChain = null
                config.device = device
                config.format = configuration.nativeFormat
                config.usage = WGPUTextureUsage_RenderAttachment
                config.width = width.toUInt()
                config.height = height.toUInt()
                config.viewFormatCount = 0uL
                config.viewFormats = null
                config.alphaMode = configuration.alphaMode
                config.presentMode = configuration.presentMode
                wgpuSurfaceConfigure(handle, config)
            }
        }
        configuredWidth = width
        configuredHeight = height
    }

    /** The configured width, or 0 before the first [configure]. */
    val width: Int get() = configuredWidth

    /** The configured height, or 0 before the first [configure]. */
    val height: Int get() = configuredHeight

    /**
     * Acquires a borrowed surface texture reference; close it after presentation.
     * The texture is valid until [present] is called with it.
     *
     * @throws SurfaceOutdatedException when the surface must be reconfigured
     *         (resize, mode change) before the next frame.
     */
    fun acquireFrame(): BorrowedSurfaceTexture {
        check(!closed) { "the surface is closed" }
        return bridge.call {
            memoryScope { allocator ->
                val surfaceTexture = WGPUSurfaceTexture.allocate(allocator)
                surfaceTexture.nextInChain = null
                surfaceTexture.texture = null
                surfaceTexture.status = 0u
                wgpuSurfaceGetCurrentTexture(handle, surfaceTexture)
                when (surfaceTexture.status) {
                    WGPUSurfaceGetCurrentTextureStatus_SuccessOptimal,
                    WGPUSurfaceGetCurrentTextureStatus_SuccessSuboptimal -> {
                        val texture = surfaceTexture.texture
                            ?: throw IllegalStateException("surface texture is null despite success status")
                        BorrowedSurfaceTexture(bridge, texture)
                    }
                    WGPUSurfaceGetCurrentTextureStatus_Outdated,
                    WGPUSurfaceGetCurrentTextureStatus_Lost ->
                        throw SurfaceOutdatedException("surface status=${surfaceTexture.status}")
                    else -> throw IllegalStateException(
                        "wgpuSurfaceGetCurrentTexture failed (status=${surfaceTexture.status})"
                    )
                }
            }
        }
    }

    /**
     * Presents [texture] (acquired by [acquireFrame]). Use it in a `use` block
     * to release the reference even when encoding or presentation fails.
     */
    fun present(texture: BorrowedSurfaceTexture) {
        check(!closed) { "the surface is closed" }
        check(!texture.released) { "the surface texture is released" }
        bridge.call {
            val status = wgpuSurfacePresent(handle)
            if (status != WGPUStatus_Success) {
                throw IllegalStateException("wgpuSurfacePresent failed (status=$status)")
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        bridge.call {
            wgpuSurfaceUnconfigure(handle)
            wgpuSurfaceRelease(handle)
        }
    }
}

/** The surface must be reconfigured before the next frame (resize, mode change). */
class SurfaceOutdatedException(message: String) : IllegalStateException(message)

/**
 * A borrowed surface texture. [close] releases only the borrowed reference,
 * never destroys the texture owned by the surface.
 */
class BorrowedSurfaceTexture internal constructor(
    private val bridge: NativeBridge,
    internal val handle: WGPUTexture,
) : AutoCloseable {
    internal var released = false
        private set

    override fun close() = bridge.call {
        if (!released) {
            released = true
            wgpuTextureRelease(handle)
        }
    }
}
