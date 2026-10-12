package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.internal.copyToString
import org.graphiks.dawn4k.native.WGPUAdapterInfo
import org.graphiks.dawn4k.native.WGPUBackendType_Undefined
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.dawn4k.DawnAdapterInfo

/**
 * Allocates a read-ready adapter-info snapshot: the `WGPU_ADAPTER_INFO_INIT`
 * equivalent (no chain, empty string views, zeroed scalars) so a field C does
 * not fill stays visibly empty rather than reading allocator garbage.
 */
internal fun MemoryAllocator.allocateAdapterInfoSnapshot(): WGPUAdapterInfo {
    val info = WGPUAdapterInfo.allocate(this)
    info.nextInChain = null
    info.vendor.data = null
    info.vendor.length = WGPU_STRLEN
    info.architecture.data = null
    info.architecture.length = WGPU_STRLEN
    info.device.data = null
    info.device.length = WGPU_STRLEN
    info.description.data = null
    info.description.length = WGPU_STRLEN
    info.backendType = WGPUBackendType_Undefined
    info.adapterType = 0u
    info.vendorID = 0u
    info.deviceID = 0u
    info.subgroupMinSize = 0u
    info.subgroupMaxSize = 0u
    return info
}

/**
 * Copies the borrowed adapter-info snapshot into a Kotlin-owned
 * [DawnAdapterInfo]: every string is copied (the native views are owned by the
 * snapshot and freed by the caller with `wgpuAdapterInfoFreeMembers` once the
 * copy is done), never aliased.
 */
internal fun copyAdapterInfo(info: WGPUAdapterInfo, isFallbackAdapter: Boolean): DawnAdapterInfo =
    DawnAdapterInfo(
        vendor = info.vendor.copyToString(),
        architecture = info.architecture.copyToString(),
        device = info.device.copyToString(),
        description = info.description.copyToString(),
        subgroupMinSize = info.subgroupMinSize,
        subgroupMaxSize = info.subgroupMaxSize,
        isFallbackAdapter = isFallbackAdapter,
    )
