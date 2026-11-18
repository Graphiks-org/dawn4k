package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.webgpu.toolkit.interop.ToolkitSessionView
import org.graphiks.dawn4k.webgpu.toolkit.testing.*
import kotlin.test.Test

class HeadlessGpuTest {
    @Test fun headlessComputeReturnsExpectedValues() = runBlocking {
        val extension = ToolkitGpuEnvironment.extension()
        ToolkitGpuEnvironment.withGpuSession(extension) { session ->
            val view = ToolkitSessionView(session, extension)
            ToolkitGpuEnvironment.report(view)
            assertComputeDoublesInput(view)
        }
    }
}
