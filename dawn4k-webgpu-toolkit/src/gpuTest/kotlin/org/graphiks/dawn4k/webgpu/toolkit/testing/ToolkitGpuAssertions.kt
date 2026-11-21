package org.graphiks.dawn4k.webgpu.toolkit.testing

import kotlinx.coroutines.withContext
import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*
import org.graphiks.webgpu.toolkit.Session
import kotlin.test.*

internal suspend fun <T> assertNoValidationErrors(
    extension: Dawn4kWebGpuToolkitExtension,
    session: Session,
    block: suspend () -> T,
): T {
    val errors = mutableListOf<GPUError>()
    val observation = (session.device as DawnDevice).observeUncapturedErrors { errors += it }
    try {
        val result = block()
        extension.dawnContext(session).processEvents()
        assertTrue(errors.isEmpty(), errors.joinToString { it.message })
        return result
    } finally { observation.close() }
}

internal suspend fun assertComputeDoublesInput(extension: Dawn4kWebGpuToolkitExtension, session: Session) = extension.runGpu(session) {
    assertNoValidationErrors(extension, session) {
        session.device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst)).use { storage ->
            session.device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead)).use { staging ->
                session.queue.writeBuffer(storage, 0uL, ArrayBuffer.of(intArrayOf(1, 2, 3, 4)))
                session.device.createShaderModule(ShaderModuleDescriptor(COMPUTE_SHADER)).use { shader ->
                    session.device.createComputePipeline(ComputePipelineDescriptor(ProgrammableStage(shader, "main"))).use { pipeline ->
                        pipeline.getBindGroupLayout(0u).use { layout ->
                            session.device.createBindGroup(BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, storage)))).use { binding ->
                                (session.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
                                    encoder.beginComputePass().use { pass ->
                                        try { pass.setPipeline(pipeline); pass.setBindGroup(0u, binding); pass.dispatchWorkgroups(4u) }
                                        finally { pass.end() }
                                    }
                                    encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, 16uL)
                                    encoder.finish().use { session.queue.submit(listOf(it)) }
                                }
                                session.queue.onSubmittedWorkDone().getOrThrow()
                                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                                try { assertContentEquals(intArrayOf(2, 4, 6, 8), staging.getMappedRange().toIntArray()) }
                                finally { staging.unmap() }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal suspend fun renderTriangle(extension: Dawn4kWebGpuToolkitExtension, session: Session, view: GPUTextureView, format: GPUTextureFormat) {
    session.device.createShaderModule(ShaderModuleDescriptor(TRIANGLE_SHADER)).use { shader ->
        session.device.createRenderPipeline(RenderPipelineDescriptor(
            vertex = VertexState(module = shader, entryPoint = "vs_main"),
            fragment = FragmentState(module = shader, entryPoint = "fs_main", targets = listOf(ColorTargetState(format))),
        )).use { pipeline ->
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
            session.queue.onSubmittedWorkDone().getOrThrow()
        }
    }
}

internal suspend fun assertOffscreenTrianglePixel(extension: Dawn4kWebGpuToolkitExtension, session: Session) = extension.runGpu(session) {
    assertNoValidationErrors(extension, session) {
        extension.createRenderTexture(session, 4u, 4u, GPUTextureFormat.RGBA8Unorm).use { texture ->
            texture.createView().use { view -> renderTriangle(extension, session, view, GPUTextureFormat.RGBA8Unorm) }
            session.device.createBuffer(BufferDescriptor(1024uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead)).use { staging ->
                (session.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
                    encoder.copyTextureToBuffer(TexelCopyTextureInfo(texture),
                        TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 4u), Extent3D(4u, 4u))
                    encoder.finish().use { session.queue.submit(listOf(it)) }
                }
                session.queue.onSubmittedWorkDone().getOrThrow()
                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                // Copy while mapped. No memory view escapes unmap.
                val pixel = try { staging.getMappedRange().toByteArray().copyOfRange(0, 4) }
                finally { staging.unmap() }
                assertContentEquals(byteArrayOf(-1, 0, 0, -1), pixel)
            }
        }
    }
}

private val COMPUTE_SHADER = """
    @group(0) @binding(0) var<storage, read_write> values: array<u32>;
    @compute @workgroup_size(1)
    fn main(@builtin(global_invocation_id) id: vec3u) { values[id.x] = values[id.x] * 2u; }
""".trimIndent()

private val TRIANGLE_SHADER = """
    @vertex fn vs_main(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
        let points = array<vec2f, 3>(vec2f(-1.0, -1.0), vec2f(3.0, -1.0), vec2f(-1.0, 3.0));
        return vec4f(points[i], 0.0, 1.0);
    }
    @fragment fn fs_main() -> @location(0) vec4f { return vec4f(1.0, 0.0, 0.0, 1.0); }
""".trimIndent()
