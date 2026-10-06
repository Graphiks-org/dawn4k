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
import org.graphiks.dawn4k.native.WGPUTextureView
import org.graphiks.dawn4k.native.WGPUSType_SurfaceSourceMetalLayer
import org.graphiks.dawn4k.native.wgpuInstanceCreateSurface
import org.graphiks.dawn4k.native.wgpuSurfaceConfigure
import org.graphiks.dawn4k.native.wgpuSurfaceGetCurrentTexture
import org.graphiks.dawn4k.native.wgpuSurfacePresent
import org.graphiks.dawn4k.native.wgpuSurfaceRelease
import org.graphiks.dawn4k.native.wgpuSurfaceUnconfigure
import org.graphiks.dawn4k.native.wgpuTextureCreateView
import org.graphiks.dawn4k.native.wgpuTextureRelease
import org.graphiks.dawn4k.native.wgpuTextureViewRelease
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope
import org.graphiks.kffi.objc.NSView
import org.graphiks.kffi.objc.ObjCRuntime
import org.graphiks.kffi.objc.PlatformAvailability
import org.graphiks.webgpu.GPUIntegerCoordinateOut
import org.graphiks.webgpu.GPUSize32Out
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureDimension
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.GPUTextureViewDescriptor

/**
 * A Dawn surface over a macOS `CAMetalLayer`, living entirely inside the demo.
 *
 * The surface is created from the raw native handles exposed by `:dawn4k`'s
 * platform-integrator accessors ([NativeBridge], `DawnDevice.nativeHandle()`).
 * Every native call is routed through [bridge.call] so it runs on the backend's
 * worker thread — the only thread allowed to touch Dawn handles.
 *
 * The current texture is BORROWED from the surface: it is released after
 * [present], never destroyed. The wrappers ([BorrowedSurfaceTexture],
 * [BorrowedSurfaceTextureView]) implement the public `GPUTexture` /
 * `GPUTextureView` contracts without registering in the backend's resource
 * registry (the texture does not belong to a device session).
 */
@OptIn(PlatformAvailability::class)
class MetalSurface private constructor(
    private val bridge: NativeBridge,
    private val instance: WGPUInstance,
    private val device: WGPUDevice,
    private val handle: WGPUSurface,
    private val metalLayer: MemorySegment,
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
            val metalLayer = MemorySegment.ofAddress(metalLayerPtr)
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
                    MetalSurface(bridge, instance, device, surface, metalLayer)
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
         * Retrieves the `CAMetalLayer*` of a Compose/Skia [nsViewPtr] (an `NSView*`
         * as a Long). Forces layer creation with `setWantsLayer(true)`, reads
         * `layer`, and verifies it is a `CAMetalLayer`. If the view's layer is a
         * plain `CALayer`, a fresh `CAMetalLayer` is created and installed with
         * `setLayer:`.
         *
         * Returns the `CAMetalLayer*` as a Long, or throws.
         */
        fun metalLayerOf(nsViewPtr: Long): Long = ObjCRuntime.autoreleasePool {
            require(nsViewPtr != 0L) { "the NSView pointer is null" }
            val view = NSView(MemorySegment.ofAddress(nsViewPtr))
            view.setWantsLayer(true)
            val layer = view.layer()
            require(layer != MemorySegment.NULL) { "setWantsLayer(true) did not create a layer" }
            val layerClass = ObjCRuntime.msgSend(
                ValueLayout.ADDRESS, layer, ObjCRuntime.sel("class")
            ) as MemorySegment
            val metalLayerClass = ObjCRuntime.getClass("CAMetalLayer")
            if (layerClass == metalLayerClass) {
                layer.address()
            } else {
                // Replace the plain CALayer with a CAMetalLayer.
                val metalLayer = createCaMetalLayer()
                view.setLayer(metalLayer)
                metalLayer.address()
            }
        }
    }

    /** The `CAMetalLayer*` this surface renders into, as a Long. */
    val metalLayerPtr: Long get() = metalLayer.address()

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
     * Acquires the current surface texture as a borrowed [GPUTexture].
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
                        BorrowedSurfaceTexture(texture, configuredWidth.toUInt(), configuredHeight.toUInt())
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
     * Presents [texture] (acquired by [acquireFrame]) and releases the borrowed
     * reference. The texture must not be used after this call.
     */
    fun present(texture: BorrowedSurfaceTexture) {
        check(!closed) { "the surface is closed" }
        bridge.call {
            val status = wgpuSurfacePresent(handle)
            if (status != WGPUStatus_Success) {
                throw IllegalStateException("wgpuSurfacePresent failed (status=$status)")
            }
            // Release the borrowed texture reference — NEVER destroy: the
            // texture belongs to the surface.
            wgpuTextureRelease(texture.handle)
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
 * A borrowed surface texture: implements [GPUTexture] without registering in
 * the backend's resource registry. [close] is a no-op — the texture belongs to
 * the surface and is released by [MetalSurface.present].
 */
class BorrowedSurfaceTexture internal constructor(
    internal val handle: WGPUTexture,
    override val width: GPUIntegerCoordinateOut,
    override val height: GPUIntegerCoordinateOut,
) : GPUTexture {

    override val depthOrArrayLayers: GPUIntegerCoordinateOut = 1u
    override val mipLevelCount: GPUIntegerCoordinateOut = 1u
    override val sampleCount: GPUSize32Out = 1u
    override val dimension: GPUTextureDimension = GPUTextureDimension.TwoD
    override val format: GPUTextureFormat = GPUTextureFormat.BGRA8Unorm
    override val usage: Set<GPUTextureUsage> = setOf(GPUTextureUsage.RenderAttachment)
    override var label: String = "surface"

    override fun createView(descriptor: GPUTextureViewDescriptor?): GPUTextureView {
        // A null descriptor adopts the C defaults (whole texture, all aspects).
        val viewHandle = wgpuTextureCreateView(handle, null)
            ?: throw IllegalStateException("wgpuTextureCreateView returned no view")
        return BorrowedSurfaceTextureView(viewHandle)
    }

    override fun close() {
        // No-op: the texture is owned by the surface and released by present().
    }
}

/**
 * A view over a borrowed surface texture. [close] releases the view reference
 * (the view is a fresh reference created by `wgpuTextureCreateView`).
 */
class BorrowedSurfaceTextureView internal constructor(
    internal val handle: WGPUTextureView,
) : GPUTextureView {

    override var label: String = "surface-view"

    override fun close() {
        wgpuTextureViewRelease(handle)
    }
}
