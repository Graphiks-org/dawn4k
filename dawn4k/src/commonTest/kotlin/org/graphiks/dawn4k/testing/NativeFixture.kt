package org.graphiks.dawn4k.testing

import org.graphiks.dawn4k.DawnBindGroup
import org.graphiks.dawn4k.DawnBuffer
import org.graphiks.dawn4k.DawnComputePipeline
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnQuerySet
import org.graphiks.dawn4k.DawnRenderPipeline
import org.graphiks.dawn4k.DawnSampler
import org.graphiks.dawn4k.DawnShaderModule
import org.graphiks.dawn4k.DawnTexture
import org.graphiks.dawn4k.DawnTextureView
import org.graphiks.dawn4k.createBindGroup
import org.graphiks.dawn4k.createBuffer
import org.graphiks.dawn4k.createComputePipeline
import org.graphiks.dawn4k.createQuerySet
import org.graphiks.dawn4k.createRenderPipeline
import org.graphiks.dawn4k.createSampler
import org.graphiks.dawn4k.createShaderModule
import org.graphiks.dawn4k.createTexture
import org.graphiks.dawn4k.internal.DawnRuntime
import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.mapper.initNull
import org.graphiks.dawn4k.native.WGPUExtent3D
import org.graphiks.dawn4k.native.WGPULoadOp_Clear
import org.graphiks.dawn4k.native.WGPURenderPassColorAttachment
import org.graphiks.dawn4k.native.WGPURenderPassDescriptor
import org.graphiks.dawn4k.native.WGPUStoreOp_Store
import org.graphiks.dawn4k.native.WGPUTexelCopyBufferInfo
import org.graphiks.dawn4k.native.WGPUTexelCopyTextureInfo
import org.graphiks.dawn4k.native.WGPUTextureAspect_All
import org.graphiks.dawn4k.native.wgpuCommandBufferRelease
import org.graphiks.dawn4k.native.wgpuCommandEncoderBeginComputePass
import org.graphiks.dawn4k.native.wgpuCommandEncoderBeginRenderPass
import org.graphiks.dawn4k.native.wgpuCommandEncoderCopyBufferToBuffer
import org.graphiks.dawn4k.native.wgpuCommandEncoderCopyTextureToBuffer
import org.graphiks.dawn4k.native.wgpuCommandEncoderFinish
import org.graphiks.dawn4k.native.wgpuCommandEncoderRelease
import org.graphiks.dawn4k.native.wgpuComputePassEncoderDispatchWorkgroups
import org.graphiks.dawn4k.native.wgpuComputePassEncoderEnd
import org.graphiks.dawn4k.native.wgpuComputePassEncoderRelease
import org.graphiks.dawn4k.native.wgpuComputePassEncoderSetBindGroup
import org.graphiks.dawn4k.native.wgpuComputePassEncoderSetPipeline
import org.graphiks.dawn4k.native.wgpuDeviceCreateCommandEncoder
import org.graphiks.dawn4k.native.wgpuQueueSubmit
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderDraw
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderEnd
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderRelease
import org.graphiks.dawn4k.native.wgpuRenderPassEncoderSetPipeline
import org.graphiks.dawn4k.onSubmittedWorkDone
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUBufferDescriptor
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUComputePipelineDescriptor
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUQuerySetDescriptor
import org.graphiks.webgpu.GPURenderPipelineDescriptor
import org.graphiks.webgpu.GPUSamplerDescriptor
import org.graphiks.webgpu.GPUShaderModuleDescriptor
import org.graphiks.webgpu.GPUTextureDescriptor
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import kotlin.test.assertContentEquals

/**
 * One raw GPU session for the tests of this module: a [DawnRuntime] and one
 * already-opened [DeviceSession], closed together in reverse order. Later tasks
 * add their raw creation helpers here as they need them for their tests.
 */
