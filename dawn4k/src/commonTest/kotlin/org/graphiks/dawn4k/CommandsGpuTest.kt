package org.graphiks.dawn4k

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUIndexFormat
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.GPUShaderStage
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUVertexFormat
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.BindGroupLayoutEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferBindingLayout
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.QuerySetDescriptor
import org.graphiks.webgpu.descriptors.RenderBundleEncoderDescriptor
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyBufferLayout
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.VertexAttribute
import org.graphiks.webgpu.descriptors.VertexBufferLayout
import org.graphiks.webgpu.descriptors.VertexState
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Real-GPU command-surface tests: queue writes and submits, command encoders and
 * their passes, indirect dispatch/draw, queries, and render bundles. Runs only
 * through the gpuTest* tasks; a host without an adapter fails these tests.
 */
class CommandsGpuTest {

    // --- queue writeBuffer + encoder copies ----------------------------------

    @Test
    fun writeBufferWithDataOffsetAndSizePreservesSentinels() = runTest {
        val fixture = NativeFixture.open()
        try {
            val buffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
            try {
                // data = [0x10 .. 0x1F]; write data[4..12) into buffer[0..8).
                val data = ArrayBuffer.of(ByteArray(16) { (0x10 + it).toByte() })
                fixture.queue.writeBuffer(buffer, 0uL, data, dataOffset = 4uL, size = 8uL)
                fixture.queue.onSubmittedWorkDone().getOrThrow()
                buffer.mapAsync(GPUMapMode.Read).getOrThrow()
                val bytes = buffer.getMappedRange().toByteArray()
                buffer.unmap()
                assertContentEquals(byteArrayOf(0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B), bytes.copyOfRange(0, 8))
                assertContentEquals(ByteArray(8) { 0 }, bytes.copyOfRange(8, 16))
            } finally {
                buffer.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun copyWithNullSizeCopiesTheRemainderAndPreservesSentinels() = runTest {
        val fixture = NativeFixture.open()
        try {
            val source = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc))
            val target = fixture.createBuffer(BufferDescriptor(24uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
            try {
                source.mapAsync(GPUMapMode.Write).getOrThrow()
                source.getMappedRange().setBytes(0uL, ByteArray(16) { (0x20 + it).toByte() })
                source.unmap()
                fixture.createEncoder().use { encoder ->
                    // sourceOffset=4, size=null -> 12 bytes: target[8..20) = source[4..16).
                    encoder.copyBufferToBuffer(source, 4uL, target, 8uL, null)
                    fixture.submit(encoder)
                }
                fixture.session.onSubmittedWorkDone().getOrThrow()
                target.mapAsync(GPUMapMode.Read).getOrThrow()
                val bytes = target.getMappedRange().toByteArray()
                target.unmap()
                assertContentEquals(ByteArray(8) { 0 }, bytes.copyOfRange(0, 8))
                assertContentEquals(
                    ByteArray(12) { (0x24 + it).toByte() },
                    bytes.copyOfRange(8, 20),
                )
                assertContentEquals(ByteArray(4) { 0 }, bytes.copyOfRange(20, 24))
            } finally {
                source.close()
                target.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun commandsExecuteInSubmissionOrder() = runTest {
        val fixture = NativeFixture.open()
        try {
            val source = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc))
            val target = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
            try {
                source.mapAsync(GPUMapMode.Write).getOrThrow()
                source.getMappedRange().setUInts(0uL, uintArrayOf(1u, 2u, 3u, 4u))
                source.unmap()
                fixture.createEncoder().use { encoder ->
                    encoder.copyBufferToBuffer(source, 0uL, target, 0uL, 16uL)
                    encoder.clearBuffer(target)
                    fixture.submit(encoder)
                }
                fixture.session.onSubmittedWorkDone().getOrThrow()
                target.mapAsync(GPUMapMode.Read).getOrThrow()
                assertContentEquals(uintArrayOf(0u, 0u, 0u, 0u), target.getMappedRange().toUIntArray())
                target.unmap()
            } finally {
                source.close()
                target.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun clearBufferClearsOnlyTheRequestedRange() = runTest {
        val fixture = NativeFixture.open()
        try {
            val buffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
            try {
                fixture.queue.writeBuffer(buffer, 0uL, ArrayBuffer.of(ByteArray(16) { 0x7F }))
                fixture.queue.onSubmittedWorkDone().getOrThrow()
                fixture.createEncoder().use { encoder ->
                    encoder.clearBuffer(buffer, offset = 4uL, size = 8uL)
                    fixture.submit(encoder)
                }
                fixture.session.onSubmittedWorkDone().getOrThrow()
                buffer.mapAsync(GPUMapMode.Read).getOrThrow()
                val bytes = buffer.getMappedRange().toByteArray()
                buffer.unmap()
                assertContentEquals(ByteArray(4) { 0x7F }, bytes.copyOfRange(0, 4))
                assertContentEquals(ByteArray(8) { 0 }, bytes.copyOfRange(4, 12))
                assertContentEquals(ByteArray(4) { 0x7F }, bytes.copyOfRange(12, 16))
            } finally {
                buffer.close()
            }
        } finally {
            fixture.close()
        }
    }

    // --- writeTexture --------------------------------------------------------

    @Test
    fun writeTextureRoundTripsTightAndPaddedRows() = runTest {
        val fixture = NativeFixture.open()
        try {
            val texture = fixture.createTexture(
                TextureDescriptor(
                    size = Extent3D(width = 4u, height = 4u),
                    format = GPUTextureFormat.RGBA8Unorm,
                    usage = GPUTextureUsage.CopyDst or GPUTextureUsage.CopySrc,
                ),
            )
            val staging = fixture.createBuffer(BufferDescriptor(1024uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
            try {
                // Tight rows: 16 bytes/row for a 4x4 RGBA8 texture (no forced 256 padding).
                fixture.queue.writeTexture(
                    destination = TexelCopyTextureInfo(texture),
                    data = ArrayBuffer.of(ByteArray(16 * 4) { (it + 1).toByte() }),
                    dataLayout = TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 16u, rowsPerImage = 4u),
                    size = Extent3D(width = 4u, height = 4u),
                )
                fixture.queue.onSubmittedWorkDone().getOrThrow()
                assertContentEquals(ByteArray(16) { (it + 1).toByte() }, readbackRow(fixture, texture, staging))

                // Padded rows: 256 bytes/row; only the first 16 bytes of each row carry texels.
                val padded = ArrayBuffer.of(ByteArray(256 * 4) { 0 })
                padded.setBytes(0uL, ByteArray(16) { (0x40 + it).toByte() })
                fixture.queue.writeTexture(
                    destination = TexelCopyTextureInfo(texture),
                    data = padded,
                    dataLayout = TexelCopyBufferLayout(offset = 0uL, bytesPerRow = 256u, rowsPerImage = 4u),
                    size = Extent3D(width = 4u, height = 4u),
                )
                fixture.queue.onSubmittedWorkDone().getOrThrow()
                assertContentEquals(ByteArray(16) { (0x40 + it).toByte() }, readbackRow(fixture, texture, staging))
            } finally {
                texture.close()
                staging.close()
            }
        } finally {
            fixture.close()
        }
    }

    // --- validation errors surfaced through Dawn's callback machinery ---------

    @Test
    fun finishingAnEncoderTwiceObservesAValidationError() = runTest {
        val fixture = NativeFixture.open()
        try {
            val before = fixture.uncapturedErrorCount()
            fixture.createEncoder().use { encoder ->
                encoder.finish().close()
                runCatching { encoder.finish() }.getOrNull()?.close()
            }
            assertTrue(fixture.uncapturedErrorCount() > before)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun aSubmittedCommandBufferCannotBeSubmittedAgain() = runTest {
        val fixture = NativeFixture.open()
        try {
            val scratch = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopyDst))
            val encoder = fixture.createEncoder()
            encoder.clearBuffer(scratch)
            val commandBuffer = encoder.finish()
            encoder.close()

            val before = fixture.uncapturedErrorCount()
            fixture.queue.submit(listOf(commandBuffer))
            fixture.queue.onSubmittedWorkDone().getOrThrow()
            fixture.queue.submit(listOf(commandBuffer))
            fixture.queue.onSubmittedWorkDone().getOrThrow()
            assertTrue(fixture.uncapturedErrorCount() > before)

            commandBuffer.close()
            scratch.close()
        } finally {
            fixture.close()
        }
    }

    @Test
    fun submittingACommandBufferReferencingADestroyedTextureFails() = runTest {
        val fixture = NativeFixture.open()
        try {
            val texture = fixture.createTexture(
                TextureDescriptor(
                    size = Extent3D(width = 4u, height = 4u),
                    format = GPUTextureFormat.RGBA8Unorm,
                    usage = GPUTextureUsage.RenderAttachment,
                ),
            )
            val encoder = fixture.createEncoder()
            val commandBuffer = try {
                encoder.beginRenderPass(
                    RenderPassDescriptor(
                        colorAttachments = listOf(
                            RenderPassColorAttachment(
                                view = texture,
                                loadOp = GPULoadOp.Clear,
                                storeOp = GPUStoreOp.Store,
                            ),
                        ),
                    ),
                ).let { pass -> pass.end() }
                encoder.finish()
            } finally {
                encoder.close()
            }
            texture.close()

            val before = fixture.uncapturedErrorCount()
            fixture.queue.submit(listOf(commandBuffer))
            fixture.queue.onSubmittedWorkDone().getOrThrow()
            assertTrue(fixture.uncapturedErrorCount() > before)

            commandBuffer.close()
        } finally {
            fixture.close()
        }
    }

    // --- compute / render pass commands --------------------------------------

    @Test
    fun setBindGroupWithEmptyAndPopulatedDynamicOffsets() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
                // Empty dynamic offsets: an auto-layout pipeline with a plain storage binding.
                fixture.createComputePipeline(ComputePipelineDescriptor(ProgrammableStage(shader, "main"))).use { pipeline ->
                    fixture.dispatchAndReadback(pipeline, uintArrayOf(1u, 2u, 3u, 4u))
                }
                // Populated dynamic offsets: an explicit dynamic-offset layout.
                fixture.session.createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        entries = listOf(
                            BindGroupLayoutEntry(
                                binding = 0u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage, hasDynamicOffset = true),
                            ),
                        ),
                    ),
                ).use { layout ->
                    fixture.session.createPipelineLayout(PipelineLayoutDescriptor(listOf(layout))).use { pipelineLayout ->
                        fixture.createComputePipeline(
                            ComputePipelineDescriptor(ProgrammableStage(shader, "main"), layout = pipelineLayout),
                        ).use { pipeline ->
                            verifyDynamicOffset(fixture, pipeline, layout, 256u)
                        }
                    }
                }
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun dispatchWorkgroupsIndirectReadsTheDispatchArguments() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
                fixture.createComputePipeline(ComputePipelineDescriptor(ProgrammableStage(shader, "main"))).use { pipeline ->
                    fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc)).use { storage ->
                        fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)).use { staging ->
                            fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Indirect or GPUBufferUsage.CopyDst)).use { indirect ->
                                fixture.queue.writeBuffer(indirect, 0uL, ArrayBuffer.of(uintArrayOf(4u, 1u, 1u)))
                                pipeline.getBindGroupLayout(0u).use { layout ->
                                    fixture.session.createBindGroup(
                                        BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, storage))),
                                    ).use { bindGroup ->
                                        fixture.createEncoder().use { encoder ->
                                            encoder.beginComputePass().let { pass ->
                                                pass.setPipeline(pipeline)
                                                pass.setBindGroup(0u, bindGroup, emptyList())
                                                pass.dispatchWorkgroupsIndirect(indirect, 0uL)
                                                pass.end()
                                            }
                                            encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, 16uL)
                                            fixture.submit(encoder)
                                        }
                                        fixture.session.onSubmittedWorkDone().getOrThrow()
                                        staging.mapAsync(GPUMapMode.Read).getOrThrow()
                                        assertContentEquals(uintArrayOf(1u, 2u, 3u, 4u), staging.getMappedRange().toUIntArray())
                                        staging.unmap()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun drawIndexedWithSignedBaseVertexAndFirstInstanceRenders() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(INDEXED_SHADER)).use { shader ->
                fixture.createRenderPipeline(
                    RenderPipelineDescriptor(
                        vertex = VertexState(
                            module = shader,
                            entryPoint = "vs_main",
                            buffers = listOf(
                                VertexBufferLayout(
                                    arrayStride = 8uL,
                                    attributes = listOf(VertexAttribute(format = GPUVertexFormat.Float32x2, offset = 0uL, shaderLocation = 0u)),
                                ),
                            ),
                        ),
                        fragment = FragmentState(
                            module = shader,
                            entryPoint = "fs_main",
                            targets = listOf(ColorTargetState(format = GPUTextureFormat.RGBA8Unorm)),
                        ),
                    ),
                ).use { pipeline ->
                    fixture.createBuffer(BufferDescriptor(64uL, GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst)).use { vertexBuffer ->
                        fixture.createBuffer(BufferDescriptor(64uL, GPUBufferUsage.Index or GPUBufferUsage.CopyDst)).use { indexBuffer ->
                            // Vertex positions: (-1,-1), (3,-1), (-1,3).
                            fixture.queue.writeBuffer(vertexBuffer, 0uL, ArrayBuffer.of(floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)))
                            // Indices {1, 2, 3} with baseVertex=-1 -> vertices {0, 1, 2}.
                            fixture.queue.writeBuffer(indexBuffer, 0uL, ArrayBuffer.of(uintArrayOf(1u, 2u, 3u)))

                            val texture = fixture.createTexture(
                                TextureDescriptor(
                                    size = Extent3D(width = 4u, height = 4u),
                                    format = GPUTextureFormat.RGBA8Unorm,
                                    usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
                                ),
                            )
                            val staging = fixture.createBuffer(BufferDescriptor(1024uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
                            try {
                                fixture.createEncoder().use { encoder ->
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
                                        pass.setVertexBuffer(0u, vertexBuffer)
                                        pass.setIndexBuffer(indexBuffer, GPUIndexFormat.Uint32)
                                        pass.drawIndexed(indexCount = 3u, firstIndex = 0u, baseVertex = -1, firstInstance = 3u)
                                        pass.end()
                                    }
                                    encoder.copyTextureToBuffer(
                                        source = TexelCopyTextureInfo(texture),
                                        destination = TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 4u),
                                        copySize = Extent3D(width = 4u, height = 4u),
                                    )
                                    fixture.submit(encoder)
                                }
                                fixture.session.onSubmittedWorkDone().getOrThrow()
                                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                                val bytes = staging.getMappedRange().toByteArray()
                                staging.unmap()
                                assertContentEquals(byteArrayOf(-1, 0, 0, -1), bytes.copyOfRange(0, 4))
                            } finally {
                                texture.close()
                                staging.close()
                            }
                        }
                    }
                }
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun drawIndirectReadsTheDrawArguments() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(TRIANGLE_SHADER)).use { shader ->
                fixture.createRenderPipeline(
                    RenderPipelineDescriptor(
                        vertex = VertexState(module = shader, entryPoint = "vs_main"),
                        fragment = FragmentState(
                            module = shader,
                            entryPoint = "fs_main",
                            targets = listOf(ColorTargetState(format = GPUTextureFormat.RGBA8Unorm)),
                        ),
                    ),
                ).use { pipeline ->
                    fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Indirect or GPUBufferUsage.CopyDst)).use { indirect ->
                        fixture.queue.writeBuffer(indirect, 0uL, ArrayBuffer.of(uintArrayOf(3u, 1u, 0u, 0u)))
                        val texture = fixture.createTexture(
                            TextureDescriptor(
                                size = Extent3D(width = 4u, height = 4u),
                                format = GPUTextureFormat.RGBA8Unorm,
                                usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
                            ),
                        )
                        val staging = fixture.createBuffer(BufferDescriptor(1024uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
                        try {
                            fixture.createEncoder().use { encoder ->
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
                                    pass.drawIndirect(indirect, 0uL)
                                    pass.end()
                                }
                                encoder.copyTextureToBuffer(
                                    source = TexelCopyTextureInfo(texture),
                                    destination = TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 4u),
                                    copySize = Extent3D(width = 4u, height = 4u),
                                )
                                fixture.submit(encoder)
                            }
                            fixture.session.onSubmittedWorkDone().getOrThrow()
                            staging.mapAsync(GPUMapMode.Read).getOrThrow()
                            val bytes = staging.getMappedRange().toByteArray()
                            staging.unmap()
                            assertContentEquals(byteArrayOf(-1, 0, 0, -1), bytes.copyOfRange(0, 4))
                        } finally {
                            texture.close()
                            staging.close()
                        }
                    }
                }
            }
        } finally {
            fixture.close()
        }
    }

    // --- queries -------------------------------------------------------------

    @Test
    fun occlusionQueryBeginEndAndResolve() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(TRIANGLE_SHADER)).use { shader ->
                fixture.createRenderPipeline(
                    RenderPipelineDescriptor(
                        vertex = VertexState(module = shader, entryPoint = "vs_main"),
                        fragment = FragmentState(
                            module = shader,
                            entryPoint = "fs_main",
                            targets = listOf(ColorTargetState(format = GPUTextureFormat.RGBA8Unorm)),
                        ),
                    ),
                ).use { pipeline ->
                    fixture.createQuerySet(QuerySetDescriptor(type = GPUQueryType.Occlusion, count = 2u)).use { querySet ->
                        fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.QueryResolve or GPUBufferUsage.CopySrc)).use { resolved ->
                            fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)).use { staging ->
                                val texture = fixture.createTexture(
                                    TextureDescriptor(
                                        size = Extent3D(width = 4u, height = 4u),
                                        format = GPUTextureFormat.RGBA8Unorm,
                                        usage = GPUTextureUsage.RenderAttachment,
                                    ),
                                )
                                try {
                                    fixture.createEncoder().use { encoder ->
                                        encoder.beginRenderPass(
                                            RenderPassDescriptor(
                                                colorAttachments = listOf(
                                                    RenderPassColorAttachment(
                                                        view = texture,
                                                        loadOp = GPULoadOp.Clear,
                                                        storeOp = GPUStoreOp.Store,
                                                    ),
                                                ),
                                                occlusionQuerySet = querySet,
                                            ),
                                        ).let { pass ->
                                            pass.setPipeline(pipeline)
                                            pass.beginOcclusionQuery(0u)
                                            pass.draw(3u)
                                            pass.endOcclusionQuery()
                                            pass.end()
                                        }
                                        encoder.resolveQuerySet(querySet, 0u, 1u, resolved, 0uL)
                                        encoder.copyBufferToBuffer(resolved, 0uL, staging, 0uL, 8uL)
                                        fixture.submit(encoder)
                                    }
                                    fixture.session.onSubmittedWorkDone().getOrThrow()
                                    staging.mapAsync(GPUMapMode.Read).getOrThrow()
                                    val result = staging.getMappedRange().getUInt(0uL)
                                    staging.unmap()
                                    // A drawn full-screen triangle passes at least one sample.
                                    assertTrue(result > 0u)
                                } finally {
                                    texture.close()
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            fixture.close()
        }
    }

    // --- render bundles ------------------------------------------------------

    @Test
    fun renderBundleRecordedAndReusedAcrossEncoders() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(TRIANGLE_SHADER)).use { shader ->
                fixture.createRenderPipeline(
                    RenderPipelineDescriptor(
                        vertex = VertexState(module = shader, entryPoint = "vs_main"),
                        fragment = FragmentState(
                            module = shader,
                            entryPoint = "fs_main",
                            targets = listOf(ColorTargetState(format = GPUTextureFormat.RGBA8Unorm)),
                        ),
                    ),
                ).use { pipeline ->
                    fixture.createRenderBundleEncoder(
                        RenderBundleEncoderDescriptor(colorFormats = listOf(GPUTextureFormat.RGBA8Unorm)),
                    ).use { bundleEncoder ->
                        bundleEncoder.setPipeline(pipeline)
                        bundleEncoder.draw(3u)
                        val bundle = bundleEncoder.finish()
                        // Execute the same bundle from two independent encoders.
                        for (pass in 0 until 2) {
                            val texture = fixture.createTexture(
                                TextureDescriptor(
                                    size = Extent3D(width = 4u, height = 4u),
                                    format = GPUTextureFormat.RGBA8Unorm,
                                    usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
                                ),
                            )
                            val staging = fixture.createBuffer(BufferDescriptor(1024uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
                            try {
                                fixture.createEncoder().use { encoder ->
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
                                    ).let { renderPass ->
                                        renderPass.executeBundles(listOf(bundle))
                                        renderPass.end()
                                    }
                                    encoder.copyTextureToBuffer(
                                        source = TexelCopyTextureInfo(texture),
                                        destination = TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 4u),
                                        copySize = Extent3D(width = 4u, height = 4u),
                                    )
                                    fixture.submit(encoder)
                                }
                                fixture.session.onSubmittedWorkDone().getOrThrow()
                                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                                val bytes = staging.getMappedRange().toByteArray()
                                staging.unmap()
                                assertContentEquals(byteArrayOf(-1, 0, 0, -1), bytes.copyOfRange(0, 4))
                            } finally {
                                texture.close()
                                staging.close()
                            }
                        }
                    }
                }
            }
        } finally {
            fixture.close()
        }
    }

    // --- helpers -------------------------------------------------------------

    private suspend fun NativeFixture.uncapturedErrorCount(): Int {
        runtime.drainEvents()
        runtime.dispatcher.drain()
        return session.callbacks.uncapturedErrors.size
    }

    private suspend fun readbackRow(fixture: NativeFixture, texture: DawnTexture, staging: DawnBuffer): ByteArray {
        fixture.createEncoder().use { encoder ->
            encoder.copyTextureToBuffer(
                source = TexelCopyTextureInfo(texture),
                destination = TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 4u),
                copySize = Extent3D(width = 4u, height = 4u),
            )
            fixture.submit(encoder)
        }
        fixture.session.onSubmittedWorkDone().getOrThrow()
        staging.mapAsync(GPUMapMode.Read).getOrThrow()
        val bytes = staging.getMappedRange().toByteArray()
        staging.unmap()
        return bytes.copyOfRange(0, 16)
    }

    private suspend fun verifyDynamicOffset(
        fixture: NativeFixture,
        pipeline: DawnComputePipeline,
        layout: DawnBindGroupLayout,
        offset: UInt,
    ) {
        fixture.createBuffer(BufferDescriptor(512uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc)).use { storage ->
            fixture.createBuffer(BufferDescriptor(512uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)).use { staging ->
                fixture.session.createBindGroup(
                    BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, BufferBinding(storage, size = 16uL)))),
                ).use { bindGroup ->
                    fixture.createEncoder().use { encoder ->
                        encoder.beginComputePass().let { pass ->
                            pass.setPipeline(pipeline)
                            pass.setBindGroup(0u, bindGroup, listOf(offset))
                            pass.dispatchWorkgroups(4u)
                            pass.end()
                        }
                        encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, 512uL)
                        fixture.submit(encoder)
                    }
                    fixture.session.onSubmittedWorkDone().getOrThrow()
                    staging.mapAsync(GPUMapMode.Read).getOrThrow()
                    val readback = staging.getMappedRange().toUIntArray()
                    staging.unmap()
                    assertContentEquals(uintArrayOf(0u, 0u, 0u, 0u), readback.copyOfRange(0, 4))
                    assertContentEquals(uintArrayOf(1u, 2u, 3u, 4u), readback.copyOfRange(64, 68))
                }
            }
        }
    }

    private companion object {
        val SCALE_SHADER = """
            @group(0) @binding(0) var<storage, read_write> output: array<u32>;
            @compute @workgroup_size(1)
            fn main(@builtin(global_invocation_id) id: vec3<u32>) {
                output[id.x] = (id.x + 1u);
            }
        """.trimIndent()

        val TRIANGLE_SHADER = """
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

        val INDEXED_SHADER = """
            @vertex
            fn vs_main(@location(0) position: vec2f) -> @builtin(position) vec4f {
                return vec4f(position, 0.0, 1.0);
            }

            @fragment
            fn fs_main() -> @location(0) vec4f {
                return vec4f(1.0, 0.0, 0.0, 1.0);
            }
        """.trimIndent()
    }
}
