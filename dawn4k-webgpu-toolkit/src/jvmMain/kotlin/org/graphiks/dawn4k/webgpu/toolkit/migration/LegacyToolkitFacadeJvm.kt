package org.graphiks.dawn4k.webgpu.toolkit.migration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.graphiks.dawn4k.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.toolkit.glfw.DawnWindowEvent
import org.graphiks.dawn4k.toolkit.glfw.DawnWindowOptions
import org.graphiks.dawn4k.toolkit.surface.DawnPresentation
import org.graphiks.dawn4k.toolkit.surface.DawnPresentationOptions
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceConfiguration
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.dawn4k.webgpu.toolkit.window.GlfwWindowHost
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.surface.Window
import org.graphiks.webgpu.toolkit.surface.WindowConfig
import org.graphiks.webgpu.toolkit.surface.WindowHost
import org.graphiks.webgpu.toolkit.withSession

/**
 * JVM-specific migration facade extensions for window-related functions.
 * 
 * This provides the `withDawnWindow` function that requires GLFW (JVM-only).
 */
public object LegacyToolkitFacadeJvm {
    
    private val extension: Dawn4kWebGpuToolkitExtension = Dawn4kWebGpuToolkitExtension()
    
    /**
     * Allows injection of a custom WindowHost for testing.
     * Defaults to GlfwWindowHost for production use.
     */
    public var windowHost: WindowHost = GlfwWindowHost()
    
    /**
     * Opens a Dawn window using the new WebGPU toolkit and executes the given block.
     * 
     * This is a migration facade that delegates to the new toolkit's window creation.
     * The window, session, and presentation are managed by the new toolkit and are NOT
     * closed by this facade.
     * 
     * **Important:** The facade NEVER closes borrowed handles (device/queue/context) — the session
     * owns them.
     * 
     * @param windowOptions Window configuration (mapped to new WindowConfig)
     * @param sessionOptions Session configuration (mapped to new SessionConfig)
     * @param presentationOptions Presentation configuration (currently not used in new API)
     * @param block The code to execute with the session, window, and presentation
     * @return The result of the block
     */
    public suspend fun <T> withDawnWindow(
        windowOptions: DawnWindowOptions = DawnWindowOptions(),
        sessionOptions: DawnSessionOptions = DawnSessionOptions(),
        presentationOptions: DawnPresentationOptions = DawnPresentationOptions(),
        block: suspend CoroutineScope.(LegacyDawnSession, LegacyDawnWindow, LegacyDawnPresentation) -> T,
    ): T {
        // Map configurations
        val sessionConfig = sessionOptions.toSessionConfig()
        val windowConfig = windowOptions.toWindowConfig()
        
        // Delegate to the new toolkit's withSession first
        return withSession(
            extension = extension,
            config = sessionConfig,
            hints = AdapterHints(),
        ) { newSession ->
            // Open a window using the injectable host
            val newWindow = windowHost.open(windowConfig)
            
            try {
                // Map the new types to legacy-compatible wrappers
                val legacySession = LegacyDawnSession(newSession)
                val legacyWindow = LegacyDawnWindow(newWindow, newSession)
                val legacyPresentation = LegacyDawnPresentation(newWindow, newSession)
                
                // Execute the block with a CoroutineScope receiver
                coroutineScope {
                    block(legacySession, legacyWindow, legacyPresentation)
                }
            } finally {
                // Note: We do NOT close the window or session here
                // The new toolkit's withSession manages the lifecycle
                // The facade never closes borrowed handles
            }
        }
    }
}

/**
 * Maps old DawnSessionOptions to new SessionConfig.
 */
private fun DawnSessionOptions.toSessionConfig(): SessionConfig {
    return SessionConfig(
        // TODO: Map relevant options from DawnSessionOptions to SessionConfig
    )
}

/**
 * Maps old DawnWindowOptions to new WindowConfig.
 */
private fun DawnWindowOptions.toWindowConfig(): WindowConfig {
    return WindowConfig(
        title = title,
        logicalWidth = width,
        logicalHeight = height,
    )
}

