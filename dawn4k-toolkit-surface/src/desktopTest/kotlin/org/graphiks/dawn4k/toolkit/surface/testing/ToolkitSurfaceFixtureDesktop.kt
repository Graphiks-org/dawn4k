package org.graphiks.dawn4k.toolkit.surface.testing

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.graphiks.dawn4k.toolkit.glfw.*
import org.graphiks.dawn4k.toolkit.testing.ToolkitGpuEnvironment

internal actual fun toolkitSurfaceTest(block: suspend (ToolkitSurfaceFixture) -> Unit): Unit = runBlocking {
    withDawnWindow(DawnWindowOptions(width = 32, height = 32), ToolkitGpuEnvironment.options) { gpu, window, presentation ->
        block(ToolkitSurfaceFixture(gpu, presentation) { width, height ->
            val previous = window.extent.value
            window.resize(width, height)
            withTimeout(10_000) { window.extent.first { it != previous && it.isPositive } }
        })
    }
}
