package org.graphiks.dawn4k.webgpu.toolkit.interop

import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.webgpu.toolkit.Session
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Tests for the offscreen render texture helper on [ToolkitSessionView].
 *
 * Creating a texture needs a live device, which a GPU-less host cannot provide, so these cover
 * the validation that runs before the device is touched. The happy path is exercised by the
 * desktop toolkit jobs, which run on real adapters.
 */
class ToolkitSessionViewOffscreenTest {

    @Test
    fun `a zero width is rejected before the device is touched`() {
        val view = ToolkitSessionView(sessionStub(), Dawn4kWebGpuToolkitExtension())

        val rejected = assertFailsWith<IllegalArgumentException> {
            view.createRenderTexture(width = 0u, height = 16u, format = GPUTextureFormat.RGBA8Unorm)
        }
        assertTrue(rejected.message!!.contains("positive"), rejected.message!!)
    }

    @Test
    fun `a zero height is rejected before the device is touched`() {
        val view = ToolkitSessionView(sessionStub(), Dawn4kWebGpuToolkitExtension())

        val rejected = assertFailsWith<IllegalArgumentException> {
            view.createRenderTexture(width = 16u, height = 0u, format = GPUTextureFormat.RGBA8Unorm)
        }
        assertTrue(rejected.message!!.contains("positive"), rejected.message!!)
    }

    /** A session whose device is never reached: the validation runs first. */
    private fun sessionStub(): Session {
        val device = java.lang.reflect.Proxy.newProxyInstance(
            GPUDevice::class.java.classLoader,
            arrayOf(GPUDevice::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getLabel" -> ""
                "hashCode" -> System.identityHashCode(method)
                "toString" -> "device-stub"
                else -> throw AssertionError("the device must not be touched: ${method.name}")
            }
        } as GPUDevice

        return object : Session {
            override val device: GPUDevice = device
            override val queue: org.graphiks.webgpu.GPUQueue
                get() = throw AssertionError("the queue must not be touched")
            // The view publishes the lifecycle, so a real value is needed; it is not a device.
            override val state: kotlinx.coroutines.flow.StateFlow<org.graphiks.webgpu.toolkit.SessionState> =
                kotlinx.coroutines.flow.MutableStateFlow(org.graphiks.webgpu.toolkit.SessionState.Ready)
            override val manualProgress: org.graphiks.webgpu.toolkit.ManualProgression? = null
            override suspend fun awaitLost(): org.graphiks.webgpu.GPUDeviceLostInfo =
                throw AssertionError("awaitLost must not be called")
            override suspend fun <R> run(
                block: suspend (Session) -> R,
            ): R = throw AssertionError("run must not be called")
            override suspend fun shutdown() = throw AssertionError("shutdown must not be called")
        }
    }
}