/**
 * Legacy-compatible window wrapper that provides the same API as DawnWindow.
 * 
 * This wrapper delegates all calls to the underlying new Window and Session.
 * It NEVER closes any handles - the underlying session owns them.
 * 
 * **Important:** This is NOT the actual DawnWindow class from dawn4k-toolkit-glfw.
 * It provides a compatible API surface for migration purposes.
 */
public class LegacyDawnWindow(
    private val newWindow: Window,
    private val newSession: Session,
) {
    /**
     * The GPU session associated with this window.
     */
    public val gpu: LegacyDawnSession = LegacyDawnSession(newSession)
    
    /**
     * The presentation associated with this window.
     */
    public val presentation: LegacyDawnPresentation = LegacyDawnPresentation(newWindow, newSession)
    
    /**
     * Events from the window.
     * Returns an empty flow since the new Window may not expose events directly.
     */
    public val events: SharedFlow<DawnWindowEvent> = MutableSharedFlow()
    
    /**
     * The native window handle.
     * Returns 0L as it's not available from the new Window.
     */
    public val nativeHandle: Long = 0L
    
    /**
     * Requests a resize of the window.
     * No-op since the new Window may not support this directly.
     */
    public suspend fun resize(width: Int, height: Int) {
        // No-op: window resize is not supported through the new toolkit's Window
    }
    
    /**
     * Requests the window to close.
     * No-op since the new Window may not support this directly.
     */
    public suspend fun requestClose() {
        // No-op: window close request is not supported through the new toolkit's Window
    }
    
    /**
     * Shutdown is a no-op because the new toolkit's withSession handles all lifecycle management.
     * The facade NEVER closes borrowed handles.
     */
    public suspend fun shutdown() {
        // No-op: the new toolkit's withSession handles cleanup automatically
        // The facade never closes borrowed handles
    }
}

/**
 * Legacy-compatible presentation wrapper that provides the same API as DawnPresentation.
 * 
 * This wrapper delegates all calls to the underlying new Window and Session.
 * It NEVER closes any handles - the underlying session owns them.
 * 
 * **Important:** This is NOT the actual DawnPresentation class from dawn4k-toolkit-surface.
 * It provides a compatible API surface for migration purposes.
 */
public class LegacyDawnPresentation(
    private val newWindow: Window,
    private val newSession: Session,
) {
    /**
     * The current surface configuration.
     * Not yet supported in migration facade - returns default.
     */
    public val configuration: DawnSurfaceConfiguration
        get() = org.graphiks.dawn4k.toolkit.surface.DawnSurfaceConfiguration(
            format = org.graphiks.webgpu.GPUTextureFormat.BGRA8Unorm,
            alphaMode = org.graphiks.dawn4k.toolkit.surface.DawnAlphaMode.Premultiplied,
            presentMode = org.graphiks.dawn4k.toolkit.surface.DawnPresentMode.Fifo,
            extent = org.graphiks.dawn4k.toolkit.surface.PixelExtent(800, 600),
        )
    
    /**
     * Executes a block with a frame.
     * Returns false since the new Window may not support this directly.
     */
    public suspend fun withFrame(block: suspend (org.graphiks.dawn4k.toolkit.surface.DawnFrame) -> Unit): Boolean {
        // No-op: frame presentation is not supported through the new toolkit's Window
        return false
    }
    
    /**
     * Replaces the target surface.
     * No-op since the new Window may not support this directly.
     */
    public suspend fun replaceTarget(target: DawnSurfaceTarget) {
        // No-op: target replacement is not supported through the new toolkit's Window
    }
    
    /**
     * Replaces the target surface with options.
     * No-op since the new Window may not support this directly.
     */
    public suspend fun replaceTarget(target: DawnSurfaceTarget, options: DawnPresentationOptions) {
        // No-op: target replacement with options is not supported through the new toolkit's Window
    }
    
    /**
     * Detaches a target by generation.
     * No-op since the new Window may not support this directly.
     */
    public suspend fun detachTarget(generation: Long) {
        // No-op: target detachment is not supported through the new toolkit's Window
    }
}
