package org.graphiks.dawn4k.toolkit.surface.internal

import org.graphiks.dawn4k.toolkit.surface.*
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.kffi.MemoryBuffer
import org.graphiks.kffi.NativeAddress

internal data class SurfaceCapabilities(
    val formats: List<GPUTextureFormat>, val alphaModes: List<DawnAlphaMode>, val presentModes: List<DawnPresentMode>,
) {
    fun select(options: DawnPresentationOptions, extent: PixelExtent) = DawnSurfaceConfiguration(
        choose("format", formats, options.preferredFormats, options.requiredFormat),
        choose("alpha mode", alphaModes, options.preferredAlphaModes, options.requiredAlphaMode),
        choose("present mode", presentModes, options.preferredPresentModes, options.requiredPresentMode),
        extent,
    )
    private fun <T> choose(name: String, supported: List<T>, preferred: List<T>, required: T?): T {
        if (required != null) {
            require(required in supported) { "required $name $required is unavailable; supported: $supported" }
            return required
        }
        return preferred.firstOrNull { it in supported } ?: supported.firstOrNull()
            ?: throw IllegalArgumentException("surface advertises no supported $name")
    }
}

internal fun readSurfaceEnums(address: NativeAddress?, count: ULong): List<UInt> {
    check(count <= 4096uL) { "unbounded surface capability count: $count" }
    if (count == 0uL) return emptyList()
    check(address != null && address.rawValue != 0L) { "null surface capability array" }
    return UIntArray(count.toInt()).also { MemoryBuffer(address, count * 4uL).readUInts(it) }.toList()
}
