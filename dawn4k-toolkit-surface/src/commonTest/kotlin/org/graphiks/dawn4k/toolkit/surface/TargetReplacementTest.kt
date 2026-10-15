package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.internal.*
import kotlin.test.*

class TargetReplacementTest {
    @Test fun zeroExtentDoesNotConfigure() = runTest {
        val target = CountingTarget(1, PixelExtent(0, 0))
        val native = CountingSurface()
        val controller = controller(target, native)
        controller.initialize()
        assertNotNull(controller.configuration.value)
        assertEquals(0, native.configurations)
        assertNull(controller.acquireFrame())
        controller.retire()
    }
    @Test fun lostSurfaceRecreatesOnce() = runTest {
        val target = CountingTarget(1)
        val first = CountingSurface().apply { statuses += SurfaceAcquisition.Lost }
        val second = CountingSurface().apply { statuses += SurfaceAcquisition.Lost }
        var recreations = 0
        val controller = controller(target, first) { recreations++; second }
        controller.initialize()
        assertNull(controller.acquireFrame())
        assertEquals(1, recreations)
        assertEquals(1, first.closes)
        controller.retire()
    }
    @Test fun oldGenerationDetachDoesNotTouchReplacement() = runTest {
        val old = CountingTarget(1)
        val fresh = CountingTarget(2)
        val controller = controller(old, CountingSurface())
        controller.initialize()
        controller.replaceTarget(fresh, attachment(fresh, CountingSurface()))
        controller.detachTarget(1)
        assertEquals(2L, controller.activeGeneration)
        assertEquals(0, fresh.retirements)
        assertEquals(1, old.retirements)
        controller.retire()
    }
    @Test fun lostSurfaceUnconfiguresBeforeRecreation() = runTest {
        val target = CountingTarget(1)
        val first = CountingSurface().apply { statuses += SurfaceAcquisition.Lost }
        val controller = controller(target, first) {
            check(first.unconfigurations > 0) { "old surface is still configured" }
            CountingSurface()
        }
        controller.initialize()
        controller.acquireFrame()!!.close()
        controller.retire()
    }
    @Test fun failedReplacementPreservesActiveGeneration() = runTest {
            val target = CountingTarget(1)
            val controller = controller(target, CountingSurface())
            controller.initialize()
            val incompatible = CountingTarget(2)
            val surface = CountingSurface().apply { capabilityFailure = IllegalStateException("incompatible adapter") }
            assertFailsWith<IllegalStateException> {
                controller.replaceTarget(incompatible, attachment(incompatible, surface))
            }
            assertEquals(1L, controller.activeGeneration)
            assertEquals(1, incompatible.retirements)
            controller.retire()
    }

    @Test fun generationChangeCannotHideOldAttachment() = runTest {
        val target = CountingTarget(1)
        val controller = controller(target, CountingSurface())
        controller.initialize()
        target.state.value = target.state.value.copy(generation = 2)
        assertNull(controller.acquireFrame())
        controller.detachTarget(1)
        assertEquals(1, target.retirements)
        assertNull(controller.configuration.value)
    }

    private suspend fun controller(
        target: CountingTarget, native: CountingSurface, recreate: () -> NativeSurface = { CountingSurface() },
    ) = PresentationController(attachment(target, native), DawnPresentationOptions(), recreate)
}
