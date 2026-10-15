package org.graphiks.dawn4k.toolkit.examples

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.PixelExtent
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class ExamplesGpuTest {
    @Test fun computeExampleReturnsDoubledValues() = runBlocking {
        withDawnSession(DawnSessionOptions(startupTimeout = 60.seconds)) { gpu ->
            assertContentEquals(intArrayOf(2, 4, 6, 8), runComputeExample(gpu))
            println("TOOLKIT_SAMPLE compute=passed adapter=${gpu.adapter.info.description}")
        }
    }
    @Test fun triangleExampleRendersExpectedPixels() = runBlocking {
        withDawnSession(DawnSessionOptions(startupTimeout = 60.seconds)) { gpu ->
            val errors = mutableListOf<GPUError>()
            val observation = gpu.device.observeUncapturedErrors { errors += it }
            try {
                gpu.createRenderTexture(16u, 16u, GPUTextureFormat.RGBA8Unorm).use { texture ->
                    TriangleRenderer(gpu, GPUTextureFormat.RGBA8Unorm).use { renderer ->
                        texture.createView().use { renderer.render(it, PixelExtent(16, 16)) }
                        gpu.device.createBuffer(BufferDescriptor(4096uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead)).use { buffer ->
                            (gpu.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
                                encoder.copyTextureToBuffer(TexelCopyTextureInfo(texture),
                                    TexelCopyBufferInfo(buffer, bytesPerRow = 256u, rowsPerImage = 16u), Extent3D(16u, 16u))
                                encoder.finish().use { gpu.queue.submit(listOf(it)) }
                            }
                            buffer.mapAsync(GPUMapMode.Read).getOrThrow()
                            val bytes = try { buffer.getMappedRange().toByteArray() } finally { buffer.unmap() }
                            val middle = 8 * 256 + 8 * 4
                            assertContentEquals(byteArrayOf(-1, 0, 0, -1), bytes.copyOfRange(middle, middle + 4))
                            assertContentEquals(byteArrayOf(0, 0, -1, -1), bytes.copyOfRange(0, 4))
                        }
                    }
                }
                gpu.queue.onSubmittedWorkDone().getOrThrow()
                gpu.context.processEvents()
                assertTrue(errors.isEmpty(), errors.joinToString { it.message })
                println("TOOLKIT_SAMPLE triangle=passed adapter=${gpu.adapter.info.description}")
            } finally { observation.close() }
        }
    }
}
