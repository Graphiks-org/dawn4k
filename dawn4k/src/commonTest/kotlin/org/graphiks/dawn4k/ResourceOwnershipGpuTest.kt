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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResourceOwnershipGpuTest {
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
                val pass = encoder.beginComputePass() as DawnComputePassEncoder
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
