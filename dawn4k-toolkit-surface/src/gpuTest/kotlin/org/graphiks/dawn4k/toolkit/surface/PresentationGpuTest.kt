package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.graphiks.dawn4k.toolkit.surface.testing.*
import org.graphiks.dawn4k.toolkit.testing.*
import kotlin.test.*

class PresentationGpuTest {
    @Test fun compatibleSurfaceSelectsPresentingAdapter() = toolkitSurfaceTest { fixture ->
        ToolkitGpuEnvironment.report(fixture.gpu)
        assertNotNull(fixture.presentation.configuration.value)
        assertPresentedTriangle(fixture.gpu, fixture.presentation)
        assertOffscreenTrianglePixel(fixture.gpu)
    }

    @Test fun presentationResizePreservesDevice() = toolkitSurfaceTest { fixture ->
        val device = fixture.gpu.device
        val original = assertNotNull(fixture.presentation.configuration.value).extent
        assertPresentedTriangle(fixture.gpu, fixture.presentation)
        fixture.resize(48, 40)
        // Configuration follows the target at the next acquisition, without replacing the device.
        assertPresentedTriangle(fixture.gpu, fixture.presentation)
        val resized = assertNotNull(fixture.presentation.configuration.value).extent
        assertTrue(resized.isPositive)
        assertNotEquals(original, resized)
        assertSame(device, fixture.gpu.device)
    }

    @Test fun cancelledFrameClosesBeforeTheNextAcquisition() = toolkitSurfaceTest { fixture ->
        var cancelled: DawnFrame? = null
        assertFailsWith<CancellationException> {
            fixture.presentation.withFrame { frame ->
                cancelled = frame
                throw CancellationException("application abandoned frame")
            }
        }
        assertFailsWith<IllegalStateException> { assertNotNull(cancelled).view }
        assertPresentedTriangle(fixture.gpu, fixture.presentation)
    }
}
