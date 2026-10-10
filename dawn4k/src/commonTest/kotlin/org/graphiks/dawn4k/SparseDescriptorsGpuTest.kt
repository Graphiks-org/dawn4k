package org.graphiks.dawn4k

import org.graphiks.dawn4k.testing.gpuTestEnvironment
import org.graphiks.dawn4k.testing.gpuTestConfig

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.mapper.allocatePipelineLayoutDescriptor
import org.graphiks.dawn4k.mapper.allocateRenderBundleEncoderDescriptor
import org.graphiks.dawn4k.mapper.allocateRenderPassDescriptor
import org.graphiks.dawn4k.mapper.allocateRenderPipelineDescriptor
import org.graphiks.dawn4k.native.WGPUColorWriteMask_All
import org.graphiks.dawn4k.native.WGPULoadOp_Undefined
import org.graphiks.dawn4k.native.WGPUStoreOp_Undefined
import org.graphiks.dawn4k.native.WGPUTextureFormat_Undefined
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA8Unorm
import org.graphiks.dawn4k.native.WGPUVertexStepMode_Undefined
import org.graphiks.kffi.MemoryBuffer
import org.graphiks.kffi.memoryScope
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.RenderBundleEncoderDescriptor
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.VertexBufferLayout
import org.graphiks.webgpu.descriptors.VertexState
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SparseDescriptorsGpuTest {
    @Test
    fun drawingToSparseAttachmentOneProducesTheShaderColorNotTheClearColor() = runTest {
        if (!gpuTestEnvironment("SparseDescriptorsGpuTest.drawingToSparseAttachmentOneProducesTheShaderColorNotTheClearColor")) return@runTest
        NativeFixture.open().use { fixture ->
            val pixel = fixture.renderPixel(GPUTextureFormat.RGBA8Unorm, sparseSlots = true)
            assertEquals(emptyList(), fixture.session.callbacks.uncapturedErrors.map { it.message })
            assertContentEquals(byteArrayOf(-1, 0, 0, -1), pixel)
        }
    }

    @Test
    fun sparseNativeArraysKeepTheirIndicesAndUseThePinnedHeaderEmptySlots() = runTest {
        if (!gpuTestEnvironment("SparseDescriptorsGpuTest.sparseNativeArraysKeepTheirIndicesAndUseThePinnedHeaderEmptySlots")) return@runTest
        DawnContext.create(gpuTestConfig()).use { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                (adapter.requestDevice().getOrThrow() as DawnDevice).use { device ->
                    val session = device.session
                    device.createBindGroupLayout(BindGroupLayoutDescriptor(emptyList())).use { layout ->
                        memoryScope { allocator ->
                            val native = allocator.allocatePipelineLayoutDescriptor(
                                PipelineLayoutDescriptor(listOf(null, layout, null)), session,
                            )
                            assertEquals(3uL, native.bindGroupLayoutCount)
                            val addresses = MemoryBuffer(native.bindGroupLayouts!!, 24uL)
                            assertEquals(0uL, addresses.readULong())
                            assertEquals((layout as DawnBindGroupLayout).handle.handler.rawValue.toULong(), addresses.readULong(8uL))
                            assertEquals(0uL, addresses.readULong(16uL))
                        }
                    }
                    device.createShaderModule(ShaderModuleDescriptor(SHADER)).use { shader ->
                        val descriptor = RenderPipelineDescriptor(
                            vertex = VertexState(shader, entryPoint = "vs_main", buffers = listOf(null, VertexBufferLayout(0uL, emptyList()))),
                            fragment = FragmentState(module = shader, entryPoint = "fs_main", targets = listOf(null, ColorTargetState(GPUTextureFormat.RGBA8Unorm))),
                        )
                        memoryScope { allocator ->
                            val native = allocator.allocateRenderPipelineDescriptor(descriptor, session)
                            assertEquals(2uL, native.vertex.bufferCount)
                            val empty = native.vertex.buffers!!
                            assertEquals(WGPUVertexStepMode_Undefined, empty.stepMode)
                            assertEquals(0uL, empty.arrayStride)
                            assertEquals(0uL, empty.attributeCount)
                            assertNull(empty.attributes)
                            val fragment = native.fragment!!
                            assertEquals(2uL, fragment.targetCount)
                            assertEquals(WGPUTextureFormat_Undefined, fragment.targets!!.format)
                            assertNull(fragment.targets!!.blend)
                            assertEquals(WGPUColorWriteMask_All, fragment.targets!!.writeMask)
                        }
                        device.pushErrorScope(GPUErrorFilter.Validation)
                        device.createRenderPipeline(descriptor).close()
                        assertNull(device.popErrorScope().getOrThrow(), "Dawn must accept the sparse render pipeline")
                    }
                    device.createTexture(TextureDescriptor(Extent3D(1u), GPUTextureFormat.RGBA8Unorm, GPUTextureUsage.RenderAttachment)).use { texture ->
                        memoryScope { allocator ->
                            val views = mutableListOf<DawnTextureView>()
                            try {
                                val native = allocator.allocateRenderPassDescriptor(
                                    RenderPassDescriptor(listOf(null, RenderPassColorAttachment(texture, GPULoadOp.Clear, GPUStoreOp.Store))), session, views,
                                )
                                assertEquals(2uL, native.colorAttachmentCount)
                                val empty = native.colorAttachments!!
                                assertNull(empty.view)
                                assertNull(empty.resolveTarget)
                                assertEquals(UInt.MAX_VALUE, empty.depthSlice)
                                assertEquals(WGPULoadOp_Undefined, empty.loadOp)
                                assertEquals(WGPUStoreOp_Undefined, empty.storeOp)
                                assertEquals(0.0, empty.clearValue.a)
                            } finally { views.forEach { it.close() } }
                        }
                    }
                    memoryScope { allocator ->
                        val native = allocator.allocateRenderBundleEncoderDescriptor(
                            RenderBundleEncoderDescriptor(colorFormats = listOf(null, GPUTextureFormat.RGBA8Unorm, null)),
                        )
                        assertEquals(3uL, native.colorFormatCount)
                        val formats = UIntArray(3)
                        MemoryBuffer(native.colorFormats!!, 12uL).readUInts(formats)
                        assertContentEquals(uintArrayOf(WGPUTextureFormat_Undefined, WGPUTextureFormat_RGBA8Unorm, WGPUTextureFormat_Undefined), formats)
                    }
                }
            }
        }
    }

    private companion object {
        val SHADER = """
            @vertex fn vs_main(@builtin(vertex_index) i: u32) -> @builtin(position) vec4f {
                var p = array<vec2f, 3>(vec2f(-1.0, -1.0), vec2f(3.0, -1.0), vec2f(-1.0, 3.0));
                return vec4f(p[i], 0.0, 1.0);
            }
            @fragment fn fs_main() -> @location(1) vec4f { return vec4f(1.0, 0.0, 0.0, 1.0); }
        """.trimIndent()
    }
}
