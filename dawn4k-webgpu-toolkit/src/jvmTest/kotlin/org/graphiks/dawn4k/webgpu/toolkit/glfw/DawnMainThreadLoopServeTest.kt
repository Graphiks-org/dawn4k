package org.graphiks.dawn4k.webgpu.toolkit.glfw

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnPresentation
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceTarget
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.toolkit.Session
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The loop's server mode: it serves the process main thread until stopped, and a stop shuts
 * every registered window down before it returns, exactly like the session mode does at the
 * end of its block.
 */
class DawnMainThreadLoopServeTest {

    @Test fun serveExitsWhenStoppedWithNoWindows() = runBlocking {
        val backend = RecordingBackend()
        val loop = DawnMainThreadLoop.createForServing(backend)
        val stopper = launch(Dispatchers.Default) {
            delay(100)
            loop.stopServing()
        }
        loop.serve()
        stopper.join()
        assertEquals(0, backend.destroys)
    }

    @Test fun serveShutsRegisteredWindowsDownAndExits() = runBlocking {
        val backend = RecordingBackend()
        val loop = DawnMainThreadLoop.createForServing(backend)
        val opened = launch(Dispatchers.Default) {
            loop.owner.onOwner {
                val runtime = loop.runtime.acquire(DawnGlfwRuntimeOwnership.Owned)
                val handle = loop.backend.createWindow(DawnWindowOptions())
                val window = DawnWindow(loop, NativeWindowLease(loop.backend, handle, runtime), FakeWindowSession())
                loop.register(window)
            }
            delay(200)
            loop.stopServing()
        }
        loop.serve()
        opened.join()
        assertEquals(1, backend.destroys, "the registered window must be destroyed by the stop")
        assertEquals(1, backend.terminations, "the window's runtime must be retired by the stop")
    }

    @Test fun stopWithoutServeIsHarmlessAndReleasesTheLoop() = runBlocking {
        val first = DawnMainThreadLoop.createForServing(RecordingBackend())
        first.stopServing()
        first.serve()
        // The gate is free again: a second loop serves and exits the same way.
        val second = DawnMainThreadLoop.createForServing(RecordingBackend())
        second.stopServing()
        second.serve()
        assertTrue(true)
    }
}

/** A window session with no GPU behind it: the stop path only touches [session]'s manual progress. */
private class FakeWindowSession : WindowSession {
    override val session: Session = sessionStub()
    override val presentation: DawnPresentation get() = error("the stop path must not touch the presentation")
    override val surfaceTarget: DawnSurfaceTarget get() = error("the stop path must not touch the target")
    override suspend fun shutdown() {}
}

/** A session whose device is never reached, mirroring the offscreen test's stub. */
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
        override val state: kotlinx.coroutines.flow.StateFlow<org.graphiks.webgpu.toolkit.SessionState> =
            kotlinx.coroutines.flow.MutableStateFlow(org.graphiks.webgpu.toolkit.SessionState.Ready)
        override val manualProgress: org.graphiks.webgpu.toolkit.ManualProgression? = null
        override suspend fun awaitLost(): org.graphiks.webgpu.GPUDeviceLostInfo =
            throw AssertionError("awaitLost must not be called")
        override suspend fun <R> run(block: suspend (Session) -> R): R =
            throw AssertionError("run must not be called")
        override suspend fun shutdown() = throw AssertionError("shutdown must not be called")
    }
}
