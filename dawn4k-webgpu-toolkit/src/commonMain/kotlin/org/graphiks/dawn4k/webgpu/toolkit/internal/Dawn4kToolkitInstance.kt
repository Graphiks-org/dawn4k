package org.graphiks.dawn4k.webgpu.toolkit.internal

import org.graphiks.dawn4k.DawnAdapter
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kTargetLease
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kSurfaceTarget
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
    
    private val adapters = mutableListOf<DawnAdapter>()
    private val surfaces = mutableListOf<Dawn4kSurfaceTarget>()
    
    override val progression: Progression = Progression.Manual
    
    override suspend fun requestAdapter(hints: AdapterHints): Result<GPUAdapter> {
        val options = hints.toRequestAdapterOptions()
        val result = context.requestAdapter(options)
        result.onSuccess { adapter ->
            if (adapter is DawnAdapter) {
                adapters.add(adapter)
            }
        }
        return result
    }
    
    override suspend fun requestAdapter(
        forTarget: TargetLease,
        hints: AdapterHints,
    ): Result<GPUAdapter> {
        // For Dawn4kSurfaceTarget leases, use the surface reference for adapter selection
        val surfaceReference = when (forTarget) {
            is Dawn4kTargetLease -> forTarget.surfaceReference
            else -> null
        }
        
        val options = hints.toRequestAdapterOptions()
        val result = if (surfaceReference != null) {
            context.requestAdapter(options, surfaceReference)
        } else {
            context.requestAdapter(options)
        }
        
        result.onSuccess { adapter ->
            if (adapter is DawnAdapter) {
                adapters.add(adapter)
            }
        }
        return result
    }
    
    override fun createSurface(adapter: GPUAdapter, lease: TargetLease): ToolkitSurfaceHandle {
        // Only Dawn4kTargetLease is supported for surface creation
        require(lease is Dawn4kTargetLease) {
            "Only Dawn4kTargetLease is supported for surface creation"
        }
        
        // Create a surface handle for the target
        // The device will be provided during configure()
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
        adapters.forEach { adapter ->
            runCatching { adapter.close() }
        }
        adapters.clear()
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
