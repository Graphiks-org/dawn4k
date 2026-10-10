package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertEquals

class DemoGeometryTest {
    @Test fun unflippedMetalViewportConvertsTopOriginWithoutIncludingControls() {
        assertEquals(LogicalViewport(300f, 180f, 700f, 600f),
            metalViewport(800f, LogicalViewport(300f, 20f, 700f, 600f), false))
    }
    @Test fun flippedMetalViewportKeepsTopOrigin() {
        assertEquals(LogicalViewport(300f, 20f, 700f, 600f),
            metalViewport(800f, LogicalViewport(300f, 20f, 700f, 600f), true))
    }
    @Test fun pixelExtentUsesIndependentBackingScale() {
        assertEquals(PixelExtent(1400, 1200),
            scaledExtent(LogicalViewport(300f, 20f, 700f, 600f), 2f))
    }
}
