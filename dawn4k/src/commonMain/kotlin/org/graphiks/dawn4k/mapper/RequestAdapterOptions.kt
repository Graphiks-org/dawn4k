package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.native.WGPUBackendType_Metal
import org.graphiks.dawn4k.native.WGPUBackendType_D3D12
import org.graphiks.dawn4k.native.WGPUBackendType_Undefined
import org.graphiks.dawn4k.native.WGPUBackendType_Vulkan
import org.graphiks.dawn4k.native.WGPUFeatureLevel_Undefined
import org.graphiks.dawn4k.native.WGPUPowerPreference_Undefined
import org.graphiks.dawn4k.native.WGPURequestAdapterOptions
import org.graphiks.dawn4k.native.WGPUSurface
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.webgpu.GPURequestAdapterOptions

/**
 * Allocates a `WGPURequestAdapterOptions` from the public contract options
 * merged with the context's configured backend (the `WGPU_REQUEST_ADAPTER_OPTIONS_INIT`
 * equivalent): every field explicit, using the public enums' native values.
 *
 * - null [options] requests the C defaults (feature level and power
 *   preference undefined, no fallback), with [backend] riding along;
 * - [GPURequestAdapterOptions.featureLevel] supplies its native enum value;
 * - [GPURequestAdapterOptions.xrCompatible] has no native counterpart: the
 *   WebGPU specification lets implementations without XR sessions ignore the
 *   hint, and native Dawn has none — it is accepted and ignored here.
 */
internal fun MemoryAllocator.allocateRequestAdapterOptions(
    options: GPURequestAdapterOptions?,
    backend: DawnBackend?,
    compatibleSurface: WGPUSurface? = null,
): WGPURequestAdapterOptions {
    val native = WGPURequestAdapterOptions.allocate(this)
    native.nextInChain = null
    native.featureLevel = options?.featureLevel?.value ?: WGPUFeatureLevel_Undefined
    native.powerPreference = options?.powerPreference?.value ?: WGPUPowerPreference_Undefined
    native.forceFallbackAdapter = if (options?.forceFallbackAdapter == true) 1u else 0u
    native.backendType = when (backend) {
        null -> WGPUBackendType_Undefined
        DawnBackend.Metal -> WGPUBackendType_Metal
        DawnBackend.Vulkan -> WGPUBackendType_Vulkan
        DawnBackend.D3D12 -> WGPUBackendType_D3D12
    }
    native.compatibleSurface = compatibleSurface
    return native
}
