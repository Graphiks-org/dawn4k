package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.webgpu.toolkit.internal.Dawn4kSurfaceHandle
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.toolkit.AlphaMode
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import org.graphiks.webgpu.toolkit.PresentMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JVM-side tests for the surface handle's configuration, which need a device stand-in and so
 * cannot live in commonTest.
 */
class Dawn4kSurfaceHandleJvmTest {

    private fun createSurfaceTarget(): Dawn4kSurfaceTarget = Dawn4kSurfaceTarget(
        dawnTarget = null,
        platformType = PlatformSurfaceType.MetalLayer,
        initialExtent = Extent2D(width = 800u, height = 600u),
    )

    @Test
    fun `configure accepts a negotiated present mode`() = runTest {
        val target = createSurfaceTarget()
        val handle = Dawn4kSurfaceHandle(target)

        handle.configure(
            PresentationConfiguration(
                device = createMockDevice(),
                format = GPUTextureFormat.BGRA8Unorm,
                alphaMode = AlphaMode.Opaque,
                presentMode = PresentMode.Mailbox,
            ),
        )

        handle.close()
        target.close()
    }

    @Test
    fun `configure rejects a format the surface cannot present`() = runTest {
        val target = createSurfaceTarget()
        val handle = Dawn4kSurfaceHandle(target)

        try {
            handle.configure(
                PresentationConfiguration(
                    device = createMockDevice(),
                    format = GPUTextureFormat.Depth32Float,
                ),
            )
            assertTrue(false, "Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Depth32Float"), e.message!!)
        }

        handle.close()
        target.close()
    }

    @Test
    fun `configure rejects a post-multiplied surface the handle does not offer`() = runTest {
        val target = createSurfaceTarget()
        val handle = Dawn4kSurfaceHandle(target)

        // Post-multiplied is offered; a mode outside the advertised set is refused rather than
        // silently replaced.
        handle.configure(
            PresentationConfiguration(
                device = createMockDevice(),
                format = GPUTextureFormat.BGRA8Unorm,
                alphaMode = AlphaMode.PostMultiplied,
                presentMode = PresentMode.Immediate,
            ),
        )

        handle.close()
        target.close()
    }

    @Test
    fun `the advertised present modes cover every dawn4k present mode`() {
        val handle = Dawn4kSurfaceHandle(createSurfaceTarget())

        val advertised = handle.capabilities.presentModes
        assertEquals(
            listOf(PresentMode.Fifo, PresentMode.FifoRelaxed, PresentMode.Mailbox, PresentMode.Immediate),
            advertised,
        )

        handle.close()
    }

    private fun createMockDevice(): GPUDevice =
        java.lang.reflect.Proxy.newProxyInstance(
            GPUDevice::class.java.classLoader,
            arrayOf(GPUDevice::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getLabel" -> ""
                "hashCode" -> System.identityHashCode(method)
                "equals" -> false
                "toString" -> "mock-device"
                else -> null
            }
        } as GPUDevice
}
