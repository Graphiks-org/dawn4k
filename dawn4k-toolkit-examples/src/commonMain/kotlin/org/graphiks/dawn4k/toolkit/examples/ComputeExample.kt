package org.graphiks.dawn4k.toolkit.examples

import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.dawn4k.toolkit.DawnSession
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*

/** Runs through the session owner and returns a copy independent of mapped GPU memory. */
suspend fun runComputeExample(session: DawnSession): IntArray = session.run { gpu ->
    gpu.device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst)).use { storage ->
        gpu.device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead)).use { readback ->
            gpu.queue.writeBuffer(storage, 0uL, ArrayBuffer.of(intArrayOf(1, 2, 3, 4)))
            gpu.device.createShaderModule(ShaderModuleDescriptor("""
                @group(0) @binding(0) var<storage, read_write> values: array<u32>;
                @compute @workgroup_size(1)
                fn main(@builtin(global_invocation_id) id: vec3u) { values[id.x] *= 2u; }
            """.trimIndent())).use { shader ->
                gpu.device.createComputePipeline(ComputePipelineDescriptor(ProgrammableStage(shader, "main"))).use { pipeline ->
                    pipeline.getBindGroupLayout(0u).use { layout ->
                        gpu.device.createBindGroup(BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, storage)))).use { binding ->
                            (gpu.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
                                encoder.beginComputePass().use { pass ->
                                    try { pass.setPipeline(pipeline); pass.setBindGroup(0u, binding); pass.dispatchWorkgroups(4u) }
                                    finally { pass.end() }
                                }
                                encoder.copyBufferToBuffer(storage, 0uL, readback, 0uL, 16uL)
                                encoder.finish().use { gpu.queue.submit(listOf(it)) }
                            }
                            readback.mapAsync(GPUMapMode.Read).getOrThrow()
                            try { readback.getMappedRange().toIntArray() }
                            finally { readback.unmap() }
                        }
                    }
                }
            }
        }
    }
}
