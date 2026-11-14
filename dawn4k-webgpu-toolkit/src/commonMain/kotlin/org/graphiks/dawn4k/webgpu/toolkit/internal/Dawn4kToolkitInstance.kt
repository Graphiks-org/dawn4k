package org.graphiks.dawn4k.webgpu.toolkit.internal

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import org.graphiks.dawn4k.DawnAdapter
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kTargetLease
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUFeatureLevel
import org.graphiks.webgpu.GPURequestAdapterOptions
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Progression
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.ToolkitSurfaceHandle

/**
 * Dawn4k implementation of [ToolkitInstance].
 * 
 * This instance manages a [DawnContext] and provides WebGPU toolkit functionality.
 * Handles are borrowed from the context and never closed by callers.
 * The instance tracks all adapters it creates and closes them on close().
 */
internal class Dawn4kToolkitInstance(
    private val context: DawnContext,
) : ToolkitInstance {
    
    // Guarded by adaptersLock: requestAdapter is suspend and may be called concurrently,
    // while close() iterates and clears the list from a non-suspend context.
    private val adaptersLock = SynchronizedObject()
    private val adapters = mutableListOf<DawnAdapter>()

    override val progression: Progression = Progression.Pumped

    override suspend fun requestAdapter(hints: AdapterHints): Result<GPUAdapter> {
        val options = hints.toRequestAdapterOptions()
        val result = context.requestAdapter(options)
        result.onSuccess { adapter ->
            if (adapter is DawnAdapter) {
                synchronized(adaptersLock) { adapters.add(adapter) }
            }
        }
        return result
    }
    
    override suspend fun requestAdapter(
        forTarget: TargetLease,
        hints: AdapterHints,
    ): Result<GPUAdapter> {
        // A dawn4k surface reference belongs to the context that created it, so a target-bound
        // adapter would be bound to a surface this context cannot present to. The surface handle
        // opens its own presentation on the retained target instead, and the adapter is chosen
        // for the device that presentation will use.
        val result = context.requestAdapter(hints.toRequestAdapterOptions())

        result.onSuccess { adapter ->
            if (adapter is DawnAdapter) {
                synchronized(adaptersLock) { adapters.add(adapter) }
            }
        }
        return result
    }

    override fun createSurface(adapter: GPUAdapter, lease: TargetLease): ToolkitSurfaceHandle {
        // Only Dawn4kTargetLease is supported for surface creation
        require(lease is Dawn4kTargetLease) {
            "Only Dawn4kTargetLease is supported for surface creation"
        }
        
        // The handle opens its own dawn4k presentation on the retained target when it acquires
        // its first frame; the configured device is the one the toolkit negotiated.
        val surfaceHandle = Dawn4kSurfaceHandle(
            surfaceTarget = lease.surfaceTarget
        )
        
        return surfaceHandle
    }
    
    override fun pumpEvents() {
        // Delegate to DawnContext.processEvents()
        context.processEvents()
    }
    
    override fun close() {
        // Close all tracked adapters before closing the context
        val tracked = synchronized(adaptersLock) {
            adapters.toList().also { adapters.clear() }
        }
        tracked.forEach { adapter ->
            runCatching { adapter.close() }
        }
        context.close()
    }
}

/**
 * Maps [AdapterHints] to [GPURequestAdapterOptions].
 * Creates an object implementing the GPURequestAdapterOptions interface with
 * the properties from AdapterHints.
 */
private fun AdapterHints.toRequestAdapterOptions(): GPURequestAdapterOptions? {
    if (powerPreference == null && forceFallbackAdapter == null) {
        return null
    }
    return object : GPURequestAdapterOptions {
        override val powerPreference = this@toRequestAdapterOptions.powerPreference
        override val forceFallbackAdapter: Boolean = this@toRequestAdapterOptions.forceFallbackAdapter ?: false
        // Use Core as default feature level when hints don't specify it
        override val featureLevel: GPUFeatureLevel = org.graphiks.webgpu.GPUFeatureLevel.Core
        override val xrCompatible: Boolean = false
    }
}
