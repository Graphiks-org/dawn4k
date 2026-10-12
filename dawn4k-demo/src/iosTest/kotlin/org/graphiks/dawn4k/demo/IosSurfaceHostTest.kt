@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.demo

import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CAMetalLayer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IosSurfaceHostTest {
    @Test fun viewportUsesMetalLayerAndTracksPhysicalDrawableSize() {
        val view = IosMetalView({ _, _ -> }, {})
        view.setFrame(CGRectMake(0.0, 0.0, 100.0, 50.0))
        view.contentScaleFactor = 3.0
        view.layoutSubviews()
        val layer = assertIs<CAMetalLayer>(view.layer)
        layer.drawableSize.useContents {
            assertEquals(300.0, width)
            assertEquals(150.0, height)
        }
    }

    @Test fun leaseRetainsLayerAfterViewRemovalAndClosesIdempotently() {
        val lease = run {
            val view = IosMetalView({ _, _ -> }, {})
            view.removeFromSuperview()
            IosSurfaceHost(1, view.layer as CAMetalLayer, PixelExtent(300, 150))
        }
        assertTrue(lease.valid)
        assertTrue(lease.layerPointer != 0L)
        assertEquals(PixelExtent(300, 150), lease.pixelExtent())
        lease.resize(PixelExtent(150, 300))
        assertEquals(PixelExtent(150, 300), lease.pixelExtent())
        lease.invalidate()
        assertFalse(lease.valid)
        assertTrue(lease.layerPointer != 0L, "invalidating cannot release a layer still used by Dawn")
        lease.close()
        lease.close()
        assertEquals(0L, lease.layerPointer)
    }
}
