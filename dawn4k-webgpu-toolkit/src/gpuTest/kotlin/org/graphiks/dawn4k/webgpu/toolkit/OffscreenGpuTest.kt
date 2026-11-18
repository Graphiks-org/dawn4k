package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.webgpu.toolkit.interop.ToolkitSessionView
import org.graphiks.dawn4k.webgpu.toolkit.testing.*
import org.graphiks.webgpu.GPUTextureFormat
import kotlin.test.*

class OffscreenGpuTest {
    @Test fun offscreenTriangleHasExpectedPixel() = runBlocking<Unit> {
        val extension = ToolkitGpuEnvironment.extension()
        ToolkitGpuEnvironment.withGpuSession(extension) { session ->
            val view = ToolkitSessionView(session, extension)
            ToolkitGpuEnvironment.report(view)
            assertOffscreenTrianglePixel(view)
            assertFailsWith<IllegalArgumentException> { view.createRenderTexture(0u, 4u, GPUTextureFormat.RGBA8Unorm) }
        }
    }
}
