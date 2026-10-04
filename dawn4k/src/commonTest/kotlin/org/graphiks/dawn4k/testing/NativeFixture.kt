package org.graphiks.dawn4k.testing

import org.graphiks.dawn4k.DawnBindGroup
import org.graphiks.dawn4k.DawnBuffer
import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.dawn4k.DawnComputePipeline
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnQuerySet
import org.graphiks.dawn4k.DawnQueue
import org.graphiks.dawn4k.DawnRenderBundleEncoder
import org.graphiks.dawn4k.DawnRenderPipeline
import org.graphiks.dawn4k.DawnSampler
import org.graphiks.dawn4k.DawnShaderModule
import org.graphiks.dawn4k.DawnTexture
import org.graphiks.dawn4k.createBindGroup
import org.graphiks.dawn4k.createBuffer
import org.graphiks.dawn4k.createCommandEncoder
import org.graphiks.dawn4k.createComputePipeline
import org.graphiks.dawn4k.createQuerySet
import org.graphiks.dawn4k.createRenderBundleEncoder
import org.graphiks.dawn4k.createRenderPipeline
import org.graphiks.dawn4k.createSampler
import org.graphiks.dawn4k.createShaderModule
import org.graphiks.dawn4k.createTexture
import org.graphiks.dawn4k.internal.DawnRuntime
import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.onSubmittedWorkDone
import org.graphiks.webgpu.GPUBufferDescriptor
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUComputePipelineDescriptor
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUQuerySetDescriptor
import org.graphiks.webgpu.GPURenderBundleEncoderDescriptor
import org.graphiks.webgpu.GPURenderPipelineDescriptor
import org.graphiks.webgpu.GPUSamplerDescriptor
import org.graphiks.webgpu.GPUShaderModuleDescriptor
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureDescriptor
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import kotlin.test.assertContentEquals

/**
 * One raw GPU session for the tests of this module: a [DawnRuntime] and one
 * already-opened [DeviceSession], closed together in reverse order. The command
 * paths (copies, dispatches, renders) go through the public wrappers added by
 * the command-surface task; the raw Dawn bindings remain available to the
 * wrappers themselves, but are no longer exercised here.
 */
