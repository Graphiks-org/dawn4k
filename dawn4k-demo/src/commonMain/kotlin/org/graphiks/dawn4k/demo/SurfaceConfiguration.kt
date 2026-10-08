package org.graphiks.dawn4k.demo

import org.graphiks.dawn4k.native.WGPUCompositeAlphaMode_Auto
import org.graphiks.dawn4k.native.WGPUCompositeAlphaMode_Opaque
import org.graphiks.dawn4k.native.WGPUCompositeAlphaMode_Premultiplied
import org.graphiks.dawn4k.native.WGPUCompositeAlphaMode_Unpremultiplied
import org.graphiks.dawn4k.native.WGPUCompositeAlphaMode_Inherit
import org.graphiks.dawn4k.native.WGPUPresentMode_Fifo
import org.graphiks.dawn4k.native.WGPUTextureFormat_BGRA8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA8Unorm
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.MemoryBuffer

internal data class SurfaceConfiguration(
    val textureFormat: GPUTextureFormat,
    val nativeFormat: UInt,
    val alphaMode: UInt,
    val presentMode: UInt,
)

/** Copy native enum arrays before Dawn's capability members are freed. */
internal fun readSurfaceEnums(address: NativeAddress?, count: ULong): List<UInt> {
    check(count <= 4096uL) { "surface capability count is unbounded: $count" }
    if (count == 0uL) return emptyList()
    check(address != null && address.rawValue != 0L) { "surface capability array is null with count=$count" }
    val values = UIntArray(count.toInt())
    MemoryBuffer(address, count * 4uL).readUInts(values)
    return values.toList()
}

internal fun selectSurfaceConfiguration(
    formats: List<UInt>, alphaModes: List<UInt>, presentModes: List<UInt>,
    requiredFormat: GPUTextureFormat? = null,
): SurfaceConfiguration {
    val candidates = when (requiredFormat) {
        null -> listOf(WGPUTextureFormat_BGRA8Unorm, WGPUTextureFormat_RGBA8Unorm)
        GPUTextureFormat.BGRA8Unorm -> listOf(WGPUTextureFormat_BGRA8Unorm)
        GPUTextureFormat.RGBA8Unorm -> listOf(WGPUTextureFormat_RGBA8Unorm)
        else -> error("unsupported required surface format: $requiredFormat")
    }
    val format = candidates
        .firstOrNull { it in formats }
        ?: error("surface cannot preserve required format $requiredFormat (supported: $formats)")
    check(WGPUPresentMode_Fifo in presentModes) { "surface does not advertise Fifo presentation" }
    val alpha = listOf(WGPUCompositeAlphaMode_Auto, WGPUCompositeAlphaMode_Opaque,
        WGPUCompositeAlphaMode_Premultiplied, WGPUCompositeAlphaMode_Unpremultiplied,
        WGPUCompositeAlphaMode_Inherit).firstOrNull { it in alphaModes }
        ?: error("surface has no supported alpha mode: $alphaModes")
    return SurfaceConfiguration(
        if (format == WGPUTextureFormat_BGRA8Unorm) GPUTextureFormat.BGRA8Unorm else GPUTextureFormat.RGBA8Unorm,
        format, alpha, WGPUPresentMode_Fifo,
    )
}