internal class NativeFixture(
    internal val runtime: DawnRuntime,
    internal val session: DeviceSession,
) : AutoCloseable {

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

    /**
     * Standalone render oracle: draws a full-screen red triangle into a 4x4
     * texture of [format], copies it (bytesPerRow=256) into a staging buffer, and
     * returns the first pixel (RGBA8 = 4 bytes) with the row padding removed. The
     * render pass stores (it is the fragment shader, not a clear, that produces
     * the readback color). The temporary attachment view is an owned reference
     * kept for the encoding and released to the session before returning.
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
                    texture.createView(null).use { view ->
                        renderAndCopy(texture, view as DawnTextureView, pipeline, staging)
                    }
                }
            }
            staging.mapAsync(GPUMapMode.Read).getOrThrow()
            val bytes = staging.getMappedRange().toByteArray()
            staging.unmap()
            return bytes.copyOfRange(0, 4)
        } finally {
            texture.close()
            staging.close()
        }
    }

    /**
     * Encodes a render pass (store to [texture]) drawing [pipeline] once, then a
     * texture-to-buffer copy into [staging] with bytesPerRow=256, submits, and
     * waits for the work to complete. The attachment view stays alive through the
     * encoding (caller-owned) and is released by its caller afterwards.
     */
    private suspend fun renderAndCopy(
        texture: DawnTexture,
        view: DawnTextureView,
        pipeline: DawnRenderPipeline,
        staging: DawnBuffer,
    ) {
        session.runtime.dispatcher.awaitCall {
            memoryScope { allocator ->
                val encoder = wgpuDeviceCreateCommandEncoder(session.handle, null)
                    ?: throw IllegalStateException("wgpuDeviceCreateCommandEncoder returned no encoder")
                try {
                    val colorAttachment = WGPURenderPassColorAttachment.allocate(allocator)
                    colorAttachment.nextInChain = null
                    colorAttachment.view = view.handle
                    // WGPU_DEPTH_SLICE_UNDEFINED: only 3D attachments define a depth slice.
                    colorAttachment.depthSlice = 0xFFFFFFFFu
                    colorAttachment.resolveTarget = null
                    colorAttachment.loadOp = WGPULoadOp_Clear
                    colorAttachment.storeOp = WGPUStoreOp_Store
                    // Clear to blue, distinct from the red the fragment shader writes,
                    // so the oracle proves the draw rather than the clear color.
                    colorAttachment.clearValue.r = 0.0
                    colorAttachment.clearValue.g = 0.0
                    colorAttachment.clearValue.b = 1.0
                    colorAttachment.clearValue.a = 1.0

                    val renderPass = WGPURenderPassDescriptor.allocate(allocator)
                    renderPass.nextInChain = null
                    renderPass.label.initNull()
                    renderPass.colorAttachmentCount = 1uL
                    renderPass.colorAttachments = WGPURenderPassColorAttachment(colorAttachment.handler)
                    renderPass.depthStencilAttachment = null
                    renderPass.occlusionQuerySet = null
                    renderPass.timestampWrites = null

                    val pass = wgpuCommandEncoderBeginRenderPass(encoder, renderPass)
                        ?: throw IllegalStateException("wgpuCommandEncoderBeginRenderPass returned no pass")
                    try {
                        wgpuRenderPassEncoderSetPipeline(pass, pipeline.handle)
                        wgpuRenderPassEncoderDraw(pass, 3u, 1u, 0u, 0u)
                        wgpuRenderPassEncoderEnd(pass)
                    } finally {
                        wgpuRenderPassEncoderRelease(pass)
                    }

                    val source = WGPUTexelCopyTextureInfo.allocate(allocator)
                    source.texture = texture.handle
                    source.mipLevel = 0u
                    source.origin.x = 0u
                    source.origin.y = 0u
                    source.origin.z = 0u
                    source.aspect = WGPUTextureAspect_All

                    val destination = WGPUTexelCopyBufferInfo.allocate(allocator)
                    destination.layout.offset = 0uL
                    destination.layout.bytesPerRow = 256u
                    destination.layout.rowsPerImage = 4u
                    destination.buffer = staging.handle

                    val copySize = WGPUExtent3D.allocate(allocator)
                    copySize.width = 4u
                    copySize.height = 4u
                    copySize.depthOrArrayLayers = 1u

                    wgpuCommandEncoderCopyTextureToBuffer(encoder, source, destination, copySize)

                    val commandBuffer = wgpuCommandEncoderFinish(encoder, null)
                        ?: throw IllegalStateException("wgpuCommandEncoderFinish returned no command buffer")
                    try {
                        val commands = allocator.bufferOfAddresses(listOf(commandBuffer.handler))
                        wgpuQueueSubmit(session.queueHandle, 1uL, commands.handler)
                    } finally {
                        wgpuCommandBufferRelease(commandBuffer)
                    }
                } finally {
                    wgpuCommandEncoderRelease(encoder)
                }
            }
        }
        session.onSubmittedWorkDone().getOrThrow()
    }

    /**
     * Raw buffer-to-buffer copy: encode on the device, finish, submit on the
     * session's queue, and wait for the submitted work to complete. This is the
     * pre-CommandEncoder-wrapper path (Task 6 replaces it with the public API).
     */
    suspend fun copyAndSubmit(source: DawnBuffer, destination: DawnBuffer, size: ULong) {
        session.runtime.dispatcher.awaitCall {
            memoryScope { allocator ->
                val encoder = wgpuDeviceCreateCommandEncoder(session.handle, null)
                    ?: throw IllegalStateException("wgpuDeviceCreateCommandEncoder returned no encoder")
                try {
                    wgpuCommandEncoderCopyBufferToBuffer(
                        encoder,
                        source.handle,
                        0uL,
                        destination.handle,
                        0uL,
                        size,
                    )
                    val commandBuffer = wgpuCommandEncoderFinish(encoder, null)
                        ?: throw IllegalStateException("wgpuCommandEncoderFinish returned no command buffer")
                    try {
                        val commands = allocator.bufferOfAddresses(listOf(commandBuffer.handler))
                        wgpuQueueSubmit(session.queueHandle, 1uL, commands.handler)
                    } finally {
                        wgpuCommandBufferRelease(commandBuffer)
                    }
                } finally {
                    wgpuCommandEncoderRelease(encoder)
                }
            }
        }
        session.onSubmittedWorkDone().getOrThrow()
    }

    /**
     * Raw compute dispatch: encode a compute pass that sets [pipeline] and
     * [bindGroup], dispatches [workgroupCountX] workgroups, copies [storage] into
     * [staging], submits, and waits for the work to complete. The caller maps and
     * reads [staging] afterwards.
     */
    suspend fun dispatchWorkgroups(
        pipeline: DawnComputePipeline,
        bindGroup: DawnBindGroup,
        storage: DawnBuffer,
        staging: DawnBuffer,
        workgroupCountX: UInt,
        dynamicOffsets: UIntArray = uintArrayOf(),
    ) {
        session.runtime.dispatcher.awaitCall {
            memoryScope { allocator ->
                val encoder = wgpuDeviceCreateCommandEncoder(session.handle, null)
                    ?: throw IllegalStateException("wgpuDeviceCreateCommandEncoder returned no encoder")
                try {
                    val pass = wgpuCommandEncoderBeginComputePass(encoder, null)
                        ?: throw IllegalStateException("wgpuCommandEncoderBeginComputePass returned no pass")
                    try {
                        wgpuComputePassEncoderSetPipeline(pass, pipeline.handle)
                        val dynamicOffsetsAddress: NativeAddress? = if (dynamicOffsets.isEmpty()) {
                            null
                        } else {
                            allocator.allocateBuffer((dynamicOffsets.size * 4).toULong()).apply {
                                writeUInts(dynamicOffsets)
                            }.handler
                        }
                        wgpuComputePassEncoderSetBindGroup(
                            pass, 0u, bindGroup.handle, dynamicOffsets.size.toULong(), dynamicOffsetsAddress,
                        )
                        wgpuComputePassEncoderDispatchWorkgroups(pass, workgroupCountX, 1u, 1u)
                        wgpuComputePassEncoderEnd(pass)
                    } finally {
                        wgpuComputePassEncoderRelease(pass)
                    }
                    wgpuCommandEncoderCopyBufferToBuffer(encoder, storage.handle, 0uL, staging.handle, 0uL, storage.size)
                    val commandBuffer = wgpuCommandEncoderFinish(encoder, null)
                        ?: throw IllegalStateException("wgpuCommandEncoderFinish returned no command buffer")
                    try {
                        val commands = allocator.bufferOfAddresses(listOf(commandBuffer.handler))
                        wgpuQueueSubmit(session.queueHandle, 1uL, commands.handler)
                    } finally {
                        wgpuCommandBufferRelease(commandBuffer)
                    }
                } finally {
                    wgpuCommandEncoderRelease(encoder)
                }
            }
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
