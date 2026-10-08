package org.graphiks.dawn4k

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SurfaceTextureViewTest {
    @Test
    fun nullViewCannotTransferOwnershipToDevice() = runTest {
        DawnContext.create().use { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                (adapter.requestDevice().getOrThrow() as DawnDevice).use { device ->
                    assertFailsWith<IllegalArgumentException> { device.adoptSurfaceTextureView(0L) }
                }
            }
        }
    }
}
