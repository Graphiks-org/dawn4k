package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.graphiks.dawn4k.webgpu.toolkit.internal.Dawn4kToolkitInstance
import org.graphiks.webgpu.toolkit.Progression
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension

/**
 * Dawn4k implementation of the [WebGpuToolkitExtension] SPI.
 *
 * The extension fronts dawn4k's own Dawn context, so every toolkit session owns one context,
 * its adapters and its device. The progression is [Progression.Manual]: the toolkit's `Pumped`
 * loop would call [ToolkitInstance.pumpEvents] from a background coroutine, while a dawn4k
 * context processes its events on the thread that created it, which is the caller's.
 */
class Dawn4kWebGpuToolkitExtension : WebGpuToolkitExtension {

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
        val instance = Dawn4kToolkitInstance(
            context = org.graphiks.dawn4k.DawnContext.create(),
            progression = Progression.Manual,
            sessionConfig = config,
        )
        synchronized(opened) { lastOpened = instance }
        return instance
    }

    /**
     * The instance behind the session that is currently starting, or `null` when this extension
     * has not opened one.
     */
    internal fun currentInstance(): Dawn4kToolkitInstance? = synchronized(opened) { lastOpened }
}
