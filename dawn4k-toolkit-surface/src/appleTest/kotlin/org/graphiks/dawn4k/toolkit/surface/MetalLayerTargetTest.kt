@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import platform.QuartzCore.CAMetalLayer
import org.graphiks.dawn4k.toolkit.DawnExecutionOwner
import kotlin.test.*

class MetalLayerTargetTest {
    @Test fun metalTargetSelectsACompatibleAdapter() = runTest {
        val layer = CAMetalLayer()
        val worker = DawnExecutionOwner.dedicated("test-layer-owner")
        try {
            val target = metalLayerTarget(layer,
                MutableStateFlow(DawnTargetState(2, PixelExtent(16, 16), true)), owner = DawnPlatformOwner.fromExecutionOwner(worker))
            val presented = openDawnSurface(target)
            try { assertNotNull(presented.presentation.configuration.value) }
            finally { presented.shutdown() }
            assertEquals(0L, presented.gpu.context.nativeBridge().instanceHandle())
        } finally { worker.close() }
    }

    @Test fun nativeLayerIsRetainedUntilAcknowledgedRetirement() = runTest {
        val layer = CAMetalLayer()
        val worker = DawnExecutionOwner.dedicated("test-retention-owner")
        try {
            val target = metalLayerTarget(layer,
                MutableStateFlow(DawnTargetState(1, PixelExtent(16, 16), true)), owner = DawnPlatformOwner.fromExecutionOwner(worker))
            val lease = target.retain()
            lease.retire()
            lease.retire()
            assertTrue(target.state.value.available)
        } finally { worker.close() }
    }
}
