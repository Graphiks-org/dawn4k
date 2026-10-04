package org.graphiks.dawn4k

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUCompilationMessageType
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUShaderStage
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.BindGroupLayoutEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferBindingLayout
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Real-GPU compute resources: shader modules, compute pipelines, bind groups and
 * their layouts, with a real dispatch oracle. Runs only through the gpuTest*
 * tasks; a host without an adapter fails these tests (no silent skip).
 */
class ComputeResourcesGpuTest {

    @Test
    fun computeConstantsAreApplied() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
                fixture.createComputePipeline(
                    ComputePipelineDescriptor(ProgrammableStage(shader, "main", mapOf("scale" to 3.0)))
                ).use { pipeline ->
                    fixture.dispatchAndReadback(pipeline, uintArrayOf(3u, 6u, 9u, 12u))
                }
            }
        } finally { fixture.close() }
    }

    @Test
    fun explicitPipelineLayoutAppliesConstants() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
                fixture.session.createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        entries = listOf(
                            BindGroupLayoutEntry(
                                binding = 0u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage),
                            )
                        )
                    )
                ).use { layout ->
                    fixture.session.createPipelineLayout(PipelineLayoutDescriptor(listOf(layout))).use { pipelineLayout ->
                        fixture.createComputePipeline(
                            ComputePipelineDescriptor(
                                ProgrammableStage(shader, "main", mapOf("scale" to 3.0)),
                                layout = pipelineLayout,
                            )
                        ).use { pipeline ->
                            fixture.dispatchAndReadback(pipeline, uintArrayOf(3u, 6u, 9u, 12u))
                        }
                    }
                }
            }
        } finally { fixture.close() }
    }

    @Test
    fun bufferBindingOffsetAndSizeAreApplied() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
                fixture.createComputePipeline(ComputePipelineDescriptor(ProgrammableStage(shader, "main"))).use { pipeline ->
                    // 512-byte storage; bind a 16-byte sub-range at the 256-aligned offset.
                    fixture.createBuffer(BufferDescriptor(512uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc)).use { storage ->
                        fixture.createBuffer(BufferDescriptor(512uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)).use { staging ->
                            pipeline.getBindGroupLayout(0u).use { layout ->
                                fixture.session.createBindGroup(
                                    BindGroupDescriptor(
                                        layout = layout,
                                        entries = listOf(BindGroupEntry(0u, BufferBinding(storage, offset = 256uL, size = 16uL))),
                                    )
                                ).use { bindGroup ->
                                    fixture.dispatchWorkgroups(pipeline, bindGroup, storage, staging, workgroupCountX = 4u)
                                    staging.mapAsync(GPUMapMode.Read).getOrThrow()
                                    val readback = staging.getMappedRange().toUIntArray()
                                    // output[0..3] = [1, 2, 3, 4] lands at u32 indices 64..67; the prefix stays zero.
                                    assertContentEquals(uintArrayOf(0u, 0u, 0u, 0u), readback.copyOfRange(0, 4))
                                    assertContentEquals(uintArrayOf(1u, 2u, 3u, 4u), readback.copyOfRange(64, 68))
                                    staging.unmap()
                                }
                            }
                        }
                    }
                }
            }
        } finally { fixture.close() }
    }

    @Test
    fun dynamicOffsetsAreApplied() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
                fixture.session.createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        entries = listOf(
                            BindGroupLayoutEntry(
                                binding = 0u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage, hasDynamicOffset = true),
                            )
                        )
                    )
                ).use { layout ->
                    fixture.session.createPipelineLayout(PipelineLayoutDescriptor(listOf(layout))).use { pipelineLayout ->
                        fixture.createComputePipeline(
                            ComputePipelineDescriptor(ProgrammableStage(shader, "main"), layout = pipelineLayout)
                        ).use { pipeline ->
                            fixture.createBuffer(BufferDescriptor(512uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc)).use { storage ->
                                fixture.createBuffer(BufferDescriptor(512uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)).use { staging ->
                                    fixture.session.createBindGroup(
                                        BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, BufferBinding(storage, size = 16uL))))
                                    ).use { bindGroup ->
                                        fixture.dispatchWorkgroups(
                                            pipeline, bindGroup, storage, staging,
                                            workgroupCountX = 4u, dynamicOffsets = uintArrayOf(256u),
                                        )
                                        staging.mapAsync(GPUMapMode.Read).getOrThrow()
                                        val readback = staging.getMappedRange().toUIntArray()
                                        assertContentEquals(uintArrayOf(0u, 0u, 0u, 0u), readback.copyOfRange(0, 4))
                                        assertContentEquals(uintArrayOf(1u, 2u, 3u, 4u), readback.copyOfRange(64, 68))
                                        staging.unmap()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally { fixture.close() }
    }

    @Test
    fun invalidShaderReportsCompilationDiagnostics() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(BROKEN_SHADER)).use { shader ->
                val info = shader.getCompilationInfo().getOrThrow()
                assertTrue(info.messages.any { it.type == GPUCompilationMessageType.Error })
            }
        } finally { fixture.close() }
    }

    @Test
    fun validShaderCompilationInfoSucceeds() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
                val info = shader.getCompilationInfo().getOrThrow()
                // Messages are implementation-defined; a valid shader reports no errors.
                assertTrue(info.messages.none { it.type == GPUCompilationMessageType.Error })
            }
        } finally { fixture.close() }
    }

    @Test
    fun asyncPipelineRejectionIsABoundedFailure() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
                val result = fixture.session.createComputePipelineAsync(
                    ComputePipelineDescriptor(ProgrammableStage(shader, "no_such_entry_point"))
                )
                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is DawnPipelineException)
            }
        } finally { fixture.close() }
    }

    @Test
    fun foreignBufferFromAnotherSessionIsRefused() = runTest {
        val fixture = NativeFixture.open()
        try {
            val otherSession = fixture.runtime.openSession()
            try {
                val foreign = otherSession.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage))
                try {
                    fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
                        fixture.createComputePipeline(ComputePipelineDescriptor(ProgrammableStage(shader, "main"))).use { pipeline ->
                            pipeline.getBindGroupLayout(0u).use { layout ->
                                assertFailsWith<IllegalArgumentException> {
                                    fixture.session.createBindGroup(
                                        BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, foreign)))
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    foreign.close()
                }
            } finally {
                otherSession.close()
            }
        } finally { fixture.close() }
    }

    @Test
    fun temporaryBindGroupLayoutReferenceIsReleasedOnClose() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(SCALE_SHADER)).use { shader ->
                fixture.createComputePipeline(ComputePipelineDescriptor(ProgrammableStage(shader, "main"))).use { pipeline ->
                    val before = fixture.session.resources.debugRemainingRefs()
                    pipeline.getBindGroupLayout(0u).use {
                        assertEquals(before + 1, fixture.session.resources.debugRemainingRefs())
                    }
                    // The owned temporary reference is released by close.
                    assertEquals(before, fixture.session.resources.debugRemainingRefs())
                }
            }
        } finally { fixture.close() }
    }

    private companion object {
        val SCALE_SHADER = """
            override scale: u32 = 1u;
            @group(0) @binding(0) var<storage, read_write> output: array<u32>;
            @compute @workgroup_size(1)
            fn main(@builtin(global_invocation_id) id: vec3<u32>) {
                output[id.x] = (id.x + 1u) * scale;
            }
        """.trimIndent()

        val BROKEN_SHADER = """
            fn main() {
                let x: i32 = "not an i32";
            }
        """.trimIndent()
    }
}
