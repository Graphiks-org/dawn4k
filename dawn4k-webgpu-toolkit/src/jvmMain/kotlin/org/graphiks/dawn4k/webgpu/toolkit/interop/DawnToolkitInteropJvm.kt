package org.graphiks.dawn4k.webgpu.toolkit.interop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.graphiks.dawn4k.toolkit.DawnSessionOptions
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.dawn4k.toolkit.glfw.DawnWindowEvent
import org.graphiks.dawn4k.toolkit.glfw.DawnWindowOptions
import org.graphiks.dawn4k.toolkit.surface.DawnPresentation
import org.graphiks.dawn4k.toolkit.surface.DawnAlphaMode
import org.graphiks.dawn4k.toolkit.surface.DawnPresentMode
import org.graphiks.dawn4k.toolkit.surface.DawnPresentationOptions
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceConfiguration
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.toolkit.surface.PixelExtent
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.dawn4k.webgpu.toolkit.window.GlfwWindow
import org.graphiks.dawn4k.webgpu.toolkit.window.GlfwWindowHost
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.surface.Window
import org.graphiks.webgpu.toolkit.surface.WindowConfig
import org.graphiks.webgpu.toolkit.surface.WindowEvent
import org.graphiks.webgpu.toolkit.surface.WindowHost
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.surface.ToolkitSurface
import org.graphiks.webgpu.toolkit.surface.withSurface
import org.graphiks.webgpu.toolkit.withSession

/**
 * JVM side of [DawnToolkitInterop]: the window entry point, which needs GLFW.
 *
 * `withDawnWindow` opens a window through a [WindowHost], presents to it with the toolkit's
 * surface protocol, and hands the dawn4k-shaped window and presentation views to the block.
 */
public object DawnToolkitInteropJvm {
    
    private val extension: Dawn4kWebGpuToolkitExtension = Dawn4kWebGpuToolkitExtension()
    
    /**
     * Allows injection of a custom WindowHost for testing.
     * Defaults to GlfwWindowHost for production use.
     */
    public var windowHost: WindowHost = GlfwWindowHost()
    
