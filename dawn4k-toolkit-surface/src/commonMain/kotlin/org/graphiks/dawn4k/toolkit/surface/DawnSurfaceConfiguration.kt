package org.graphiks.dawn4k.toolkit.surface

import org.graphiks.webgpu.GPUTextureFormat

/**
 * A surface's negotiated configuration, published by `DawnPresentation.configuration`
 * whenever the active target reconfigures.
 */
data class DawnSurfaceConfiguration(
    /** Frame texture format; a pipeline rendering into frames must match it. */
    val format: GPUTextureFormat,
    /** How the compositor blends presented frames with the content behind the target. */
    val alphaMode: DawnAlphaMode,
    /** When presented frames become visible relative to display refresh. */
    val presentMode: DawnPresentMode,
    /** Frame size in physical pixels; matches the extent of frames acquired from the target. */
    val extent: PixelExtent,
)
