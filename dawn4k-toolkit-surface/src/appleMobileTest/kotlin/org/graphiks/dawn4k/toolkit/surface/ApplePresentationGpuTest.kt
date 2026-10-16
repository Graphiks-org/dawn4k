@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CAMetalLayer
import kotlin.test.*

class ApplePresentationGpuTest {
    @Test fun rendersOnIosAndTvos() = appleMainTest {
        val layer = DawnPlatformOwners.appleMain().onOwner {
            val view = DawnMetalView(CGRectMake(0.0, 0.0, 16.0, 16.0))
            view.contentScaleFactor = 1.0
            view.layoutSubviews()
            view.layer as CAMetalLayer
        }
        val target = metalLayerTarget(layer, MutableStateFlow(DawnTargetState(1, PixelExtent(16, 16), true)))
        val presented = openDawnSurface(target)
        try {
            assertNotNull(presented.presentation.configuration.value)
            assertTrue(presented.presentation.withFrame { frame ->
                presented.gpu.run { gpu ->
                    (gpu.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
                        encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = listOf(
                            RenderPassColorAttachment(frame.view, loadOp = GPULoadOp.Clear, storeOp = GPUStoreOp.Store,
                                clearValue = Color(1.0, 0.0, 0.0, 1.0)),
                        ))).use { it.end() }
                        encoder.finish().use { gpu.queue.submit(listOf(it)) }
                    }
                    gpu.queue.onSubmittedWorkDone().getOrThrow()
                    frame.present()
                }
            })
            presented.gpu.run { gpu ->
                gpu.device.createTexture(TextureDescriptor(size = Extent3D(4u, 4u), format = GPUTextureFormat.RGBA8Unorm,
                    usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc)).use { texture ->
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
        } finally { presented.shutdown() }
    }
}
