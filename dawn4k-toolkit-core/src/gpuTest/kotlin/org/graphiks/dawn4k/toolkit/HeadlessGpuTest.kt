package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.toolkit.testing.*
import kotlin.test.Test

class HeadlessGpuTest {
    @Test fun headlessComputeReturnsExpectedValues() = runBlocking {
        withDawnSession(ToolkitGpuEnvironment.options) { session ->
            ToolkitGpuEnvironment.report(session)
            assertComputeDoublesInput(session)
        }
    }
}
