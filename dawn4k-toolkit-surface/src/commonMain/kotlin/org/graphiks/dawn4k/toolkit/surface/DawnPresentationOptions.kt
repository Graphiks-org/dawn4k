package org.graphiks.dawn4k.toolkit.surface

import org.graphiks.dawn4k.native.*
import org.graphiks.webgpu.GPUTextureFormat

enum class DawnPresentMode(internal val nativeValue: UInt) {
    Fifo(WGPUPresentMode_Fifo), FifoRelaxed(WGPUPresentMode_FifoRelaxed),
    Immediate(WGPUPresentMode_Immediate), Mailbox(WGPUPresentMode_Mailbox),
}
enum class DawnAlphaMode(internal val nativeValue: UInt) {
    Auto(WGPUCompositeAlphaMode_Auto), Opaque(WGPUCompositeAlphaMode_Opaque),
    Premultiplied(WGPUCompositeAlphaMode_Premultiplied), Unpremultiplied(WGPUCompositeAlphaMode_Unpremultiplied),
    Inherit(WGPUCompositeAlphaMode_Inherit),
}

data class DawnPresentationOptions(
    val preferredFormats: List<GPUTextureFormat> = listOf(GPUTextureFormat.BGRA8Unorm, GPUTextureFormat.RGBA8Unorm),
    val requiredFormat: GPUTextureFormat? = null,
    val preferredPresentModes: List<DawnPresentMode> = listOf(DawnPresentMode.Fifo),
    val requiredPresentMode: DawnPresentMode? = null,
    val preferredAlphaModes: List<DawnAlphaMode> = listOf(
        DawnAlphaMode.Auto, DawnAlphaMode.Opaque, DawnAlphaMode.Premultiplied, DawnAlphaMode.Unpremultiplied, DawnAlphaMode.Inherit,
    ),
    val requiredAlphaMode: DawnAlphaMode? = null,
)
