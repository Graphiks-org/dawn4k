package org.graphiks.dawn4k.toolkit.examples

import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.dawn4k.toolkit.DawnSession
import org.graphiks.dawn4k.toolkit.surface.PixelExtent
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*

/** Create, render and close inside the same session.run lifetime. The destination view is borrowed. */
class TriangleRenderer(private val session: DawnSession, format: GPUTextureFormat) : AutoCloseable {
    private var closed = false
    private val pipeline = session.device.createShaderModule(ShaderModuleDescriptor("""
        @vertex fn vs_main(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
            let points = array<vec2f, 3>(vec2f(-0.8, -0.8), vec2f(0.8, -0.8), vec2f(0.0, 0.8));
            return vec4f(points[i], 0.0, 1.0);
        }
        @fragment fn fs_main() -> @location(0) vec4f { return vec4f(1.0, 0.0, 0.0, 1.0); }
    """.trimIndent())).use { shader ->
        session.device.createRenderPipeline(RenderPipelineDescriptor(
            vertex = VertexState(module = shader, entryPoint = "vs_main"),
            fragment = FragmentState(module = shader, entryPoint = "fs_main", targets = listOf(ColorTargetState(format))),
        ))
    }

    fun render(view: GPUTextureView, extent: PixelExtent) {
        check(!closed) { "triangle renderer is closed" }
        require(extent.width > 0 && extent.height > 0) { "render extent must be positive" }
        (session.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
            encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = listOf(
                RenderPassColorAttachment(view, loadOp = GPULoadOp.Clear, storeOp = GPUStoreOp.Store,
                    clearValue = Color(0.0, 0.0, 1.0, 1.0)),
            ))).use { pass ->
                try { pass.setPipeline(pipeline); pass.draw(3u) }
                finally { pass.end() }
            }
            encoder.finish().use { session.queue.submit(listOf(it)) }
        }
    }

    /** Await submitted work before closing, while the owning session remains admitted. */
    override fun close() { if (!closed) { pipeline.close(); closed = true } }
}
