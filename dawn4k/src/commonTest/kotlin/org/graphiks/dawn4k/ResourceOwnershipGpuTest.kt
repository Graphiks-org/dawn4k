package org.graphiks.dawn4k

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.dawn4k.testing.gpuTestEnvironment
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResourceOwnershipGpuTest {
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
