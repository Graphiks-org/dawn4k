package org.graphiks.dawn4k.webgpu.toolkit.interop

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.graphiks.dawn4k.DawnAdapter
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.DawnQueue
import org.graphiks.dawn4k.DawnTexture
import org.graphiks.dawn4k.webgpu.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.webgpu.toolkit.DawnSessionState
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUDeviceLostInfo
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.ManualProgression
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.SessionState
import org.graphiks.webgpu.toolkit.withSession

/**
 * Exposes the dawn4k toolkit's own session API on top of the generic WebGPU toolkit.
 *
 * The two toolkits are both current: dawn4k publishes `DawnSession`, `DawnWindow` and
 * `DawnPresentation`, while the Graphiks toolkit publishes `Session`, `Window` and
 * `ToolkitSurface`. This object lets code written against the dawn4k types run on the generic
 * toolkit, so an application can move to `org.graphiks.webgpu:toolkit-*` without rewriting its
 * render loop.
 *
 * **Important:** the views borrow handles (device, queue, context); the session owns them and
 * closes them. `shutdown()` on a view is a no-op for that reason.
 * 
 * **Note:** `withDawnWindow` needs GLFW, so it lives in the JVM-only [DawnToolkitInteropJvm].
 * 
 * **Usage:**
 * ```kotlin
 * DawnToolkitInterop.withDawnSession { session ->
 *     // session.device, session.queue, session.run { ... }
 * }
 * ```
 */
public object DawnToolkitInterop {
    
    private val extension: Dawn4kWebGpuToolkitExtension = Dawn4kWebGpuToolkitExtension()
    
    /**
     * Opens a Dawn session using the new WebGPU toolkit and executes the given block.
     * 
     * Delegates to the toolkit's `withSession`; the session owns the device, queue and
     * context, and the view never closes them.
     * 
     * @param config Session configuration of the new toolkit
     * @param hints Adapter request hints of the new toolkit
     * @param block The code to execute with the session
     * @return The result of the block
     */
    public suspend fun <T> withDawnSession(
        config: SessionConfig = SessionConfig(),
        hints: AdapterHints = AdapterHints(),
        block: suspend CoroutineScope.(ToolkitSessionView) -> T,
    ): T {
        // Delegate to the new toolkit's withSession
        // The new toolkit handles all lifecycle management including cleanup
        return withSession(
            extension = extension,
            config = config,
            hints = hints,
        ) { newSession ->
            val view = ToolkitSessionView(newSession, extension)

            coroutineScope {
                block(view)
            }
        }
    }
}

/**
 * The dawn4k `DawnSession` surface over a toolkit [Session].
 *
 * Every call delegates to the toolkit session, which owns the device, queue and context; the
 * view never closes them. This is not `org.graphiks.dawn4k.webgpu.toolkit.DawnSession`, it only
 * offers the same members so dawn4k-style code keeps compiling.
 */
