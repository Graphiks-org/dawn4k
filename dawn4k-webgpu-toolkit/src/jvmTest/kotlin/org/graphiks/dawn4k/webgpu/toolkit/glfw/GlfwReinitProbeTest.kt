package org.graphiks.dawn4k.webgpu.toolkit.glfw

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test

/** Throwaway probe: two real GLFW init/terminate cycles in one JVM (macOS Cocoa re-init). */
class GlfwReinitProbeTest {
    @Test fun cycle1() = runBlocking {
        assumeTrue(System.getenv("DAWN_DESKTOP_TESTS") == "1")
        val loop = DawnMainThreadLoop.createForServing(null)
        val lease = loop.runtime.acquire(DawnGlfwRuntimeOwnership.Owned)
        lease.retire()
    }

    @Test fun cycle2() = runBlocking {
        assumeTrue(System.getenv("DAWN_DESKTOP_TESTS") == "1")
        val loop = DawnMainThreadLoop.createForServing(null)
        val lease = loop.runtime.acquire(DawnGlfwRuntimeOwnership.Owned)
        lease.retire()
    }
}
