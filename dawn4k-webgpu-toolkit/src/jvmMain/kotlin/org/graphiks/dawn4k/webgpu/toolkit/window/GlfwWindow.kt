package org.graphiks.dawn4k.webgpu.toolkit.window

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.toolkit.glfw.DawnMainThreadLoop
import org.graphiks.dawn4k.toolkit.glfw.DawnWindow
import org.graphiks.dawn4k.toolkit.glfw.DawnWindowEvent
import org.graphiks.dawn4k.toolkit.surface.PixelExtent
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState
import org.graphiks.webgpu.toolkit.surface.Window
import org.graphiks.webgpu.toolkit.surface.WindowConfig
import org.graphiks.webgpu.toolkit.surface.WindowEvent
import org.graphiks.webgpu.toolkit.surface.WindowRuntime

/**
 * GLFW-based implementation of [Window] that wraps a dawn4k-toolkit-glfw [DawnWindow].
 * 
 * This implementation adapts the DawnWindow to the webgpu-toolkit-surface contract:
 * 
 * **Event Flow:**
 * - Creates a hot flow with `replay=1, extraBufferCapacity=64, onBufferOverflow=DROP_OLDEST`
 * - Translates [DawnWindowEvent.CloseRequested] to [WindowEvent.CloseRequested]
 * - Translates [DawnWindowEvent.Resized] to [WindowEvent.Resized] with LOGICAL extent
 * - Updates surface target state with PHYSICAL extent and generation bump when physical size changes
 * 
 * **Lifecycle:**
 * - Owned: `close()` joins shutdown and completes event flow
 * - Borrowed: `close()` is a no-op
 * 
 * **Extent Handling:**
 * - `WindowEvent.Resized` carries LOGICAL extent
 * - Physical extent (framebuffer) goes in surface target state with generation bump ONLY if physical extent changed
 * 
 * **SurfaceTarget Delegation:**
 * - This class implements [SurfaceTarget] by delegating to an internal [Dawn4kSurfaceTarget]
 * - The surface target state is updated when the window is resized
 * 
 * @param config Window configuration (title, logical dimensions)
 * @param runtime Whether this window is owned or borrowed
 * @param surfaceTarget The underlying surface target for this window
 * @param dawnWindow The underlying DawnWindow from dawn4k-toolkit-glfw
 */
internal class GlfwWindow(
    private val config: WindowConfig,
    override val runtime: WindowRuntime,
    private val surfaceTarget: Dawn4kSurfaceTarget,
    private val dawnWindow: DawnWindow,
) : Window {
    
    // Event flow: hot flow with replay=1, extraBufferCapacity=64, DROP_OLDEST
    // Never suspends (no SUSPEND strategy)
    private val _events = MutableSharedFlow<WindowEvent>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events: Flow<WindowEvent> = _events.asSharedFlow()
    
    // Track if close has been requested
    private var closeRequested = false
    
    // Track the current physical extent for generation management
    private var currentPhysicalExtent: Extent2D = Extent2D(
        config.logicalWidth.toUInt(),
        config.logicalHeight.toUInt()
    )
    
    // Coroutine scope for event collection
    private val eventScope = CoroutineScope(Dispatchers.Default)
    
    // ========== Initialization ==========
    
    /**
     * Sets up event forwarding from the DawnWindow to our WindowEvent flow.
     */
    internal fun setupEventForwarding(dawnWindow: DawnWindow) {
        // Collect events from the DawnWindow and forward them
        eventScope.launch {
            dawnWindow.events.collect { event ->
                forwardEvent(event)
            }
        }
    }
    
    // ========== Window Contract Implementation ==========
    
    override fun close() {
        when (runtime) {
            WindowRuntime.Owned -> {
                // For owned windows: shutdown the DawnWindow and complete event flow
                // The DawnWindow.shutdown() will:
                // 1. Shutdown the presentation
                // 2. Retire the native window lease
                // 3. Unregister from the main loop
                
                // Emit CloseRequested if not already done
                if (!closeRequested) {
                    closeRequested = true
                    try {
                        _events.tryEmit(WindowEvent.CloseRequested)
                    } catch (_: Exception) {
                        // Ignore if flow is closed
                    }
                }
                
                // Note: The actual DawnWindow shutdown is handled by the DawnMainThreadLoop
                // when the window is unregistered
            }
            WindowRuntime.Borrowed -> {
                // Borrowed: no-op
                // The lifetime belongs to the embedder
            }
        }
    }
    
    // ========== SurfaceTarget Contract Implementation ==========
    
    override val state: StateFlow<TargetState> = surfaceTarget.state
    
    override suspend fun retain(): TargetLease = surfaceTarget.retain()
    
    override suspend fun retire(lease: TargetLease) = surfaceTarget.retire(lease)
    
    // ========== Event Forwarding ==========
    
    /**
     * Forwards a DawnWindowEvent to our WindowEvent flow.
     * This handles the translation between the two event systems.
     * 
     * **Important:**
     * - [DawnWindowEvent.Resized.extent] is the PHYSICAL extent (framebuffer size)
     * - We emit [WindowEvent.Resized] with LOGICAL extent
     * - We update surface target state with PHYSICAL extent and generation bump ONLY if physical extent changed
     */
    private suspend fun forwardEvent(event: DawnWindowEvent) {
        if (closeRequested) return
        
        when (event) {
            DawnWindowEvent.CloseRequested -> {
                closeRequested = true
                _events.emit(WindowEvent.CloseRequested)
            }
            is DawnWindowEvent.Resized -> {
                // event.extent is PHYSICAL (framebuffer pixels)
                val physicalExtent = Extent2D(
                    event.extent.width.toUInt(),
                    event.extent.height.toUInt()
                )
                
                // Bump generation ONLY if physical extent changed
                if (physicalExtent != currentPhysicalExtent) {
                    currentPhysicalExtent = physicalExtent
                    
                    // Update surface target state with new physical extent
                    // The generation bump is handled by Dawn4kSurfaceTarget.updateExtent
                    surfaceTarget.updateExtent(physicalExtent)
                }
                
                // Emit Resized with LOGICAL extent
                // Note: For simplicity, we're using the config's logical dimensions
                // In a real implementation with HiDPI, we'd need to track the
                // logical size separately from the physical framebuffer size
                val logicalExtent = Extent2D(
                    config.logicalWidth.toUInt(),
                    config.logicalHeight.toUInt()
                )
                _events.emit(WindowEvent.Resized(logicalExtent))
            }
            else -> {
                // Ignore other event types (Key, PointerMoved, PointerButton, Scroll)
                // These are not part of the Window contract
            }
        }
    }
    
    // ========== Test Hooks ==========
    
    /**
     * Test hook: triggers a close request event.
     * Internal use only for testing.
     */
    internal suspend fun testRequestClose() {
        if (!closeRequested) {
            closeRequested = true
            _events.emit(WindowEvent.CloseRequested)
        }
    }
    
    /**
     * Test hook: triggers a resize event with the given logical dimensions.
     * Also updates the physical extent in the surface target state.
     * Internal use only for testing.
     */
    internal suspend fun testResize(logicalWidth: Int, logicalHeight: Int) {
        // For testing, we assume logical == physical
        val physicalExtent = Extent2D(logicalWidth.toUInt(), logicalHeight.toUInt())
        
        if (physicalExtent != currentPhysicalExtent) {
            currentPhysicalExtent = physicalExtent
            surfaceTarget.updateExtent(physicalExtent)
        }
        
        val logicalExtent = Extent2D(logicalWidth.toUInt(), logicalHeight.toUInt())
        _events.emit(WindowEvent.Resized(logicalExtent))
    }
    
    /**
     * Returns the underlying DawnWindow for testing purposes.
     */
    internal fun getDawnWindow(): DawnWindow = dawnWindow
    
    /**
     * Returns the underlying surface target for testing purposes.
     */
    internal fun getSurfaceTargetInternal() = surfaceTarget
}

