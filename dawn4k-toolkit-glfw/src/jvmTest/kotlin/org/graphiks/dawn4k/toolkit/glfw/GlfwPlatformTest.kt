package org.graphiks.dawn4k.toolkit.glfw

import org.graphiks.dawn4k.toolkit.glfw.internal.*
import org.graphiks.dawn4k.toolkit.surface.PixelExtent
import kotlin.test.*

class GlfwPlatformTest {
    @Test fun mainThreadIsAcceptedAndWorkerNamedMainIsRejected() {
        requireGlfwMainThread()
        var failure: Throwable? = null
        Thread({ failure = runCatching { requireGlfwMainThread() }.exceptionOrNull() }, "main").apply {
            start()
            join()
        }
        assertIs<IllegalStateException>(failure)
    }

    @Test fun usesFramebufferSizeNotLogicalSize() {
        val backend = RecordingBackend()
        assertEquals(PixelExtent(300, 150), WindowSurfaceState(backend, 1).extent.value)
    }
    @Test fun linuxUsesActualGlfwPlatform() {
        assertEquals(GlfwPlatform.X11, glfwPlatform(0x00060004))
        assertEquals(GlfwPlatform.Wayland, glfwPlatform(0x00060003))
        assertFailsWith<UnsupportedOperationException> { glfwPlatform(0) }
    }
}
