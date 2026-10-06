package org.graphiks.dawn4k.demo

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import org.graphiks.dawn4k.NativeBridge
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
import org.graphiks.kffi.memoryScope
import org.graphiks.kffi.objc.NSView
import org.graphiks.kffi.objc.CALayer
import org.graphiks.kffi.objc.CAAutoresizingMask
import org.graphiks.kffi.objc.ObjCRuntime
import org.graphiks.kffi.objc.PlatformAvailability

/**
 * A Dawn surface over a macOS `CAMetalLayer`, living entirely inside the demo.
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
@OptIn(PlatformAvailability::class)
class MetalSurface private constructor(
    private val bridge: NativeBridge,
    private val device: WGPUDevice,
    private val handle: WGPUSurface,
) : AutoCloseable {

    private var configuredWidth: Int = 0
    private var configuredHeight: Int = 0
    private var closed = false

    /**
     * Creates a surface over [metalLayerPtr] (a `CAMetalLayer*` as a Long).
     * The layer must outlive the surface.
     */
    companion object {
        /** webgpu.h `WGPU_STRLEN` (SIZE_MAX): the NUL-terminated length sentinel. */
        private const val WGPU_STRLEN: ULong = ULong.MAX_VALUE

        fun create(
            bridge: NativeBridge,
            deviceHandle: Long,
            metalLayerPtr: Long,
        ): MetalSurface {
            val instanceHandle = bridge.instanceHandle()
            require(instanceHandle != 0L) { "the context is closed: no instance handle" }
            require(deviceHandle != 0L) { "the device is closed: no device handle" }
            require(metalLayerPtr != 0L) { "the metal layer pointer is null" }
            return bridge.call {
                memoryScope { allocator ->
                    val instance = WGPUInstance(NativeAddress(instanceHandle))
                    val device = WGPUDevice(NativeAddress(deviceHandle))

                    // WGPUSurfaceSourceMetalLayer (sType = SurfaceSourceMetalLayer = 4)
                    val metalSource = WGPUSurfaceSourceMetalLayer.allocate(allocator)
                    metalSource.chain.next = null
                    metalSource.chain.sType = WGPUSType_SurfaceSourceMetalLayer
                    metalSource.layer = NativeAddress(metalLayerPtr)

                    val descriptor = WGPUSurfaceDescriptor.allocate(allocator)
                    descriptor.nextInChain = metalSource.chain
                    descriptor.label.data = null
                    descriptor.label.length = WGPU_STRLEN

                    val surface = wgpuInstanceCreateSurface(instance, descriptor)
                        ?: throw IllegalStateException("wgpuInstanceCreateSurface returned no surface")
                    MetalSurface(bridge, device, surface)
                }
            }
        }

        /**
         * Creates a fresh `CAMetalLayer` via the ObjC runtime (`alloc` + `init`).
         * Returns the layer as a [MemorySegment]; the caller owns the reference.
         *
         * Note: `setDrawableSize:` takes a `CGSize` by value and requires
         * `ObjCRuntime.ObjCStructArg` (internal to kffi-objc, inaccessible
         * cross-module). The layer inherits its size from its parent `NSView`;
         * Dawn adjusts the drawable via `wgpuSurfaceConfigure`.
         */
        fun createCaMetalLayer(): MemorySegment {
            val layerClass = ObjCRuntime.getClass("CAMetalLayer")
            val allocated = ObjCRuntime.msgSend(
                ValueLayout.ADDRESS, layerClass, ObjCRuntime.sel("alloc")
            ) as MemorySegment
            require(allocated != MemorySegment.NULL) { "CAMetalLayer alloc failed" }
            val initialized = ObjCRuntime.msgSend(
                ValueLayout.ADDRESS, allocated, ObjCRuntime.sel("init")
            ) as MemorySegment
            require(initialized != MemorySegment.NULL) { "CAMetalLayer init failed" }
            return initialized
        }

        /**
         * Installs a dedicated Metal layer below the transparent Compose/Skia children.
         * Never replace the backing layer: Compose still owns it.
         * Call on the AppKit thread. The caller must detach and release the layer.
         */
        fun metalLayerOf(nsViewPtr: Long): Long = ObjCRuntime.autoreleasePool {
            require(nsViewPtr != 0L) { "the NSView pointer is null" }
            val view = NSView(MemorySegment.ofAddress(nsViewPtr))
            view.setWantsLayer(true)
            val layer = view.layer()
            require(layer != MemorySegment.NULL) { "setWantsLayer(true) did not create a layer" }
            val metalLayer = createCaMetalLayer()
            val overlay = CALayer(metalLayer)
            overlay.setFrame(view.bounds())
            overlay.setAutoresizingMask(
                CAAutoresizingMask.kCALayerWidthSizable + CAAutoresizingMask.kCALayerHeightSizable
            )
            overlay.setZPosition(-1.0)
            CALayer(layer).addSublayer(metalLayer)
            metalLayer.address()
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
                config.format = WGPUTextureFormat_BGRA8Unorm
                config.usage = WGPUTextureUsage_RenderAttachment
                config.width = width.toUInt()
                config.height = height.toUInt()
                config.viewFormatCount = 0uL
                config.viewFormats = null
                config.alphaMode = WGPUCompositeAlphaMode_Auto
                config.presentMode = WGPUPresentMode_Fifo
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