internal class NativeFixture(
    internal val runtime: DawnRuntime,
    internal val session: DeviceSession,
) : AutoCloseable {

    /** The session's owned queue, wrapped. */
    internal val queue: DawnQueue
        get() = session.queue

    companion object {
        suspend fun open(): NativeFixture {
            val runtime = DawnRuntime(DawnConfig())
            val session = runtime.openSession()
            return NativeFixture(runtime, session)
        }
    }

    /** Creates a raw [DawnBuffer] on the fixture's session. */
    fun createBuffer(descriptor: GPUBufferDescriptor): DawnBuffer =
        session.createBuffer(descriptor)

    /** Creates a raw [DawnShaderModule] on the fixture's session. */
    fun createShaderModule(descriptor: GPUShaderModuleDescriptor): DawnShaderModule =
        session.createShaderModule(descriptor)

    /** Creates a raw [DawnComputePipeline] on the fixture's session. */
    fun createComputePipeline(descriptor: GPUComputePipelineDescriptor): DawnComputePipeline =
        session.createComputePipeline(descriptor)

    /** Creates a raw [DawnTexture] on the fixture's session. */
    fun createTexture(descriptor: GPUTextureDescriptor): DawnTexture =
        session.createTexture(descriptor)

    /** Creates a raw [DawnSampler] on the fixture's session. */
    fun createSampler(descriptor: GPUSamplerDescriptor): DawnSampler =
        session.createSampler(descriptor)

    /** Creates a raw [DawnRenderPipeline] on the fixture's session. */
    fun createRenderPipeline(descriptor: GPURenderPipelineDescriptor): DawnRenderPipeline =
        session.createRenderPipeline(descriptor)

    /** Creates a raw [DawnQuerySet] on the fixture's session. */
    fun createQuerySet(descriptor: GPUQuerySetDescriptor): DawnQuerySet =
        session.createQuerySet(descriptor)

    /** Creates a command encoder on the fixture's session. */
    fun createEncoder(): DawnCommandEncoder = session.createCommandEncoder()

    /** Creates a render bundle encoder on the fixture's session. */
    fun createRenderBundleEncoder(descriptor: GPURenderBundleEncoderDescriptor): DawnRenderBundleEncoder =
        session.createRenderBundleEncoder(descriptor)

    /**
     * Finishes [encoder] into a command buffer, submits it on the fixture's
     * queue, and releases the command buffer.
     */
    fun submit(encoder: DawnCommandEncoder) {
        val commandBuffer = encoder.finish()
        try {
            queue.submit(listOf(commandBuffer))
        } finally {
            commandBuffer.close()
        }
    }

    /**
     * Standalone render oracle: draws a full-screen red triangle into a 4x4
     * texture of [format], copies it (bytesPerRow=256) into a staging buffer, and
     * returns the first pixel (RGBA8 = 4 bytes) with the row padding removed. The
     * render pass stores (it is the fragment shader, not a clear, that produces
     * the readback color). The temporary attachment view is created by
     * [DawnCommandEncoder.beginRenderPass] for the [GPUTexture] attachment and
     * released when the pass ends.
     */
    suspend fun renderPixel(format: GPUTextureFormat): ByteArray {
        val texture = createTexture(
            TextureDescriptor(
                size = Extent3D(width = 4u, height = 4u),
                format = format,
                usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
            ),
        )
        // 256 bytes/row * 4 rows: the Metal copy-buffer row alignment.
        val staging = createBuffer(BufferDescriptor(1024uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
        try {
            createShaderModule(ShaderModuleDescriptor(RENDER_SHADER)).use { shader ->
                createRenderPipeline(
                    RenderPipelineDescriptor(
                        vertex = VertexState(module = shader, entryPoint = "vs_main"),
                        fragment = FragmentState(
                            module = shader,
                            entryPoint = "fs_main",
                            targets = listOf(ColorTargetState(format = format)),
                        ),
                    ),
                ).use { pipeline ->
                    createEncoder().use { encoder ->
                        encoder.beginRenderPass(
                            RenderPassDescriptor(
                                colorAttachments = listOf(
                                    RenderPassColorAttachment(
                                        view = texture,
                                        loadOp = GPULoadOp.Clear,
                                        storeOp = GPUStoreOp.Store,
                                        clearValue = Color(0.0, 0.0, 1.0, 1.0),
                                    ),
                                ),
                            ),
                        ).let { pass ->
                            pass.setPipeline(pipeline)
                            pass.draw(3u)
                            pass.end()
                        }
                        encoder.copyTextureToBuffer(
                            source = TexelCopyTextureInfo(texture),
                            destination = TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 4u),
                            copySize = Extent3D(width = 4u, height = 4u),
                        )
                        submit(encoder)
                    }
                    session.onSubmittedWorkDone().getOrThrow()
                    staging.mapAsync(GPUMapMode.Read).getOrThrow()
                    val bytes = staging.getMappedRange().toByteArray()
                    staging.unmap()
                    return bytes.copyOfRange(0, 4)
                }
            }
        } finally {
            texture.close()
            staging.close()
        }
    }

    /**
     * Buffer-to-buffer copy via the public command encoder and queue, submitted
     * and awaited to completion.
     */
    suspend fun copyAndSubmit(source: DawnBuffer, destination: DawnBuffer, size: ULong) {
        createEncoder().use { encoder ->
            encoder.copyBufferToBuffer(source, 0uL, destination, 0uL, size)
            submit(encoder)
        }
        session.onSubmittedWorkDone().getOrThrow()
    }

    /**
     * Compute dispatch via the public wrappers: a compute pass that sets
     * [pipeline] and [bindGroup], dispatches [workgroupCountX] workgroups, copies
     * [storage] into [staging], submits, and waits for the work to complete. The
     * caller maps and reads [staging] afterwards.
     */
    suspend fun dispatchWorkgroups(
        pipeline: DawnComputePipeline,
        bindGroup: DawnBindGroup,
        storage: DawnBuffer,
        staging: DawnBuffer,
        workgroupCountX: UInt,
        dynamicOffsets: UIntArray = uintArrayOf(),
    ) {
        createEncoder().use { encoder ->
            encoder.beginComputePass().let { pass ->
                pass.setPipeline(pipeline)
                pass.setBindGroup(0u, bindGroup, dynamicOffsets.toList())
                pass.dispatchWorkgroups(workgroupCountX)
                pass.end()
            }
            encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, storage.size)
            submit(encoder)
        }
        session.onSubmittedWorkDone().getOrThrow()
    }

    /**
     * Standalone compute oracle: a 16-byte storage buffer bound at the pipeline's
     * own group 0 layout, 4 workgroups, copy to a 16-byte staging buffer, map and
     * compare against [expected]. The temporary bind-group layout and bind group
     * are released before returning.
     */
    suspend fun dispatchAndReadback(pipeline: DawnComputePipeline, expected: UIntArray) {
        val storage = createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc))
        val staging = createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
        try {
            pipeline.getBindGroupLayout(0u).use { layout ->
                session.createBindGroup(BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, storage)))).use { bindGroup ->
                    dispatchWorkgroups(pipeline, bindGroup, storage, staging, workgroupCountX = 4u)
                    staging.mapAsync(GPUMapMode.Read).getOrThrow()
                    assertContentEquals(expected, staging.getMappedRange().toUIntArray())
                    staging.unmap()
                }
            }
        } finally {
            storage.close()
            staging.close()
        }
    }

    override fun close() {
        session.close()
        runtime.close()
    }
}

/**
 * A full-screen triangle: the vertex shader synthesizes the three vertices from
 * the vertex index (no vertex buffer), and the fragment shader outputs opaque red.
 */
private val RENDER_SHADER = """
    @vertex
    fn vs_main(@builtin(vertex_index) vertexIndex: u32) -> @builtin(position) vec4f {
        var positions = array<vec2f, 3>(
            vec2f(-1.0, -1.0),
            vec2f( 3.0, -1.0),
            vec2f(-1.0,  3.0),
        );
        return vec4f(positions[vertexIndex], 0.0, 1.0);
    }

    @fragment
    fn fs_main() -> @location(0) vec4f {
        return vec4f(1.0, 0.0, 0.0, 1.0);
    }
""".trimIndent()
