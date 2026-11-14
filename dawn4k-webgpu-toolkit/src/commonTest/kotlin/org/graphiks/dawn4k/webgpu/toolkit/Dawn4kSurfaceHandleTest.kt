package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.DawnSurfaceReference
import org.graphiks.dawn4k.webgpu.toolkit.internal.Dawn4kSurfaceHandle
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.AlphaMode
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Dawn4kSurfaceHandleTest {

    private fun createSurfaceTarget(): Dawn4kSurfaceTarget {
        val extent = Extent2D(width = 800u, height = 600u)
        
        return Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.MetalLayer,
            initialExtent = extent
        )
    }

    @Test
    fun `capabilities returns supported formats and alpha modes`() = runTest {
        val target = createSurfaceTarget()
        val handle = Dawn4kSurfaceHandle(target)
        
        val capabilities = handle.capabilities
        assertNotNull(capabilities)
        
        // Check preferred format
        assertEquals(GPUTextureFormat.BGRA8Unorm, capabilities.preferredFormat)
        
        // Check formats list
        assertTrue(GPUTextureFormat.BGRA8Unorm in capabilities.formats)
        assertTrue(GPUTextureFormat.RGBA8Unorm in capabilities.formats)
        assertTrue(GPUTextureFormat.RGBA16Float in capabilities.formats)
        
        // Check alpha modes
        assertTrue(AlphaMode.Opaque in capabilities.alphaModes)
        assertTrue(AlphaMode.Premultiplied in capabilities.alphaModes)
        
        handle.close()
        target.close()
    }

    @Test
    fun `acquireFrame without configure throws`() = runTest {
        val target = createSurfaceTarget()
        val handle = Dawn4kSurfaceHandle(target)
        
        try {
            handle.acquireFrame()
            assertTrue(false, "Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Surface must be configured before acquiring frames", e.message)
        }
        
        handle.close()
        target.close()
    }

    @Test
    fun `present without acquired frame throws`() = runTest {
        val target = createSurfaceTarget()
        val handle = Dawn4kSurfaceHandle(target)
        
        val fakeFrame = AcquiredFrame(
            view = createMockTextureView(),
            extent = Extent2D(width = 100u, height = 100u)
        )
        
        try {
            handle.present(fakeFrame)
            assertTrue(false, "Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // Since the surface is not configured, it will throw "Surface must be configured before presenting"
            assertEquals("Surface must be configured before presenting", e.message)
        }
        
        handle.close()
        target.close()
    }

    @Test
    fun `close is idempotent`() = runTest {
        val target = createSurfaceTarget()
        val handle = Dawn4kSurfaceHandle(target)
        
        handle.close()
        handle.close() // Should not throw
        
        target.close()
    }

    private fun createMockTextureView(): org.graphiks.webgpu.GPUTextureView {
        return object : org.graphiks.webgpu.GPUTextureView {
            override var label: String = "mock-texture-view"
            override fun close() {
            }
        }
    }
}
