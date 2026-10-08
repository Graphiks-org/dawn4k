package org.graphiks.dawn4k

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.native.wgpuTextureCreateView
import org.graphiks.dawn4k.native.wgpuTextureViewRelease
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.Color
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RenderPassColorAttachment
import org.graphiks.webgpu.descriptors.RenderPassDescriptor
import org.graphiks.webgpu.descriptors.TexelCopyBufferInfo
import org.graphiks.webgpu.descriptors.TexelCopyTextureInfo
import org.graphiks.webgpu.descriptors.TextureDescriptor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class SurfaceTextureViewGpuTest {
    private fun texture(fixture: NativeFixture): DawnTexture = fixture.createTexture(TextureDescriptor(
        size = Extent3D(4u, 4u), format = GPUTextureFormat.RGBA8Unorm,
        usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
    ))

    @Test
    fun explicitCloseRemovesAdoptedReferenceOnceBeforeDeviceTeardown() = runTest {
        NativeFixture.open().use { fixture ->
            val device = DawnDevice(fixture.session, null, false)
            texture(fixture).use { texture ->
                val before = fixture.session.resources.debugRemainingRefs()
                val raw = fixture.runtime.dispatcher.call { assertNotNull(wgpuTextureCreateView(texture.handle, null)) }
                val view = device.adoptSurfaceTextureView(raw.handler.rawValue, "adopted")
                assertEquals("adopted", view.label)
                assertEquals(before + 1, fixture.session.resources.debugRemainingRefs())
                view.close()
                view.close()
                assertEquals(before, fixture.session.resources.debugRemainingRefs())
            }
            device.close()
            device.close()
            assertEquals(0, fixture.session.resources.debugRemainingRefs())
        }
    }

    @Test
    fun deviceTeardownReleasesUnclosedAdoptedViewAndLateCloseIsHarmless() = runTest {
        NativeFixture.open().use { fixture ->
            val device = DawnDevice(fixture.session, null, false)
            val texture = texture(fixture)
            val raw = fixture.runtime.dispatcher.call { assertNotNull(wgpuTextureCreateView(texture.handle, null)) }
            val view = device.adoptSurfaceTextureView(raw.handler.rawValue)
            assertEquals(3, fixture.session.resources.debugRemainingRefs(), "queue, texture and adopted view")
            device.close()
            assertEquals(0, fixture.session.resources.debugRemainingRefs())
            view.close()
            texture.close()
            assertEquals(0, fixture.session.resources.debugRemainingRefs())
        }
    }

    @Test
    fun rejectedAdoptionLeavesLiveViewReferenceWithCaller() = runTest {
        NativeFixture.open().use { fixture ->
            val device = DawnDevice(fixture.session, null, false)
            val texture = texture(fixture)
            val raw = fixture.runtime.dispatcher.call { assertNotNull(wgpuTextureCreateView(texture.handle, null)) }
            try {
                device.close()
                assertFailsWith<IllegalStateException> { device.adoptSurfaceTextureView(raw.handler.rawValue) }
                assertEquals(0, fixture.session.resources.debugRemainingRefs())
            } finally {
                // Failure must not transfer/release the raw reference. Caller still owns it.
                fixture.runtime.dispatcher.call { wgpuTextureViewRelease(raw) }
            }
        }
    }

    @Test
    fun adoptedViewCanRenderAndReadBackRealAttachmentPixels() = runTest {
        NativeFixture.open().use { fixture ->
            val device = DawnDevice(fixture.session, null, false)
            texture(fixture).use { texture ->
                val raw = fixture.runtime.dispatcher.call { assertNotNull(wgpuTextureCreateView(texture.handle, null)) }
                device.adoptSurfaceTextureView(raw.handler.rawValue).use { view ->
                    fixture.createBuffer(BufferDescriptor(1024uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)).use { staging ->
                        fixture.createEncoder().use { encoder ->
                            encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = listOf(
                                RenderPassColorAttachment(view = view, loadOp = GPULoadOp.Clear,
                                    storeOp = GPUStoreOp.Store, clearValue = Color(0.0, 0.0, 1.0, 1.0)),
                            ))).end()
                            encoder.copyTextureToBuffer(TexelCopyTextureInfo(texture),
                                TexelCopyBufferInfo(staging, bytesPerRow = 256u), Extent3D(4u, 4u))
                            fixture.submit(encoder)
                        }
                        fixture.queue.onSubmittedWorkDone().getOrThrow()
                        staging.mapAsync(GPUMapMode.Read).getOrThrow()
                        try {
                            assertContentEquals(byteArrayOf(0, 0, -1, -1),
                                staging.getMappedRange().toByteArray().copyOfRange(0, 4))
                        } finally { staging.unmap() }
                    }
                }
            }
        }
    }
}
