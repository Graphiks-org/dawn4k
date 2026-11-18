package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.webgpu.toolkit.internal.Dawn4kSurfaceHandle
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType
import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUQueueDescriptor
import org.graphiks.webgpu.GPURequiredLimits
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.SessionConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Checks that the session options reach the surface handle, which opens its own dawn4k
 * presentation and so must open it the way the session asked for.
 */
class Dawn4kToolkitInstanceSessionOptionsTest {

    private fun createTarget(): Dawn4kSurfaceTarget = Dawn4kSurfaceTarget(
        dawnTarget = null,
        platformType = PlatformSurfaceType.MetalLayer,
        initialExtent = Extent2D(width = 640u, height = 480u),
    )

    @Test
    fun `createSurface hands the session device descriptor and timeouts to the handle`() = runTest {
        val descriptor = object : GPUDeviceDescriptor {
            override val requiredFeatures: List<GPUFeatureName> = emptyList()
            override val requiredLimits: GPURequiredLimits? = null
            override val defaultQueue: GPUQueueDescriptor = object : GPUQueueDescriptor {
                override val label: String = ""
            }
            override val onUncapturedError: GPUUncapturedErrorCallback? = null
            override val label: String = "session-device"
        }
        val config = SessionConfig(
            startupTimeout = 3.seconds,
            shutdownTimeout = 4.seconds,
            deviceDescriptor = descriptor,
        )
        val extension = Dawn4kWebGpuToolkitExtension()
        val instance = extension.open(config)

        try {
            val target = createTarget()
            val lease = target.retain()
            val handle = instance.createSurface(createAdapterStub(), lease) as Dawn4kSurfaceHandle

            assertSame(descriptor, handle.deviceDescriptor)
            assertEquals(3.seconds, handle.startupTimeout)
            assertEquals(4.seconds, handle.shutdownTimeout)

            handle.close()
            target.close()
        } finally {
            instance.close()
        }
    }

    @Test
    fun `createSurface without a descriptor leaves the presentation on its defaults`() = runTest {
        val extension = Dawn4kWebGpuToolkitExtension()
        val instance = extension.open(SessionConfig())

        try {
            val target = createTarget()
            val lease = target.retain()
            val handle = instance.createSurface(createAdapterStub(), lease) as Dawn4kSurfaceHandle

            assertEquals(null, handle.deviceDescriptor)
            assertEquals(10.seconds, handle.startupTimeout)

            handle.close()
            target.close()
        } finally {
            instance.close()
        }
    }

    @Test
    fun `close from a foreign thread tears the instance down without racing the pump`() {
        // The raw extension API is not marshalled by the toolkit core, so close() must
        // marshal itself onto the execution owner: the pump there may be mid-iteration
        // when a foreign thread decides to close, and the context refuses to close
        // during processEvents().
        repeat(10) {
            val extension = Dawn4kWebGpuToolkitExtension()
            val instance = extension.open(SessionConfig())
            val done = java.util.concurrent.CountDownLatch(1)
            val failure = java.util.concurrent.atomic.AtomicReference<Throwable?>()
            Thread {
                try {
                    instance.close()
                } catch (t: Throwable) {
                    failure.set(t)
                } finally {
                    done.countDown()
                }
            }.start()
            assertTrue(done.await(15, java.util.concurrent.TimeUnit.SECONDS), "close() never returned")
            failure.get()?.let { throw it }
            // The second close is a no-op, not a second teardown racing the first.
            instance.close()
        }
    }

    private fun createAdapterStub(): org.graphiks.webgpu.GPUAdapter =
        java.lang.reflect.Proxy.newProxyInstance(
            org.graphiks.webgpu.GPUAdapter::class.java.classLoader,
            arrayOf(org.graphiks.webgpu.GPUAdapter::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getLabel" -> ""
                "hashCode" -> System.identityHashCode(method)
                "equals" -> false
                "toString" -> "adapter-stub"
                else -> null
            }
        } as org.graphiks.webgpu.GPUAdapter
}
