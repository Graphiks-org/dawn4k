package org.graphiks.dawn4k

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.dawn4k.testing.gpuTestEnvironment
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.QuerySetDescriptor
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RenderBundleEncoderDescriptor
import org.graphiks.webgpu.GPURenderPassDescriptor
import org.graphiks.webgpu.GPURenderPassColorAttachment
import org.graphiks.webgpu.GPUComputePipelineDescriptor
import org.graphiks.webgpu.GPURenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.VertexState
import org.graphiks.webgpu.descriptors.DepthStencilState
import org.graphiks.webgpu.GPUCompareFunction
import kotlin.test.assertFalse
import org.graphiks.dawn4k.mapper.allocateRenderPassDescriptor
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResourceOwnershipGpuTest {
    @Test
    fun partialRenderPassMappingReleasesEveryAcquiredView() = runTest {
        if (!gpuTestEnvironment("ResourceOwnershipGpuTest.partialRenderPassMappingReleasesEveryAcquiredView")) return@runTest
        NativeFixture.open().use { fixture ->
            fixture.createTexture(TextureDescriptor(Extent3D(4u, 4u), GPUTextureFormat.RGBA8Unorm,
                GPUTextureUsage.RenderAttachment)).use { texture ->
                val closedView = texture.createView(null) as DawnTextureView
                closedView.close()
                val descriptor = RenderPassDescriptor(listOf(
                    RenderPassColorAttachment(texture, GPULoadOp.Clear, GPUStoreOp.Store),
                    RenderPassColorAttachment(closedView, GPULoadOp.Clear, GPUStoreOp.Store),
                ))
                val temporary = mutableListOf<DawnTextureView>()
                assertFailsWith<IllegalStateException> {
                    withTemporaryViews(temporary) {
                        memoryScope { it.allocateRenderPassDescriptor(descriptor, temporary) }
                    }
                }
                assertEquals(1, temporary.size)
                assertFailsWith<IllegalStateException> { temporary.single().handle }
                fixture.createEncoder().use { encoder ->
                    assertFailsWith<IllegalStateException> { encoder.beginRenderPass(descriptor) }
                }
            }
        }
    }

    @Test
    fun closedEncoderRejectsBeforeMappingAttachments() = runTest {
        if (!gpuTestEnvironment("ResourceOwnershipGpuTest.closedEncoderRejectsBeforeMappingAttachments")) return@runTest
        NativeFixture.open().use { fixture ->
            var attachmentReads = 0
            val descriptor = object : GPURenderPassDescriptor by RenderPassDescriptor(emptyList()) {
                override val colorAttachments: List<GPURenderPassColorAttachment?>
                    get() { attachmentReads++; return emptyList() }
            }
            val encoder = fixture.createEncoder()
            encoder.close()
            assertFailsWith<IllegalStateException> { encoder.beginRenderPass(descriptor) }
            assertEquals(0, attachmentReads)
        }
    }

    @Test
    fun asyncPipelineLabelsAreSnapshottedBeforeNativeIssuance() = runTest {
        if (!gpuTestEnvironment("ResourceOwnershipGpuTest.asyncPipelineLabelsAreSnapshottedBeforeNativeIssuance")) return@runTest
        NativeFixture.open().use { fixture ->
            fixture.createShaderModule(ShaderModuleDescriptor("""
                @compute @workgroup_size(1) fn main() {}
                @vertex fn vertex() -> @builtin(position) vec4f { return vec4f(0.0); }
            """.trimIndent())).use { shader ->
                val compute = object : GPUComputePipelineDescriptor by ComputePipelineDescriptor(ProgrammableStage(shader, "main")) {
                    override val label: String
                        get() { assertFalse(fixture.runtime.hasPendingOperations()); return "compute snapshot" }
                }
                fixture.session.createComputePipelineAsync(compute).getOrThrow().use {
                    assertEquals("compute snapshot", it.label)
                }
                val render = object : GPURenderPipelineDescriptor by RenderPipelineDescriptor(
                    VertexState(shader, entryPoint = "vertex"),
                    depthStencil = DepthStencilState(GPUTextureFormat.Depth32Float,
                        depthWriteEnabled = false, depthCompare = GPUCompareFunction.Always),
                ) {
                    override val label: String
                        get() { assertFalse(fixture.runtime.hasPendingOperations()); return "render snapshot" }
                }
                fixture.session.createRenderPipelineAsync(render).getOrThrow().use {
                    assertEquals("render snapshot", it.label)
                }
            }
        }
    }

    @Test
    fun closedSessionRejectsDeviceAndBorrowedQueueUse() = runTest {
        if (!gpuTestEnvironment("ResourceOwnershipGpuTest.closedSessionRejectsDeviceAndBorrowedQueueUse")) return@runTest
        NativeFixture.open().use { fixture ->
            val device = DawnDevice(fixture.session, null, false)
            val queue = fixture.queue
            device.close()
            assertFailsWith<IllegalStateException> { device.nativeHandle() }
            assertFailsWith<IllegalStateException> { queue.handle }
            assertFailsWith<IllegalStateException> { queue.submit(emptyList()) }
            assertFailsWith<IllegalStateException> { device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopyDst)) }
        }
    }

    @Test
    fun bundleHasIndependentExplicitOwnership() = runTest {
        if (!gpuTestEnvironment("ResourceOwnershipGpuTest.bundleHasIndependentExplicitOwnership")) return@runTest
        NativeFixture.open().use { fixture ->
            fixture.createRenderBundleEncoder(RenderBundleEncoderDescriptor(
                colorFormats = listOf(GPUTextureFormat.RGBA8Unorm))).use { encoder ->
                val bundle: DawnRenderBundle = encoder.finish(null)
                bundle.close()
                bundle.close()
                assertFailsWith<IllegalStateException> { bundle.handle }
            }
            fixture.createEncoder().use { encoder ->
                val pass = encoder.beginComputePass()
                pass.close()
                pass.close()
                assertFailsWith<IllegalStateException> { pass.dispatchWorkgroups(1u) }
                assertFailsWith<IllegalStateException> { pass.end() }
            }
        }
    }

    @Test
    fun closedDestroyBackedResourcesCannotReachEncoding() = runTest {
        if (!gpuTestEnvironment("ResourceOwnershipGpuTest.closedDestroyBackedResourcesCannotReachEncoding")) return@runTest
        NativeFixture.open().use { fixture ->
            val buffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopySrc))
            buffer.close()
            assertFailsWith<IllegalStateException> { buffer.handle }
            fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopyDst)).use { destination ->
                fixture.createEncoder().use { encoder ->
                    assertFailsWith<IllegalStateException> { encoder.copyBufferToBuffer(buffer, 0uL, destination, 0uL, 16uL) }
                }
            }
            val texture = fixture.createTexture(TextureDescriptor(Extent3D(4u, 4u), GPUTextureFormat.RGBA8Unorm, GPUTextureUsage.CopySrc))
            texture.close()
            assertFailsWith<IllegalStateException> { texture.createView(null) }
            assertEquals(4u, texture.width)
            val queries = fixture.createQuerySet(QuerySetDescriptor(GPUQueryType.Occlusion, 1u))
            queries.close()
            queries.close()
            assertFailsWith<IllegalStateException> { queries.handle }
        }
    }

    @Test
    fun closedSamplerCannotBePassedThroughDescriptor() = runTest {
        if (!gpuTestEnvironment("ResourceOwnershipGpuTest.closedSamplerCannotBePassedThroughDescriptor")) return@runTest
        NativeFixture.open().use { fixture ->
            val sampler = fixture.createSampler(SamplerDescriptor())
            sampler.close()
            sampler.close()
            assertFailsWith<IllegalStateException> { sampler.handle }
            fixture.session.createBindGroupLayout(BindGroupLayoutDescriptor(emptyList())).use { layout ->
                assertFailsWith<IllegalStateException> {
                    fixture.session.createBindGroup(BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, sampler))))
                }
            }
            assertEquals("", sampler.label)
        }
    }

    @Test
    fun closedShaderCannotStartCompilationRequest() = runTest {
        if (!gpuTestEnvironment("ResourceOwnershipGpuTest.closedShaderCannotStartCompilationRequest")) return@runTest
        NativeFixture.open().use { fixture ->
            val shader = fixture.createShaderModule(ShaderModuleDescriptor("@compute @workgroup_size(1) fn main() {}"))
            shader.close()
            assertFailsWith<IllegalStateException> { shader.handle }
            assertFailsWith<IllegalStateException> { shader.getCompilationInfo() }
            shader.close()
        }
    }

    @Test
    fun closedCommandBufferCannotBeSubmitted() = runTest {
        if (!gpuTestEnvironment("ResourceOwnershipGpuTest.closedCommandBufferCannotBeSubmitted")) return@runTest
        NativeFixture.open().use { fixture ->
            fixture.createEncoder().use { encoder ->
                val commands = encoder.finish()
                commands.close()
                assertFailsWith<IllegalStateException> { (commands as DawnCommandBuffer).handle }
                assertFailsWith<IllegalStateException> { fixture.queue.submit(listOf(commands)) }
                commands.close()
            }
        }
    }
}