public class ToolkitSessionView(
    private var newSession: Session?,
    private val extension: Dawn4kWebGpuToolkitExtension,
) {
    /**
     * Creates a view before its session exists, for an initialization that runs during startup
     * and needs the view already. [bind] supplies the session once startup returns.
     */
    public constructor(extension: Dawn4kWebGpuToolkitExtension) : this(null, extension)

    /** Attaches the session this view delegates to, once startup produced it. */
    internal fun bind(session: Session) {
        newSession = session
    }

    /** The device startup produced, kept for an initialization that runs before the session. */
    private var startedDevice: GPUDevice? = null

    /** The session this view delegates to; fails when startup has not produced it yet. */
    private val session: Session
        get() = checkNotNull(newSession) { "the toolkit session is not available yet" }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * The device from the underlying session.
     * This is a DawnDevice that can be used for WebGPU operations.
     */
    public val device: DawnDevice
        get() = (startedDevice ?: session.device) as DawnDevice

    /**
     * Records the device startup produced, so an initialization that runs before the session
     * exists can already reach it.
     */
    internal fun bindDevice(device: GPUDevice) {
        startedDevice = device
    }
    
    /**
     * The queue from the underlying session.
     * This is a DawnQueue that can be used for WebGPU operations.
     */
    public val queue: DawnQueue
        get() = device.queue as DawnQueue

    /**
     * The Dawn context the session's device came from, matching dawn4k's `DawnSession.context`.
     *
     * A surface reference belongs to the context that created it, so this is what a caller needs
     * to retain an additional native surface — `DawnSurfaceTargets.metalLayer(...)` and friends —
     * and present to it on the same device.
     */
    public val context: DawnContext
        get() = checkNotNull(extension.currentInstance()) {
            "the session view lost the instance its extension opened"
        }.dawnContext

    /**
     * The adapter the session's device was requested from, matching dawn4k's
     * `DawnSession.adapter`.
     */
    public val adapter: DawnAdapter
        get() = checkNotNull(extension.currentInstance()?.lastAdapter) {
            "the session has not requested an adapter yet"
        }

    /**
     * The lifecycle the session publishes, translated to dawn4k's `DawnSessionState`.
     *
     * The generic toolkit names the same phases; a loss carries no info there, so
     * `DawnSessionState.Lost` is published without one and [awaitLost] still returns it.
     * Built on first read, because a view created before startup has no session yet.
     */
    public val state: StateFlow<DawnSessionState> by lazy {
        session.state
            .map { it.toDawnSessionState() }
            .stateIn(scope, SharingStarted.Eagerly, session.state.value.toDawnSessionState())
    }

    /**
     * The manual progression lease, or `null` when the session is not driven manually.
     *
     * The dawn4k extension always runs `Manual`, so a caller must pump events through this lease
     * on the session's execution owner.
     */
    public val manualProgress: ManualProgression?
        get() = session.manualProgress

    /**
     * The dispatcher every session operation must run on, matching dawn4k's
     * `DawnSession.executionOwner`.
     *
     * A dawn4k context processes its events on the thread that created it, so work coming from
     * another thread gets back with `withContext(executionOwner)`. The toolkit publishes it as a
     * dispatcher; dawn4k's own owner type wraps the same guarantee.
     */
    public val executionOwner: CoroutineDispatcher?
        get() = session.executionOwner
    
    /**
     * Executes a block with this session as the receiver, on the thread that owns the Dawn
     * context: native calls (queue submit, surface acquire/present, …) must not leave it.
     */
    public suspend fun <T> run(block: suspend CoroutineScope.(ToolkitSessionView) -> T): T {
        val owner = extension.currentInstance()?.dawnOwner
        return if (owner != null) {
            owner.run { coroutineScope { block(this@ToolkitSessionView) } }
        } else {
            coroutineScope { block(this@ToolkitSessionView) }
        }
    }

    /**
     * Shuts the session down, pumping its events until the teardown completes.
     *
     * A dawn4k device notifies its loss only when its events are processed, and the toolkit
     * waits for that notification while releasing handles. A `Manual` session therefore needs a
     * driver running through its shutdown; stopping the hand-over first only makes shutdown
     * refuse to start.
     */
    public suspend fun shutdown() {
        // `EventLoop` sessions have no driver to stop; the extension's own pump keeps running
        // until the instance closes, and the core runs the teardown on the instance's owner.
        session.shutdown()
    }

    /**
     * Awaits device loss.
     */
    public suspend fun awaitLost(): Result<GPUDeviceLostInfo> {
        return device.awaitLost()
    }

    /**
     * Creates an offscreen render texture owned by the caller, matching dawn4k's
     * `DawnSession.createRenderTexture`.
     *
     * The texture belongs to the caller and must be closed after use; the session does not
     * track it. Rendering into it is the ordinary WebGPU path: encode a render pass with
     * [texture]'s view as the colour attachment, then copy it out if the caller needs the
     * pixels.
     *
     * @param width Texture width in texels
     * @param height Texture height in texels
     * @param format Texture format, which a pipeline rendering into it must match
     * @param usage Usages the texture is created with
     */
    public fun createRenderTexture(
        width: UInt,
        height: UInt,
        format: GPUTextureFormat,
        usage: GPUTextureUsage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
    ): DawnTexture {
        require(width > 0u && height > 0u) { "render texture dimensions must be positive" }
        return device.createTexture(
            TextureDescriptor(
                size = Extent3D(width, height),
                format = format,
                usage = usage,
            ),
        ) as DawnTexture
    }
}

/**
 * Translates the toolkit's session lifecycle to dawn4k's.
 *
 * Both name the same phases. The toolkit's loss carries no info, so `DawnSessionState.Lost` is
 * published without one; [ToolkitSessionView.awaitLost] still returns the details.
 */
internal fun SessionState.toDawnSessionState(): DawnSessionState = when (this) {
    SessionState.Ready -> DawnSessionState.Ready
    SessionState.ShuttingDown -> DawnSessionState.ShuttingDown
    SessionState.ShutdownTimedOut -> DawnSessionState.ShutdownTimedOut
    SessionState.Lost -> DawnSessionState.Lost(UnknownDeviceLost)
    SessionState.Failed -> DawnSessionState.Failed(
        IllegalStateException("the toolkit session failed; see the session log"),
    )
    SessionState.Closed -> DawnSessionState.Closed
}

/**
 * The loss dawn4k's state carries when the toolkit only reports that the device was lost.
 *
 * The toolkit's `SessionState.Lost` has no payload; [ToolkitSessionView.awaitLost] returns the
 * details, so this stands in for the state flow only.
 */
private object UnknownDeviceLost : org.graphiks.webgpu.GPUDeviceLostInfo {
    override val reason: org.graphiks.webgpu.GPUDeviceLostReason =
        org.graphiks.webgpu.GPUDeviceLostReason.Unknown
    override val message: String = "the device was lost; awaitLost carries the details"
}

/**
 * Maps the dawn4k [DawnSessionOptions] to the toolkit [SessionConfig]: the startup/shutdown
 * timeouts and the device descriptor, which both contracts hand to `requestDevice`.
 *
 * The progression is not carried over: a dawn4k context processes its events on the thread that
 * created it, and the extension reports `Manual` for that reason, so asking for another
 * progression would fail startup.
 */
internal fun DawnSessionOptions.toSessionConfig(): SessionConfig {
    return SessionConfig(
        startupTimeout = startupTimeout,
        shutdownTimeout = shutdownTimeout,
        deviceDescriptor = deviceDescriptor,
    )
}
