package org.graphiks.dawn4k.toolkit.surface

import org.graphiks.dawn4k.native.*
import org.graphiks.webgpu.GPUTextureFormat

/** When presented frames become visible relative to display refresh. */
enum class DawnPresentMode(internal val nativeValue: UInt) {
    Fifo(WGPUPresentMode_Fifo), FifoRelaxed(WGPUPresentMode_FifoRelaxed),
    Immediate(WGPUPresentMode_Immediate), Mailbox(WGPUPresentMode_Mailbox),
}

/** How the compositor blends a presented frame with the content behind the target. */
enum class DawnAlphaMode(internal val nativeValue: UInt) {
    Auto(WGPUCompositeAlphaMode_Auto), Opaque(WGPUCompositeAlphaMode_Opaque),
    Premultiplied(WGPUCompositeAlphaMode_Premultiplied), Unpremultiplied(WGPUCompositeAlphaMode_Unpremultiplied),
    Inherit(WGPUCompositeAlphaMode_Inherit),
}

/**
 * Negotiation policy applied whenever a surface configures: each `required` value
 * must be advertised by the target or configuration fails, otherwise the first
 * supported `preferred` entry wins, falling back to the target's first
 * advertised capability.
 */
data class DawnPresentationOptions(
    /** Texture formats tried in order; a pipeline rendering into frames must match the selected one. */
    val preferredFormats: List<GPUTextureFormat> = listOf(GPUTextureFormat.BGRA8Unorm, GPUTextureFormat.RGBA8Unorm),
    /** A format the target must support; takes precedence over [preferredFormats]. */
    val requiredFormat: GPUTextureFormat? = null,
    /** Present modes tried in order. */
    val preferredPresentModes: List<DawnPresentMode> = listOf(DawnPresentMode.Fifo),
    /** A present mode the target must support; takes precedence over [preferredPresentModes]. */
    val requiredPresentMode: DawnPresentMode? = null,
    /** Alpha modes tried in order; defaults to every mode the backend defines. */
    val preferredAlphaModes: List<DawnAlphaMode> = listOf(
        DawnAlphaMode.Auto, DawnAlphaMode.Opaque, DawnAlphaMode.Premultiplied, DawnAlphaMode.Unpremultiplied, DawnAlphaMode.Inherit,
    ),
    /** An alpha mode the target must support; takes precedence over [preferredAlphaModes]. */
    val requiredAlphaMode: DawnAlphaMode? = null,
)
