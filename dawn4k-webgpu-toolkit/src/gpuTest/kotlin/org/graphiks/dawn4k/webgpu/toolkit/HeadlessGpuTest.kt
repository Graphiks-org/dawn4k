package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.webgpu.toolkit.testing.*
import org.graphiks.webgpu.GPUTextureFormat
import kotlin.test.*

class HeadlessGpuTest {
    @Test fun headlessComputeReturnsExpectedValues() = runBlocking {
        val extension = ToolkitGpuEnvironment.extension()
        ToolkitGpuEnvironment.withGpuSession(extension) { session ->
            ToolkitGpuEnvironment.report(extension, session)
            assertComputeDoublesInput(extension, session)
        }
    }
}
