package org.graphiks.dawn4k

import kotlinx.coroutines.CancellationException
import org.graphiks.dawn4k.internal.DawnRuntime
import org.graphiks.dawn4k.internal.requireWgpuSuccess
import org.graphiks.dawn4k.mapper.allocateAdapterInfoSnapshot
import org.graphiks.dawn4k.mapper.allocateLimitsSnapshot
import org.graphiks.dawn4k.mapper.applyDeviceDescriptor
import org.graphiks.dawn4k.mapper.copyAdapterInfo
import org.graphiks.dawn4k.mapper.copySupportedFeatures
import org.graphiks.dawn4k.mapper.readLimits
import org.graphiks.dawn4k.native.WGPUAdapter
import org.graphiks.dawn4k.native.WGPUSupportedFeatures
import org.graphiks.dawn4k.native.wgpuAdapterGetFeatures
import org.graphiks.dawn4k.native.wgpuAdapterGetInfo
import org.graphiks.dawn4k.native.wgpuAdapterGetLimits
import org.graphiks.dawn4k.native.wgpuAdapterInfoFreeMembers
import org.graphiks.dawn4k.native.wgpuAdapterRelease
import org.graphiks.dawn4k.native.wgpuSupportedFeaturesFreeMembers
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUAdapterInfo
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPUSupportedFeatures
import org.graphiks.webgpu.GPUSupportedLimits

/**
 * A public [GPUAdapter] backed by a Dawn `WGPUAdapter` reference the adapter
 * owns: every [DawnContext.requestAdapter] yields a fresh adapter with its
 * own native reference (no shared cache), and [close] releases exactly that
 * reference, once, on the closing caller.
 *
 * The capability snapshots ([features], [limits], [info]) copy their borrowed
 * native data — the feature list and adapter-info members are freed after the
 * copy — and refuse undefined limits instead of inventing them. A device
 * request acquires the session's own adapter reference, so the caller's
 * reference stays untouched.
 */
class DawnAdapter internal constructor(
    internal val runtime: DawnRuntime,
    internal val handle: WGPUAdapter,
    private val fallback: Boolean,
) : GPUAdapter {

    /** Consumers stop/join adapter users before closing it. */
    private var released = false

    override val features: GPUSupportedFeatures
        get() = run {
            memoryScope { allocator ->
                val supported = WGPUSupportedFeatures.allocate(allocator)
                supported.featureCount = 0uL
                supported.features = null
                wgpuAdapterGetFeatures(handle, supported)
                try {
                    // Copy before the members are freed; the borrowed struct is untouched.
                    copySupportedFeatures(supported)
                } finally {
                    wgpuSupportedFeaturesFreeMembers(supported)
                }
            }
        }

    override val limits: GPUSupportedLimits
        get() = run {
            memoryScope { allocator ->
                val native = allocator.allocateLimitsSnapshot()
                requireWgpuSuccess(wgpuAdapterGetLimits(handle, native), "wgpuAdapterGetLimits")
                readLimits(native)
            }
        }

    override val info: GPUAdapterInfo
        get() = run {
            memoryScope { allocator ->
                val native = allocator.allocateAdapterInfoSnapshot()
                requireWgpuSuccess(wgpuAdapterGetInfo(handle, native), "wgpuAdapterGetInfo")
                try {
                    // Copy before the members are freed; the borrowed views are Kotlin-owned after this.
                    copyAdapterInfo(native, fallback)
                } finally {
                    wgpuAdapterInfoFreeMembers(native)
                }
            }
        }

    /**
     * The raw `WGPUAdapter` pointer of this adapter, for platform integrators
     * (surface bridges). Not part of the WebGPU contract: the handle is valid
     * only while this adapter is open. Callers own synchronization and lifetime.
     */
    fun nativeHandle(): Long = handle.handler.rawValue

    override suspend fun requestDevice(descriptor: GPUDeviceDescriptor?): Result<GPUDevice> = try {
        val session = runtime.openSessionOnAdapter(handle) { native, allocator ->
            native.applyDeviceDescriptor(descriptor, allocator)
        }
        Result.success(DawnDevice(session, descriptor, fallback))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    override fun close() {
        run {
            if (released) return@run
            runtime.requireCanClose(handle.handler.rawValue)
            released = true
            wgpuAdapterRelease(handle)
            runtime.releaseAdapter(handle)
        }
    }
}