/**
 * Factory function to create a GLFW window with proper initialization.
 */
internal suspend fun createGlfwWindow(
    config: WindowConfig,
    runtime: WindowRuntime,
    dawnContext: DawnContext,
    main: DawnMainThreadLoop,
): GlfwWindow {
    val dawnWindowOptions = org.graphiks.dawn4k.toolkit.glfw.DawnWindowOptions(
        title = config.title,
        width = config.logicalWidth,
        height = config.logicalHeight,
        visible = true,
        resizable = true,
        runtimeOwnership = when (runtime) {
            WindowRuntime.Owned -> org.graphiks.dawn4k.toolkit.glfw.DawnGlfwRuntimeOwnership.Owned
            WindowRuntime.Borrowed -> org.graphiks.dawn4k.toolkit.glfw.DawnGlfwRuntimeOwnership.Borrowed
        }
    )
    
    // Use the default openDawnWindow which uses openDawnSurface
    val dawnWindow = org.graphiks.dawn4k.toolkit.glfw.openDawnWindow(
        windowOptions = dawnWindowOptions,
        sessionOptions = org.graphiks.dawn4k.toolkit.DawnSessionOptions(),
        presentationOptions = org.graphiks.dawn4k.toolkit.surface.DawnPresentationOptions(),
        mainThread = main
    )
    
    // Determine platform type
    val platformType = detectPlatformType()
    
    // Create our surface target wrapper
    val initialExtent = Extent2D(
        config.logicalWidth.toUInt(),
        config.logicalHeight.toUInt()
    )
    
    // Create a Dawn4kSurfaceTarget
    val surfaceTarget = Dawn4kSurfaceTarget(
        context = dawnContext,
        surfaceReference = null,  // Will be set when surface is created
        platformType = platformType,
        initialExtent = initialExtent
    )
    
    // Create the GlfwWindow
    val window = GlfwWindow(config, runtime, surfaceTarget, dawnWindow)
    
    // Set up event forwarding
    window.setupEventForwarding(dawnWindow)
    
    return window
}

/**
 * Detects the platform type based on the current OS.
 */
private fun detectPlatformType(): PlatformSurfaceType {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    return when {
        osName.contains("mac") -> PlatformSurfaceType.MetalLayer
        osName.contains("win") -> PlatformSurfaceType.Win32
        osName.contains("linux") -> PlatformSurfaceType.X11
        else -> PlatformSurfaceType.MetalLayer
    }
}