    /**
     * Opens a Dawn window using the new WebGPU toolkit and executes the given block.
     * 
     * Delegates to the toolkit's window and surface protocol.
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
        block: suspend CoroutineScope.(ToolkitSessionView, ToolkitWindowView, ToolkitPresentationView) -> T,
    ): T {
        // Map configurations
        val sessionConfig = sessionOptions.toSessionConfig()
        val windowConfig = windowOptions.toWindowConfig()
        
        // Open the window first, then present to it through the toolkit's surface protocol.
        val newWindow = windowHost.open(windowConfig)
        return withSurface(
            extension = extension,
            target = newWindow,
            hints = sessionOptions.toAdapterHints(),
        ) { newSession, newSurface ->
            try {
                val sessionView = ToolkitSessionView(newSession)
                val windowView = ToolkitWindowView(newWindow, newSession, newSurface)
                val presentationView = windowView.presentation

                // Execute the block with a CoroutineScope receiver
                coroutineScope {
                    block(sessionView, windowView, presentationView)
                }
            } finally {
                // The toolkit owns the surface, session and window lifetimes; the facade only
                // hands out borrowed handles and never closes them.
            }
        }
    }
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
 * The dawn4k `DawnWindow` surface over a toolkit [Window].
 * 
 * This wrapper delegates all calls to the underlying new Window and Session.
 * It NEVER closes any handles - the underlying session owns them.
 * 
 * **Important:** This is NOT the actual DawnWindow class from dawn4k-toolkit-glfw.
 * It offers the same members so dawn4k-style code keeps compiling.
 */
public class ToolkitWindowView(
    private val newWindow: Window,
    private val newSession: Session,
    private val newSurface: ToolkitSurface,
) {
    /**
     * The GPU session associated with this window.
     */
    public val gpu: ToolkitSessionView = ToolkitSessionView(newSession)
    
    /**
     * The presentation associated with this window.
     */
    public val presentation: ToolkitPresentationView = ToolkitPresentationView(newSurface, newWindow)
    
    /**
     * Events from the window.
     *
     * Delegates to the new [Window.events] flow, translating [WindowEvent]s back to
     * dawn4k [DawnWindowEvent]s. Because the underlying window publishes a hot flow
     * (replay = 1), the mapped flow is hot as well; note the dawn4k `DawnWindow.events`
     * was a `SharedFlow`, while this exposes the mapped [Flow].
     */
    public val events: Flow<DawnWindowEvent> = newWindow.events.map { it.toDawnWindowEvent() }

    /**
     * The native window handle.
     * Returns 0L as it's not available from the new Window.
     */
    public val nativeHandle: Long = 0L

    /**
     * Requests a resize of the window.
     *
     * Delegates to the new window's real `resize` (available on GLFW-backed windows,
     * i.e. windows opened by [GlfwWindowHost]).
     */
    public suspend fun resize(width: Int, height: Int) {
        val glfwWindow = newWindow as? GlfwWindow
            ?: throw UnsupportedOperationException(
                "resize is only supported for GLFW-backed windows (opened by GlfwWindowHost), " +
                    "got ${newWindow::class.simpleName}"
            )
        glfwWindow.resize(width, height)
    }

    /**
     * Requests the window to close.
     *
     * Delegates to the new window's real close-request path (available on GLFW-backed
     * windows, i.e. windows opened by [GlfwWindowHost]).
     */
    public suspend fun requestClose() {
        val glfwWindow = newWindow as? GlfwWindow
            ?: throw UnsupportedOperationException(
                "requestClose is only supported for GLFW-backed windows (opened by GlfwWindowHost), " +
                    "got ${newWindow::class.simpleName}"
            )
        glfwWindow.requestClose()
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
 * The dawn4k `DawnPresentation` surface over a toolkit `ToolkitSurface`.
 * 
 * This wrapper delegates all calls to the underlying new Window and Session.
 * It NEVER closes any handles - the underlying session owns them.
 * 
 * **Important:** This is NOT the actual DawnPresentation class from dawn4k-toolkit-surface.
 * It offers the same members so dawn4k-style code keeps compiling.
 */
public class ToolkitPresentationView(
    private val surface: ToolkitSurface,
    private val target: SurfaceTarget,
) {
    /**
     * The configuration the toolkit negotiated for this surface, in dawn4k terms.
     *
     * The toolkit publishes the format and alpha mode it bound; the extent is the target's
     * physical extent, which is the size of the frames it hands out. `presentMode` is `Fifo`
     * because the toolkit does not expose a present mode yet.
     */
    public val configuration: DawnSurfaceConfiguration
        get() {
            val negotiated = checkNotNull(surface.configuration.value) {
                "the toolkit surface is still negotiating its configuration"
            }
            val extent = target.state.value.extent
            return DawnSurfaceConfiguration(
                format = negotiated.format,
                alphaMode = negotiated.alphaMode.toDawnAlphaMode(),
                presentMode = DawnPresentMode.Fifo,
                extent = PixelExtent(extent.width.toInt(), extent.height.toInt()),
            )
        }
    
    /**
     * Executes a block with the toolkit's frame for this window and presents it on a normal
     * return, exactly like the toolkit's own `withFrame`.
     */
    public suspend fun withFrame(block: suspend (org.graphiks.webgpu.toolkit.surface.Frame) -> Unit) {
        // The toolkit frame block is not suspending while dawn4k blocks are, so the frame is
        // handed to the caller through a bounded bridge that keeps the frame open during the body.
        coroutineScope {
            surface.withFrame { frame ->
                runBlocking { block(frame) }
            }
        }
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

/**
 * Translates a toolkit alpha mode to its dawn4k equivalent; both describe how the compositor
 * blends presented frames with the content behind the target.
 */
private fun org.graphiks.webgpu.toolkit.AlphaMode.toDawnAlphaMode(): DawnAlphaMode = when (this) {
    org.graphiks.webgpu.toolkit.AlphaMode.Opaque -> DawnAlphaMode.Opaque
    org.graphiks.webgpu.toolkit.AlphaMode.Premultiplied -> DawnAlphaMode.Premultiplied
    org.graphiks.webgpu.toolkit.AlphaMode.PostMultiplied -> DawnAlphaMode.Unpremultiplied
}

/**
 * Translates a toolkit [WindowEvent] to the dawn4k [DawnWindowEvent].
 * [WindowEvent.Resized] carries the LOGICAL extent, mapped into [PixelExtent].
 */
private fun WindowEvent.toDawnWindowEvent(): DawnWindowEvent = when (this) {
    WindowEvent.CloseRequested -> DawnWindowEvent.CloseRequested
    is WindowEvent.Resized -> DawnWindowEvent.Resized(
        PixelExtent(width = logical.width.toInt(), height = logical.height.toInt())
    )
}

/**
 * Maps the dawn4k [DawnSessionOptions] to the toolkit [SessionConfig], carrying over the
 * startup/shutdown timeouts (the other dawn4k options have no toolkit equivalent).
 *
 * Lives in the JVM source set because the dawn4k toolkit types are only available there.
 */
internal fun DawnSessionOptions.toSessionConfig(): SessionConfig {
    return SessionConfig(
        startupTimeout = startupTimeout,
        shutdownTimeout = shutdownTimeout,
    )
}

/**
 * Maps the dawn4k [DawnSessionOptions] to the toolkit [AdapterHints], carrying over the
 * adapter request options' power preference and fallback-adapter flag.
 */
internal fun DawnSessionOptions.toAdapterHints(): AdapterHints {
    return AdapterHints(
        powerPreference = adapterOptions?.powerPreference,
        forceFallbackAdapter = adapterOptions?.forceFallbackAdapter,
    )
}
