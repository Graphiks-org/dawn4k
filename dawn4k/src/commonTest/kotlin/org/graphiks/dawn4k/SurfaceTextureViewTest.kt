package org.graphiks.dawn4k

import org.graphiks.dawn4k.testing.gpuTestEnvironment
import org.graphiks.dawn4k.testing.gpuTestConfig

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SurfaceTextureViewTest {
    @Test
    fun nullViewCannotTransferOwnershipToDevice() = runTest {
        if (!gpuTestEnvironment("SurfaceTextureViewTest.nullViewCannotTransferOwnershipToDevice")) return@runTest
        DawnContext.create(gpuTestConfig()).use { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                (adapter.requestDevice().getOrThrow() as DawnDevice).use { device ->
                    assertFailsWith<IllegalArgumentException> { device.adoptSurfaceTextureView(0L) }
                }
            }
        }
    }
}
