package org.graphiks.dawn4k.webgpu.toolkit.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kSurfaceTarget
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureView
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.AlphaMode
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import org.graphiks.webgpu.toolkit.SurfaceCapabilities
import org.graphiks.webgpu.toolkit.ToolkitSurfaceHandle

/**
 * Dawn4k implementation of [ToolkitSurfaceHandle] for native platform surfaces.
 * 
 * This handle manages the lifecycle of a surface bound to a device, including
 * configuration, frame acquisition, presentation, and frame release.
 *
 * **Known limitation (unimplemented):** frame acquisition and presentation are
 * currently backed by a mock texture view — the real native present wiring
 * (`wgpuSurfaceGetCurrentTexture` / `wgpuSurfacePresent` against an adopted
 * native surface reference) lands in a later snapshot. The related TODOs in
 * [acquireFrame] / [present] are marked known-unimplemented.
 *
 * @param surfaceTarget The native surface target to present to
 */
class Dawn4kSurfaceHandle(
    private val surfaceTarget: Dawn4kSurfaceTarget,
) : ToolkitSurfaceHandle {
    
    private val mutex = Mutex()
    private var isConfigured = false
    private var isClosed = false
    
    // Configuration state
    private var configuredDevice: GPUDevice? = null
    private var configuredFormat: GPUTextureFormat? = null
    private var configuredAlphaMode: AlphaMode = AlphaMode.Opaque
    
    // Frame tracking
    private var currentFrame: AcquiredFrame? = null
    private var framePresented = false
    
    // Capabilities (computed lazily)
    private var _capabilities: SurfaceCapabilities? = null
    
    override val capabilities: SurfaceCapabilities
        get() {
            if (_capabilities == null) {
                throw IllegalStateException("Surface capabilities not yet computed")
            }
            return _capabilities!!
        }
    
    init {
        // Compute capabilities based on the surface and adapter
        // For dawn4k, we'll use a reasonable default set of capabilities
        _capabilities = SurfaceCapabilities(
            preferredFormat = GPUTextureFormat.BGRA8Unorm,
            formats = listOf(
                GPUTextureFormat.BGRA8Unorm,
                GPUTextureFormat.RGBA8Unorm,
                GPUTextureFormat.RGBA16Float,
            ),
            alphaModes = listOf(
                AlphaMode.Opaque,
                AlphaMode.Premultiplied,
            )
        )
    }
    
    override fun configure(configuration: PresentationConfiguration) {
        check(!isClosed) { "Cannot configure a closed surface handle" }
        check(!isConfigured) { "Surface is already configured" }
        
        // Validate configuration
        require(configuration.format in capabilities.formats) {
            "Format ${configuration.format} is not supported by this surface"
        }
        require(configuration.alphaMode in capabilities.alphaModes) {
            "Alpha mode ${configuration.alphaMode} is not supported by this surface"
        }
        
        configuredDevice = configuration.device
        configuredFormat = configuration.format
        configuredAlphaMode = configuration.alphaMode
        isConfigured = true
    }
    
    override suspend fun acquireFrame(): AcquiredFrame = mutex.withLock {
        check(!isClosed) { "Cannot acquire frame from a closed surface handle" }
        check(isConfigured) { "Surface must be configured before acquiring frames" }
        check(currentFrame == null) { "Previous frame must be released before acquiring a new one" }
        
        // Check that the surface target is available and has valid extent
        val state = surfaceTarget.state.value
        check(state.available) { "Surface target is not available" }
        check(state.extent.width > 0u && state.extent.height > 0u) {
            "Surface extent is zero, cannot acquire frame"
        }
        
        // Check generation - if stale, fail fast
        // KNOWN UNIMPLEMENTED: in the real native present wiring this would acquire
        // the current texture from the native surface; for now we create a mock frame
        val textureView = createMockTextureView(state.extent)
        
        currentFrame = AcquiredFrame(
            view = textureView,
            extent = state.extent
        )
        framePresented = false
        
        return currentFrame!!
    }
    
    override fun present(frame: AcquiredFrame) {
        check(!isClosed) { "Cannot present on a closed surface handle" }
        check(isConfigured) { "Surface must be configured before presenting" }
        check(frame === currentFrame) { "Can only present the currently acquired frame" }
        check(!framePresented) { "Frame has already been presented" }
        
        // KNOWN UNIMPLEMENTED: the real native present wiring would call
        // wgpuSurfacePresent here; for now we just mark it as presented
        framePresented = true
    }
    
    override fun releaseFrame(frame: AcquiredFrame) {
        // Idempotent: no-op if frame is not the current one or already released
        if (frame !== currentFrame) {
            return
        }
        
        // Release the frame resources
        // In a real implementation, this would release the texture
        if (frame.view is MockTextureView) {
            (frame.view as MockTextureView).close()
        }
        
        currentFrame = null
        framePresented = false
    }
    
    override fun close() {
        if (isClosed) return
        
        // Release any current frame
        currentFrame?.let { frame ->
            releaseFrame(frame)
        }
        
        isClosed = true
    }
    
    /**
     * Mock texture view for testing purposes.
     * KNOWN UNIMPLEMENTED: in the real native present wiring (later snapshot) this
     * would be a real GPUTextureView from Dawn's surface.
     */
    private fun createMockTextureView(extent: org.graphiks.webgpu.toolkit.Extent2D): GPUTextureView {
        return MockTextureView(extent)
    }

    /**
     * Mock implementation of GPUTextureView for testing.
     * KNOWN UNIMPLEMENTED until the real native present wiring lands.
     */
    private class MockTextureView(val extent: org.graphiks.webgpu.toolkit.Extent2D) : GPUTextureView {
        private var isClosed = false
        
        override var label: String = "mock-texture-view"
        
        override fun close() {
            isClosed = true
        }
    }
}
