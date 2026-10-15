package org.graphiks.dawn4k.toolkit.surface

import org.graphiks.webgpu.GPUTextureFormat

data class DawnSurfaceConfiguration(
    val format: GPUTextureFormat,
    val alphaMode: DawnAlphaMode,
    val presentMode: DawnPresentMode,
    val extent: PixelExtent,
)
