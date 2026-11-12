package org.graphiks.dawn4k.webgpu.toolkit

import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.webgpu.toolkit.internal.Dawn4kToolkitInstance
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.WebGpuToolkitExtension

/**
 * Dawn4k implementation of the [WebGpuToolkitExtension] SPI.
 * 
 * This extension provides a headless WebGPU toolkit binding backed by Dawn4k.
 * It creates a [DawnContext] and delegates to [Dawn4kToolkitInstance] for session management.
 */
class Dawn4kWebGpuToolkitExtension : WebGpuToolkitExtension {
    
    override val id: String = "dawn4k"
    
    override fun open(config: SessionConfig): ToolkitInstance {
        // Create a DawnContext for this session
        val context = DawnContext.create()
        return Dawn4kToolkitInstance(context)
    }
}
