package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.sync.Mutex
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnTexture
import org.graphiks.dawn4k.webgpu.toolkit.internal.Dawn4kToolkitInstance
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.toolkit.Progression
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension

/**
 * Dawn4k implementation of the [WebGpuToolkitExtension] SPI.
 *
 * The extension fronts dawn4k's own Dawn context, so every toolkit session owns one context,
 * its adapters and its device.
 *
 * The progression is [Progression.Manual], and the instance drives its own pump on a dedicated
 * thread: a dawn4k context processes its events on the thread that created it, so the toolkit's
 * `Pumped` loop, which would pump from whatever coroutine it runs on, cannot be used. The
 * instance owns that thread and pumps from it, which is what dawn4k's own `Managed` execution
 * does, and what a caller of a surface session expects: frames advance on their own.
 */
class Dawn4kWebGpuToolkitExtension(
    /**
     * Backend configuration of the Dawn context each session opens. The default picks the
     * platform backend; a test or an application chooses another one here.
     */
    private val dawnConfig: DawnConfig = DawnConfig(),
) : WebGpuToolkitExtension {

    override val id: String = "dawn4k"

    /**
     * The instance opened most recently.
     *
     * A toolkit session publishes neither the instance it was opened from nor the Dawn context
     * that instance owns, but a dawn4k caller needs both. The toolkit opens the instance during
     * startup, before it runs the caller's block, so reading this inside that block yields the
     * session's own instance.
     */
    private val opened = SynchronizedObject()
    private var lastOpened: Dawn4kToolkitInstance? = null

    override fun open(config: SessionConfig): ToolkitInstance {
        // A dawn4k context advances only when its events are processed, and it must be driven
        // from the thread that created it. The instance therefore owns a pinned thread, creates
        // its context there and pumps from it: `EventLoop` is the progression that says the
        // extension drives its own loop, so the toolkit neither pumps nor hands a driver over.
        val owner = DawnExecutionOwner.dedicated("dawn4k-toolkit")
        val context = owner.runBlocking { org.graphiks.dawn4k.DawnContext.create(dawnConfig) }
        val instance = Dawn4kToolkitInstance(
            context = context,
            owner = owner,
            progression = Progression.EventLoop,
            sessionConfig = config,
        )
        instance.startPump()
        synchronized(opened) { lastOpened = instance }
        return instance
    }

    /**
     * The instance behind the session that is currently starting, or `null` when this extension
     * has not opened one.
     */
    internal fun currentInstance(): Dawn4kToolkitInstance? = synchronized(opened) { lastOpened }

    /**
     * Serializes session startups through [openDawnSession], so the instance an attach records
     * is the one this very startup opened, never a concurrent one's.
     */
    internal val startupMutex = Mutex()

    /** The sessions this extension bound to the instances they opened. */
    private val sessions = mutableMapOf<Session, Dawn4kToolkitInstance>()

    /** Binds [session] to the instance its startup opened. */
    internal fun attach(session: Session, instance: Dawn4kToolkitInstance) = synchronized(opened) {
        sessions[session] = instance
    }

    /** Releases the binding of a shut-down session, so the map holds only live sessions. */
    internal fun detach(session: Session) = synchronized(opened) {
        sessions.remove(session)
    }

    /** The instance [session] was opened with, or a failure naming the fix. */
    internal fun instanceOf(session: Session): Dawn4kToolkitInstance = synchronized(opened) {
        sessions[session]
    } ?: error("the session was not opened through openDawnSession; the dawn accessors need an attached session")

    /** The dawn context [session]'s instance owns, for retaining additional native surfaces. */
    fun dawnContext(session: Session): org.graphiks.dawn4k.DawnContext = instanceOf(session).dawnContext

    /**
     * The adapter [session]'s device was requested from, or a failure before the request.
     */
    fun dawnAdapter(session: Session): org.graphiks.dawn4k.DawnAdapter =
        checkNotNull(instanceOf(session).lastAdapter) { "the session has not requested an adapter yet" }

    /** The session's device as the dawn4k type it is. */
    fun dawnDevice(session: Session): org.graphiks.dawn4k.DawnDevice {
        instanceOf(session)
        return session.device as org.graphiks.dawn4k.DawnDevice
    }

    /** The session's primary queue as the dawn4k type it is. */
    fun dawnQueue(session: Session): org.graphiks.dawn4k.DawnQueue {
        instanceOf(session)
        return session.queue as org.graphiks.dawn4k.DawnQueue
    }

    /**
     * Creates an offscreen render texture owned by the caller, on [session]'s device.
     *
     * The texture belongs to the caller and must be closed after use; the session does not
     * track it. Rendering into it is the ordinary WebGPU path: encode a render pass with
     * [texture]'s view as the colour attachment, then copy it out if the caller needs the
     * pixels.
     */
    fun createRenderTexture(
        session: Session,
        width: UInt,
        height: UInt,
        format: GPUTextureFormat,
        usage: GPUTextureUsage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
    ): DawnTexture {
        require(width > 0u && height > 0u) { "render texture dimensions must be positive" }
        val device = session.device as org.graphiks.dawn4k.DawnDevice
        return device.createTexture(
            TextureDescriptor(
                size = Extent3D(width, height),
                format = format,
                usage = usage,
            ),
        ) as DawnTexture
    }
}
