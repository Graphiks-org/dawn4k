package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.native.WGPUBackendType_Metal
import org.graphiks.dawn4k.native.WGPUBackendType_D3D12
import org.graphiks.dawn4k.native.WGPUBackendType_Undefined
import org.graphiks.dawn4k.native.WGPUBackendType_Vulkan
import org.graphiks.dawn4k.native.WGPUFeatureLevel_Compatibility
import org.graphiks.dawn4k.native.WGPUFeatureLevel_Core
import org.graphiks.dawn4k.native.WGPUFeatureLevel_Undefined
import org.graphiks.dawn4k.native.WGPUPowerPreference_HighPerformance
import org.graphiks.dawn4k.native.WGPUPowerPreference_LowPower
import org.graphiks.dawn4k.native.WGPUPowerPreference_Undefined
import org.graphiks.dawn4k.native.WGPURequestAdapterOptions
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPUPowerPreference
import org.graphiks.webgpu.GPURequestAdapterOptions

/**
 * Allocates a `WGPURequestAdapterOptions` from the public contract options
 * merged with the context's configured backend (the `WGPU_REQUEST_ADAPTER_OPTIONS_INIT`
 * equivalent): every field explicit, never assuming numeric values coincide.
 *
 * - null [options] requests the C defaults (feature level and power
 *   preference undefined, no fallback), with [backend] riding along;
 * - [GPURequestAdapterOptions.featureLevel] "core"/"compatibility" maps
 *   through its named constant; anything else is refused, never guessed;
 * - [GPURequestAdapterOptions.xrCompatible] has no native counterpart: the
 *   WebGPU specification lets implementations without XR sessions ignore the
 *   hint, and native Dawn has none — it is accepted and ignored here.
 */
internal fun MemoryAllocator.allocateRequestAdapterOptions(
    options: GPURequestAdapterOptions?,
    backend: DawnBackend?,
): WGPURequestAdapterOptions {
    val native = WGPURequestAdapterOptions.allocate(this)
    native.nextInChain = null
    native.featureLevel = when (options?.featureLevel) {
        null -> WGPUFeatureLevel_Undefined
        "core" -> WGPUFeatureLevel_Core
        "compatibility" -> WGPUFeatureLevel_Compatibility
        else -> throw IllegalArgumentException(
            "unsupported feature level '${options.featureLevel}' (core or compatibility)",
        )
    }
    native.powerPreference = when (options?.powerPreference) {
        null -> WGPUPowerPreference_Undefined
        GPUPowerPreference.LowPower -> WGPUPowerPreference_LowPower
        GPUPowerPreference.HighPerformance -> WGPUPowerPreference_HighPerformance
    }
    native.forceFallbackAdapter = if (options?.forceFallbackAdapter == true) 1u else 0u
    native.backendType = when (backend) {
        null -> WGPUBackendType_Undefined
        DawnBackend.Metal -> WGPUBackendType_Metal
        DawnBackend.Vulkan -> WGPUBackendType_Vulkan
        DawnBackend.D3D12 -> WGPUBackendType_D3D12
    }
    native.compatibleSurface = null
    return native
}
