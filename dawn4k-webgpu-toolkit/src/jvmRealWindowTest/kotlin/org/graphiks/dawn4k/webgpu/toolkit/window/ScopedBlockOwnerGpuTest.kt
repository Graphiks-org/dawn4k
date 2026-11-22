package org.graphiks.dawn4k.webgpu.toolkit.window

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.dawn4k.webgpu.toolkit.glfw.withDawnWindow
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The scoped entries hand their block to the caller already admitted on the session's GPU
 * owner: native calls made directly inside the block must not leave the dawn4k context
 * thread. Lives in its own class and gates on [DAWN_REAL_WINDOW_TESTS] rather than
 * [DAWN_DESKTOP_TESTS]: a JVM must hold at most one real-window GPU session, and CI's
 * gated suites already give that slot to `DawnWindowTest.liveWindowRendersAndCloses`.
 * Run locally with `DAWN_DESKTOP_TESTS=1 DAWN_REAL_WINDOW_TESTS=1` in a dedicated
 * Gradle invocation.
 */
class ScopedBlockOwnerGpuTest {
    @Test fun scopedBlockRunsAdmittedOnTheOwner() {
        assumeTrue(System.getenv("DAWN_REAL_WINDOW_TESTS") == "1")
        val extension = Dawn4kWebGpuToolkitExtension(DawnConfig(DawnBackend.Metal))
        runBlocking {
            withDawnWindow(
                org.graphiks.dawn4k.webgpu.toolkit.glfw.DawnWindowOptions(width = 64, height = 64),
                extension = extension,
            ) { session, _, _ ->
                assertTrue(
                    Thread.currentThread().name.startsWith("dawn4k-toolkit"),
                    "the scoped block must run on the session's GPU owner, not ${Thread.currentThread().name}",
                )
            }
        }
    }
}
