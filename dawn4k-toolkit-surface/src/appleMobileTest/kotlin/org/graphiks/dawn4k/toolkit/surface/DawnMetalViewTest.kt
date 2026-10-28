@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.surface

import kotlinx.cinterop.useContents
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.toolkit.surface.internal.HandleLease
import org.graphiks.dawn4k.toolkit.surface.internal.TargetLifecycle
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSThread
import platform.QuartzCore.CAMetalLayer
import kotlin.test.*

class DawnMetalViewTest {
    @Test fun layoutUsesPhysicalDrawableSize() = appleMainTest {
        DawnPlatformOwners.appleMain().onOwner {
            val view = DawnMetalView(CGRectMake(0.0, 0.0, 100.0, 50.0))
            view.contentScaleFactor = 3.0
            view.layoutSubviews()
            assertIs<CAMetalLayer>(view.layer).drawableSize.useContents {
                assertEquals(300.0, width)
                assertEquals(150.0, height)
            }
        }
    }
    @Test fun layerSurvivesViewRemovalUntilRetirement() = appleMainTest {
        val owner = DawnPlatformOwners.appleMain()
        val view = owner.onOwner { DawnMetalView(CGRectMake(0.0, 0.0, 16.0, 16.0)) }
        val layer = owner.onOwner { view.layer as CAMetalLayer }
        val states = MutableStateFlow(DawnTargetState(1, PixelExtent(16, 16), true))
        val lease = metalLayerTarget(layer, states).retain() as HandleLease
        assertNotEquals(0L, lease.retainedHandle)
        owner.onOwner { view.removeFromSuperview() }
        states.value = states.value.copy(available = false)
        assertNotEquals(0L, lease.retainedHandle)
        lease.retire()
        lease.retire()
        assertEquals(0L, lease.retainedHandle)
    }
    @Test fun delayedOldDetachLeavesReplacementAlive() {
        val lifecycle = TargetLifecycle()
        val old = lifecycle.available(PixelExtent(16, 16))
        val fresh = lifecycle.available(PixelExtent(32, 32))
        lifecycle.invalidate(old)
        assertEquals(fresh, lifecycle.state.value.generation)
        assertTrue(lifecycle.state.value.available)
    }
    @Test fun uiOperationsUseMainThread() = appleMainTest {
        assertTrue(DawnPlatformOwners.appleMain().onOwner { NSThread.isMainThread })
        assertFailsWith<IllegalStateException> { DawnMetalView(CGRectMake(0.0, 0.0, 1.0, 1.0)) }
    }
}
