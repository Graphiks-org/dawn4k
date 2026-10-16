package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.internal.*
import kotlin.test.*
import org.graphiks.webgpu.GPUTextureFormat

class TargetReplacementTest {
    @Test fun rejectedReplacementKeepsFailedRetirementForShutdownRetry() = runTest {
        val original = CountingTarget(1)
        val controller = controller(original, CountingSurface())
        controller.initialize()
        val rejected = CountingTarget(2)
        var attempts = 0
        rejected.onRetire = { if (++attempts == 1) error("temporary host retirement failure") }
        val native = CountingSurface().apply { capabilityFailure = IllegalArgumentException("incompatible") }
        val failure = assertFailsWith<IllegalArgumentException> {
            controller.replaceTarget(rejected, attachment(rejected, native))
        }
        assertEquals(1, failure.suppressedExceptions.size)
        assertEquals(1L, controller.activeGeneration)
        assertEquals(0, rejected.retirements)
        controller.retire()
        assertEquals(2, attempts)
        assertEquals(1, rejected.retirements)
        assertEquals(1, original.retirements)
        assertEquals(1, native.closes)
        controller.retire()
        assertEquals(2, attempts)
    }

    @Test fun replacementCanRequireTheExistingSceneFormatAndKeepsItOnAcquisition() = runTest {
        val old = CountingTarget(1)
        val rgba = CountingSurface().apply { advertised = advertised.copy(formats = listOf(GPUTextureFormat.RGBA8Unorm)) }
        val controller = controller(old, rgba)
        controller.initialize()
        val fresh = CountingTarget(2)
        val both = CountingSurface().apply { advertised = advertised.copy(formats = listOf(GPUTextureFormat.BGRA8Unorm, GPUTextureFormat.RGBA8Unorm)) }
        controller.replaceTarget(fresh, attachment(fresh, both), DawnPresentationOptions(requiredFormat = GPUTextureFormat.RGBA8Unorm))
        assertEquals(GPUTextureFormat.RGBA8Unorm, controller.configuration.value?.format)
        controller.acquireFrame()!!.close()
        assertEquals(GPUTextureFormat.RGBA8Unorm, controller.configuration.value?.format)
        controller.retire()
    }

    @Test fun rejectedReplacementOptionsDoNotChangeTheActiveConfiguration() = runTest {
        val old = CountingTarget(1)
        val controller = controller(old, CountingSurface())
        controller.initialize()
        val fresh = CountingTarget(2)
        assertFailsWith<IllegalArgumentException> {
            controller.replaceTarget(fresh, attachment(fresh, CountingSurface()),
                DawnPresentationOptions(requiredFormat = GPUTextureFormat.RGBA8Unorm))
        }
        assertEquals(1L, controller.activeGeneration)
        controller.acquireFrame()!!.close()
        assertEquals(GPUTextureFormat.BGRA8Unorm, controller.configuration.value?.format)
        assertEquals(1, fresh.retirements)
        controller.retire()
    }

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
    @Test fun repeatedLostSurfaceFailsAfterOneRecreationAndRemainsRetirable() = runTest {
        val target = CountingTarget(1)
        val first = CountingSurface().apply { statuses += SurfaceAcquisition.Lost }
        val second = CountingSurface().apply { statuses += SurfaceAcquisition.Lost }
        var recreations = 0
        val controller = controller(target, first) { recreations++; second }
        controller.initialize()
        val failure = assertFailsWith<IllegalStateException> { controller.acquireFrame() }
        assertTrue(failure.message.orEmpty().contains("lost", ignoreCase = true))
        assertEquals(1, recreations)
        assertEquals(1, first.closes)
        controller.retire()
        assertEquals(1, second.closes)
        assertEquals(1, target.retirements)
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
