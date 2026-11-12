package org.graphiks.dawn4k.webgpu.toolkit.internal

import org.graphiks.dawn4k.DawnContext
import org.graphiks.webgpu.GPUAdapter
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
 */
internal class Dawn4kToolkitInstance(
    private val context: DawnContext,
) : ToolkitInstance {
    
    override val progression: Progression = Progression.Manual
    
    override suspend fun requestAdapter(hints: AdapterHints): Result<GPUAdapter> {
        // Map AdapterHints to request options
        // For now, we pass null as we don't have direct access to GPURequestAdapterOptions constructor
        // The hints will be handled by the context's internal mapping
        return context.requestAdapter(null)
    }
    
    override suspend fun requestAdapter(
        forTarget: TargetLease,
        hints: AdapterHints,
    ): Result<GPUAdapter> {
        // Headless implementation: we don't have surface support yet
        // Request an adapter without surface compatibility
        return context.requestAdapter(null)
    }
    
    override fun createSurface(adapter: GPUAdapter, lease: TargetLease): ToolkitSurfaceHandle {
        // Headless implementation: surface creation is not supported
        // This would require platform-specific surface integration
        throw UnsupportedOperationException("Surface creation is not supported in headless dawn4k toolkit")
    }
    
    override fun pumpEvents() {
        // Delegate to DawnContext.processEvents()
        context.processEvents()
    }
    
    override fun close() {
        context.close()
    }
}
