package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.internal.*
import kotlin.test.*

class PresentationBootstrapTest {
    @Test fun rejectedUnboundReplacementRetirementIsRetriedDuringSessionShutdown() = runTest {
        val original = CountingTarget(1)
        val rejected = CountingTarget(2)
        var attempts = 0
        var retryAllowed = false
        rejected.onRetire = {
            attempts++
            check(retryAllowed) { "temporary host retirement failure" }
        }
        val factory = SurfaceFactory { lease, _ ->
            if (lease === rejected) error("surface creation failed")
            val native = CountingSurface()
            SurfaceAttachment(lease, null, { native }, native::close)
        }
        val presented = openDawnSurface(original, DawnSessionOptions(), DawnPresentationOptions(), factory)
        try {
            val failure = assertFailsWith<IllegalStateException> { presented.presentation.replaceTarget(rejected) }
            assertEquals("surface creation failed", failure.message)
            assertEquals(0, rejected.retirements)
            retryAllowed = true
            presented.shutdown()
            assertTrue(attempts >= 2)
            assertEquals(1, rejected.retirements)
            assertEquals(1, original.retirements)
        } finally { retryAllowed = true; presented.shutdown() }
    }

    @Test fun frameCancellationReleasesViewThenTexture() = runTest {
        val target = CountingTarget(1)
        val native = CountingSurface()
        val factory = SurfaceFactory { lease, _ -> SurfaceAttachment(lease, null, { native }, native::close) }
        val presented = openDawnSurface(target, DawnSessionOptions(), DawnPresentationOptions(), factory)
        try {
            assertFailsWith<kotlinx.coroutines.CancellationException> {
                presented.presentation.withFrame { throw kotlinx.coroutines.CancellationException("cancel frame") }
            }
            assertEquals(listOf("view", "texture"), native.releases)
        } finally { presented.shutdown() }
    }

    @Test fun targetGenerationChangeRetiresTheOldAttachment() = runTest {
        val target = CountingTarget(1)
        val native = CountingSurface()
        val retired = CompletableDeferred<Unit>()
        target.onRetire = { retired.complete(Unit) }
        val factory = SurfaceFactory { lease, _ -> SurfaceAttachment(lease, null, { native }, native::close) }
        val presented = openDawnSurface(target, DawnSessionOptions(), DawnPresentationOptions(), factory)
        try {
            presented.gpu.run {
                target.state.value = target.state.value.copy(generation = 2, available = true)
                withTimeout(1_000) { retired.await() }
            }
            assertNull(presented.presentation.configuration.value)
            assertEquals(1, target.retirements)
        } finally { presented.shutdown() }
    }

    @Test fun targetRetirementWaitsForFrameAndCanProgressInsideRun() = runTest {
        val target = CountingTarget(1)
        val native = CountingSurface()
        val retired = CompletableDeferred<Unit>()
        target.onRetire = { retired.complete(Unit) }
        val factory = SurfaceFactory { lease, _ -> SurfaceAttachment(lease, null, { native }, native::close) }
        val presented = openDawnSurface(target, DawnSessionOptions(), DawnPresentationOptions(), factory)
        try {
            presented.gpu.run {
                presented.presentation.withFrame {
                    target.state.value = target.state.value.copy(available = false)
                    delay(10)
                    assertFalse(retired.isCompleted)
                }
                withTimeout(1_000) { retired.await() }
            }
            assertEquals(listOf("view", "texture"), native.releases)
            assertEquals(1, target.retirements)
        } finally { presented.shutdown() }
    }

    @Test fun capabilityFailureRetiresSurfaceAndTargetBeforeContext() = runTest {
        val target = CountingTarget(1)
        val native = CountingSurface().apply { capabilityFailure = IllegalStateException("unsupported capability") }
        var context: DawnContext? = null
        val factory = SurfaceFactory { lease, suppliedContext ->
            context = suppliedContext
            native.onClose = {
                assertNotEquals(0L, suppliedContext.nativeBridge().instanceHandle())
            }
            target.onRetire = {
                assertEquals(1, native.closes)
                assertNotEquals(0L, suppliedContext.nativeBridge().instanceHandle())
            }
            SurfaceAttachment(lease, null, { native }, native::close)
        }
        assertFailsWith<IllegalStateException> {
            openDawnSurface(target, DawnSessionOptions(), DawnPresentationOptions(), factory)
        }
        assertEquals(1, native.closes)
        assertEquals(1, target.retirements)
        assertEquals(0L, assertNotNull(context).nativeBridge().instanceHandle())
    }
}
