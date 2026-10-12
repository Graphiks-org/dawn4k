package org.graphiks.dawn4k

import org.graphiks.webgpu.GPUAdapterInfo

/**
 * A copied adapter-info snapshot, every string Kotlin-owned (copied out of the
 * borrowed native views before the snapshot's members were freed).
 * [isFallbackAdapter] mirrors the request that produced the adapter: this
 * backend only ever yields a fallback adapter when the request forces one, and
 * the native `WGPUAdapterInfo` has no fallback field to read.
 */
class DawnAdapterInfo internal constructor(
    override val vendor: String,
    override val architecture: String,
    override val device: String,
    override val description: String,
    override val subgroupMinSize: UInt,
    override val subgroupMaxSize: UInt,
    override val isFallbackAdapter: Boolean,
) : GPUAdapterInfo
