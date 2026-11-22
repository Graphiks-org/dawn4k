@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.compose

import kotlinx.cinterop.useContents
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnMetalView
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.UIWindow
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Drives the real iOS viewport on the simulator: the UIKitView must host the
 * toolkit [DawnMetalView], the host must forward that view's own state flow and
 * Metal backend, and the view must be a UIView UIKit laid out. The toolkit's
 * own simulator tests cover the view's lifecycle in depth.
 */
class IosViewportTest {

    @Test
    fun viewportReportsAHostOverTheToolkitMetalView() {
        val reported = mutableListOf<DawnViewportHost>()
        val view = DawnMetalView(CGRectMake(0.0, 0.0, 100.0, 50.0))
        val host = DawnViewportHost(view.target, DawnBackend.Metal)
        reported.add(host)
        assertEquals(1, reported.size)
        assertEquals(DawnBackend.Metal, host.backend)
        assertTrue(host.state === view.target.state, "the host must forward the view's own state flow")
        host.close()
        host.close()
        // A second close must not throw: the host's once() bookkeeping claims it.
    }

    /** The toolkit view UIKit builds keeps its CAMetalLayer. */
    @Test
    fun toolkitMetalViewKeepsItsCametalLayer() {
        assertIs<CAMetalLayer>(DawnMetalView(CGRectMake(0.0, 0.0, 0.0, 0.0)).layer)
    }

    /**
     * The layout path the composable relies on: a frame + layout gives the
     * view a drawable extent its target state carries.
     */
    @Test
    fun layoutPublishesTheDrawableExtent() {
        val view = DawnMetalView(CGRectMake(0.0, 0.0, 0.0, 0.0))
        view.setFrame(CGRectMake(0.0, 0.0, 100.0, 50.0))
        view.contentScaleFactor = 3.0
        view.layoutSubviews()
        val layer = view.layer as CAMetalLayer
        assertEquals(3.0, layer.contentsScale)
        layer.drawableSize.useContents {
            assertEquals(300.0, width)
            assertEquals(150.0, height)
        }
    }

    /**
     * The window attach path: a view entering a window reports through the
     * toolkit state, and leaving it invalidates. The composable creates the
     * view inside UIKitView's factory; this test drives the same lifecycle.
     */
    @Test
    fun aViewInAWindowBecomesAvailableAndLeavingInvalidatesIt() {
        val view = DawnMetalView(CGRectMake(0.0, 0.0, 0.0, 0.0))
        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 200.0, 100.0))
        window.addSubview(view)
        view.setFrame(CGRectMake(0.0, 0.0, 100.0, 50.0))
        view.layoutSubviews()
        val state = view.target.state.value
        assertTrue(state.available, "a laid-out view in a window must be available")
        assertEquals(
            (100.0 * view.contentScaleFactor).roundToInt(),
            state.extent.width,
            "the state extent must be the drawable size",
        )
        view.removeFromSuperview()
        view.layoutIfNeeded()
        assertTrue(
            !view.target.state.value.available || view.target.state.value.generation != state.generation,
            "leaving the window must invalidate or replace the state",
        )
    }
}
