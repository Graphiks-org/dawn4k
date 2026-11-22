package org.graphiks.dawn4k.webgpu.toolkit.window

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnSurfaceReference
import org.graphiks.dawn4k.webgpu.toolkit.glfw.RecordingBackend
import org.graphiks.dawn4k.webgpu.toolkit.glfw.internal.*
import org.graphiks.dawn4k.webgpu.toolkit.surface.*
import org.graphiks.dawn4k.webgpu.toolkit.surface.internal.*
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.toolkit.surface.Window
import org.graphiks.webgpu.toolkit.surface.WindowConfig
import org.graphiks.webgpu.toolkit.surface.WindowRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The host's serving contract: [GlfwWindowHost.open] returns a live window only while the host
 * serves, a stop shuts the remaining windows down, and opening without a serve fails with a
 * message that names the fix.
 */
class GlfwWindowHostServeTest {

    @Test fun openWithoutServeFailsWithActionableMessage() = runBlocking {
        val host = GlfwWindowHost()
        val failure = assertFailsWith<IllegalStateException> { host.open(WindowConfig()) }
        assertTrue(failure.message!!.contains("serving"), failure.message)
        assertTrue(failure.message!!.contains("process main thread"), failure.message)
    }

    @Test fun openReturnsALiveWindowWhileServing() = runBlocking {
        val backend = FakeSurfaceBackend()
        val host = GlfwWindowHost(WindowRuntime.Owned).apply { surfaceFactory = fakeSurfaceFactory() }
        var opened: Window? = null
        val driver = launch(Dispatchers.Default) {
            try {
                // Serving must be live before open: a loop without a serve shuts the window down.
                withTimeout(5_000) { while (!host.isServing) delay(10) }
                val window = host.open(WindowConfig(logicalWidth = 64, logicalHeight = 64))
                // The window is alive at open: its handle passes the liveness guard.
                assertTrue((window as GlfwWindow).dawnWindow.nativeHandle != 0L)
                opened = window
            } finally {
                host.close()
            }
        }
        host.serve(backend)
        driver.join()
        assertNotNull(opened)
        assertEquals(1, backend.destroys, "the stop must shut the opened window down")
        assertEquals(1, backend.terminations, "the opened window's runtime must be retired by the stop")
    }

    @Test fun closeIsIdempotent() {
        val host = GlfwWindowHost()
        host.close()
        host.close()
    }
}

/** A recording backend whose targets retain into fake leases, so a session opens without natives. */
private class FakeSurfaceBackend : RecordingBackend() {
    override fun surfaceTarget(handle: Long): DawnSurfaceTarget = object : DawnSurfaceTarget {
        override val state = MutableStateFlow(DawnTargetState(handle, PixelExtent(300, 150), true))
        override suspend fun retain(): DawnTargetLease = object : DawnTargetLease {
            override val state = MutableStateFlow(DawnTargetState(handle, PixelExtent(300, 150), true))
            override fun createSurface(context: DawnContext): DawnSurfaceReference =
                error("the fake factory never asks the lease for a reference")
            override suspend fun retire() {}
        }
    }
}

/** Binds retained targets to a fake native surface; negotiation runs against the advertised enums. */
private fun fakeSurfaceFactory() = SurfaceFactory { lease, _ ->
    SurfaceAttachment(
        lease,
        null,
        { _, _ -> FakeNativeSurface() },
        {},
    )
}

private class FakeNativeSurface : NativeSurface {
    override fun capabilities() = SurfaceCapabilities(
        formats = listOf(GPUTextureFormat.RGBA8Unorm),
        alphaModes = listOf(DawnAlphaMode.Opaque),
        presentModes = listOf(DawnPresentMode.Fifo),
    )
    override fun configure(configuration: DawnSurfaceConfiguration) {}
    override fun unconfigure() {}
    override fun acquire(): SurfaceAcquisition = error("no frame acquisition in this fixture")
    override fun present() {}
    override fun close() {}
}
