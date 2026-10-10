package org.graphiks.dawn4k.demo

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnContext
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*
import org.graphiks.webgpu.suite.demos.particles.ParticleScene
import org.graphiks.webgpu.suite.demos.particles.initialParticles
import kotlin.test.Test
import kotlin.test.assertTrue

class AndroidParticleRenderTest {
    @Test fun vulkanParticleDrawProducesNonblackPixelsNotJustSuccessfulPresent() = runBlocking {
        DawnContext.create(DawnConfig(backend = DawnBackend.Vulkan, implicitDeviceSynchronization = true)).useWithDemoEventProgress { _, _, device ->
                    device.pushErrorScope(GPUErrorFilter.Validation)
                    device.createTexture(TextureDescriptor(size = Extent3D(64u, 64u),
                        format = GPUTextureFormat.RGBA8Unorm,
                        usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc)).use { texture ->
                        texture.createView().use { view ->
                            ParticleScene.create(device, GPUTextureFormat.RGBA8Unorm, initialParticles(256)).use { scene ->
                                device.createBuffer(BufferDescriptor(16384uL,
                                    GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)).use { staging ->
                                    device.createCommandEncoder().use { encoder ->
                                        scene.encodeFrame(encoder, view, 64, 64, 0f)
                                        encoder.copyTextureToBuffer(TexelCopyTextureInfo(texture),
                                            TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 64u),
                                            Extent3D(64u, 64u))
                                        encoder.finish().use { device.queue.submit(listOf(it)) }
                                    }
                                    device.queue.onSubmittedWorkDone().getOrThrow()
                                    assertTrue(device.popErrorScope().getOrThrow() == null)
                                    staging.mapAsync(GPUMapMode.Read).getOrThrow()
                                    try {
                                        val mapped = staging.getMappedRange()
                                        // Keep diagnostics usable on Android ART: pinned upstream
                                        // bulk readBytes references an absent Unsafe constant.
                                        val pixels = ByteArray(16384) { mapped.getByte(it.toULong()) }
                                        assertTrue(pixels.indices.any { it % 4 != 3 && pixels[it] != 0.toByte() },
                                            "real particle draw must produce nonblack RGB pixels")
                                    } finally { staging.unmap() }
                                }
                            }
                        }
                    }
        }
    }
}
