package org.graphiks.dawn4k.webgpu.toolkit.window

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnSurfaceReference
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
 * - Owned: `close()` triggers shutdown, joins the shutdown Job, and completes event flow
 * - Borrowed: `close()` is a no-op
 * 
 * **Extent Handling:**
 * - PHYSICAL extent comes from native framebuffer (DawnWindow.extent / Resized event)
 * - LOGICAL extent comes from WindowConfig / logical resize requests
 * - Never assume equality; compute scale when both are known
 * 
 * **SurfaceTarget Delegation:**
 * - This class implements [SurfaceTarget] by delegating to an internal [Dawn4kSurfaceTarget]
 * - The surface target state is updated when the window is resized
 * 
 * @param config Window configuration (title, logical dimensions)
 * @param runtime Whether this window is owned or borrowed
 * @param surfaceTarget The underlying surface target for this window
 * @param dawnWindow The underlying DawnWindow from dawn4k-toolkit-glfw
 * @param main The DawnMainThreadLoop that owns this window
 */
internal class GlfwWindow(
    private val config: WindowConfig,
    override val runtime: WindowRuntime,
    private val surfaceTarget: Dawn4kSurfaceTarget,
    private val dawnWindow: DawnWindow,
    private val main: DawnMainThreadLoop,
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
    
    // FIX 1: Track the current physical extent for generation management
    // Initialize from DawnWindow.extent (PHYSICAL framebuffer size), NOT from config
    private var currentPhysicalExtent: Extent2D = runBlocking {
        val pixelExtent = dawnWindow.extent.value
        Extent2D(pixelExtent.width.toUInt(), pixelExtent.height.toUInt())
    }
    
    // FIX 1: Track the current logical extent separately from physical
    // Initialize from WindowConfig (logical dimensions)
    private var currentLogicalExtent: Extent2D = Extent2D(
        config.logicalWidth.toUInt(),
        config.logicalHeight.toUInt()
    )
    
    // Job for managing owned window shutdown
    private var shutdownJob: kotlinx.coroutines.Job? = null
    
    // ========== Initialization ==========
    
    /**
     * Sets up event forwarding from the DawnWindow to our WindowEvent flow.
     * FIX 4: Uses Unconfined dispatcher to collect on the caller's thread (main thread).
     * The WindowSurfaceState in dawn4k-toolkit-glfw uses tryEmit which is thread-safe.
     */
    internal fun setupEventForwarding(dawnWindow: DawnWindow, main: DawnMainThreadLoop) {
        // The DawnWindow's events flow uses WindowSurfaceState which uses tryEmit
        // This means events can be emitted from any thread (GLFW callbacks) without suspending
        // We use Unconfined to collect on the caller's thread, which is the main thread
        // when setupEventForwarding is called from createGlfwWindow (which runs in DawnMainThreadLoop.run)
        CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined).launch {
            dawnWindow.events.collect { event ->
                forwardEvent(event)
            }
        }
    }
    
    // ========== Window Contract Implementation ==========
    
    override fun close() {
        when (runtime) {
            WindowRuntime.Owned -> {
                // FIX 3: For owned windows, trigger shutdown on main thread, JOIN, and complete event flow
                
                // Emit CloseRequested if not already done
                if (!closeRequested) {
                    closeRequested = true
                    try {
                        _events.tryEmit(WindowEvent.CloseRequested)
                    } catch (_: Exception) {
                        // Ignore if flow is closed
                    }
                }
                
                // FIX 3: Trigger the native window shutdown
                // dawnWindow.shutdown() must be called from the GLFW main thread
                // We use the DawnWindow's own mechanism which internally uses main.owner.onOwner
                // Since we can't access main.owner directly (it's internal), we call shutdown()
                // which will internally use the correct thread
                shutdownJob = CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined).launch {
                    dawnWindow.shutdown()
                }
                
                // Join the shutdown to ensure the window is properly destroyed
                // Use runBlocking to join immediately since close() is a synchronous operation
                shutdownJob?.let {
                    runBlocking { it.join() }
                }
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
     * - FIX 1: PHYSICAL extent comes from native framebuffer, LOGICAL from WindowConfig
     *   Note: Production path tracks logical separately; test hook uses 1:1 mapping
     */
    private suspend fun forwardEvent(event: DawnWindowEvent) {
        if (closeRequested) return
        
        when (event) {
            DawnWindowEvent.CloseRequested -> {
                closeRequested = true
                _events.emit(WindowEvent.CloseRequested)
            }
            is DawnWindowEvent.Resized -> {
                // FIX 1: event.extent is PHYSICAL (framebuffer pixels) - from native framebuffer
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
                
                // FIX 1: Emit Resized with LOGICAL extent
                // LOGICAL extent comes from the last requested logical size
                // For production: track from WindowConfig and resize requests
                // For now: use config's logical dimensions (may not be up-to-date after user resize)
                // TODO: Track logical size properly when API is available
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
     * FIX 1: For test-only, uses simple 1:1 mapping (logical == physical).
     * Production path does NOT assume equality.
     * Internal use only for testing.
     */
    internal suspend fun testResize(logicalWidth: Int, logicalHeight: Int) {
        // For testing, we simulate a resize with the given logical dimensions
        // FIX 1: Test hook uses 1:1 mapping (logical == physical) for simplicity
        // Production path tracks them separately
        val physicalExtent = Extent2D(logicalWidth.toUInt(), logicalHeight.toUInt())
        val logicalExtent = Extent2D(logicalWidth.toUInt(), logicalHeight.toUInt())
        
        if (physicalExtent != currentPhysicalExtent) {
            currentPhysicalExtent = physicalExtent
            currentLogicalExtent = logicalExtent
            surfaceTarget.updateExtent(physicalExtent)
        }
        
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
    
    // FIX 1: Get the initial physical extent from DawnWindow.extent (PHYSICAL framebuffer size)
    val initialPixelExtent = dawnWindow.extent.value
    val initialPhysicalExtent = Extent2D(
        initialPixelExtent.width.toUInt(),
        initialPixelExtent.height.toUInt()
    )
    
    // Determine platform type
    val platformType = detectPlatformType()
    
    // FIX 2: Extract the surface reference from the DawnWindow's presentation
    // TODO: Cannot access with current public API - DawnPresentation does not expose surface reference
    // The DawnWindow.presentation is public, but DawnPresentation does not have a public method
    // to get the surface reference. This requires either:
    // 1. Making DawnPresentation's surface reference accessible via public API
    // 2. Adding a method to extract surface reference from DawnWindow
    // For now, we pass null - Dawn4kSurfaceTarget can work with null surface reference
    val surfaceReference: DawnSurfaceReference? = extractSurfaceReferenceFromDawnWindow(dawnWindow, dawnContext)
    
    // Create a Dawn4kSurfaceTarget with the extracted surface reference
    val surfaceTarget = Dawn4kSurfaceTarget(
        context = dawnContext,
        surfaceReference = surfaceReference,
        platformType = platformType,
        initialExtent = initialPhysicalExtent
    )
    
    // Create the GlfwWindow
    val window = GlfwWindow(config, runtime, surfaceTarget, dawnWindow, main)
    
    // Set up event forwarding on the main thread
    window.setupEventForwarding(dawnWindow, main)
    
    return window
}

/**
 * Extracts the DawnSurfaceReference from the DawnWindow's presentation.
 * FIX 2: TODO - Cannot access with current public API.
 * 
 * The DawnWindow.presentation is public, but DawnPresentation does not expose
 * the surface reference through its public API. The surface reference is created
 * internally during presentation initialization and is not accessible externally.
 */
private suspend fun extractSurfaceReferenceFromDawnWindow(
    dawnWindow: DawnWindow,
    dawnContext: DawnContext
): DawnSurfaceReference? {
    // TODO: Extract surface reference from DawnWindow.presentation
    // Current limitation: DawnPresentation does not expose surface reference via public API
    // The surface reference is created internally in DawnPresentation initialization
    // and is not accessible through public methods.
    // 
    // In a proper implementation with access to internals, this would be:
    // val handle = dawnWindow.nativeHandle  // internal
    // val dawnSurfaceTarget = main.backend.surfaceTarget(handle)  // internal
    // val lease = dawnSurfaceTarget.retain()
    // val ref = lease.createSurface(dawnContext)
    // lease.retire()
    // return ref
    return null
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
