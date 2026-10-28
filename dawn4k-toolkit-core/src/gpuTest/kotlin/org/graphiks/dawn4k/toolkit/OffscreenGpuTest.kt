package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.toolkit.testing.*
import org.graphiks.webgpu.GPUTextureFormat
import kotlin.test.*

class OffscreenGpuTest {
    @Test fun offscreenTriangleHasExpectedPixel() = runBlocking<Unit> {
        withDawnSession(ToolkitGpuEnvironment.options) { session ->
            ToolkitGpuEnvironment.report(session)
            assertOffscreenTrianglePixel(session)
            assertFailsWith<IllegalArgumentException> { session.createRenderTexture(0u, 4u, GPUTextureFormat.RGBA8Unorm) }
        }
    }
    @Test fun renderTextureRequiresTheSessionOwner() = runBlocking<Unit> {
        val session = openDawnSession(ToolkitGpuEnvironment.options)
        try { assertFailsWith<IllegalStateException> { session.createRenderTexture(4u, 4u, GPUTextureFormat.RGBA8Unorm) } }
        finally { session.shutdown() }
    }
}
