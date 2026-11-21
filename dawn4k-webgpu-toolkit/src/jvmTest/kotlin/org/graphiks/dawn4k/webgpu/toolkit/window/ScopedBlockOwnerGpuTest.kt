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
 * thread. Lives in its own class because two real-window GPU sessions in one JVM hang
 * (see the ledger's sequential-flake finding); run gated suites one GPU class per invocation.
 */
class ScopedBlockOwnerGpuTest {
    @Test fun scopedBlockRunsAdmittedOnTheOwner() {
        assumeTrue(System.getenv("DAWN_DESKTOP_TESTS") == "1")
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
