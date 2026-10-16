package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.toolkit.withDawnSession
import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*
import kotlin.test.*

class AndroidPresentationGpuTest {
    @Test fun rendersAndReadsBackOnAndroid() = runBlocking {
        withDawnSession { gpu ->
            gpu.device.createTexture(TextureDescriptor(
                size = Extent3D(width = 4u, height = 4u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
            )).use { texture ->
                gpu.device.createBuffer(BufferDescriptor(1024uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)).use { staging ->
                    (gpu.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
                        encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = listOf(
                            RenderPassColorAttachment(texture, loadOp = GPULoadOp.Clear, storeOp = GPUStoreOp.Store,
                                clearValue = Color(0.0, 0.0, 1.0, 1.0)),
                        ))).use { it.end() }
                        encoder.copyTextureToBuffer(TexelCopyTextureInfo(texture),
                            TexelCopyBufferInfo(staging, bytesPerRow = 256u, rowsPerImage = 4u), Extent3D(4u, 4u))
                        encoder.finish().use { gpu.queue.submit(listOf(it)) }
                    }
                    gpu.queue.onSubmittedWorkDone().getOrThrow()
                    staging.mapAsync(GPUMapMode.Read).getOrThrow()
                    try { assertContentEquals(byteArrayOf(0, 0, -1, -1), staging.getMappedRange().toByteArray().copyOfRange(0, 4)) }
                    finally { staging.unmap() }
                }
            }
        }
    }
}
