package org.graphiks.dawn4k.webgpu.toolkit.migration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
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
    private val windowHost: WindowHost = GlfwWindowHost()
    
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
            // Open a window using the host
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
                // The new toolkit manages the lifecycle
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
     * Note: This is not fully implemented in the migration facade.
     */
    public val events: SharedFlow<DawnWindowEvent>
        get() = error("Window events not yet supported in migration facade")
    
    /**
     * The native window handle.
     * Note: This is not supported in the migration facade.
     */
    public val nativeHandle: Long
        get() = error("Native handle access not supported in migration facade")
    
    /**
     * Requests a resize of the window.
     * Note: This is not fully implemented in the migration facade.
     */
    public suspend fun resize(width: Int, height: Int) {
        error("Window resize not yet supported in migration facade")
    }
    
    /**
     * Requests the window to close.
     * Note: This is not fully implemented in the migration facade.
     */
    public suspend fun requestClose() {
        error("Window requestClose not yet supported in migration facade")
    }
    
    /**
     * **Important:** This method throws an error because the facade NEVER closes borrowed handles.
     * The session owns the window and its resources, and only the new toolkit can close them.
     */
    public suspend fun shutdown() {
        error(
            "shutdown() should not be called on borrowed windows - the new toolkit owns the lifecycle. " +
            "The facade NEVER closes borrowed handles."
        )
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
     */
    public val configuration: DawnSurfaceConfiguration
        get() = error("Presentation configuration not yet supported in migration facade")
    
    /**
     * Executes a block with a frame.
     * Note: This is not fully implemented in the migration facade.
     */
    public suspend fun withFrame(block: suspend (org.graphiks.dawn4k.toolkit.surface.DawnFrame) -> Unit): Boolean {
        error("Presentation withFrame not yet supported in migration facade")
    }
    
    /**
     * Replaces the target surface.
     * Note: This is not fully implemented in the migration facade.
     */
    public suspend fun replaceTarget(target: DawnSurfaceTarget) {
        error("Presentation replaceTarget not yet supported in migration facade")
    }
    
    /**
     * Replaces the target surface with options.
     * Note: This is not fully implemented in the migration facade.
     */
    public suspend fun replaceTarget(target: DawnSurfaceTarget, options: DawnPresentationOptions) {
        error("Presentation replaceTarget with options not yet supported in migration facade")
    }
    
    /**
     * Detaches a target by generation.
     * Note: This is not fully implemented in the migration facade.
     */
    public suspend fun detachTarget(generation: Long) {
        error("Presentation detachTarget not yet supported in migration facade")
    }
}
