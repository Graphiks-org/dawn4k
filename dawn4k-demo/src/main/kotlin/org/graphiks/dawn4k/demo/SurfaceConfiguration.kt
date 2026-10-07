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
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_INT

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
    val values = MemorySegment.ofAddress(address.rawValue).reinterpret(count.toLong() * 4L)
    return List(count.toInt()) { index -> values.get(JAVA_INT, index * 4L).toUInt() }
}

internal fun selectSurfaceConfiguration(
    formats: List<UInt>, alphaModes: List<UInt>, presentModes: List<UInt>,
): SurfaceConfiguration {
    val format = listOf(WGPUTextureFormat_BGRA8Unorm, WGPUTextureFormat_RGBA8Unorm)
        .firstOrNull { it in formats }
        ?: error("surface supports neither BGRA8Unorm nor RGBA8Unorm: $formats")
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
