package org.graphiks.dawn4k.webgpu.toolkit.window

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.junit.Assume.assumeTrue
import org.graphiks.webgpu.toolkit.surface.withWindow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The canonical desktop path, end to end: `withWindow` on a served [GlfwWindowHost] opens a
 * live window, presents to it, and closes it on exit. This is the regression test for the
 * upstream break of 2026-11-21, where a window opened through the host was shut down by the
 * loop before the caller ever saw it ("GLFW main loop is stopped").
 */
class CanonicalWithWindowGpuTest {
    @Test fun canonicalWithWindowPresentsOnAServedHost() {
        assumeTrue(System.getenv("DAWN_DESKTOP_TESTS") == "1")
        val backend = DawnBackend.valueOf(System.getenv("DAWN_TEST_BACKEND") ?: "Metal")
        val extension = Dawn4kWebGpuToolkitExtension(DawnConfig(backend))
        val host = GlfwWindowHost()
        runBlocking {
            val driver = launch(Dispatchers.Default) {
                try {
                    // Serving must be live before open: a loop without a serve shuts the window down.
                    withTimeout(5_000) { while (!host.isServing) delay(10) }
                    withWindow(extension, host, org.graphiks.webgpu.toolkit.surface.WindowConfig(logicalWidth = 64, logicalHeight = 64)) { session, window, surface ->
                        // The window reports zero until the platform lays it out; wait for the first layout.
                        withTimeout(5_000) {
                            window.state.first { it.extent.width > 0u && it.extent.height > 0u }
                        }
                        assertTrue(window.state.value.extent.width > 0u, "the laid-out window must report a live extent")
                        val frame = surface.acquireFrame()
                        frame.present()
                        surface.releaseFrame(frame)
                    }
                } finally {
                    host.close()
                }
            }
            withTimeout(60_000) { host.serve() }
            driver.join()
        }
    }
}
