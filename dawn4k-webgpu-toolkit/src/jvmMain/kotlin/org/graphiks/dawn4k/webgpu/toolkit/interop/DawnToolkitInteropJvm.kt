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
import org.graphiks.webgpu.toolkit.surface.KeyState
import org.graphiks.webgpu.toolkit.surface.PointerButton
import org.graphiks.webgpu.toolkit.surface.PointerState
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
     * The platform window handle (GLFWwindow*), or `0` for a window that is not GLFW-backed.
     * Valid until the window closes.
     */
    public val nativeHandle: Long
        get() = (newWindow as? GlfwWindow)?.dawnWindow?.nativeHandle ?: 0L

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
     * Swaps the presented target for [target], keeping the device when it supports the current
     * format. The toolkit retains and rolls back the replacement; a failure leaves the previous
     * target presenting.
     */
    public suspend fun replaceTarget(target: DawnSurfaceTarget) {
        surface.replaceTarget(target.toSurfaceTarget())
    }

    /**
     * Swaps the presented target, applying [options] to the new configuration.
     *
     * The toolkit negotiates from the replacement's capabilities; dawn4k's per-option policy
     * (required or preferred format, present mode, alpha mode) has no toolkit equivalent yet, so
     * the surface keeps its own negotiation and the options only bound the accepted format.
     */
    public suspend fun replaceTarget(target: DawnSurfaceTarget, options: DawnPresentationOptions) {
        val required = options.requiredFormat
        val replacement = target.toSurfaceTarget()
        if (required != null) {
            require(required in replacement.capabilitiesFormats) {
                "the replacement target does not support the required format $required"
            }
        }
        surface.replaceTarget(replacement)
    }

    /**
     * Detaches the target whose lease carries [generation]; [withFrame] then suspends until a
     * replacement arrives. A generation the surface no longer holds is ignored by the toolkit.
     */
    public suspend fun detachTarget(generation: Long) {
        surface.detachTarget(generation)
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
    is WindowEvent.Pointer -> when (state) {
        PointerState.Moved -> DawnWindowEvent.PointerMoved(position.x, position.y)
        PointerState.Pressed -> DawnWindowEvent.PointerButton(
            button = (button ?: PointerButton.Primary).toGlfwButton(),
            action = GLFW_PRESS,
            modifiers = 0,
        )
        PointerState.Released -> DawnWindowEvent.PointerButton(
            button = (button ?: PointerButton.Primary).toGlfwButton(),
            action = GLFW_RELEASE,
            modifiers = 0,
        )
    }
    WindowEvent.PointerExited -> DawnWindowEvent.PointerExited
    is WindowEvent.Scroll -> DawnWindowEvent.Scroll(delta.x, delta.y)
    is WindowEvent.Key -> DawnWindowEvent.Key(
        key = key.toGlfwKey(),
        scanCode = 0,
        action = if (state == KeyState.Pressed) GLFW_PRESS else GLFW_RELEASE,
        modifiers = 0,
        text = text,
    )
    is WindowEvent.FocusChanged -> DawnWindowEvent.FocusChanged(focused)
}

/** The window contract names keys; dawn4k carries the GLFW code, so the common ones map back. */
private fun String.toGlfwKey(): Int = when (this) {
    "Space" -> GLFW_KEY_SPACE
    "Escape" -> GLFW_KEY_ESCAPE
    "Enter" -> GLFW_KEY_ENTER
    "Tab" -> GLFW_KEY_TAB
    "Backspace" -> GLFW_KEY_BACKSPACE
    "Delete" -> GLFW_KEY_DELETE
    "ArrowLeft" -> GLFW_KEY_LEFT
    "ArrowRight" -> GLFW_KEY_RIGHT
    "ArrowUp" -> GLFW_KEY_UP
    "ArrowDown" -> GLFW_KEY_DOWN
    "Home" -> GLFW_KEY_HOME
    "End" -> GLFW_KEY_END
    "PageUp" -> GLFW_KEY_PAGE_UP
    "PageDown" -> GLFW_KEY_PAGE_DOWN
    "Shift" -> GLFW_KEY_LEFT_SHIFT
    "Control" -> GLFW_KEY_LEFT_CONTROL
    "Alt" -> GLFW_KEY_LEFT_ALT
    "Meta" -> GLFW_KEY_LEFT_SUPER
    else -> when {
        length == 4 && startsWith("Key") -> GLFW_KEY_A + (this[3] - 'A')
        length == 6 && startsWith("Digit") -> GLFW_KEY_0 + (this[5] - '0')
        startsWith("F") && drop(1).toIntOrNull() in 1..25 -> GLFW_KEY_F1 + (drop(1).toInt() - 1)
        else -> 0
    }
}

private fun PointerButton.toGlfwButton(): Int = when (this) {
    PointerButton.Primary -> GLFW_MOUSE_BUTTON_LEFT
    PointerButton.Secondary -> GLFW_MOUSE_BUTTON_RIGHT
    PointerButton.Middle -> GLFW_MOUSE_BUTTON_MIDDLE
    PointerButton.Back -> 3
    PointerButton.Forward -> 4
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

// GLFW key, button and action codes (glfw3.h), mirroring the window contract's names.
private const val GLFW_RELEASE = 0
private const val GLFW_PRESS = 1
private const val GLFW_KEY_SPACE = 32
private const val GLFW_KEY_0 = 48
private const val GLFW_KEY_A = 65
private const val GLFW_KEY_ESCAPE = 256
private const val GLFW_KEY_ENTER = 257
private const val GLFW_KEY_TAB = 258
private const val GLFW_KEY_BACKSPACE = 259
private const val GLFW_KEY_DELETE = 261
private const val GLFW_KEY_RIGHT = 262
private const val GLFW_KEY_LEFT = 263
private const val GLFW_KEY_DOWN = 264
private const val GLFW_KEY_UP = 265
private const val GLFW_KEY_PAGE_UP = 266
private const val GLFW_KEY_PAGE_DOWN = 267
private const val GLFW_KEY_HOME = 268
private const val GLFW_KEY_END = 269
private const val GLFW_KEY_F1 = 290
private const val GLFW_KEY_LEFT_SHIFT = 340
private const val GLFW_KEY_LEFT_CONTROL = 341
private const val GLFW_KEY_LEFT_ALT = 342
private const val GLFW_KEY_LEFT_SUPER = 343
private const val GLFW_MOUSE_BUTTON_LEFT = 0
private const val GLFW_MOUSE_BUTTON_RIGHT = 1
private const val GLFW_MOUSE_BUTTON_MIDDLE = 2

/**
 * Presents a dawn4k target through the toolkit contract, so it can replace the surface's
 * current target. Adapters are kept until the surface releases them.
 */
private fun DawnSurfaceTarget.toSurfaceTarget(): SurfaceTarget =
    DawnTargetAdapter(this).also { targetAdapters.add(it) }

/** Adapters created for target replacement; closed when the presentation view is closed. */
private val targetAdapters = mutableListOf<DawnTargetAdapter>()
