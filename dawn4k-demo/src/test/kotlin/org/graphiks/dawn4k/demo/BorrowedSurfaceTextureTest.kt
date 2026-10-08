package org.graphiks.dawn4k.demo

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.native.*
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.descriptors.CommandEncoderDescriptor
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.Color
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BorrowedSurfaceTextureTest {
    @Test
    fun closingBorrowedReferenceDoesNotDestroyTextureAndRefusesAnotherView() = runBlocking {
        DawnContext.create().use { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                (adapter.requestDevice().getOrThrow() as DawnDevice).use { device ->
                    val bridge = context.nativeBridge()
                    val raw = bridge.call {
                        memoryScope { allocator ->
                            val descriptor = WGPUTextureDescriptor.allocate(allocator)
                            descriptor.nextInChain = null
                            descriptor.label.data = null
                            descriptor.label.length = 0uL
                            descriptor.usage = WGPUTextureUsage_RenderAttachment
                            descriptor.dimension = WGPUTextureDimension_2D
                            descriptor.size.width = 4u
                            descriptor.size.height = 4u
                            descriptor.size.depthOrArrayLayers = 1u
                            descriptor.format = WGPUTextureFormat_RGBA8Unorm
                            descriptor.mipLevelCount = 1u
                            descriptor.sampleCount = 1u
                            descriptor.viewFormatCount = 0uL
                            descriptor.viewFormats = null
                            assertNotNull(wgpuDeviceCreateTexture(WGPUDevice(NativeAddress(device.nativeHandle())), descriptor))
                        }
                    }
                    try {
                        bridge.call { wgpuTextureAddRef(raw) }
                        val borrowed = BorrowedSurfaceTexture(bridge, raw)
                        borrowed.createView(device).use { }
                        borrowed.close()
                        borrowed.close()
                        assertTrue(borrowed.released)
                        assertFailsWith<IllegalStateException> { borrowed.createView(device) }
                        // Our independent reference must remain usable: Destroy would
                        // invalidate a render attachment despite its live reference.
                        device.pushErrorScope(org.graphiks.webgpu.GPUErrorFilter.Validation)
                        val view = bridge.call { assertNotNull(wgpuTextureCreateView(raw, null)) }
                        device.adoptSurfaceTextureView(view.handler.rawValue).use { attachment ->
                            device.createCommandEncoder(CommandEncoderDescriptor()).use { encoder ->
                                encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = listOf(
                                    RenderPassColorAttachment(view = attachment, loadOp = GPULoadOp.Clear,
                                        storeOp = GPUStoreOp.Store, clearValue = Color(0.0, 0.0, 1.0, 1.0)),
                                ))).end()
                                encoder.finish().use { commands -> device.queue.submit(listOf(commands)) }
                            }
                            device.queue.onSubmittedWorkDone().getOrThrow()
                            assertTrue(device.popErrorScope().getOrThrow() == null, "borrowed close must not destroy")
                        }
                    } finally { bridge.call { wgpuTextureRelease(raw) } }
                }
            }
        }
    }
}
