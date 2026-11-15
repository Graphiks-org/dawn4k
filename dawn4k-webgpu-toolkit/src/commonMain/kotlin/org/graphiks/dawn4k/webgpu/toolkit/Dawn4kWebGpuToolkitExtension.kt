package org.graphiks.dawn4k.webgpu.toolkit

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

    override fun open(config: SessionConfig): ToolkitInstance {
        return Dawn4kToolkitInstance(
            context = org.graphiks.dawn4k.DawnContext.create(),
            progression = Progression.Manual,
        )
    }
}
