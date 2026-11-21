package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.webgpu.toolkit.testing.*
import org.graphiks.webgpu.GPUTextureFormat
import kotlin.test.*

class OffscreenGpuTest {
    @Test fun offscreenTriangleHasExpectedPixel() = runBlocking<Unit> {
        val extension = ToolkitGpuEnvironment.extension()
        ToolkitGpuEnvironment.withGpuSession(extension) { session ->
            ToolkitGpuEnvironment.report(extension, session)
            assertOffscreenTrianglePixel(extension, session)
            assertFailsWith<IllegalArgumentException> { extension.createRenderTexture(session, 0u, 4u, GPUTextureFormat.RGBA8Unorm) }
        }
    }
}
